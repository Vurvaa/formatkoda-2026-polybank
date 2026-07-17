package ru.formatkoda.notification.messaging.dto;

import tools.jackson.databind.JsonNode;

import java.util.List;

public record NotificationEventDto(
        List<String> notificationTypeNames,
        String notificationTemplateName,
        JsonNode entity
) {}
