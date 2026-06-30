package ru.formatkoda.polybank.repository;

import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;
import ru.formatkoda.polybank.domain.Authority;
import ru.formatkoda.polybank.jooq.generated.tables.Roles;

@Repository
@RequiredArgsConstructor
public class AuthorityRepository {
    private final DSLContext dsl;

    public Authority findByAuthority(String authority) {
        return dsl.selectFrom(Roles.ROLES)
                .where(Roles.ROLES.NAME.eq(authority))
                .fetchAny()
                .into(Authority.class);
    }
}
