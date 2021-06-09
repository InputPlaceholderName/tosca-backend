CREATE TABLE users (
    id INT GENERATED ALWAYS AS IDENTITY,
    user_id TEXT NOT NULL UNIQUE,
    first_name TEXT,
    last_name TEXT,
    PRIMARY KEY (id)
)