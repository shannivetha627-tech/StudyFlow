package com.nivetha.studentproductivity.task;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.nivetha.studentproductivity.user.User;

public interface TaskRepository extends JpaRepository<Task, Long> {

	List<Task> findByUserOrderByCreatedAtDesc(User user);

	Optional<Task> findByIdAndUser(Long id, User user);
}
