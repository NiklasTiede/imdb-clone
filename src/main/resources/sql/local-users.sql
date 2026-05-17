-- Local development roles and demo accounts.
-- Movie data is owned by the versioned movie seed containers.

insert into role(id, name)
values
    (1, 'ROLE_ADMIN'),
    (2, 'ROLE_USER')
on conflict (id) do update
set name = excluded.name;

insert into account(username, email, password, first_name, last_name, bio, phone, birthday, locked, enabled)
values
    ('les_grossman', 'tom.cruise@gmail.com', '$2a$10$hjtGB7lxHBkTL51MEEsA/Oap2frjDpunlH/njP4xvuiV06RiCWKeW', 'Tom', 'Cruise', 'I will massacre you!', '491628264723', '1962-07-03', false, true),
    ('jeff_portnoy', 'jack.black@gmail.com', '$2a$10$hjtGB7lxHBkTL51MEEsA/Oap2frjDpunlH/njP4xvuiV06RiCWKeW', 'Jack', 'Black', 'You go, girl!', '4915122973088', '1969-08-28', false, true)
on conflict (username) do update
set email = excluded.email,
    password = excluded.password,
    first_name = excluded.first_name,
    last_name = excluded.last_name,
    bio = excluded.bio,
    phone = excluded.phone,
    birthday = excluded.birthday,
    locked = excluded.locked,
    enabled = excluded.enabled;

insert into account_roles(account_id, roles_id)
select account.id, role.id
from account
join role on role.name in ('ROLE_ADMIN', 'ROLE_USER')
where account.username = 'les_grossman'
union
select account.id, role.id
from account
join role on role.name = 'ROLE_USER'
where account.username = 'jeff_portnoy'
on conflict (account_id, roles_id) do nothing;

select setval(pg_get_serial_sequence('role', 'id'), (select max(id) from role));
