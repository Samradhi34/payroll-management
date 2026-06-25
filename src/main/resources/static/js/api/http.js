/**
 * http.js — Centralized HTTP Fetch Wrapper
 *
 * Responsibilities:
 *  - Inject Authorization: Bearer <token> on every request automatically
 *  - Normalize all API responses into a consistent { ok, data, message, status } shape
 *  - Handle 401 (auto-logout + redirect), 403 (show forbidden toast)
 *  - Show/hide global loader for non-silent requests
 *  - Support JSON bodies and multipart FormData bodies
 *  - Provide typed shorthand: get(), post(), put(), patch(), del(), postForm()
 *
 * No business logic lives here — only transport and error normalization.
 */

import { getToken, clearSession } from '../auth/auth.store.js';
import { showToast } from '../components/toast.js';
import { showLoader, hideLoader } from '../components/loader.js';

const BASE_URL = 'http://localhost:8080';

/**
 * @typedef {Object} ApiResult
 * @property {boolean} ok       - true if HTTP 2xx
 * @property {number}  status   - HTTP status code
 * @property {string}  message  - backend message string
 * @property {*}       data     - response data payload (may be null)
 * @property {Object}  raw      - full backend JSON response
 */

/**
 * Core fetch wrapper.
 *
 * @param {string}  path    - API path e.g. '/auth/login'
 * @param {Object}  options - fetch options override
 * @param {boolean} silent  - if true, don't show global loader (for background fetches)
 * @returns {Promise<ApiResult>}
 */
async function request(path, options = {}, silent = false) {
  if (!silent) showLoader();

  const token = getToken();
  const headers = { ...(options.headers ?? {}) };

  // Inject auth token when present (skip for FormData — browser sets Content-Type with boundary)
  if (token) headers['Authorization'] = `Bearer ${token}`;
  if (!(options.body instanceof FormData)) {
    headers['Content-Type'] = 'application/json';
  }

  const config = { ...options, headers };

  let response;
  try {
    response = await fetch(`${BASE_URL}${path}`, config);
  } catch (networkError) {
    // Network-level failure (server down, CORS, DNS)
    console.error('[HTTP] Network error:', networkError);
    showToast('error', 'Network Error', 'Cannot reach the server. Please check your connection.');
    if (!silent) hideLoader();
    return { ok: false, status: 0, message: 'Network error', data: null, raw: {} };
  }

  if (!silent) hideLoader();

  // ── Parse response body ────────────────────────────────────────────────────
  let body = {};
  const contentType = response.headers.get('content-type') ?? '';
  if (contentType.includes('application/json')) {
    try { body = await response.json(); } catch { body = {}; }
  } else {
    // Non-JSON (e.g. plain text error from Spring)
    const text = await response.text().catch(() => '');
    body = { message: text || response.statusText };
  }

  const message = body.message ?? body.error ?? response.statusText ?? '';
  const data    = body.data ?? null;

  // ── Handle specific HTTP error statuses ────────────────────────────────────
  if (response.status === 401) {
    if (path.startsWith('/auth/')) {
      return { ok: false, status: 401, message, data: null, raw: body };
    }
    clearSession();
    showToast('error', 'Session Expired', 'Please log in again.');
    // Redirect to login after short delay so toast is visible
    setTimeout(() => { window.location.hash = '#/login'; }, 1200);
    return { ok: false, status: 401, message, data: null, raw: body };
  }

  if (response.status === 403) {
    showToast('error', 'Access Denied', 'You do not have permission to perform this action.');
    return { ok: false, status: 403, message, data: null, raw: body };
  }

  if (response.status === 422 || response.status === 409) {
    // Business-rule violations — caller handles these, no global toast
    return { ok: false, status: response.status, message, data: null, raw: body };
  }

  if (response.status >= 500) {
    showToast('error', 'Server Error', message || 'An unexpected server error occurred.');
    return { ok: false, status: response.status, message, data: null, raw: body };
  }

  return {
    ok:      response.ok,
    status:  response.status,
    message,
    data,
    raw:     body,
  };
}

/* ── Shorthand methods ────────────────────────────────────────────────────── */

/**
 * HTTP GET
 * @param {string} path - API path (can include query string)
 * @param {boolean} [silent=false]
 * @returns {Promise<ApiResult>}
 */
export function get(path, silent = false) {
  return request(path, { method: 'GET' }, silent);
}

/**
 * HTTP POST with JSON body
 * @param {string} path
 * @param {Object} body
 * @param {boolean} [silent=false]
 * @returns {Promise<ApiResult>}
 */
export function post(path, body = {}, silent = false) {
  return request(path, { method: 'POST', body: JSON.stringify(body) }, silent);
}

/**
 * HTTP PUT with JSON body
 * @param {string} path
 * @param {Object} body
 * @returns {Promise<ApiResult>}
 */
export function put(path, body = {}) {
  return request(path, { method: 'PUT', body: JSON.stringify(body) });
}

/**
 * HTTP PATCH with optional JSON body
 * @param {string} path
 * @param {Object} [body]
 * @returns {Promise<ApiResult>}
 */
export function patch(path, body) {
  const opts = { method: 'PATCH' };
  if (body !== undefined) opts.body = JSON.stringify(body);
  return request(path, opts);
}

/**
 * HTTP DELETE
 * @param {string} path
 * @returns {Promise<ApiResult>}
 */
export function del(path) {
  return request(path, { method: 'DELETE' });
}

/**
 * HTTP POST with multipart/form-data (file upload).
 * Content-Type is NOT set manually — browser handles boundary automatically.
 * @param {string}   path
 * @param {FormData} formData
 * @returns {Promise<ApiResult>}
 */
export function postForm(path, formData) {
  return request(path, { method: 'POST', body: formData });
}

/**
 * Build a query string from an object, skipping null/undefined/empty values.
 * Usage: buildQuery({ page: 1, size: 10, search: 'john', status: '' })
 *        → '?page=1&size=10&search=john'
 *
 * @param {Object} params
 * @returns {string} e.g. '?key=val&key2=val2' or '' if empty
 */
export function buildQuery(params) {
  const q = Object.entries(params)
    .filter(([, v]) => v !== null && v !== undefined && v !== '')
    .map(([k, v]) => `${encodeURIComponent(k)}=${encodeURIComponent(v)}`)
    .join('&');
  return q ? `?${q}` : '';
}
