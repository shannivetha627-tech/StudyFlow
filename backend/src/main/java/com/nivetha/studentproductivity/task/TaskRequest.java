package com.nivetha.studentproductivity.task;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record TaskRequest(
		@NotBlank(message = "Title is required")
		@Size(max = 120, message = "Title cannot exceed 120 characters")
		String title,

		@Size(max = 2000, message = "Description cannot exceed 2000 characters")
		String description,

		@NotBlank(message = "Category is required")
		@Size(max = 60, message = "Category cannot exceed 60 characters")
		String category,

		@NotNull(message = "Priority is required")
		Priority priority,

		LocalDate dueDate,

		boolean completed
) {

	@JsonCreator
	public static TaskRequest of(
			@JsonProperty("title") String title,
			@JsonProperty("description") String description,
			@JsonProperty("category") String category,
			@JsonProperty("priority") Object priorityObj,
			@JsonProperty("dueDate") Object dueDateObj,
			@JsonProperty("completed") Boolean completed
	) {
		Priority resolvedPriority = null;
		if (priorityObj instanceof Priority p) {
			resolvedPriority = p;
		} else if (priorityObj instanceof String s && !s.isBlank()) {
			resolvedPriority = Priority.fromString(s);
		}

		LocalDate resolvedDueDate = null;
		if (dueDateObj instanceof String s) {
			String trimmed = s.trim();
			if (!trimmed.isBlank()) {
				try {
					resolvedDueDate = LocalDate.parse(trimmed);
				} catch (DateTimeParseException e) {
					throw new IllegalArgumentException("Invalid date format for dueDate: " + s);
				}
			}
		} else if (dueDateObj instanceof LocalDate ld) {
			resolvedDueDate = ld;
		}

		return new TaskRequest(
				title,
				description,
				category,
				resolvedPriority,
				resolvedDueDate,
				completed != null && completed
		);
	}
}
