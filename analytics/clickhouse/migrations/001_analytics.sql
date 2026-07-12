CREATE DATABASE IF NOT EXISTS analytics;

CREATE TABLE IF NOT EXISTS analytics.polybank_users_raw
(
    event_id UUID,
    event_time DateTime64(3, 'UTC'),
    event_type LowCardinality(String),

    payload String EPHEMERAL,

    user_id Int64 MATERIALIZED JSONExtractInt(payload, 'userId'),
    user_login String MATERIALIZED JSONExtractString(payload, 'userLogin'),
    name String MATERIALIZED JSONExtractString(payload, 'name'),
    last_name String MATERIALIZED JSONExtractString(payload, 'lastName'),
    role LowCardinality(String) MATERIALIZED JSONExtractString(payload, 'role'),
    created_at DateTime64(3, 'UTC') MATERIALIZED parseDateTime64BestEffortOrZero(JSONExtractString(payload, 'createdAt'), 3, 'UTC'),

    ingested_at DateTime64(3, 'UTC') DEFAULT now64(3)
)
ENGINE = ReplacingMergeTree
PARTITION BY toYYYYMM(event_time)
ORDER BY (event_type, event_time, event_id);

CREATE TABLE IF NOT EXISTS analytics.polybank_accounts_raw
(
    event_id UUID,
    event_time DateTime64(3, 'UTC'),
    event_type LowCardinality(String),

    payload String EPHEMERAL,

    account_id Int64 MATERIALIZED JSONExtractInt(payload, 'accountId'),
    account_number String MATERIALIZED JSONExtractString(payload, 'accountNumber'),
    user_id Int64 MATERIALIZED JSONExtractInt(payload, 'userId'),
    account_type LowCardinality(String) MATERIALIZED JSONExtractString(payload, 'accountType'),
    created_at DateTime64(3, 'UTC') MATERIALIZED parseDateTime64BestEffortOrZero(JSONExtractString(payload, 'createdAt'), 3, 'UTC'),

    ingested_at DateTime64(3, 'UTC') DEFAULT now64(3)
)
ENGINE = ReplacingMergeTree
PARTITION BY toYYYYMM(event_time)
ORDER BY (event_type, event_time, event_id);

CREATE TABLE IF NOT EXISTS analytics.polybank_transactions_raw
(
    event_id UUID,
    event_time DateTime64(3, 'UTC'),
    event_type LowCardinality(String),

    payload String EPHEMERAL,

    transaction_id Int64 MATERIALIZED JSONExtractInt(payload, 'transactionId'),
    from_account_number String MATERIALIZED JSONExtractString(payload, 'fromAccountNumber'),
    to_account_number String MATERIALIZED JSONExtractString(payload, 'toAccountNumber'),
    amount Decimal(19, 2) MATERIALIZED JSONExtract(payload, 'amount', 'Decimal(19, 2)'),
    transaction_type LowCardinality(String) MATERIALIZED JSONExtractString(payload, 'transactionType'),
    transaction_status LowCardinality(String) MATERIALIZED JSONExtractString(payload, 'transactionStatus'),
    created_at DateTime64(3, 'UTC') MATERIALIZED parseDateTime64BestEffortOrZero(JSONExtractString(payload, 'createdAt'), 3, 'UTC'),

    ingested_at DateTime64(3, 'UTC') DEFAULT now64(3)
)
ENGINE = ReplacingMergeTree
PARTITION BY toYYYYMM(event_time)
ORDER BY (event_type, event_time, event_id);

CREATE TABLE IF NOT EXISTS analytics.polybank_events_queue
(
    eventId UUID,
    eventTime String,
    eventType LowCardinality(String),
    payload String
)
ENGINE = Kafka
SETTINGS
    kafka_broker_list = '192.168.130.82:9092',
    kafka_topic_list = 'bank.users,bank.accounts,bank.transactions',
    kafka_group_name = 'clickhouse-analytics',
    kafka_format = 'JSONEachRow',
    kafka_num_consumers = 1,
    kafka_handle_error_mode = 'stream';

CREATE MATERIALIZED VIEW IF NOT EXISTS analytics.polybank_events_queue_users_to_raw
TO analytics.polybank_users_raw
AS
SELECT
    eventId AS event_id,
    parseDateTime64BestEffort(eventTime, 3, 'UTC') AS event_time,
    eventType AS event_type,
    payload
FROM analytics.polybank_events_queue
WHERE eventType = 'UserRegistered';

CREATE MATERIALIZED VIEW IF NOT EXISTS analytics.polybank_events_queue_accounts_to_raw
TO analytics.polybank_accounts_raw
AS
SELECT
    eventId AS event_id,
    parseDateTime64BestEffort(eventTime, 3, 'UTC') AS event_time,
    eventType AS event_type,
    payload
FROM analytics.polybank_events_queue
WHERE eventType = 'AccountCreated';

CREATE MATERIALIZED VIEW IF NOT EXISTS analytics.polybank_events_queue_transactions_to_raw
TO analytics.polybank_transactions_raw
AS
SELECT
    eventId AS event_id,
    parseDateTime64BestEffort(eventTime, 3, 'UTC') AS event_time,
    eventType AS event_type,
    payload
FROM analytics.polybank_events_queue
WHERE eventType = 'TransactionCreated';
