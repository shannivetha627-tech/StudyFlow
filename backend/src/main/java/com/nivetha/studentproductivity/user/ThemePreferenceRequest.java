package com.nivetha.studentproductivity.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ThemePreferenceRequest(
		@NotBlank
		@Pattern(regexp = "dark|light", message = "Theme must be dark or light")
		String theme
) {
}
