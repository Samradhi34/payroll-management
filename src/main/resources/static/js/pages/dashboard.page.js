/**
 * dashboard.page.js — Dashboard Overview
 *
 * Displays stat cards: total employees, departments, payrolls, attendance.
 * Shows quick-action buttons based on user role.
 * Data loaded in parallel for performance.
 */

import { getEmployees }   from '../api/employee.api.js';
import { getDepartments } from '../api/department.api.js';
import { getPayrolls }    from '../api/payroll.api.js';
import { getAttendances, checkIn, checkOut, getTodayAttendance } from '../api/attendance.api.js';
import { getPrimaryRole, getUsername } from '../auth/auth.store.js';
import { navigate }       from '../router.js';
import { showToast }      from '../components/toast.js';

let _timerInterval = null;

export async function render() {
  const content = document.getElementById('page-content');
  if (!content) return;

  if (_timerInterval) {
    clearInterval(_timerInterval);
    _timerInterval = null;
  }

  const role = getPrimaryRole();

  content.innerHTML = `
    <div class="page-header">
      <div>
        <h1 class="page-title">Dashboard</h1>
        <p class="page-subtitle">Welcome back, <strong id="dash-username"></strong></p>
      </div>
      ${role !== 'EMPLOYEE' ? `
        <div class="page-actions">
          <button class="btn btn-outline btn-sm" onclick="window.location.hash='#/employees'">
            View Employees
          </button>
          <button class="btn btn-primary btn-sm" onclick="window.location.hash='#/payroll'">
            Manage Payroll
          </button>
        </div>` : ''}
    </div>

    <!-- Check-In/Check-Out Widget -->
    <div id="check-in-out-container" style="margin-bottom:1.5rem;"></div>

    <!-- Stats grid (skeleton while loading) -->
    <div class="stats-grid" id="stats-grid">
      ${_skeletonCard()} ${_skeletonCard()} ${_skeletonCard()} ${_skeletonCard()}
    </div>

    <!-- Recent activity (role-based) -->
    ${role !== 'EMPLOYEE' ? `
    <div class="card">
      <div class="card-header">
        <h2>Quick Actions</h2>
      </div>
      <div class="card-body">
        <div style="display:flex; gap:12px; flex-wrap:wrap;">
          <button class="btn btn-primary" onclick="window.location.hash='#/employees'">
            <svg fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2" style="width:16px;height:16px;">
              <path stroke-linecap="round" stroke-linejoin="round" d="M12 4v16m8-8H4"/>
            </svg>
            New Employee
          </button>
          <button class="btn btn-outline" onclick="window.location.hash='#/attendance'">
            <svg fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2" style="width:16px;height:16px;">
              <path stroke-linecap="round" stroke-linejoin="round" d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z"/>
            </svg>
            Mark Attendance
          </button>
          <button class="btn btn-outline" onclick="window.location.hash='#/payroll'">
            <svg fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2" style="width:16px;height:16px;">
              <path stroke-linecap="round" stroke-linejoin="round" d="M9 14l6-6m-5.5.5h.01m4.99 5h.01M19 21V5a2 2 0 00-2-2H7a2 2 0 00-2 2v16l3.5-2 3.5 2 3.5-2 3.5 2z"/>
            </svg>
            Generate Payroll
          </button>
        </div>
      </div>
    </div>` : ''}
  `;

  document.getElementById('dash-username').textContent = getUsername();

  // Initialize Check-In/Check-Out widget
  _initCheckInOut();

  // Load stats in parallel — non-blocking
  _loadStats(role);
}

