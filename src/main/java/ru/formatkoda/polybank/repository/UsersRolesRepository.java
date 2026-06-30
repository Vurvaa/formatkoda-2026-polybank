package ru.formatkoda.polybank.repository;

import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;
import ru.formatkoda.polybank.jooq.generated.tables.UsersRoles;

@Repository
@RequiredArgsConstructor
public class UsersRolesRepository {
    private final DSLContext dsl;

    public void bindUserWithRole(long userId, long roleId) {
        dsl.insertInto(UsersRoles.USERS_ROLES,
                        UsersRoles.USERS_ROLES.USER_ID,
                        UsersRoles.USERS_ROLES.ROLE_ID)
                .values(userId, roleId)
                .execute();
    }
}
