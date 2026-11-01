package com.nivetha.studentproductivity.task;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nivetha.studentproductivity.user.User;
import com.nivetha.studentproductivity.user.UserService;

@Service
public class TaskService {

	private final TaskRepository taskRepository;
	private final UserService userService;

	public TaskService(TaskRepository taskRepository, UserService userService) {
		this.taskRepository = taskRepository;
		this.userService = userService;
	}

	@Transactional(readOnly = true)
	public List<Task> getAllTasks() {
		User currentUser = userService.getAuthenticatedUser();
		return taskRepository.findByUserOrderByCreatedAtDesc(currentUser);
	}

	@Transactional(readOnly = true)
	public Task getTask(Long id) {
		User currentUser = userService.getAuthenticatedUser();
		Task task = findTask(id);
		verifyOwnership(task, currentUser);
		return task;
	}

	@Transactional
	public Task createTask(TaskRequest request) {
		User currentUser = userService.getAuthenticatedUser();
		Task task = new Task();
		task.setUser(currentUser);
		updateTaskFields(task, request);
		return taskRepository.save(task);
	}

	@Transactional
	public Task updateTask(Long id, TaskRequest request) {
		User currentUser = userService.getAuthenticatedUser();
		Task task = findTask(id);
		verifyOwnership(task, currentUser);
		updateTaskFields(task, request);
		return taskRepository.save(task);
	}

	@Transactional
	public Task markCompleted(Long id) {
		User currentUser = userService.getAuthenticatedUser();
		Task task = findTask(id);
		verifyOwnership(task, currentUser);
		task.setCompleted(true);
		return taskRepository.save(task);
	}

	@Transactional
	public void deleteTask(Long id) {
		User currentUser = userService.getAuthenticatedUser();
		Task task = findTask(id);
		verifyOwnership(task, currentUser);
		taskRepository.delete(task);
	}

	private Task findTask(Long id) {
		return taskRepository.findById(id).orElseThrow(() -> new TaskNotFoundException(id));
	}

	private void verifyOwnership(Task task, User currentUser) {
		if (task.getUser() == null || !task.getUser().getId().equals(currentUser.getId())) {
			throw new TaskAccessDeniedException("You do not have permission to access this task");
		}
	}

	private void updateTaskFields(Task task, TaskRequest request) {
		task.setTitle(request.title().trim());
		task.setDescription(request.description() == null ? null : request.description().trim());
		task.setCategory(request.category().trim());
		task.setPriority(request.priority());
		task.setDueDate(request.dueDate());
		task.setCompleted(request.completed());
	}
}
