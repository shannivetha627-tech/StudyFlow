package com.nivetha.studentproductivity.notification;

import java.net.URI;
import java.util.Locale;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.nivetha.studentproductivity.user.User;
import com.nivetha.studentproductivity.user.UserService;

@Service
public class PushSubscriptionService {

	private static final Set<String> PUSH_HOSTS = Set.of(
			"fcm.googleapis.com",
			"push.services.mozilla.com",
			"notify.windows.com",
			"push.apple.com"
	);

	private final PushSubscriptionRepository repository;
	private final UserService userService;

	public PushSubscriptionService(PushSubscriptionRepository repository, UserService userService) {
		this.repository = repository;
		this.userService = userService;
	}

	@Transactional
	public PushSubscription save(PushSubscriptionRequest request) {
		User user = userService.getAuthenticatedUser();
		validateEndpoint(request.endpoint());
		PushSubscription subscription = repository.findByEndpoint(request.endpoint())
				.orElseGet(PushSubscription::new);
		if (subscription.getUser() != null && !subscription.getUser().getId().equals(user.getId())) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "This browser subscription is already linked to another account.");
		}
		subscription.setUser(user);
		subscription.setEndpoint(request.endpoint());
		subscription.setPublicKey(request.keys().p256dh());
		subscription.setAuthKey(request.keys().auth());
		return repository.save(subscription);
	}

	@Transactional
	public void remove(PushSubscriptionRequest request) {
		User user = userService.getAuthenticatedUser();
		repository.findByEndpointAndUser(request.endpoint(), user).ifPresent(repository::delete);
	}

	private void validateEndpoint(String endpoint) {
		try {
			URI uri = URI.create(endpoint);
			String host = uri.getHost() == null ? "" : uri.getHost().toLowerCase(Locale.ROOT);
			boolean trustedHost = PUSH_HOSTS.stream()
					.anyMatch(allowed -> host.equals(allowed) || host.endsWith("." + allowed));
			if (!"https".equalsIgnoreCase(uri.getScheme())
					|| !trustedHost
					|| uri.getUserInfo() != null
					|| (uri.getPort() != -1 && uri.getPort() != 443)) {
				throw new IllegalArgumentException();
			}
		} catch (IllegalArgumentException exception) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Push endpoint must use HTTPS on a supported browser push service.");
		}
	}
}
