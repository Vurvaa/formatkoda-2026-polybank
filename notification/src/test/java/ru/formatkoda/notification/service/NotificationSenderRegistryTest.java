package ru.formatkoda.notification.service;

import org.junit.jupiter.api.Test;
import ru.formatkoda.notification.domain.DeliveryEntity;
import ru.formatkoda.notification.exception.ResourceNotFoundException;
import ru.formatkoda.notification.sender.NotificationSender;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class NotificationSenderRegistryTest {
    @Test
    void getNotificationSenderShouldReturnSenderForType() {
        NotificationSender emailSender = mock(NotificationSender.class);
        when(emailSender.getNotificationType()).thenReturn(DeliveryEntity.Type.EMAIL);
        NotificationSenderRegistry registry = new NotificationSenderRegistry(List.of(emailSender));

        NotificationSender result = registry.getNotificationSender(DeliveryEntity.Type.EMAIL);

        assertThat(result).isSameAs(emailSender);
    }

    @Test
    void getNotificationSenderShouldThrowExceptionWhenSenderNotFound() {
        NotificationSenderRegistry registry = new NotificationSenderRegistry(List.of());

        assertThatThrownBy(() -> registry.getNotificationSender(DeliveryEntity.Type.EMAIL))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("unknown notification type");
    }
}
