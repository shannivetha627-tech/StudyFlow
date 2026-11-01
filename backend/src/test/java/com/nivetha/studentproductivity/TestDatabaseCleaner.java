package com.nivetha.studentproductivity;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.nivetha.studentproductivity.notification.NotificationRepository;
import com.nivetha.studentproductivity.notification.PushSubscriptionRepository;
import com.nivetha.studentproductivity.notification.UserNotificationSettingsRepository;
import com.nivetha.studentproductivity.session.StudySessionRepository;
import com.nivetha.studentproductivity.task.TaskRepository;
import com.nivetha.studentproductivity.user.UserRepository;

@Component
public class TestDatabaseCleaner {

	@Autowired
	private NotificationRepository notificationRepository;

	@Autowired
	private PushSubscriptionRepository pushSubscriptionRepository;

	@Autowired
	private UserNotificationSettingsRepository userNotificationSettingsRepository;

	@Autowired
	private StudySessionRepository studySessionRepository;

	@Autowired
	private TaskRepository taskRepository;

	@Autowired
	private UserRepository userRepository;

	public void clearAll() {
		notificationRepository.deleteAll();
		pushSubscriptionRepository.deleteAll();
		userNotificationSettingsRepository.deleteAll();
		studySessionRepository.deleteAll();
		taskRepository.deleteAll();
		userRepository.deleteAll();
	}
}
