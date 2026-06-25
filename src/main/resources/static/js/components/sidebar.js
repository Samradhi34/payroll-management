/**
 * sidebar.js — Role-Aware Sidebar Navigation Component
 *
 * Renders the sidebar dynamically based on the logged-in user's role.
 * Handles collapse/expand toggle, mobile overlay, active link state,
 * user info display, and logout.
 *
 * NOTE: Does NOT import router.js to avoid circular dependency.
 *       Navigation uses window.location.hash directly.
 *       Sidebar state updates via the 'epms:route-changed' CustomEvent.
 */

import { getUser, getPrimaryRole, clearSession } from '../auth/auth.store.js';

/* ── Navigation items with role guards ────────────────────────────────────── */
const NAV_ITEMS = [
  {
    section: 'MAIN',
    items: [
      {
        path: '/dashboard',
        label: 'Dashboard',
        roles: ['ADMIN', 'HR', 'EMPLOYEE'],
        icon: `<svg fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2"><rect x="3" y="3" width="7" height="7" rx="1"/><rect x="14" y="3" width="7" height="7" rx="1"/><rect x="3" y="14" width="7" height="7" rx="1"/><rect x="14" y="14" width="7" height="7" rx="1"/></svg>`,
      },
    ],
  },
  {
    section: 'MANAGEMENT',
    items: [
      {
        path: '/employees',
        label: 'Employees',
        roles: ['ADMIN', 'HR'],
        icon: `<svg fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2"><path stroke-linecap="round" stroke-linejoin="round" d="M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0z"/></svg>`,
      },
      {
        path: '/departments',
        label: 'Departments',
        roles: ['ADMIN', 'HR'],
        icon: `<svg fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2"><path stroke-linecap="round" stroke-linejoin="round" d="M19 21V5a2 2 0 00-2-2H7a2 2 0 00-2 2v16m14 0h2m-2 0h-5m-9 0H3m2 0h5M9 7h1m-1 4h1m4-4h1m-1 4h1m-5 10v-5a1 1 0 011-1h2a1 1 0 011 1v5m-4 0h4"/></svg>`,
      },
      {
        path: '/attendance',
        label: 'Attendance',
        roles: ['ADMIN', 'HR', 'EMPLOYEE'],
        icon: `<svg fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2"><path stroke-linecap="round" stroke-linejoin="round" d="M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z"/></svg>`,
      },
    ],
  },
  {
    section: 'PAYROLL',
    items: [
      {
        path: '/payroll',
        label: 'Payroll',
        roles: ['ADMIN', 'HR'],
        icon: `<svg fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2"><path stroke-linecap="round" stroke-linejoin="round" d="M9 14l6-6m-5.5.5h.01m4.99 5h.01M19 21V5a2 2 0 00-2-2H7a2 2 0 00-2 2v16l3.5-2 3.5 2 3.5-2 3.5 2z"/></svg>`,
      },
      {
        path: '/my-payroll',
        label: 'My Payroll',
        roles: ['EMPLOYEE'],
        icon: `<svg fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2"><path stroke-linecap="round" stroke-linejoin="round" d="M17 9V7a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2m2 4h10a2 2 0 002-2v-6a2 2 0 00-2-2H9a2 2 0 00-2 2v6a2 2 0 002 2z"/></svg>`,
      },
    ],
  },
];

/**
 * Initialize and render the sidebar.
 * Safe to call multiple times — always replaces inner HTML.
 */
export function initSidebar() {
  const sidebar = document.getElementById('sidebar');
  if (!sidebar) return;

  sidebar.innerHTML = `
    <div class="sidebar-logo">
      <div class="sidebar-logo-icon">
        <svg fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
          <path stroke-linecap="round" stroke-linejoin="round"
            d="M12 8c-1.657 0-3 .895-3 2s1.343 2 3 2 3 .895 3 2-1.343 2-3 2m0-8c1.11 0 2.08.402 2.599 1M12 8V7m0 1v8m0 0v1m0-1c-1.11 0-2.08-.402-2.599-1M21 12a9 9 0 11-18 0 9 9 0 0118 0z"/>
        </svg>
      </div>
      <span class="sidebar-logo-text">EMS<span>Pro</span></span>
    </div>
    <nav class="sidebar-nav" id="sidebar-nav" aria-label="Main navigation"></nav>
    <div class="sidebar-footer" id="sidebar-footer"></div>
  `;

  _renderNav();
  _renderFooter();

  // Listen for route changes to update active state + breadcrumb
  // Remove old listener first to avoid duplicate handlers on re-init
  window.removeEventListener('epms:route-changed', _onRouteChanged);
  window.addEventListener('epms:route-changed', _onRouteChanged);
}

