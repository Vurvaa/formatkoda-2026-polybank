package ru.formatkoda.notification.testutil;

import org.jooq.JSONB;
import ru.formatkoda.notification.domain.DeliveryEntity;
import ru.formatkoda.notification.domain.EventEntity;
import ru.formatkoda.notification.messaging.dto.NotificationEventDto;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.OffsetDateTime;
import java.util.List;

public final class NotificationTestData {
    public static final Long USER_ID = 42L;
    public static final Long EVENT_ID = 100L;
    public static final Long DELIVERY_ID = 200L;
    public static final String EMAIL = "client@polybank.ru";
    public static final String TEMPLATE_NAME = "USER_REGISTERED";
    public static final List<String> EMAIL_TYPES = List.of("EMAIL");
    public static final OffsetDateTime RECEIVED_AT = OffsetDateTime.parse("2026-06-29T12:00:00Z");
    public static final OffsetDateTime RETRY_AT = OffsetDateTime.parse("2026-06-29T12:00:10Z");

    private NotificationTestData() {
    }

    public static EventEntity newEvent() {
        return new EventEntity(
                null,
                JSONB.jsonb(notificationPayloadJson()),
                null
        );
    }

    public static EventEntity event() {
        return new EventEntity(
                EVENT_ID,
                JSONB.jsonb(notificationPayloadJson()),
                RECEIVED_AT
        );
    }

    public static EventEntity eventWithoutEmail() {
        return new EventEntity(
                EVENT_ID,
                JSONB.jsonb(notificationPayloadWithoutEmailJson()),
                RECEIVED_AT
        );
    }

    public static DeliveryEntity pendingDelivery() {
        return deliveryWithAttempts((short) 5);
    }

    public static DeliveryEntity deliveryWithAttempts(short remainingAttempts) {
        return new DeliveryEntity(
                DELIVERY_ID,
                EVENT_ID,
                DeliveryEntity.Status.PENDING,
                RETRY_AT,
                remainingAttempts,
                DeliveryEntity.Type.EMAIL
        );
    }

    public static DeliveryEntity sentDelivery() {
        return new DeliveryEntity(
                DELIVERY_ID,
                EVENT_ID,
                DeliveryEntity.Status.SENT,
                RETRY_AT,
                (short) 4,
                DeliveryEntity.Type.EMAIL
        );
    }

    public static NotificationEventDto notificationEventDto(ObjectMapper objectMapper) throws JacksonException {
        return new NotificationEventDto(
                EMAIL_TYPES,
                TEMPLATE_NAME,
                payloadEntity(objectMapper)
        );
    }

    public static JsonNode payloadEntity(ObjectMapper objectMapper) throws JacksonException {
        return objectMapper.readTree(notificationPayloadJson()).get("entity");
    }

    public static String notificationPayloadJson() {
        return """
                {
                  "userId": 42,
                  "notificationTypeNames": ["EMAIL"],
                  "notificationTemplateName": "USER_REGISTERED",
                  "entity": {
                    "email": "client@polybank.ru",
                    "name": "Ivan",
                    "createdAt": "2026-06-29T12:00:00Z"
                  }
                }
                """;
    }

    private static String notificationPayloadWithoutEmailJson() {
        return """
                {
                  "userId": 42,
                  "notificationTypeNames": ["EMAIL"],
                  "notificationTemplateName": "USER_REGISTERED",
                  "entity": {
                    "name": "Ivan",
                    "createdAt": "2026-06-29T12:00:00Z"
                  }
                }
                """;
    }
}
