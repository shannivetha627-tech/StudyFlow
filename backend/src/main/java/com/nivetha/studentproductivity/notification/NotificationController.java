package com.nivetha.studentproductivity.notification;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

	private final NotificationService notificationService;

	public NotificationController(NotificationService notificationService) {
		this.notificationService = notificationService;
	}

	@GetMapping
	public List<Notification> getNotifications() {
		return notificationService.getNotificationsForCurrentUser();
	}

	@GetMapping("/unread-count")
	public Map<String, Long> getUnreadCount() {
		return Collections.singletonMap("unreadCount", notificationService.getUnreadCount());
	}

	@PatchMapping("/{id}/read")
	public ResponseEntity<Void> markAsRead(@PathVariable Long id) {
		notificationService.markAsRead(id);
		return ResponseEntity.ok().build();
	}

	@PostMapping("/read-all")
	public ResponseEntity<Void> markAllAsRead() {
		notificationService.markAllAsRead();
		return ResponseEntity.ok().build();
	}

	@GetMapping("/settings")
	public UserNotificationSettings getSettings() {
		return notificationService.getSettingsForCurrentUser();
	}

	@PutMapping("/settings")
	public UserNotificationSettings updateSettings(@RequestBody UserNotificationSettings settings) {
		return notificationService.updateSettings(settings);
	}
}

