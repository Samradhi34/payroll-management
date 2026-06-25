/**
 * login.page.js — Login & Register Page
 *
 * Handles:
 *  - Login form with validation + JWT storage
 *  - Register form with validation
 *  - Toggle between login and register views
 *  - Password visibility toggle
 *  - Loading state on submit button
 *  - Error display (field-level and global)
 */

import * as AuthApi from '../api/auth.api.js';
import { setSession } from '../auth/auth.store.js';
import { navigate }   from '../router.js';
import { showToast }  from '../components/toast.js';

/** Renders the login/register page into the auth shell */
export async function render() {
  const shell = document.getElementById('auth-shell');
  if (!shell) return;

  shell.innerHTML = `
    <div class="auth-card" id="auth-card">
      <!-- Logo -->
      <div class="auth-logo">
        <div class="auth-logo-icon">
          <svg fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
            <path stroke-linecap="round" stroke-linejoin="round"
              d="M12 8c-1.657 0-3 .895-3 2s1.343 2 3 2 3 .895 3 2-1.343 2-3 2m0-8c1.11 0 2.08.402 2.599 1M12 8V7m0 1v8m0 0v1m0-1c-1.11 0-2.08-.402-2.599-1M21 12a9 9 0 11-18 0 9 9 0 0118 0z"/>
          </svg>
        </div>
        <span class="auth-logo-text">EMSPro</span>
      </div>

      <!-- Login Form -->
      <div id="login-view">
        <h1 class="auth-title">Welcome back</h1>
        <p class="auth-subtitle">Sign in to your account to continue</p>

        <div id="login-error" class="form-error" role="alert" style="margin-bottom:1rem; font-size:13px;"></div>

        <form id="login-form" novalidate autocomplete="on">
          <div class="form-group">
            <label class="form-label required" for="login-username">Username</label>
            <input
              id="login-username" name="username" type="text"
              class="form-control" placeholder="Enter your username"
              autocomplete="username" required maxlength="50"
              aria-describedby="login-username-err"
            />
            <span class="form-error" id="login-username-err"></span>
          </div>

          <div class="form-group">
            <label class="form-label required" for="login-password">Password</label>
            <div class="password-wrapper">
              <input
                id="login-password" name="password" type="password"
                class="form-control" placeholder="Enter your password"
                autocomplete="current-password" required minlength="6"
                aria-describedby="login-password-err"
              />
              <button type="button" class="password-toggle" id="login-pwd-toggle"
                aria-label="Toggle password visibility">
                <svg id="login-eye-icon" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
                  <path stroke-linecap="round" stroke-linejoin="round" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z"/>
                  <path stroke-linecap="round" stroke-linejoin="round" d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z"/>
                </svg>
              </button>
            </div>
            <span class="form-error" id="login-password-err"></span>
          </div>

          <button type="submit" class="btn btn-primary w-full" id="login-btn" style="margin-top:.5rem;">
            Sign In
          </button>
        </form>

        <p class="auth-footer">
          Don't have an account?
          <span class="auth-link" id="go-register" role="button" tabindex="0">Register</span>
        </p>
      </div>

      <!-- Register Form -->
      <div id="register-view" class="hidden">
        <h1 class="auth-title">Create account</h1>
        <p class="auth-subtitle">Fill in the details to register</p>

        <div id="register-error" class="form-error" role="alert" style="margin-bottom:1rem; font-size:13px;"></div>

        <form id="register-form" novalidate autocomplete="off">
          <div class="form-group">
            <label class="form-label required" for="reg-username">Username</label>
            <input id="reg-username" type="text" class="form-control"
              placeholder="Choose a username" required minlength="3" maxlength="30"
              aria-describedby="reg-username-err"/>
            <span class="form-error" id="reg-username-err"></span>
          </div>

          <div class="form-group">
            <label class="form-label required" for="reg-email">Email</label>
            <input id="reg-email" type="email" class="form-control"
              placeholder="your@email.com" required maxlength="100"
              aria-describedby="reg-email-err"/>
            <span class="form-error" id="reg-email-err"></span>
          </div>

          <div class="form-group">
            <label class="form-label required" for="reg-password">Password</label>
            <div class="password-wrapper">
              <input id="reg-password" type="password" class="form-control"
                placeholder="Min 8 characters" required minlength="8" maxlength="64"
                aria-describedby="reg-password-err"/>
              <button type="button" class="password-toggle" id="reg-pwd-toggle" aria-label="Toggle password">
                <svg fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
                  <path stroke-linecap="round" stroke-linejoin="round" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z"/>
                  <path stroke-linecap="round" stroke-linejoin="round" d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z"/>
                </svg>
              </button>
            </div>
            <span class="form-error" id="reg-password-err"></span>
          </div>

          <button type="submit" class="btn btn-primary w-full" id="register-btn" style="margin-top:.5rem;">
            Create Account
          </button>
        </form>

        <p class="auth-footer">
          Already have an account?
          <span class="auth-link" id="go-login" role="button" tabindex="0">Sign In</span>
        </p>
      </div>
    </div>
  `;

  _bindEvents();
}

