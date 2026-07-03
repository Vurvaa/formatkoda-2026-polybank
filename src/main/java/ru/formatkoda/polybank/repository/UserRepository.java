package ru.formatkoda.polybank.repository;

import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.exception.DataAccessException;
import org.springframework.stereotype.Repository;
import ru.formatkoda.polybank.domain.user.UserEntity;
import ru.formatkoda.polybank.domain.user.UserLogin;
import ru.formatkoda.polybank.jooq.generated.tables.records.UsersRecord;

import static ru.formatkoda.polybank.jooq.generated.Tables.USERS;
import static ru.formatkoda.polybank.jooq.generated.Tables.USERS_ROLES;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class UserRepository {

    private final DSLContext dsl;

    public Optional<UserEntity> findUserByLogin(UserLogin login) {
        return dsl.selectFrom(USERS)
                .where(USERS.LOGIN.eq(login.value()))
                .fetchOptional(this::toEntity);
    }

    public long createUserAndReturnId(UserEntity user) {
        Long id = dsl.insertInto(USERS)
                .set(toRecord(user))
                .returning(USERS.ID)
                .fetchOne(USERS.ID);

        if (id == null)
            throw new DataAccessException("error inserting entity with login: " + user.login());

        return id;
    }

    public void bindUserWithRole(long userId, long roleId) {
        dsl.insertInto(USERS_ROLES,
                        USERS_ROLES.USER_ID,
                        USERS_ROLES.ROLE_ID)
                .values(userId, roleId)
                .execute();
    }

    private UsersRecord toRecord(UserEntity user) {
        return dsl.newRecord(USERS)
                .setLogin(user.login().value())
                .setName(user.name())
                .setLastName(user.lastName())
                .setPasswordHash(user.passwordHash())
                .setCreatedAt(user.createdAt())
                .setBlockedAt(user.blockedAt());
    }

    private UserEntity toEntity(Record r) {
        return new UserEntity(
                r.get(USERS.ID),
                new UserLogin(r.get(USERS.LOGIN)),
                r.get(USERS.NAME),
                r.get(USERS.LAST_NAME),
                r.get(USERS.PASSWORD_HASH),
                r.get(USERS.CREATED_AT),
                r.get(USERS.BLOCKED_AT)
        );
    }
}
