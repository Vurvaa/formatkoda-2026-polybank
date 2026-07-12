CREATE USER debezium WITH PASSWORD 'debezium' REPLICATION;
GRANT CONNECT ON DATABASE polybank_db TO debezium;
GRANT USAGE ON SCHEMA public TO debezium;
GRANT SELECT ON TABLE public.outbox_events TO debezium;
