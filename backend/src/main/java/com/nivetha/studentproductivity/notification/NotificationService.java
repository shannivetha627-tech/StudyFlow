package com.nivetha.studentproductivity.notification;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nivetha.studentproductivity.session.StudySession;
import com.nivetha.studentproductivity.session.StudySessionRepository;
import com.nivetha.studentproductivity.task.Task;
import com.nivetha.studentproductivity.task.TaskRepository;
import com.nivetha.studentproductivity.user.User;
import com.nivetha.studentproductivity.user.UserRepository;
import com.nivetha.studentproductivity.user.UserService;

@Service
public class NotificationService {

	private final NotificationRepository notificationRepository;
	private final UserNotificationSettingsRepository settingsRepository;
	private final StudySessionRepository studySessionRepository;
	private final TaskRepository taskRepository;
	private final UserService userService;
	private final UserRepository userRepository;
	private final WebPushSender webPushSender;

	public NotificationService(
			NotificationRepository notificationRepository,
			UserNotificationSettingsRepository settingsRepository,
			StudySessionRepository studySessionRepository,
			TaskRepository taskRepository,
			UserService userService,
			UserRepository userRepository,
			WebPushSender webPushSender
	) {
		this.notificationRepository = notificationRepository;
		this.settingsRepository = settingsRepository;
		this.studySessionRepository = studySessionRepository;
		this.taskRepository = taskRepository;
		this.userService = userService;
		this.userRepository = userRepository;
		this.webPushSender = webPushSender;
	}

	@Transactional
	public List<Notification> getNotificationsForCurrentUser() {
		User user = userService.getAuthenticatedUser();
		syncRemindersForUser(user);
		return notificationRepository.findByUserOrderByCreatedAtDesc(user);
	}

	@Transactional(readOnly = true)
	public long getUnreadCount() {
		User user = userService.getAuthenticatedUser();
		return notificationRepository.countByUserAndIsReadFalse(user);
	}

	@Transactional
	public void markAsRead(Long id) {
		User user = userService.getAuthenticatedUser();
		Notification notification = notificationRepository.findByIdAndUser(id, user)
				.orElseThrow(() -> new IllegalArgumentException("Notification not found"));
		notification.setRead(true);
		notificationRepository.save(notification);
	}

	@Transactional
	public void markAllAsRead() {
		User user = userService.getAuthenticatedUser();
		List<Notification> list = notificationRepository.findByUserOrderByCreatedAtDesc(user);
		for (Notification n : list) {
			if (!n.isRead()) {
				n.setRead(true);
			}
		}
		notificationRepository.saveAll(list);
	}

	@Transactional
	public UserNotificationSettings getSettingsForCurrentUser() {
		User user = userService.getAuthenticatedUser();
		return getOrCreateSettings(user);
	}

	@Transactional
	public UserNotificationSettings updateSettings(UserNotificationSettings update) {
		User user = userService.getAuthenticatedUser();
		UserNotificationSettings settings = getOrCreateSettings(user);
		settings.setNotificationsEnabled(update.isNotificationsEnabled());
		settings.setReminderMinutes(update.getReminderMinutes());
		settings.setSoundEnabled(update.isSoundEnabled());
		settings.setDailyReminderEnabled(update.isDailyReminderEnabled());
		if (update.getDailyReminderTime() != null) {
			settings.setDailyReminderTime(update.getDailyReminderTime());
		}
		return settingsRepository.save(settings);
	}

	@Scheduled(fixedDelayString = "${studyflow.notifications.poll-interval-ms:60000}")
	@Transactional
	public void scheduleReminders() {
		for (User user : userRepository.findAll()) {
			syncRemindersForUser(user);
		}
	}

