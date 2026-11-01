package com.nivetha.studentproductivity.user;

import java.time.LocalDateTime;

public record ProfileResponse(
		Long id,
		String fullName,
		String email,
		String phoneNumber,
		LocalDateTime createdAt,
		String themePreference
) {
	public static ProfileResponse from(User user) {
		return new ProfileResponse(
				user.getId(),
				user.getFullName(),
				user.getEmail(),
				user.getPhoneNumber(),
				user.getCreatedAt(),
				user.getThemePreference()
		);
	}
}
