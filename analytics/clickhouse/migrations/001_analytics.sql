CREATE DATABASE IF NOT EXISTS analytics;

CREATE TABLE IF NOT EXISTS analytics.bank_events_raw
(
    event_id String,
    event_type LowCardinality(String),
    event_version UInt16,
    occurred_at DateTime64(3, 'UTC'),
    aggregate_id Nullable(String),
    payload String,
    ingested_at DateTime64(3, 'UTC') DEFAULT now64(3)
)
ENGINE = MergeTree
PARTITION BY toYYYYMM(occurred_at)
ORDER BY (event_type, occurred_at, event_id);

CREATE TABLE IF NOT EXISTS analytics.bank_events_queue
(
    event_id String,
    event_type String,
    event_version UInt16,
    occurred_at DateTime64(3, 'UTC'),
    aggregate_id Nullable(String),
    payload String
)
ENGINE = Kafka
SETTINGS
    kafka_broker_list = 'kafka:29092',
    kafka_topic_list = 'bank.events',
    kafka_group_name = 'clickhouse-analytics',
    kafka_format = 'JSONEachRow',
    kafka_num_consumers = 1,
    kafka_handle_error_mode = 'stream';

CREATE MATERIALIZED VIEW IF NOT EXISTS analytics.bank_events_queue_to_raw
TO analytics.bank_events_raw
AS
SELECT
    event_id,
    event_type,
    event_version,
    occurred_at,
    aggregate_id,
    payload
FROM analytics.bank_events_queue;
