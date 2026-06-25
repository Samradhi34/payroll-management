/**
 * employees.page.js — Employee Management Page
 *
 * Features:
 *  - Paginated employee list with search (name/email), status filter, department filter
 *  - Create employee with image upload (multipart)
 *  - Edit employee details
 *  - Change employee status (ACTIVE / INACTIVE / TERMINATED / RESIGNED)
 *  - View employee profile inline
 *  - Status badge coloring based on EmployeeStatus enum
 */

import * as EmpApi  from '../api/employee.api.js';
import * as DeptApi from '../api/department.api.js';
import { showToast }  from '../components/toast.js';
import { openModal, closeModal, openConfirm } from '../components/modal.js';
import { hasRole } from '../auth/auth.store.js';

const PAGE_SIZE = 10;
let _currentPage = 1;
let _filters     = {};
let _departments = [];
let _editingId   = null;

export async function render() {
  const content = document.getElementById('page-content');
  if (!content) return;

  const isAdmin = hasRole('ADMIN');
  const isHR    = hasRole('HR');

  content.innerHTML = `
    <div class="page-header">
      <div>
        <h1 class="page-title">Employees</h1>
        <p class="page-subtitle">Manage all employee records</p>
      </div>
      <div class="page-actions">
        ${(isAdmin || isHR) ? `
          <button class="btn btn-primary" id="add-employee-btn">
            <svg fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2" style="width:16px;height:16px;">
              <path stroke-linecap="round" stroke-linejoin="round" d="M12 4v16m8-8H4"/>
            </svg>
            Add Employee
          </button>` : ''}
      </div>
    </div>

    <!-- Filter Bar -->
    <div class="card" style="margin-bottom:1.5rem;">
      <div class="card-body-sm">
        <div class="filter-bar">
          <div class="form-group" style="flex:2;">
            <label class="form-label">Search</label>
            <div class="input-group">
              <span class="input-group-icon">
                <svg fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2"><circle cx="11" cy="11" r="8"/><path stroke-linecap="round" stroke-linejoin="round" d="M21 21l-4.35-4.35"/></svg>
              </span>
              <input type="text" class="form-control" id="filter-search" placeholder="Search by name or email…" maxlength="50"/>
            </div>
          </div>
          <div class="form-group">
            <label class="form-label">Status</label>
            <select class="form-control" id="filter-status">
              <option value="">All statuses</option>
              <option value="ACTIVE">Active</option>
              <option value="INACTIVE">Inactive</option>
              <option value="TERMINATED">Terminated</option>
              <option value="RESIGNED">Resigned</option>
            </select>
          </div>
          <div class="form-group">
            <label class="form-label">Department</label>
            <select class="form-control" id="filter-dept">
              <option value="">All departments</option>
            </select>
          </div>
          <button class="btn btn-primary btn-sm" id="apply-filter-btn">Search</button>
          <button class="btn btn-ghost btn-sm"   id="clear-filter-btn">Clear</button>
        </div>
      </div>
    </div>

    <!-- Table -->
    <div class="card">
      <div class="card-header">
        <h2>Employee List</h2>
        <span id="emp-count" class="text-sm text-muted"></span>
      </div>
      <div class="table-container">
        <table class="table">
          <thead>
            <tr>
              <th>#</th>
              <th>Employee</th>
              <th>Designation</th>
              <th>Department</th>
              <th>Base Salary</th>
              <th>Joining Date</th>
              <th>Status</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody id="emp-tbody">
            <tr><td colspan="8"><div class="table-empty"><p>Loading employees…</p></div></td></tr>
          </tbody>
        </table>
      </div>
      <div id="emp-pagination"></div>
    </div>

    <!-- Add/Edit Employee Modal -->
    <div class="modal-backdrop" id="emp-modal" role="dialog" aria-modal="true" aria-labelledby="emp-modal-title">
      <div class="modal modal-lg">
        <div class="modal-header">
          <h3 id="emp-modal-title">Add Employee</h3>
          <button class="modal-close" id="emp-close-btn" aria-label="Close">
            <svg fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2"><path stroke-linecap="round" stroke-linejoin="round" d="M6 18L18 6M6 6l12 12"/></svg>
          </button>
        </div>
        <div class="modal-body">
          <div id="emp-modal-error" class="form-error" style="margin-bottom:1rem;font-size:13px;" role="alert"></div>
          <form id="emp-form" novalidate>
            <div class="form-row">
              <div class="form-group">
                <label class="form-label required" for="emp-first-name">First Name</label>
                <input type="text" class="form-control" id="emp-first-name" required maxlength="50"/>
                <span class="form-error" id="emp-first-name-err"></span>
              </div>
              <div class="form-group">
                <label class="form-label required" for="emp-last-name">Last Name</label>
                <input type="text" class="form-control" id="emp-last-name" required maxlength="50"/>
                <span class="form-error" id="emp-last-name-err"></span>
              </div>
            </div>
            <div class="form-row">
              <div class="form-group">
                <label class="form-label required" for="emp-email">Email</label>
                <input type="email" class="form-control" id="emp-email" required maxlength="100"/>
                <span class="form-error" id="emp-email-err"></span>
              </div>
              <div class="form-group">
                <label class="form-label required" for="emp-phone">Phone</label>
                <input type="tel" class="form-control" id="emp-phone" required maxlength="15" pattern="[0-9]{10,15}"/>
                <span class="form-error" id="emp-phone-err"></span>
              </div>
            </div>
            <div class="form-row">
              <div class="form-group">
                <label class="form-label required" for="emp-designation">Designation</label>
                <input type="text" class="form-control" id="emp-designation" required maxlength="100"/>
                <span class="form-error" id="emp-designation-err"></span>
              </div>
              <div class="form-group">
                <label class="form-label required" for="emp-dept">Department</label>
                <select class="form-control" id="emp-dept" required>
                  <option value="">Select department…</option>
                </select>
                <span class="form-error" id="emp-dept-err"></span>
              </div>
            </div>
            <div class="form-row">
              <div class="form-group">
                <label class="form-label required" for="emp-salary">Base Salary (₹)</label>
                <input type="number" class="form-control" id="emp-salary" required min="0" step="0.01"/>
                <span class="form-error" id="emp-salary-err"></span>
              </div>
              <div class="form-group">
                <label class="form-label required" for="emp-joining">Joining Date</label>
                <input type="date" class="form-control" id="emp-joining" required/>
                <span class="form-error" id="emp-joining-err"></span>
              </div>
            </div>
            <div class="form-row">
              <div class="form-group" id="emp-status-group">
                <label class="form-label required" for="emp-status">Employee Status</label>
                <select class="form-control" id="emp-status" required>
                  <option value="ACTIVE" selected>Active</option>
                  <option value="INACTIVE">Inactive</option>
                  <option value="TERMINATED">Terminated</option>
                  <option value="RESIGNED">Resigned</option>
                </select>
                <span class="form-error" id="emp-status-err"></span>
              </div>
              <div class="form-group" id="emp-image-group">
                <label class="form-label" for="emp-image">Profile Photo (PNG/JPG, max 2MB)</label>
                <input type="file" class="form-control" id="emp-image" accept=".jpg,.jpeg,.png"/>
                <span class="form-error" id="emp-image-err"></span>
              </div>
            </div>
          </form>
        </div>
        <div class="modal-footer">
          <button class="btn btn-ghost" id="emp-cancel-btn">Cancel</button>
          <button class="btn btn-primary" id="emp-submit-btn">Save Employee</button>
        </div>
      </div>
    </div>

    <!-- Change Status Modal -->
    <div class="modal-backdrop" id="status-modal" role="dialog" aria-modal="true" aria-labelledby="status-modal-title">
      <div class="modal modal-sm">
        <div class="modal-header">
          <h3 id="status-modal-title">Change Status</h3>
          <button class="modal-close" id="status-close-btn" aria-label="Close">
            <svg fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2"><path stroke-linecap="round" stroke-linejoin="round" d="M6 18L18 6M6 6l12 12"/></svg>
          </button>
        </div>
        <div class="modal-body">
          <div class="form-group">
            <label class="form-label">New Status for <strong id="status-emp-name"></strong></label>
            <select class="form-control" id="status-select">
              <option value="ACTIVE">Active</option>
              <option value="INACTIVE">Inactive</option>
              <option value="TERMINATED">Terminated</option>
              <option value="RESIGNED">Resigned</option>
            </select>
          </div>
        </div>
        <div class="modal-footer">
          <button class="btn btn-ghost" id="status-cancel-btn">Cancel</button>
          <button class="btn btn-primary" id="status-submit-btn">Update Status</button>
        </div>
      </div>
    </div>
  `;

  _bindEvents();
  await Promise.all([_loadDepartments(), _loadEmployees()]);
}

