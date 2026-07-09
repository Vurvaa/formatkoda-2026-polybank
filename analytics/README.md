# Bank analytics

- Kafka: `localhost:9092`
- ClickHouse HTTP: `localhost:8123`
- ClickHouse Native: `localhost:9000`
- Grafana: `http://localhost:3000`
- Grafana login/password: `admin` / `admin`
- Kafka topic: `bank.events`

Общий флоу: 
пишем в кафку, кликхаус получает событие в bank_events_queue, 
через materialized view bank_events_queue_to_raw кладет его в таблицу bank_events_raw, к которой можно слать запросы

## Connect to Kafka

```text
bootstrap.servers=localhost:9092
```

## Формат тестового события (временный)

```json
{
  "event_id": "event-1",
  "event_type": "TRANSACTION_CREATED",
  "event_version": 1,
  "occurred_at": "2026-07-09 10:00:00.000",
  "aggregate_id": "transaction-1",
  "payload": "{\"amount\":100.00,\"currency\":\"EUR\"}"
}
```

## ClickHouse

```bash
docker exec bank-clickhouse clickhouse-client \
  --user analytics \
  --password analytics \
  --query "SELECT * FROM analytics.bank_events_raw ORDER BY occurred_at DESC LIMIT 10"
```

## Grafana provisioning

При старте Grafana читает `grafana/provisioning`:

- `datasources/clickhouse.yml` - подключение к ClickHouse;
- `dashboards/dashboards.yml` указывает, откуда загружать dashboard JSON;
- `grafana/dashboards/bank-events-overview.json` - дашборд и SQL панелей.
