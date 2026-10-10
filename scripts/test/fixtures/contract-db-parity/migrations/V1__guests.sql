create type guest_status as enum ('INVITED', 'CONFIRMED');
create table guests (id uuid primary key, name varchar(100) not null, status guest_status not null);
