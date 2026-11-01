"use strict";

async function getCsrfToken() {
  const match = document.cookie.match(/XSRF-TOKEN=([^;]+)/);
  if (match) {
    return decodeURIComponent(match[1]);
  }
  const response = await fetch("/api/auth/csrf");
  if (!response.ok) {
    throw new Error("Unable to initialize secure form submission. Please refresh and try again.");
  }
  const data = await response.json();
  if (!data.token) {
    throw new Error("Unable to initialize secure form submission. Please refresh and try again.");
  }
  return data.token;
}

// Helper: API request with CSRF header
async function authApiRequest(url, options = {}) {
  const csrfToken = await getCsrfToken();
  const headers = {
    "Content-Type": "application/json",
    ...(csrfToken ? { "X-XSRF-TOKEN": csrfToken } : {}),
    ...(options.headers || {})
  };

  let response;
  try {
    response = await fetch(url, { ...options, headers });
  } catch {
    throw new Error("Unable to connect to the server. Please check your connection.");
  }

  const responseText = response.status === 204 ? "" : await response.text();
  let payload = null;
  if (responseText) {
    try {
      payload = JSON.parse(responseText);
    } catch {
      payload = null;
    }
  }

  if (!response.ok) {
    if (payload?.fieldErrors) {
      const messages = Object.values(payload.fieldErrors);
      throw new Error(messages.join(". "));
    }
    throw new Error(payload?.message || `Request failed (${response.status})`);
  }

  return payload;
}

// Redirect to dashboard if already authenticated
async function checkAlreadyAuthenticated() {
  try {
    const response = await fetch("/api/auth/me");
    if (response.ok) {
      window.location.replace("/index.html");
    }
  } catch {
    // Not authenticated, stay on auth page
  }
}

// UI helper to display messages
function setAlert(alertElement, message, type = "error") {
  if (!alertElement) return;
  if (!message) {
    alertElement.hidden = true;
    alertElement.textContent = "";
    return;
  }
  alertElement.hidden = false;
  alertElement.className = `auth-alert ${type}`;
  alertElement.textContent = message;
}

// Password show/hide toggle initialization
function setupPasswordToggles() {
  document.querySelectorAll("[data-toggle-password]").forEach((btn) => {
    btn.addEventListener("click", () => {
      const targetId = btn.getAttribute("data-toggle-password");
      const input = document.getElementById(targetId);
      if (!input) return;

      const isPassword = input.type === "password";
      input.type = isPassword ? "text" : "password";
      btn.setAttribute("aria-label", isPassword ? "Hide password" : "Show password");

      const eyeIcon = btn.querySelector(".icon-eye");
      const eyeOffIcon = btn.querySelector(".icon-eye-off");
      if (eyeIcon && eyeOffIcon) {
        eyeIcon.style.display = isPassword ? "none" : "block";
        eyeOffIcon.style.display = isPassword ? "block" : "none";
      }
    });
  });
}

// Password strength indicator for registration
function evaluatePasswordStrength(password) {
  let score = 0;
  if (password.length >= 8) score++;
  if (/[A-Z]/.test(password)) score++;
  if (/[a-z]/.test(password)) score++;
  if (/\d/.test(password)) score++;
  if (/[^A-Za-z0-9]/.test(password)) score++;
  return score; // 0 to 5
}

function setupPasswordStrengthMeter() {
  const passwordInput = document.getElementById("reg-password");
  const strengthBars = document.getElementById("strength-bars");
  const strengthText = document.getElementById("strength-text");

  if (!passwordInput || !strengthBars || !strengthText) return;

  const bars = strengthBars.querySelectorAll(".strength-bar");

  passwordInput.addEventListener("input", () => {
    const val = passwordInput.value;
    if (!val) {
      bars.forEach((b) => (b.className = "strength-bar"));
      strengthText.textContent = "Use at least 8 characters with letters, numbers & symbols";
      return;
    }

    const score = evaluatePasswordStrength(val);
    bars.forEach((b, idx) => {
      b.className = "strength-bar";
      if (idx < Math.min(score, 4)) {
        if (score <= 2) b.classList.add("weak");
        else if (score <= 4) b.classList.add("medium");
        else b.classList.add("strong");
      }
    });

    if (val.length < 8) {
      strengthText.textContent = "Too short (minimum 8 characters)";
    } else if (score < 4) {
      strengthText.textContent = "Medium strength (add uppercase, number or symbol)";
    } else {
      strengthText.textContent = "Strong password";
    }
  });
}

