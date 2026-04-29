create table tickets (
    id bigserial primary key,
    title varchar(150) not null,
    description text not null,
    status varchar(30) not null,
    priority varchar(30) not null,
    created_by bigint not null,
    assigned_to bigint null,
    created_at timestamp not null default current_timestamp,
    updated_at timestamp not null default current_timestamp,
    constraint fk_tickets_created_by
        foreign key (created_by) references users(id),
    constraint fk_tickets_assigned_to
        foreign key (assigned_to) references users(id)
);

create index idx_tickets_created_by on tickets(created_by);
create index idx_tickets_assigned_to on tickets(assigned_to);
create index idx_tickets_status on tickets(status);
create index idx_tickets_priority on tickets(priority);