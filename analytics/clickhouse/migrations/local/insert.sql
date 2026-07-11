INSERT INTO analytics.polybank_users_raw
(event_id, event_time, event_type, payload)
VALUES
    (
        generateUUIDv4(),
        now64(3),
        'aaa',
        '{"userId": 123, "userLogin": "bbb", "name": "bob", "lastName": "ddd", "role": "user", "createdAt": "2026-07-10 13:24:00"}'
    );

SELECT event_id, user_id, user_login, created_at FROM analytics.polybank_users_raw;

SELECT
    database,
    table,
    num_messages_read,
    last_poll_time,
    exceptions.text
FROM system.kafka_consumers
WHERE database = 'analytics';

select count(*) from polybank_accounts_raw