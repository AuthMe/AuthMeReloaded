CREATE TABLE ls_players (
    id integer not null,
    unique_user_id varchar(128),
    last_name varchar(16),
    ip_address varchar(64),
    password varchar(512),
    hashing_algorithm integer,
    location_id integer,
    inventory_id integer,
    last_login datetime,
    registration_date date,
    optlock integer not null,
    uuid_mode varchar(1) check ( uuid_mode in ('M','U','O')),
    constraint uq_ls_players_unique_user_id unique (unique_user_id),
    constraint uq_ls_players_location_id unique (location_id),
    constraint uq_ls_players_inventory_id unique (inventory_id),
    constraint pk_ls_players primary key (id)
);
CREATE TABLE ls_locations (
    id integer not null,
    world varchar(255),
    x double,
    y double,
    z double,
    yaw integer,
    pitch integer,
    constraint pk_ls_locations primary key (id)
);

INSERT INTO ls_players VALUES(1,'0cb1fa9b-846a-3cda-a1d9-5a9d6939ce14','Player1',NULL,'$2a$10$E1Ri7XKeIIBv4qVaiPplgepT7QH9xGFh3hbHfcmCjq7hiW.UBTiGK',7,NULL,NULL,'2017-05-08T11:14:53+00:00','2017-05-08',1,'O');
INSERT INTO ls_players VALUES(2,'6765ad15-4e42-364e-a116-3ca7a7433d08','Player2','127.4.5.6','$2a$10$p27YkDQWrDHS/7k/l/86xeHW5NBnFEL61.2o2Y.BM5IEn/yrZC5VW',7,NULL,NULL,'2017-03-12T11:22:33+00:00','2017-01-04',1,'O');
INSERT INTO ls_players VALUES(3,'1a1975b9-0bbd-3ced-afb8-1059d8c22c9e','Player3','127.8.9.0','$2a$10$WFui8KSXMLDOVXKFpCLyPukPi4M82w1cv/rNojsAnwJjba3pp8sba',7,NULL,NULL,'2017-04-20T15:02:19+00:00','2017-03-17',1,'O');

INSERT INTO ls_locations VALUES(3, 'hubb', 14.24, 67.99, -12.83, -10, 185);
