/**
 * departments.page.js — Department Management Page
 */
import * as DeptApi from '../api/department.api.js';
import { showToast }  from '../components/toast.js';
import { openModal, closeModal, openConfirm } from '../components/modal.js';
import { hasRole } from '../auth/auth.store.js';

const PAGE_SIZE = 10;
let _currentPage = 1;
let _filters     = {};
let _editingId   = null;

export async function render() {
  const content = document.getElementById('page-content');
  if (!content) return;
  const isAdmin = hasRole('ADMIN');

  content.innerHTML = `
    <div class="page-header">
      <div><h1 class="page-title">Departments</h1><p class="page-subtitle">Manage company departments</p></div>
      <div class="page-actions">
        ${isAdmin ? `<button class="btn btn-primary" id="add-dept-btn">
          <svg fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2" style="width:16px;height:16px;"><path stroke-linecap="round" stroke-linejoin="round" d="M12 4v16m8-8H4"/></svg>
          Add Department
        </button>` : ''}
      </div>
    </div>

    <!-- Filter -->
    <div class="card" style="margin-bottom:1.5rem;">
      <div class="card-body-sm">
        <div class="filter-bar">
          <div class="form-group" style="flex:2;">
            <label class="form-label">Search</label>
            <div class="input-group">
              <span class="input-group-icon"><svg fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2"><circle cx="11" cy="11" r="8"/><path d="M21 21l-4.35-4.35"/></svg></span>
              <input type="text" class="form-control" id="filter-search" placeholder="Search by name or location…" maxlength="100"/>
            </div>
          </div>
          <button class="btn btn-primary btn-sm" id="apply-filter-btn">Search</button>
          <button class="btn btn-ghost btn-sm"   id="clear-filter-btn">Clear</button>
        </div>
      </div>
    </div>

    <!-- Table -->
    <div class="card">
      <div class="card-header"><h2>Department List</h2><span id="dept-count" class="text-sm text-muted"></span></div>
      <div class="table-container">
        <table class="table">
          <thead><tr><th>#</th><th>Name</th><th>Location</th><th>Description</th><th>Status</th>${isAdmin ? '<th>Actions</th>' : ''}</tr></thead>
          <tbody id="dept-tbody"><tr><td colspan="6"><div class="table-empty"><p>Loading…</p></div></td></tr></tbody>
        </table>
      </div>
      <div id="dept-pagination"></div>
    </div>

    <!-- Add/Edit Modal -->
    <div class="modal-backdrop" id="dept-modal" role="dialog" aria-modal="true" aria-labelledby="dept-modal-title">
      <div class="modal">
        <div class="modal-header">
          <h3 id="dept-modal-title">Add Department</h3>
          <button class="modal-close" id="dept-close-btn" aria-label="Close">
            <svg fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2"><path stroke-linecap="round" stroke-linejoin="round" d="M6 18L18 6M6 6l12 12"/></svg>
          </button>
        </div>
        <div class="modal-body">
          <div id="dept-error" class="form-error" style="margin-bottom:1rem;font-size:13px;" role="alert"></div>
          <form id="dept-form" novalidate>
            <div class="form-group">
              <label class="form-label required" for="dept-name">Department Name</label>
              <input type="text" class="form-control" id="dept-name" required maxlength="100"/>
              <span class="form-error" id="dept-name-err"></span>
            </div>
            <div class="form-group">
              <label class="form-label required" for="dept-location">Location</label>
              <input type="text" class="form-control" id="dept-location" required maxlength="100"/>
              <span class="form-error" id="dept-location-err"></span>
            </div>
            <div class="form-group">
              <label class="form-label" for="dept-desc">Description</label>
              <textarea class="form-control" id="dept-desc" maxlength="250" rows="3"></textarea>
            </div>
          </form>
        </div>
        <div class="modal-footer">
          <button class="btn btn-ghost" id="dept-cancel-btn">Cancel</button>
          <button class="btn btn-primary" id="dept-submit-btn">Save Department</button>
        </div>
      </div>
    </div>
  `;

  _bindEvents();
  _loadDepartments();
}

