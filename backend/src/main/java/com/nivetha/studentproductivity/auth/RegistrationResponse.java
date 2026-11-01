package com.nivetha.studentproductivity.auth;

public record RegistrationResponse(String message) {

	public static RegistrationResponse generic() {
		return new RegistrationResponse("If the details can be registered, you can now sign in.");
	}
}
