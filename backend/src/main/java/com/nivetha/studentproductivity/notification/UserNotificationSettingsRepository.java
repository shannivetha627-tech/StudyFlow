package com.nivetha.studentproductivity.notification;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.nivetha.studentproductivity.user.User;

public interface UserNotificationSettingsRepository extends JpaRepository<UserNotificationSettings, Long> {

	Optional<UserNotificationSettings> findByUser(User user);
}

