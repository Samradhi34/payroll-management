/**
 * payroll.page.js — Payroll Management Page
 *
 * Features:
 *  - Paginated payroll list with status filter
 *  - Generate payroll modal (with employee + salary fields)
 *  - Approve / Mark as Paid / Cancel actions (with confirm dialogs)
 *  - Generate salary slip for approved/paid payrolls
 *  - Status badges matching backend PayrollStatus enum
 *  - Role-based action visibility (ADMIN-only approve/pay/cancel)
 */

import * as PayrollApi  from '../api/payroll.api.js';
import { getEmployees } from '../api/employee.api.js';
import { showToast }    from '../components/toast.js';
import { openModal, closeModal, openConfirm } from '../components/modal.js';
import { hasRole, hasAnyRole, getToken } from '../auth/auth.store.js';

const PAGE_SIZE = 10;
let _currentPage = 1;
let _filters     = {};

export async function render() {
  const content = document.getElementById('page-content');
  if (!content) return;

  const isAdmin = hasRole('ADMIN');
  const isHR    = hasRole('HR');

  content.innerHTML = `
    <div class="page-header">
      <div>
        <h1 class="page-title">Payroll Management</h1>
        <p class="page-subtitle">Generate, approve, and manage employee payrolls</p>
      </div>
      <div class="page-actions" style="display:flex; gap:10px;">
        ${(isAdmin || isHR) ? `
          <button class="btn btn-outline" id="generate-all-payroll-btn">
            <svg fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2" style="width:16px;height:16px;margin-right:6px;">
              <path stroke-linecap="round" stroke-linejoin="round" d="M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0z"/>
            </svg>
            Generate All
          </button>
          <button class="btn btn-primary" id="generate-payroll-btn">
            <svg fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2" style="width:16px;height:16px;">
              <path stroke-linecap="round" stroke-linejoin="round" d="M12 4v16m8-8H4"/>
            </svg>
            Generate Payroll
          </button>` : ''}
      </div>
    </div>

    <!-- Filter Bar -->
    <div class="card" style="margin-bottom:1.5rem;">
      <div class="card-body-sm">
        <div class="filter-bar">
          <div class="form-group">
            <label class="form-label">Month</label>
            <select class="form-control" id="filter-month">
              <option value="">All months</option>
              ${Array.from({length:12},(_,i)=>`<option value="${i+1}">${new Date(2000,i).toLocaleString('default',{month:'long'})}</option>`).join('')}
            </select>
          </div>
          <div class="form-group">
            <label class="form-label">Year</label>
            <select class="form-control" id="filter-year">
              <option value="">All years</option>
              ${Array.from({length:5},(_,i)=>`<option value="${new Date().getFullYear()-i}">${new Date().getFullYear()-i}</option>`).join('')}
            </select>
          </div>
          <div class="form-group">
            <label class="form-label">Status</label>
            <select class="form-control" id="filter-status">
              <option value="">All statuses</option>
              <option value="GENERATED">Generated</option>
              <option value="APPROVED">Approved</option>
              <option value="PAID">Paid</option>
              <option value="CANCELLED">Cancelled</option>
            </select>
          </div>
          <button class="btn btn-primary btn-sm" id="apply-filter-btn">Apply</button>
          <button class="btn btn-ghost btn-sm"   id="clear-filter-btn">Clear</button>
        </div>
      </div>
    </div>

    <!-- Table -->
    <div class="card">
      <div class="card-header">
        <h2>Payroll Records</h2>
        <span id="payroll-count" class="text-sm text-muted"></span>
      </div>
      <div class="table-container">
        <table class="table" id="payroll-table">
          <thead>
            <tr>
              <th>#</th>
              <th>Employee</th>
              <th>Period</th>
              <th>Base Salary</th>
              <th>Bonus</th>
              <th>Deductions</th>
              <th>Net Salary</th>
              <th>Status</th>
              <th>Generated</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody id="payroll-tbody">
            <tr><td colspan="10"><div class="table-empty"><p>Loading payrolls…</p></div></td></tr>
          </tbody>
        </table>
      </div>
      <div id="payroll-pagination"></div>
    </div>

    <!-- Generate Payroll Modal -->
    <div class="modal-backdrop" id="generate-payroll-modal" role="dialog" aria-modal="true" aria-labelledby="gp-modal-title">
      <div class="modal">
        <div class="modal-header">
          <h3 id="gp-modal-title">Generate Payroll</h3>
          <button class="modal-close" id="gp-close-btn" aria-label="Close modal">
            <svg fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2"><path stroke-linecap="round" stroke-linejoin="round" d="M6 18L18 6M6 6l12 12"/></svg>
          </button>
        </div>
        <div class="modal-body">
          <div id="gp-error" class="form-error" style="margin-bottom:1rem;font-size:13px;" role="alert"></div>
          <form id="generate-payroll-form" novalidate>
            <div class="form-group">
              <label class="form-label required" for="gp-employee">Employee</label>
              <select class="form-control" id="gp-employee" required aria-describedby="gp-employee-err">
                <option value="">Loading employees…</option>
              </select>
              <span class="form-error" id="gp-employee-err"></span>
            </div>
            <div class="form-row">
              <div class="form-group">
                <label class="form-label required" for="gp-month">Month</label>
                <select class="form-control" id="gp-month" required>
                  ${Array.from({length:12},(_,i)=>`<option value="${i+1}" ${i===new Date().getMonth()?'selected':''}>${new Date(2000,i).toLocaleString('default',{month:'long'})}</option>`).join('')}
                </select>
                <span class="form-error" id="gp-month-err"></span>
              </div>
              <div class="form-group">
                <label class="form-label required" for="gp-year">Year</label>
                <input type="number" class="form-control" id="gp-year"
                  value="${new Date().getFullYear()}" min="2000" max="2099" required/>
                <span class="form-error" id="gp-year-err"></span>
              </div>
            </div>
            <div class="form-row">
              <div class="form-group">
                <label class="form-label" for="gp-bonus">Bonus (₹)</label>
                <input type="number" class="form-control" id="gp-bonus"
                  placeholder="0.00" min="0" step="0.01"/>
              </div>
              <div class="form-group">
                <label class="form-label required" for="gp-deductions">Deductions (₹)</label>
                <input type="number" class="form-control" id="gp-deductions"
                  placeholder="0.00" min="0" step="0.01" required/>
                <span class="form-error" id="gp-deductions-err"></span>
              </div>
            </div>
          </form>
        </div>
        <div class="modal-footer">
          <button class="btn btn-ghost" id="gp-cancel-btn">Cancel</button>
          <button class="btn btn-primary" id="gp-submit-btn">Generate</button>
        </div>
      </div>
    </div>

    <!-- Generate All Payrolls Modal -->
    <div class="modal-backdrop" id="generate-all-modal" role="dialog" aria-modal="true" aria-labelledby="ga-modal-title">
      <div class="modal">
        <div class="modal-header">
          <h3 id="ga-modal-title">Generate All Payrolls</h3>
          <button class="modal-close" id="ga-close-btn" aria-label="Close modal">
            <svg fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2"><path stroke-linecap="round" stroke-linejoin="round" d="M6 18L18 6M6 6l12 12"/></svg>
          </button>
        </div>
        <div class="modal-body">
          <div id="ga-error" class="form-error" style="margin-bottom:1rem;font-size:13px;" role="alert"></div>
          <p class="text-sm text-muted" style="margin-bottom:1rem;">This will generate payroll for all active employees who do not have a payroll record for the selected period.</p>
          <form id="generate-all-form" novalidate>
            <div class="form-row">
              <div class="form-group">
                <label class="form-label required" for="ga-month">Month</label>
                <select class="form-control" id="ga-month" required>
                  ${Array.from({length:12},(_,i)=>`<option value="${i+1}" ${i===new Date().getMonth()?'selected':''}>${new Date(2000,i).toLocaleString('default',{month:'long'})}</option>`).join('')}
                </select>
                <span class="form-error" id="ga-month-err"></span>
              </div>
              <div class="form-group">
                <label class="form-label required" for="ga-year">Year</label>
                <input type="number" class="form-control" id="ga-year"
                  value="${new Date().getFullYear()}" min="2000" max="2099" required/>
                <span class="form-error" id="ga-year-err"></span>
              </div>
            </div>
            <div class="form-row">
              <div class="form-group">
                <label class="form-label" for="ga-bonus">Default Bonus (₹)</label>
                <input type="number" class="form-control" id="ga-bonus"
                  placeholder="0.00" min="0" step="0.01" value="0.00"/>
              </div>
              <div class="form-group">
                <label class="form-label required" for="ga-deductions">Default Deductions (₹)</label>
                <input type="number" class="form-control" id="ga-deductions"
                  placeholder="0.00" min="0" step="0.01" value="0.00" required/>
                <span class="form-error" id="ga-deductions-err"></span>
              </div>
            </div>
          </form>
        </div>
        <div class="modal-footer">
          <button class="btn btn-ghost" id="ga-cancel-btn">Cancel</button>
          <button class="btn btn-primary" id="ga-submit-btn">Generate For All</button>
        </div>
      </div>
    </div>

    <!-- Payroll Details Modal -->
    <div class="modal-backdrop" id="payroll-detail-modal" role="dialog" aria-modal="true" aria-labelledby="pd-modal-title">
      <div class="modal" style="max-width:600px;">
        <div class="modal-header">
          <h3 id="pd-modal-title">Payroll Details</h3>
          <button class="modal-close" id="pd-close-btn" aria-label="Close modal">
            <svg fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2"><path stroke-linecap="round" stroke-linejoin="round" d="M6 18L18 6M6 6l12 12"/></svg>
          </button>
        </div>
        <div class="modal-body" id="pd-modal-body" style="padding:1.5rem;">
          <!-- Dynamically populated -->
        </div>
        <div class="modal-footer">
          <button class="btn btn-primary" id="pd-close-bottom-btn">Close</button>
        </div>
      </div>
    </div>
  `;

  _bindEvents();
  await _loadPayrolls();
  if (isAdmin || isHR) await _loadEmployeesForModal();
}