async function _loadStats(role) {
  if (role === 'EMPLOYEE') {
    const grid = document.getElementById('stats-grid');
    grid.innerHTML = `<div class="table-empty"><p>Loading profile details...</p></div>`;
    
    const { getProfile } = await import('../api/employee.api.js');
    const result = await getProfile();
    
    if (!result.ok) {
      grid.innerHTML = `
        <div class="card" style="grid-column: 1 / -1; border-left: 4px solid #EF4444; width: 100%;">
          <div class="card-body">
            <h3 class="text-danger" style="margin-top:0;">Profile Not Found</h3>
            <p class="text-muted" style="margin-bottom:0;">An employee record has not yet been linked to your user account by the Admin. Please contact your HR department to complete onboarding with your registered email.</p>
          </div>
        </div>
      `;
      return;
    }
    
    const emp = result.data;
    
    grid.style.display = 'block'; // Full width layout for the profile card
    grid.innerHTML = `
      <div class="card" style="max-width: 800px; margin-bottom: 2rem;">
        <div class="card-header">
          <h2>My Profile Details</h2>
          <span class="badge badge-${emp.employeeStatus}">${emp.employeeStatus}</span>
        </div>
        <div class="card-body">
          <div style="display: flex; gap: 2rem; align-items: center; flex-wrap: wrap;">
            <div style="width: 100px; height: 100px; border-radius: 50%; overflow: hidden; background: #E5E7EB; display: flex; align-items: center; justify-content: center; border: 3px solid #4F46E5; flex-shrink: 0;">
              ${emp.profileImagePath ? `
                <img src="${emp.profileImagePath}" alt="Profile Image" style="width:100%; height:100%; object-fit:cover;"/>
              ` : `
                <svg fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="1.5" style="width:50px; height:50px; color:#9CA3AF;"><path stroke-linecap="round" stroke-linejoin="round" d="M15.75 6a3.75 3.75 0 11-7.5 0 3.75 3.75 0 017.5 0zM4.501 20.118a7.5 7.5 0 0114.998 0A17.933 17.933 0 0112 21.75c-2.676 0-5.216-.584-7.499-1.632z"/></svg>
              `}
            </div>
            <div style="flex: 1; display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 1rem 1.5rem;">
              <div>
                <p class="text-xs text-muted" style="margin:0 0 2px 0; text-transform:uppercase; font-size:10px; font-weight:700; letter-spacing:0.5px;">Full Name</p>
                <p style="margin:0; font-weight:600; font-size:15px; color:#1F2937;">${emp.firstName} ${emp.lastName}</p>
              </div>
              <div>
                <p class="text-xs text-muted" style="margin:0 0 2px 0; text-transform:uppercase; font-size:10px; font-weight:700; letter-spacing:0.5px;">Designation</p>
                <p style="margin:0; font-weight:600; font-size:15px; color:#1F2937;">${emp.designation}</p>
              </div>
              <div>
                <p class="text-xs text-muted" style="margin:0 0 2px 0; text-transform:uppercase; font-size:10px; font-weight:700; letter-spacing:0.5px;">Department</p>
                <p style="margin:0; font-weight:600; font-size:15px; color:#1F2937;">${emp.departmentName ?? '—'}</p>
              </div>
              <div>
                <p class="text-xs text-muted" style="margin:0 0 2px 0; text-transform:uppercase; font-size:10px; font-weight:700; letter-spacing:0.5px;">Base Salary</p>
                <p style="margin:0; font-weight:600; font-size:15px; color:#1F2937;">₹${parseFloat(emp.baseSalary).toLocaleString('en-IN', { minimumFractionDigits: 2 })}</p>
              </div>
              <div>
                <p class="text-xs text-muted" style="margin:0 0 2px 0; text-transform:uppercase; font-size:10px; font-weight:700; letter-spacing:0.5px;">Email Address</p>
                <p style="margin:0; font-weight:600; font-size:15px; color:#1F2937;">${emp.email}</p>
              </div>
              <div>
                <p class="text-xs text-muted" style="margin:0 0 2px 0; text-transform:uppercase; font-size:10px; font-weight:700; letter-spacing:0.5px;">Phone Number</p>
                <p style="margin:0; font-weight:600; font-size:15px; color:#1F2937;">${emp.phone}</p>
              </div>
              <div>
                <p class="text-xs text-muted" style="margin:0 0 2px 0; text-transform:uppercase; font-size:10px; font-weight:700; letter-spacing:0.5px;">Joining Date</p>
                <p style="margin:0; font-weight:600; font-size:15px; color:#1F2937;">${new Date(emp.joiningDate).toLocaleDateString('en-IN', { day:'2-digit', month:'short', year:'numeric' })}</p>
              </div>
            </div>
          </div>
        </div>
      </div>
    `;
    return;
  }

  const [empRes, deptRes, payRes, attRes] = await Promise.allSettled([
    getEmployees({ size: 1 }, true),
    getDepartments({ size: 1 }, true),
    getPayrolls({ size: 1 }, true),
    getAttendances({ size: 1 }, true),
  ]);

  const emp  = empRes.status  === 'fulfilled' ? (empRes.value.raw?.totalCount  ?? '—') : '—';
  const dept = deptRes.status === 'fulfilled' ? (deptRes.value.raw?.totalCount ?? '—') : '—';
  const pay  = payRes.status  === 'fulfilled' ? (payRes.value.raw?.totalCount  ?? '—') : '—';
  const att  = attRes.status  === 'fulfilled' ? (attRes.value.raw?.totalCount  ?? '—') : '—';

  document.getElementById('stats-grid').innerHTML = `
    ${_statCard('Total Employees', emp, 'Across all departments', '#4F46E5', 'rgba(79,70,229,0.12)',
      `<path stroke-linecap="round" stroke-linejoin="round" d="M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0z"/>`,
      '#/employees')}
    ${_statCard('Departments', dept, 'Active departments', '#10B981', 'rgba(16,185,129,0.12)',
      `<path stroke-linecap="round" stroke-linejoin="round" d="M19 21V5a2 2 0 00-2-2H7a2 2 0 00-2 2v16m14 0h2m-2 0h-5m-9 0H3m2 0h5M9 7h1m-1 4h1m4-4h1m-1 4h1m-5 10v-5a1 1 0 011-1h2a1 1 0 011 1v5m-4 0h4"/>`,
      '#/departments')}
    ${_statCard('Payroll Records', pay, 'Total payroll entries', '#F59E0B', 'rgba(245,158,11,0.12)',
      `<path stroke-linecap="round" stroke-linejoin="round" d="M9 14l6-6m-5.5.5h.01m4.99 5h.01M19 21V5a2 2 0 00-2-2H7a2 2 0 00-2 2v16l3.5-2 3.5 2 3.5-2 3.5 2z"/>`,
      '#/payroll')}
    ${_statCard('Attendance Entries', att, 'Total recorded entries', '#3B82F6', 'rgba(59,130,246,0.12)',
      `<path stroke-linecap="round" stroke-linejoin="round" d="M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z"/>`,
      '#/attendance')}
  `;
}