function setupAuthThemeToggle() {
  const toggle = document.getElementById("auth-theme-toggle");
  const applyTheme = (theme) => {
    const selectedTheme = theme === "light" ? "light" : "dark";
    document.documentElement.dataset.theme = selectedTheme;
    document.body.classList.toggle("theme-light", selectedTheme === "light");
    document.body.classList.toggle("theme-dark", selectedTheme === "dark");
    localStorage.setItem("studyflow_theme", selectedTheme);
    if (toggle) {
      toggle.textContent = selectedTheme === "dark" ? "Switch to light mode" : "Switch to dark mode";
      toggle.setAttribute("aria-pressed", String(selectedTheme === "light"));
    }
    document.querySelector('meta[name="theme-color"]')?.setAttribute(
      "content",
      selectedTheme === "dark" ? "#0b0d12" : "#f8f9fe"
    );
  };
  applyTheme(document.documentElement.dataset.theme);
  toggle?.addEventListener("click", () => {
    applyTheme(document.documentElement.dataset.theme === "dark" ? "light" : "dark");
  });
}

// Login form handler
function setupLoginForm() {
  const form = document.getElementById("login-form");
  if (!form) return;

  const alertBox = document.getElementById("auth-alert");
  const submitBtn = document.getElementById("login-btn");

  form.addEventListener("submit", async (e) => {
    e.preventDefault();
    setAlert(alertBox, "");

    const identifier = document.getElementById("login-identifier").value.trim();
    const password = document.getElementById("login-password").value;

    if (!identifier || !password) {
      setAlert(alertBox, "Please provide your email/phone number and password.");
      return;
    }

    submitBtn.disabled = true;
    const originalText = submitBtn.innerHTML;
    submitBtn.innerHTML = "Signing in…";

    try {
      await authApiRequest("/api/auth/login", {
        method: "POST",
        body: JSON.stringify({ identifier, password })
      });

      setAlert(alertBox, "Login successful! Redirecting to dashboard…", "success");
      window.location.replace("/index.html");
    } catch (err) {
      setAlert(alertBox, err.message || "Invalid email/phone number or password.");
      submitBtn.disabled = false;
      submitBtn.innerHTML = originalText;
    }
  });
}

// Registration form handler
function setupRegisterForm() {
  const form = document.getElementById("register-form");
  if (!form) return;

  const alertBox = document.getElementById("auth-alert");
  const submitBtn = document.getElementById("register-btn");

  form.addEventListener("submit", async (e) => {
    e.preventDefault();
    setAlert(alertBox, "");

    const fullName = document.getElementById("reg-name").value.trim();
    const email = document.getElementById("reg-email").value.trim();
    const phoneNumber = document.getElementById("reg-phone").value.trim();
    const password = document.getElementById("reg-password").value;
    const confirmPassword = document.getElementById("reg-confirm-password").value;

    if (password !== confirmPassword) {
      setAlert(alertBox, "Passwords do not match. Please verify your password.");
      return;
    }

    if (password.length < 8) {
      setAlert(alertBox, "Password must be at least 8 characters long.");
      return;
    }

    const strongPattern = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[^a-zA-Z\d]).{8,}$/;
    if (!strongPattern.test(password)) {
      setAlert(alertBox, "Password must contain at least one uppercase letter, one lowercase letter, one number, and one special character.");
      return;
    }

    submitBtn.disabled = true;
    const originalText = submitBtn.innerHTML;
    submitBtn.innerHTML = "Creating account…";

    try {
      await authApiRequest("/api/auth/register", {
        method: "POST",
        body: JSON.stringify({
          fullName,
          email,
          phoneNumber,
          password,
          confirmPassword
        })
      });

      setAlert(alertBox, "Account created! Signing you in…", "success");

      // Log in automatically after registration
      await authApiRequest("/api/auth/login", {
        method: "POST",
        body: JSON.stringify({ identifier: email, password })
      });

      window.location.replace("/index.html");
    } catch (err) {
      setAlert(alertBox, err.message || "Registration failed. Please check your information.");
      submitBtn.disabled = false;
      submitBtn.innerHTML = originalText;
    }
  });
}

// Initialize on DOM load
document.addEventListener("DOMContentLoaded", () => {
  const theme = localStorage.getItem("studyflow_theme") === "light" ? "light" : "dark";
  document.body.classList.add(`theme-${theme}`);
  setupAuthThemeToggle();
  checkAlreadyAuthenticated();
  setupPasswordToggles();
  setupPasswordStrengthMeter();
  setupLoginForm();
  setupRegisterForm();
});
