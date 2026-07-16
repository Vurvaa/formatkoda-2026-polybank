package ru.formatkoda.notification.service;

import ru.formatkoda.notification.exception.ResourceNotFoundException;
import tools.jackson.databind.JsonNode;

import java.util.List;

public enum NotificationTemplate {
    USER_REGISTERED(List.of("name", "createdAt"), "Поздравляем, %s! Вы успешно зарегистрировались! Дата регистрации: %s"),
    ACCOUNT_CREATED(List.of("name", "number"), "%s, счет создан успешно! Номер счета: %s"),
    TRANSACTION_WITHDRAW(List.of("name", "fromAccountNumber", "amount"), "%s, Вы сняли со счета %s %s рублей"),
    TRANSACTION_TOP_UP(List.of("name", "toAccountNumber", "amount"), "Отлично, %s! Вы пополнили счет %s на %s рублей"),
    TRANSACTION_BETWEEN_PERSON_ACCOUNTS(List.of("name", "amount", "fromAccountNumber", "toAccountNumber"), "%s, Вы перевели %s рублей между своими счетами %s и %s"),
    TRANSACTION_TOP_UP_BETWEEN_ACCOUNTS(List.of("name", "amount", "fromAccountNumber", "toAccountNumber"), "%s, вам перевели %s рублей со счета %s на ваш счет %s"),
    TRANSACTION_WITHDRAW_BETWEEN_ACCOUNTS(List.of("name", "amount", "fromAccountNumber", "toAccountNumber"), "%s, Вы перевели %s рублей со своего счета %s на счет %s");

    private final List<String> params;
    private final String templateText;

    NotificationTemplate(List<String> params, String templateText) {
        this.params = params;
        this.templateText = templateText;
    }

    public static String renderTemplate(String templateName, JsonNode root) {
        NotificationTemplate template = NotificationTemplate.fromName(templateName);
        Object[] parameters = template.params.stream().map(
                param -> root.get(param).asString()
        ).toList().toArray();

        return template.templateText.formatted(parameters);
    }

    public static NotificationTemplate fromName(String templateName) {
        try {
            return NotificationTemplate.valueOf(templateName);
        } catch (IllegalArgumentException _) {
            throw new ResourceNotFoundException("template not found");
        }
    }
}
