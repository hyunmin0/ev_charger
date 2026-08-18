CREATE EXTENSION if not exists postgis;
create table profile_image (
	id int generated always as identity primary key,
	image_url varchar(512) not null unique,
	name varchar(50) not null unique
);
create table users (
	user_id uuid primary key DEFAULT gen_random_uuid(),
	role varchar(20) not null default 'USER' check (role in ('ADMIN', 'USER')),
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
create table car (
	car_id BIGINT primary key generated always as identity,
	brand varchar(50) not null,
	model varchar(50) not null,
	battery_type varchar(20) not null,
	model_year int not null,
	drive_type varchar(3) not null check (drive_type in ('2WD', '4WD')),
	wheel_size int not null,
	battery_capacity Float not null,
	trim varchar(50),
	combined Float,
	city Float,
	highway Float,
	unique (
		brand,
		model,
		battery_type,
		model_year,
		drive_type,
		wheel_size,
		trim
	)
);
create table car_charger (
	car_charger_id BIGINT primary key generated always as identity,
	brand varchar(50) not null,
	model varchar(50) not null,
	model_year int not null,
	charger_type char(2) not null check (
		charger_type in ('01', '02', '04', '07', '08', '09', '11')
	),
	unique (brand, model, model_year, charger_type)
);
create table charge (
	charge_id BIGINT primary key generated always as identity,
	brand varchar(50) not null,
	model varchar(50) not null,
	battery_type varchar(20) not null,
	model_year int not null,
	charger_type varchar(50) not null,
	charger_output int,
	minutes int not null
);
create unique index charge_unique_key on charge (
	brand,
	model,
	battery_type,
	model_year,
	charger_type,
	coalesce(charger_output, -1) -- null을 -1인셈 치고 중복 체크
);
CREATE TABLE user_car(
	user_car_id BIGINT generated always as identity primary key,
	user_id UUID not NULL,
	car_id BIGINT not null,
	battery_capacity Float not null,
	unique(car_id, user_id),
	FOREIGN KEY (user_id) REFERENCES users (user_id) ON DELETE cascade,
	FOREIGN KEY (car_id) REFERENCES car (car_id) ON DELETE cascade
);
create table station_operator (
	"busiId" varchar(2) primary key,
	"busiNm" varchar(50) not null,
	"busiCall" varchar(20) not null
);
create table station (
	"statNm" varchar(100) not null,
	"statId" varchar(8) primary key,
	addr varchar(150) not null,
	"addrDetail" varchar(200),
	location geography(point, 4326) not null,
	"useTime" varchar(50) not null,
	"busiId" varchar(2),
	zcode varchar(2) not null,
	zscode varchar(5),
	kind varchar(2),
	"kindDetail" varchar(4),
	"parkingFree" varchar(1) check("parkingFree" in ('Y', 'N')),
	note varchar(200),
	"limitYn" varchar(1) check ("limitYn" in ('Y', 'N')),
	"limitDetail" varchar(100),
	"floorNum" varchar(50),
	"floorType" varchar(2) check ("floorType" in ('F', 'B')),
	foreign key ("busiId") references station_operator("busiId") on delete
	set null
);
CREATE TABLE charger(
	"statId" varchar(8) NOT NULL,
	"chgerId" varchar(2) NOT NULL,
	"chgerType" char(2) not null check (
		"chgerType" in (
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
	"statUpdDt" char(14) not null,
	"lastTsdt" char(14),
	"lastTedt" char(14),
	output varchar(20),
	method varchar(10) check(method in ('단독', '동시')),
	PRIMARY KEY ("statId", "chgerId"),
	FOREIGN KEY ("statId") REFERENCES station("statId") ON DELETE CASCADE
);
create table review (
	review_id BIGINT generated always as identity primary key,
	user_id UUID not null,
	content varchar(500) not null,
	rating int not null check(
		rating between 1 and 5
	),
	"statId" varchar(8) not null,
	--1~5
	created_at TIMESTAMP NOT NULL,
	updated_at TIMESTAMP NOT NULL,
	foreign key ("statId") references station("statId") on delete cascade,
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
	"statId" varchar(8) NOT NULL,
	created_at TIMESTAMP NOT NULL,
	unique (user_id, "statId"),
	FOREIGN KEY (user_id) REFERENCES users (user_id) ON DELETE CASCADE,
	FOREIGN KEY ("statId") REFERENCES station("statId") ON DELETE CASCADE
);
create table charger_alert(
	alert_id BIGINT generated always as identity primary key,
	user_id UUID not null,
	"statId" varchar(8) not null,
	"chgerId" varchar(2) not null,
	created_at TIMESTAMP not null,
	unique (user_id, "statId", "chgerId"),
	FOREIGN KEY (user_id) REFERENCES users (user_id) ON DELETE CASCADE,
	foreign key ("statId", "chgerId") references charger("statId", "chgerId") on delete cascade
);
create table fcm_token(
	id BIGINT generated always as identity primary key,
	user_id UUID not null,
	token varchar(255) not null unique,
	created_at timestamp not null,
	foreign key (user_id) references users (user_id) on delete cascade
);
create table notice(
	notice_id BIGINT generated always as identity primary key,
	title varchar(255) not null,
	content varchar(512) not null,
	created_at timestamp not null
);
create table notification_history(
	id BIGINT generated always as identity primary key,
	"statId" varchar(8),
	"chgerId" varchar(2),
	notice_id BIGINT,
	user_id UUID not null,
	is_read boolean not null,
	created_at timestamp,
	foreign key (user_id) references users (user_id) on delete cascade,
	foreign key (notice_id) references notice(notice_id) on delete cascade,
	check (
		("chgerId" is not null)::int + (notice_id is not null)::int = 1
	) -- chger, notice 둘 중 하나는 not null이어야 함
);
CREATE TABLE congestion (
	id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
	"statId" varchar(8) NOT NULL,
	"targetTime" int NOT NULL,
	"congestionLevel" varchar(2) NOT NULL CHECK ("congestionLevel" IN ('여유', '보통', '혼잡')),
	"congestionScore" double precision,
	"predictedAt" TIMESTAMP,
	FOREIGN KEY ("statId") REFERENCES station ("statId") ON DELETE CASCADE
);
create index idx_station_location on station using gist(location);