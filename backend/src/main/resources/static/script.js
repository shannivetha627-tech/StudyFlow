/**
 * StudyFlow — Student Productivity & Study Management Frontend Client
 * Fully integrated with real Spring Boot REST APIs and Web Audio Alarms
 */
"use strict";

// State Storage
let currentUser = null;
let userTasks = [];
let userSessions = [];
let userNotifications = [];
let userNotificationSettings = {
  notificationsEnabled: true,
  reminderMinutes: 10,
  soundEnabled: true,
  dailyReminderEnabled: true
};
let activeFilterTask = "all";
let activeTaskSearch = "";
let activeFilterSession = "all";
let calendarCurrentDate = new Date();
let calendarViewMode = "month";
let editingTaskId = null;
let editingSessionId = null;
let audioContext = null;

function localDateISO(date = new Date()) {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, "0");
  const day = String(date.getDate()).padStart(2, "0");
  return `${year}-${month}-${day}`;
}

// ============================================================
// CSRF & SECURE API REQUEST HELPER
// ============================================================

async function getCsrfToken() {
  const match = document.cookie.match(/XSRF-TOKEN=([^;]+)/);
  if (match) return decodeURIComponent(match[1]);

  try {
    const res = await fetch("/api/auth/csrf");
    if (res.ok) {
      const data = await res.json();
      return data.token || "";
    }
  } catch (err) {
    console.warn("CSRF fetch error:", err);
  }
  return "";
}

async function apiRequest(url, options = {}) {
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
    throw new Error("Unable to connect to the server. Please check your network.");
  }

  if (response.status === 401) {
    window.location.replace("/login.html");
    return null;
  }

  const text = response.status === 204 ? "" : await response.text();
  let json = null;
  if (text) {
    try { json = JSON.parse(text); } catch { json = null; }
  }

  if (!response.ok) {
    if (json?.fieldErrors) {
      const messages = Object.values(json.fieldErrors);
      throw new Error(messages.join(". "));
    }
    throw new Error(json?.message || `Request failed with status ${response.status}`);
  }

  return json;
}

// UI Toast Message Banner
function showToast(message, type = "info") {
  const toast = document.getElementById("app-toast");
  if (!toast) return;
  toast.textContent = message;
  toast.className = `app-toast ${type}`;
  toast.hidden = false;
  setTimeout(() => {
    toast.hidden = true;
  }, 4000);
}

// Synthesize pleasant chime using Web Audio API
function playAlarmChime() {
  if (!userNotificationSettings.soundEnabled) return;
  try {
    const AudioContextClass = window.AudioContext || window.webkitAudioContext;
    if (!AudioContextClass) return;
    if (!audioContext) audioContext = new AudioContextClass();
    if (audioContext.state === "suspended") audioContext.resume();

    const now = audioContext.currentTime;
    const osc = audioContext.createOscillator();
    const gain = audioContext.createGain();

    osc.type = "sine";
    osc.frequency.setValueAtTime(587.33, now); // D5
    osc.frequency.setValueAtTime(880.0, now + 0.15); // A5
    osc.frequency.setValueAtTime(1174.66, now + 0.3); // D6

    gain.gain.setValueAtTime(0.2, now);
    gain.gain.exponentialRampToValueAtTime(0.001, now + 0.9);

    osc.connect(gain);
    gain.connect(audioContext.destination);

    osc.start(now);
    osc.stop(now + 0.9);
  } catch (err) {
    console.warn("Audio alarm playback error:", err);
  }
}

// ============================================================
// INITIALIZATION & USER AUTH CHECK
// ============================================================

async function checkSessionAndInit() {
  try {
    const res = await fetch("/api/auth/me");
    if (!res.ok) {
      window.location.replace("/login.html");
      return;
    }
    currentUser = await res.json();
    setupUserUi();
    setupNavigation();
    setupEventListeners();
    await reloadAllData();

    // Start background check for study session alarms every 30 seconds
    setInterval(checkSessionAlarms, 30000);
  } catch {
    window.location.replace("/login.html");
  }
}

function setupUserUi() {
  if (!currentUser) return;

  const initial = (currentUser.fullName.trim()[0] || "S").toUpperCase();
  const sidebarAvatar = document.getElementById("sidebar-avatar");
  const topbarAvatar = document.getElementById("topbar-avatar");
  if (sidebarAvatar) sidebarAvatar.textContent = initial;
  if (topbarAvatar) topbarAvatar.textContent = initial;

  const sidebarName = document.getElementById("sidebar-user-name");
  const sidebarEmail = document.getElementById("sidebar-user-email");
  if (sidebarName) sidebarName.textContent = currentUser.fullName;
  if (sidebarEmail) sidebarEmail.textContent = currentUser.email || currentUser.phoneNumber;

  const dropName = document.getElementById("dropdown-user-name");
  const dropEmail = document.getElementById("dropdown-user-email");
  if (dropName) dropName.textContent = currentUser.fullName;
  if (dropEmail) dropEmail.textContent = currentUser.email || currentUser.phoneNumber;

  // Settings profile form
  const profName = document.getElementById("profile-name");
  const profEmail = document.getElementById("profile-email");
  const profPhone = document.getElementById("profile-phone");
  if (profName) profName.value = currentUser.fullName;
  if (profEmail) profEmail.value = currentUser.email;
  if (profPhone) profPhone.value = currentUser.phoneNumber;

  // Topbar personalized greeting
  const greetingEl = document.getElementById("topbar-greeting");
  const dateEl = document.getElementById("topbar-date");
  if (greetingEl) {
    const hour = new Date().getHours();
    let timeGreeting = "Good morning";
    if (hour >= 12 && hour < 17) timeGreeting = "Good afternoon";
    else if (hour >= 17) timeGreeting = "Good evening";
    greetingEl.textContent = `${timeGreeting}, ${currentUser.fullName.split(" ")[0]} 👋`;
  }
  if (dateEl) {
    const options = { weekday: "long", year: "numeric", month: "short", day: "numeric" };
    dateEl.textContent = new Intl.DateTimeFormat(undefined, options).format(new Date());
  }

  // Load saved theme
  const savedTheme = localStorage.getItem("studyflow_theme") || "lavender";
  setTheme(savedTheme);
}

function setTheme(theme) {
  document.body.className = `theme-${theme}`;
  localStorage.setItem("studyflow_theme", theme);
  document.querySelectorAll(".theme-card").forEach(c => {
    c.classList.toggle("active", c.dataset.theme === theme);
  });
}

// ============================================================
// DATA LOADING & REFRESHING
// ============================================================

async function reloadAllData() {
  await Promise.allSettled([
    fetchTasks(),
    fetchStudySessions(),
    fetchNotifications(),
    fetchNotificationSettings(),
  ]);
  updateDashboardOverview();
  renderTasksView();
  renderAssignmentsView();
  renderPlannerView();
  renderCalendarView();
  renderProgressView();
}

async function fetchTasks() {
  try {
    userTasks = (await apiRequest("/api/tasks")) || [];
    const countEl = document.getElementById("nav-task-count");
    const activeTasks = userTasks.filter(t => !t.completed);
    if (countEl) countEl.textContent = activeTasks.length;
  } catch (err) {
    console.error("Failed to load tasks:", err);
  }
}

