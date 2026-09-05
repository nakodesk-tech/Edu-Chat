import { createClient } from "npm:@supabase/supabase-js@2";

export interface AuthorizedContext {
  userId: string;
  groupId: string;
  role: string;
}

export type AuthResult =
  | { success: true; context: AuthorizedContext }
  | { success: false; status: number; error: string };

export function isAuthorizedRole(role: string | null | undefined): boolean {
  if (!role) return false;
  const normalized = role.trim().toLowerCase();
  return ["officer_admin", "school_admin", "teacher", "student"].includes(normalized);
}

export function isOfficerAdminRole(role: string | null | undefined): boolean {
  if (!role) return false;
  const normalized = role.trim().toLowerCase();
  return normalized === "officer_admin";
}

/**
 * Extracts the Bearer token from the HTTP Authorization header.
 */
export function getBearerToken(req: Request): string | null {
  const authHeader = req.headers.get("Authorization") || req.headers.get("authorization");
  if (!authHeader) return null;
  const match = authHeader.match(/^Bearer\s+(.+)$/i);
  return match ? match[1].trim() : null;
}

/**
 * Authorizes a user for group media operations:
 * 1. Validates authenticated user session from Supabase Auth token.
 * 2. Validates active user profile in `profiles` (does not require school_id, does not whitelist roles).
 * 3. Validates active target group in `groups`.
 * 4. Validates active membership in `group_members` for the target group.
 */
export async function authorizeGroupAccess(
  req: Request,
  targetGroupId: string
): Promise<AuthResult> {
  const token = getBearerToken(req);
  if (!token) {
    return {
      success: false,
      status: 401,
      error: "Missing or invalid authorization header. Bearer token required.",
    };
  }

  const supabaseUrl = Deno.env.get("SUPABASE_URL") || "";
  const supabaseAnonKey = Deno.env.get("SUPABASE_ANON_KEY") || "";
  const supabaseServiceRoleKey =
    Deno.env.get("SUPABASE_SERVICE_ROLE_KEY") || supabaseAnonKey;

  if (!supabaseUrl || !supabaseAnonKey) {
    return {
      success: false,
      status: 500,
      error: "Server configuration error: missing Supabase credentials.",
    };
  }

  // 1. Authenticate user from session token
  const userClient = createClient(supabaseUrl, supabaseAnonKey, {
    global: { headers: { Authorization: `Bearer ${token}` } },
  });

  const {
    data: { user },
    error: authError,
  } = await userClient.auth.getUser();

  if (authError || !user) {
    return {
      success: false,
      status: 401,
      error: "Unauthorized: invalid or expired session token.",
    };
  }

  const userId = user.id;

  if (!targetGroupId || typeof targetGroupId !== "string" || targetGroupId.trim() === "") {
    return {
      success: false,
      status: 400,
      error: "Bad Request: target groupId is required.",
    };
  }
  const cleanGroupId = targetGroupId.trim();

  // Admin client to verify database state
  const adminClient = createClient(supabaseUrl, supabaseServiceRoleKey);

  // 2. Validate user profile: active profile (school_id is NOT required, roles are NOT whitelisted)
  const { data: profile, error: profileError } = await adminClient
    .from("profiles")
    .select("id, is_active, role")
    .eq("id", userId)
    .maybeSingle();

  if (profileError) {
    return {
      success: false,
      status: 500,
      error: `Database error verifying user profile: ${profileError.message}`,
    };
  }

  if (!profile) {
    return {
      success: false,
      status: 403,
      error: "Forbidden: user profile not found.",
    };
  }

  if (profile.is_active === false) {
    return {
      success: false,
      status: 403,
      error: "Forbidden: user profile is deactivated.",
    };
  }

  const rawRole = (profile.role || "").trim().toLowerCase();
  if (!isAuthorizedRole(rawRole)) {
    return {
      success: false,
      status: 403,
      error: "Forbidden: unauthorized role.",
    };
  }

  // 3. Validate target group: active group
  const { data: group, error: groupError } = await adminClient
    .from("groups")
    .select("id, is_active")
    .eq("id", cleanGroupId)
    .maybeSingle();

  if (groupError) {
    return {
      success: false,
      status: 500,
      error: `Database error verifying group: ${groupError.message}`,
    };
  }

  if (!group) {
    return {
      success: false,
      status: 404,
      error: "Forbidden: target group not found.",
    };
  }

  if (group.is_active === false) {
    return {
      success: false,
      status: 403,
      error: "Forbidden: target group is deactivated.",
    };
  }

  // 4. Validate group membership: active member of the target group
  // Officer Admin has administrative group access or group membership.
  // Other roles must be verified active members in `group_members`.
  const isOfficerAdmin = isOfficerAdminRole(rawRole);

  const { data: membership, error: memberError } = await adminClient
    .from("group_members")
    .select("id, is_active, role_in_group")
    .eq("group_id", cleanGroupId)
    .eq("user_id", userId)
    .maybeSingle();

  if (memberError) {
    return {
      success: false,
      status: 500,
      error: `Database error verifying group membership: ${memberError.message}`,
    };
  }

  if (!isOfficerAdmin) {
    if (!membership) {
      return {
        success: false,
        status: 403,
        error: "Forbidden: authenticated user is not a member of this group.",
      };
    }

    if (membership.is_active === false) {
      return {
        success: false,
        status: 403,
        error: "Forbidden: group membership has been deactivated.",
      };
    }
  } else {
    // If an officer admin has an explicit group_members row, it must not be deactivated
    if (membership && membership.is_active === false) {
      return {
        success: false,
        status: 403,
        error: "Forbidden: group membership has been deactivated.",
      };
    }
  }

  return {
    success: true,
    context: {
      userId,
      groupId: cleanGroupId,
      role: rawRole,
    },
  };
}
