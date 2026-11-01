package com.nivetha.studentproductivity.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record PasswordChangeRequest(
		@NotBlank(message = "Current password is required")
		String currentPassword,

		@NotBlank(message = "New password is required")
		@Pattern(
				regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^a-zA-Z\\d]).{8,}$",
				message = "Password must be at least 8 characters long and contain at least one uppercase letter, one lowercase letter, one number, and one special character"
		)
		String newPassword,

		@NotBlank(message = "Confirm new password is required")
		String confirmPassword
) {
}

