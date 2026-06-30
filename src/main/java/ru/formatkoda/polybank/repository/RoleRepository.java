package ru.formatkoda.polybank.repository;

import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;
import ru.formatkoda.polybank.domain.RoleEntity;

import java.util.Optional;

import static ru.formatkoda.polybank.jooq.generated.Tables.ROLES;

@Repository
@RequiredArgsConstructor
public class RoleRepository {
    private final DSLContext dsl;

    public Optional<RoleEntity> findRoleEntityByName(String name) {
        return dsl.selectFrom(ROLES)
                .where(ROLES.NAME.eq(name))
                .fetchOptionalInto(RoleEntity.class);
    }
}