	private synchronized void syncRemindersForUser(User user) {
		UserNotificationSettings settings = getOrCreateSettings(user);
		if (!settings.isNotificationsEnabled()) {
			return;
		}

		ZoneId zoneId = ZoneId.of(user.getTimeZone());
		LocalDateTime localNow = LocalDateTime.now(zoneId);
		LocalDate today = localNow.toLocalDate();
		LocalTime now = localNow.toLocalTime();

		if (settings.isDailyReminderEnabled()
				&& !now.isBefore(settings.getDailyReminderTime())
				&& !notificationRepository.existsByUserAndTypeAndCreatedAtAfter(
						user, NotificationType.DAILY_REMINDER, today.atStartOfDay())) {
			saveAndPush(new Notification(
					user,
					"Your daily study reminder is ready",
					"Review today's tasks and study sessions to plan your next study block.",
					NotificationType.DAILY_REMINDER,
					null
			));
		}

		// 1. Check today's study sessions
		List<StudySession> todaySessions = studySessionRepository.findByUserAndSessionDateOrderByStartTimeAsc(user, today);
		for (StudySession session : todaySessions) {
			if (session.isCompleted() || !session.isReminderEnabled()) continue;

			// Starting soon (within reminderMinutes)
			int reminderMins = session.getReminderMinutes() > 0 ? session.getReminderMinutes() : settings.getReminderMinutes();
			LocalTime reminderThreshold = session.getStartTime().minusMinutes(reminderMins);

			if (now.isAfter(reminderThreshold) && now.isBefore(session.getStartTime())) {
				if (!notificationRepository.existsByUserAndTypeAndReferenceId(user, NotificationType.SESSION_SOON, session.getId())) {
					Notification notification = new Notification(
							user,
							"Study session starting soon: " + session.getSubject(),
							"'" + session.getTopic() + "' begins at " + session.getStartTime() + " (" + reminderMins + " min reminder)",
							NotificationType.SESSION_SOON,
							session.getId()
					);
					saveAndPush(notification);
				}
			}

			// Starting now
			if (!now.isBefore(session.getStartTime()) && now.isBefore(session.getEndTime())) {
				if (!notificationRepository.existsByUserAndTypeAndReferenceId(user, NotificationType.SESSION_NOW, session.getId())) {
					Notification notification = new Notification(
							user,
							"Study session starting now: " + session.getSubject(),
							"Time to study '" + session.getTopic() + "'! Scheduled until " + session.getEndTime(),
							NotificationType.SESSION_NOW,
							session.getId()
					);
					saveAndPush(notification);
				}
			}
		}

		// 2. Check overdue tasks
		List<Task> tasks = taskRepository.findByUserOrderByCreatedAtDesc(user);
		for (Task task : tasks) {
			if (task.isCompleted() || task.getDueDate() == null) continue;

			if (task.getDueDate().isBefore(today)) {
				if (!notificationRepository.existsByUserAndTypeAndReferenceId(user, NotificationType.TASK_OVERDUE, task.getId())) {
					Notification notification = new Notification(
							user,
							"Task overdue: " + task.getTitle(),
							"This task was due on " + task.getDueDate() + ". Stay on track by completing it!",
							NotificationType.TASK_OVERDUE,
							task.getId()
					);
					saveAndPush(notification);
				}
			} else if (task.getDueDate().equals(today)) {
				if (!notificationRepository.existsByUserAndTypeAndReferenceId(user, NotificationType.ASSIGNMENT_DEADLINE, task.getId())) {
					Notification notification = new Notification(
							user,
							"Deadline today: " + task.getTitle(),
							"Category: " + task.getCategory() + " (Priority: " + task.getPriority() + "). Due today!",
							NotificationType.ASSIGNMENT_DEADLINE,
							task.getId()
					);
					saveAndPush(notification);
				}
			} else if (task.getDueDate().equals(today.plusDays(1))) {
				if (!notificationRepository.existsByUserAndTypeAndReferenceId(user, NotificationType.TASK_DUE_TOMORROW, task.getId())) {
					Notification notification = new Notification(
							user,
							"Deadline tomorrow: " + task.getTitle(),
							"Category: " + task.getCategory() + " (Priority: " + task.getPriority() + "). Due tomorrow.",
							NotificationType.TASK_DUE_TOMORROW,
							task.getId()
					);
					saveAndPush(notification);
				}
			}
		}
	}

	private void saveAndPush(Notification notification) {
		Notification saved = notificationRepository.save(notification);
		notificationRepository.flush();
		webPushSender.sendToUser(saved.getUser(), saved);
	}

	private UserNotificationSettings getOrCreateSettings(User user) {
		UserNotificationSettings existing = settingsRepository.findByUser(user).orElse(null);
		if (existing != null) {
			return existing;
		}
		User lockedUser = userRepository.findByIdForUpdate(user.getId())
				.orElseThrow(() -> new IllegalStateException("Authenticated user no longer exists."));
		return settingsRepository.findByUser(lockedUser)
				.orElseGet(() -> settingsRepository.save(new UserNotificationSettings(lockedUser)));
	}
}
