CREATE TABLE workspaces (
    id INT GENERATED ALWAYS AS IDENTITY,
    creator INT,
    name TEXT,
    info TEXT,
    PRIMARY KEY (id),
    FOREIGN KEY (creator) REFERENCES users(id) ON DELETE SET NULL
)