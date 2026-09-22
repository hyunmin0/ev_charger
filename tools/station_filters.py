"""충전소 조회 쿼리의 WHERE 조각을 만드는 곳 (+ 그 조건에 대응하는 LLM 파라미터 스키마).

역할 경계는 딱 하나로 정한다:
  - 이 파일(station_filters.py): WHERE에 들어갈 SQL 조각을 **만들기만** 한다. DB에 접속하지 않는다
  - station.py: 어떤 조각을 쓸지 고르고, AND로 조립하고, 실제로 실행한다

WHERE 조각은 두 종류다.
  - 검색 출발점: 어느 범위에서 찾기 시작할지 (반경 / 키워드). tool마다 하나씩 직접 고른다
  - 거르기 조건: 그 범위 안에서 무엇을 남길지 (충전기 타입, 급속/완속, 이용 가능, 무료주차).
                모든 조회 tool이 똑같이 쓰므로 station.py의 _append_common_filters가 일괄로 붙인다

각 build_* 함수는 (SQL 조각, 바인딩 파라미터 dict)를 돌려준다.
조건이 안 걸릴 수 있는 것(거르기 조건)은 조건이 없으면 (None, {})을 돌려준다.
"""

# 급속/완속 기준 출력 (환경부 급속충전기 기준 50kW)
FAST_CHARGER_MIN_KW = 50

# charger.output이 varchar라서 바로 숫자 비교가 안 된다.
# 숫자가 아닌 문자를 모두 지운 뒤 int로 캐스팅하고, 빈 문자열이 되면 NULL 처리.
# TODO: output이 비어있는 충전기는 급속/완속 어느 쪽에도 안 잡힘.
#       실데이터에 빈 값이 많으면 chgerType 기반 분류를 폴백으로 추가할 것
_OUTPUT_KW_SQL = "NULLIF(regexp_replace(c.output, '[^0-9]', '', 'g'), '')::int"

# 거리 계산식 — SELECT의 distance_km와 ORDER BY에서 공용으로 씀 (:lat, :lng 바인딩 필요)
DISTANCE_KM_SQL = "ST_Distance(s.location, ST_MakePoint(:lng, :lat)::geography) / 1000"


# ---------------------------------------------------------------- LLM 파라미터 스키마 조각
# 두 tool의 JSON 스키마에서 그대로 재사용 (설명이 따로 놀면 LLM이 tool마다 다르게 해석하므로 한 곳에서 관리)

CHARGER_TYPES_PROPERTY = {
    "type": "array",
    "description": (
        "충전기 타입 코드 목록 (하나라도 일치하면 반환). "
        "01=DC차데모, 02=AC완속, 03=DC차데모+AC3상, "
        "04=DC콤보, 05=DC차데모+DC콤보, 06=DC차데모+AC3상+DC콤보, "
        "07=AC3상, 08=DC콤보+AC3상, 09=HPC(버스), "
        "10=슈퍼차저, 11=이동형충전기. "
        "사용자가 타입을 밝히지 않았고 등록된 차량이 있으면, 그 차량이 지원하는 커넥터 타입 전체를 넘기세요."
    ),
    "items": {
        "type": "string",
        "enum": ["01", "02", "03", "04", "05", "06", "07", "08", "09", "10", "11"],
    },
}

SPEED_PROPERTY = {
    "type": "string",
    "description": (
        f"충전 속도 필터. fast=급속({FAST_CHARGER_MIN_KW}kW 이상), slow=완속({FAST_CHARGER_MIN_KW}kW 미만). "
        "사용자가 '급속'/'완속'을 직접 언급했을 때만 넘기고, 언급이 없으면 생략하세요."
    ),
    "enum": ["fast", "slow"],
}

PARKING_FREE_PROPERTY = {
    "type": "boolean",
    "description": "True이면 무료주차 충전소만 반환. 미지정이면 조건 없음",
}


# ---------------------------------------------------------------- WHERE 조각 ① 검색 출발점
# tool마다 하나씩 직접 골라서 쓴다 (get_nearby_stations는 반경, search_stations는 키워드).
# 항상 조건이 걸리므로 None을 돌려주지 않는다.

def build_radius_clause(radius_km: float) -> tuple[str, dict]:
    """좌표 기준 반경 필터. km → m 변환도 여기서 한다 (:radius_m을 쓰는 건 이 조건뿐이므로)."""
    return (
        "ST_DWithin(s.location, ST_MakePoint(:lng, :lat)::geography, :radius_m)",
        {"radius_m": radius_km * 1000},
    )


def build_keyword_clause(keyword: str) -> tuple[str, dict]:
    """충전소명 또는 주소 부분 일치. 사용자 입력이라 LIKE 와일드카드는 이스케이프한다."""
    escaped = keyword.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_")
    return '(s."statNm" ILIKE :keyword OR s.addr ILIKE :keyword)', {"keyword": f"%{escaped}%"}


# ---------------------------------------------------------------- WHERE 조각 ② 거르기 조건
# 모든 조회 tool이 공통으로 쓴다. 사용자가 지정 안 하면 조건이 없으므로 (None, {})을 돌려줄 수 있다.

def build_charger_clause(
    charger_types: list[str] | None = None,
    available_only: bool = True,
    speed: str | None = None,
) -> tuple[str | None, dict]:
    """charger 테이블 조건들을 EXISTS 하나로 묶는다.

    조건을 AND로 묶어 EXISTS 안에 넣기 때문에, "같은 충전기 한 대"가 모든 조건을
    동시에 만족해야 매칭된다 (급속기는 있는데 고장이고 멀쩡한 건 완속기뿐인 곳은 제외됨).
    """
    conditions = []
    params: dict = {}

    if available_only:
        conditions.append("c.stat = '1'")       # '1' = 이용가능

    if charger_types:
        # 목록 중 하나라도 일치하면 매칭 (예: 차량이 DC콤보+AC완속을 둘 다 지원하면 둘 다 허용)
        conditions.append('c."chgerType" = ANY(:charger_types)')
        params["charger_types"] = charger_types

    if speed == "fast":
        conditions.append(f"{_OUTPUT_KW_SQL} >= :fast_min_kw")
        params["fast_min_kw"] = FAST_CHARGER_MIN_KW
    elif speed == "slow":
        conditions.append(f"{_OUTPUT_KW_SQL} < :fast_min_kw")
        params["fast_min_kw"] = FAST_CHARGER_MIN_KW

    if not conditions:
        return None, {}

    clause = (
        'EXISTS (SELECT 1 FROM charger c '
        'WHERE c."statId" = s."statId" AND ' + " AND ".join(conditions) + ")"
    )
    return clause, params


def build_parking_free_clause(parking_free: bool | None) -> tuple[str | None, dict]:
    """무료주차 조건. False로 넘어와도 '유료만'이 아니라 '조건 없음'으로 취급한다."""
    if parking_free is True:
        return 's."parkingFree" = \'Y\'', {}
    return None, {}
