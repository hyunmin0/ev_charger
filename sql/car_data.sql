-- 차량 기본 데이터 (car, car_charger, charge) 적재
-- 원본: sql/data/*.csv (UTF-8, 첫 줄은 헤더라 건너뜀)
-- 실행: psql에서 \encoding UTF8 → \i C:/projects/ev_charger/sql/car_data.sql
-- CSV의 빈 칸은 NULL로 들어감 (trim, combined, city, highway, charger_output)
-- 다시 실행하면 unique 제약에 걸리므로, 재적재할 때는 아래 truncate 주석을 풀고 실행
-- truncate car, car_charger, charge restart identity cascade;

begin;

\copy car (brand, model, battery_type, model_year, drive_type, wheel_size, battery_capacity, trim, combined, city, highway) from 'C:/projects/ev_charger/sql/data/car.csv' with (format csv, header true, encoding 'UTF8')

-- CSV 헤더는 year지만 테이블 컬럼은 model_year
\copy car_charger (brand, model, model_year, charger_type) from 'C:/projects/ev_charger/sql/data/car_charger.csv' with (format csv, header true, encoding 'UTF8')

-- CSV 컬럼 순서: brand, model, battery_type, year, charger_output, charger_type, minutes
\copy charge (brand, model, battery_type, model_year, charger_output, charger_type, minutes) from 'C:/projects/ev_charger/sql/data/charge.csv' with (format csv, header true, encoding 'UTF8')

commit;

select 'car' as table_name, count(*) from car
union all select 'car_charger', count(*) from car_charger
union all select 'charge', count(*) from charge;