async function fetchStudySessions() {
  try {
    userSessions = (await apiRequest("/api/study-sessions")) || [];
    const countEl = document.getElementById("nav-session-count");
    if (countEl) countEl.textContent = userSessions.filter(s => !s.completed).length;
  } catch (err) {
    console.error("Failed to load sessions:", err);
  }
}

async function fetchNotifications() {
  try {
    userNotifications = (await apiRequest("/api/notifications")) || [];
    renderNotificationsUi();
  } catch (err) {
    console.error("Failed to load notifications:", err);
  }
}

async function fetchNotificationSettings() {
  try {
    const s = await apiRequest("/api/notifications/settings");
    if (s) {
      userNotificationSettings = s;
      const tNotif = document.getElementById("toggle-notif-enabled");
      const tSound = document.getElementById("toggle-sound-enabled");
      const tDaily = document.getElementById("toggle-daily-enabled");
      const sInterval = document.getElementById("select-reminder-interval");
      if (tNotif) tNotif.checked = s.notificationsEnabled;
      if (tSound) tSound.checked = s.soundEnabled;
      if (tDaily) tDaily.checked = s.dailyReminderEnabled;
      if (sInterval) sInterval.value = String(s.reminderMinutes);
    }
  } catch (err) {
    console.error("Failed to load notification settings:", err);
  }
}

// ============================================================
// DASHBOARD OVERVIEW & METRICS
// ============================================================

function updateDashboardOverview() {
  const totalTasks = userTasks.length;
  const completedTasks = userTasks.filter(t => t.completed).length;
  const pendingTasks = totalTasks - completedTasks;

  const todayStr = localDateISO();
  const upcomingDeadlines = userTasks.filter(t => !t.completed && t.dueDate && t.dueDate >= todayStr).length;

  // Compute today's scheduled study hours
  const todaySessions = userSessions.filter(s => s.sessionDate === todayStr);
  let totalMinutesToday = 0;
  todaySessions.forEach(s => {
    const [sh, sm] = s.startTime.split(":").map(Number);
    const [eh, em] = s.endTime.split(":").map(Number);
    const diff = (eh * 60 + em) - (sh * 60 + sm);
    if (diff > 0) totalMinutesToday += diff;
  });
  const todayHours = (totalMinutesToday / 60).toFixed(1);

  // Set card values
  document.getElementById("stat-total-tasks").textContent = totalTasks;
  document.getElementById("stat-completed-tasks").textContent = completedTasks;
  document.getElementById("stat-pending-tasks").textContent = pendingTasks;
  document.getElementById("stat-upcoming-deadlines").textContent = upcomingDeadlines;
  document.getElementById("stat-today-hours").textContent = `${todayHours}h`;

  const deadlineNavCount = document.getElementById("nav-deadline-count");
  if (deadlineNavCount) deadlineNavCount.textContent = upcomingDeadlines;

  // Task Completion Progress Ring
  const pct = totalTasks > 0 ? Math.round((completedTasks / totalTasks) * 100) : 0;
  document.getElementById("dashboard-progress-pct").textContent = `${pct}%`;
  const ringFill = document.getElementById("dashboard-ring-fill");
  if (ringFill) {
    const circumference = 2 * Math.PI * 50; // r=50 -> 314.159
    const offset = circumference - (pct / 100) * circumference;
    ringFill.style.strokeDashoffset = String(offset);
  }

  document.getElementById("pstat-done").textContent = completedTasks;
  document.getElementById("pstat-pending").textContent = pendingTasks;
  document.getElementById("pstat-total").textContent = totalTasks;

  // Render Today's Study Schedule List
  const todayListEl = document.getElementById("dashboard-today-sessions");
  if (todaySessions.length === 0) {
    todayListEl.innerHTML = '<p class="empty-state-p">No study sessions scheduled for today. Click \'+ Schedule\' to plan one!</p>';
  } else {
    todayListEl.innerHTML = todaySessions.map(s => `
      <div class="session-row-item ${s.completed ? "is-completed" : ""}">
        <div class="session-time-col">
          <span class="session-time">${escapeHtml(s.startTime.slice(0, 5))} – ${escapeHtml(s.endTime.slice(0, 5))}</span>
          <span class="session-duration">${s.reminderMinutes}m alert</span>
        </div>
        <div class="session-info-col">
          <span class="session-subj-pill">${escapeHtml(s.subject)}</span>
          <h4 class="session-topic-title">${escapeHtml(s.topic)}</h4>
        </div>
        <button class="task-checkbox-btn ${s.completed ? "checked" : ""}" type="button" data-toggle-session="${s.id}" title="Mark completed">
          ${s.completed ? "✓" : ""}
        </button>
      </div>
    `).join("");
  }

  // Render Upcoming Deadlines List (top 4)
  const deadlinesListEl = document.getElementById("dashboard-upcoming-deadlines");
  const pendingWithDates = userTasks
    .filter(t => !t.completed && t.dueDate)
    .sort((a, b) => a.dueDate.localeCompare(b.dueDate))
    .slice(0, 4);

  if (pendingWithDates.length === 0) {
    deadlinesListEl.innerHTML = '<p class="empty-state-p">No upcoming deadlines. All caught up!</p>';
  } else {
    deadlinesListEl.innerHTML = pendingWithDates.map(t => {
      const isOverdue = t.dueDate < todayStr;
      return `
        <div class="deadline-row-item">
          <div class="deadline-left">
            <button class="task-checkbox-btn ${t.completed ? "checked" : ""}" type="button" data-toggle-task="${t.id}">
              ${t.completed ? "✓" : ""}
            </button>
            <div>
              <strong class="task-name-text">${escapeHtml(t.title)}</strong>
              <div class="task-meta-pills">
                <span class="course-pill">${escapeHtml(t.category)}</span>
                <span class="priority-pill priority-${t.priority.toLowerCase()}">${t.priority}</span>
              </div>
            </div>
          </div>
          <span class="due-countdown ${isOverdue ? "overdue" : ""}">${formatDueDate(t.dueDate)}</span>
        </div>
      `;
    }).join("");
  }

  // Render Upcoming Study Sessions in right col (next 7 days)
  const upcomingSessionsEl = document.getElementById("dashboard-upcoming-sessions");
  const futureSessions = userSessions
    .filter(s => s.sessionDate > todayStr)
    .sort((a, b) => a.sessionDate.localeCompare(b.sessionDate) || a.startTime.localeCompare(b.startTime))
    .slice(0, 4);

  if (futureSessions.length === 0) {
    upcomingSessionsEl.innerHTML = '<p class="empty-state-p">No upcoming sessions. Plan your next focus block!</p>';
  } else {
    upcomingSessionsEl.innerHTML = futureSessions.map(s => `
      <div class="session-row-item">
        <div class="session-time-col">
          <span class="session-time">${formatShortDate(s.sessionDate)}</span>
          <span class="session-duration">${escapeHtml(s.startTime.slice(0, 5))}</span>
        </div>
        <div class="session-info-col">
          <span class="session-subj-pill">${escapeHtml(s.subject)}</span>
          <h4 class="session-topic-title">${escapeHtml(s.topic)}</h4>
        </div>
      </div>
    `).join("");
  }

  // Render Weekly Study Activity Chart
  renderWeeklyChart();
}

