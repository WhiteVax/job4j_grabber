CREATE TABLE post(
    id SERIAL PRIMARY KEY,
    name TEXT,
    link VARCHAR(255) UNIQUE,
    description TEXT,
    created TIMESTAMP
);