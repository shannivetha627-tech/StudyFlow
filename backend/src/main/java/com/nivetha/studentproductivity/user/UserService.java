package com.nivetha.studentproductivity.user;

import java.util.Optional;
import java.util.UUID;
import java.time.DateTimeException;
import java.time.ZoneId;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nivetha.studentproductivity.auth.RegistrationRequest;
import com.nivetha.studentproductivity.auth.UnauthenticatedException;

@Service
public class UserService {

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final String dummyPasswordHash;

	public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
		this.dummyPasswordHash = passwordEncoder.encode(UUID.randomUUID().toString());
	}

	@Transactional
	public void registerUser(RegistrationRequest request) {
		String cleanEmail = request.email().trim().toLowerCase();
		String cleanPhone = normalizePhoneNumber(request.phoneNumber());

		if (request.confirmPassword() != null && !request.confirmPassword().isBlank()
				&& !request.password().equals(request.confirmPassword())) {
			throw new IllegalArgumentException("Passwords do not match");
		}

		String hashedPassword = passwordEncoder.encode(request.password());
		if (userRepository.existsByEmailIgnoreCase(cleanEmail)
				|| userRepository.existsByPhoneNumber(cleanPhone)) {
			return;
		}

		User user = new User(request.fullName().trim(), cleanEmail, cleanPhone, hashedPassword);
		userRepository.saveAndFlush(user);
	}

	@Transactional(readOnly = true)
	public User authenticate(String identifier, String rawPassword) {
		if (identifier == null || identifier.isBlank() || rawPassword == null || rawPassword.isBlank()) {
			throw new BadCredentialsException("Invalid email/phone number or password");
		}

		String trimmed = identifier.trim();
		String cleanPhone = normalizePhoneNumber(trimmed);
		String digitsOnly = trimmed.replaceAll("[^0-9]", "");
		String withPlus = "+" + digitsOnly;
		String withPlusOne = digitsOnly.length() == 10 ? "+1" + digitsOnly : withPlus;

		Optional<User> userOpt = userRepository.findByEmailIgnoreCase(trimmed)
				.or(() -> userRepository.findByPhoneNumber(trimmed))
				.or(() -> userRepository.findByPhoneNumber(cleanPhone))
				.or(() -> !digitsOnly.isBlank() ? userRepository.findByPhoneNumber(digitsOnly) : Optional.empty())
				.or(() -> !digitsOnly.isBlank() ? userRepository.findByPhoneNumber(withPlus) : Optional.empty())
				.or(() -> !digitsOnly.isBlank() ? userRepository.findByPhoneNumber(withPlusOne) : Optional.empty());

		if (userOpt.isEmpty()) {
			passwordEncoder.matches(rawPassword, dummyPasswordHash);
			throw new BadCredentialsException("Invalid email/phone number or password");
		}

		User user = userOpt.get();
		if (!passwordEncoder.matches(rawPassword, user.getPassword())) {
			throw new BadCredentialsException("Invalid email/phone number or password");
		}

		return user;
	}

	@Transactional(readOnly = true)
	public User getAuthenticatedUser() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null || !authentication.isAuthenticated()
				|| "anonymousUser".equals(authentication.getPrincipal())) {
			throw new UnauthenticatedException("Authentication required");
		}

		String identifier = authentication.getName();
		String trimmed = identifier.trim();
		String cleanPhone = normalizePhoneNumber(trimmed);
		String digitsOnly = trimmed.replaceAll("[^0-9]", "");
		String withPlus = "+" + digitsOnly;
		String withPlusOne = digitsOnly.length() == 10 ? "+1" + digitsOnly : withPlus;

		return userRepository.findByEmailIgnoreCase(trimmed)
				.or(() -> userRepository.findByPhoneNumber(trimmed))
				.or(() -> userRepository.findByPhoneNumber(cleanPhone))
				.or(() -> !digitsOnly.isBlank() ? userRepository.findByPhoneNumber(digitsOnly) : Optional.empty())
				.or(() -> !digitsOnly.isBlank() ? userRepository.findByPhoneNumber(withPlus) : Optional.empty())
				.or(() -> !digitsOnly.isBlank() ? userRepository.findByPhoneNumber(withPlusOne) : Optional.empty())
				.orElseThrow(() -> new UnauthenticatedException("Authenticated user account not found"));
	}

	@Transactional(readOnly = true)
	public User getByEmailOrPhone(String identifier) {
		String trimmed = identifier.trim();
		String cleanPhone = normalizePhoneNumber(trimmed);
		String digitsOnly = trimmed.replaceAll("[^0-9]", "");
		String withPlus = "+" + digitsOnly;
		String withPlusOne = digitsOnly.length() == 10 ? "+1" + digitsOnly : withPlus;

		return userRepository.findByEmailIgnoreCase(trimmed)
				.or(() -> userRepository.findByPhoneNumber(trimmed))
				.or(() -> userRepository.findByPhoneNumber(cleanPhone))
				.or(() -> !digitsOnly.isBlank() ? userRepository.findByPhoneNumber(digitsOnly) : Optional.empty())
				.or(() -> !digitsOnly.isBlank() ? userRepository.findByPhoneNumber(withPlus) : Optional.empty())
				.or(() -> !digitsOnly.isBlank() ? userRepository.findByPhoneNumber(withPlusOne) : Optional.empty())
				.orElseThrow(() -> new UnauthenticatedException("User not found"));
	}

	@Transactional
	public User updateProfile(ProfileUpdateRequest request) {
		User user = getAuthenticatedUser();
		String cleanPhone = normalizePhoneNumber(request.phoneNumber());

		if (!cleanPhone.equalsIgnoreCase(user.getPhoneNumber())
				&& userRepository.existsByPhoneNumber(cleanPhone)) {
			throw new IllegalArgumentException("Phone number is already associated with another account");
		}

		user.setFullName(request.fullName().trim());
		user.setPhoneNumber(cleanPhone);
		return userRepository.save(user);
	}

	@Transactional
	public User updateThemePreference(ThemePreferenceRequest request) {
		User user = getAuthenticatedUser();
		user.setThemePreference(request.theme());
		return userRepository.save(user);
	}

	@Transactional
	public void updateTimeZone(TimeZoneRequest request) {
		User user = getAuthenticatedUser();
		try {
			ZoneId.of(request.timeZone());
		} catch (DateTimeException exception) {
			throw new IllegalArgumentException("Invalid time zone");
		}
		user.setTimeZone(request.timeZone());
		userRepository.save(user);
	}

	@Transactional
	public void changePassword(PasswordChangeRequest request) {
		User user = getAuthenticatedUser();

		if (!passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
			throw new BadCredentialsException("Current password does not match");
		}

		if (!request.newPassword().equals(request.confirmPassword())) {
			throw new IllegalArgumentException("New passwords do not match");
		}

		user.setPassword(passwordEncoder.encode(request.newPassword()));
		userRepository.save(user);
	}

	public static String normalizePhoneNumber(String phone) {
		if (phone == null) return "";
		String trimmed = phone.trim();
		boolean hasPlus = trimmed.startsWith("+");
		String digitsOnly = trimmed.replaceAll("[^0-9]", "");
		return hasPlus ? "+" + digitsOnly : digitsOnly;
	}

}
