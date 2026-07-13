# Сброс офсета

Остановить коннектор

```bash
curl --fail-with-body -X PUT http://localhost:8083/connectors/application-outbox-connector/stop
```

```bash
curl -s http://localhost:8083/connectors/application-outbox-connector/status | jq
```

Проверить, что слот должен быть неактивен:

```postgresql
SELECT slot_name, active
FROM pg_replication_slots
WHERE slot_name = 'polybank_outbox_slot';
```

```postgresql
SELECT PG_DROP_REPLICATION_SLOT('polybank_outbox_slot');
```

Сбросить Kafka Connect offset

```bash
curl --fail-with-body -X DELETE http://localhost:8083/connectors/application-outbox-connector/offsets
```

```bash
curl --fail-with-body -X PUT http://localhost:8083/connectors/application-outbox-connector/resume
```