function renderWeeklyChart() {
  const chartEl = document.getElementById("dash-bar-chart");
  if (!chartEl) return;

  const now = new Date();
  const dayOfWeek = now.getDay(); // 0 is Sunday
  const startOfWeek = new Date(now);
  startOfWeek.setDate(now.getDate() - (dayOfWeek === 0 ? 6 : dayOfWeek - 1)); // Mon
  startOfWeek.setHours(0, 0, 0, 0);

  const daysLabels = ["Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"];
  const daysHours = [0, 0, 0, 0, 0, 0, 0];

  for (let i = 0; i < 7; i++) {
    const cur = new Date(startOfWeek);
    cur.setDate(startOfWeek.getDate() + i);
    const dateStr = localDateISO(cur);

    const sessionsOnDate = userSessions.filter(s => s.sessionDate === dateStr);
    let mins = 0;
    sessionsOnDate.forEach(s => {
      const [sh, sm] = s.startTime.split(":").map(Number);
      const [eh, em] = s.endTime.split(":").map(Number);
      const diff = (eh * 60 + em) - (sh * 60 + sm);
      if (diff > 0) mins += diff;
    });
    daysHours[i] = mins / 60;
  }

  const maxHour = Math.max(...daysHours, 3); // minimum 3 hours scale
  chartEl.innerHTML = daysLabels.map((lbl, idx) => {
    const h = daysHours[idx];
    const heightPct = Math.round((h / maxHour) * 100);
    return `
      <div class="bar-col">
        <span class="bar-val">${h > 0 ? h.toFixed(1) + "h" : ""}</span>
        <div class="bar-fill-track">
          <div class="bar-fill" style="height: ${heightPct}%"></div>
        </div>
        <span class="bar-label">${lbl}</span>
      </div>
    `;
  }).join("");
}

// ============================================================
// TASK MANAGEMENT
// ============================================================

function renderTasksView() {
  const container = document.getElementById("tasks-container");
  if (!container) return;

  let filtered = [...userTasks];

  if (activeFilterTask === "pending") {
    filtered = filtered.filter(t => !t.completed);
  } else if (activeFilterTask === "completed") {
    filtered = filtered.filter(t => t.completed);
  } else if (activeFilterTask === "high") {
    filtered = filtered.filter(t => t.priority && t.priority.toUpperCase() === "HIGH");
  }

  if (activeTaskSearch) {
    const q = activeTaskSearch.toLowerCase();
    filtered = filtered.filter(t =>
      t.title.toLowerCase().includes(q) ||
      t.category.toLowerCase().includes(q) ||
      (t.description && t.description.toLowerCase().includes(q))
    );
  }

  if (filtered.length === 0) {
    container.innerHTML = '<p class="empty-state-p">No matching tasks found. Click \'+ New Task\' to add one.</p>';
    return;
  }

  container.innerHTML = filtered.map(t => `
    <div class="task-item-card ${t.completed ? "is-complete" : ""}">
      <div class="task-item-left">
        <button class="task-checkbox-btn ${t.completed ? "checked" : ""}" type="button" data-toggle-task="${t.id}">
          ${t.completed ? "✓" : ""}
        </button>
        <div>
          <h4 class="task-name-text">${escapeHtml(t.title)}</h4>
          ${t.description ? `<p class="task-desc-text">${escapeHtml(t.description)}</p>` : ""}
          <div class="task-meta-pills">
            <span class="course-pill">${escapeHtml(t.category)}</span>
            <span class="priority-pill priority-${t.priority.toLowerCase()}">${t.priority}</span>
            ${t.dueDate ? `<span class="due-countdown ${t.dueDate < localDateISO() ? "overdue" : ""}">${formatDueDate(t.dueDate)}</span>` : ""}
          </div>
        </div>
      </div>
      <div class="task-item-right">
        <div class="task-actions-group">
          <button class="action-icon-btn" type="button" data-edit-task="${t.id}" title="Edit task">
            <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2"><path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"/><path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"/></svg>
          </button>
          <button class="action-icon-btn delete-btn" type="button" data-delete-task="${t.id}" title="Delete task">
            <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2"><polyline points="3 6 5 6 21 6"/><path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"/></svg>
          </button>
        </div>
      </div>
    </div>
  `).join("");
}

function renderAssignmentsView() {
  const barEl = document.getElementById("assignments-category-bar");
  const gridEl = document.getElementById("assignments-grid");
  if (!gridEl) return;

  const categories = [...new Set(userTasks.map(t => t.category))].filter(Boolean);

  if (barEl) {
    barEl.innerHTML = categories.map(cat => `
      <button class="filter-pill" type="button" data-filter-assignment-cat="${escapeHtml(cat)}">${escapeHtml(cat)}</button>
    `).join("");
  }

  const assignments = userTasks.filter(t => t.dueDate);
  if (assignments.length === 0) {
    gridEl.innerHTML = '<p class="empty-state-p">No coursework assignments found with due dates. Add a task with a due date to track it here!</p>';
    return;
  }

  gridEl.innerHTML = assignments.map(t => `
    <div class="assignment-card">
      <div class="assignment-card-top">
        <span class="course-pill">${escapeHtml(t.category)}</span>
        <span class="priority-pill priority-${t.priority.toLowerCase()}">${t.priority}</span>
      </div>
      <div>
        <h3 class="assignment-title">${escapeHtml(t.title)}</h3>
        ${t.description ? `<p class="task-desc-text">${escapeHtml(t.description)}</p>` : ""}
      </div>
      <div class="assignment-footer">
        <span class="due-countdown ${t.dueDate < localDateISO() ? "overdue" : ""}">${formatDueDate(t.dueDate)}</span>
        <button class="task-checkbox-btn ${t.completed ? "checked" : ""}" type="button" data-toggle-task="${t.id}">
          ${t.completed ? "✓" : ""}
        </button>
      </div>
    </div>
  `).join("");
}

// ============================================================
// STUDY PLANNER & CALENDAR
// ============================================================

