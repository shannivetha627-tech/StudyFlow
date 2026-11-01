package com.nivetha.studentproductivity.auth;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
		@NotBlank(message = "Email or phone number is required")
		String identifier,

		@NotBlank(message = "Password is required")
		String password
) {

	@JsonCreator
	public static LoginRequest of(
			@JsonProperty("identifier") String identifier,
			@JsonProperty("email") String email,
			@JsonProperty("phoneNumber") String phoneNumber,
			@JsonProperty("username") String username,
			@JsonProperty("password") String password
	) {
		String resolvedIdentifier = identifier != null && !identifier.isBlank() ? identifier
				: (email != null && !email.isBlank() ? email
				: (phoneNumber != null && !phoneNumber.isBlank() ? phoneNumber : username));
		return new LoginRequest(resolvedIdentifier, password);
	}
}
