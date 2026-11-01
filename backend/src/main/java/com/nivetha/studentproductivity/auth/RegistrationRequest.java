package com.nivetha.studentproductivity.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegistrationRequest(
		@NotBlank(message = "Full name is required")
		@Size(min = 2, max = 100, message = "Full name must be between 2 and 100 characters")
		String fullName,

		@NotBlank(message = "Email address is required")
		@Email(message = "Invalid email address format")
		@Size(max = 120, message = "Email cannot exceed 120 characters")
		String email,

		@NotBlank(message = "Phone number is required")
		@Pattern(regexp = "^[+]?[0-9\\s\\-().]{7,25}$", message = "Invalid phone number format")
		String phoneNumber,

		@NotBlank(message = "Password is required")
		@Pattern(
				regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^a-zA-Z\\d]).{8,}$",
				message = "Password must be at least 8 characters long and contain at least one uppercase letter, one lowercase letter, one number, and one special character"
		)
		String password,

		String confirmPassword
) {
}
