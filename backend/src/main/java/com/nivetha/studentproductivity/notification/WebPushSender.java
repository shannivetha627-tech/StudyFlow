package com.nivetha.studentproductivity.notification;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.Security;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import nl.martijndwars.webpush.PushService;
import nl.martijndwars.webpush.Subscription;
import org.apache.http.HttpResponse;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.jose4j.lang.JoseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.nivetha.studentproductivity.user.User;

@Component
public class WebPushSender {

	private static final Logger logger = LoggerFactory.getLogger(WebPushSender.class);

	private final PushSubscriptionRepository subscriptionRepository;
	private final ObjectMapper objectMapper = new ObjectMapper();
	private final String publicKey;
	private final String privateKey;
	private final String subject;
	private volatile PushService pushService;

	public WebPushSender(
			PushSubscriptionRepository subscriptionRepository,
			@Value("${studyflow.push.vapid-public-key:}") String publicKey,
			@Value("${studyflow.push.vapid-private-key:}") String privateKey,
			@Value("${studyflow.push.vapid-subject:}") String subject
	) {
		this.subscriptionRepository = subscriptionRepository;
		this.publicKey = publicKey;
		this.privateKey = privateKey;
		this.subject = subject;
		if (publicKey.isBlank() != privateKey.isBlank()) {
			throw new IllegalStateException("Configure both VAPID public and private keys.");
		}
		if (!publicKey.isBlank() && subject.isBlank()) {
			throw new IllegalStateException("Configure studyflow.push.vapid-subject when enabling Web Push.");
		}
	}

	public boolean isConfigured() {
		return !publicKey.isBlank() && !privateKey.isBlank();
	}

	public String getPublicKey() {
		return isConfigured() ? publicKey : "";
	}

	public void sendToUser(User user, com.nivetha.studentproductivity.notification.Notification notification) {
		if (!isConfigured()) {
			return;
		}
		List<PushSubscription> subscriptions = subscriptionRepository.findByUser(user);
		for (PushSubscription stored : subscriptions) {
			try {
				Subscription subscription = new Subscription(
						stored.getEndpoint(),
						new Subscription.Keys(stored.getPublicKey(), stored.getAuthKey())
				);
				HttpResponse response = getPushService().send(new nl.martijndwars.webpush.Notification(
						subscription,
						objectMapper.writeValueAsString(Map.of(
								"title", notification.getTitle(),
								"body", notification.getMessage(),
								"url", "/index.html?view=notifications",
								"tag", "studyflow-notification-" + notification.getId()
						))
				));
				int status = response.getStatusLine().getStatusCode();
				if (status == 404 || status == 410) {
					subscriptionRepository.delete(stored);
				} else if (status < 200 || status >= 300) {
					logger.warn("Web Push delivery for subscription {} returned HTTP {}.", stored.getId(), status);
				}
			} catch (JsonProcessingException exception) {
				logger.error("Could not serialize a StudyFlow push notification.", exception);
				return;
			} catch (IOException | GeneralSecurityException | JoseException | ExecutionException exception) {
				logger.warn("Web Push delivery failed for subscription {} ({}).", stored.getId(), exception.getClass().getSimpleName());
			} catch (InterruptedException exception) {
				Thread.currentThread().interrupt();
				logger.warn("Web Push delivery was interrupted for subscription {}.", stored.getId());
				return;
			}
		}
	}

	private PushService getPushService() throws GeneralSecurityException {
		PushService current = pushService;
		if (current == null) {
			synchronized (this) {
				current = pushService;
				if (current == null) {
					Security.addProvider(new BouncyCastleProvider());
					current = new PushService(publicKey, privateKey, subject);
					pushService = current;
				}
			}
		}
		return current;
	}
}
