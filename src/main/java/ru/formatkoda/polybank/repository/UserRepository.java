package ru.formatkoda.polybank.repository;

import lombok.AllArgsConstructor;
import org.jooq.DSLContext;
import org.jooq.exception.DataAccessException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Repository;
import ru.formatkoda.polybank.domain.User;
import ru.formatkoda.polybank.dto.UserRegistrationDto;
import ru.formatkoda.polybank.jooq.generated.tables.Users;
import ru.formatkoda.polybank.jooq.generated.tables.records.UsersRecord;

import java.time.OffsetDateTime;
import java.util.Optional;

@Repository
@AllArgsConstructor
public class UserRepository {
    private final DSLContext dsl;
    private final PasswordEncoder passwordEncoder;

    public Optional<User> findUserByLogin(String login) {
        return dsl.selectFrom(Users.USERS)
                .where(Users.USERS.LOGIN.eq(login))
                .fetchOptionalInto(User.class);
    }

    public String insertUserAndReturnLogin(UserRegistrationDto user) {
        return dsl.insertInto(Users.USERS)
                .set(dsl.newRecord(Users.USERS, mappingUser(user)))
                .returning(Users.USERS.LOGIN)
                .fetchOptional()
                .orElseThrow(() -> new DataAccessException("Error inserting entity: " + user.login()))
                .get(Users.USERS.LOGIN);
    }

    private UsersRecord mappingUser(UserRegistrationDto user) {
        UsersRecord userEntity = dsl.newRecord(Users.USERS);
        userEntity.setLogin(user.login());
        userEntity.setName(user.name());
        userEntity.setLastName(user.lastName());
        userEntity.setPasswordHash(passwordEncoder.encode(user.password()));
        userEntity.setCreatedAt(OffsetDateTime.now());

        return userEntity;
    }
}
