package com.nivetha.studentproductivity.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
import com.nivetha.studentproductivity.user.UserRepository;

@SpringBootTest(classes = StudentProductivityApplication.class)
@AutoConfigureMockMvc
class AuthControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private TestDatabaseCleaner databaseCleaner;

	@BeforeEach
	void setUp() {
		databaseCleaner.clearAll();
	}

	@Test
	void successfulRegistration() throws Exception {
		mockMvc.perform(post("/api/auth/register")
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "fullName": "Alice Walker",
								  "email": "alice@example.com",
								  "phoneNumber": "+1234567890",
								  "password": "StrongPassword123!"
								}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.message").value("If the details can be registered, you can now sign in."))
				.andExpect(jsonPath("$.password").doesNotExist());
		assertTrue(userRepository.findByEmailIgnoreCase("alice@example.com")
				.map(user -> passwordEncoder.matches("StrongPassword123!", user.getPassword()))
				.orElse(false));
	}

	@Test
	void duplicateEmailRejection() throws Exception {
		// First registration
		mockMvc.perform(post("/api/auth/register")
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "fullName": "Alice Walker",
								  "email": "alice@example.com",
								  "phoneNumber": "+1234567890",
								  "password": "StrongPassword123!"
								}
								"""))
				.andExpect(status().isOk());

		// Second registration with duplicate email
		mockMvc.perform(post("/api/auth/register")
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "fullName": "Another Alice",
								  "email": "alice@example.com",
								  "phoneNumber": "+9876543210",
								  "password": "AnotherPassword123!"
								}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.message").value("If the details can be registered, you can now sign in."));
		assertEquals(1, userRepository.count());
	}

	@Test
	void duplicatePhoneRejection() throws Exception {
		// First registration
		mockMvc.perform(post("/api/auth/register")
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "fullName": "Bob Builder",
								  "email": "bob@example.com",
								  "phoneNumber": "+15551234567",
								  "password": "StrongPassword123!"
								}
								"""))
				.andExpect(status().isOk());

		// Second registration with duplicate phone
		mockMvc.perform(post("/api/auth/register")
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "fullName": "Charlie Builder",
								  "email": "charlie@example.com",
								  "phoneNumber": "+15551234567",
								  "password": "StrongPassword123!"
								}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.message").value("If the details can be registered, you can now sign in."));
		assertEquals(1, userRepository.count());
	}

	@Test
	void invalidRegistrationData() throws Exception {
		// Invalid email format, invalid phone, weak password, blank name
		mockMvc.perform(post("/api/auth/register")
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "fullName": " ",
								  "email": "not-an-email",
								  "phoneNumber": "123",
								  "password": "weak"
								}
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.fieldErrors.fullName").exists())
				.andExpect(jsonPath("$.fieldErrors.email").exists())
				.andExpect(jsonPath("$.fieldErrors.phoneNumber").exists())
				.andExpect(jsonPath("$.fieldErrors.password").exists());
	}

	@Test
	void successfulEmailLogin() throws Exception {
		// Register
		mockMvc.perform(post("/api/auth/register")
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "fullName": "David Green",
								  "email": "david@example.com",
								  "phoneNumber": "+18889990001",
								  "password": "SecurePassword1!"
								}
								"""))
				.andExpect(status().isOk());

		// Login with email
		MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "identifier": "david@example.com",
								  "password": "SecurePassword1!"
								}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.fullName").value("David Green"))
				.andExpect(jsonPath("$.email").value("david@example.com"))
				.andExpect(jsonPath("$.password").doesNotExist())
				.andReturn();

		MockHttpSession session = (MockHttpSession) loginResult.getRequest().getSession(false);

		// Use session to access /api/auth/me
		mockMvc.perform(get("/api/auth/me").session(session))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.fullName").value("David Green"));
	}

	@Test
	void successfulPhoneLogin() throws Exception {
		// Register
		mockMvc.perform(post("/api/auth/register")
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "fullName": "Elena Rostova",
								  "email": "elena@example.com",
								  "phoneNumber": "+18889990002",
								  "password": "SecurePassword1!"
								}
								"""))
				.andExpect(status().isOk());

		// Login with phone number
		MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "identifier": "+18889990002",
								  "password": "SecurePassword1!"
								}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.fullName").value("Elena Rostova"))
				.andReturn();

		MockHttpSession session = (MockHttpSession) loginResult.getRequest().getSession(false);

		// Verify session
		mockMvc.perform(get("/api/auth/me").session(session))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.email").value("elena@example.com"));
	}

	@Test
	void loginWithIncorrectPassword() throws Exception {
		mockMvc.perform(post("/api/auth/register")
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "fullName": "Frank Castle",
								  "email": "frank@example.com",
								  "phoneNumber": "+18889990003",
								  "password": "SecurePassword1!"
								}
								"""))
				.andExpect(status().isOk());

		// Wrong password on existing email returns generic error
		mockMvc.perform(post("/api/auth/login")
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "identifier": "frank@example.com",
								  "password": "WrongPassword!"
								}
								"""))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.message").value("Invalid email/phone number or password"));
	}

	@Test
	void loginWithUnknownAccount() throws Exception {
		// Non-existent identifier gives the exact same generic message
		mockMvc.perform(post("/api/auth/login")
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "identifier": "nonexistent@example.com",
								  "password": "SomePassword1!"
								}
								"""))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.message").value("Invalid email/phone number or password"));
	}

	@Test
	void sessionPersistenceAfterPageRefresh() throws Exception {
		// Register & login
		mockMvc.perform(post("/api/auth/register")
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "fullName": "Refresh Tester",
								  "email": "refresh@example.com",
								  "phoneNumber": "+18889990088",
								  "password": "SecurePassword1!"
								}
								"""))
				.andExpect(status().isOk());

		MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "identifier": "refresh@example.com",
								  "password": "SecurePassword1!"
								}
								"""))
				.andExpect(status().isOk())
				.andReturn();

		MockHttpSession session = (MockHttpSession) loginResult.getRequest().getSession(false);

		// First request (initial load)
		mockMvc.perform(get("/api/auth/me").session(session))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.email").value("refresh@example.com"));

		// Second request (simulating browser page refresh with same session cookie)
		mockMvc.perform(get("/api/auth/me").session(session))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.fullName").value("Refresh Tester"));

		// Third request to tasks endpoint with same session
		mockMvc.perform(get("/api/tasks").session(session))
				.andExpect(status().isOk());
	}

	@Test
	void logoutAndSubsequentLogin() throws Exception {
		// Register & login
		mockMvc.perform(post("/api/auth/register")
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "fullName": "Grace Hopper",
								  "email": "grace@example.com",
								  "phoneNumber": "+18889990004",
								  "password": "SecurePassword1!"
								}
								"""))
				.andExpect(status().isOk());

		MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "identifier": "grace@example.com",
								  "password": "SecurePassword1!"
								}
								"""))
				.andExpect(status().isOk())
				.andReturn();

		MockHttpSession session = (MockHttpSession) loginResult.getRequest().getSession(false);

		// Logout
		mockMvc.perform(post("/api/auth/logout")
						.with(csrf())
						.session(session))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.message").value("Logged out successfully"));

		// Accessing /api/auth/me with the invalidated session returns 401
		mockMvc.perform(get("/api/auth/me").session(session))
				.andExpect(status().isUnauthorized());

		// Subsequent login with fresh session works
		MvcResult secondLogin = mockMvc.perform(post("/api/auth/login")
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "identifier": "grace@example.com",
								  "password": "SecurePassword1!"
								}
								"""))
				.andExpect(status().isOk())
				.andReturn();

		MockHttpSession newSession = (MockHttpSession) secondLogin.getRequest().getSession(false);
		mockMvc.perform(get("/api/auth/me").session(newSession))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.email").value("grace@example.com"));
	}

	@Test
	void accessProtectedEndpointsWithoutAuthenticationReturns401() throws Exception {
		mockMvc.perform(get("/api/tasks"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.message").value("Authentication required"));

		mockMvc.perform(get("/api/auth/me"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.message").value("Authentication required"));
	}

	@Test
	void loginRotatesAnExistingSessionId() throws Exception {
		mockMvc.perform(post("/api/auth/register")
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "fullName": "Session User",
								  "email": "session@example.com",
								  "phoneNumber": "+18889990005",
								  "password": "SecurePassword1!"
								}
								"""))
				.andExpect(status().isOk());

		MockHttpSession preAuthenticationSession = new MockHttpSession();
		String oldSessionId = preAuthenticationSession.getId();
		MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
						.with(csrf())
						.session(preAuthenticationSession)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "identifier": "session@example.com",
								  "password": "SecurePassword1!"
								}
								"""))
				.andExpect(status().isOk())
				.andReturn();

		MockHttpSession authenticatedSession = (MockHttpSession) loginResult.getRequest().getSession(false);
		assertNotEquals(oldSessionId, authenticatedSession.getId());
	}

	@Test
	void stateChangingAuthenticationRequestsRequireCsrf() throws Exception {
		mockMvc.perform(post("/api/auth/register")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "fullName": "No CSRF",
								  "email": "csrf@example.com",
								  "phoneNumber": "+18889990006",
								  "password": "SecurePassword1!"
								}
								"""))
				.andExpect(status().isForbidden());
		assertEquals(0, userRepository.count());
	}
}
