package com.nivetha.studentproductivity.task;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.nivetha.studentproductivity.auth.UnauthenticatedException;

@RestControllerAdvice
public class TaskExceptionHandler {

	@ExceptionHandler(TaskNotFoundException.class)
	@ResponseStatus(HttpStatus.NOT_FOUND)
	public Map<String, Object> handleTaskNotFound(TaskNotFoundException exception) {
		return error(HttpStatus.NOT_FOUND, exception.getMessage());
	}

	@ExceptionHandler(TaskAccessDeniedException.class)
	@ResponseStatus(HttpStatus.FORBIDDEN)
	public Map<String, Object> handleTaskAccessDenied(TaskAccessDeniedException exception) {
		return error(HttpStatus.FORBIDDEN, exception.getMessage());
	}

	@ExceptionHandler(BadCredentialsException.class)
	@ResponseStatus(HttpStatus.UNAUTHORIZED)
	public Map<String, Object> handleBadCredentials(BadCredentialsException exception) {
		return error(HttpStatus.UNAUTHORIZED, "Invalid email/phone number or password");
	}

	@ExceptionHandler(UnauthenticatedException.class)
	@ResponseStatus(HttpStatus.UNAUTHORIZED)
	public Map<String, Object> handleUnauthenticated(UnauthenticatedException exception) {
		return error(HttpStatus.UNAUTHORIZED, exception.getMessage());
	}

	@ExceptionHandler(IllegalArgumentException.class)
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	public Map<String, Object> handleIllegalArgument(IllegalArgumentException exception) {
		return error(HttpStatus.BAD_REQUEST, exception.getMessage());
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	public Map<String, Object> handleValidation(MethodArgumentNotValidException exception) {
		Map<String, String> fieldErrors = exception.getBindingResult()
				.getFieldErrors()
				.stream()
				.collect(Collectors.toMap(
						FieldError::getField,
						FieldError::getDefaultMessage,
						(first, second) -> first,
						LinkedHashMap::new));
		Map<String, Object> response = error(HttpStatus.BAD_REQUEST, "Please correct the invalid request fields");
		response.put("fieldErrors", fieldErrors);
		return response;
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	public Map<String, Object> handleUnreadableRequest() {
		return error(HttpStatus.BAD_REQUEST, "The request body contains invalid or missing values");
	}

	private Map<String, Object> error(HttpStatus status, String message) {
		Map<String, Object> response = new LinkedHashMap<>();
		response.put("status", status.value());
		response.put("error", status.getReasonPhrase());
		response.put("message", message);
		return response;
	}
}
