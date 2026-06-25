/**
 * department.api.js — Department API calls
 * Paths: GET/POST/PUT /departments, PATCH /{id}/status
 */
import { get, post, put, patch, buildQuery } from './http.js';

export function getDepartments(params = {}) {
  return get(`/departments${buildQuery(params)}`);
}

export function getDepartment(id) {
  return get(`/departments/${id}`);
}

export function createDepartment(body) {
  return post('/departments', body);
}

export function updateDepartment(id, body) {
  return put(`/departments/${id}`, body);
}

/**
 * @param {number}  id
 * @param {boolean} active - true to activate, false to deactivate
 */
export function changeDepartmentStatus(id, active) {
  return patch(`/departments/${id}/status?active=${active}`);
}
