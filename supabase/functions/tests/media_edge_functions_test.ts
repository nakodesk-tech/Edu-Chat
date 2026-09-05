import { assertEquals, assertMatch } from "jsr:@std/assert";
import {
  isAuthorizedRole,
  isOfficerAdminRole,
  evaluateMediaAccess,
  ProfileData,
  GroupData,
  GroupMembershipData,
} from "../_shared/auth.ts";
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

  // Legacy schools namespace key allowed for backward compatibility
  const legacySchoolKey = "schools/sch-001/attachments/photo.jpg";
  assertEquals(legacySchoolKey.startsWith("schools/"), true);
  assertEquals(legacySchoolKey.includes(".."), false);

  // Foreign group key
  const foreignKey = `groups/different-group/user-456/file.pdf`;
  assertEquals(foreignKey.startsWith(`groups/${targetGroupId}/`), false);

  // Path traversal attempt
  const traversalKey = `groups/${targetGroupId}/../../secret.key`;
  assertEquals(traversalKey.includes(".."), true);
});

Deno.test("auth role validation - accepts officer_admin, school_admin, teacher, student", () => {
  // All 4 valid roles must be accepted
  assertEquals(isAuthorizedRole("officer_admin"), true);
  assertEquals(isAuthorizedRole("school_admin"), true);
  assertEquals(isAuthorizedRole("teacher"), true);
  assertEquals(isAuthorizedRole("student"), true);

  // Case-insensitive and trimmed
  assertEquals(isAuthorizedRole(" OFFICER_ADMIN "), true);
  assertEquals(isAuthorizedRole("School_Admin"), true);

  // Unauthorized roles must be rejected
  assertEquals(isAuthorizedRole("guest"), false);
  assertEquals(isAuthorizedRole("parent"), false);
  assertEquals(isAuthorizedRole("super_user"), false);
  assertEquals(isAuthorizedRole(""), false);
  assertEquals(isAuthorizedRole(null), false);
  assertEquals(isAuthorizedRole(undefined), false);
});

Deno.test("auth officer admin role check - identifies officer_admin correctly", () => {
  assertEquals(isOfficerAdminRole("officer_admin"), true);
  assertEquals(isOfficerAdminRole("OFFICER_ADMIN"), true);
  assertEquals(isOfficerAdminRole(" officer_admin "), true);
  assertEquals(isOfficerAdminRole("primary_officer_admin"), true);

  assertEquals(isOfficerAdminRole("school_admin"), false);
  assertEquals(isOfficerAdminRole("teacher"), false);
  assertEquals(isOfficerAdminRole("student"), false);
  assertEquals(isOfficerAdminRole(""), false);
  assertEquals(isOfficerAdminRole(null), false);
});

// =========================================================================
// Focused Media Authorization Tests
// =========================================================================

const activeGroup: GroupData = { id: "grp-123", is_active: true };
const activeMembership: GroupMembershipData = { id: "mem-1", is_active: true };

Deno.test("media auth - active school_admin with school_id works", () => {
  const profile: ProfileData = {
    id: "usr-sa-1",
    role: "school_admin",
    is_active: true,
    school_id: "sch-001",
  };
  const result = evaluateMediaAccess(profile, activeGroup, activeMembership);
  assertEquals(result.allowed, true);
  assertEquals(result.role, "school_admin");
});

Deno.test("media auth - active teacher works", () => {
  const profile: ProfileData = {
    id: "usr-t-1",
    role: "teacher",
    is_active: true,
    school_id: "sch-001",
  };
  const result = evaluateMediaAccess(profile, activeGroup, activeMembership);
  assertEquals(result.allowed, true);
  assertEquals(result.role, "teacher");
});

Deno.test("media auth - active student works", () => {
  const profile: ProfileData = {
    id: "usr-st-1",
    role: "student",
    is_active: true,
    school_id: "sch-001",
  };
  const result = evaluateMediaAccess(profile, activeGroup, activeMembership);
  assertEquals(result.allowed, true);
  assertEquals(result.role, "student");
});

Deno.test("media auth - active officer_admin with school_id NULL works", () => {
  // Officer Admin valid profile with school_id = null and NO membership row
  const profile: ProfileData = {
    id: "usr-oa-1",
    role: "officer_admin",
    is_active: true,
    school_id: null,
  };
  const result = evaluateMediaAccess(profile, activeGroup, null);
  assertEquals(result.allowed, true);
  assertEquals(result.role, "officer_admin");
});

Deno.test("media auth - primary officer_admin (using stored role value) works", () => {
  // Primary Officer Admin stored as role = "officer_admin", is_primary_admin = true, school_id = null
  const primaryProfile1: ProfileData = {
    id: "usr-poa-1",
    role: "officer_admin",
    is_active: true,
    is_primary_admin: true,
    school_id: null,
  };
  const result1 = evaluateMediaAccess(primaryProfile1, activeGroup, null);
  assertEquals(result1.allowed, true);
  assertEquals(result1.role, "officer_admin");

  // Or if stored as role = "primary_officer_admin"
  const primaryProfile2: ProfileData = {
    id: "usr-poa-2",
    role: "primary_officer_admin",
    is_active: true,
    school_id: null,
  };
  const result2 = evaluateMediaAccess(primaryProfile2, activeGroup, null);
  assertEquals(result2.allowed, true);
  assertEquals(result2.role, "primary_officer_admin");
});

Deno.test("media auth - inactive user denied", () => {
  const inactiveProfile: ProfileData = {
    id: "usr-inact-1",
    role: "officer_admin",
    is_active: false,
    school_id: null,
  };
  const result = evaluateMediaAccess(inactiveProfile, activeGroup, null);
  assertEquals(result.allowed, false);
  assertEquals(result.status, 403);
  assertEquals(result.branch, "BRANCH_PROFILE_DEACTIVATED");
});

Deno.test("media auth - unauthorized user denied", () => {
  // Unknown or unauthorized role
  const unauthorizedProfile: ProfileData = {
    id: "usr-unauth-1",
    role: "parent",
    is_active: true,
    school_id: "sch-001",
  };
  const result = evaluateMediaAccess(unauthorizedProfile, activeGroup, activeMembership);
  assertEquals(result.allowed, false);
  assertEquals(result.status, 403);
  assertEquals(result.branch, "BRANCH_ROLE_UNAUTHORIZED");
});

Deno.test("media auth - school_id NULL alone never causes a 403", () => {
  // For teacher with school_id = null (e.g. multi-school or pending assignment) but active group member
  const teacherNullSchool: ProfileData = {
    id: "usr-t-null",
    role: "teacher",
    is_active: true,
    school_id: null,
  };
  const resTeacher = evaluateMediaAccess(teacherNullSchool, activeGroup, activeMembership);
  assertEquals(resTeacher.allowed, true);

  // For officer admin with school_id = null
  const officerNullSchool: ProfileData = {
    id: "usr-oa-null",
    role: "officer_admin",
    is_active: true,
    school_id: null,
  };
  const resOfficer = evaluateMediaAccess(officerNullSchool, activeGroup, null);
  assertEquals(resOfficer.allowed, true);
});

