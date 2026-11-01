package com.nivetha.studentproductivity.notification;

import java.util.List;
import java.util.Optional;
import java.time.LocalDateTime;

import org.springframework.data.jpa.repository.JpaRepository;

import com.nivetha.studentproductivity.user.User;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

	List<Notification> findByUserOrderByCreatedAtDesc(User user);

	long countByUserAndIsReadFalse(User user);

	Optional<Notification> findByIdAndUser(Long id, User user);

	boolean existsByUserAndTypeAndReferenceId(User user, NotificationType type, Long referenceId);

	boolean existsByUserAndTypeAndCreatedAtAfter(User user, NotificationType type, LocalDateTime createdAt);
}