/* ── Data loading ─────────────────────────────────────────────────────────── */

async function _loadPayrolls() {
  const tbody = document.getElementById('payroll-tbody');
  tbody.innerHTML = `<tr><td colspan="10"><div class="table-empty"><p>Loading…</p></div></td></tr>`;

  const params = {
    page: _currentPage,
    size: PAGE_SIZE,
    sort: 'id,desc',
    ..._filters,
  };

  const isAdminOrHR = hasRole('ADMIN') || hasRole('HR');
  let result;

  if (isAdminOrHR) {
    result = await PayrollApi.getPayrolls(params);
  } else {
    const { getProfile } = await import('../api/employee.api.js');
    const profileRes = await getProfile();
    if (!profileRes.ok) {
      tbody.innerHTML = `<tr><td colspan="10"><div class="table-empty"><p>Profile not linked. Please contact Admin.</p></div></td></tr>`;
      return;
    }
    const empId = profileRes.data.id;
    result = await PayrollApi.getPayrollsByEmployee(empId);
    if (result.ok) {
      result.raw = {
        totalCount: result.data ? result.data.length : 0,
        totalPages: 1,
      };
    }
  }

  if (!result.ok) {
    tbody.innerHTML = `<tr><td colspan="10"><div class="table-empty"><p>Failed to load payrolls</p></div></td></tr>`;
    return;
  }

  const records = result.data ?? [];
  const total   = result.raw?.totalCount ?? 0;
  const pages   = result.raw?.totalPages ?? 1;

  document.getElementById('payroll-count').textContent =
    total > 0 ? `${total} record${total !== 1 ? 's' : ''}` : '';

  if (!records.length) {
    tbody.innerHTML = `
      <tr><td colspan="10">
        <div class="table-empty">
          <svg fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="1.5">
            <path stroke-linecap="round" stroke-linejoin="round" d="M9 14l6-6m-5.5.5h.01m4.99 5h.01M19 21V5a2 2 0 00-2-2H7a2 2 0 00-2 2v16l3.5-2 3.5 2 3.5-2 3.5 2z"/>
          </svg>
          <p>No payroll records found</p>
        </div>
      </td></tr>`;
    document.getElementById('payroll-pagination').innerHTML = '';
    return;
  }

  tbody.innerHTML = records.map((p, i) => `
    <tr>
      <td>${((_currentPage - 1) * PAGE_SIZE) + i + 1}</td>
      <td><span class="font-semibold"></span></td>
      <td>${_monthName(p.month)} ${p.year}</td>
      <td>₹${_fmt(p.baseSalary)}</td>
      <td>₹${_fmt(p.bonus)}</td>
      <td>₹${_fmt(p.deductions)}</td>
      <td><strong>₹${_fmt(p.netSalary)}</strong></td>
      <td><span class="badge badge-${p.payrollStatus}">${p.payrollStatus}</span></td>
      <td>${_formatDate(p.generatedDate)}</td>
      <td>
        <div class="table-actions">
          ${_renderActions(p)}
        </div>
      </td>
    </tr>
  `).join('');

  // Safe employee name injection
  records.forEach((p, i) => {
    const rows = tbody.querySelectorAll('tr');
    if (rows[i]) rows[i].querySelector('.font-semibold').textContent = p.employeeName ?? '—';
  });

  _renderPagination(pages);
  _bindActionButtons();
}

