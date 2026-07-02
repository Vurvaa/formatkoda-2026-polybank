package ru.formatkoda.polybank.domain.account;

import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.springframework.stereotype.Component;

import java.math.BigInteger;

import static ru.formatkoda.polybank.domain.account.AccountEntity.ACCOUNT_NUMBER_PREFIX;

@Component
@RequiredArgsConstructor
public class AccountNumberGenerator {
    private static final String SEQUENCE_NAME = "account_number_seq";

    private final DSLContext dsl;

    public String generate() {
        BigInteger value = dsl.nextval(SEQUENCE_NAME);

        return ACCOUNT_NUMBER_PREFIX + String.format("%016d", value);
    }
}