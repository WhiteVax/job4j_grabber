CREATE TABLE post(
    id SERIAL PRIMARY KEY,
    name TEXT,
    link TEXT UNIQUE,
    description TEXT,
    created TIMESTAMP
);