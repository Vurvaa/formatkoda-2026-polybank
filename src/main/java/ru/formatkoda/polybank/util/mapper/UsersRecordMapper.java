package ru.formatkoda.polybank.util.mapper;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.formatkoda.polybank.repository.UserRepository;

@RequiredArgsConstructor
@Component
public class UsersRecordMapper {
    private final UserRepository userRepository;

}
