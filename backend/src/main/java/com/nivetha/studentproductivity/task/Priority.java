package com.nivetha.studentproductivity.task;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum Priority {
	LOW,
	MEDIUM,
	HIGH;

	@JsonCreator
	public static Priority fromString(String value) {
		if (value == null || value.isBlank()) {
			return null;
		}
		for (Priority priority : Priority.values()) {
			if (priority.name().equalsIgnoreCase(value.trim())) {
				return priority;
			}
		}
		throw new IllegalArgumentException("Unknown priority: " + value);
	}
}
