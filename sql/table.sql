create table users (
	userid uuid primary key DEFAULT gen_random_uuid(),
	nickname varchar(50) not null,
	profile varchar(512),
	email varchar(255) check(email like '%@%'),
	provider varchar(20) not null check (provider in ('kakao', 'google')),
	provider_id varchar(255) not null,
	refresh_token varchar(512),
	unique(provider, provider_id)
);
CREATE TABLE user_car(
	carid UUID NOT NULL DEFAULT gen_random_uuid(),
	userid UUID not NULL,
	model VARCHAR(255) NOT NULL,
	battery_capacity FLOAT NOT NULL,
	PRIMARY KEY (carid),
	FOREIGN KEY (userid) REFERENCES users (userid) ON DELETE cascade
);
CREATE TABLE favorite(
	id BIGINT generated always as identity primary key,
	userid UUID NOT NULL,
	statId int NOT NULL,
	created TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
	unique (userid, statId),
	FOREIGN KEY (userid) REFERENCES users (userid) ON DELETE CASCADE,
	FOREIGN KEY (statId) REFERENCES station(statId) ON DELETE CASCADE
);
create table station (
	statNm varchar(100) not null,
	statId int primary key,
	addr varchar(150) not null,
	addrDetail varchar(200) not null,
	location geography(point, 4326) not null,
	useTime varchar(50) not null,
	busiNm varchar(50) not null,
	busiCall varchar(20) not null,
	zcode int not null,
	zscode int,
	kind varchar(2),
	kindDetail varchar(4),
	parkingFree varchar(1) check(parkingFree in ('Y', 'N')),
	note varchar(200),
	limitYn varchar(1) not null check (limitYn in ('Y', 'N')),
	limitDetail varchar(100),
	floorNum int,
	floorType varchar(2) check (floorType in ('F', 'B'))
);
CREATE TABLE charger(
	statId int NOT NULL,
	chgerId varchar(2) NOT NULL,
	chgerType varchar(2) not null check (
		chgerType in (
			'01',
			'02',
			'03',
			'04',
			'05',
			'06',
			'07',
			'08',
			'09',
			'10'
		)
	),
	stat varchar(1) not null check (stat in ('0', '1', '2', '3', '4', '5')),
	statUpdDt not null varchar(14),
	lastTsdt varchar(14),
	lastTedt varchar(14),
	output INTEGER check (output in (3, 7, 50, 100, 200)),
	method varchar(10) check(method in ('단독', '동시')),
	PRIMARY KEY (statId, chgerId),
	FOREIGN KEY (statId) REFERENCES station(statId) ON DELETE CASCADE
);