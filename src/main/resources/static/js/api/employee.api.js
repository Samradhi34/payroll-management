/**
 * employee.api.js — Employee API calls
 * Maps to: GET/POST/PUT /employees, PATCH /{id}/status, PATCH /{id}/profile-image
 */
import { get, post, put, patch, postForm, buildQuery } from './http.js';

/**
 * Get all employees with optional filtering + pagination.
 * @param {Object} params - { search, status, departmentId, page, size, sort }
 * @returns {Promise<ApiResult>}
 */
export function getEmployees(params = {}) {
  return get(`/employees${buildQuery(params)}`);
}

/**
 * Get single employee by ID.
 * @param {number} id
 * @returns {Promise<ApiResult>}
 */
export function getEmployee(id) {
  return get(`/employees/${id}`);
}

/**
 * Create a new employee (multipart: form fields + optional image).
 * @param {FormData} formData
 * @returns {Promise<ApiResult>}
 */
export function createEmployee(formData) {
  return postForm('/employees', formData);
}

/**
 * Update employee details.
 * @param {number} id
 * @param {Object} body - EmployeeUpdateRequest fields
 * @returns {Promise<ApiResult>}
 */
export function updateEmployee(id, body) {
  return put(`/employees/${id}`, body);
}

/**
 * Change employee status (ACTIVE, INACTIVE, TERMINATED, RESIGNED).
 * @param {number} id
 * @param {string} status - EmployeeStatus enum value
 * @returns {Promise<ApiResult>}
 */
export function changeEmployeeStatus(id, status) {
  return patch(`/employees/${id}/status?status=${encodeURIComponent(status)}`);
}

/**
 * Update employee profile image.
 * @param {number} id
 * @param {File}   imageFile
 * @returns {Promise<ApiResult>}
 */
export function updateEmployeeImage(id, imageFile) {
  const fd = new FormData();
  fd.append('image', imageFile);
  return postForm(`/employees/${id}/profile-image`, fd);
}

/**
 * Fetch currently logged-in user employee profile.
 * @returns {Promise<ApiResult>}
 */
export function getProfile() {
  return get('/employees/me');
}
