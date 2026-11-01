package com.nivetha.studentproductivity.notification;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.nivetha.studentproductivity.user.User;

public interface PushSubscriptionRepository extends JpaRepository<PushSubscription, Long> {
	List<PushSubscription> findByUser(User user);

	Optional<PushSubscription> findByEndpoint(String endpoint);

	Optional<PushSubscription> findByEndpointAndUser(String endpoint, User user);
}
