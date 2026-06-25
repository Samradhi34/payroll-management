/**
 * attendance.page.js — Attendance Management Page
 */
import * as AttApi from '../api/attendance.api.js';
import { getEmployees, getProfile } from '../api/employee.api.js';
import { showToast }  from '../components/toast.js';
import { openModal, closeModal } from '../components/modal.js';
import { hasRole } from '../auth/auth.store.js';

const PAGE_SIZE = 10;
let _currentPage = 1;
let _filters     = {};

export async function render() {
  const content = document.getElementById('page-content');
  if (!content) return;
  const isAdminOrHR = hasRole('ADMIN') || hasRole('HR');

  content.innerHTML = `
    <div class="page-header">
      <div><h1 class="page-title">Attendance</h1><p class="page-subtitle">Track and manage employee attendance</p></div>
      <div class="page-actions">
        ${isAdminOrHR ? `
          <button class="btn btn-outline" id="upload-csv-btn">
            <svg fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2" style="width:16px;height:16px;"><path stroke-linecap="round" stroke-linejoin="round" d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-8l-4-4m0 0L8 8m4-4v12"/></svg>
            Upload CSV
          </button>` : ''}
        <button class="btn btn-primary" id="mark-att-btn">
          <svg fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2" style="width:16px;height:16px;"><path stroke-linecap="round" stroke-linejoin="round" d="M12 4v16m8-8H4"/></svg>
          Mark Attendance
        </button>
      </div>
    </div>

    <!-- Filter Bar -->
    <div class="card" style="margin-bottom:1.5rem;">
      <div class="card-body-sm">
        <div class="filter-bar">
          <div class="form-group">
            <label class="form-label">Status</label>
            <select class="form-control" id="filter-status">
              <option value="">All</option>
              <option value="PRESENT">Present</option>
              <option value="ABSENT">Absent</option>
              <option value="PAID_LEAVE">Paid Leave</option>
              <option value="UNPAID_LEAVE">Unpaid Leave</option>
              <option value="HALF_DAY">Half Day</option>
              <option value="WORK_FROM_HOME">Work From Home</option>
            </select>
          </div>
          <div class="form-group">
            <label class="form-label">Date From</label>
            <input type="date" class="form-control" id="filter-date-from"/>
          </div>
          <div class="form-group">
            <label class="form-label">Date To</label>
            <input type="date" class="form-control" id="filter-date-to"/>
          </div>
          <button class="btn btn-primary btn-sm" id="apply-filter-btn">Filter</button>
          <button class="btn btn-ghost btn-sm"   id="clear-filter-btn">Clear</button>
        </div>
      </div>
    </div>

    <!-- Table -->
    <div class="card">
      <div class="card-header"><h2>Attendance Records</h2><span id="att-count" class="text-sm text-muted"></span></div>
      <div class="table-container">
        <table class="table">
          <thead><tr><th>#</th><th>Employee</th><th>Date</th><th>Status</th><th>Working Hours</th>${isAdminOrHR ? '<th>Actions</th>' : ''}</tr></thead>
          <tbody id="att-tbody"><tr><td colspan="6"><div class="table-empty"><p>Loading…</p></div></td></tr></tbody>
        </table>
      </div>
      <div id="att-pagination"></div>
    </div>

    <!-- Mark Attendance Modal -->
    <div class="modal-backdrop" id="att-modal" role="dialog" aria-modal="true" aria-labelledby="att-modal-title">
      <div class="modal">
        <div class="modal-header">
          <h3 id="att-modal-title">Mark Attendance</h3>
          <button class="modal-close" id="att-close-btn" aria-label="Close">
            <svg fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2"><path stroke-linecap="round" stroke-linejoin="round" d="M6 18L18 6M6 6l12 12"/></svg>
          </button>
        </div>
        <div class="modal-body">
          <div id="att-error" class="form-error" style="margin-bottom:1rem;font-size:13px;" role="alert"></div>
          <form id="att-form" novalidate>
            <div class="form-group">
              <label class="form-label required" for="att-employee">Employee</label>
              <select class="form-control" id="att-employee" required>
                <option value="">Loading employees…</option>
              </select>
              <span class="form-error" id="att-employee-err"></span>
            </div>
            <div class="form-row">
              <div class="form-group">
                <label class="form-label required" for="att-date">Date</label>
                <input type="date" class="form-control" id="att-date" required max="${new Date().toISOString().split('T')[0]}"/>
                <span class="form-error" id="att-date-err"></span>
              </div>
              <div class="form-group">
                <label class="form-label required" for="att-status">Attendance Status</label>
                <select class="form-control" id="att-status" required>
                  <option value="PRESENT">Present</option>
                  <option value="ABSENT">Absent</option>
                  <option value="PAID_LEAVE">Paid Leave</option>
                  <option value="UNPAID_LEAVE">Unpaid Leave</option>
                  <option value="HALF_DAY">Half Day</option>
                  <option value="WORK_FROM_HOME">Work From Home</option>
                </select>
              </div>
            </div>
            <div class="form-group" id="att-hours-group">
              <label class="form-label required" for="att-hours">Working Hours</label>
              <input type="number" class="form-control" id="att-hours" min="0" max="24" step="0.5" value="8"/>
              <span class="form-error" id="att-hours-err"></span>
            </div>
          </form>
        </div>
        <div class="modal-footer">
          <button class="btn btn-ghost" id="att-cancel-btn">Cancel</button>
          <button class="btn btn-primary" id="att-submit-btn">Mark Attendance</button>
        </div>
      </div>
    </div>

    <!-- CSV Upload Modal -->
    <div class="modal-backdrop" id="csv-modal" role="dialog" aria-modal="true" aria-labelledby="csv-modal-title">
      <div class="modal modal-sm">
        <div class="modal-header">
          <h3 id="csv-modal-title">Upload Attendance CSV</h3>
          <button class="modal-close" id="csv-close-btn" aria-label="Close">
            <svg fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2"><path stroke-linecap="round" stroke-linejoin="round" d="M6 18L18 6M6 6l12 12"/></svg>
          </button>
        </div>
        <div class="modal-body">
          <p class="text-sm text-muted" style="margin-bottom:1rem;">Upload a CSV file with columns: <strong>employeeId, date (YYYY-MM-DD), status, workingHours</strong></p>
          <div class="form-group">
            <label class="form-label required" for="csv-file">CSV File</label>
            <input type="file" class="form-control" id="csv-file" accept=".csv" required/>
            <span class="form-error" id="csv-file-err"></span>
          </div>
        </div>
        <div class="modal-footer">
          <button class="btn btn-ghost" id="csv-cancel-btn">Cancel</button>
          <button class="btn btn-primary" id="csv-submit-btn">Upload</button>
        </div>
      </div>
    </div>

    <!-- Edit/View Attendance Modal -->
    <div class="modal-backdrop" id="edit-att-modal" role="dialog" aria-modal="true" aria-labelledby="edit-att-modal-title">
      <div class="modal">
        <div class="modal-header">
          <h3 id="edit-att-modal-title">Edit Attendance</h3>
          <button class="modal-close" id="edit-att-close-btn" aria-label="Close">
            <svg fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2"><path stroke-linecap="round" stroke-linejoin="round" d="M6 18L18 6M6 6l12 12"/></svg>
          </button>
        </div>
        <div class="modal-body">
          <div id="edit-att-error" class="form-error" style="margin-bottom:1rem;font-size:13px;" role="alert"></div>
          <form id="edit-att-form" novalidate>
            <input type="hidden" id="edit-att-id" />
            <div class="form-group">
              <label class="form-label" for="edit-att-employee">Employee</label>
              <input type="text" class="form-control" id="edit-att-employee" readonly />
            </div>
            <div class="form-row">
              <div class="form-group">
                <label class="form-label required" for="edit-att-date">Date</label>
                <input type="date" class="form-control" id="edit-att-date" required max="${new Date().toISOString().split('T')[0]}"/>
                <span class="form-error" id="edit-att-date-err"></span>
              </div>
              <div class="form-group">
                <label class="form-label required" for="edit-att-status">Attendance Status</label>
                <select class="form-control" id="edit-att-status" required>
                  <option value="PRESENT">Present</option>
                  <option value="ABSENT">Absent</option>
                  <option value="PAID_LEAVE">Paid Leave</option>
                  <option value="UNPAID_LEAVE">Unpaid Leave</option>
                  <option value="HALF_DAY">Half Day</option>
                  <option value="WORK_FROM_HOME">Work From Home</option>
                </select>
              </div>
            </div>
            <div class="form-group" id="edit-att-hours-group">
              <label class="form-label required" for="edit-att-hours">Working Hours</label>
              <input type="number" class="form-control" id="edit-att-hours" min="0" max="24" step="0.5" />
              <span class="form-error" id="edit-att-hours-err"></span>
            </div>
            <div class="form-group">
              <label class="form-label" for="edit-att-remarks">Remarks</label>
              <textarea class="form-control" id="edit-att-remarks" rows="2" placeholder="Reason for change..."></textarea>
            </div>
          </form>
        </div>
        <div class="modal-footer">
          <button class="btn btn-ghost" id="edit-att-cancel-btn">Cancel</button>
          <button class="btn btn-primary" id="edit-att-submit-btn">Save Changes</button>
        </div>
      </div>
    </div>
  `;

  _bindEvents();
  _loadAttendances();
  _loadEmployeesForModal();

  // Toggle working hours based on status
  document.getElementById('att-status')?.addEventListener('change', (e) => {
    const hoursGroup = document.getElementById('att-hours-group');
    const isAbsent   = ['ABSENT', 'PAID_LEAVE', 'UNPAID_LEAVE'].includes(e.target.value);
    hoursGroup.style.display = isAbsent ? 'none' : '';
    if (isAbsent) document.getElementById('att-hours').value = '0';
    else if (!document.getElementById('att-hours').value) document.getElementById('att-hours').value = '8';
  });
}

