CREATE TABLE t_ratelimite_conf
(
    channelType  VARCHAR(255),
    interfaceNo  VARCHAR(255) PRIMARY KEY,
    interfaceUrl VARCHAR(255),
    rateLimit    INT,
    cache        DOUBLE,
    status       BOOLEAN
);