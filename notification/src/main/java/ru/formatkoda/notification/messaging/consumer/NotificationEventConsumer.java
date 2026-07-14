package ru.formatkoda.notification.messaging.consumer;

import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import ru.formatkoda.notification.messaging.dto.NotificationEventDto;
import ru.formatkoda.notification.service.NotificationService;
import tools.jackson.databind.ObjectMapper;

@Component
@RequiredArgsConstructor
public class NotificationEventConsumer {
    private final ObjectMapper objectMapper;
    private final NotificationService notificationService;

    @KafkaListener(topics = "bank.notifications")
    public void consume(String message) {
        NotificationEventDto notificationEventDto = objectMapper.readValue(message, NotificationEventDto.class);

        notificationService.processNotification(notificationEventDto);
    }
}
