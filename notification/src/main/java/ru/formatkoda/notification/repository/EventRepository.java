package ru.formatkoda.notification.repository;

import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Repository;
import ru.formatkoda.notification.domain.EventEntity;

import java.util.List;
import java.util.Optional;

import static ru.formatkoda.notification.jooq.generated.Tables.EVENTS;

@Repository
@RequiredArgsConstructor
public class EventRepository {
    private final DSLContext dsl;

    public Optional<EventEntity> save(EventEntity event) {
        return dsl
                .insertInto(EVENTS)
                .set(EVENTS.USER_ID, event.userId())
                .set(EVENTS.PAYLOAD, event.payload())
                .returning()
                .fetchOptional(this::toEntity);
    }

    public List<EventEntity> save(List<EventEntity> events) {
        return dsl
                .insertInto(EVENTS, EVENTS.USER_ID, EVENTS.PAYLOAD)
                .valuesOfRows(events.stream()
                        .map(e ->
                                DSL.row(e.userId(), e.payload())
                        )
                        .toList()
                )
                .returning()
                .fetch(this::toEntity);
    }

    public Optional<EventEntity> findById(long id) {
        return dsl
                .selectFrom(EVENTS)
                .where(EVENTS.ID.eq(id))
                .fetchOptional(this::toEntity);
    }

    public EventEntity toEntity(Record r) {
        return new EventEntity(
                r.get(EVENTS.ID),
                r.get(EVENTS.USER_ID),
                r.get(EVENTS.PAYLOAD),
                r.get(EVENTS.RECEIVED_AT)
        );
    }
}
