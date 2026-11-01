package com.nivetha.studentproductivity.notification;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.nivetha.studentproductivity.StudentProductivityApplication;
import com.nivetha.studentproductivity.session.StudySession;
import com.nivetha.studentproductivity.session.StudySessionRepository;
import com.nivetha.studentproductivity.task.Priority;
import com.nivetha.studentproductivity.task.Task;
import com.nivetha.studentproductivity.task.TaskRepository;
import com.nivetha.studentproductivity.user.User;
import com.nivetha.studentproductivity.user.UserRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Set;
import java.util.stream.Collectors;

@SpringBootTest(classes = StudentProductivityApplication.class)
@AutoConfigureMockMvc
class NotificationTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private StudySessionRepository sessionRepository;

	@Autowired
	private TaskRepository taskRepository;

	@Autowired
	private NotificationRepository notificationRepository;

	@Autowired
	private NotificationService notificationService;

	@Autowired
	private com.nivetha.studentproductivity.TestDatabaseCleaner databaseCleaner;

	private MockHttpSession session;

	@BeforeEach
	void setUp() throws Exception {
		databaseCleaner.clearAll();

		mockMvc.perform(post("/api/auth/register")
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "fullName": "Student Alex",
						  "email": "alex@studyflow.edu",
						  "phoneNumber": "+1234567891",
						  "password": "StudyPassword123!"
						}
						"""))
				.andExpect(status().isOk());

		MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "identifier": "alex@studyflow.edu",
						  "password": "StudyPassword123!"
						}
						"""))
				.andExpect(status().isOk())
				.andReturn();

		session = (MockHttpSession) loginResult.getRequest().getSession(false);
	}

	@Test
	void getAndMarkNotifications() throws Exception {
		mockMvc.perform(get("/api/notifications").session(session))
				.andExpect(status().isOk());

		mockMvc.perform(get("/api/notifications/unread-count").session(session))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.unreadCount").exists());
	}

	@Test
	void updateNotificationSettings() throws Exception {
		mockMvc.perform(put("/api/notifications/settings")
				.session(session)
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "notificationsEnabled": true,
						  "reminderMinutes": 15,
						  "soundEnabled": false,
						  "dailyReminderEnabled": true,
						  "dailyReminderTime": "08:30:00"
						}
						"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.reminderMinutes").value(15))
				.andExpect(jsonPath("$.soundEnabled").value(false));
	}

	@Test
	void scheduledRemindersUsePersistedUserTimeZoneAndSkipCompletedRecords() {
		User user = userRepository.findByEmailIgnoreCase("alex@studyflow.edu").orElseThrow();
		ZoneId zone = ZoneId.of("UTC");
		LocalDateTime now = LocalDateTime.now(zone);

		Task dueToday = task(user, "Due today", LocalDate.now(zone), false);
		Task overdue = task(user, "Overdue", LocalDate.now(zone).minusDays(1), false);
		Task completed = task(user, "Already complete", LocalDate.now(zone).minusDays(1), true);

		StudySession startingSoon = new StudySession();
		startingSoon.setUser(user);
		startingSoon.setSubject("Math");
		startingSoon.setTopic("Algebra");
		startingSoon.setSessionDate(now.toLocalDate());
		startingSoon.setStartTime(now.toLocalTime().plusMinutes(5));
		startingSoon.setEndTime(now.toLocalTime().plusMinutes(25));
		startingSoon.setReminderMinutes(10);
		sessionRepository.save(startingSoon);

		StudySession startingNow = new StudySession();
		startingNow.setUser(user);
		startingNow.setSubject("Physics");
		startingNow.setTopic("Motion");
		startingNow.setSessionDate(now.toLocalDate());
		startingNow.setStartTime(now.toLocalTime().minusMinutes(1));
		startingNow.setEndTime(now.toLocalTime().plusMinutes(10));
		sessionRepository.save(startingNow);

		notificationService.scheduleReminders();

		Set<NotificationType> types = notificationRepository.findByUserOrderByCreatedAtDesc(user)
				.stream().map(Notification::getType).collect(Collectors.toSet());
		assertTrue(types.contains(NotificationType.SESSION_SOON));
		assertTrue(types.contains(NotificationType.SESSION_NOW));
		assertTrue(types.contains(NotificationType.ASSIGNMENT_DEADLINE));
		assertTrue(types.contains(NotificationType.TASK_OVERDUE));
		assertTrue(notificationRepository.existsByUserAndTypeAndReferenceId(
				user, NotificationType.TASK_OVERDUE, overdue.getId()));
		assertTrue(!notificationRepository.existsByUserAndTypeAndReferenceId(
				user, NotificationType.TASK_OVERDUE, completed.getId()));
		assertTrue(notificationRepository.existsByUserAndTypeAndReferenceId(
				user, NotificationType.ASSIGNMENT_DEADLINE, dueToday.getId()));
	}

	@Test
	void authenticatedUserCanSaveTheirTimeZone() throws Exception {
		mockMvc.perform(put("/api/user/time-zone")
				.session(session)
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"timeZone":"America/New_York"}
						"""))
				.andExpect(status().isNoContent());
		assertEquals("America/New_York",
				userRepository.findByEmailIgnoreCase("alex@studyflow.edu").orElseThrow().getTimeZone());
	}

	private Task task(User user, String title, LocalDate dueDate, boolean completed) {
		Task task = new Task();
		task.setUser(user);
		task.setTitle(title);
		task.setDescription("");
		task.setCategory("Coursework");
		task.setPriority(Priority.MEDIUM);
		task.setDueDate(dueDate);
		task.setCompleted(completed);
		return taskRepository.save(task);
	}
}