function _bindEvents() {
  // Toggle between login and register
  document.getElementById('go-register')?.addEventListener('click', () => _showView('register'));
  document.getElementById('go-login')?.addEventListener('click',    () => _showView('login'));
  document.getElementById('go-register')?.addEventListener('keydown', e => e.key === 'Enter' && _showView('register'));
  document.getElementById('go-login')?.addEventListener('keydown',    e => e.key === 'Enter' && _showView('login'));

  // Password toggles
  _bindPasswordToggle('login-password',  'login-pwd-toggle');
  _bindPasswordToggle('reg-password',    'reg-pwd-toggle');

  // Form submissions
  document.getElementById('login-form')?.addEventListener('submit',    _handleLogin);
  document.getElementById('register-form')?.addEventListener('submit', _handleRegister);
}

/** Switch between login and register view */
function _showView(view) {
  document.getElementById('login-view').classList.toggle('hidden',    view !== 'login');
  document.getElementById('register-view').classList.toggle('hidden', view !== 'register');
  // Clear errors on switch
  _clearFormErrors('login-form');
  _clearFormErrors('register-form');
}

/** Toggle password field visibility */
function _bindPasswordToggle(inputId, btnId) {
  document.getElementById(btnId)?.addEventListener('click', () => {
    const input = document.getElementById(inputId);
    if (!input) return;
    input.type = input.type === 'password' ? 'text' : 'password';
  });
}

/* ── Login handler ─────────────────────────────────────────────────────────── */
async function _handleLogin(e) {
  e.preventDefault();
  _clearFormErrors('login-form');

  const username = document.getElementById('login-username')?.value.trim();
  const password = document.getElementById('login-password')?.value;

  // Client-side validation
  let valid = true;
  if (!username) { _showFieldError('login-username-err', 'Username is required');  valid = false; }
  if (!password) { _showFieldError('login-password-err', 'Password is required');  valid = false; }
  if (!valid) return;

  const btn = document.getElementById('login-btn');
  _setButtonLoading(btn, true, 'Signing in…');

  const result = await AuthApi.login(username, password);

  _setButtonLoading(btn, false, 'Sign In');

  if (!result.ok) {
    _showGlobalError('login-error', result.message || 'Invalid username or password.');
    document.getElementById('login-password').value = '';
    return;
  }

  // ── Success: store session, init sidebar, then navigate ──────────────────
  setSession(result.data.token, result.data.username);

  // Signal app.js to init sidebar BEFORE routing — sidebar must exist in DOM
  // before the dashboard page tries to render into #page-content
  window.dispatchEvent(new CustomEvent('epms:login'));

  showToast('success', 'Welcome back!', `Signed in as ${result.data.username}`);

  // Short delay lets sidebar render before hash changes trigger router
  setTimeout(() => navigate('#/dashboard'), 50);
}

/* ── Register handler ──────────────────────────────────────────────────────── */
async function _handleRegister(e) {
  e.preventDefault();
  _clearFormErrors('register-form');

  const username = document.getElementById('reg-username')?.value.trim();
  const email    = document.getElementById('reg-email')?.value.trim();
  const password = document.getElementById('reg-password')?.value;

  // Client-side validation
  let valid = true;
  if (!username || username.length < 3)  { _showFieldError('reg-username-err', 'Username must be at least 3 characters'); valid = false; }
  if (!email || !_isValidEmail(email))   { _showFieldError('reg-email-err',    'Enter a valid email address');             valid = false; }
  if (!password || password.length < 8)  { _showFieldError('reg-password-err', 'Password must be at least 8 characters'); valid = false; }
  if (!valid) return;

  const btn = document.getElementById('register-btn');
  _setButtonLoading(btn, true, 'Creating account…');

  const result = await AuthApi.register(username, email, password);

  _setButtonLoading(btn, false, 'Create Account');

  if (!result.ok) {
    _showGlobalError('register-error', result.message || 'Registration failed. Please try again.');
    return;
  }

  showToast('success', 'Account Created!', 'You can now sign in.');
  _showView('login');
}

/* ── Utilities ─────────────────────────────────────────────────────────────── */

function _showFieldError(id, msg) {
  const el = document.getElementById(id);
  if (!el) return;
  el.textContent = msg;
  el.classList.add('visible');
  el.previousElementSibling?.classList.add('is-invalid');
}

function _showGlobalError(id, msg) {
  const el = document.getElementById(id);
  if (!el) return;
  el.textContent = msg;
  el.classList.add('visible');
}

function _clearFormErrors(formId) {
  const form = document.getElementById(formId);
  if (!form) return;
  form.querySelectorAll('.form-error').forEach(el => {
    el.textContent = '';
    el.classList.remove('visible');
  });
  form.querySelectorAll('.is-invalid').forEach(el => el.classList.remove('is-invalid'));
}

function _setButtonLoading(btn, loading, label) {
  if (!btn) return;
  btn.disabled = loading;
  btn.textContent = loading
    ? label
    : btn.getAttribute('data-original-label') || label;
  if (!loading) btn.setAttribute('data-original-label', label);
}

function _isValidEmail(email) {
  return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email);
}