function renderPlannerView() {
  const container = document.getElementById("planner-sessions-container");
  if (!container) return;

  const todayStr = localDateISO();
  let list = [...userSessions];

  if (activeFilterSession === "today") {
    list = list.filter(s => s.sessionDate === todayStr);
  } else if (activeFilterSession === "upcoming") {
    list = list.filter(s => s.sessionDate >= todayStr && !s.completed);
  } else if (activeFilterSession === "completed") {
    list = list.filter(s => s.completed);
  }

  if (list.length === 0) {
    container.innerHTML = '<p class="empty-state-p">No study sessions found. Click \'+ Schedule Session\' to create one!</p>';
    return;
  }

  container.innerHTML = list.map(s => `
    <div class="session-card ${s.completed ? "completed" : ""}">
      <div class="session-card-header">
        <div>
          <span class="session-card-subject">${escapeHtml(s.subject)}</span>
          <h3 class="session-card-topic">${escapeHtml(s.topic)}</h3>
        </div>
        <button class="task-checkbox-btn ${s.completed ? "checked" : ""}" type="button" data-toggle-session="${s.id}">
          ${s.completed ? "✓" : ""}
        </button>
      </div>

      <div class="session-card-meta">
        <div class="session-meta-row">
          <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2"><rect x="3" y="4" width="18" height="18" rx="2" ry="2"/><line x1="16" y1="2" x2="16" y2="6"/><line x1="8" y1="2" x2="8" y2="6"/><line x1="3" y1="10" x2="21" y2="10"/></svg>
          <span>${formatShortDate(s.sessionDate)}</span>
        </div>
        <div class="session-meta-row">
          <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><polyline points="12 6 12 12 16 14"/></svg>
          <span>${escapeHtml(s.startTime.slice(0, 5))} – ${escapeHtml(s.endTime.slice(0, 5))}</span>
        </div>
        ${s.reminderEnabled ? `
          <div class="session-meta-row">
            <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2"><path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9"/><path d="M13.73 21a2 2 0 0 1-3.46 0"/></svg>
            <span>Alert ${s.reminderMinutes} minutes before</span>
          </div>
        ` : ""}
      </div>

      ${s.description ? `<p class="task-desc-text">${escapeHtml(s.description)}</p>` : ""}

      <div class="session-card-actions">
        <button class="button-quiet-sm" type="button" data-edit-session="${s.id}">Edit</button>
        <button class="action-icon-btn delete-btn" type="button" data-delete-session="${s.id}" title="Delete session">
          <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2"><polyline points="3 6 5 6 21 6"/><path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"/></svg>
        </button>
      </div>
    </div>
  `).join("");
}

function renderCalendarView() {
  const wrapper = document.getElementById("calendar-grid-wrapper");
  const monthTitle = document.getElementById("cal-month-title");
  if (!wrapper || !monthTitle) return;

  const modeButtons = {
    month: document.getElementById("btn-cal-mode-month"),
    week: document.getElementById("btn-cal-mode-week"),
    day: document.getElementById("btn-cal-mode-day")
  };
  Object.entries(modeButtons).forEach(([mode, button]) => {
    button?.classList.toggle("active", calendarViewMode === mode);
    button?.setAttribute("aria-pressed", String(calendarViewMode === mode));
  });
  const periodName = { month: "month", week: "week", day: "day" }[calendarViewMode];
  document.getElementById("cal-prev")?.setAttribute("aria-label", `Previous ${periodName}`);
  document.getElementById("cal-next")?.setAttribute("aria-label", `Next ${periodName}`);

  if (calendarViewMode === "day") {
    renderCalendarDayView(wrapper, monthTitle);
    return;
  }
  if (calendarViewMode === "week") {
    renderCalendarWeekView(wrapper, monthTitle);
    return;
  }

  renderCalendarMonthView(wrapper, monthTitle);
}

function renderCalendarMonthView(wrapper, monthTitle) {
  const year = calendarCurrentDate.getFullYear();
  const month = calendarCurrentDate.getMonth();
  const monthNames = [
    "January", "February", "March", "April", "May", "June",
    "July", "August", "September", "October", "November", "December"
  ];
  monthTitle.textContent = `${monthNames[month]} ${year}`;

  const firstDay = new Date(year, month, 1).getDay();
  const daysInMonth = new Date(year, month + 1, 0).getDate();
  const previousMonthDays = new Date(year, month, 0).getDate();
  const todayStr = localDateISO();
  let html = `
    <div class="calendar-month-grid">
      <div class="cal-weekday-header">Sun</div>
      <div class="cal-weekday-header">Mon</div>
      <div class="cal-weekday-header">Tue</div>
      <div class="cal-weekday-header">Wed</div>
      <div class="cal-weekday-header">Thu</div>
      <div class="cal-weekday-header">Fri</div>
      <div class="cal-weekday-header">Sat</div>
  `;

  for (let i = firstDay - 1; i >= 0; i--) {
    html += `<div class="cal-day-cell other-month"><span class="cal-day-num">${previousMonthDays - i}</span></div>`;
  }

  for (let day = 1; day <= daysInMonth; day++) {
    const dayStr = `${year}-${String(month + 1).padStart(2, "0")}-${String(day).padStart(2, "0")}`;
    const isToday = dayStr === todayStr;
    const sessionsThisDay = userSessions
      .filter(session => session.sessionDate === dayStr)
      .sort((a, b) => a.startTime.localeCompare(b.startTime));
    const accessibleDate = new Date(year, month, day, 12).toLocaleDateString(undefined, {
      weekday: "long", month: "long", day: "numeric", year: "numeric"
    });

    html += `
      <div class="cal-day-cell ${isToday ? "is-today" : ""}">
        <button class="cal-day-num" type="button" data-cal-date="${dayStr}" aria-label="View ${accessibleDate}">${day}</button>
        ${sessionsThisDay.map(session => `
          <button class="cal-session-chip ${session.completed ? "completed" : ""}" type="button"
            data-edit-session="${session.id}" title="${escapeHtml(session.subject)}: ${escapeHtml(session.topic)}">
            ${escapeHtml(session.startTime.slice(0, 5))} ${escapeHtml(session.subject)}
          </button>
        `).join("")}
      </div>
    `;
  }

  const totalCells = firstDay + daysInMonth;
  const remainingCells = (7 - (totalCells % 7)) % 7;
  for (let day = 1; day <= remainingCells; day++) {
    html += `<div class="cal-day-cell other-month"><span class="cal-day-num">${day}</span></div>`;
  }

  wrapper.innerHTML = `${html}</div>`;
}

function renderCalendarWeekView(wrapper, monthTitle) {
  const weekStart = new Date(calendarCurrentDate);
  weekStart.setHours(12, 0, 0, 0);
  weekStart.setDate(weekStart.getDate() - weekStart.getDay());
  const weekEnd = new Date(weekStart);
  weekEnd.setDate(weekEnd.getDate() + 6);
  monthTitle.textContent = `${weekStart.toLocaleDateString(undefined, { month: "short", day: "numeric" })} – ${weekEnd.toLocaleDateString(undefined, { month: "short", day: "numeric", year: "numeric" })}`;

  const days = Array.from({ length: 7 }, (_, index) => {
    const date = new Date(weekStart);
    date.setDate(weekStart.getDate() + index);
    const dateStr = localDateISO(date);
    const sessions = userSessions
      .filter(session => session.sessionDate === dateStr)
      .sort((a, b) => a.startTime.localeCompare(b.startTime));
    const heading = date.toLocaleDateString(undefined, { weekday: "short", month: "short", day: "numeric" });

    return `
      <section class="calendar-week-day ${dateStr === localDateISO() ? "is-today" : ""}">
        <button class="calendar-week-day-heading" type="button" data-cal-date="${dateStr}" aria-label="View ${date.toLocaleDateString()}">
          <span>${escapeHtml(heading)}</span>
          <span>${sessions.length} ${sessions.length === 1 ? "session" : "sessions"}</span>
        </button>
        <div class="calendar-week-sessions">
          ${sessions.length ? sessions.map(session => `
            <button class="calendar-week-session ${session.completed ? "completed" : ""}" type="button" data-edit-session="${session.id}">
              <span class="calendar-session-time">${escapeHtml(session.startTime.slice(0, 5))}–${escapeHtml(session.endTime.slice(0, 5))}</span>
              <strong>${escapeHtml(session.subject)}</strong>
              <span>${escapeHtml(session.topic)}</span>
            </button>
          `).join("") : '<p class="calendar-no-sessions">No sessions</p>'}
        </div>
      </section>
    `;
  }).join("");

  wrapper.innerHTML = `<div class="calendar-week-scroll"><div class="calendar-week-grid">${days}</div></div>`;
}

