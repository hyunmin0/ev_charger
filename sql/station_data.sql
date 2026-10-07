-- 충전소 기본 데이터 (station, charger) 적재 / 주기적 갱신
-- 원본: sql/data/station.csv (102,370행), sql/data/charger.csv (526,586행) (UTF-8, 첫 줄은 헤더라 건너뜀)
-- 실행: table.sql → station_operator.sql 실행 후 psql에서 \encoding UTF8 → \i C:/projects/ev_charger/sql/station_data.sql
-- 다시 실행해도 됨: 같은 statId / (statId, chgerId)는 CSV 값으로 덮어씀 (upsert)
--
-- CSV를 임시 테이블(전부 text)에 먼저 넣고, 값을 정리한 뒤 실제 테이블로 옮김
--  - 빈 칸은 NULL로 넣음 (floorType, parkingFree, limitYn, method의 check 제약이 ''를 허용하지 않음)
--  - useTime(1,950행), addr(1행)은 not null인데 CSV에 값이 없어서 ''로 넣음
--  - busiId가 빈 충전소(1,303개)는 statId 앞 두 글자로 채움
--    (busiId가 있는 101,067개 모두 statId 앞 두 글자 = busiId, 예: BNBN0150 → BN)
--  - 운영기관(station_operator)은 station_operator.sql에서 관리하므로 덮어쓰지 않음
--    CSV에 처음 보는 busiId가 있을 때만 CSV의 이름, 전화번호로 임시 추가 → 이후 station_operator.sql에 정식으로 추가
--  - location은 CSV에 hex EWKB(SRID 4326)로 들어 있어서 geography로 바로 변환됨

set time zone 'Asia/Seoul';
begin;

create temp table station_csv (
    "statNm" text, "statId" text, addr text, "addrDetail" text, location text, "useTime" text,
    "busiNm" text, "busiCall" text, zcode text, zscode text, kind text, "kindDetail" text,
    "parkingFree" text, note text, "limitYn" text, "limitDetail" text, "floorNum" text, "floorType" text, "busiId" text
) on commit drop;

create temp table charger_csv (
    "statId" text, "chgerId" text, "chgerType" text, stat text, "statUpdDt" text,
    "lastTsdt" text, "lastTedt" text, output text, method text
) on commit drop;

\copy station_csv from 'C:/projects/ev_charger/sql/data/station.csv' with (format csv, header true, encoding 'UTF8')
\copy charger_csv from 'C:/projects/ev_charger/sql/data/charger.csv' with (format csv, header true, encoding 'UTF8')

-- busiId가 빈 행은 statId 앞 두 글자로
update station_csv
set "busiId" = left("statId", 2)
where nullif("busiId", '') is null;

-- ===== 운영기관 (station_operator.sql에 없는 코드만 임시 추가) =====
-- mode(): 그룹 안에서 가장 많이 나온 값 (NULL은 무시)
insert into station_operator ("busiId", "busiNm", "busiCall")
select "busiId",
    coalesce(mode() within group (order by nullif("busiNm", '')), "busiId"),
    coalesce(mode() within group (order by nullif("busiCall", '')), '') -- busiCall은 not null
from station_csv
group by "busiId"
on conflict ("busiId") do nothing;

-- ===== 충전소 =====
insert into station (
        "statNm", "statId", addr, "addrDetail", location, "useTime", "busiId", zcode, zscode,
        kind, "kindDetail", "parkingFree", note, "limitYn", "limitDetail", "floorNum", "floorType"
    )
select "statNm",
    "statId",
    coalesce(addr, ''),
    nullif("addrDetail", ''),
    location::geography,
    coalesce("useTime", ''),
    "busiId",
    zcode,
    nullif(zscode, ''),
    nullif(kind, ''),
    nullif("kindDetail", ''),
    nullif("parkingFree", ''),
    nullif(note, ''),
    nullif("limitYn", ''),
    nullif("limitDetail", ''),
    nullif("floorNum", ''),
    nullif("floorType", '')
from station_csv
on conflict ("statId") do update
set "statNm" = excluded."statNm",
    addr = excluded.addr,
    "addrDetail" = excluded."addrDetail",
    location = excluded.location,
    "useTime" = excluded."useTime",
    "busiId" = excluded."busiId",
    zcode = excluded.zcode,
    zscode = excluded.zscode,
    kind = excluded.kind,
    "kindDetail" = excluded."kindDetail",
    "parkingFree" = excluded."parkingFree",
    note = excluded.note,
    "limitYn" = excluded."limitYn",
    "limitDetail" = excluded."limitDetail",
    "floorNum" = excluded."floorNum",
    "floorType" = excluded."floorType";

-- ===== 충전기 =====
insert into charger (
        "statId", "chgerId", "chgerType", stat, "statUpdDt", "lastTsdt", "lastTedt", output, method
    )
select "statId",
    "chgerId",
    "chgerType",
    stat,
    "statUpdDt",
    nullif("lastTsdt", ''),
    nullif("lastTedt", ''),
    nullif(output, ''),
    nullif(method, '')
from charger_csv
on conflict ("statId", "chgerId") do update
set "chgerType" = excluded."chgerType",
    stat = excluded.stat,
    "statUpdDt" = excluded."statUpdDt",
    "lastTsdt" = excluded."lastTsdt",
    "lastTedt" = excluded."lastTedt",
    output = excluded.output,
    method = excluded.method;

commit;

select 'station_operator' as table_name, count(*) from station_operator
union all select 'station', count(*) from station
union all select 'charger', count(*) from charger;
