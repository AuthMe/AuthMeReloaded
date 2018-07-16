-- Important: separate SQL statements by ; followed directly by a newline. We split the file contents by ";\n"

CREATE TABLE authme (
    id INTEGER AUTO_INCREMENT,
    username VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    totp VARCHAR(16),
    ip VARCHAR(40),
    lastlogin BIGINT,
    regdate BIGINT NOT NULL,
    regip VARCHAR(40),
    x DOUBLE NOT NULL DEFAULT '0.0',
    y DOUBLE NOT NULL DEFAULT '0.0',
    z DOUBLE NOT NULL DEFAULT '0.0',
    world VARCHAR(255) NOT NULL DEFAULT 'world',
    yaw FLOAT,
    pitch FLOAT,
    email VARCHAR(255),
    isLogged INT DEFAULT '0',
    realname VARCHAR(255) NOT NULL DEFAULT 'Player',
    salt varchar(255),
    hasSession INT NOT NULL DEFAULT '0',
    CONSTRAINT table_const_prim PRIMARY KEY (id)
);

INSERT INTO authme (id, username, password, ip, lastlogin, x, y, z, world, yaw, pitch, email, isLogged, realname, salt, regdate, regip, totp)
VALUES (1,'bobby','$SHA$11aa0706173d7272$dbba966','123.45.67.89',1449136800,1.05,2.1,4.2,'world',-0.44,2.77,'your@email.com',0,'Bobby',NULL,1436778723,'127.0.4.22','JBSWY3DPEHPK3PXP');
INSERT INTO authme (id, username, password, ip, lastlogin, x, y, z, world, yaw, pitch, email, isLogged, realname, salt, regdate)
VALUES (NULL,'user','b28c32f624a4eb161d6adc9acb5bfc5b','34.56.78.90',1453242857,124.1,76.3,-127.8,'nether',0.23,4.88,'user@example.org',0,'user','f750ba32',0);