async function _loadEmployeesForModal() {
  const sel = document.getElementById('gp-employee');
  const result = await getEmployees({ size: 200, status: 'ACTIVE' });
  if (!result.ok || !result.data?.length) {
    sel.innerHTML = '<option value="">No active employees found</option>';
    return;
  }
  sel.innerHTML = '<option value="">Select employee…</option>' +
    result.data.map(e => `<option value="${e.id}" data-name="${e.firstName} ${e.lastName}">${e.firstName} ${e.lastName} (ID: ${e.id})</option>`).join('');
}

/* ── Actions ──────────────────────────────────────────────────────────────── */

function _renderActions(p) {
  const isAdmin = hasRole('ADMIN');
  const isHR    = hasRole('HR');
  const actions = [];

  // View details
  actions.push(`<button class="btn btn-sm btn-ghost action-view-btn" data-id="${p.id}" title="View Details">View</button>`);

  // View salary slip
  if (p.payrollStatus === 'APPROVED' || p.payrollStatus === 'PAID') {
    actions.push(`
      <button class="btn btn-sm btn-outline action-slip-btn" data-id="${p.id}" title="Salary Slip" style="display:flex; align-items:center; gap:4px;">
        <svg fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2" style="width:14px;height:14px;"><path stroke-linecap="round" stroke-linejoin="round" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"/></svg>
        Salary Slip
      </button>`);
  }

  if (isAdmin) {
    if (p.payrollStatus === 'GENERATED') {
      actions.push(`<button class="btn btn-sm btn-warning action-approve-btn" data-id="${p.id}" title="Approve">Approve</button>`);
      actions.push(`<button class="btn btn-sm btn-danger-outline action-cancel-btn" data-id="${p.id}" title="Cancel">Cancel</button>`);
    }
    if (p.payrollStatus === 'APPROVED') {
      actions.push(`<button class="btn btn-sm btn-success action-pay-btn" data-id="${p.id}" title="Mark as Paid">Pay</button>`);
      actions.push(`<button class="btn btn-sm btn-danger-outline action-cancel-btn" data-id="${p.id}" title="Cancel">Cancel</button>`);
    }
  }



  return actions.join('');
}

