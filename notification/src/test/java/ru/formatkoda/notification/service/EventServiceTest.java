package ru.formatkoda.notification.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.formatkoda.notification.domain.EventEntity;
import ru.formatkoda.notification.exception.BusinessLogicException;
import ru.formatkoda.notification.exception.ResourceNotFoundException;
import ru.formatkoda.notification.repository.EventRepository;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.formatkoda.notification.testutil.NotificationTestData.EVENT_ID;
import static ru.formatkoda.notification.testutil.NotificationTestData.event;
import static ru.formatkoda.notification.testutil.NotificationTestData.newEvent;

@ExtendWith(MockitoExtension.class)
class EventServiceTest {
    @Mock
    private EventRepository eventRepository;

    @InjectMocks
    private EventService eventService;

    @Test
    void createEventOrThrowShouldReturnCreatedEvent() {
        EventEntity eventToSave = newEvent();
        EventEntity createdEvent = event();

        when(eventRepository.save(eventToSave)).thenReturn(Optional.of(createdEvent));

        EventEntity result = eventService.createEventOrThrow(eventToSave);

        assertThat(result).isSameAs(createdEvent);
        verify(eventRepository).save(eventToSave);
    }

    @Test
    void createEventOrThrowShouldThrowExceptionWhenEventWasNotCreated() {
        EventEntity eventToSave = newEvent();

        when(eventRepository.save(eventToSave)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> eventService.createEventOrThrow(eventToSave))
                .isInstanceOf(BusinessLogicException.class)
                .hasMessage("event not created");

        verify(eventRepository).save(eventToSave);
    }

    @Test
    void findEventByIdOrThrowShouldReturnEventWhenItExists() {
        EventEntity expectedEvent = event();

        when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(expectedEvent));

        EventEntity result = eventService.findEventByIdOrThrow(EVENT_ID);

        assertThat(result).isSameAs(expectedEvent);
        verify(eventRepository).findById(EVENT_ID);
    }

    @Test
    void findEventByIdOrThrowShouldThrowExceptionWhenEventNotFound() {
        when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> eventService.findEventByIdOrThrow(EVENT_ID))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("event not found");

        verify(eventRepository).findById(EVENT_ID);
    }
}
