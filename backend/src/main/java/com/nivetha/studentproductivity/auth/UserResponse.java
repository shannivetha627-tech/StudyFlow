package com.nivetha.studentproductivity.auth;

import java.time.LocalDateTime;

import com.nivetha.studentproductivity.user.User;

public record UserResponse(
		Long id,
		String fullName,
		String email,
		String phoneNumber,
		LocalDateTime createdAt,
		String themePreference
) {

	public static UserResponse from(User user) {
		return new UserResponse(
				user.getId(),
				user.getFullName(),
				user.getEmail(),
				user.getPhoneNumber(),
				user.getCreatedAt(),
				user.getThemePreference()
		);
	}
}