/* ── Data loading ─────────────────────────────────────────────────────────── */

async function _loadDepartments() {
  const res = await DeptApi.getDepartments({ size: 200 });
  _departments = res.ok ? (res.data ?? []) : [];
  const deptSel = document.getElementById('filter-dept');
  const empDept = document.getElementById('emp-dept');
  const opts = _departments.map(d => `<option value="${d.id}">${d.name}</option>`).join('');
  if (deptSel) deptSel.innerHTML += opts;
  if (empDept) empDept.innerHTML += opts;
}

async function _loadEmployees() {
  const tbody = document.getElementById('emp-tbody');
  tbody.innerHTML = `<tr><td colspan="8"><div class="table-empty"><p>Loading…</p></div></td></tr>`;

  const params = { page: _currentPage, size: PAGE_SIZE, sort: 'id,desc', ..._filters };
  const result = await EmpApi.getEmployees(params);

  if (!result.ok) {
    tbody.innerHTML = `<tr><td colspan="8"><div class="table-empty"><p>Failed to load employees.</p></div></td></tr>`;
    return;
  }

  const records = result.data ?? [];
  const total   = result.raw?.totalCount ?? 0;
  const pages   = result.raw?.totalPages ?? 1;

  document.getElementById('emp-count').textContent = total > 0 ? `${total} employee${total !== 1 ? 's' : ''}` : '';

  if (!records.length) {
    tbody.innerHTML = `
      <tr><td colspan="8">
        <div class="table-empty">
          <svg fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="1.5">
            <path stroke-linecap="round" stroke-linejoin="round" d="M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0z"/>
          </svg>
          <p>No employees found</p>
        </div>
      </td></tr>`;
    document.getElementById('emp-pagination').innerHTML = '';
    return;
  }

  const isAdminOrHR = hasRole('ADMIN') || hasRole('HR');

  tbody.innerHTML = records.map((e, i) => `
    <tr>
      <td>${((_currentPage - 1) * PAGE_SIZE) + i + 1}</td>
      <td>
        <div class="emp-info">
          <div class="emp-avatar" id="emp-avatar-${e.id}">
            ${e.profileImagePath
              ? `<img src="${e.profileImagePath}" alt="" loading="lazy">`
              : '<span></span>'}
          </div>
          <div>
            <div class="emp-name" id="emp-name-${e.id}"></div>
            <div class="emp-email" id="emp-email-${e.id}"></div>
          </div>
        </div>
      </td>
      <td id="emp-desig-${e.id}"></td>
      <td id="emp-dept-disp-${e.id}"></td>
      <td>₹${parseFloat(e.baseSalary || 0).toLocaleString('en-IN')}</td>
      <td>${e.joiningDate ?? '—'}</td>
      <td><span class="badge badge-${e.employeeStatus}">${e.employeeStatus}</span></td>
      <td>
        <div class="table-actions">
          ${isAdminOrHR ? `
            <button class="btn btn-sm btn-outline action-edit-btn" data-id="${e.id}" title="Edit">
              <svg fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2" style="width:14px;height:14px;">
                <path stroke-linecap="round" stroke-linejoin="round" d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z"/>
              </svg>
            </button>
            <button class="btn btn-sm btn-ghost action-status-btn"
              data-id="${e.id}"
              data-name="${''}"
              data-status="${e.employeeStatus}"
              title="Change Status">
              <svg fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2" style="width:14px;height:14px;">
                <path stroke-linecap="round" stroke-linejoin="round" d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15"/>
              </svg>
            </button>` : ''}
        </div>
      </td>
    </tr>
  `).join('');

  // Safe text injection for all user data
  records.forEach(e => {
    const nameEl  = document.getElementById(`emp-name-${e.id}`);
    const emailEl = document.getElementById(`emp-email-${e.id}`);
    const desigEl = document.getElementById(`emp-desig-${e.id}`);
    const deptEl  = document.getElementById(`emp-dept-disp-${e.id}`);
    const avatarSpan = document.querySelector(`#emp-avatar-${e.id} span`);

    const fullName = `${e.firstName} ${e.lastName}`;
    if (nameEl)  nameEl.textContent  = fullName;
    if (emailEl) emailEl.textContent = e.email ?? '';
    if (desigEl) desigEl.textContent = e.designation ?? '—';
    if (deptEl)  deptEl.textContent  = e.departmentName ?? '—';
    if (avatarSpan) avatarSpan.textContent = (e.firstName ?? 'E').charAt(0).toUpperCase();

    // Set name on status button for display
    const statusBtn = document.querySelector(`.action-status-btn[data-id="${e.id}"]`);
    if (statusBtn) statusBtn.setAttribute('data-name', fullName);
  });

  _renderPagination(pages);
  _bindActionButtons();
}

