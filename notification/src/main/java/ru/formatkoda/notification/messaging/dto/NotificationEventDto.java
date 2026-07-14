package ru.formatkoda.notification.messaging.dto;

import tools.jackson.databind.JsonNode;

public record NotificationEventDto(
        Long userId,
        String notificationTypeName,
        String notificationTemplateName,
        JsonNode entity
) {}
