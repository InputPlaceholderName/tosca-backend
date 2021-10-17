CREATE TABLE waiting_lists (
    id INT GENERATED ALWAYS AS IDENTITY,
    workspace INT NOT NULL,
    title TEXT,
    information TEXT,
    PRIMARY KEY (id),
    FOREIGN KEY (workspace) REFERENCES workspaces(id) ON DELETE CASCADE
);

CREATE TABLE signups (
    id INT GENERATED ALWAYS AS IDENTITY,
    workspace INT NOT NULL,
    title TEXT,
    information TEXT,
    max_user_signups INT NOT NULL DEFAULT(1),
    PRIMARY KEY (id),
    FOREIGN KEY (workspace) REFERENCES workspaces(id) ON DELETE CASCADE
);