/* ── Events ───────────────────────────────────────────────────────────────── */

function _bindEvents() {
  // Add employee
  document.getElementById('add-employee-btn')?.addEventListener('click', () => {
    _editingId = null;
    document.getElementById('emp-modal-title').textContent = 'Add Employee';
    document.getElementById('emp-form')?.reset();
    document.getElementById('emp-image-group').style.display = '';
    document.getElementById('emp-status-group').style.display = '';
    _clearFieldErrors('#emp-form');
    openModal('emp-modal');
  });

  // Close modals
  document.getElementById('emp-close-btn')?.addEventListener('click',    () => closeModal('emp-modal'));
  document.getElementById('emp-cancel-btn')?.addEventListener('click',   () => closeModal('emp-modal'));
  document.getElementById('status-close-btn')?.addEventListener('click', () => closeModal('status-modal'));
  document.getElementById('status-cancel-btn')?.addEventListener('click',() => closeModal('status-modal'));

  // Filters
  document.getElementById('apply-filter-btn')?.addEventListener('click', _applyFilters);
  document.getElementById('clear-filter-btn')?.addEventListener('click',  _clearFilters);
  document.getElementById('filter-search')?.addEventListener('keydown', e => { if (e.key === 'Enter') _applyFilters(); });

  // Save employee
  document.getElementById('emp-submit-btn')?.addEventListener('click', _submitEmployee);

  // Update status
  document.getElementById('status-submit-btn')?.addEventListener('click', _submitStatusChange);

  // Image validation
  document.getElementById('emp-image')?.addEventListener('change', _validateImage);
}

