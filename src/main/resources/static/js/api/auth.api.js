/**
 * auth.api.js — Auth API calls (login, register)
 * Paths: POST /auth/login, POST /auth/register
 */
import { post } from './http.js';

/**
 * Login user with username and password.
 * @param {string} username
 * @param {string} password
 * @returns {Promise<ApiResult>} data = { token, username }
 */
export function login(username, password) {
  return post('/auth/login', { username, password });
}

/**
 * Register a new user. Backend assigns EMPLOYEE role by default.
 * @param {string} username
 * @param {string} email
 * @param {string} password
 * @returns {Promise<ApiResult>}
 */
export function register(username, email, password) {
  return post('/auth/register', { username, email, password });
}
