package com.nivetha.studentproductivity.session;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.LocalTime;

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
import com.nivetha.studentproductivity.notification.NotificationRepository;
import com.nivetha.studentproductivity.user.UserRepository;

@SpringBootTest(classes = StudentProductivityApplication.class)
@AutoConfigureMockMvc
class StudySessionTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private StudySessionRepository sessionRepository;

	@Autowired
	private NotificationRepository notificationRepository;

	@Autowired
	private com.nivetha.studentproductivity.TestDatabaseCleaner databaseCleaner;

	private MockHttpSession session;

	@BeforeEach
	void setUp() throws Exception {
		databaseCleaner.clearAll();

		// Register and Login
		mockMvc.perform(post("/api/auth/register")
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "fullName": "Student Jane",
						  "email": "jane@studyflow.edu",
						  "phoneNumber": "+1234567890",
						  "password": "StudyPassword123!"
						}
						"""))
				.andExpect(status().isOk());

		MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "identifier": "jane@studyflow.edu",
						  "password": "StudyPassword123!"
						}
						"""))
				.andExpect(status().isOk())
				.andReturn();

		session = (MockHttpSession) loginResult.getRequest().getSession(false);
	}

	@Test
	void createAndRetrieveStudySession() throws Exception {
		mockMvc.perform(post("/api/study-sessions")
				.session(session)
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "subject": "Java",
						  "topic": "Arrays and Strings",
						  "description": "Learn memory model and manipulation",
						  "sessionDate": "2026-11-02",
						  "startTime": "18:00:00",
						  "endTime": "19:00:00",
						  "reminderEnabled": true,
						  "reminderMinutes": 10
						}
						"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.subject").value("Java"))
				.andExpect(jsonPath("$.topic").value("Arrays and Strings"))
				.andExpect(jsonPath("$.completed").value(false));

		assertEquals(1, sessionRepository.count());

		mockMvc.perform(get("/api/study-sessions").session(session))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].topic").value("Arrays and Strings"));
	}

	@Test
	void preventOverlappingSessions() throws Exception {
		mockMvc.perform(post("/api/study-sessions")
				.session(session)
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "subject": "Physics",
						  "topic": "Electromagnetism",
						  "sessionDate": "2026-11-03",
						  "startTime": "10:00:00",
						  "endTime": "11:30:00"
						}
						"""))
				.andExpect(status().isCreated());

		// Overlapping session (10:30 to 12:00)
		mockMvc.perform(post("/api/study-sessions")
				.session(session)
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "subject": "Math",
						  "topic": "Calculus",
						  "sessionDate": "2026-11-03",
						  "startTime": "10:30:00",
						  "endTime": "12:00:00"
						}
						"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Session overlaps with existing session: Electromagnetism"));
	}

	@Test
	void toggleCompletedAndDeleteSession() throws Exception {
		MvcResult result = mockMvc.perform(post("/api/study-sessions")
				.session(session)
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "subject": "History",
						  "topic": "World War II",
						  "sessionDate": "2026-11-04",
						  "startTime": "14:00:00",
						  "endTime": "15:00:00"
						}
						"""))
				.andExpect(status().isCreated())
				.andReturn();

		StudySession created = sessionRepository.findAll().get(0);
		assertFalse(created.isCompleted());

		// Toggle complete
		mockMvc.perform(patch("/api/study-sessions/" + created.getId() + "/complete")
				.session(session)
				.with(csrf()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.completed").value(true));

		// Delete
		mockMvc.perform(delete("/api/study-sessions/" + created.getId())
				.session(session)
				.with(csrf()))
				.andExpect(status().isNoContent());

		assertEquals(0, sessionRepository.count());
	}

	@Test
	void updateSessionAndRejectInvalidTimes() throws Exception {
		mockMvc.perform(post("/api/study-sessions")
				.session(session)
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "subject": "Math",
						  "topic": "Derivatives",
						  "sessionDate": "2026-11-05",
						  "startTime": "09:00:00",
						  "endTime": "10:00:00"
						}
						"""))
				.andExpect(status().isCreated());

		Long id = sessionRepository.findAll().get(0).getId();
		mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put(
						"/api/study-sessions/" + id)
				.session(session)
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "subject": "Math",
						  "topic": "Limits and continuity",
						  "sessionDate": "2026-11-05",
						  "startTime": "10:00:00",
						  "endTime": "11:00:00",
						  "reminderEnabled": true,
						  "reminderMinutes": 15
						}
						"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.topic").value("Limits and continuity"))
				.andExpect(jsonPath("$.reminderMinutes").value(15));

		mockMvc.perform(post("/api/study-sessions")
				.session(session)
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "subject": "Physics",
						  "topic": "Invalid time range",
						  "sessionDate": "2026-11-05",
						  "startTime": "12:00:00",
						  "endTime": "11:00:00"
						}
						"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Start time must be before end time"));

		mockMvc.perform(post("/api/study-sessions")
				.session(session)
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "subject": "Physics",
						  "topic": "Invalid reminder interval",
						  "sessionDate": "2026-11-05",
						  "startTime": "12:00:00",
						  "endTime": "13:00:00",
						  "reminderMinutes": 0
						}
						"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.fieldErrors.reminderMinutes").exists());
	}

	@Test
	void rejectReversedDateRange() throws Exception {
		mockMvc.perform(get("/api/study-sessions")
				.session(session)
				.param("start", "2026-11-07")
				.param("end", "2026-11-06"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Start date must be on or before end date"));
	}

	@Test
	void usersCanOnlyRetrieveTheirOwnSessions() throws Exception {
		mockMvc.perform(post("/api/study-sessions")
				.session(session)
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "subject": "Computer Science",
						  "topic": "Database design",
						  "sessionDate": "2026-11-06",
						  "startTime": "13:00:00",
						  "endTime": "14:00:00"
						}
						"""))
				.andExpect(status().isCreated());
		Long sessionId = sessionRepository.findAll().get(0).getId();

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

		MvcResult otherLogin = mockMvc.perform(post("/api/auth/login")
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
		MockHttpSession otherSession = (MockHttpSession) otherLogin.getRequest().getSession(false);

		mockMvc.perform(get("/api/study-sessions").session(otherSession))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$").isEmpty());
		mockMvc.perform(get("/api/study-sessions/" + sessionId).session(otherSession))
				.andExpect(status().isForbidden());
		mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put(
						"/api/study-sessions/" + sessionId)
				.session(otherSession)
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "subject": "Computer Science",
						  "topic": "Unauthorized edit",
						  "sessionDate": "2026-11-06",
						  "startTime": "13:00:00",
						  "endTime": "14:00:00"
						}
						"""))
				.andExpect(status().isForbidden());
		mockMvc.perform(patch("/api/study-sessions/" + sessionId + "/complete")
				.session(otherSession)
				.with(csrf()))
				.andExpect(status().isForbidden());
		mockMvc.perform(delete("/api/study-sessions/" + sessionId)
				.session(otherSession)
				.with(csrf()))
				.andExpect(status().isForbidden());
	}
}
