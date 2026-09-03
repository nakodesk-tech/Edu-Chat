import {
  S3Client,
  PutObjectCommand,
  GetObjectCommand,
} from "npm:@aws-sdk/client-s3@^3.750.0";
import { getSignedUrl } from "npm:@aws-sdk/s3-request-presigner@^3.750.0";

// Sensible maximum file size server-side: 50 MB
export const MAX_FILE_SIZE_BYTES = 50 * 1024 * 1024;

// Disallowed dangerous script and executable extensions
export const DANGEROUS_EXTENSIONS = new Set<string>([
  "exe",
  "dll",
  "bat",
  "cmd",
  "sh",
  "bash",
  "ps1",
  "vbs",
  "js",
  "mjs",
  "cjs",
  "ts",
  "jsx",
  "tsx",
  "php",
  "phtml",
  "phar",
  "py",
  "rb",
  "pl",
  "cgi",
  "apk",
  "jar",
  "war",
  "ear",
  "msi",
  "scr",
  "com",
  "pif",
  "hta",
  "cpl",
  "wsf",
  "scf",
]);

// Disallowed dangerous executable / script MIME types
export const DANGEROUS_MIME_TYPES = new Set<string>([
  "application/x-msdownload",
  "application/x-executable",
  "application/x-sh",
  "application/x-bat",
  "application/x-csh",
  "application/x-dosexec",
  "application/x-msdos-program",
  "text/javascript",
  "application/javascript",
  "application/x-javascript",
  "application/x-php",
  "application/php",
  "text/html",
  "application/xhtml+xml",
  "application/java-archive",
  "application/x-httpd-php",
]);

/**
 * Validates whether the requested file and MIME type are safe.
 */
export function validateMediaFile(
  fileName: string,
  contentType: string,
  fileSize?: number | null
): { valid: true } | { valid: false; error: string } {
  const trimmedName = fileName.trim();
  const trimmedMime = contentType.trim().toLowerCase();

  if (!trimmedName) {
    return { valid: false, error: "File name cannot be empty." };
  }
  if (!trimmedMime) {
    return { valid: false, error: "Content-Type cannot be empty." };
  }

  // Reject dangerous MIME types
  if (DANGEROUS_MIME_TYPES.has(trimmedMime)) {
    return {
      valid: false,
      error: `Disallowed content type: executable and script media are prohibited (${trimmedMime}).`,
    };
  }

  // Check file extension
  const extensionMatch = trimmedName.match(/\.([a-zA-Z0-9]+)$/);
  if (extensionMatch) {
    const ext = extensionMatch[1].toLowerCase();
    if (DANGEROUS_EXTENSIONS.has(ext)) {
      return {
        valid: false,
        error: `Disallowed file extension: .${ext} files are prohibited for security.`,
      };
    }
  }

  // Enforce server-side maximum file size limit
  if (fileSize !== undefined && fileSize !== null) {
    if (fileSize <= 0) {
      return { valid: false, error: "File size must be greater than zero bytes." };
    }
    if (fileSize > MAX_FILE_SIZE_BYTES) {
      return {
        valid: false,
        error: `File size exceeds server limit of ${MAX_FILE_SIZE_BYTES / (1024 * 1024)}MB.`,
      };
    }
  }

  return { valid: true };
}

/**
 * Extracts or derives a safe sanitized extension.
 */
export function getSafeExtension(fileName: string, contentType: string): string {
  const extensionMatch = fileName.trim().match(/\.([a-zA-Z0-9]+)$/);
  if (extensionMatch) {
    const ext = extensionMatch[1].toLowerCase();
    if (!DANGEROUS_EXTENSIONS.has(ext)) {
      return `.${ext}`;
    }
  }

  switch (contentType.trim().toLowerCase()) {
    case "image/jpeg":
      return ".jpg";
    case "image/png":
      return ".png";
    case "image/webp":
      return ".webp";
    case "image/gif":
      return ".gif";
    case "application/pdf":
      return ".pdf";
    case "text/csv":
      return ".csv";
    case "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet":
      return ".xlsx";
    case "application/vnd.ms-excel":
      return ".xls";
    case "application/vnd.openxmlformats-officedocument.wordprocessingml.document":
      return ".docx";
    case "application/msword":
      return ".doc";
    case "text/plain":
      return ".txt";
    case "application/zip":
    case "application/x-zip-compressed":
      return ".zip";
    default:
      return ".bin";
  }
}

/**
 * Sanitizes original file name for safe metadata storage.
 */
export function sanitizeFileName(fileName: string): string {
  return fileName
    .trim()
    .replace(/[^\w\s\.-]/gi, "_")
    .replace(/\s+/g, "_");
}

/**
 * Generates a server-controlled, UUID-based R2 object key scoped to group and authenticated user.
 * Path format: groups/{groupId}/{userId}/{uuid}{extension}
 * Never trusts a client-supplied object key or path.
 */
export function generateServerScopedObjectKey(
  groupId: string,
  userId: string,
  fileName: string,
  contentType: string
): string {
  const safeExt = getSafeExtension(fileName, contentType);
  const uuid = crypto.randomUUID();
  return `groups/${groupId}/${userId}/${uuid}${safeExt}`;
}

/**
 * Returns an S3 client configured for private Cloudflare R2 bucket access.
 */
export function getR2Client(): { s3: S3Client; bucketName: string } {
  const accountId = Deno.env.get("R2_ACCOUNT_ID") || "";
  const accessKeyId = Deno.env.get("R2_ACCESS_KEY_ID") || "";
  const secretAccessKey = Deno.env.get("R2_SECRET_ACCESS_KEY") || "";
  const bucketName = Deno.env.get("R2_BUCKET_NAME") || "educhat-media";

  if (!accountId || !accessKeyId || !secretAccessKey) {
    throw new Error(
      "Missing Cloudflare R2 secrets. Ensure R2_ACCOUNT_ID, R2_ACCESS_KEY_ID, and R2_SECRET_ACCESS_KEY are set."
    );
  }

  const s3 = new S3Client({
    region: "auto",
    endpoint: `https://${accountId}.r2.cloudflarestorage.com`,
    credentials: {
      accessKeyId,
      secretAccessKey,
    },
  });

  return { s3, bucketName };
}

/**
 * Generates a presigned PUT upload URL for Cloudflare R2.
 */
export async function createPresignedUploadUrl(
  objectKey: string,
  contentType: string,
  expiresInSeconds: number = 3600
): Promise<string> {
  const { s3, bucketName } = getR2Client();
  const command = new PutObjectCommand({
    Bucket: bucketName,
    Key: objectKey,
    ContentType: contentType,
  });
  return await getSignedUrl(s3, command, { expiresIn: expiresInSeconds });
}

/**
 * Generates a short-lived presigned GET download URL for Cloudflare R2.
 */
export async function createPresignedDownloadUrl(
  objectKey: string,
  expiresInSeconds: number = 3600
): Promise<string> {
  const { s3, bucketName } = getR2Client();
  const command = new GetObjectCommand({
    Bucket: bucketName,
    Key: objectKey,
  });
  return await getSignedUrl(s3, command, { expiresIn: expiresInSeconds });
}
