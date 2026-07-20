package ru.formatkoda.authorization.auth.repository;

import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.springframework.stereotype.Repository;
import ru.formatkoda.authorization.auth.domain.UserEmail;
import ru.formatkoda.authorization.auth.domain.UserEntity;
import ru.formatkoda.authorization.auth.domain.UserLogin;

import java.util.List;
import java.util.Optional;

import static ru.formatkoda.authorization.jooq.generated.Tables.ROLES;
import static ru.formatkoda.authorization.jooq.generated.Tables.USERS;
import static ru.formatkoda.authorization.jooq.generated.Tables.USERS_ROLES;

@Repository
@RequiredArgsConstructor
public class UserRepository {

	private final DSLContext dsl;

	public Optional<UserEntity> findUserByLogin(UserLogin login) {
		return dsl.selectFrom(USERS)
				.where(USERS.LOGIN.eq(login.value()))
				.fetchOptional(this::toEntity);
	}

	public List<String> findAllUserRoles(UserEntity user) {
		return dsl.select(ROLES.NAME)
				.from(USERS_ROLES)
				.join(ROLES)
				.on(USERS_ROLES.ROLE_ID.eq(ROLES.ID))
				.where(USERS_ROLES.USER_ID.eq(user.id()))
				.fetch(ROLES.NAME);
	}

	private UserEntity toEntity(Record userRecord) {
		return new UserEntity(
				userRecord.get(USERS.ID),
				new UserLogin(userRecord.get(USERS.LOGIN)),
				new UserEmail(userRecord.get(USERS.EMAIL)),
				userRecord.get(USERS.NAME),
				userRecord.get(USERS.LAST_NAME),
				userRecord.get(USERS.PASSWORD_HASH),
				userRecord.get(USERS.CREATED_AT),
				userRecord.get(USERS.BLOCKED_AT)
		);
	}
}
