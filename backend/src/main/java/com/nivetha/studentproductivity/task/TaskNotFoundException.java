package com.nivetha.studentproductivity.task;

public class TaskNotFoundException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public TaskNotFoundException(Long id) {
		super("Task with id " + id + " was not found");
	}
}
