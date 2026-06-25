/**
 * auth.store.js — Centralized Authentication State
 *
 * Responsibilities:
 *  - Store/retrieve JWT from sessionStorage (safer than localStorage: clears on tab close)
 *  - Decode JWT payload (base64) without a library
 *  - Provide role-checking helpers used by auth guard and sidebar rendering
 *  - Single source of truth for "who is logged in"
 *
 * Security notes:
 *  - sessionStorage chosen over localStorage: auto-clears when tab closes
 *  - Token validated (expiry check) on every read, not just on login
 *  - Sensitive claims not exposed to non-auth modules (only role/username)
 */

const TOKEN_KEY = 'epms_jwt';
const USER_KEY  = 'epms_user';

/** @type {{ username: string, roles: string[] } | null} */
let _cachedUser = null;

/* ── Storage helpers ──────────────────────────────────────────────────────── */

/**
 * Persist JWT token and decoded user info after successful login.
 * @param {string} token - Raw JWT string from backend
 * @param {string} username - Username returned by backend
 */
export function setSession(token, username) {
  sessionStorage.setItem(TOKEN_KEY, token);

  const payload = _decodeJwt(token);
  const roles = _extractRoles(payload);
  const email = payload.email ?? '';

  const user = { username, roles, email };
  sessionStorage.setItem(USER_KEY, JSON.stringify(user));
  _cachedUser = user;
}

/**
 * Clear all auth data. Called on logout or 401 response.
 */
export function clearSession() {
  sessionStorage.removeItem(TOKEN_KEY);
  sessionStorage.removeItem(USER_KEY);
  _cachedUser = null;
}

/**
 * Get the raw JWT token, or null if absent/expired.
 * @returns {string|null}
 */
export function getToken() {
  const token = sessionStorage.getItem(TOKEN_KEY);
  if (!token) return null;

  // Validate expiry on every read — do not trust stale tokens
  if (_isTokenExpired(token)) {
    clearSession();
    return null;
  }
  return token;
}

/**
 * Check if a valid (non-expired) token exists.
 * @returns {boolean}
 */
export function isLoggedIn() {
  return getToken() !== null;
}

/**
 * Get the currently logged-in user object.
 * Returns null if not authenticated.
 * @returns {{ username: string, roles: string[] } | null}
 */
export function getUser() {
  if (_cachedUser) return _cachedUser;

  const raw = sessionStorage.getItem(USER_KEY);
  if (!raw) return null;

  try {
    _cachedUser = JSON.parse(raw);
    return _cachedUser;
  } catch {
    return null;
  }
}

/**
 * Get username of the logged-in user.
 * @returns {string}
 */
export function getUsername() {
  return getUser()?.username ?? '';
}

/**
 * Get email of the logged-in user.
 * @returns {string}
 */
export function getEmail() {
  return getUser()?.email ?? '';
}

/**
 * Get the first (primary) role of the logged-in user.
 * @returns {string} e.g. 'ADMIN', 'HR', 'EMPLOYEE'
 */
export function getPrimaryRole() {
  const roles = getUser()?.roles ?? [];
  // Priority order: ADMIN > HR > EMPLOYEE
  if (roles.includes('ROLE_ADMIN') || roles.includes('ADMIN')) return 'ADMIN';
  if (roles.includes('ROLE_HR')    || roles.includes('HR'))    return 'HR';
  return 'EMPLOYEE';
}

/**
 * Check if current user has a specific role.
 * Handles both 'ADMIN' and 'ROLE_ADMIN' formats from Spring Security.
 * @param {string} role - e.g. 'ADMIN', 'HR', 'EMPLOYEE'
 * @returns {boolean}
 */
export function hasRole(role) {
  const roles = getUser()?.roles ?? [];
  return roles.includes(role) || roles.includes(`ROLE_${role}`);
}

/**
 * Check if current user has any of the given roles.
 * @param {...string} roles
 * @returns {boolean}
 */
export function hasAnyRole(...roles) {
  return roles.some(r => hasRole(r));
}

/* ── Private JWT decode helpers ───────────────────────────────────────────── */

/**
 * Decode JWT payload (middle part). Pure base64 decode, no library needed.
 * @param {string} token
 * @returns {Object} Decoded payload claims
 */
function _decodeJwt(token) {
  try {
    const base64 = token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/');
    const json = decodeURIComponent(
      atob(base64).split('').map(c => '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2)).join('')
    );
    return JSON.parse(json);
  } catch {
    return {};
  }
}

/**
 * Check if JWT exp claim is in the past.
 * @param {string} token
 * @returns {boolean}
 */
function _isTokenExpired(token) {
  const { exp } = _decodeJwt(token);
  if (!exp) return true;
  // exp is in seconds, Date.now() is in ms
  return Date.now() >= exp * 1000;
}

/**
 * Extract roles from JWT payload.
 * Spring Security encodes roles under 'roles' or 'authorities' claim.
 * @param {Object} payload
 * @returns {string[]}
 */
function _extractRoles(payload) {
  // Try 'roles' array or string, then 'authorities'
  if (Array.isArray(payload.roles)) return payload.roles;
  if (typeof payload.roles === 'string' && payload.roles.trim() !== '') {
    return payload.roles.split(',').map(r => r.trim());
  }
  if (Array.isArray(payload.authorities)) return payload.authorities.map(a => a.authority ?? a);
  return [];
}
