CREATE TABLE mycar(
    mycarid UUID NOT NULL DEFAULT gen_random_uuid(),
    userid UUID NULL ,
    model VARCHAR(255) NOT NULL,
    battery_capacity FLOAT NOT NULL,
    PRIMARY KEY (mycarid),
    FOREIGN KEY (userid) REFERENCES users (userid) ON DELETE SET NULL
);

CREATE TABLE favorite(
    userid UUID NOT NULL,
    statid  int NOT NULL,
    created TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (userid, statid),
    FOREIGN KEY (userid) REFERENCES users (userid) ON DELETE CASCADE,
    FOREIGN KEY (statid) REFERENCES station(statid) ON DELETE CASCADE
);

CREATE TABLE charger(
    statid  int NOT NULL,
    chgerid varchar(2) NOT NULL,
    chgertype varchar(2) check (chgertype in ('01', '02', '03', '04', '05', '06', '07', '08', '09', '10')) ,
    stat varchar(1) check (stat in ('0', '1', '2', '3', '4', '5')) ,
    statUpdDt varchar(14) ,
    lastTsdt varchar(14) ,
    lastTedt varchar(14),
    output INTEGER,
    method varchar(255),
    PRIMARY KEY (statid, chgerid),
    FOREIGN KEY (statid) REFERENCES station(statid) ON DELETE CASCADE
);