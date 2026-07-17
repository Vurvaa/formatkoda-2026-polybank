package ru.formatkoda.polybank.repository;

import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.springframework.stereotype.Repository;
import ru.formatkoda.polybank.domain.user.UserEntity;
import ru.formatkoda.polybank.domain.user.UserLogin;
import ru.formatkoda.polybank.jooq.generated.tables.records.UsersRecord;
import ru.formatkoda.polybank.util.pagination.PageRequest;
import ru.formatkoda.polybank.domain.user.UserEmail;

import static ru.formatkoda.polybank.jooq.generated.Tables.ROLES;
import static ru.formatkoda.polybank.jooq.generated.Tables.USERS;
import static ru.formatkoda.polybank.jooq.generated.Tables.USERS_ROLES;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
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

    public Optional<UserEntity> findUserById(long userId) {
        return dsl.selectFrom(USERS)
                .where(USERS.ID.eq(userId))
                .fetchOptional(this::toEntity);
    }

    public Optional<UserEntity> findUserByEmail(UserEmail email) {
        return dsl.selectFrom(USERS)
                .where(USERS.EMAIL.eq(email.value()))
                .fetchOptional(this::toEntity);
    }

    public boolean existsUserByEmail(UserEmail email) {
        return dsl.fetchExists(
                dsl.selectOne()
                        .from(USERS)
                        .where(USERS.EMAIL.eq(email.value()))
        );
    }

    public Optional<UserEntity> changeUserEmail(UserLogin userLogin, UserEmail email) {
        return dsl.update(USERS)
                .set(USERS.EMAIL, email.value())
                .where(USERS.LOGIN.eq(userLogin.value()))
                .returning()
                .fetchOptional(this::toEntity);
    }

    public Optional<UserEntity> changeUserPassword(UserLogin userLogin, String newPasswordHash) {
        return dsl.update(USERS)
                .set(USERS.PASSWORD_HASH, newPasswordHash)
                .where(USERS.LOGIN.eq(userLogin.value()))
                .returning()
                .fetchOptional(this::toEntity);
    }

    public List<UserEntity> findAllUsers(PageRequest pageRequest) {
        return dsl
                .selectFrom(USERS)
                .orderBy(USERS.CREATED_AT.desc(), USERS.LOGIN.desc())
                .limit(pageRequest.size())
                .offset(pageRequest.offset())
                .fetch(this::toEntity);

    }

    public Long countAll() {
        return dsl.selectCount()
                .from(USERS)
                .fetchOne(0, Long.class);
    }

    public Optional<Long> createUserAndReturnId(UserEntity user) {
        return dsl.insertInto(USERS)
                .set(toRecord(user))
                .returning(USERS.ID)
                .fetchOptional(USERS.ID);
    }

    public Optional<UserEntity> createUser(UserEntity user) {
        return dsl
                .insertInto(USERS)
                .set(toRecord(user))
                .returning()
                .fetchOptional(this::toEntity);
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
                .fetchOptional(this::toEntity);
    }

    public Optional<UserEntity> unBlockUserById(Long userId) {
        return dsl.update(USERS)
                .setNull(USERS.BLOCKED_AT)
                .where(USERS.ID.eq(userId))
                .returning()
                .fetchOptional(this::toEntity);
    }

    public boolean removeUserRole(Long userId, Long roleId) {
        int deletedRoles = dsl
                .delete(USERS_ROLES)
                .where(USERS_ROLES.USER_ID.eq(userId)
                        .and(USERS_ROLES.ROLE_ID.eq(roleId))
                )
                .execute();

        return deletedRoles == 1;
    }

    public List<String> findAllUserRoles(UserEntity user) {
        return dsl.select(ROLES.NAME)
                .from(USERS_ROLES)
                .join(ROLES)
                .on(USERS_ROLES.ROLE_ID.eq(ROLES.ID))
                .where(USERS_ROLES.USER_ID.eq(user.id()))
                .fetch(ROLES.NAME);
    }

    private UsersRecord toRecord(UserEntity user) {
        return dsl.newRecord(USERS)
                .setLogin(user.login().value())
                .setEmail(user.email().value())
                .setName(user.name())
                .setLastName(user.lastName())
                .setPasswordHash(user.passwordHash())
                .setCreatedAt(user.createdAt())
                .setBlockedAt(user.blockedAt());
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
