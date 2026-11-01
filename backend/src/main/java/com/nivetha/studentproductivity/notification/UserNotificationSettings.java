package com.nivetha.studentproductivity.notification;

import java.time.LocalTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.nivetha.studentproductivity.user.User;

@Entity
@Table(name = "user_notification_settings")
public class UserNotificationSettings {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@OneToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false, unique = true)
	@JsonIgnore
	private User user;

	@Column(nullable = false)
	private boolean notificationsEnabled = true;

	@Column(nullable = false)
	private int reminderMinutes = 10;

	@Column(nullable = false)
	private boolean soundEnabled = true;

	@Column(nullable = false)
	private boolean dailyReminderEnabled = true;

	@Column(nullable = false)
	private LocalTime dailyReminderTime = LocalTime.of(9, 0);

	public UserNotificationSettings() {
	}

	public UserNotificationSettings(User user) {
		this.user = user;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public User getUser() {
		return user;
	}

	public void setUser(User user) {
		this.user = user;
	}

	public boolean isNotificationsEnabled() {
		return notificationsEnabled;
	}

	public void setNotificationsEnabled(boolean notificationsEnabled) {
		this.notificationsEnabled = notificationsEnabled;
	}

	public int getReminderMinutes() {
		return reminderMinutes;
	}

	public void setReminderMinutes(int reminderMinutes) {
		this.reminderMinutes = reminderMinutes;
	}

	public boolean isSoundEnabled() {
		return soundEnabled;
	}

	public void setSoundEnabled(boolean soundEnabled) {
		this.soundEnabled = soundEnabled;
	}

	public boolean isDailyReminderEnabled() {
		return dailyReminderEnabled;
	}

	public void setDailyReminderEnabled(boolean dailyReminderEnabled) {
		this.dailyReminderEnabled = dailyReminderEnabled;
	}

	public LocalTime getDailyReminderTime() {
		return dailyReminderTime;
	}

	public void setDailyReminderTime(LocalTime dailyReminderTime) {
		this.dailyReminderTime = dailyReminderTime;
	}
}