function renderCalendarDayView(wrapper, monthTitle) {
  const dateStr = localDateISO(calendarCurrentDate);
  monthTitle.textContent = calendarCurrentDate.toLocaleDateString(undefined, {
    weekday: "long", month: "long", day: "numeric", year: "numeric"
  });
  const sessions = userSessions
    .filter(session => session.sessionDate === dateStr)
    .sort((a, b) => a.startTime.localeCompare(b.startTime));

  if (!sessions.length) {
    wrapper.innerHTML = `<div class="calendar-empty-day"><strong>No study sessions scheduled</strong><span>Choose another date or add a session for this day.</span></div>`;
    return;
  }

  wrapper.innerHTML = `
    <div class="calendar-day-agenda">
      ${sessions.map(session => `
        <article class="calendar-day-session ${session.completed ? "completed" : ""}">
          <div class="calendar-day-session-time">
            <strong>${escapeHtml(session.startTime.slice(0, 5))}</strong>
            <span>${escapeHtml(session.endTime.slice(0, 5))}</span>
          </div>
          <div class="calendar-day-session-details">
            <strong>${escapeHtml(session.subject)}${session.completed ? " · Completed" : ""}</strong>
            <span>${escapeHtml(session.topic)}</span>
            ${session.description ? `<p>${escapeHtml(session.description)}</p>` : ""}
          </div>
          <div class="calendar-day-session-actions">
            <button class="button-quiet-sm" type="button" data-toggle-session="${session.id}">${session.completed ? "Reopen" : "Complete"}</button>
            <button class="button-quiet-sm" type="button" data-edit-session="${session.id}">Edit</button>
            <button class="button-quiet-sm" type="button" data-delete-session="${session.id}">Delete</button>
          </div>
        </article>
      `).join("")}
    </div>
  `;
}

function navigateCalendar(direction) {
  if (calendarViewMode === "day") {
    calendarCurrentDate.setDate(calendarCurrentDate.getDate() + direction);
  } else if (calendarViewMode === "week") {
    calendarCurrentDate.setDate(calendarCurrentDate.getDate() + direction * 7);
  } else {
    const originalDay = calendarCurrentDate.getDate();
    calendarCurrentDate.setDate(1);
    calendarCurrentDate.setMonth(calendarCurrentDate.getMonth() + direction);
    const lastDay = new Date(calendarCurrentDate.getFullYear(), calendarCurrentDate.getMonth() + 1, 0).getDate();
    calendarCurrentDate.setDate(Math.min(originalDay, lastDay));
  }
  renderCalendarView();
}

function renderProgressView() {
  const barsEl = document.getElementById("progress-subject-bars");
  const distEl = document.getElementById("progress-cat-dist");
  if (!barsEl || !distEl) return;

  // Study hours by subject
  const subjectHours = {};
  userSessions.forEach(s => {
    const [sh, sm] = s.startTime.split(":").map(Number);
    const [eh, em] = s.endTime.split(":").map(Number);
    const mins = (eh * 60 + em) - (sh * 60 + sm);
    if (mins > 0) {
      subjectHours[s.subject] = (subjectHours[s.subject] || 0) + mins / 60;
    }
  });

  const subjects = Object.keys(subjectHours);
  if (subjects.length === 0) {
    barsEl.innerHTML = '<p class="empty-state-p">No study sessions recorded yet. Schedule sessions to see your subject hours breakdown!</p>';
  } else {
    const maxH = Math.max(...Object.values(subjectHours), 1);
    barsEl.innerHTML = subjects.map(subj => {
      const h = subjectHours[subj];
      const pct = Math.round((h / maxH) * 100);
      return `
        <div style="margin-bottom: 14px;">
          <div style="display: flex; justify-content: space-between; font-size: 0.85rem; font-weight: 600; margin-bottom: 4px;">
            <span>${escapeHtml(subj)}</span>
            <span>${h.toFixed(1)} hrs</span>
          </div>
          <div style="background: var(--border-light); height: 8px; border-radius: 4px; overflow: hidden;">
            <div style="background: var(--primary-gradient); width: ${pct}%; height: 100%; border-radius: 4px;"></div>
          </div>
        </div>
      `;
    }).join("");
  }

  // Category distribution
  const catCounts = {};
  userTasks.forEach(t => {
    catCounts[t.category] = (catCounts[t.category] || 0) + 1;
  });

  const cats = Object.keys(catCounts);
  if (cats.length === 0) {
    distEl.innerHTML = '<p class="empty-state-p">No tasks added yet.</p>';
  } else {
    distEl.innerHTML = cats.map(c => `
      <div style="display: flex; justify-content: space-between; padding: 10px 0; border-bottom: 1px solid var(--border-light);">
        <span style="font-weight: 600;">${escapeHtml(c)}</span>
        <span class="course-pill">${catCounts[c]} tasks</span>
      </div>
    `).join("");
  }
}

// ============================================================
// NOTIFICATIONS & STUDY ALARMS
// ============================================================

function renderNotificationsUi() {
  const unreadCount = userNotifications.filter(n => !n.read).length;
  const navBadge = document.getElementById("nav-unread-count");
  const bellBadge = document.getElementById("topbar-bell-badge");
  const dropCount = document.getElementById("dropdown-unread-count");

  if (navBadge) navBadge.textContent = unreadCount;
  if (dropCount) dropCount.textContent = `${unreadCount} unread`;

  if (bellBadge) {
    bellBadge.textContent = unreadCount;
    bellBadge.hidden = unreadCount === 0;
  }

  // Populate Dropdown
  const dropList = document.getElementById("dropdown-notifications-list");
  if (dropList) {
    if (userNotifications.length === 0) {
      dropList.innerHTML = '<p class="dropdown-empty">No notifications yet.</p>';
    } else {
      dropList.innerHTML = userNotifications.slice(0, 6).map(n => `
        <div class="notif-item ${!n.read ? "unread" : ""}" data-mark-notif-read="${n.id}">
          <span class="notif-dot"></span>
          <div class="notif-content">
            <h5 class="notif-title">${escapeHtml(n.title)}</h5>
            <p class="notif-message">${escapeHtml(n.message)}</p>
            <span class="notif-time">${formatShortDate(n.createdAt ? n.createdAt.slice(0, 10) : "")}</span>
          </div>
        </div>
      `).join("");
    }
  }

  // Populate Full Notifications View
  const fullList = document.getElementById("notifications-full-list");
  if (fullList) {
    if (userNotifications.length === 0) {
      fullList.innerHTML = '<p class="empty-state-p">No notifications yet. Reminders for sessions and deadlines will appear here.</p>';
    } else {
      fullList.innerHTML = userNotifications.map(n => `
        <div class="notif-item ${!n.read ? "unread" : ""}" data-mark-notif-read="${n.id}">
          <span class="notif-dot"></span>
          <div class="notif-content">
            <h4 class="notif-title">${escapeHtml(n.title)}</h4>
            <p class="notif-message">${escapeHtml(n.message)}</p>
            <span class="notif-time">${formatShortDate(n.createdAt ? n.createdAt.slice(0, 10) : "")}</span>
          </div>
          ${!n.read ? `<button class="button-quiet-sm" type="button" data-mark-notif-read="${n.id}">Mark read</button>` : ""}
        </div>
      `).join("");
    }
  }
}