async function _loadAttendances() {
  const tbody = document.getElementById('att-tbody');
  const isAdminOrHR = hasRole('ADMIN') || hasRole('HR');
  tbody.innerHTML = `<tr><td colspan="6"><div class="table-empty"><p>Loading…</p></div></td></tr>`;

  const params = { page: _currentPage, size: PAGE_SIZE, sort: 'attendanceDate,desc', ..._filters };
  const result = await AttApi.getAttendances(params);

  if (!result.ok) { tbody.innerHTML = `<tr><td colspan="6"><div class="table-empty"><p>Failed to load.</p></div></td></tr>`; return; }

  const records = result.data ?? [];
  const total   = result.raw?.totalCount ?? 0;
  const pages   = result.raw?.totalPages ?? 1;
  document.getElementById('att-count').textContent = total > 0 ? `${total} record${total!==1?'s':''}` : '';

  if (!records.length) {
    tbody.innerHTML = `<tr><td colspan="6"><div class="table-empty"><svg fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="1.5" style="width:48px;height:48px;margin:0 auto 1rem;opacity:.35;"><path stroke-linecap="round" stroke-linejoin="round" d="M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z"/></svg><p>No attendance records found</p></div></td></tr>`;
    document.getElementById('att-pagination').innerHTML = '';
    return;
  }

  const statusBadge = { PRESENT: 'badge-success', ABSENT: 'badge-danger', PAID_LEAVE: 'badge-warning', UNPAID_LEAVE: 'badge-warning', HALF_DAY: 'badge-info', WORK_FROM_HOME: 'badge-success' };

  tbody.innerHTML = records.map((a, i) => `
    <tr>
      <td>${((_currentPage-1)*PAGE_SIZE)+i+1}</td>
      <td id="att-emp-${a.id}"></td>
      <td>${a.attendanceDate ?? '—'}</td>
      <td><span class="badge ${statusBadge[a.attendanceStatus] ?? 'badge-gray'}">${a.attendanceStatus}</span></td>
      <td>${a.workingHours != null ? `${a.workingHours}h` : '—'}</td>
      ${isAdminOrHR ? `<td><div class="table-actions"><button class="btn btn-sm btn-ghost action-edit-att-btn" data-id="${a.id}" title="View">View</button></div></td>` : ''}
    </tr>
  `).join('');

  // Bind action buttons
  document.querySelectorAll('.action-edit-att-btn').forEach(btn => {
    btn.addEventListener('click', () => _openEditModal(btn.dataset.id));
  });

  records.forEach(a => {
    const el = document.getElementById(`att-emp-${a.id}`);
    if (el) el.textContent = a.employeeName ?? `Employee #${a.employeeId}`;
  });

  _renderPagination(pages);
}

