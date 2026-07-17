package ru.formatkoda.notification.service;

import org.junit.jupiter.api.Test;
import ru.formatkoda.notification.exception.ResourceNotFoundException;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static ru.formatkoda.notification.testutil.NotificationTestData.TEMPLATE_NAME;
import static ru.formatkoda.notification.testutil.NotificationTestData.payloadEntity;

class NotificationTemplateTest {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void fromNameShouldReturnTemplateWhenItExists() {
        NotificationTemplate result = NotificationTemplate.fromName(TEMPLATE_NAME);

        assertThat(result).isEqualTo(NotificationTemplate.USER_REGISTERED);
    }

    @Test
    void fromNameShouldThrowExceptionWhenTemplateNotFound() {
        assertThatThrownBy(() -> NotificationTemplate.fromName("UNKNOWN"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("template not found");
    }

    @Test
    void renderTemplateShouldRenderKnownTemplate() throws JacksonException {
        String result = NotificationTemplate.renderTemplate(
                TEMPLATE_NAME,
                payloadEntity(objectMapper)
        );

        assertThat(result)
                .contains("Ivan")
                .contains("2026-06-29T12:00:00Z");
    }
}
