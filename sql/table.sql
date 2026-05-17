CREATE TABLE mycar(
    mycarid UUID NOT NULL DEFAULT gen_random_uuid(),
    userid UUID NULL ,
    model VARCHAR(255) NOT NULL,
    battery_capacity FLOAT NOT NULL,
    PRIMARY KEY (mycarid),
    FOREIGN KEY (userid) REFERENCES "user"(userid) ON DELETE SET NULL
);

CREATE TABLE favorite(
    userid UUID NOT NULL,
    statid VARCHAR(255) NOT NULL,
    created TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (userid, statid),
    FOREIGN KEY (userid) REFERENCES "user"(userid) ON DELETE CASCADE,
    FOREIGN KEY (statid) REFERENCES station(statid) ON DELETE CASCADE
);

CREATE TABLE charger(
    statid VARCHAR(255) NOT NULL,
    chgerid varchar(255) NOT NULL,
    chgertype varchar(255),
    stat SMALLINT,
    statUpdDt varchar(255) ,
    lastTsdt varchar(255) ,
    lastTedt varchar(255),
    output INTEGER,
    method varchar(255),
    PRIMARY KEY (statid, chgerid),
    FOREIGN KEY (statid) REFERENCES station(statid) ON DELETE CASCADE
);