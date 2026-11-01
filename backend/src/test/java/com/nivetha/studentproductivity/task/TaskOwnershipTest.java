package com.nivetha.studentproductivity.task;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.nivetha.studentproductivity.StudentProductivityApplication;
import com.nivetha.studentproductivity.user.User;
import com.nivetha.studentproductivity.user.UserRepository;

@SpringBootTest(classes = StudentProductivityApplication.class)
@AutoConfigureMockMvc
class TaskOwnershipTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private TaskRepository taskRepository;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private com.nivetha.studentproductivity.TestDatabaseCleaner databaseCleaner;

	private MockHttpSession sessionUserA;
	private MockHttpSession sessionUserB;
	private Long userBTaskId;

	@BeforeEach
	void setUp() throws Exception {
		databaseCleaner.clearAll();

		// Create and login User A
		mockMvc.perform(post("/api/auth/register")
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "fullName": "User Alpha",
								  "email": "alpha@example.com",
								  "phoneNumber": "+10000000001",
								  "password": "Password123!"
								}
								"""))
				.andExpect(status().isOk());

		MvcResult loginAResult = mockMvc.perform(post("/api/auth/login")
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "identifier": "alpha@example.com",
								  "password": "Password123!"
								}
								"""))
				.andExpect(status().isOk())
				.andReturn();
		sessionUserA = (MockHttpSession) loginAResult.getRequest().getSession(false);

		// Create and login User B
		mockMvc.perform(post("/api/auth/register")
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "fullName": "User Beta",
								  "email": "beta@example.com",
								  "phoneNumber": "+10000000002",
								  "password": "Password123!"
								}
								"""))
				.andExpect(status().isOk());

		MvcResult loginBResult = mockMvc.perform(post("/api/auth/login")
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "identifier": "beta@example.com",
								  "password": "Password123!"
								}
								"""))
				.andExpect(status().isOk())
				.andReturn();
		sessionUserB = (MockHttpSession) loginBResult.getRequest().getSession(false);

		// User B creates a task
		MvcResult taskBResult = mockMvc.perform(post("/api/tasks")
						.with(csrf())
						.session(sessionUserB)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "title": "User B Secret Task",
								  "description": "Confidential research",
								  "category": "Research",
								  "priority": "HIGH",
								  "dueDate": "2026-11-10",
								  "completed": false
								}
								"""))
				.andExpect(status().isCreated())
				.andReturn();

		// User A creates a task
		mockMvc.perform(post("/api/tasks")
						.with(csrf())
						.session(sessionUserA)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "title": "User A Task",
								  "description": "General task",
								  "category": "Math",
								  "priority": "MEDIUM",
								  "dueDate": "2026-11-12",
								  "completed": false
								}
								"""))
				.andExpect(status().isCreated());

		userBTaskId = taskRepository.findAll().stream()
				.filter(t -> t.getTitle().equals("User B Secret Task"))
				.findFirst()
				.orElseThrow()
				.getId();
	}

	@Test
	void userACannotReadUserBTask() throws Exception {
		mockMvc.perform(get("/api/tasks/{id}", userBTaskId).session(sessionUserA))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.message").value("You do not have permission to access this task"));
	}

	@Test
	void userACannotUpdateUserBTask() throws Exception {
		mockMvc.perform(put("/api/tasks/{id}", userBTaskId)
						.with(csrf())
						.session(sessionUserA)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "title": "Tampered Task",
								  "description": "Hacked",
								  "category": "Research",
								  "priority": "LOW",
								  "dueDate": "2026-11-15",
								  "completed": true
								}
								"""))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.message").value("You do not have permission to access this task"));
	}

	@Test
	void userACannotCompleteUserBTask() throws Exception {
		mockMvc.perform(patch("/api/tasks/{id}/complete", userBTaskId)
						.with(csrf())
						.session(sessionUserA))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.message").value("You do not have permission to access this task"));
	}

	@Test
	void userACannotDeleteUserBTask() throws Exception {
		mockMvc.perform(delete("/api/tasks/{id}", userBTaskId)
						.with(csrf())
						.session(sessionUserA))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.message").value("You do not have permission to access this task"));
	}

	@Test
	void userASeesOnlyTheirOwnTasks() throws Exception {
		mockMvc.perform(get("/api/tasks").session(sessionUserA))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(1)))
				.andExpect(jsonPath("$[0].title").value("User A Task"));

		mockMvc.perform(get("/api/tasks").session(sessionUserB))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(1)))
				.andExpect(jsonPath("$[0].title").value("User B Secret Task"));
	}
}
