CREATE TABLE IF NOT EXISTS saved_telegram_chat_ids_table(
    id SERIAL PRIMARY KEY,
    telegram_chat_id BIGINT NOT NULL
);