function _statCard(label, value, sub, accent, iconBg, iconPath, link) {
  return `
    <div class="stat-card" style="--card-accent:${accent}; --card-icon-bg:${iconBg}; --card-icon-color:${accent}; cursor:pointer;"
         onclick="window.location.hash='${link}'" role="link" tabindex="0" aria-label="${label}: ${value}">
      <div>
        <p class="stat-card-label">${label}</p>
        <p class="stat-card-value" id="stat-${label.replace(/\s+/g,'-').toLowerCase()}">${value}</p>
        <p class="stat-card-sub">${sub}</p>
      </div>
      <div class="stat-card-icon">
        <svg fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">${iconPath}</svg>
      </div>
    </div>
  `;
}

function _skeletonCard() {
  return `
    <div class="stat-card">
      <div style="flex:1;">
        <div class="skeleton" style="height:12px; width:60%; margin-bottom:10px;"></div>
        <div class="skeleton" style="height:28px; width:40%; margin-bottom:8px;"></div>
        <div class="skeleton" style="height:10px; width:70%;"></div>
      </div>
      <div class="skeleton" style="width:48px; height:48px; border-radius:8px; flex-shrink:0;"></div>
    </div>
  `;
}

async function _initCheckInOut() {
  const container = document.getElementById('check-in-out-container');
  if (!container) return;

  container.innerHTML = `
    <div class="card check-in-card">
      <div class="card-body" style="padding:1.25rem 1.5rem;">
        <p class="text-sm text-muted" style="margin:0;">Loading time tracking status...</p>
      </div>
    </div>
  `;

  const res = await getTodayAttendance();
  if (!res.ok) {
    container.innerHTML = `
      <div class="card check-in-card" style="border-left-color: #EF4444;">
        <div class="card-body" style="padding:1.25rem 1.5rem;">
          <p class="text-sm text-danger" style="margin:0;">Failed to load time tracking status. Please refresh the page.</p>
        </div>
      </div>
    `;
    return;
  }

  const att = res.data; // AttendanceResponseDto or null

  if (!att) {
    // NOT CHECKED IN
    container.innerHTML = `
      <div class="card check-in-card" style="border-left-color: var(--color-primary);">
        <div class="card-body" style="display:flex; justify-content:space-between; align-items:center; flex-wrap:wrap; gap:1rem; padding:1.25rem 1.5rem;">
          <div style="display:flex; align-items:center; gap:1rem;">
            <div class="check-in-clock-icon">
              <svg fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2" style="width:24px; height:24px;">
                <path stroke-linecap="round" stroke-linejoin="round" d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" />
              </svg>
            </div>
            <div>
              <h3 style="margin:0 0 2px 0; font-size:15px; font-weight:600; color:var(--text-primary);">Time Tracking</h3>
              <p class="text-xs text-muted" style="margin:0;">You are not checked in for today.</p>
            </div>
          </div>
          <button class="btn btn-primary" id="btn-dashboard-checkin">
            <svg fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2" style="width:16px;height:16px;margin-right:6px;"><path stroke-linecap="round" stroke-linejoin="round" d="M11 16l-4-4m0 0l4-4m-4 4h14m-5 4v1a3 3 0 01-3 3H6a3 3 0 01-3-3V7a3 3 0 013-3h7a3 3 0 013 3v1"/></svg>
            Check In
          </button>
        </div>
      </div>
    `;

    document.getElementById('btn-dashboard-checkin')?.addEventListener('click', async () => {
      const btn = document.getElementById('btn-dashboard-checkin');
      btn.disabled = true;
      btn.textContent = 'Checking In...';
      const actionRes = await checkIn();
      if (actionRes.ok) {
        showToast('success', 'Checked In', 'You have successfully checked in for today.');
        _initCheckInOut();
      } else {
        showToast('error', 'Check In Failed', actionRes.message);
        btn.disabled = false;
        btn.innerHTML = `<svg fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2" style="width:16px;height:16px;margin-right:6px;"><path stroke-linecap="round" stroke-linejoin="round" d="M11 16l-4-4m0 0l4-4m-4 4h14m-5 4v1a3 3 0 01-3 3H6a3 3 0 01-3-3V7a3 3 0 013-3h7a3 3 0 013 3v1"/></svg> Check In`;
      }
    });
  } else if (att.checkInTime && !att.checkOutTime) {
    // CHECKED IN
    container.innerHTML = `
      <div class="card check-in-card" style="border-left-color: #10B981;">
        <div class="card-body" style="display:flex; justify-content:space-between; align-items:center; flex-wrap:wrap; gap:1.5rem; padding:1.25rem 1.5rem;">
          <div style="display:flex; align-items:center; gap:1rem;">
            <div class="check-in-clock-icon check-in-clock-active">
              <svg fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2" style="width:24px; height:24px;">
                <path stroke-linecap="round" stroke-linejoin="round" d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" />
              </svg>
            </div>
            <div>
              <h3 style="margin:0 0 2px 0; font-size:15px; font-weight:600; color:var(--text-primary);">Checked In</h3>
              <p class="text-xs text-muted" style="margin:0;">Shift started at <strong id="dashboard-checkin-time">${_formatLocalTime(att.checkInTime)}</strong></p>
            </div>
          </div>
          <div style="display:flex; align-items:center; gap:1.5rem;">
            <div style="text-align:right;">
              <span class="text-xs text-muted" style="display:block; font-size:10px; text-transform:uppercase; font-weight:700; letter-spacing:0.5px; margin-bottom:2px;">Working Duration</span>
              <span id="elapsed-time-counter" style="font-family:monospace; font-size:20px; font-weight:700; color:#10B981;">00:00:00</span>
            </div>
            <button class="btn btn-danger" id="btn-dashboard-checkout">
              <svg fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2" style="width:16px;height:16px;margin-right:6px;"><path stroke-linecap="round" stroke-linejoin="round" d="M17 16l4-4m0 0l-4-4m4 4H7m6 4v1a3 3 0 01-3 3H6a3 3 0 01-3-3V7a3 3 0 013-3h4a3 3 0 013 3v1"/></svg>
              Check Out
            </button>
          </div>
        </div>
      </div>
    `;

    // Start timer counter
    const checkInDate = _parseLocalTime(att.checkInTime);
    if (checkInDate) {
      const updateTimer = () => {
        const elapsedMs = new Date() - checkInDate;
        if (elapsedMs < 0) return;
        const secs = Math.floor(elapsedMs / 1000) % 60;
        const mins = Math.floor(elapsedMs / (1000 * 60)) % 60;
        const hours = Math.floor(elapsedMs / (1000 * 60 * 60));
        const el = document.getElementById('elapsed-time-counter');
        if (el) {
          el.textContent = `${String(hours).padStart(2, '0')}:${String(mins).padStart(2, '0')}:${String(secs).padStart(2, '0')}`;
        } else {
          clearInterval(_timerInterval);
        }
      };
      updateTimer();
      _timerInterval = setInterval(updateTimer, 1000);
    }

    document.getElementById('btn-dashboard-checkout')?.addEventListener('click', async () => {
      const btn = document.getElementById('btn-dashboard-checkout');
      btn.disabled = true;
      btn.textContent = 'Checking Out...';
      const actionRes = await checkOut();
      if (actionRes.ok) {
        if (_timerInterval) {
          clearInterval(_timerInterval);
          _timerInterval = null;
        }
        showToast('success', 'Checked Out', 'You have successfully checked out for today.');
        _initCheckInOut();
      } else {
        showToast('error', 'Check Out Failed', actionRes.message);
        btn.disabled = false;
        btn.innerHTML = `<svg fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2" style="width:16px;height:16px;margin-right:6px;"><path stroke-linecap="round" stroke-linejoin="round" d="M17 16l4-4m0 0l-4-4m4 4H7m6 4v1a3 3 0 01-3 3H6a3 3 0 01-3-3V7a3 3 0 013-3h4a3 3 0 013 3v1"/></svg> Check Out`;
      }
    });
  } else {
    // CHECKED OUT
    container.innerHTML = `
      <div class="card check-in-card" style="border-left-color: var(--text-secondary); background: #F9FAFB;">
        <div class="card-body" style="display:flex; justify-content:space-between; align-items:center; flex-wrap:wrap; gap:1rem; padding:1.25rem 1.5rem;">
          <div style="display:flex; align-items:center; gap:1rem;">
            <div class="check-in-clock-icon" style="background:#E5E7EB; color:#6B7280;">
              <svg fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2" style="width:24px; height:24px;">
                <path stroke-linecap="round" stroke-linejoin="round" d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
              </svg>
            </div>
            <div>
              <h3 style="margin:0 0 2px 0; font-size:15px; font-weight:600; color:var(--text-secondary);">Completed for Today</h3>
              <p class="text-xs text-muted" style="margin:0;">
                Status: <strong>${att.attendanceStatus}</strong>
                ${att.checkInTime ? ` | In: <strong>${_formatLocalTime(att.checkInTime)}</strong>` : ''}
                ${att.checkOutTime ? ` | Out: <strong>${_formatLocalTime(att.checkOutTime)}</strong>` : ''}
              </p>
            </div>
          </div>
          <div style="text-align:right;">
            <span class="text-xs text-muted" style="display:block; font-size:10px; text-transform:uppercase; font-weight:700; letter-spacing:0.5px; margin-bottom:2px;">Total Hours</span>
            <span style="font-size:18px; font-weight:700; color:var(--text-secondary);" id="dashboard-checkout-hours">${att.workingHours != null ? att.workingHours + 'h' : '—'}</span>
          </div>
        </div>
      </div>
    `;
  }
}

