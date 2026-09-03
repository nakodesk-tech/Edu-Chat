import "jsr:@supabase/functions-js/edge-runtime.d.ts";
import { corsHeaders } from "../_shared/cors.ts";
import { authorizeGroupAccess } from "../_shared/auth.ts";
import {
  validateMediaFile,
  sanitizeFileName,
  generateServerScopedObjectKey,
  createPresignedUploadUrl,
} from "../_shared/r2.ts";

interface CreateUploadUrlBody {
  fileName?: string;
  contentType?: string;
  groupId?: string;
  fileSize?: number;
}

Deno.serve(async (req: Request) => {
  // Handle CORS preflight
  if (req.method === "OPTIONS") {
    return new Response("ok", { headers: corsHeaders });
  }

  if (req.method !== "POST") {
    return new Response(JSON.stringify({ error: "Method not allowed" }), {
      status: 405,
      headers: { ...corsHeaders, "Content-Type": "application/json" },
    });
  }

  try {
    let body: CreateUploadUrlBody;
    try {
      body = await req.json();
    } catch {
      return new Response(JSON.stringify({ error: "Invalid JSON body" }), {
        status: 400,
        headers: { ...corsHeaders, "Content-Type": "application/json" },
      });
    }

    const { fileName, contentType, groupId, fileSize } = body;

    if (!groupId || typeof groupId !== "string" || groupId.trim() === "") {
      return new Response(
        JSON.stringify({ error: "Bad Request: groupId is required." }),
        {
          status: 400,
          headers: { ...corsHeaders, "Content-Type": "application/json" },
        }
      );
    }

    if (!fileName || typeof fileName !== "string" || fileName.trim() === "") {
      return new Response(
        JSON.stringify({ error: "Bad Request: fileName is required." }),
        {
          status: 400,
          headers: { ...corsHeaders, "Content-Type": "application/json" },
        }
      );
    }

    if (!contentType || typeof contentType !== "string" || contentType.trim() === "") {
      return new Response(
        JSON.stringify({ error: "Bad Request: contentType is required." }),
        {
          status: 400,
          headers: { ...corsHeaders, "Content-Type": "application/json" },
        }
      );
    }

    // 1. Authorize: active profile + active target group + active group membership
    const authResult = await authorizeGroupAccess(req, groupId);
    if (!authResult.success) {
      return new Response(JSON.stringify({ error: authResult.error }), {
        status: authResult.status,
        headers: { ...corsHeaders, "Content-Type": "application/json" },
      });
    }

    const { userId } = authResult.context;

    // 2. Validate media file: safe MIME, safe extension, max size check
    const validation = validateMediaFile(fileName, contentType, fileSize);
    if (!validation.valid) {
      return new Response(JSON.stringify({ error: validation.error }), {
        status: 400,
        headers: { ...corsHeaders, "Content-Type": "application/json" },
      });
    }

    // 3. Generate server-controlled UUID-based R2 object key scoped to group & user
    const objectKey = generateServerScopedObjectKey(
      groupId.trim(),
      userId,
      fileName,
      contentType
    );

    const safeName = sanitizeFileName(fileName);
    const expiresInSeconds = 3600; // 1 hour

    // 4. Generate presigned PUT URL
    const uploadUrl = await createPresignedUploadUrl(
      objectKey,
      contentType.trim(),
      expiresInSeconds
    );

    // 5. Return exact Android contract in camelCase
    return new Response(
      JSON.stringify({
        uploadUrl,
        objectKey,
        contentType: contentType.trim(),
        fileName: safeName,
        expiresIn: expiresInSeconds,
      }),
      {
        status: 200,
        headers: { ...corsHeaders, "Content-Type": "application/json" },
      }
    );
  } catch (err: unknown) {
    const message = err instanceof Error ? err.message : "Internal Server Error";
    console.error("Error in r2-create-upload-url:", err);
    return new Response(JSON.stringify({ error: message }), {
      status: 500,
      headers: { ...corsHeaders, "Content-Type": "application/json" },
    });
  }
});
