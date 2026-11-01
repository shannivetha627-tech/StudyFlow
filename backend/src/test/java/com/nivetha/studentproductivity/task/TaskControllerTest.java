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
import com.nivetha.studentproductivity.TestDatabaseCleaner;
import com.nivetha.studentproductivity.user.User;
import com.nivetha.studentproductivity.user.UserRepository;

@SpringBootTest(classes = StudentProductivityApplication.class)
@AutoConfigureMockMvc
class TaskControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private TaskRepository taskRepository;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private TestDatabaseCleaner databaseCleaner;

	private User testUser;
	private MockHttpSession session;

	@BeforeEach
	void setUp() throws Exception {
		databaseCleaner.clearAll();
		testUser = userRepository.save(new User(
				"Test User",
				"test@example.com",
				"+1234567890",
				passwordEncoder.encode("Password123!")
		));

		MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "identifier": "test@example.com",
								  "password": "Password123!"
								}
								"""))
				.andExpect(status().isOk())
				.andReturn();
		session = (MockHttpSession) loginResult.getRequest().getSession(false);
	}

	@Test
	void createTaskWithAllFields() throws Exception {
		mockMvc.perform(post("/api/tasks")
						.with(csrf())
						.session(session)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "title": "Complete Algorithms Assignment",
								  "description": "Solve dynamic programming problems 1 through 5",
								  "category": "Computer Science",
								  "priority": "HIGH",
								  "dueDate": "2026-11-15",
								  "completed": false
								}
								"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id").isNumber())
				.andExpect(jsonPath("$.title").value("Complete Algorithms Assignment"))
				.andExpect(jsonPath("$.description").value("Solve dynamic programming problems 1 through 5"))
				.andExpect(jsonPath("$.category").value("Computer Science"))
				.andExpect(jsonPath("$.priority").value("HIGH"))
				.andExpect(jsonPath("$.dueDate").value("2026-11-15"))
				.andExpect(jsonPath("$.completed").value(false));

		// Also verify priority with mixed-case "Medium" works cleanly
		mockMvc.perform(post("/api/tasks")
						.with(csrf())
						.session(session)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "title": "Read Chemistry Notes",
								  "description": "Periodic trends summary",
								  "category": "Chemistry",
								  "priority": "Medium",
								  "dueDate": "2026-11-20",
								  "completed": false
								}
								"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.priority").value("MEDIUM"));
	}

	@Test
	void createTaskWithOptionalFieldsEmpty() throws Exception {
		// Null dueDate, empty description
		mockMvc.perform(post("/api/tasks")
						.with(csrf())
						.session(session)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "title": "Quick Task without optional fields",
								  "category": "General",
								  "priority": "Low",
								  "description": null,
								  "dueDate": null
								}
								"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id").isNumber())
				.andExpect(jsonPath("$.title").value("Quick Task without optional fields"))
				.andExpect(jsonPath("$.priority").value("LOW"))
				.andExpect(jsonPath("$.description").doesNotExist())
				.andExpect(jsonPath("$.dueDate").doesNotExist())
				.andExpect(jsonPath("$.completed").value(false));

		// Empty string dueDate should also deserialize cleanly to null
		mockMvc.perform(post("/api/tasks")
						.with(csrf())
						.session(session)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "title": "Task with empty string due date",
								  "category": "General",
								  "priority": "HIGH",
								  "description": "",
								  "dueDate": ""
								}
								"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.dueDate").doesNotExist());
	}

	@Test
	void taskEditingCompletionAndDeletion() throws Exception {
		// 1. Create
		MvcResult createResult = mockMvc.perform(post("/api/tasks")
						.with(csrf())
						.session(session)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "title": "Original Task Title",
								  "description": "Original Description",
								  "category": "History",
								  "priority": "Medium",
								  "dueDate": "2026-11-10",
								  "completed": false
								}
								"""))
				.andExpect(status().isCreated())
				.andReturn();

		Long taskId = taskRepository.findAll().get(0).getId();

		// 2. Edit (PUT)
		mockMvc.perform(put("/api/tasks/{id}", taskId)
						.with(csrf())
						.session(session)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "title": "Updated Task Title",
								  "description": "Updated Description",
								  "category": "World History",
								  "priority": "HIGH",
								  "dueDate": "2026-11-12",
								  "completed": false
								}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.title").value("Updated Task Title"))
				.andExpect(jsonPath("$.category").value("World History"))
				.andExpect(jsonPath("$.priority").value("HIGH"));

		// 3. Mark completed (PATCH)
		mockMvc.perform(patch("/api/tasks/{id}/complete", taskId)
						.with(csrf())
						.session(session))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.completed").value(true));

		// 4. Delete (DELETE)
		mockMvc.perform(delete("/api/tasks/{id}", taskId)
						.with(csrf())
						.session(session))
				.andExpect(status().isNoContent());

		// Verify deleted
		mockMvc.perform(get("/api/tasks/{id}", taskId).session(session))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.message").value("Task with id " + taskId + " was not found"));
	}

	@Test
	void supportsTaskCrudLifecycle() throws Exception {
		mockMvc.perform(post("/api/tasks")
						.with(csrf())
						.session(session)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "title": "Read biology chapter",
								  "description": "Take notes",
								  "category": "Biology",
								  "priority": "HIGH",
								  "dueDate": "2026-11-02",
								  "completed": false
								}
								"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id").isNumber())
				.andExpect(jsonPath("$.title").value("Read biology chapter"))
				.andExpect(jsonPath("$.priority").value("HIGH"))
				.andExpect(jsonPath("$.createdAt").isNotEmpty());

		Long id = taskRepository.findAll().get(0).getId();
		mockMvc.perform(get("/api/tasks").session(session))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(1)));
		mockMvc.perform(get("/api/tasks/{id}", id).session(session))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.category").value("Biology"));

		mockMvc.perform(put("/api/tasks/{id}", id)
						.with(csrf())
						.session(session)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "title": "Updated biology chapter",
								  "description": "Review notes",
								  "category": "Science",
								  "priority": "MEDIUM",
								  "dueDate": "2026-11-04",
								  "completed": false
								}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.title").value("Updated biology chapter"))
				.andExpect(jsonPath("$.priority").value("MEDIUM"));

		mockMvc.perform(patch("/api/tasks/{id}/complete", id)
						.with(csrf())
						.session(session))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.completed").value(true));

		mockMvc.perform(delete("/api/tasks/{id}", id)
						.with(csrf())
						.session(session))
				.andExpect(status().isNoContent());
		mockMvc.perform(get("/api/tasks/{id}", id).session(session))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.message").value("Task with id " + id + " was not found"));
	}

	@Test
	void returnsHelpfulErrorsForInvalidAndMissingTasks() throws Exception {
		mockMvc.perform(post("/api/tasks")
						.with(csrf())
						.session(session)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "title": " ",
								  "description": "",
								  "category": "Biology",
								  "priority": "HIGH",
								  "dueDate": null,
								  "completed": false
								}
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Please correct the invalid request fields"))
				.andExpect(jsonPath("$.fieldErrors.title").exists());

		mockMvc.perform(get("/api/tasks/{id}", 999999).session(session))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.error").value("Not Found"));

		mockMvc.perform(post("/api/tasks")
						.with(csrf())
						.session(session)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "title": "Invalid priority",
								  "category": "Biology",
								  "priority": "URGENT",
								  "completed": false
								}
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("The request body contains invalid or missing values"));
	}
}
