/**
 * router.js — Hash-Based SPA Router
 *
 * Routes are defined as { path, label, page, roles? }
 * The router:
 *  1. Parses location.hash on every hashchange
 *  2. Checks auth + roles via auth guard
 *  3. Toggles auth-shell vs app-shell
 *  4. Calls page.render(params) to inject content into #page-content
 *  5. Dispatches 'epms:route-changed' event — sidebar listens and updates itself
 *
 * NOTE: router.js does NOT import sidebar.js to avoid circular dependency.
 *       Communication happens via CustomEvents on window.
 */

import { isLoggedIn, hasAnyRole } from './auth/auth.store.js';
import { showToast } from './components/toast.js';

/** @type {Array<{path:string, label:string, page:Object, roles?:string[]}>} */
let _routes = [];

/**
 * Register all application routes.
 * @param {Array} routes
 */
export function registerRoutes(routes) {
  _routes = routes;
}

/**
 * Navigate to a route programmatically.
 * @param {string} path - e.g. '#/employees'
 */
export function navigate(path) {
  window.location.hash = path;
}

/**
 * Start the router — listen for hash changes and handle initial load.
 */
export function startRouter() {
  window.addEventListener('hashchange', _handleRoute);
  _handleRoute(); // handle the current URL on page load
}

/** Parse the current hash and resolve the matching route */
async function _handleRoute() {
  const hash     = window.location.hash || '#/login';
  const [rawPath] = hash.split('?');
  const path     = rawPath.replace(/^#/, '') || '/login';

  // ── Auth redirect guards ────────────────────────────────────────────────────
  if (!isLoggedIn() && path !== '/login' && path !== '/register') {
    navigate('#/login');
    return;
  }
  if (isLoggedIn() && (path === '/login' || path === '/register')) {
    navigate('#/dashboard');
    return;
  }

  // ── Route matching ──────────────────────────────────────────────────────────
  const match = _matchRoute(path);
  if (!match) {
    navigate(isLoggedIn() ? '#/dashboard' : '#/login');
    return;
  }

  const { route, params } = match;

  // ── Role guard ──────────────────────────────────────────────────────────────
  if (route.roles && !hasAnyRole(...route.roles)) {
    navigate('#/dashboard');
    return;
  }

  // ── Toggle auth-shell vs app-shell ──────────────────────────────────────────
  const isAuthRoute = path === '/login' || path === '/register';
  _toggleShell(isAuthRoute);

  // ── Notify sidebar + breadcrumb via event (avoids circular import) ──────────
  window.dispatchEvent(new CustomEvent('epms:route-changed', {
    detail: { path, label: route.label }
  }));

  // ── Clear previous page and render new one ──────────────────────────────────
  const content = document.getElementById('page-content');
  if (content) content.innerHTML = '';

  try {
    await route.page.render(params);
  } catch (err) {
    console.error('[Router] Page render error:', err);
    showToast('error', 'Page Render Error', err.message);
    const content = document.getElementById('page-content');
    if (content) {
      content.innerHTML = `
        <div style="padding: 2rem; max-width: 600px; margin: 2rem auto; background: #FEF2F2; border: 1px solid #FCA5A5; border-radius: 8px; color: #991B1B;">
          <h3 style="margin-top:0;">Failed to Render Page</h3>
          <p style="font-size:14px; font-weight:600;">${err.name}: ${err.message}</p>
          <pre style="background: #FFF; padding: 1rem; border-radius: 4px; overflow-x: auto; font-size: 12px; border: 1px solid #FEE2E2;">${err.stack || err}</pre>
        </div>
      `;
    }
  }
}

/** Match a path string against registered routes, extracting :param segments */
function _matchRoute(path) {
  for (const route of _routes) {
    const routeSegs = route.path.split('/');
    const pathSegs  = path.split('/');

    if (routeSegs.length !== pathSegs.length) continue;

    const params = {};
    let matched  = true;

    for (let i = 0; i < routeSegs.length; i++) {
      if (routeSegs[i].startsWith(':')) {
        params[routeSegs[i].slice(1)] = pathSegs[i];
      } else if (routeSegs[i] !== pathSegs[i]) {
        matched = false;
        break;
      }
    }

    if (matched) return { route, params };
  }
  return null;
}

/** Show/hide auth shell vs. app shell */
function _toggleShell(isAuth) {
  const app = document.getElementById('app');
  if (!app) return;
  if (isAuth) {
    app.classList.add('auth-mode');
  } else {
    app.classList.remove('auth-mode');
  }
}
