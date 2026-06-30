package ru.formatkoda.polybank.repository;

import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.springframework.stereotype.Repository;
import ru.formatkoda.polybank.domain.user.UserEntity;

import java.util.Optional;

import static ru.formatkoda.polybank.jooq.generated.Tables.USERS;

@Repository
@RequiredArgsConstructor
public class UserRepository {
	private final DSLContext dsl;

	public Optional<UserEntity> findUserByLogin(String login) {
		return dsl
				.select(
						USERS.ID,
						USERS.LOGIN,
						USERS.NAME,
						USERS.LAST_NAME,
						USERS.PASSWORD_HASH,
						USERS.CREATED_AT,
						USERS.BLOCKED_AT
				)
				.from(USERS)
				.where(USERS.LOGIN.eq(login))
				.fetchOptional(this::toEntity);
	}

	private UserEntity toEntity(Record r) {
		return new UserEntity(
				r.get(USERS.ID),
				r.get(USERS.LOGIN),
				r.get(USERS.NAME),
				r.get(USERS.LAST_NAME),
				r.get(USERS.PASSWORD_HASH),
				r.get(USERS.CREATED_AT),
				r.get(USERS.BLOCKED_AT)
		);
	}
}
