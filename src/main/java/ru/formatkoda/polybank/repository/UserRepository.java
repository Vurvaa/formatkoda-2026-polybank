package ru.formatkoda.polybank.repository;

import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.jooq.exception.DataAccessException;
import org.jooq.Record;
import org.springframework.stereotype.Repository;
import ru.formatkoda.polybank.domain.user.UserEntity;
import ru.formatkoda.polybank.domain.user.UserLogin;
import ru.formatkoda.polybank.domain.user.UserWithRolesView;
import ru.formatkoda.polybank.jooq.generated.tables.records.UsersRecord;
import ru.formatkoda.polybank.util.pagination.PageRequest;

import static ru.formatkoda.polybank.jooq.generated.Tables.ROLES;
import static ru.formatkoda.polybank.jooq.generated.Tables.USERS;
import static ru.formatkoda.polybank.jooq.generated.Tables.USERS_ROLES;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.List;
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

    public List<UserEntity> findAllUsers(PageRequest pageRequest) {
        return dsl
                .selectFrom(USERS)
                .orderBy(USERS.CREATED_AT.desc(), USERS.LOGIN.desc())
                .limit(pageRequest.size())
                .offset(pageRequest.offset())
                .fetchInto(UserEntity.class);
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

    public Optional<UserEntity> blockUserById(Long userId) {
        return dsl.update(USERS)
                .set(USERS.BLOCKED_AT, OffsetDateTime.now(ZoneOffset.UTC))
                .where(USERS.ID.eq(userId))
                .returning()
                .fetchOptionalInto(UserEntity.class);
    }

    public Optional<UserEntity> unBlockUserById(Long userId) {
        return dsl.update(USERS)
                .setNull(USERS.BLOCKED_AT)
                .where(USERS.ID.eq(userId))
                .returning()
                .fetchOptionalInto(UserEntity.class);
    }

    public List<String> findAllUserRoles(UserEntity user) {
        return dsl.select(ROLES.NAME)
                .from(USERS_ROLES)
                .join(ROLES)
                .on(USERS_ROLES.ROLE_ID.eq(ROLES.ID))
                .where(USERS_ROLES.USER_ID.eq(user.id()))
                .fetch(ROLES.NAME);
    }

    public UserWithRolesView toUserWithRolesView(UserEntity user) {
        return new UserWithRolesView(
                user.login(),
                user.name(),
                user.lastName(),
                findAllUserRoles(user),
                user.createdAt(),
                user.blockedAt()
        );
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