function _bindActionButtons() {
  document.querySelectorAll('.action-edit-btn').forEach(btn => {
    btn.addEventListener('click', async () => {
      _editingId = btn.dataset.id;
      const res  = await EmpApi.getEmployee(_editingId);
      if (!res.ok) { showToast('error', 'Error', res.message); return; }

      const e = res.data;
      document.getElementById('emp-modal-title').textContent = 'Edit Employee';
      document.getElementById('emp-first-name').value = e.firstName ?? '';
      document.getElementById('emp-last-name').value  = e.lastName  ?? '';
      document.getElementById('emp-email').value      = e.email     ?? '';
      document.getElementById('emp-phone').value      = e.phone     ?? '';
      document.getElementById('emp-designation').value= e.designation ?? '';
      document.getElementById('emp-salary').value     = e.baseSalary ?? '';
      document.getElementById('emp-joining').value    = e.joiningDate ?? '';
      // Hide image and status fields on edit (separate endpoint / modal)
      document.getElementById('emp-image-group').style.display = 'none';
      document.getElementById('emp-status-group').style.display = 'none';
      _clearFieldErrors('#emp-form');
      openModal('emp-modal');
    });
  });

  document.querySelectorAll('.action-status-btn').forEach(btn => {
    btn.addEventListener('click', () => {
      const empName = btn.getAttribute('data-name') || '';
      document.getElementById('status-emp-name').textContent = empName;
      document.getElementById('status-select').value = btn.dataset.status ?? 'ACTIVE';
      document.getElementById('status-submit-btn').setAttribute('data-id', btn.dataset.id);
      openModal('status-modal');
    });
  });
}

/* ── Submit handlers ──────────────────────────────────────────────────────── */

