/**
 * loader.js — Global Page Loader & Inline Skeleton
 *
 * showLoader() / hideLoader() are called by the HTTP client automatically.
 * Components can also call them directly for custom loading states.
 */

let _loaderCount = 0; // ref-count so concurrent requests don't fight

/**
 * Show the full-page loading overlay.
 * Uses a ref-counter so multiple concurrent requests don't flash.
 */
export function showLoader() {
  _loaderCount++;
  const el = document.getElementById('global-loader');
  if (el) el.classList.add('active');
}

/**
 * Hide the full-page loading overlay.
 * Only hides when all concurrent requests have completed.
 */
export function hideLoader() {
  _loaderCount = Math.max(0, _loaderCount - 1);
  if (_loaderCount === 0) {
    const el = document.getElementById('global-loader');
    if (el) el.classList.remove('active');
  }
}

/**
 * Show a skeleton loader inside a container element.
 * Replaces content with animated skeleton rows.
 *
 * @param {HTMLElement} container
 * @param {number} rows - number of skeleton rows to show
 */
export function showSkeleton(container, rows = 5) {
  container.innerHTML = Array.from({ length: rows }, (_, i) => `
    <tr>
      <td colspan="99">
        <div class="skeleton" style="height:18px; width:${60 + (i % 3) * 15}%; border-radius:4px;"></div>
      </td>
    </tr>
  `).join('');
}

/**
 * Initialize the global loader element (called once at app startup).
 */
export function initLoader() {
  if (document.getElementById('global-loader')) return;
  const loader = document.createElement('div');
  loader.id = 'global-loader';
  loader.setAttribute('aria-label', 'Loading');
  loader.setAttribute('role', 'status');
  loader.innerHTML = `
    <div class="spinner" aria-hidden="true"></div>
    <span class="loading-text">Loading…</span>
  `;
  document.body.appendChild(loader);
}
