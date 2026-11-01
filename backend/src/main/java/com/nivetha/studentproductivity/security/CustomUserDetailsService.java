package com.nivetha.studentproductivity.security;

import java.util.Collections;
import java.util.Optional;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.nivetha.studentproductivity.user.User;
import com.nivetha.studentproductivity.user.UserRepository;
import com.nivetha.studentproductivity.user.UserService;

@Service
public class CustomUserDetailsService implements UserDetailsService {

	private final UserRepository userRepository;

	public CustomUserDetailsService(UserRepository userRepository) {
		this.userRepository = userRepository;
	}

	@Override
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		if (username == null || username.isBlank()) {
			throw new UsernameNotFoundException("Username cannot be empty");
		}

		String trimmed = username.trim();
		String cleanPhone = UserService.normalizePhoneNumber(trimmed);
		String digitsOnly = trimmed.replaceAll("[^0-9]", "");
		String withPlus = "+" + digitsOnly;
		String withPlusOne = digitsOnly.length() == 10 ? "+1" + digitsOnly : withPlus;

		Optional<User> userOpt = userRepository.findByEmailIgnoreCase(trimmed)
				.or(() -> userRepository.findByPhoneNumber(trimmed))
				.or(() -> userRepository.findByPhoneNumber(cleanPhone))
				.or(() -> !digitsOnly.isBlank() ? userRepository.findByPhoneNumber(digitsOnly) : Optional.empty())
				.or(() -> !digitsOnly.isBlank() ? userRepository.findByPhoneNumber(withPlus) : Optional.empty())
				.or(() -> !digitsOnly.isBlank() ? userRepository.findByPhoneNumber(withPlusOne) : Optional.empty());

		User user = userOpt.orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

		return new org.springframework.security.core.userdetails.User(
				user.getEmail(),
				user.getPassword(),
				Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
		);
	}
}