function _bindActionButtons() {
  // View Details
  document.querySelectorAll('.action-view-btn').forEach(btn => {
    btn.addEventListener('click', () => _openPayrollDetails(btn.dataset.id));
  });

  // Approve
  document.querySelectorAll('.action-approve-btn').forEach(btn => {
    btn.addEventListener('click', async () => {
      const confirmed = await openConfirm({
        title:   'Approve Payroll',
        message: 'Are you sure you want to approve this payroll? This will move it to APPROVED status.',
        type:    'warning',
        confirmLabel: 'Approve',
      });
      if (!confirmed) return;
      const res = await PayrollApi.approvePayroll(btn.dataset.id);
      if (res.ok) { showToast('success', 'Approved', res.message); _loadPayrolls(); }
      else showToast('error', 'Failed', res.message);
    });
  });

  // Mark as Paid
  document.querySelectorAll('.action-pay-btn').forEach(btn => {
    btn.addEventListener('click', async () => {
      const confirmed = await openConfirm({
        title:   'Mark as Paid',
        message: 'Confirm that salary has been disbursed for this payroll. This action cannot be undone.',
        type:    'warning',
        confirmLabel: 'Mark Paid',
      });
      if (!confirmed) return;
      const res = await PayrollApi.payPayroll(btn.dataset.id);
      if (res.ok) { showToast('success', 'Marked as Paid', res.message); _loadPayrolls(); }
      else showToast('error', 'Failed', res.message);
    });
  });

  // Cancel
  document.querySelectorAll('.action-cancel-btn').forEach(btn => {
    btn.addEventListener('click', async () => {
      const confirmed = await openConfirm({
        title:   'Cancel Payroll',
        message: 'Are you sure you want to cancel this payroll? This cannot be undone.',
        type:    'danger',
        confirmLabel: 'Yes, Cancel',
      });
      if (!confirmed) return;
      const res = await PayrollApi.cancelPayroll(btn.dataset.id);
      if (res.ok) { showToast('success', 'Cancelled', res.message); _loadPayrolls(); }
      else showToast('error', 'Failed', res.message);
    });
  });



  // View / Download Salary Slip
  document.querySelectorAll('.action-slip-btn').forEach(btn => {
    btn.addEventListener('click', () => {
      const payrollId = btn.dataset.id;
      const token     = getToken();
      // Build URL — Spring Security validates the JWT in the Authorization header.
      // We use fetch + createObjectURL so no token is exposed in the browser URL bar.
      const url = `/payrolls/${payrollId}/salary-slip/download`;
      fetch(url, {
        headers: { 'Authorization': `Bearer ${token}` },
      })
        .then(res => {
          if (!res.ok) throw new Error('Slip not found or server error');
          return res.blob();
        })
        .then(blob => {
          const objUrl = URL.createObjectURL(blob);
          const a = document.createElement('a');
          a.href = objUrl;
          a.download = `salary-slip-${payrollId}.pdf`;
          document.body.appendChild(a);
          a.click();
          a.remove();
          URL.revokeObjectURL(objUrl);
        })
        .catch(err => showToast('error', 'Download Failed', err.message));
    });
  });
}

