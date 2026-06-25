/**
 * payroll.api.js — Payroll & SalarySlip API calls
 * Maps all PayrollController and salary-slip endpoints
 */
import { get, post, patch, buildQuery } from './http.js';

/* ── Payroll ─────────────────────────────────────────────────────────────── */

/**
 * Get paginated list of all payrolls.
 * @param {Object} params - { page, size, sort, month, year, status, employeeId }
 */
export function getPayrolls(params = {}) {
  return get(`/payrolls${buildQuery(params)}`);
}

/** @param {number} id */
export function getPayroll(id) {
  return get(`/payrolls/${id}`);
}

/** @param {number} employeeId */
export function getPayrollsByEmployee(employeeId) {
  return get(`/payrolls/employee/${employeeId}`);
}

/**
 * @param {number} month 1-12
 * @param {number} year  e.g. 2024
 */
export function getPayrollsByMonthYear(month, year) {
  return get(`/payrolls/month-year?month=${month}&year=${year}`);
}

/**
 * Generate a new payroll record.
 * @param {{ employeeId, month, year, bonus, deductions }} body
 */
export function generatePayroll(body) {
  return post('/payrolls', body);
}

/**
 * Generate payroll for all active employees.
 * @param {{ month, year, bonus, deductions }} body
 */
export function generateAllPayrolls(body) {
  return post('/payrolls/generate-all', body);
}

/** Approve: GENERATED → APPROVED */
export function approvePayroll(id) {
  return patch(`/payrolls/${id}/approve`);
}

/** Pay: APPROVED → PAID */
export function payPayroll(id) {
  return patch(`/payrolls/${id}/pay`);
}

/** Cancel: GENERATED|APPROVED → CANCELLED */
export function cancelPayroll(id) {
  return patch(`/payrolls/${id}/cancel`);
}

/* ── Salary Slip ─────────────────────────────────────────────────────────── */

/** @param {number} payrollId */
export function generateSalarySlip(payrollId) {
  return post(`/payrolls/${payrollId}/salary-slip`);
}

/** @param {number} payrollId */
export function getSalarySlip(payrollId) {
  return get(`/payrolls/${payrollId}/salary-slip`);
}

/** @param {number} employeeId */
export function getSalarySlipsByEmployee(employeeId) {
  return get(`/payrolls/employee/${employeeId}/salary-slips`);
}
