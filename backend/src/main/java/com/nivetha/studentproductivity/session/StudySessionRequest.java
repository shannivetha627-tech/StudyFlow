package com.nivetha.studentproductivity.session;

import java.time.LocalDate;
import java.time.LocalTime;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record StudySessionRequest(
		@NotBlank(message = "Subject is required")
		@Size(max = 100, message = "Subject cannot exceed 100 characters")
		String subject,

		@NotBlank(message = "Topic is required")
		@Size(max = 150, message = "Topic cannot exceed 150 characters")
		String topic,

		@Size(max = 2000, message = "Description cannot exceed 2000 characters")
		String description,

		@NotNull(message = "Session date is required")
		LocalDate sessionDate,

		@NotNull(message = "Start time is required")
		LocalTime startTime,

		@NotNull(message = "End time is required")
		LocalTime endTime,

		Boolean reminderEnabled,

		@Min(value = 1, message = "Reminder interval must be at least 1 minute")
		@Max(value = 1440, message = "Reminder interval cannot exceed 24 hours")
		Integer reminderMinutes,

		Boolean completed
) {
}