async function _loadDepartments() {
  const tbody = document.getElementById('dept-tbody');
  const isAdmin = hasRole('ADMIN');
  tbody.innerHTML = `<tr><td colspan="6"><div class="table-empty"><p>Loading…</p></div></td></tr>`;

  const params = { page: _currentPage, size: PAGE_SIZE, ..._filters };
  const result = await DeptApi.getDepartments(params);
  if (!result.ok) { tbody.innerHTML = `<tr><td colspan="6"><div class="table-empty"><p>Failed to load.</p></div></td></tr>`; return; }

  const records = result.data ?? [];
  const total   = result.raw?.totalCount ?? 0;
  const pages   = result.raw?.totalPages ?? 1;
  document.getElementById('dept-count').textContent = total > 0 ? `${total} department${total!==1?'s':''}` : '';

  if (!records.length) {
    tbody.innerHTML = `<tr><td colspan="6"><div class="table-empty"><svg fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="1.5" style="width:48px;height:48px;margin:0 auto 1rem;opacity:.35;"><path stroke-linecap="round" stroke-linejoin="round" d="M19 21V5a2 2 0 00-2-2H7a2 2 0 00-2 2v16m14 0h2m-2 0h-5m-9 0H3m2 0h5M9 7h1m-1 4h1m4-4h1m-1 4h1m-5 10v-5a1 1 0 011-1h2a1 1 0 011 1v5m-4 0h4"/></svg><p>No departments found</p></div></td></tr>`;
    document.getElementById('dept-pagination').innerHTML = '';
    return;
  }

  tbody.innerHTML = records.map((d, i) => `
    <tr>
      <td>${((_currentPage-1)*PAGE_SIZE)+i+1}</td>
      <td class="font-semibold" id="dname-${d.id}"></td>
      <td id="dloc-${d.id}"></td>
      <td id="ddesc-${d.id}" style="max-width:200px;overflow:hidden;text-overflow:ellipsis;"></td>
      <td><span class="badge ${d.active ? 'badge-success' : 'badge-danger'}">${d.active ? 'Active' : 'Inactive'}</span></td>
      ${isAdmin ? `
      <td>
        <div class="table-actions">
          <button class="btn btn-sm btn-outline action-edit-dept" data-id="${d.id}" title="Edit">
            <svg fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2" style="width:14px;height:14px;"><path stroke-linecap="round" stroke-linejoin="round" d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z"/></svg>
          </button>
          <button class="btn btn-sm ${d.active ? 'btn-danger-outline' : 'btn-success'} action-toggle-dept"
            data-id="${d.id}" data-active="${d.active}" title="${d.active ? 'Deactivate' : 'Activate'}">
            ${d.active ? 'Deactivate' : 'Activate'}
          </button>
        </div>
      </td>` : ''}
    </tr>
  `).join('');

  // Safe injection
  records.forEach(d => {
    const nameEl = document.getElementById(`dname-${d.id}`);
    const locEl  = document.getElementById(`dloc-${d.id}`);
    const descEl = document.getElementById(`ddesc-${d.id}`);
    if (nameEl) nameEl.textContent = d.name ?? '';
    if (locEl)  locEl.textContent  = d.location ?? '';
    if (descEl) descEl.textContent = d.description ?? '—';
  });

  // Action bindings
  document.querySelectorAll('.action-edit-dept').forEach(btn => {
    btn.addEventListener('click', async () => {
      _editingId = btn.dataset.id;
      const res  = await DeptApi.getDepartment(_editingId);
      if (!res.ok) return;
      const d = res.data;
      document.getElementById('dept-modal-title').textContent = 'Edit Department';
      document.getElementById('dept-name').value     = d.name ?? '';
      document.getElementById('dept-location').value = d.location ?? '';
      document.getElementById('dept-desc').value     = d.description ?? '';
      openModal('dept-modal');
    });
  });

  document.querySelectorAll('.action-toggle-dept').forEach(btn => {
    btn.addEventListener('click', async () => {
      const active   = btn.dataset.active === 'true';
      const newState = !active;
      const confirmed = await openConfirm({
        title:   newState ? 'Activate Department' : 'Deactivate Department',
        message: newState ? 'This will activate the department.' : 'This will deactivate the department.',
        type:    newState ? 'warning' : 'danger',
        confirmLabel: newState ? 'Activate' : 'Deactivate',
      });
      if (!confirmed) return;
      const res = await DeptApi.changeDepartmentStatus(btn.dataset.id, newState);
      if (res.ok) { showToast('success', 'Updated', res.message); _loadDepartments(); }
      else showToast('error', 'Failed', res.message);
    });
  });

  _renderPagination(pages);
}

