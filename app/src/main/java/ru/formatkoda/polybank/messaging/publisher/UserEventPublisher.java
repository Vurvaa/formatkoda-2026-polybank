package ru.formatkoda.polybank.messaging.publisher;

import ru.formatkoda.polybank.domain.user.UserEntity;

public interface UserEventPublisher {

	void publishUserRegistered(UserEntity user, String role);
}
