/**
 * toast.js — Global Toast Notification System
 *
 * Provides show/hide for non-blocking user feedback messages.
 * 4 types: success, error, warning, info
 * Auto-dismisses after configurable duration.
 * Accessible: uses role="alert" and aria-live="assertive".
 */

/* ── SVG icons (inline, no external dependency) ───────────────────────────── */
const ICONS = {
  success: `<svg fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2"><path stroke-linecap="round" stroke-linejoin="round" d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z"/></svg>`,
  error:   `<svg fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2"><path stroke-linecap="round" stroke-linejoin="round" d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"/></svg>`,
  warning: `<svg fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2"><path stroke-linecap="round" stroke-linejoin="round" d="M12 9v2m0 4h.01M10.29 3.86L1.82 18a2 2 0 001.71 3h16.94a2 2 0 001.71-3L13.71 3.86a2 2 0 00-3.42 0z"/></svg>`,
  info:    `<svg fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2"><path stroke-linecap="round" stroke-linejoin="round" d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"/></svg>`,
};

const CLOSE_ICON = `<svg fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2"><path stroke-linecap="round" stroke-linejoin="round" d="M6 18L18 6M6 6l12 12"/></svg>`;

const DURATION = { success: 3500, info: 4000, warning: 5000, error: 6000 };

/**
 * Initialize the toast container in the DOM.
 * Call once on app startup.
 */
export function initToasts() {
  if (document.getElementById('toast-container')) return;
  const container = document.createElement('div');
  container.id = 'toast-container';
  container.setAttribute('aria-live', 'assertive');
  container.setAttribute('aria-atomic', 'false');
  container.setAttribute('role', 'region');
  container.setAttribute('aria-label', 'Notifications');
  document.body.appendChild(container);
}

/**
 * Show a toast notification.
 *
 * @param {'success'|'error'|'warning'|'info'} type
 * @param {string} title   - Bold heading line
 * @param {string} message - Descriptive body text
 * @param {number} [duration] - Auto-dismiss ms (0 = manual only)
 */
export function showToast(type, title, message = '', duration) {
  const container = document.getElementById('toast-container');
  if (!container) return;

  const ms = duration ?? DURATION[type] ?? 4000;

  const toast = document.createElement('div');
  toast.className = `toast toast-${type}`;
  toast.setAttribute('role', 'alert');
  toast.innerHTML = `
    <span class="toast-icon">${ICONS[type] ?? ICONS.info}</span>
    <div class="toast-body">
      <div class="toast-title"></div>
      <div class="toast-message"></div>
    </div>
    <button class="toast-close" aria-label="Dismiss notification">${CLOSE_ICON}</button>
  `;

  // Use textContent to prevent XSS — never innerHTML for user data
  toast.querySelector('.toast-title').textContent   = title;
  toast.querySelector('.toast-message').textContent = message;

  toast.querySelector('.toast-close').addEventListener('click', () => _remove(toast));

  container.appendChild(toast);

  if (ms > 0) {
    setTimeout(() => _remove(toast), ms);
  }
}

/** Animate then remove toast element */
function _remove(toast) {
  toast.classList.add('removing');
  toast.addEventListener('animationend', () => toast.remove(), { once: true });
  // Fallback if animationend doesn't fire
  setTimeout(() => toast.remove(), 400);
}
