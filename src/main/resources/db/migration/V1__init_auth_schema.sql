create table users (
    id bigserial primary key,
    name varchar(100) not null,
    email varchar(150) not null unique,
    password varchar(255) not null,
    role varchar(30) not null,
    created_at timestamp not null default current_timestamp,
    updated_at timestamp not null default current_timestamp
);