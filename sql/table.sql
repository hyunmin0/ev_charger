CREATE EXTENSION postgis;
create table profile_image (
	id int generated always as identity primary key,
	image_url varchar(512) not null unique,
	name varchar(50) not null unique
);
create table users (
	userid uuid primary key DEFAULT gen_random_uuid(),
	nickname varchar(50) not null,
	profile_image_id int,
	email varchar(255) check(email like '%@%'),
	provider varchar(20) not null check (provider in ('kakao', 'google')),
	provider_id varchar(255) not null,
	refresh_token varchar(512),
	unique(provider, provider_id),
	foreign key (profile_image_id) references profile_image(id) on delete
	set null
);
CREATE TABLE user_car(
	carid UUID NOT NULL DEFAULT gen_random_uuid(),
	userid UUID not NULL,
	model VARCHAR(255) NOT NULL,
	battery_capacity FLOAT NOT NULL,
	PRIMARY KEY (carid),
	FOREIGN KEY (userid) REFERENCES users (userid) ON DELETE cascade
);
create table station (
	statNm varchar(100) not null,
	statId varchar(8) primary key,
	addr varchar(150) not null,
	addrDetail varchar(200),
	location geography(point, 4326) not null,
	useTime varchar(50) not null,
	busiNm varchar(50) not null,
	busiCall varchar(20) not null,
	zcode varchar(2) not null,
	zscode varchar(5),
	kind varchar(2),
	kindDetail varchar(4),
	parkingFree varchar(1) check(parkingFree in ('Y', 'N')),
	note varchar(200),
	limitYn varchar(1) not null check (limitYn in ('Y', 'N')),
	limitDetail varchar(100),
	floorNum varchar(50),
	floorType varchar(2) check (floorType in ('F', 'B'))
);
CREATE TABLE charger(
	statId varchar(8) NOT NULL,
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
	statUpdDt varchar(14) not null,
	lastTsdt varchar(14),
	lastTedt varchar(14),
	output varchar(20) check (output in ('3', '7', '50', '100', '200')),
	method varchar(10) check(method in ('단독', '동시')),
	PRIMARY KEY (statId, chgerId),
	FOREIGN KEY (statId) REFERENCES station(statId) ON DELETE CASCADE
);
CREATE TABLE favorite(
	id BIGINT generated always as identity primary key,
	userid UUID NOT NULL,
	statId varchar(8) NOT NULL,
	created TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
	unique (userid, statId),
	FOREIGN KEY (userid) REFERENCES users (userid) ON DELETE CASCADE,
	FOREIGN KEY (statId) REFERENCES station(statId) ON DELETE CASCADE
);
create index idx_station_location on station using gist(location);