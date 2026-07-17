package ru.formatkoda.notification.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.formatkoda.notification.domain.EventEntity;
import ru.formatkoda.notification.exception.BusinessLogicException;
import ru.formatkoda.notification.exception.ResourceNotFoundException;
import ru.formatkoda.notification.repository.EventRepository;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EventService {
    private final EventRepository eventRepository;

    @Transactional
    public EventEntity createEventOrThrow(EventEntity eventEntity) {
        return eventRepository
                .save(eventEntity)
                .orElseThrow(() -> new BusinessLogicException("event not created"));
    }

    public EventEntity findEventByIdOrThrow(Long id) {
        return eventRepository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("event not found"));
    }
}
