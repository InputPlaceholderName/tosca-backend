CREATE TABLE users_workspaces (
    id INT GENERATED ALWAYS AS IDENTITY,
    user_id INT NOT NULL,
    workspace INT NOT NULL,
    role TEXT CHECK(role IN('Admin', 'Normal')),
    PRIMARY KEY(id),
    UNIQUE(user_id, workspace),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (workspace) REFERENCES workspaces(id) ON DELETE CASCADE
);