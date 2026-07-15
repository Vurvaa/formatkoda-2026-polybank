package ru.formatkoda.notification.service;

import ru.formatkoda.notification.exception.ResourceNotFoundException;
import tools.jackson.databind.JsonNode;

import java.util.List;

public enum NotificationTemplate {
    USER_REGISTERED(List.of("name", "createdAt"), "Поздравляем, %s! Вы успешно зарегистрировались! Дата регистрации: %s");

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
