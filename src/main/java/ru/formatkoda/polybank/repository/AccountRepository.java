package ru.formatkoda.polybank.repository;

import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;
import ru.formatkoda.polybank.domain.account.AccountNumber;
import ru.formatkoda.polybank.domain.user.UserLogin;

import static ru.formatkoda.polybank.jooq.generated.tables.Users.USERS;
import static ru.formatkoda.polybank.jooq.generated.tables.Accounts.ACCOUNTS;

@Repository
@RequiredArgsConstructor
public class AccountRepository {
	private final DSLContext dsl;

	public boolean existsByNumberAndUserLogin(AccountNumber number, UserLogin login) {
		return dsl.fetchExists(
				dsl.selectOne()
						.from(ACCOUNTS)
						.join(USERS).on(ACCOUNTS.USER_ID.eq(USERS.ID))
						.where(ACCOUNTS.NUMBER.eq(number.value()))
						.and(USERS.LOGIN.eq(login.value()))
		);
	}
}
