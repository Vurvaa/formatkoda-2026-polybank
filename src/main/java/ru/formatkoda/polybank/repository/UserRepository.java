package ru.formatkoda.polybank.repository;

import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.jooq.exception.DataAccessException;
import org.springframework.stereotype.Repository;
import ru.formatkoda.polybank.domain.UserEntity;
import ru.formatkoda.polybank.util.mapper.UserEntityUnmapper;

import static ru.formatkoda.polybank.jooq.generated.Tables.USERS;
import static ru.formatkoda.polybank.jooq.generated.Tables.USERS_ROLES;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class UserRepository {

    private final DSLContext dsl;
    private final UserEntityUnmapper recordUnmapper;

    public Optional<UserEntity> findUserByLogin(String login) {
        return dsl.selectFrom(USERS)
                .where(USERS.LOGIN.eq(login))
                .fetchOptionalInto(UserEntity.class);
    }

    public long createUserAndReturnId(UserEntity user) {
        Long id = dsl.insertInto(USERS)
                .set(recordUnmapper.unmap(user))
                .returning(USERS.ID)
                .fetchOne(USERS.ID);

        if (id == null)
            throw new DataAccessException("Error inserting entity with login: " + user.login());

        return id;
    }

    public void bindUserWithRole(long userId, long roleId) {
        dsl.insertInto(USERS_ROLES,
                        USERS_ROLES.USER_ID,
                        USERS_ROLES.ROLE_ID)
                .values(userId, roleId)
                .execute();
    }
}