function _parseLocalTime(ltVal) {
  if (!ltVal) return null;
  const now = new Date();
  if (Array.isArray(ltVal)) {
    return new Date(now.getFullYear(), now.getMonth(), now.getDate(), ltVal[0], ltVal[1], ltVal[2] || 0);
  }
  if (typeof ltVal === 'string') {
    const parts = ltVal.split(':');
    if (parts.length >= 2) {
      return new Date(now.getFullYear(), now.getMonth(), now.getDate(), parseInt(parts[0], 10), parseInt(parts[1], 10), parseInt(parts[2] || '0', 10));
    }
  }
  return null;
}

function _formatLocalTime(ltVal) {
  if (!ltVal) return '—';
  if (Array.isArray(ltVal)) {
    const h = ltVal[0];
    const m = ltVal[1];
    const ampm = h >= 12 ? 'PM' : 'AM';
    const displayH = h % 12 || 12;
    const displayM = String(m).padStart(2, '0');
    return `${displayH}:${displayM} ${ampm}`;
  }
  if (typeof ltVal === 'string') {
    const parts = ltVal.split(':');
    if (parts.length >= 2) {
      const h = parseInt(parts[0], 10);
      const m = parseInt(parts[1], 10);
      const ampm = h >= 12 ? 'PM' : 'AM';
      const displayH = h % 12 || 12;
      const displayM = String(m).padStart(2, '0');
      return `${displayH}:${displayM} ${ampm}`;
    }
  }
  return String(ltVal);
}

