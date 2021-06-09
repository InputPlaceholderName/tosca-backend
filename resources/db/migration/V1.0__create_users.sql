CREATE TABLE users (
    id INT GENERATED ALWAYS AS IDENTITY,
    name TEXT,
    age INT,
    PRIMARY KEY (id)
)