async function _loadEmployeesForModal() {
  const sel = document.getElementById('att-employee');
  if (!sel) return;
  const isAdminOrHR = hasRole('ADMIN') || hasRole('HR');
  if (isAdminOrHR) {
    const res = await getEmployees({ size: 200, status: 'ACTIVE' });
    if (!res.ok || !res.data?.length) { sel.innerHTML = '<option value="">No active employees</option>'; return; }
    sel.innerHTML = '<option value="">Select employee…</option>' +
      res.data.map(e => `<option value="${e.id}">${e.firstName} ${e.lastName}</option>`).join('');
  } else {
    const res = await getProfile();
    if (!res.ok) { sel.innerHTML = '<option value="">No active profile found</option>'; return; }
    const emp = res.data;
    sel.innerHTML = `<option value="${emp.id}">${emp.firstName} ${emp.lastName}</option>`;
    sel.disabled = true;
  }
}

function _bindEvents() {
  document.getElementById('mark-att-btn')?.addEventListener('click',   () => openModal('att-modal'));
  document.getElementById('att-close-btn')?.addEventListener('click',  () => closeModal('att-modal'));
  document.getElementById('att-cancel-btn')?.addEventListener('click', () => closeModal('att-modal'));
  document.getElementById('upload-csv-btn')?.addEventListener('click', () => openModal('csv-modal'));
  document.getElementById('csv-close-btn')?.addEventListener('click',  () => closeModal('csv-modal'));
  document.getElementById('csv-cancel-btn')?.addEventListener('click', () => closeModal('csv-modal'));
  document.getElementById('att-submit-btn')?.addEventListener('click',  _submitAttendance);
  document.getElementById('csv-submit-btn')?.addEventListener('click',  _submitCsv);

  document.getElementById('edit-att-close-btn')?.addEventListener('click',  () => closeModal('edit-att-modal'));
  document.getElementById('edit-att-cancel-btn')?.addEventListener('click', () => closeModal('edit-att-modal'));
  document.getElementById('edit-att-submit-btn')?.addEventListener('click',  _submitEditAttendance);
  document.getElementById('edit-att-status')?.addEventListener('change', (e) => {
    const hoursGroup = document.getElementById('edit-att-hours-group');
    const isAbsent   = ['ABSENT', 'PAID_LEAVE', 'UNPAID_LEAVE'].includes(e.target.value);
    hoursGroup.style.display = isAbsent ? 'none' : '';
    if (isAbsent) document.getElementById('edit-att-hours').value = '0';
    else if (!document.getElementById('edit-att-hours').value) document.getElementById('edit-att-hours').value = '8';
  });
  document.getElementById('apply-filter-btn')?.addEventListener('click', () => {
    _filters = {
      status: document.getElementById('filter-status')?.value || undefined,
      dateFrom: document.getElementById('filter-date-from')?.value || undefined,
      dateTo:   document.getElementById('filter-date-to')?.value   || undefined,
    };
    _currentPage = 1; _loadAttendances();
  });
  document.getElementById('clear-filter-btn')?.addEventListener('click', () => {
    document.getElementById('filter-status').value = '';
    document.getElementById('filter-date-from').value = '';
    document.getElementById('filter-date-to').value = '';
    _filters = {}; _currentPage = 1; _loadAttendances();
  });
}

