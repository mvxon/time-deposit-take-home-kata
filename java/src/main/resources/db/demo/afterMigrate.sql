-- Demo data for the `demo` Spring profile: the API cannot create deposits, so Swagger would otherwise show an empty list.
-- A Flyway afterMigrate callback is not recorded in the schema history and is idempotent, so the profile can be switched on and off on the same database.
insert into time_deposits (id, plan_type, days, balance)
values (1, 'basic', 45, 1234567.00),
       (2, 'basic', 20, 1000.00),
       (3, 'student', 200, 5000.00),
       (4, 'student', 400, 5000.00),
       (5, 'premium', 40, 20000.00),
       (6, 'premium', 90, 20000.00)
on conflict (id) do nothing;

insert into withdrawals (id, time_deposit_id, amount, date)
values (1, 1, 1000.00, '2026-01-15'),
       (2, 1, 250.50, '2026-02-10'),
       (3, 3, 100.00, '2026-03-01'),
       (4, 6, 5000.00, '2026-04-20')
on conflict (id) do nothing;

select setval(pg_get_serial_sequence('time_deposits', 'id'), (select max(id) from time_deposits));
select setval(pg_get_serial_sequence('withdrawals', 'id'), (select max(id) from withdrawals));