function _bindEvents() {
  document.getElementById('add-dept-btn')?.addEventListener('click', () => {
    _editingId = null;
    document.getElementById('dept-modal-title').textContent = 'Add Department';
    document.getElementById('dept-form')?.reset();
    openModal('dept-modal');
  });
  document.getElementById('dept-close-btn')?.addEventListener('click',  () => closeModal('dept-modal'));
  document.getElementById('dept-cancel-btn')?.addEventListener('click', () => closeModal('dept-modal'));
  document.getElementById('dept-submit-btn')?.addEventListener('click', _submitDept);
  document.getElementById('apply-filter-btn')?.addEventListener('click', () => {
    _filters = { search: document.getElementById('filter-search')?.value.trim() || undefined };
    _currentPage = 1; _loadDepartments();
  });
  document.getElementById('clear-filter-btn')?.addEventListener('click', () => {
    document.getElementById('filter-search').value = '';
    _filters = {}; _currentPage = 1; _loadDepartments();
  });
}

async function _submitDept() {
  const errEl = document.getElementById('dept-error');
  errEl.textContent = ''; errEl.classList.remove('visible');

  const name     = document.getElementById('dept-name')?.value.trim();
  const location = document.getElementById('dept-location')?.value.trim();
  const description = document.getElementById('dept-desc')?.value.trim();

  let valid = true;
  if (!name)     { _fieldErr('dept-name-err',     'Department name is required'); valid = false; }
  if (!location) { _fieldErr('dept-location-err', 'Location is required');        valid = false; }
  if (!valid) return;

  const btn = document.getElementById('dept-submit-btn');
  btn.disabled = true; btn.textContent = 'Saving…';

  const body = { name, location, description };
  const res  = _editingId ? await DeptApi.updateDepartment(_editingId, body) : await DeptApi.createDepartment(body);

  btn.disabled = false; btn.textContent = 'Save Department';

  if (!res.ok) { errEl.textContent = res.message || 'Failed.'; errEl.classList.add('visible'); return; }
  showToast('success', _editingId ? 'Updated' : 'Created', res.message);
  closeModal('dept-modal');
  _currentPage = 1; _loadDepartments();
}

function _renderPagination(totalPages) {
  const c = document.getElementById('dept-pagination');
  if (totalPages <= 1) { c.innerHTML = ''; return; }
  c.innerHTML = `<div class="pagination"><span class="pagination-info">Page ${_currentPage} of ${totalPages}</span><div class="pagination-controls"><button class="page-btn" id="prev-page" ${_currentPage<=1?'disabled':''} aria-label="Previous"><svg fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2"><polyline points="15 18 9 12 15 6"/></svg></button><span class="page-btn active">${_currentPage}</span><button class="page-btn" id="next-page" ${_currentPage>=totalPages?'disabled':''} aria-label="Next"><svg fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2"><polyline points="9 18 15 12 9 6"/></svg></button></div></div>`;
  document.getElementById('prev-page')?.addEventListener('click', () => { _currentPage--; _loadDepartments(); });
  document.getElementById('next-page')?.addEventListener('click', () => { _currentPage++; _loadDepartments(); });
}

function _fieldErr(id, msg) {
  const el = document.getElementById(id);
  if (el) { el.textContent = msg; el.classList.add('visible'); }
}