async function _submitAttendance() {
  const errEl = document.getElementById('att-error');
  errEl.textContent = ''; errEl.classList.remove('visible');

  const employeeId   = document.getElementById('att-employee')?.value;
  const date         = document.getElementById('att-date')?.value;
  const status       = document.getElementById('att-status')?.value;
  const workingHours = parseFloat(document.getElementById('att-hours')?.value || '0');

  let valid = true;
  if (!employeeId) { _fieldErr('att-employee-err', 'Please select an employee'); valid = false; }
  if (!date)       { _fieldErr('att-date-err',     'Date is required');          valid = false; }
  if (new Date(date) > new Date()) { _fieldErr('att-date-err', 'Date cannot be in the future'); valid = false; }
  if (!valid) return;

  const btn = document.getElementById('att-submit-btn');
  btn.disabled = true; btn.textContent = 'Saving…';

  const res = await AttApi.markAttendance({
    employeeId: parseInt(employeeId),
    attendanceDate: date,
    attendanceStatus: status,
    workingHours,
  });

  btn.disabled = false; btn.textContent = 'Mark Attendance';

  if (!res.ok) { errEl.textContent = res.message || 'Failed.'; errEl.classList.add('visible'); return; }
  showToast('success', 'Attendance Marked', res.message);
  closeModal('att-modal');
  _currentPage = 1; _loadAttendances();
}

