CREATE EXTENSION if not exists postgis;
create table profile_image (
	id int generated always as identity primary key,
	image_url varchar(512) not null unique,
	name varchar(50) not null unique
);
create table users (
	user_id uuid primary key DEFAULT gen_random_uuid(),
	nickname varchar(50) not null,
	profile_image_id int,
	email varchar(255) check(email like '%@%'),
	provider varchar(20) not null check (provider in ('KAKAO', 'GOOGLE')),
	provider_id varchar(255) not null,
	refresh_token varchar(512),
	unique(provider, provider_id),
	foreign key (profile_image_id) references profile_image(id) on delete
	set null
);
CREATE TABLE user_car(
	car_id UUID NOT NULL DEFAULT gen_random_uuid(),
	user_id UUID not NULL,
	model VARCHAR(255) NOT NULL unique,
	battery_capacity FLOAT NOT NULL,
	PRIMARY KEY (car_id),
	FOREIGN KEY (user_id) REFERENCES users (user_id) ON DELETE cascade
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
	limitYn varchar(1) check (limitYn in ('Y', 'N')),
	limitDetail varchar(100),
	floorNum varchar(50),
	floorType varchar(2) check (floorType in ('F', 'B'))
);
CREATE TABLE charger(
	statId varchar(8) NOT NULL,
	chgerId varchar(2) NOT NULL,
	chgerType char(2) not null check (
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
			'10',
			'11'
		)
	),
	stat char(1) not null check (stat in ('0', '1', '2', '3', '4', '5', '6', '9')),
	statUpdDt char(14) not null,
	lastTsdt char(14),
	lastTedt char(14),
	output varchar(20) check (output in ('3', '7', '50', '100', '200')),
	method varchar(10) check(method in ('단독', '동시')),
	PRIMARY KEY (statId, chgerId),
	FOREIGN KEY (statId) REFERENCES station(statId) ON DELETE CASCADE
);
create table review (
	review_id BIGINT generated always as identity primary key,
	user_id UUID not null,
	content varchar(500) not null,
	rating int not null check(
		rating between 1 and 5
	),
	statId varchar(8) not null,
	--1~5
	created TIMESTAMP NOT NULL,
	foreign key (statId) references station(statId) on delete cascade,
	foreign key (user_id) references users(user_id) on delete cascade
);
create table review_image (
	image_id BIGINT generated always as identity primary key,
	review_id BIGINT not null,
	image_url varchar(512) not null,
	foreign key (review_id) references review(review_id) on delete cascade
);
CREATE TABLE favorite(
	id BIGINT generated always as identity primary key,
	user_id UUID NOT NULL,
	statId varchar(8) NOT NULL,
	created TIMESTAMP NOT NULL,
	unique (user_id, statId),
	FOREIGN KEY (user_id) REFERENCES users (user_id) ON DELETE CASCADE,
	FOREIGN KEY (statId) REFERENCES station(statId) ON DELETE CASCADE
);
create index idx_station_location on station using gist(location);