/* ── Generate Payroll Form ────────────────────────────────────────────────── */

function _bindEvents() {
  // Open/close modal
  document.getElementById('generate-payroll-btn')?.addEventListener('click', () => openModal('generate-payroll-modal'));
  document.getElementById('gp-close-btn')?.addEventListener('click',         () => closeModal('generate-payroll-modal'));
  document.getElementById('gp-cancel-btn')?.addEventListener('click',        () => closeModal('generate-payroll-modal'));

  // Generate All
  document.getElementById('generate-all-payroll-btn')?.addEventListener('click', () => openModal('generate-all-modal'));
  document.getElementById('ga-close-btn')?.addEventListener('click',             () => closeModal('generate-all-modal'));
  document.getElementById('ga-cancel-btn')?.addEventListener('click',            () => closeModal('generate-all-modal'));
  document.getElementById('ga-submit-btn')?.addEventListener('click',            _submitGenerateAllPayrolls);

  // Detail View
  document.getElementById('pd-close-btn')?.addEventListener('click',             () => closeModal('payroll-detail-modal'));
  document.getElementById('pd-close-bottom-btn')?.addEventListener('click',      () => closeModal('payroll-detail-modal'));

  // Filters
  document.getElementById('apply-filter-btn')?.addEventListener('click', () => {
    _filters = {
      month:  document.getElementById('filter-month')?.value  || undefined,
      year:   document.getElementById('filter-year')?.value   || undefined,
      status: document.getElementById('filter-status')?.value || undefined,
    };
    _currentPage = 1;
    _loadPayrolls();
  });
  document.getElementById('clear-filter-btn')?.addEventListener('click', () => {
    document.getElementById('filter-month').value  = '';
    document.getElementById('filter-year').value   = '';
    document.getElementById('filter-status').value = '';
    _filters = {};
    _currentPage = 1;
    _loadPayrolls();
  });

  // Generate payroll submit
  document.getElementById('gp-submit-btn')?.addEventListener('click', _submitGeneratePayroll);
}