async function _submitCsv() {
  const fileInput = document.getElementById('csv-file');
  const file = fileInput?.files?.[0];
  const errEl = document.getElementById('csv-file-err');

  if (!file) { errEl.textContent = 'Please select a CSV file'; errEl.classList.add('visible'); return; }
  if (!file.name.endsWith('.csv')) { errEl.textContent = 'Only .csv files are allowed'; errEl.classList.add('visible'); return; }

  const btn = document.getElementById('csv-submit-btn');
  btn.disabled = true; btn.textContent = 'Uploading…';
  const res = await AttApi.uploadAttendanceCsv(file);
  btn.disabled = false; btn.textContent = 'Upload';

  if (!res.ok) { showToast('error', 'Upload Failed', res.message); return; }
  showToast('success', 'CSV Uploaded', res.message);
  closeModal('csv-modal');
  fileInput.value = '';
  _currentPage = 1; _loadAttendances();
}

function _renderPagination(totalPages) {
  const c = document.getElementById('att-pagination');
  if (totalPages <= 1) { c.innerHTML = ''; return; }
  c.innerHTML = `<div class="pagination"><span class="pagination-info">Page ${_currentPage} of ${totalPages}</span><div class="pagination-controls"><button class="page-btn" id="prev-page" ${_currentPage<=1?'disabled':''} aria-label="Previous"><svg fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2"><polyline points="15 18 9 12 15 6"/></svg></button><span class="page-btn active">${_currentPage}</span><button class="page-btn" id="next-page" ${_currentPage>=totalPages?'disabled':''} aria-label="Next"><svg fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2"><polyline points="9 18 15 12 9 6"/></svg></button></div></div>`;
  document.getElementById('prev-page')?.addEventListener('click', () => { _currentPage--; _loadAttendances(); });
  document.getElementById('next-page')?.addEventListener('click', () => { _currentPage++; _loadAttendances(); });
}

function _fieldErr(id, msg) {
  const el = document.getElementById(id);
  if (el) { el.textContent = msg; el.classList.add('visible'); }
}

async function _openEditModal(id) {
  const errEl = document.getElementById('edit-att-error');
  if (errEl) { errEl.textContent = ''; errEl.classList.remove('visible'); }
  _clearEditFieldErrors();

  const res = await AttApi.getAttendance(id);
  if (!res.ok) {
    showToast('error', 'Error', 'Failed to fetch attendance details');
    return;
  }

  const att = res.data;
  document.getElementById('edit-att-id').value = att.id;
  document.getElementById('edit-att-employee').value = att.employeeName ?? `Employee #${att.employeeId}`;
  document.getElementById('edit-att-date').value = att.attendanceDate;
  document.getElementById('edit-att-status').value = att.attendanceStatus;
  document.getElementById('edit-att-hours').value = att.workingHours ?? '0';
  document.getElementById('edit-att-remarks').value = att.remarks ?? '';

  const hoursGroup = document.getElementById('edit-att-hours-group');
  const isAbsent = ['ABSENT', 'PAID_LEAVE', 'UNPAID_LEAVE'].includes(att.attendanceStatus);
  if (hoursGroup) hoursGroup.style.display = isAbsent ? 'none' : '';

  openModal('edit-att-modal');
}

async function _submitEditAttendance() {
  const errEl = document.getElementById('edit-att-error');
  if (errEl) { errEl.textContent = ''; errEl.classList.remove('visible'); }
  _clearEditFieldErrors();

  const id           = document.getElementById('edit-att-id').value;
  const date         = document.getElementById('edit-att-date').value;
  const status       = document.getElementById('edit-att-status').value;
  const workingHours = parseFloat(document.getElementById('edit-att-hours').value || '0');
  const remarks      = document.getElementById('edit-att-remarks').value;

  let valid = true;
  if (!date) { _fieldErr('edit-att-date-err', 'Date is required'); valid = false; }
  if (new Date(date) > new Date()) { _fieldErr('edit-att-date-err', 'Date cannot be in the future'); valid = false; }
  if (!valid) return;

  const btn = document.getElementById('edit-att-submit-btn');
  btn.disabled = true; btn.textContent = 'Saving…';

  const res = await AttApi.updateAttendance(id, {
    attendanceDate: date,
    attendanceStatus: status,
    workingHours,
    remarks
  });

  btn.disabled = false; btn.textContent = 'Save Changes';

  if (!res.ok) { 
    if (errEl) { errEl.textContent = res.message || 'Failed to update attendance'; errEl.classList.add('visible'); }
    return; 
  }
  showToast('success', 'Attendance Updated', res.message);
  closeModal('edit-att-modal');
  _loadAttendances();
}

function _clearEditFieldErrors() {
  document.querySelectorAll('#edit-att-form .form-error').forEach(el => {
    el.textContent = ''; el.classList.remove('visible');
  });
}
