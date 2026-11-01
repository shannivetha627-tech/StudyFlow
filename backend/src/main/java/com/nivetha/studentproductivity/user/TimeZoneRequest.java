package com.nivetha.studentproductivity.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TimeZoneRequest(
		@NotBlank @Size(max = 64) String timeZone
) {
}
