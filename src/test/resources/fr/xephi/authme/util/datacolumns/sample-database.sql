create table testingdata (
  id int not null,
  username varchar(80),
  ip varchar(15),
  email varchar(100),
  is_locked tinyint not null,
  is_active tinyint not null,
  last_login bigint,
  color varchar(16),
  primary key(id)
);

insert into testingdata (id, username, ip, email, is_locked, is_active, last_login)
VALUES
 ( 1, 'Alex',  '111.111.111.111', NULL,               1, 0, 123456),
 ( 2, 'Brett', '111.111.111.111', 'test@example.com', 0, 1, 123456),
 ( 3, 'Cody',  '22.22.22.22',     'test@example.com', 0, 0, 888888),
 ( 4, 'Dan',   '22.22.22.22',     NULL,               1, 1, NULL),
 ( 5, 'Emily', NULL,              NULL,               0, 1, 888888),
 ( 6, 'Finn',  '111.111.111.111', 'finn@example.org', 0, 0, NULL),
 ( 7, 'Gary',  '44.144.41.144',   'test@example.com', 0, 1, 123456),
 ( 8, 'Hans',  NULL,              'other@test.tld',   1, 0, 77665544),
 ( 9, 'Igor',  '22.22.22.22',     'other@test.tld',   0, 1, 725124),
 (10, 'Jake',  '44.144.41.144',   NULL,               0, 0, 123456),
 (11, 'Keane', '22.22.22.22',     'test@example.com', 0, 1, 888888),
 (12, 'Louis', NULL,              'other@test.tld',   0, 1, 732452);