// Background session alarm trigger
function checkSessionAlarms() {
  if (!userNotificationSettings.notificationsEnabled) return;

  const now = new Date();
  const todayStr = localDateISO(now);
  const curTime = `${String(now.getHours()).padStart(2, "0")}:${String(now.getMinutes()).padStart(2, "0")}`;

  userSessions.forEach(s => {
    if (s.completed || s.sessionDate !== todayStr) return;

    // Check if session start time matches now
    const sessionStart = s.startTime.slice(0, 5);
    if (sessionStart === curTime) {
      triggerAlarmModal(s);
    }
  });
}

function triggerAlarmModal(session) {
  const dialog = document.getElementById("alarm-dialog");
  if (!dialog || dialog.open) return;

  document.getElementById("alarm-subject").textContent = session.subject;
  document.getElementById("alarm-topic").textContent = session.topic;
  document.getElementById("alarm-time").textContent = `${session.startTime.slice(0, 5)} – ${session.endTime.slice(0, 5)}`;

  playAlarmChime();

  // If browser notification allowed, push desktop notification
  if ("Notification" in window && Notification.permission === "granted") {
    new Notification(`StudyFlow: ${session.subject} Study Session`, {
      body: `It's time to study "${session.topic}" (${session.startTime.slice(0, 5)} - ${session.endTime.slice(0, 5)})!`,
      icon: "/favicon.ico"
    });
  }

  dialog.showModal();
}

// ============================================================
// EVENT LISTENERS & ROUTING
// ============================================================

function setupNavigation() {
  document.querySelectorAll("[data-view]").forEach(btn => {
    btn.addEventListener("click", (e) => {
      e.preventDefault();
      const targetView = btn.dataset.view;
      switchView(targetView);

      // If clicked inside mobile sidebar, close it
      document.getElementById("app-sidebar").classList.remove("open");

      // Close dropdowns
      document.getElementById("notifications-dropdown").hidden = true;
      document.getElementById("profile-dropdown").hidden = true;
    });
  });
}

function switchView(viewName) {
  document.querySelectorAll(".view-panel").forEach(p => p.classList.remove("active"));
  document.querySelectorAll(".nav-tab").forEach(t => t.classList.remove("active"));

  const targetPanel = document.getElementById(`view-${viewName}`);
  if (targetPanel) targetPanel.classList.add("active");

  const targetTab = document.querySelector(`.nav-tab[data-view="${viewName}"]`);
  if (targetTab) targetTab.classList.add("active");

  if (viewName === "calendar") renderCalendarView();
  else if (viewName === "progress") renderProgressView();
}

function setupEventListeners() {
  // Mobile sidebar toggle
  document.getElementById("mobile-toggle")?.addEventListener("click", () => {
    document.getElementById("app-sidebar").classList.toggle("open");
  });

  // Dropdown toggles
  document.getElementById("btn-bell")?.addEventListener("click", () => {
    const d = document.getElementById("notifications-dropdown");
    d.hidden = !d.hidden;
    document.getElementById("profile-dropdown").hidden = true;
  });

  document.getElementById("btn-profile-menu")?.addEventListener("click", () => {
    const d = document.getElementById("profile-dropdown");
    d.hidden = !d.hidden;
    document.getElementById("notifications-dropdown").hidden = true;
  });

  // Close dropdowns on outside click
  document.addEventListener("click", (e) => {
    if (!e.target.closest(".dropdown-wrapper")) {
      document.getElementById("notifications-dropdown").hidden = true;
      document.getElementById("profile-dropdown").hidden = true;
    }
  });

  // Sign out buttons
  document.getElementById("sidebar-logout-btn")?.addEventListener("click", handleLogout);
  document.getElementById("topbar-logout-btn")?.addEventListener("click", handleLogout);

  // Quick Action Buttons
  document.getElementById("btn-quick-task")?.addEventListener("click", () => openTaskDialog());
  document.getElementById("btn-tasks-new")?.addEventListener("click", () => openTaskDialog());
  document.getElementById("btn-assignments-new")?.addEventListener("click", () => openTaskDialog());

  document.getElementById("btn-quick-schedule")?.addEventListener("click", () => openSessionDialog());
  document.getElementById("btn-dash-add-session")?.addEventListener("click", () => openSessionDialog());
  document.getElementById("btn-planner-new-session")?.addEventListener("click", () => openSessionDialog());
  document.getElementById("btn-cal-new-session")?.addEventListener("click", () => openSessionDialog());

  // Task Dialog
  document.getElementById("btn-close-task-dialog")?.addEventListener("click", () => document.getElementById("task-dialog").close());
  document.getElementById("btn-cancel-task")?.addEventListener("click", () => document.getElementById("task-dialog").close());
  document.getElementById("task-form")?.addEventListener("submit", handleTaskSubmit);

  // Session Dialog
  document.getElementById("btn-close-session-dialog")?.addEventListener("click", () => document.getElementById("session-dialog").close());
  document.getElementById("btn-cancel-session")?.addEventListener("click", () => document.getElementById("session-dialog").close());
  document.getElementById("session-form")?.addEventListener("submit", handleSessionSubmit);

  // Task Filters & Search
  document.getElementById("task-filter-pills")?.addEventListener("click", (e) => {
    const btn = e.target.closest("[data-task-filter]");
    if (!btn) return;
    document.querySelectorAll("[data-task-filter]").forEach(b => b.classList.remove("active"));
    btn.classList.add("active");
    activeFilterTask = btn.dataset.taskFilter;
    renderTasksView();
  });

  document.getElementById("tasks-search-input")?.addEventListener("input", (e) => {
    activeTaskSearch = e.target.value;
    renderTasksView();
  });

  // Session Filters
  document.getElementById("session-filter-pills")?.addEventListener("click", (e) => {
    const btn = e.target.closest("[data-session-filter]");
    if (!btn) return;
    document.querySelectorAll("[data-session-filter]").forEach(b => b.classList.remove("active"));
    btn.classList.add("active");
    activeFilterSession = btn.dataset.sessionFilter;
    renderPlannerView();
  });

  // Calendar Controls
  document.getElementById("cal-prev")?.addEventListener("click", () => {
    navigateCalendar(-1);
  });
  document.getElementById("cal-next")?.addEventListener("click", () => {
    navigateCalendar(1);
  });
  document.getElementById("cal-today")?.addEventListener("click", () => {
    calendarCurrentDate = new Date();
    renderCalendarView();
  });
  [["month", "btn-cal-mode-month"], ["week", "btn-cal-mode-week"], ["day", "btn-cal-mode-day"]]
    .forEach(([mode, id]) => {
      document.getElementById(id)?.addEventListener("click", () => {
        calendarViewMode = mode;
        renderCalendarView();
      });
    });

  // Notifications
  document.getElementById("btn-mark-all-read")?.addEventListener("click", handleMarkAllNotificationsRead);
  document.getElementById("btn-notif-mark-all")?.addEventListener("click", handleMarkAllNotificationsRead);
  document.getElementById("btn-req-browser-notif")?.addEventListener("click", requestBrowserNotificationPermission);

  // Settings Tabs
  document.getElementById("settings-nav-tabs")?.addEventListener("click", (e) => {
    const btn = e.target.closest("[data-stab]");
    if (!btn) return;
    document.querySelectorAll(".settings-nav-btn").forEach(b => b.classList.remove("active"));
    document.querySelectorAll(".settings-pane").forEach(p => p.classList.remove("active"));
    btn.classList.add("active");
    document.getElementById(`spane-${btn.dataset.stab}`)?.classList.add("active");
  });

  // Settings Form Submissions
  document.getElementById("profile-form")?.addEventListener("submit", handleProfileUpdate);
  document.getElementById("password-form")?.addEventListener("submit", handlePasswordChange);
  document.getElementById("notif-settings-form")?.addEventListener("submit", handleNotificationSettingsUpdate);

  // Theme Picker
  document.querySelectorAll(".theme-card").forEach(c => {
    c.addEventListener("click", () => setTheme(c.dataset.theme));
  });

  // Delegate Dynamic Clicks for Tasks, Sessions & Notifications
  document.addEventListener("click", handleDelegatedClicks);

  // Alarm Dialog buttons
  document.getElementById("btn-alarm-dismiss")?.addEventListener("click", () => {
    document.getElementById("alarm-dialog").close();
  });
  document.getElementById("btn-alarm-start")?.addEventListener("click", () => {
    document.getElementById("alarm-dialog").close();
    switchView("planner");
  });
}

