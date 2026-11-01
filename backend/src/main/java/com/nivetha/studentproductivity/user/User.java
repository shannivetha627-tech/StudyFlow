package com.nivetha.studentproductivity.user;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Entity
@Table(name = "users")
public class User {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@NotBlank
	@Size(min = 2, max = 100)
	@Column(nullable = false, length = 100)
	private String fullName;

	@NotBlank
	@Email
	@Size(max = 120)
	@Column(nullable = false, unique = true, length = 120)
	private String email;

	@NotBlank
	@Size(min = 7, max = 25)
	@Column(nullable = false, unique = true, length = 25)
	private String phoneNumber;

	@NotBlank
	@Column(nullable = false, length = 100)
	private String password;

	@Column(nullable = false, updatable = false)
	private LocalDateTime createdAt;

	@Column(length = 16)
	private String themePreference = "dark";

	@Column(length = 64)
	private String timeZone = "UTC";

	public User() {
	}

	public User(String fullName, String email, String phoneNumber, String password) {
		this.fullName = fullName;
		this.email = email;
		this.phoneNumber = phoneNumber;
		this.password = password;
	}

	@PrePersist
	void setCreatedAt() {
		if (createdAt == null) {
			createdAt = LocalDateTime.now();
		}
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getFullName() {
		return fullName;
	}

	public void setFullName(String fullName) {
		this.fullName = fullName;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getPhoneNumber() {
		return phoneNumber;
	}

	public void setPhoneNumber(String phoneNumber) {
		this.phoneNumber = phoneNumber;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(LocalDateTime createdAt) {
		this.createdAt = createdAt;
	}

	public String getThemePreference() {
		return themePreference == null ? "dark" : themePreference;
	}

	public void setThemePreference(String themePreference) {
		this.themePreference = themePreference;
	}

	public String getTimeZone() {
		return timeZone == null || timeZone.isBlank() ? "UTC" : timeZone;
	}

	public void setTimeZone(String timeZone) {
		this.timeZone = timeZone;
	}
}
