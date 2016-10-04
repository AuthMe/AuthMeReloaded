-- Important: separate SQL statements by ; followed directly by a newline. We split the file contents by ";\n"

CREATE TABLE authme (
    id INTEGER AUTO_INCREMENT,
    username VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    ip VARCHAR(40) NOT NULL,
    lastlogin BIGINT,
    x DOUBLE NOT NULL DEFAULT '0.0',
    y DOUBLE NOT NULL DEFAULT '0.0',
    z DOUBLE NOT NULL DEFAULT '0.0',
    world VARCHAR(255) NOT NULL DEFAULT 'world',
    email VARCHAR(255) DEFAULT 'your@email.com', 
    isLogged INT DEFAULT '0', realname VARCHAR(255) NOT NULL DEFAULT 'Player', 
    salt varchar(255),
    recoverycode VARCHAR(20),
    recoveryexpiration BIGINT,
    CONSTRAINT table_const_prim PRIMARY KEY (id)
);

INSERT INTO authme (id, username, password, ip, lastlogin, x, y, z, world, email, isLogged, realname, salt)
VALUES (1,'bobby','$SHA$11aa0706173d7272$dbba966','123.45.67.89',1449136800,1.05,2.1,4.2,'world','your@email.com',0,'Bobby',NULL);
INSERT INTO authme (id, username, password, ip, lastlogin, x, y, z, world, email, isLogged, realname, salt)
VALUES (NULL,'user','b28c32f624a4eb161d6adc9acb5bfc5b','34.56.78.90',1453242857,124.1,76.3,-127.8,'nether','user@example.org',0,'user','f750ba32');
