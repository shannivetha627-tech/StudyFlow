package com.nivetha.studentproductivity.auth;

import java.util.Collections;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolderStrategy;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nivetha.studentproductivity.user.User;
import com.nivetha.studentproductivity.user.UserService;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

	private final UserService userService;
	private final AuthenticationManager authenticationManager;
	private final SecurityContextRepository securityContextRepository;
	private final SecurityContextHolderStrategy securityContextHolderStrategy =
			SecurityContextHolder.getContextHolderStrategy();

	public AuthController(
			UserService userService,
			AuthenticationManager authenticationManager,
			SecurityContextRepository securityContextRepository
	) {
		this.userService = userService;
		this.authenticationManager = authenticationManager;
		this.securityContextRepository = securityContextRepository;
	}

	@PostMapping("/register")
	public ResponseEntity<RegistrationResponse> register(@Valid @RequestBody RegistrationRequest request) {
		try {
			userService.registerUser(request);
		} catch (DataIntegrityViolationException exception) {
			return ResponseEntity.ok(RegistrationResponse.generic());
		}
		return ResponseEntity.ok(RegistrationResponse.generic());
	}

	@PostMapping("/login")
	public ResponseEntity<UserResponse> login(
			@Valid @RequestBody LoginRequest request,
			HttpServletRequest httpRequest,
			HttpServletResponse httpResponse
	) {
		User user = userService.authenticate(request.identifier(), request.password());

		Authentication authentication = authenticationManager.authenticate(
				new UsernamePasswordAuthenticationToken(user.getEmail(), request.password())
		);

		httpRequest.getSession(true);
		httpRequest.changeSessionId();

		SecurityContext context = securityContextHolderStrategy.createEmptyContext();
		context.setAuthentication(authentication);
		securityContextHolderStrategy.setContext(context);
		securityContextRepository.saveContext(context, httpRequest, httpResponse);

		return ResponseEntity.ok(UserResponse.from(user));
	}

	@GetMapping("/me")
	public ResponseEntity<UserResponse> getCurrentUser() {
		User user = userService.getAuthenticatedUser();
		return ResponseEntity.ok(UserResponse.from(user));
	}

	@PostMapping("/logout")
	public ResponseEntity<Map<String, String>> logout(
			HttpServletRequest request,
			HttpServletResponse response,
			Authentication authentication
	) {
		if (authentication != null) {
			new SecurityContextLogoutHandler().logout(request, response, authentication);
		}
		HttpSession session = request.getSession(false);
		if (session != null) {
			session.invalidate();
		}
		SecurityContextHolder.clearContext();
		return ResponseEntity.ok(Collections.singletonMap("message", "Logged out successfully"));
	}

	@GetMapping("/csrf")
	public ResponseEntity<Map<String, String>> getCsrfToken(HttpServletRequest request) {
		CsrfToken csrfToken = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
		if (csrfToken == null) {
			return ResponseEntity.ok(Collections.emptyMap());
		}
		return ResponseEntity.ok(Map.of(
				"token", csrfToken.getToken(),
				"headerName", csrfToken.getHeaderName(),
				"parameterName", csrfToken.getParameterName()
		));
	}
}