// ============================================================
// CRUD SUBMISSIONS & HANDLERS
// ============================================================

function openTaskDialog(task = null) {
  editingTaskId = task ? task.id : null;
  const dialog = document.getElementById("task-dialog");
  const titleEl = document.getElementById("task-dialog-title");
  const titleInput = document.getElementById("task-title");
  const catInput = document.getElementById("task-category");
  const priInput = document.getElementById("task-priority");
  const dueInput = document.getElementById("task-due-date");
  const descInput = document.getElementById("task-desc");

  titleEl.textContent = task ? "Edit Task" : "Add New Task";
  titleInput.value = task ? task.title : "";
  catInput.value = task ? task.category : "";
  priInput.value = task ? (task.priority || "MEDIUM").toUpperCase() : "MEDIUM";
  dueInput.value = task ? (task.dueDate || "") : "";
  descInput.value = task ? (task.description || "") : "";

  dialog.showModal();
}

async function handleTaskSubmit(e) {
  e.preventDefault();
  const title = document.getElementById("task-title").value.trim();
  const category = document.getElementById("task-category").value.trim();
  const priority = document.getElementById("task-priority").value;
  const dueDate = document.getElementById("task-due-date").value || null;
  const description = document.getElementById("task-desc").value.trim() || null;

  const existingTask = editingTaskId ? userTasks.find(t => t.id === editingTaskId) : null;
  const completed = existingTask ? existingTask.completed : false;

  const payload = { title, category, priority, dueDate, description, completed };

  try {
    if (editingTaskId) {
      await apiRequest(`/api/tasks/${editingTaskId}`, { method: "PUT", body: JSON.stringify(payload) });
      showToast("Task updated successfully!", "success");
    } else {
      await apiRequest("/api/tasks", { method: "POST", body: JSON.stringify(payload) });
      showToast("Task created successfully!", "success");
    }
    document.getElementById("task-dialog").close();
    await fetchTasks();
    updateDashboardOverview();
    renderTasksView();
    renderAssignmentsView();
  } catch (err) {
    showToast(err.message, "error");
  }
}

function openSessionDialog(session = null) {
  editingSessionId = session ? session.id : null;
  const dialog = document.getElementById("session-dialog");
  const titleEl = document.getElementById("session-dialog-title");
  const subjInput = document.getElementById("session-subject");
  const topInput = document.getElementById("session-topic");
  const dateInput = document.getElementById("session-date");
  const startInput = document.getElementById("session-start-time");
  const endInput = document.getElementById("session-end-time");
  const minsSelect = document.getElementById("session-reminder-mins");
  const remCheck = document.getElementById("session-reminder-enabled");
  const descInput = document.getElementById("session-desc");

  titleEl.textContent = session ? "Edit Study Session" : "Schedule Study Session";
  subjInput.value = session ? session.subject : "";
  topInput.value = session ? session.topic : "";
  dateInput.value = session ? session.sessionDate : localDateISO();
  startInput.value = session ? session.startTime.slice(0, 5) : "18:00";
  endInput.value = session ? session.endTime.slice(0, 5) : "19:00";
  minsSelect.value = session ? String(session.reminderMinutes) : "10";
  remCheck.checked = session ? session.reminderEnabled : true;
  descInput.value = session ? (session.description || "") : "";

  dialog.showModal();
}

async function handleSessionSubmit(e) {
  e.preventDefault();
  const subject = document.getElementById("session-subject").value.trim();
  const topic = document.getElementById("session-topic").value.trim();
  const sessionDate = document.getElementById("session-date").value;
  let startTime = document.getElementById("session-start-time").value;
  let endTime = document.getElementById("session-end-time").value;
  const reminderMinutes = Number(document.getElementById("session-reminder-mins").value);
  const reminderEnabled = document.getElementById("session-reminder-enabled").checked;
  const description = document.getElementById("session-desc").value.trim() || null;

  if (startTime.length === 5) startTime += ":00";
  if (endTime.length === 5) endTime += ":00";

  const payload = {
    subject, topic, sessionDate, startTime, endTime, reminderMinutes, reminderEnabled, description
  };

  try {
    if (editingSessionId) {
      await apiRequest(`/api/study-sessions/${editingSessionId}`, { method: "PUT", body: JSON.stringify(payload) });
      showToast("Study session updated!", "success");
    } else {
      await apiRequest("/api/study-sessions", { method: "POST", body: JSON.stringify(payload) });
      showToast("Study session scheduled!", "success");
    }
    document.getElementById("session-dialog").close();
    await fetchStudySessions();
    updateDashboardOverview();
    renderPlannerView();
    renderCalendarView();
  } catch (err) {
    showToast(err.message, "error");
  }
}

