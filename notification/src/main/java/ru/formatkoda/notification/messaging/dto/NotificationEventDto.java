package ru.formatkoda.notification.messaging.dto;

import tools.jackson.databind.JsonNode;

import java.util.List;

public record NotificationEventDto(
        Long userId,
        List<String> notificationTypeNames,
        String notificationTemplateName,
        JsonNode entity
) {}
