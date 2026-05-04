create table ticket_attachments (
    id bigserial primary key,
    ticket_id bigint not null,
    uploaded_by bigint not null,
    original_file_name varchar(255) not null,
    stored_file_name varchar(255) not null unique,
    content_type varchar(255),
    size bigint not null,
    created_at timestamp not null default current_timestamp,
    constraint fk_ticket_attachments_ticket
        foreign key (ticket_id) references tickets(id),
    constraint fk_ticket_attachments_uploaded_by
        foreign key (uploaded_by) references users(id)
);

create index idx_ticket_attachments_ticket_id on ticket_attachments(ticket_id);