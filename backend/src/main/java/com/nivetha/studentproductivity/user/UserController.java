package com.nivetha.studentproductivity.user;

import java.util.Collections;
import java.util.Map;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/user")
public class UserController {

	private final UserService userService;

	public UserController(UserService userService) {
		this.userService = userService;
	}

	@GetMapping("/profile")
	public ProfileResponse getProfile() {
		User user = userService.getAuthenticatedUser();
		return ProfileResponse.from(user);
	}

	@PutMapping("/profile")
	public ProfileResponse updateProfile(@Valid @RequestBody ProfileUpdateRequest request) {
		User updated = userService.updateProfile(request);
		return ProfileResponse.from(updated);
	}

	@PutMapping("/theme")
	public ProfileResponse updateThemePreference(@Valid @RequestBody ThemePreferenceRequest request) {
		return ProfileResponse.from(userService.updateThemePreference(request));
	}

	@PutMapping("/time-zone")
	public ResponseEntity<Void> updateTimeZone(@Valid @RequestBody TimeZoneRequest request) {
		userService.updateTimeZone(request);
		return ResponseEntity.noContent().build();
	}

	@PutMapping("/password")
	public ResponseEntity<Map<String, String>> changePassword(@Valid @RequestBody PasswordChangeRequest request) {
		userService.changePassword(request);
		return ResponseEntity.ok(Collections.singletonMap("message", "Password updated successfully"));
	}
}
