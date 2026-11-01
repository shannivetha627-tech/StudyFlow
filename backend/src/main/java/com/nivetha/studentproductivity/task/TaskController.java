package com.nivetha.studentproductivity.task;

import java.net.URI;
import java.util.List;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {

	private final TaskService taskService;

	public TaskController(TaskService taskService) {
		this.taskService = taskService;
	}

	@GetMapping
	public List<Task> getAllTasks() {
		return taskService.getAllTasks();
	}

	@GetMapping("/{id}")
	public Task getTask(@PathVariable Long id) {
		return taskService.getTask(id);
	}

	@PostMapping
	public ResponseEntity<Task> createTask(@Valid @RequestBody TaskRequest request) {
		Task task = taskService.createTask(request);
		URI location = ServletUriComponentsBuilder.fromCurrentRequest()
				.path("/{id}")
				.buildAndExpand(task.getId())
				.toUri();
		return ResponseEntity.created(location).body(task);
	}

	@PutMapping("/{id}")
	public Task updateTask(@PathVariable Long id, @Valid @RequestBody TaskRequest request) {
		return taskService.updateTask(id, request);
	}

	@PatchMapping("/{id}/complete")
	public Task markCompleted(@PathVariable Long id) {
		return taskService.markCompleted(id);
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> deleteTask(@PathVariable Long id) {
		taskService.deleteTask(id);
		return ResponseEntity.noContent().build();
	}
}