async function _submitGeneratePayroll() {
  const errEl  = document.getElementById('gp-error');
  errEl.textContent = ''; errEl.classList.remove('visible');
  _clearFieldErrors();

  const employeeId  = document.getElementById('gp-employee')?.value;
  const month       = parseInt(document.getElementById('gp-month')?.value);
  const year        = parseInt(document.getElementById('gp-year')?.value);
  const bonus       = parseFloat(document.getElementById('gp-bonus')?.value || '0');
  const deductions  = parseFloat(document.getElementById('gp-deductions')?.value);

  // Validate
  let valid = true;
  if (!employeeId) { _fieldErr('gp-employee-err', 'Please select an employee'); valid = false; }
  if (!month || month < 1 || month > 12) { _fieldErr('gp-month-err', 'Invalid month'); valid = false; }
  if (!year  || year < 2000)             { _fieldErr('gp-year-err',  'Enter a valid year (≥ 2000)'); valid = false; }
  if (isNaN(deductions) || deductions < 0) { _fieldErr('gp-deductions-err', 'Deductions must be 0 or more'); valid = false; }
  if (!valid) return;

  const btn = document.getElementById('gp-submit-btn');
  btn.disabled = true; btn.textContent = 'Generating…';

  const res = await PayrollApi.generatePayroll({
    employeeId: parseInt(employeeId),
    month, year,
    bonus:      bonus || 0,
    deductions,
  });

  btn.disabled = false; btn.textContent = 'Generate';

  if (!res.ok) {
    errEl.textContent = res.message || 'Failed to generate payroll.';
    errEl.classList.add('visible');
    return;
  }

  showToast('success', 'Payroll Generated', res.message);
  closeModal('generate-payroll-modal');
  document.getElementById('generate-payroll-form')?.reset();
  _currentPage = 1;
  _loadPayrolls();
}

/* ── Pagination ───────────────────────────────────────────────────────────── */

function _renderPagination(totalPages) {
  const container = document.getElementById('payroll-pagination');
  if (totalPages <= 1) { container.innerHTML = ''; return; }

  container.innerHTML = `
    <div class="pagination">
      <span class="pagination-info">Page ${_currentPage} of ${totalPages}</span>
      <div class="pagination-controls">
        <button class="page-btn" id="prev-page" ${_currentPage <= 1 ? 'disabled' : ''} aria-label="Previous">
          <svg fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2"><polyline points="15 18 9 12 15 6"/></svg>
        </button>
        <span class="page-btn active" aria-current="page">${_currentPage}</span>
        <button class="page-btn" id="next-page" ${_currentPage >= totalPages ? 'disabled' : ''} aria-label="Next">
          <svg fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2"><polyline points="9 18 15 12 9 6"/></svg>
        </button>
      </div>
    </div>
  `;

  document.getElementById('prev-page')?.addEventListener('click', () => { _currentPage--; _loadPayrolls(); });
  document.getElementById('next-page')?.addEventListener('click', () => { _currentPage++; _loadPayrolls(); });
}

/* ── Helpers ─────────────────────────────────────────────────────────────── */

