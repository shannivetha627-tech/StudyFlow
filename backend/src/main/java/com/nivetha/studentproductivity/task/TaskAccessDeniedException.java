package com.nivetha.studentproductivity.task;

public class TaskAccessDeniedException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public TaskAccessDeniedException(String message) {
		super(message);
	}
}
