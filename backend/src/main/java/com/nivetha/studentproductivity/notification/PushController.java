package com.nivetha.studentproductivity.notification;

import java.util.Map;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/push")
public class PushController {

	private final WebPushSender webPushSender;
	private final PushSubscriptionService subscriptionService;

	public PushController(WebPushSender webPushSender, PushSubscriptionService subscriptionService) {
		this.webPushSender = webPushSender;
		this.subscriptionService = subscriptionService;
	}

	@GetMapping("/config")
	public Map<String, Object> getConfiguration() {
		return Map.of("configured", webPushSender.isConfigured(), "publicKey", webPushSender.getPublicKey());
	}

	@PostMapping("/subscriptions")
	public ResponseEntity<Void> register(@Valid @RequestBody PushSubscriptionRequest request) {
		subscriptionService.save(request);
		return ResponseEntity.noContent().build();
	}

	@PutMapping("/subscriptions")
	public ResponseEntity<Void> update(@Valid @RequestBody PushSubscriptionRequest request) {
		subscriptionService.save(request);
		return ResponseEntity.noContent().build();
	}

	@DeleteMapping("/subscriptions")
	public ResponseEntity<Void> remove(@Valid @RequestBody PushSubscriptionRequest request) {
		subscriptionService.remove(request);
		return ResponseEntity.noContent().build();
	}
}
