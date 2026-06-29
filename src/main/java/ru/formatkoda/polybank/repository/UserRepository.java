package ru.formatkoda.polybank.repository;

import org.springframework.stereotype.Repository;
import ru.formatkoda.polybank.domain.User;

@Repository
public class UserRepository {
    //TODO wait pulling jooq config

    public User findUserByLogin(String login) {

    }
}
