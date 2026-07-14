package ru.formatkoda.notification.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.formatkoda.notification.domain.EventEntity;
import ru.formatkoda.notification.repository.EventRepository;

@Service
@RequiredArgsConstructor
public class EventService {
    private final EventRepository eventRepository;

    public EventEntity createEventOrThrow(EventEntity eventEntity) {
        return eventRepository
                .save(eventEntity)
                .orElseThrow(() -> new RuntimeException("event not created"));
    }

    public EventEntity findEventByIdOrThrow(long id) {
        return eventRepository
                .findById(id)
                .orElseThrow(() -> new RuntimeException("event not found"));
    }
}
