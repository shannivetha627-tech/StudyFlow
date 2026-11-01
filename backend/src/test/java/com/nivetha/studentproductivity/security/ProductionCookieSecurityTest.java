package com.nivetha.studentproductivity.security;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;

@SpringBootTest(classes = com.nivetha.studentproductivity.StudentProductivityApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("prod")
class ProductionCookieSecurityTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private Environment environment;

	@Test
	void productionEnablesSecureCookiesAndDisablesH2Console() throws Exception {
		assertTrue(environment.getProperty("app.security.secure-cookies", Boolean.class));
		assertTrue(environment.getProperty("server.servlet.session.cookie.secure", Boolean.class));
		assertFalse(environment.getProperty("spring.h2.console.enabled", Boolean.class));

		mockMvc.perform(get("/api/auth/csrf"))
				.andExpect(status().isOk())
				.andExpect(header().string("Set-Cookie", allOf(
						containsString("XSRF-TOKEN="),
						containsString("Secure"))));
	}
}
