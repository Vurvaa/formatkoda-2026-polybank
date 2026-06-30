package ru.formatkoda.polybank.repository;

import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;
import ru.formatkoda.polybank.jooq.generated.tables.Roles;
import ru.formatkoda.polybank.jooq.generated.tables.records.RolesRecord;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class AuthorityRepository {
    private final DSLContext dsl;

    public Optional<RolesRecord> findIdByAuthority(String authority) {
        return dsl.selectFrom(Roles.ROLES)
                .where(Roles.ROLES.NAME.eq(authority))
                .fetchOptional();
    }
}
