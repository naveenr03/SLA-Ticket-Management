create table ticket_comments (
    id bigserial primary key,
    ticket_id bigint not null,
    author_id bigint not null,
    message text not null,
    created_at timestamp not null default current_timestamp,
    constraint fk_ticket_comments_ticket
        foreign key (ticket_id) references tickets(id),
    constraint fk_ticket_comments_author
        foreign key (author_id) references users(id)
);

create index idx_ticket_comments_ticket_id on ticket_comments(ticket_id);

create table ticket_history (
    id bigserial primary key,
    ticket_id bigint not null,
    event_type varchar(50) not null,
    details text null,
    performed_by bigint not null,
    created_at timestamp not null default current_timestamp,
    constraint fk_ticket_history_performed_by
        foreign key (performed_by) references users(id)
);

create index idx_ticket_history_ticket_id on ticket_history(ticket_id);