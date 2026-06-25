/**
 * modal.js — Reusable Modal System
 *
 * Provides:
 *  openModal(id)       — show a pre-built modal
 *  closeModal(id)      — hide it
 *  openConfirm({...})  — programmatic confirm dialog (returns Promise<boolean>)
 *  closeAllModals()    — close every open modal
 *
 * Accessibility:
 *  - Focus trapped inside modal when open
 *  - ESC key closes active modal
 *  - Returns focus to trigger element on close
 */

let _triggerEl = null; // element that opened modal (for focus return)

/**
 * Open a modal by its backdrop ID.
 * @param {string} id - the ID of the .modal-backdrop element
 */
export function openModal(id) {
  const backdrop = document.getElementById(id);
  if (!backdrop) return;

  _triggerEl = document.activeElement;
  backdrop.classList.add('open');
  backdrop.removeAttribute('aria-hidden');

  // Trap focus on first focusable element
  const focusable = backdrop.querySelectorAll(
    'button, [href], input, select, textarea, [tabindex]:not([tabindex="-1"])'
  );
  if (focusable.length) focusable[0].focus();

  // Close on backdrop click (outside modal box)
  backdrop.addEventListener('click', _onBackdropClick);
}

/**
 * Close a modal by its backdrop ID.
 * @param {string} id
 */
export function closeModal(id) {
  const backdrop = document.getElementById(id);
  if (!backdrop) return;

  backdrop.classList.remove('open');
  backdrop.setAttribute('aria-hidden', 'true');
  backdrop.removeEventListener('click', _onBackdropClick);

  // Return focus to the element that opened the modal
  if (_triggerEl) { _triggerEl.focus(); _triggerEl = null; }
}

/** Close all open modals */
export function closeAllModals() {
  document.querySelectorAll('.modal-backdrop.open').forEach(el => {
    el.classList.remove('open');
    el.setAttribute('aria-hidden', 'true');
  });
}

/** Close if user clicked the backdrop (not the modal box itself) */
function _onBackdropClick(e) {
  if (e.target === e.currentTarget) {
    closeModal(e.currentTarget.id);
  }
}

/* ── ESC key handler (global) ─────────────────────────────────────────────── */
document.addEventListener('keydown', (e) => {
  if (e.key !== 'Escape') return;
  const open = document.querySelector('.modal-backdrop.open');
  if (open) closeModal(open.id);
});

/**
 * Open a programmatic confirmation dialog.
 * Returns a Promise that resolves to true (confirmed) or false (cancelled).
 *
 * @param {Object} options
 * @param {string} options.title
 * @param {string} options.message
 * @param {'danger'|'warning'} [options.type='danger']
 * @param {string} [options.confirmLabel='Confirm']
 * @param {string} [options.cancelLabel='Cancel']
 * @returns {Promise<boolean>}
 */
export function openConfirm({ title, message, type = 'danger', confirmLabel = 'Confirm', cancelLabel = 'Cancel' }) {
  return new Promise((resolve) => {
    const MODAL_ID = 'confirm-modal';

    // Remove existing confirm modal if present
    document.getElementById(MODAL_ID)?.remove();

    const confirmIcon = type === 'danger'
      ? `<svg fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2"><path stroke-linecap="round" stroke-linejoin="round" d="M12 9v2m0 4h.01M10.29 3.86L1.82 18a2 2 0 001.71 3h16.94a2 2 0 001.71-3L13.71 3.86a2 2 0 00-3.42 0z"/></svg>`
      : `<svg fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2"><path stroke-linecap="round" stroke-linejoin="round" d="M8.228 9c.549-1.165 2.03-2 3.772-2 2.21 0 4 1.343 4 3 0 1.4-1.278 2.575-3.006 2.907-.542.104-.994.54-.994 1.093m0 3h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"/></svg>`;

    const backdrop = document.createElement('div');
    backdrop.id = MODAL_ID;
    backdrop.className = 'modal-backdrop';
    backdrop.setAttribute('role', 'dialog');
    backdrop.setAttribute('aria-modal', 'true');
    backdrop.setAttribute('aria-labelledby', 'confirm-title');
    backdrop.innerHTML = `
      <div class="modal modal-sm">
        <div class="modal-body" style="text-align:center; padding: 2rem 2rem 1.5rem;">
          <div class="modal-confirm-icon ${type}">${confirmIcon}</div>
          <h3 id="confirm-title" style="margin-bottom:.5rem;"></h3>
          <p class="modal-confirm-text"></p>
        </div>
        <div class="modal-footer" style="justify-content:center;">
          <button id="confirm-cancel-btn" class="btn btn-ghost">${cancelLabel}</button>
          <button id="confirm-ok-btn" class="btn btn-${type === 'danger' ? 'danger' : 'warning'}">${confirmLabel}</button>
        </div>
      </div>
    `;

    // Safe text injection
    backdrop.querySelector('#confirm-title').textContent = title;
    backdrop.querySelector('.modal-confirm-text').textContent = message;

    document.body.appendChild(backdrop);

    const cleanup = (result) => {
      closeModal(MODAL_ID);
      setTimeout(() => backdrop.remove(), 300);
      resolve(result);
    };

    backdrop.querySelector('#confirm-ok-btn').addEventListener('click',    () => cleanup(true));
    backdrop.querySelector('#confirm-cancel-btn').addEventListener('click', () => cleanup(false));

    openModal(MODAL_ID);
  });
}
