import { createClient } from "npm:@supabase/supabase-js@2";

export interface AuthorizedContext {
  userId: string;
  groupId: string;
  role: string;
}

export type AuthResult =
  | { success: true; context: AuthorizedContext }
  | {
      success: false;
      status: number;
      error: string;
      branch?: string;
      debug?: {
        userId?: string;
        role?: string | null;
        isActive?: boolean | null;
        schoolId?: string | null;
        groupId?: string;
      };
    };

export function isAuthorizedRole(role: string | null | undefined): boolean {
  if (!role) return false;
  const normalized = role.trim().toLowerCase();
  return [
    "officer_admin",
    "primary_officer_admin",
    "school_admin",
    "teacher",
    "student",
  ].includes(normalized);
}

export function isOfficerAdminRole(role: string | null | undefined): boolean {
  if (!role) return false;
  const normalized = role.trim().toLowerCase();
  return normalized === "officer_admin" || normalized === "primary_officer_admin";
}

export interface ProfileData {
  id?: string;
  role?: string | null;
  is_active?: boolean | null;
  school_id?: string | null;
  is_primary_admin?: boolean | null;
}

export interface GroupData {
  id?: string;
  is_active?: boolean | null;
}

export interface GroupMembershipData {
  id?: string;
  is_active?: boolean | null;
  role_in_group?: string | null;
}

export interface EvaluationResult {
  allowed: boolean;
  status?: number;
  error?: string;
  branch?: string;
  role?: string;
}

/**
 * Pure evaluation function for media authorization.
 *
 * Rules:
 * 1. Requires active profile (profile.is_active === true).
 * 2. Does NOT require school_id (school_id = null alone never causes a 403).
 * 3. Requires valid authorized role (officer_admin, primary_officer_admin, school_admin, teacher, student).
 * 4. Requires active target group (group.is_active !== false).
 * 5. Officer admin has administrative group access (unless membership row explicitly exists and is deactivated).
 * 6. Other roles require active membership in group_members.
 */
export function evaluateMediaAccess(
  profile: ProfileData | null | undefined,
  group: GroupData | null | undefined,
  membership: GroupMembershipData | null | undefined
): EvaluationResult {
  if (!profile) {
    return {
      allowed: false,
      status: 403,
      error: "Forbidden: user profile not found.",
      branch: "BRANCH_PROFILE_NOT_FOUND",
    };
  }

  if (profile.is_active !== true) {
    return {
      allowed: false,
      status: 403,
      error: "Forbidden: user profile is deactivated.",
      branch: "BRANCH_PROFILE_DEACTIVATED",
    };
  }

  const rawRole = (profile.role || "").trim().toLowerCase();
  if (!isAuthorizedRole(rawRole)) {
    return {
      allowed: false,
      status: 403,
      error: "Forbidden: unauthorized role.",
      branch: "BRANCH_ROLE_UNAUTHORIZED",
    };
  }

  if (!group) {
    return {
      allowed: false,
      status: 404,
      error: "Forbidden: target group not found.",
      branch: "BRANCH_TARGET_GROUP_NOT_FOUND",
    };
  }

  if (group.is_active === false) {
    return {
      allowed: false,
      status: 403,
      error: "Forbidden: target group is deactivated.",
      branch: "BRANCH_TARGET_GROUP_DEACTIVATED",
    };
  }

  const isOfficerAdmin = isOfficerAdminRole(rawRole);

  if (!isOfficerAdmin) {
    if (!membership) {
      return {
        allowed: false,
        status: 403,
        error: "Forbidden: authenticated user is not a member of this group.",
        branch: "BRANCH_MEMBERSHIP_NOT_FOUND",
      };
    }

    if (membership.is_active === false) {
      return {
        allowed: false,
        status: 403,
        error: "Forbidden: group membership has been deactivated.",
        branch: "BRANCH_MEMBERSHIP_DEACTIVATED",
      };
    }
  } else {
    // If an officer admin has an explicit group_members row, it must not be deactivated
    if (membership && membership.is_active === false) {
      return {
        allowed: false,
        status: 403,
        error: "Forbidden: group membership has been deactivated.",
        branch: "BRANCH_OFFICER_ADMIN_MEMBERSHIP_DEACTIVATED",
      };
    }
  }

  return {
    allowed: true,
    role: rawRole,
  };
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
    .select("id, is_active, role, school_id")
    .eq("id", userId)
    .maybeSingle();

  if (profileError) {
    return {
      success: false,
      status: 500,
      error: `Database error verifying user profile: ${profileError.message}`,
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

  // 4. Validate group membership: active member of the target group
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

  // 5. Evaluate media access using unified rule engine
  const evalResult = evaluateMediaAccess(profile, group, membership);
  if (!evalResult.allowed) {
    if (evalResult.status === 403) {
      console.error(
        `[AUTH_403_LOG] authenticated_user_id=${userId} profile.role=${profile?.role ?? "null"} profile.is_active=${profile?.is_active ?? "null"} profile.school_id=${profile?.school_id ?? "null"} groupId=${cleanGroupId} branch=${evalResult.branch ?? "UNKNOWN"} error="${evalResult.error}"`
      );
    }
    return {
      success: false,
      status: evalResult.status ?? 403,
      error: evalResult.error ?? "Forbidden",
      branch: evalResult.branch,
      debug: {
        userId,
        role: profile?.role,
        isActive: profile?.is_active,
        schoolId: profile?.school_id,
        groupId: cleanGroupId,
      },
    };
  }

  return {
    success: true,
    context: {
      userId,
      groupId: cleanGroupId,
      role: evalResult.role!,
    },
  };
}
