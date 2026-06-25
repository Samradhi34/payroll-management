/**
 * attendance.api.js — Attendance API calls
 * Paths: GET/POST/PUT /attendances, POST /attendances/upload
 *        POST /attendances/check-in, POST /attendances/check-out
 *        GET /attendances/my/today
 */
import { get, post, put, postForm, buildQuery } from './http.js';

export function getAttendances(params = {}) {
  return get(`/attendances${buildQuery(params)}`);
}

export function getAttendance(id) {
  return get(`/attendances/${id}`);
}

export function markAttendance(body) {
  return post('/attendances/mark', body);
}

export function updateAttendance(id, body) {
  return put(`/attendances/${id}`, body);
}

/**
 * Upload attendance CSV file.
 * @param {File} file
 * @returns {Promise<ApiResult>}
 */
export function uploadAttendanceCsv(file) {
  const fd = new FormData();
  fd.append('file', file);
  return postForm('/attendances/upload', fd);
}

/**
 * Employee check-in for today.
 * Uses the logged-in user's identity (resolved server-side).
 * @returns {Promise<ApiResult>}
 */
export function checkIn() {
  return post('/attendances/check-in', {});
}

/**
 * Employee check-out for today.
 * Uses the logged-in user's identity (resolved server-side).
 * @returns {Promise<ApiResult>}
 */
export function checkOut() {
  return post('/attendances/check-out', {});
}

/**
 * Get today's attendance record for the logged-in employee.
 * @returns {Promise<ApiResult>} - data may be null if not checked in yet
 */
export function getTodayAttendance() {
  return get('/attendances/my/today', true);
}
