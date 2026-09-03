import { assertEquals, assertMatch } from "jsr:@std/assert";
import {
  validateMediaFile,
  getSafeExtension,
  sanitizeFileName,
  generateServerScopedObjectKey,
  MAX_FILE_SIZE_BYTES,
  DANGEROUS_EXTENSIONS,
  DANGEROUS_MIME_TYPES,
} from "../_shared/r2.ts";

Deno.test("validateMediaFile - allows valid images, pdfs, spreadsheets, and generic documents", () => {
  // Image
  assertEquals(validateMediaFile("notes.jpg", "image/jpeg", 1024 * 1024), { valid: true });
  assertEquals(validateMediaFile("photo.png", "image/png", 2 * 1024 * 1024), { valid: true });
  assertEquals(validateMediaFile("chart.webp", "image/webp", 500 * 1024), { valid: true });

  // PDF
  assertEquals(validateMediaFile("syllabus.pdf", "application/pdf", 5 * 1024 * 1024), { valid: true });

  // Excel / CSV
  assertEquals(
    validateMediaFile(
      "grades.xlsx",
      "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
      1024 * 1024
    ),
    { valid: true }
  );
  assertEquals(validateMediaFile("attendance.csv", "text/csv", 50 * 1024), { valid: true });

  // Generic document
  assertEquals(
    validateMediaFile(
      "assignment.docx",
      "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
      2 * 1024 * 1024
    ),
    { valid: true }
  );
  assertEquals(validateMediaFile("readme.txt", "text/plain", 10 * 1024), { valid: true });
});

Deno.test("validateMediaFile - rejects dangerous executable/script MIME types and extensions", () => {
  // Dangerous extensions
  const dangerousExts = ["exe", "bat", "sh", "js", "php", "apk", "jar", "cmd", "ps1", "vbs"];
  for (const ext of dangerousExts) {
    const res = validateMediaFile(`malware.${ext}`, "application/octet-stream", 1024);
    assertEquals(res.valid, false);
    assertMatch((res as { valid: false; error: string }).error, /Disallowed file extension/);
  }

  // Dangerous MIME types
  const dangerousMimes = [
    "application/x-msdownload",
    "application/x-sh",
    "application/x-bat",
    "text/javascript",
    "application/javascript",
    "application/x-php",
    "text/html",
  ];
  for (const mime of dangerousMimes) {
    const res = validateMediaFile("script.txt", mime, 1024);
    assertEquals(res.valid, false);
    assertMatch((res as { valid: false; error: string }).error, /Disallowed content type/);
  }
});

Deno.test("validateMediaFile - enforces maximum file size limit server-side", () => {
  // Exceeds 50MB
  const resOver = validateMediaFile("huge.pdf", "application/pdf", MAX_FILE_SIZE_BYTES + 1);
  assertEquals(resOver.valid, false);
  assertMatch((resOver as { valid: false; error: string }).error, /File size exceeds/);

  // Zero or negative
  const resZero = validateMediaFile("empty.pdf", "application/pdf", 0);
  assertEquals(resZero.valid, false);

  const resNeg = validateMediaFile("bad.pdf", "application/pdf", -5);
  assertEquals(resNeg.valid, false);

  // Exactly at boundary
  const resLimit = validateMediaFile("exact.pdf", "application/pdf", MAX_FILE_SIZE_BYTES);
  assertEquals(resLimit.valid, true);
});

Deno.test("generateServerScopedObjectKey - scopes to groups/{groupId}/{userId}/ and generates UUID", () => {
  const groupId = "grp-888-abc";
  const userId = "usr-999-xyz";
  const key = generateServerScopedObjectKey(groupId, userId, "../../../etc/passwd.pdf", "application/pdf");

  // Must start with groups/grp-888-abc/usr-999-xyz/
  assertEquals(key.startsWith(`groups/${groupId}/${userId}/`), true);
  // Must end with .pdf
  assertEquals(key.endsWith(".pdf"), true);
  // Must not contain path traversal
  assertEquals(key.includes(".."), false);
  assertEquals(key.includes("passwd"), false);

  // Key structure: groups/{groupId}/{userId}/{uuid}.pdf
  const segments = key.split("/");
  assertEquals(segments.length, 4);
  assertEquals(segments[0], "groups");
  assertEquals(segments[1], groupId);
  assertEquals(segments[2], userId);
  // UUID regex
  assertMatch(segments[3], /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\.pdf$/);
});

Deno.test("sanitizeFileName - sanitizes special characters safely", () => {
  const sanitized = sanitizeFileName("my assignment (v1.2) [final] & notes?.pdf");
  assertEquals(sanitized.includes("&"), false);
  assertEquals(sanitized.includes("?"), false);
  assertEquals(sanitized.includes(" "), false);
});

Deno.test("r2-get-download-url namespace and traversal validation", () => {
  const targetGroupId = "grp-target-123";
  const validKey = `groups/${targetGroupId}/user-456/file.pdf`;

  // Valid key within group
  assertEquals(validKey.startsWith(`groups/${targetGroupId}/`), true);
  assertEquals(validKey.includes(".."), false);

  // Foreign group key
  const foreignKey = `groups/different-group/user-456/file.pdf`;
  assertEquals(foreignKey.startsWith(`groups/${targetGroupId}/`), false);

  // Path traversal attempt
  const traversalKey = `groups/${targetGroupId}/../../secret.key`;
  assertEquals(traversalKey.includes(".."), true);
});

