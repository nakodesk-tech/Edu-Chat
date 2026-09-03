import "jsr:@supabase/functions-js/edge-runtime.d.ts";
import { corsHeaders } from "../_shared/cors.ts";
import { authorizeGroupAccess } from "../_shared/auth.ts";
import { createPresignedDownloadUrl } from "../_shared/r2.ts";

interface GetDownloadUrlBody {
  groupId?: string;
  objectKey?: string;
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
    let body: GetDownloadUrlBody;
    try {
      body = await req.json();
    } catch {
      return new Response(JSON.stringify({ error: "Invalid JSON body" }), {
        status: 400,
        headers: { ...corsHeaders, "Content-Type": "application/json" },
      });
    }

    const { groupId, objectKey } = body;

    if (!groupId || typeof groupId !== "string" || groupId.trim() === "") {
      return new Response(
        JSON.stringify({ error: "Bad Request: groupId is required." }),
        {
          status: 400,
          headers: { ...corsHeaders, "Content-Type": "application/json" },
        }
      );
    }

    if (!objectKey || typeof objectKey !== "string" || objectKey.trim() === "") {
      return new Response(
        JSON.stringify({ error: "Bad Request: objectKey is required." }),
        {
          status: 400,
          headers: { ...corsHeaders, "Content-Type": "application/json" },
        }
      );
    }

    const cleanGroupId = groupId.trim();
    const cleanObjectKey = objectKey.trim();

    // Prevent directory traversal attacks
    if (
      cleanObjectKey.includes("..") ||
      cleanObjectKey.includes("//") ||
      cleanObjectKey.startsWith("/")
    ) {
      return new Response(
        JSON.stringify({ error: "Bad Request: invalid objectKey format." }),
        {
          status: 400,
          headers: { ...corsHeaders, "Content-Type": "application/json" },
        }
      );
    }

    // Validate that the requested objectKey belongs to the target group namespace
    const expectedGroupPrefix = `groups/${cleanGroupId}/`;
    if (!cleanObjectKey.startsWith(expectedGroupPrefix)) {
      return new Response(
        JSON.stringify({
          error: "Forbidden: objectKey does not belong to the requested group namespace.",
        }),
        {
          status: 403,
          headers: { ...corsHeaders, "Content-Type": "application/json" },
        }
      );
    }

    // 1. Authorize: active profile + active target group + active group membership
    const authResult = await authorizeGroupAccess(req, cleanGroupId);
    if (!authResult.success) {
      return new Response(JSON.stringify({ error: authResult.error }), {
        status: authResult.status,
        headers: { ...corsHeaders, "Content-Type": "application/json" },
      });
    }

    const expiresInSeconds = 3600; // 1 hour short-lived presigned URL

    // 2. Generate short-lived signed GET URL from Cloudflare R2
    const downloadUrl = await createPresignedDownloadUrl(
      cleanObjectKey,
      expiresInSeconds
    );

    // 3. Return exact Android contract in camelCase
    return new Response(
      JSON.stringify({
        downloadUrl,
        objectKey: cleanObjectKey,
        expiresIn: expiresInSeconds,
      }),
      {
        status: 200,
        headers: { ...corsHeaders, "Content-Type": "application/json" },
      }
    );
  } catch (err: unknown) {
    const message = err instanceof Error ? err.message : "Internal Server Error";
    console.error("Error in r2-get-download-url:", err);
    return new Response(JSON.stringify({ error: message }), {
      status: 500,
      headers: { ...corsHeaders, "Content-Type": "application/json" },
    });
  }
});