async function _submitEmployee() {
  _clearFieldErrors('#emp-form');
  const errEl = document.getElementById('emp-modal-error');
  errEl.textContent = ''; errEl.classList.remove('visible');

  const firstName   = document.getElementById('emp-first-name')?.value.trim();
  const lastName    = document.getElementById('emp-last-name')?.value.trim();
  const email       = document.getElementById('emp-email')?.value.trim();
  const phone       = document.getElementById('emp-phone')?.value.trim();
  const designation = document.getElementById('emp-designation')?.value.trim();
  const deptId      = document.getElementById('emp-dept')?.value;
  const salary      = document.getElementById('emp-salary')?.value;
  const joining     = document.getElementById('emp-joining')?.value;
  const status      = document.getElementById('emp-status')?.value;
  const imageFile   = document.getElementById('emp-image')?.files?.[0];

  let valid = true;
  if (!firstName)   { _fieldErr('emp-first-name-err', 'First name is required');  valid = false; }
  if (!lastName)    { _fieldErr('emp-last-name-err',  'Last name is required');   valid = false; }
  if (!email || !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) { _fieldErr('emp-email-err', 'Valid email is required'); valid = false; }
  if (!phone || !/^[0-9]{10,15}$/.test(phone))  { _fieldErr('emp-phone-err', 'Enter 10-15 digit phone number'); valid = false; }
  if (!designation) { _fieldErr('emp-designation-err', 'Designation is required'); valid = false; }
  if (!deptId)      { _fieldErr('emp-dept-err',        'Please select department'); valid = false; }
  if (!salary || parseFloat(salary) < 0) { _fieldErr('emp-salary-err', 'Valid salary is required'); valid = false; }
  if (!joining)     { _fieldErr('emp-joining-err', 'Joining date is required'); valid = false; }
  if (!_editingId && !status) { _fieldErr('emp-status-err', 'Employee Status is required'); valid = false; }
  if (!valid) return;

  const btn = document.getElementById('emp-submit-btn');
  btn.disabled = true; btn.textContent = 'Saving…';

  let res;
  if (_editingId) {
    // Edit — JSON body (no image in edit for simplicity; separate image endpoint)
    res = await EmpApi.updateEmployee(_editingId, { firstName, lastName, email, phone, designation, departmentId: parseInt(deptId), baseSalary: parseFloat(salary), joiningDate: joining });
  } else {
    // Create — multipart FormData
    const fd = new FormData();
    fd.append('firstName',    firstName);
    fd.append('lastName',     lastName);
    fd.append('email',        email);
    fd.append('phone',        phone);
    fd.append('designation',  designation);
    fd.append('departmentId', deptId);
    fd.append('baseSalary',   salary);
    fd.append('joiningDate',  joining);
    fd.append('employeeStatus', status);
    if (imageFile) fd.append('image', imageFile);
    res = await EmpApi.createEmployee(fd);
  }

  btn.disabled = false; btn.textContent = 'Save Employee';

  if (!res.ok) {
    errEl.textContent = res.message || 'Failed to save employee.';
    errEl.classList.add('visible');
    return;
  }

  showToast('success', _editingId ? 'Employee Updated' : 'Employee Created', res.message);
  closeModal('emp-modal');
  document.getElementById('emp-form')?.reset();
  document.getElementById('emp-image-group').style.display = '';
  document.getElementById('emp-status-group').style.display = '';
  _currentPage = 1;
  _loadEmployees();
}

async function _submitStatusChange() {
  const btn    = document.getElementById('status-submit-btn');
  const id     = btn.getAttribute('data-id');
  const status = document.getElementById('status-select')?.value;

  btn.disabled = true;
  const res = await EmpApi.changeEmployeeStatus(id, status);
  btn.disabled = false;

  if (!res.ok) { showToast('error', 'Failed', res.message); return; }
  showToast('success', 'Status Updated', res.message);
  closeModal('status-modal');
  _loadEmployees();
}

function _validateImage() {
  const file   = document.getElementById('emp-image')?.files?.[0];
  const errEl  = document.getElementById('emp-image-err');
  if (!file) return;

  const allowed = ['image/jpeg', 'image/png'];
  const maxSize = 2 * 1024 * 1024; // 2MB

  if (!allowed.includes(file.type)) {
    errEl.textContent = 'Only PNG, JPG, JPEG allowed.';
    errEl.classList.add('visible');
    document.getElementById('emp-image').value = '';
    return;
  }
  if (file.size > maxSize) {
    errEl.textContent = 'File size must be less than 2MB.';
    errEl.classList.add('visible');
    document.getElementById('emp-image').value = '';
    return;
  }
  errEl.textContent = ''; errEl.classList.remove('visible');
}

/* ── Filter helpers ───────────────────────────────────────────────────────── */

function _applyFilters() {
  _filters = {
    search:       document.getElementById('filter-search')?.value.trim() || undefined,
    status:       document.getElementById('filter-status')?.value || undefined,
    departmentId: document.getElementById('filter-dept')?.value || undefined,
  };
  _currentPage = 1;
  _loadEmployees();
}

function _clearFilters() {
  document.getElementById('filter-search').value = '';
  document.getElementById('filter-status').value = '';
  document.getElementById('filter-dept').value   = '';
  _filters = {};
  _currentPage = 1;
  _loadEmployees();
}

/* ── Pagination ───────────────────────────────────────────────────────────── */

function _renderPagination(totalPages) {
  const container = document.getElementById('emp-pagination');
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
  document.getElementById('prev-page')?.addEventListener('click', () => { _currentPage--; _loadEmployees(); });
  document.getElementById('next-page')?.addEventListener('click', () => { _currentPage++; _loadEmployees(); });
}

/* ── Utility ─────────────────────────────────────────────────────────────── */

function _fieldErr(id, msg) {
  const el = document.getElementById(id);
  if (el) { el.textContent = msg; el.classList.add('visible'); }
}
function _clearFieldErrors(formSel) {
  document.querySelectorAll(`${formSel} .form-error`).forEach(el => {
    el.textContent = ''; el.classList.remove('visible');
  });
  document.querySelectorAll(`${formSel} .is-invalid`).forEach(el => el.classList.remove('is-invalid'));
}