/** Handle route-changed event from router */
function _onRouteChanged(e) {
  const { path, label } = e.detail ?? {};
  _updateSidebarActive(path);
  _updateBreadcrumb(label);
}

/** Render navigation items filtered by current user role */
function _renderNav() {
  const nav  = document.getElementById('sidebar-nav');
  const role = getPrimaryRole();
  if (!nav) return;

  nav.innerHTML = NAV_ITEMS.map(section => {
    const visibleItems = section.items.filter(item =>
      !item.roles || item.roles.includes(role)
    );
    if (!visibleItems.length) return '';

    return `
      <div class="nav-section">
        <div class="nav-section-title">${section.section}</div>
        ${visibleItems.map(item => `
          <a
            class="nav-item"
            href="#${item.path}"
            data-path="${item.path}"
            title="${item.label}"
            aria-label="${item.label}"
          >
            <span class="nav-icon">${item.icon}</span>
            <span class="nav-label">${item.label}</span>
          </a>
        `).join('')}
      </div>
    `;
  }).join('');
}

/** Render user info + logout button in sidebar footer */
function _renderFooter() {
  const footer = document.getElementById('sidebar-footer');
  const user   = getUser();
  if (!footer || !user) return;

  const role    = getPrimaryRole();
  const initial = (user.username ?? 'U').charAt(0).toUpperCase();

  footer.innerHTML = `
    <div class="sidebar-user">
      <div class="sidebar-avatar" aria-hidden="true"></div>
      <div class="sidebar-user-info">
        <div class="sidebar-username"></div>
        <div class="sidebar-role"></div>
      </div>
      <button class="sidebar-logout-btn" id="logout-btn" title="Log out" aria-label="Log out">
        <svg fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
          <path stroke-linecap="round" stroke-linejoin="round"
            d="M17 16l4-4m0 0l-4-4m4 4H7m6 4v1a3 3 0 01-3 3H6a3 3 0 01-3-3V7a3 3 0 013-3h4a3 3 0 013 3v1"/>
        </svg>
      </button>
    </div>
  `;

  // Safe text injection — never innerHTML with user data
  footer.querySelector('.sidebar-avatar').textContent    = initial;
  footer.querySelector('.sidebar-username').textContent  = user.username;
  footer.querySelector('.sidebar-role').textContent      = role;

  document.getElementById('logout-btn')?.addEventListener('click', _handleLogout);
}

/** Set the active nav item based on current path */
function _updateSidebarActive(path) {
  document.querySelectorAll('.nav-item[data-path]').forEach(el => {
    const btnPath = el.getAttribute('data-path');
    const isActive = path === btnPath || path.startsWith(btnPath + '/');
    el.classList.toggle('active', isActive);
    el.setAttribute('aria-current', isActive ? 'page' : 'false');
  });
}

/** Update the header breadcrumb label */
function _updateBreadcrumb(label) {
  const el = document.getElementById('breadcrumb-page');
  if (el) el.textContent = label ?? '';
}

/** Initialize the sidebar toggle (hamburger) button */
export function initSidebarToggle() {
  const toggleBtn = document.getElementById('sidebar-toggle');
  const sidebar   = document.getElementById('sidebar');
  const main      = document.getElementById('main');
  const overlay   = document.getElementById('sidebar-overlay');

  if (!toggleBtn || !sidebar) return;

  // Remove old listener before adding to prevent duplicate handlers
  toggleBtn.replaceWith(toggleBtn.cloneNode(true));
  const freshToggle = document.getElementById('sidebar-toggle');

  freshToggle?.addEventListener('click', () => {
    const isMobile = window.innerWidth <= 1024;
    if (isMobile) {
      sidebar.classList.toggle('mobile-open');
      if (overlay) overlay.style.display = sidebar.classList.contains('mobile-open') ? 'block' : 'none';
    } else {
      sidebar.classList.toggle('collapsed');
      main?.classList.toggle('sidebar-collapsed');
    }
  });

  overlay?.addEventListener('click', () => {
    sidebar.classList.remove('mobile-open');
    if (overlay) overlay.style.display = 'none';
  });
}

/** Logout: clear session, navigate to login */
function _handleLogout() {
  clearSession();
  window.location.hash = '#/login';
  // Hide sidebar immediately — auth-mode will be set by router
  const sidebar = document.getElementById('sidebar');
  if (sidebar) sidebar.innerHTML = '';
}
