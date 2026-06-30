package ru.formatkoda.polybank.util.mapper;

import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.jooq.RecordUnmapper;
import org.jooq.exception.MappingException;
import org.springframework.stereotype.Component;
import ru.formatkoda.polybank.domain.UserEntity;
import ru.formatkoda.polybank.jooq.generated.tables.records.UsersRecord;
import static ru.formatkoda.polybank.jooq.generated.Tables.USERS;

import java.time.OffsetDateTime;


@RequiredArgsConstructor
@Component
public class UserEntityUnmapper implements RecordUnmapper<UserEntity, UsersRecord> {
    private final DSLContext dsl;

    @Override
    public UsersRecord unmap(UserEntity source) throws MappingException {
        UsersRecord usersRecord = dsl.newRecord(USERS);
        usersRecord.setLogin(source.login());
        usersRecord.setName(source.name());
        usersRecord.setLastName(source.lastName());
        usersRecord.setPasswordHash(source.passwordHash());
        usersRecord.setCreatedAt(OffsetDateTime.now());

        return usersRecord;
    }
}
