package ru.formatkoda.polybank.repository;

import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.jooq.exception.DataAccessException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Repository;
import ru.formatkoda.polybank.domain.UserEntity;
import ru.formatkoda.polybank.dto.UserRegistrationDto;
import ru.formatkoda.polybank.jooq.generated.tables.Users;
import ru.formatkoda.polybank.jooq.generated.tables.records.UsersRecord;
import ru.formatkoda.polybank.util.mapper.UsersRecordUnMapper;

import java.time.OffsetDateTime;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class UserRepository {
    private final DSLContext dsl;
    private final UsersRecordUnMapper recordUnMapper;

    public Optional<UserEntity> findUserByLogin(String login) {
        return dsl.selectFrom(Users.USERS)
                .where(Users.USERS.LOGIN.eq(login))
                .fetchOptionalInto(UserEntity.class);
    }

    public long insertUserAndReturnId(UserRegistrationDto user) {
        return dsl.insertInto(Users.USERS)
                .set(recordUnMapper.unmap(user))
                .returning(Users.USERS.LOGIN)
                .fetchOptional()
                .orElseThrow(() -> new DataAccessException("Error inserting entity: " + user.login()))
                .get(Users.USERS.ID);
    }
}
