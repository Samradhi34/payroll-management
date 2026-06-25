/**
 * app.js — Application Entry Point
 *
 * Boot sequence:
 *  1. Build the HTML app shell into #app
 *  2. Initialize global services (toasts, loader)
 *  3. If already logged in → init sidebar + header
 *  4. Register routes
 *  5. Listen for epms:login event (fired by login.page.js after successful auth)
 *  6. Start the router
 */

import { initToasts }  from './components/toast.js';
import { initLoader }  from './components/loader.js';
import { initSidebar, initSidebarToggle } from './components/sidebar.js';
import { registerRoutes, startRouter }    from './router.js';
import { isLoggedIn, getUsername, getPrimaryRole } from './auth/auth.store.js';

/* ── Lazy page loaders ────────────────────────────────────────────────────── */
// Pages are imported only when first visited — reduces initial load time

function lazyPage(loader) {
  let module = null;
  return {
    async render(params) {
      if (!module) module = await loader();
      return module.render(params);
    },
  };
}

const pages = {
  login:       () => import('./pages/login.page.js'),
  dashboard:   () => import('./pages/dashboard.page.js'),
  employees:   () => import('./pages/employees.page.js'),
  departments: () => import('./pages/departments.page.js'),
  attendance:  () => import('./pages/attendance.page.js'),
  payroll:     () => import('./pages/payroll.page.js'),
};

/* ── Route definitions ────────────────────────────────────────────────────── */

const routes = [
  { path: '/login',       label: 'Login',       page: lazyPage(pages.login) },
  { path: '/register',    label: 'Register',    page: lazyPage(pages.login) },
  { path: '/dashboard',   label: 'Dashboard',   roles: ['ADMIN', 'HR', 'EMPLOYEE'], page: lazyPage(pages.dashboard) },
  { path: '/employees',   label: 'Employees',   roles: ['ADMIN', 'HR'],             page: lazyPage(pages.employees) },
  { path: '/departments', label: 'Departments', roles: ['ADMIN', 'HR'],             page: lazyPage(pages.departments) },
  { path: '/attendance',  label: 'Attendance',  roles: ['ADMIN', 'HR', 'EMPLOYEE'],  page: lazyPage(pages.attendance) },
  { path: '/payroll',     label: 'Payroll',     roles: ['ADMIN', 'HR'],             page: lazyPage(pages.payroll) },
  { path: '/my-payroll',  label: 'My Payroll',  roles: ['EMPLOYEE'],                page: lazyPage(pages.payroll) },
];

/* ── App Shell ────────────────────────────────────────────────────────────── */

function buildAppShell() {
  document.getElementById('app').innerHTML = `
    <!-- Auth shell: shown for login/register (no sidebar) -->
    <div id="auth-shell" role="main"></div>

    <!-- Mobile sidebar overlay -->
    <div id="sidebar-overlay" role="presentation" aria-hidden="true"></div>

    <!-- Sidebar navigation -->
    <aside id="sidebar" role="navigation" aria-label="Main navigation"></aside>

    <!-- Main content area -->
    <div id="main">
      <header id="header" role="banner">
        <div class="header-left">
          <button class="header-toggle" id="sidebar-toggle"
            aria-label="Toggle navigation" aria-controls="sidebar" aria-expanded="true">
            <svg fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
              <line x1="3" y1="6"  x2="21" y2="6"/>
              <line x1="3" y1="12" x2="21" y2="12"/>
              <line x1="3" y1="18" x2="21" y2="18"/>
            </svg>
          </button>
          <nav class="breadcrumb" aria-label="Breadcrumb">
            <span class="breadcrumb-item">EMSPro</span>
            <span class="breadcrumb-sep" aria-hidden="true">›</span>
            <span class="breadcrumb-item active" id="breadcrumb-page" aria-current="page"></span>
          </nav>
        </div>
        <div class="header-right" id="header-right"></div>
      </header>

      <!-- Dynamic page content injected here by the router -->
      <main id="page-content" role="main" aria-live="polite" aria-label="Page content"></main>
    </div>
  `;
}

/* ── Header user info ─────────────────────────────────────────────────────── */

function renderHeaderUser() {
  const container = document.getElementById('header-right');
  if (!container) return;

  container.innerHTML = ''; // clear previous

  if (!isLoggedIn()) return;

  const username = getUsername();
  const role     = getPrimaryRole();
  const initial  = username.charAt(0).toUpperCase();

  // Safe DOM construction — no innerHTML with user data
  const wrapper = document.createElement('div');
  wrapper.className = 'header-user';

  const avatar = document.createElement('div');
  avatar.className   = 'header-avatar';
  avatar.setAttribute('aria-hidden', 'true');
  avatar.textContent = initial;

  const name = document.createElement('span');
  name.className   = 'header-username';
  name.textContent = username;

  const badge = document.createElement('span');
  badge.className   = 'header-role-badge';
  badge.textContent = role;

  wrapper.appendChild(avatar);
  wrapper.appendChild(name);
  wrapper.appendChild(badge);
  container.appendChild(wrapper);
}

/* ── Init sidebar + header (called on login and on page refresh if logged in) */

function initAppChrome() {
  initSidebar();
  renderHeaderUser();
  initSidebarToggle();
}

/* ── Bootstrap ────────────────────────────────────────────────────────────── */

function bootstrap() {
  // 1. Build the DOM shell
  buildAppShell();

  // 2. Global services
  initToasts();
  initLoader();

  // 3. If session exists (e.g. page refresh), init chrome immediately
  if (isLoggedIn()) {
    initAppChrome();
  }

  // 4. Listen for successful login — init sidebar + header before routing
  window.addEventListener('epms:login', () => {
    initAppChrome();
  });

  // 5. Register routes
  registerRoutes(routes);

  // 6. Start the router (handles current URL)
  startRouter();
}

// Run when DOM is ready
if (document.readyState === 'loading') {
  document.addEventListener('DOMContentLoaded', bootstrap);
} else {
  bootstrap();
}
