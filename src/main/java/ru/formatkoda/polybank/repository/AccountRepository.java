package ru.formatkoda.polybank.repository;

import org.springframework.stereotype.Repository;
import ru.formatkoda.polybank.domain.account.AccountNumber;
import ru.formatkoda.polybank.domain.user.UserLogin;

@Repository
public class AccountRepository {
	public boolean existsByNumberAndUserLogin(UserLogin login, AccountNumber number) {
		return false;  // todo implement repo when jooq init task is merged
	}
}
