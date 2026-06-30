package ru.formatkoda.polybank.util.mapper;

import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.jooq.RecordUnmapper;
import org.jooq.exception.MappingException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import ru.formatkoda.polybank.dto.UserRegistrationDto;
import ru.formatkoda.polybank.jooq.generated.tables.Users;
import ru.formatkoda.polybank.jooq.generated.tables.records.UsersRecord;

import java.time.OffsetDateTime;

@RequiredArgsConstructor
@Component
public class UsersRecordUnMapper implements RecordUnmapper<UserRegistrationDto, UsersRecord> {
    private final DSLContext dsl;
    private final PasswordEncoder passwordEncoder;

    @Override
    public UsersRecord unmap(UserRegistrationDto source) throws MappingException {
        UsersRecord usersRecord = dsl.newRecord(Users.USERS);
        usersRecord.setLogin(source.login());
        usersRecord.setName(source.name());
        usersRecord.setLastName(source.lastName());
        usersRecord.setPasswordHash(passwordEncoder.encode(source.password()));
        usersRecord.setCreatedAt(OffsetDateTime.now());

        return usersRecord;
    }
}
