"use strict";

const CACHE_NAME = "studyflow-static-v3";
const APP_SHELL = [
  "/login.html",
  "/register.html",
  "/style.css",
  "/auth.css",
  "/script.js",
  "/auth.js",
  "/manifest.webmanifest",
  "/icons/studyflow.svg"
];

self.addEventListener("install", (event) => {
  event.waitUntil(
    caches.open(CACHE_NAME)
      .then((cache) => cache.addAll(APP_SHELL))
      .then(() => self.skipWaiting())
  );
});

self.addEventListener("activate", (event) => {
  event.waitUntil(
    caches.keys()
      .then((keys) => Promise.all(keys
        .filter((key) => key.startsWith("studyflow-static-") && key !== CACHE_NAME)
        .map((key) => caches.delete(key))))
      .then(() => self.clients.claim())
  );
});

self.addEventListener("fetch", (event) => {
  const request = event.request;
  const url = new URL(request.url);
  if (request.method !== "GET" || url.origin !== self.location.origin || url.pathname.startsWith("/api/")) {
    return;
  }

  if (request.mode === "navigate") {
    event.respondWith(fetch(request).catch(() => caches.match("/login.html")));
    return;
  }

  if (APP_SHELL.includes(url.pathname)) {
    event.respondWith(
      caches.match(request).then((cached) => cached || fetch(request))
    );
  }
});

self.addEventListener("push", (event) => {
  let payload = {};
  try {
    payload = event.data ? event.data.json() : {};
  } catch {
    payload = { body: event.data ? event.data.text() : "" };
  }

  const title = typeof payload.title === "string" && payload.title.trim()
    ? payload.title
    : "StudyFlow reminder";
  const body = typeof payload.body === "string" ? payload.body : "";
  const url = typeof payload.url === "string" && payload.url.startsWith("/")
    ? payload.url
    : "/index.html";

  event.waitUntil(self.registration.showNotification(title, {
    body,
    icon: "/icons/studyflow.svg",
    badge: "/icons/studyflow.svg",
    tag: typeof payload.tag === "string" ? payload.tag : undefined,
    renotify: false,
    data: { url }
  }));
});

self.addEventListener("notificationclick", (event) => {
  event.notification.close();
  const target = new URL(
    event.notification.data && event.notification.data.url
      ? event.notification.data.url
      : "/index.html",
    self.location.origin
  );
  if (target.origin !== self.location.origin) {
    return;
  }

  event.waitUntil(
    self.clients.matchAll({ type: "window", includeUncontrolled: true })
      .then((clients) => {
        const existing = clients.find((client) => new URL(client.url).origin === target.origin);
        if (existing) {
          return existing.navigate(target.href).then(() => existing.focus());
        }
        return self.clients.openWindow(target.href);
      })
  );
});