function _fieldErr(id, msg) {
  const el = document.getElementById(id);
  if (el) { el.textContent = msg; el.classList.add('visible'); }
}
function _clearFieldErrors() {
  document.querySelectorAll('#generate-payroll-form .form-error').forEach(el => {
    el.textContent = ''; el.classList.remove('visible');
  });
}
function _fmt(val) {
  return val != null ? parseFloat(val).toLocaleString('en-IN', { minimumFractionDigits: 2 }) : '0.00';
}
function _monthName(m) {
  return new Date(2000, m - 1).toLocaleString('default', { month: 'short' });
}
function _formatDate(dateStr) {
  if (!dateStr) return '—';
  return new Date(dateStr).toLocaleDateString('en-IN', { day:'2-digit', month:'short', year:'numeric' });
}

async function _openPayrollDetails(id) {
  const bodyEl = document.getElementById('pd-modal-body');
  if (!bodyEl) return;
  bodyEl.innerHTML = '<div class="table-empty"><p>Loading details...</p></div>';
  openModal('payroll-detail-modal');

  const res = await PayrollApi.getPayroll(id);
  if (!res.ok) {
    bodyEl.innerHTML = '<div class="form-error visible">Failed to load payroll details.</div>';
    return;
  }

  const p = res.data;

  bodyEl.innerHTML = `
    <div style="display:flex; justify-content:space-between; align-items:center; border-bottom:1px solid #E5E7EB; padding-bottom:1rem; margin-bottom:1.5rem;">
      <div>
        <h4 style="margin:0; font-size:16px; font-weight:700; color:var(--text-primary);">${p.employeeName ?? `Employee #${p.employeeId}`}</h4>
        <p class="text-xs text-muted" style="margin:2px 0 0 0;">Employee ID: ${p.employeeId}</p>
      </div>
      <div>
        <span class="badge badge-${p.payrollStatus}">${p.payrollStatus}</span>
      </div>
    </div>

    <div style="display:grid; grid-template-columns:1fr 1fr; gap:1.25rem; margin-bottom:1.5rem;">
      <div>
        <span class="text-xs text-muted" style="display:block; text-transform:uppercase; font-weight:700; font-size:10px; letter-spacing:0.5px; margin-bottom:2px;">Payroll Period</span>
        <strong style="color:var(--text-primary); font-size:14px;">${_monthName(p.month)} ${p.year}</strong>
      </div>
      <div>
        <span class="text-xs text-muted" style="display:block; text-transform:uppercase; font-weight:700; font-size:10px; letter-spacing:0.5px; margin-bottom:2px;">Generated Date</span>
        <span style="color:var(--text-primary); font-size:14px;">${_formatDate(p.generatedDate)}</span>
      </div>
    </div>

    <h5 style="margin:0 0 0.75rem 0; font-size:12px; font-weight:700; text-transform:uppercase; color:var(--text-secondary); letter-spacing:0.5px; border-bottom:1px solid #F3F4F6; padding-bottom:4px;">Salary Breakdown</h5>
    <div style="display:flex; flex-direction:column; gap:0.75rem; margin-bottom:1.5rem; background:#F9FAFB; padding:1rem; border-radius:8px;">
      <div style="display:flex; justify-content:space-between;">
        <span class="text-sm text-muted">Base Salary</span>
        <span class="text-sm font-semibold" style="color:var(--text-primary);">₹${_fmt(p.baseSalary)}</span>
      </div>
      <div style="display:flex; justify-content:space-between;">
        <span class="text-sm text-muted">Bonus</span>
        <span class="text-sm font-semibold text-success">+ ₹${_fmt(p.bonus)}</span>
      </div>
      <div style="display:flex; justify-content:space-between; border-bottom:1px dashed #E5E7EB; padding-bottom:0.75rem; margin-bottom:0.25rem;">
        <span class="text-sm text-muted">Deductions</span>
        <span class="text-sm font-semibold text-danger">- ₹${_fmt(p.deductions)}</span>
      </div>
      <div style="display:flex; justify-content:space-between; align-items:center;">
        <strong class="text-base" style="color:var(--text-primary);">Net Salary</strong>
        <strong class="text-lg" style="color:#4F46E5;">₹${_fmt(p.netSalary)}</strong>
      </div>
    </div>

    ${(p.payrollStatus === 'APPROVED' || p.payrollStatus === 'PAID') ? `
      <div style="display:flex; justify-content:center; margin-top:1.5rem; border-top:1px solid #E5E7EB; padding-top:1.25rem;">
        <button class="btn btn-outline" id="pd-download-slip-btn" style="display:flex; align-items:center; gap:6px; width:100%; justify-content:center;">
          <svg fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2" style="width:16px;height:16px;"><path stroke-linecap="round" stroke-linejoin="round" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"/></svg>
          Download Salary Slip
        </button>
      </div>
    ` : ''}
  `;

  if (p.payrollStatus === 'APPROVED' || p.payrollStatus === 'PAID') {
    document.getElementById('pd-download-slip-btn')?.addEventListener('click', () => {
      const token = getToken();
      const url = `/payrolls/${p.id}/salary-slip/download`;
      fetch(url, {
        headers: { 'Authorization': `Bearer ${token}` },
      })
        .then(res => {
          if (!res.ok) throw new Error('Slip not found or server error');
          return res.blob();
        })
        .then(blob => {
          const objUrl = URL.createObjectURL(blob);
          const a = document.createElement('a');
          a.href = objUrl;
          a.download = `salary-slip-${p.id}.pdf`;
          document.body.appendChild(a);
          a.click();
          a.remove();
          URL.revokeObjectURL(objUrl);
        })
        .catch(err => showToast('error', 'Download Failed', err.message));
    });
  }
}

async function _submitGenerateAllPayrolls() {
  const errEl  = document.getElementById('ga-error');
  if (errEl) { errEl.textContent = ''; errEl.classList.remove('visible'); }
  
  document.querySelectorAll('#generate-all-form .form-error').forEach(el => {
    el.textContent = ''; el.classList.remove('visible');
  });

  const month       = parseInt(document.getElementById('ga-month')?.value);
  const year        = parseInt(document.getElementById('ga-year')?.value);
  const bonus       = parseFloat(document.getElementById('ga-bonus')?.value || '0');
  const deductions  = parseFloat(document.getElementById('ga-deductions')?.value || '0');

  let valid = true;
  if (!month || month < 1 || month > 12) { 
    const err = document.getElementById('ga-month-err');
    if (err) { err.textContent = 'Invalid month'; err.classList.add('visible'); }
    valid = false; 
  }
  if (!year  || year < 2000) { 
    const err = document.getElementById('ga-year-err');
    if (err) { err.textContent = 'Enter a valid year (≥ 2000)'; err.classList.add('visible'); }
    valid = false; 
  }
  if (isNaN(deductions) || deductions < 0) { 
    const err = document.getElementById('ga-deductions-err');
    if (err) { err.textContent = 'Deductions must be 0 or more'; err.classList.add('visible'); }
    valid = false; 
  }
  if (!valid) return;

  const btn = document.getElementById('ga-submit-btn');
  btn.disabled = true; btn.textContent = 'Generating…';

  const res = await PayrollApi.generateAllPayrolls({
    month, year,
    bonus:      bonus || 0,
    deductions: deductions || 0
  });

  btn.disabled = false; btn.textContent = 'Generate For All';

  if (!res.ok) {
    if (errEl) {
      errEl.textContent = res.message || 'Failed to generate payrolls.';
      errEl.classList.add('visible');
    }
    return;
  }

  const summary = res.data;
  const msg = `Bulk generation finished!\nGenerated: ${summary.generated}, Skipped: ${summary.skipped}, Failed: ${summary.failed}`;
  showToast('success', 'Bulk Generation Complete', msg);
  
  closeModal('generate-all-modal');
  document.getElementById('generate-all-form')?.reset();
  _currentPage = 1;
  _loadPayrolls();
}
