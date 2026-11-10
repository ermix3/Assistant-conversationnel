CREATE EXTENSION IF NOT EXISTS vector;
CREATE TABLE prices
(
    service_code varchar(80) PRIMARY KEY,
    label        varchar(200)   NOT NULL,
    amount       numeric(12, 2) NOT NULL CHECK (amount >= 0),
    unit         varchar(100)   NOT NULL,
    updated_at   timestamptz    NOT NULL DEFAULT current_timestamp
);
CREATE TABLE club_facts
(
    fact_key   varchar(80) PRIMARY KEY,
    category   varchar(20)  NOT NULL CHECK (category IN ('contact', 'hours')),
    fact_value varchar(500) NOT NULL
);
INSERT INTO club_facts
VALUES ('phone', 'contact', '05 22 52 78 31'),
       ('email', 'contact', 'contact@escapehealthclub.com');
CREATE TABLE vector_store
(
    id        uuid PRIMARY KEY,
    content   text,
    metadata  json,
    embedding vector(1024)
);
CREATE INDEX vector_store_embedding_idx ON vector_store USING hnsw (embedding vector_cosine_ops);
