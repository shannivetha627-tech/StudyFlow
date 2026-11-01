package com.nivetha.studentproductivity.notification;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
import com.nivetha.studentproductivity.TestDatabaseCleaner;

@SpringBootTest(classes = StudentProductivityApplication.class)
@AutoConfigureMockMvc
class PushSubscriptionTest {

	private static final String SUBSCRIPTION = """
			{
			  "endpoint": "https://fcm.googleapis.com/fcm/send/opaque",
			  "keys": {
			    "p256dh": "BKxExamplePublicKey",
			    "auth": "opaqueAuthSecret"
			  }
			}
			""";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private PushSubscriptionRepository repository;

	@Autowired
	private TestDatabaseCleaner databaseCleaner;

	private MockHttpSession ownerSession;

	@BeforeEach
	void setUp() throws Exception {
		databaseCleaner.clearAll();
		registerAndLogin("push-owner@example.test", "+1234567801", "PushOwnerPassword123!");
		MvcResult login = login("push-owner@example.test", "PushOwnerPassword123!");
		ownerSession = (MockHttpSession) login.getRequest().getSession(false);
	}

	@Test
	void subscriptionsAreOwnedAndCannotBeChangedByAnotherAccount() throws Exception {
		mockMvc.perform(post("/api/push/subscriptions")
				.session(ownerSession)
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content(SUBSCRIPTION))
				.andExpect(status().isNoContent());
		assertEquals(1, repository.count());

		registerAndLogin("push-other@example.test", "+1234567802", "PushOtherPassword123!");
		MvcResult otherLogin = login("push-other@example.test", "PushOtherPassword123!");
		MockHttpSession otherSession = (MockHttpSession) otherLogin.getRequest().getSession(false);

		mockMvc.perform(put("/api/push/subscriptions")
				.session(otherSession)
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content(SUBSCRIPTION))
				.andExpect(status().isConflict());
		mockMvc.perform(delete("/api/push/subscriptions")
				.session(otherSession)
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content(SUBSCRIPTION))
				.andExpect(status().isNoContent());
		assertEquals(1, repository.count());

		mockMvc.perform(delete("/api/push/subscriptions")
				.session(ownerSession)
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content(SUBSCRIPTION))
				.andExpect(status().isNoContent());
		assertEquals(0, repository.count());
	}

	private void registerAndLogin(String email, String phone, String password) throws Exception {
		mockMvc.perform(post("/api/auth/register")
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "fullName": "Push User",
						  "email": "%s",
						  "phoneNumber": "%s",
						  "password": "%s"
						}
						""".formatted(email, phone, password)))
				.andExpect(status().isOk());
	}

	private MvcResult login(String email, String password) throws Exception {
		return mockMvc.perform(post("/api/auth/login")
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "identifier": "%s",
						  "password": "%s"
						}
						""".formatted(email, password)))
				.andExpect(status().isOk())
				.andReturn();
	}
}