async function handleDelegatedClicks(e) {
  const calendarDateBtn = e.target.closest("[data-cal-date]");
  if (calendarDateBtn) {
    const [year, month, day] = calendarDateBtn.dataset.calDate.split("-").map(Number);
    calendarCurrentDate = new Date(year, month - 1, day, 12);
    calendarViewMode = "day";
    renderCalendarView();
    return;
  }

  // Toggle Task Completed
  const toggleTaskBtn = e.target.closest("[data-toggle-task]");
  if (toggleTaskBtn) {
    const id = toggleTaskBtn.dataset.toggleTask;
    try {
      await apiRequest(`/api/tasks/${id}/complete`, { method: "PATCH" });
      await fetchTasks();
      updateDashboardOverview();
      renderTasksView();
      renderAssignmentsView();
    } catch (err) {
      showToast(err.message, "error");
    }
    return;
  }

  // Edit Task
  const editTaskBtn = e.target.closest("[data-edit-task]");
  if (editTaskBtn) {
    const id = Number(editTaskBtn.dataset.editTask);
    const task = userTasks.find(t => t.id === id);
    if (task) openTaskDialog(task);
    return;
  }

  // Delete Task
  const delTaskBtn = e.target.closest("[data-delete-task]");
  if (delTaskBtn) {
    const id = delTaskBtn.dataset.deleteTask;
    if (confirm("Delete this task?")) {
      try {
        await apiRequest(`/api/tasks/${id}`, { method: "DELETE" });
        showToast("Task deleted", "info");
        await fetchTasks();
        updateDashboardOverview();
        renderTasksView();
        renderAssignmentsView();
      } catch (err) {
        showToast(err.message, "error");
      }
    }
    return;
  }

  // Toggle Session Completed
  const toggleSessionBtn = e.target.closest("[data-toggle-session]");
  if (toggleSessionBtn) {
    const id = toggleSessionBtn.dataset.toggleSession;
    try {
      await apiRequest(`/api/study-sessions/${id}/complete`, { method: "PATCH" });
      await fetchStudySessions();
      updateDashboardOverview();
      renderPlannerView();
      renderCalendarView();
    } catch (err) {
      showToast(err.message, "error");
    }
    return;
  }

  // Edit Session
  const editSessionBtn = e.target.closest("[data-edit-session]");
  if (editSessionBtn) {
    const id = Number(editSessionBtn.dataset.editSession);
    const session = userSessions.find(s => s.id === id);
    if (session) openSessionDialog(session);
    return;
  }

  // Delete Session
  const delSessionBtn = e.target.closest("[data-delete-session]");
  if (delSessionBtn) {
    const id = delSessionBtn.dataset.deleteSession;
    if (confirm("Delete this scheduled session?")) {
      try {
        await apiRequest(`/api/study-sessions/${id}`, { method: "DELETE" });
        showToast("Session deleted", "info");
        await fetchStudySessions();
        updateDashboardOverview();
        renderPlannerView();
        renderCalendarView();
      } catch (err) {
        showToast(err.message, "error");
      }
    }
    return;
  }

  // Mark Notification Read
  const notifItem = e.target.closest("[data-mark-notif-read]");
  if (notifItem) {
    const id = notifItem.dataset.markNotifRead;
    try {
      await apiRequest(`/api/notifications/${id}/read`, { method: "PATCH" });
      await fetchNotifications();
    } catch (err) {
      console.warn("Mark read failed:", err);
    }
    return;
  }

}

async function handleMarkAllNotificationsRead() {
  try {
    await apiRequest("/api/notifications/read-all", { method: "POST" });
    await fetchNotifications();
    showToast("All notifications marked as read", "info");
  } catch (err) {
    showToast(err.message, "error");
  }
}

async function requestBrowserNotificationPermission() {
  if (!("Notification" in window)) {
    showToast("This browser does not support desktop notifications.", "error");
    return;
  }

  try {
    const perm = await Notification.requestPermission();
    if (perm === "granted") {
      showToast("Browser alerts enabled successfully! 🔔", "success");
      new Notification("StudyFlow Active", { body: "You will now receive desktop alerts for study sessions!" });
    } else {
      showToast("Browser notification permission was denied.", "info");
    }
  } catch (err) {
    showToast("Unable to request notification permission: " + err.message, "error");
  }
}

async function handleProfileUpdate(e) {
  e.preventDefault();
  const fullName = document.getElementById("profile-name").value.trim();
  const phoneNumber = document.getElementById("profile-phone").value.trim();

  try {
    const updated = await apiRequest("/api/user/profile", {
      method: "PUT",
      body: JSON.stringify({ fullName, phoneNumber })
    });
    currentUser.fullName = updated.fullName;
    currentUser.phoneNumber = updated.phoneNumber;
    setupUserUi();
    showToast("Profile updated successfully!", "success");
  } catch (err) {
    showToast(err.message, "error");
  }
}

async function handlePasswordChange(e) {
  e.preventDefault();
  const currentPassword = document.getElementById("pwd-current").value;
  const newPassword = document.getElementById("pwd-new").value;
  const confirmPassword = document.getElementById("pwd-confirm").value;

  if (newPassword !== confirmPassword) {
    showToast("New passwords do not match", "error");
    return;
  }

  try {
    await apiRequest("/api/user/password", {
      method: "PUT",
      body: JSON.stringify({ currentPassword, newPassword, confirmPassword })
    });
    document.getElementById("password-form").reset();
    showToast("Password updated successfully!", "success");
  } catch (err) {
    showToast(err.message, "error");
  }
}

async function handleNotificationSettingsUpdate(e) {
  e.preventDefault();
  const notificationsEnabled = document.getElementById("toggle-notif-enabled").checked;
  const soundEnabled = document.getElementById("toggle-sound-enabled").checked;
  const dailyReminderEnabled = document.getElementById("toggle-daily-enabled").checked;
  const reminderMinutes = Number(document.getElementById("select-reminder-interval").value);

  const payload = { notificationsEnabled, soundEnabled, dailyReminderEnabled, reminderMinutes };

  try {
    const updated = await apiRequest("/api/notifications/settings", {
      method: "PUT",
      body: JSON.stringify(payload)
    });
    userNotificationSettings = updated;
    showToast("Notification preferences saved!", "success");
  } catch (err) {
    showToast(err.message, "error");
  }
}

async function handleLogout() {
  try {
    await apiRequest("/api/auth/logout", { method: "POST" });
  } catch (err) {
    console.warn("Logout error:", err);
  }
  window.location.replace("/login.html");
}

// ============================================================
// FORMATTING HELPERS
// ============================================================

function escapeHtml(str) {
  return String(str || "").replace(/[&<>"']/g, c => ({
    "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;"
  })[c]);
}

function formatDueDate(isoDate) {
  if (!isoDate) return "";
  const [y, m, d] = isoDate.split("-").map(Number);
  const target = new Date(y, m - 1, d);
  const today = new Date();
  today.setHours(0, 0, 0, 0);

  const diffDays = Math.round((target - today) / (1000 * 60 * 60 * 24));
  if (diffDays < 0) return `${Math.abs(diffDays)}d overdue`;
  if (diffDays === 0) return "Due today";
  if (diffDays === 1) return "Due tomorrow";
  return `Due in ${diffDays}d`;
}

function formatShortDate(isoDate) {
  if (!isoDate) return "";
  const [y, m, d] = isoDate.split("-").map(Number);
  const date = new Date(y, m - 1, d);
  return new Intl.DateTimeFormat(undefined, { month: "short", day: "numeric" }).format(date);
}

// Boot on DOM Ready
document.addEventListener("DOMContentLoaded", checkSessionAndInit);
