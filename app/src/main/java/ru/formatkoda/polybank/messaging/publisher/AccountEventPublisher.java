package ru.formatkoda.polybank.messaging.publisher;

import ru.formatkoda.polybank.domain.account.AccountEntity;

public interface AccountEventPublisher {

	void publishAccountCreated(AccountEntity account);
}
