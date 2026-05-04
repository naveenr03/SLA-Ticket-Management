insert into users (name, email, password, role, created_at, updated_at)
values
('Agent One', 'agent1@slaticket.com', '$2a$12$armuWuABScX7CsgXUL.lA.f1D37hTprGUzbig.50hsYW5EWMiDeba', 'ROLE_AGENT', current_timestamp, current_timestamp),
('Manager One', 'manager1@slaticket.com', '$2a$12$fnnbKY730H//H.40kOg/Net8zBc4M1Euh4agGqz92DHf5FW6MAl2a', 'ROLE_MANAGER', current_timestamp, current_timestamp),
('Admin One', 'admin1@slaticket.com', '$2a$12$GnpPujHGg2EvqAeeJ8fDFOodMU5MkC9w09sCQAFGPVwAaNyL5mulG', 'ROLE_ADMIN', current_timestamp, current_timestamp);