package ru.formatkoda.polybank.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.formatkoda.polybank.domain.UserEntity;
import ru.formatkoda.polybank.dto.UserRegistrationDto;
import ru.formatkoda.polybank.domain.exceptions.UserAlreadyExistsException;
import ru.formatkoda.polybank.jooq.generated.tables.records.RolesRecord;
import ru.formatkoda.polybank.repository.AuthorityRepository;
import ru.formatkoda.polybank.repository.UserRepository;
import ru.formatkoda.polybank.repository.UsersRolesRepository;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final AuthorityRepository authorityRepository;
    private final UsersRolesRepository usersRolesRepository;

    public String createUser(UserRegistrationDto user) {
        Optional<UserEntity> userOptional = userRepository.findUserByLogin(user.login());
        if (!userOptional.isEmpty()) {
            throw new UserAlreadyExistsException("this login already exist");
        }

        Optional<RolesRecord> rolesRecordOptional = authorityRepository.findIdByAuthority("ROLE_USER");
        if (rolesRecordOptional.isEmpty()) {
            throw new RuntimeException("authority not found");
        }

        long userId = userRepository.insertUserAndReturnId(user);
        long roleId = rolesRecordOptional.get().getId();

        usersRolesRepository.bindUserWithRole(userId, roleId);

        return user.login();
    }
}
