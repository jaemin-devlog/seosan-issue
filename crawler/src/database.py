# -*- coding: utf-8 -*-
import os
import time
import datetime
import logging
import mysql.connector
from mysql.connector import errorcode
from mysql.connector.pooling import MySQLConnectionPool
from contextlib import contextmanager

# ---------------------------------------
# 로깅
# ---------------------------------------
logging.basicConfig(level=logging.INFO, format="%(asctime)s - %(levelname)s - %(message)s")

# ---------------------------------------
# DB 설정 (환경변수 우선)
# ---------------------------------------
DB_CONFIG = {
    "host": os.environ.get("DB_HOST", "localhost"),
    "user": os.environ.get("DB_USER", "root"),
    "password": os.environ.get("DB_PASSWORD"),
    "database": os.environ.get("DB_NAME", "seosan_issue_db"),
    "charset": "utf8mb4",
    # mysql-connector는 collation 파라미터를 직접 받지 않으므로
    # 테이블/칼럼 단에서 COLLATE 지정으로 일관성 유지.
}

POOL: MySQLConnectionPool | None = None


def _build_pool(pool_size: int = 5) -> None:
    """글로벌 커넥션 풀 초기화."""
    global POOL
    if POOL is None:
        POOL = MySQLConnectionPool(pool_name="seosan_pool", pool_size=pool_size, **DB_CONFIG)
        logging.info(f"MySQL 커넥션 풀 생성 완료 (size={pool_size})")


def get_db_connection(max_retries: int = 5, retry_delay: int = 5):
    """풀에서 커넥션을 얻는다. 실패 시 재시도."""
    _build_pool()
    assert POOL is not None
    for i in range(max_retries):
        try:
            conn = POOL.get_connection()
            logging.debug(f"DB 커넥션 획득 성공 (시도 {i+1}/{max_retries})")
            return conn
        except mysql.connector.Error as err:
            if err.errno == errorcode.ER_ACCESS_DENIED_ERROR:
                logging.error("MySQL 접근 권한 오류: 사용자/비밀번호 확인 필요.", exc_info=True)
                return None
            elif err.errno == errorcode.ER_BAD_DB_ERROR:
                logging.error(f"데이터베이스 '{DB_CONFIG['database']}'가 존재하지 않습니다.", exc_info=True)
                return None
            else:
                logging.warning(f"DB 커넥션 오류 (시도 {i+1}/{max_retries}): {err}")
                if i < max_retries - 1:
                    time.sleep(retry_delay)
                else:
                    logging.error(f"최대 재시도 초과. DB 연결 실패.", exc_info=True)
                    return None
    return None


@contextmanager
def db_cursor(dict_cursor: bool = False):
    """with 블록으로 conn/cursor 자동 정리."""
    conn = get_db_connection()
    if not conn:
        yield None, None
        return
    cursor = None
    try:
        cursor = conn.cursor(dictionary=dict_cursor)
        yield conn, cursor
        conn.commit()
    except Exception as e:
        if conn:
            conn.rollback()
        logging.error(f"DB 작업 중 오류: {e}", exc_info=True)
        raise
    finally:
        if cursor:
            cursor.close()
        conn.close()


def init_db():
    """스키마 초기화: 테이블 생성 및 인덱스 보강."""
    with db_cursor() as (conn, cur):
        if not cur:
            return

        # post: link 길이 1024로 확장, 인덱스 보강
        cur.execute("""
            CREATE TABLE IF NOT EXISTS `post` (
              `id` BIGINT NOT NULL AUTO_INCREMENT,
              `crawled_at` DATETIME NOT NULL,
              `pub_date` VARCHAR(64) NOT NULL,
              `views` INT DEFAULT 0,
              `category` VARCHAR(255) NOT NULL,
              `content` MEDIUMTEXT NOT NULL,
              `department` VARCHAR(255) DEFAULT NULL,
              `link` VARCHAR(1024) NOT NULL,
              `region` VARCHAR(255) NOT NULL,
              `title` VARCHAR(255) NOT NULL,
              PRIMARY KEY (`id`),
              UNIQUE KEY `UK_post_link` (`link`),
              KEY `IDX_post_crawled_at` (`crawled_at`),
              KEY `IDX_post_category` (`category`),
              KEY `IDX_post_region` (`region`)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
        """)

        # 크롤링 진행상태 테이블
        cur.execute("""
            CREATE TABLE IF NOT EXISTS `last_crawled_info` (
                `category_name` VARCHAR(255) PRIMARY KEY,
                `last_crawled_link` VARCHAR(1024),
                `last_crawled_at` DATETIME DEFAULT CURRENT_TIMESTAMP
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
        """)

        logging.info(f"DB 초기화 완료 (database='{DB_CONFIG['database']}').")


# ---------------------------------------
# 카테고리 매핑
# ---------------------------------------
def map_category(category_name: str) -> str:
    """크롤링 카테고리명 → 백엔드 Enum 매핑."""
    mapping = {
        "복지정보-어르신": "WELFARE_SENIOR",
        "복지정보-장애인": "WELFARE_DISABLED",
        "복지정보-여성가족": "WELFARE_WOMEN_FAMILY",
        "복지정보-아동청소년": "WELFARE_CHILD_YOUTH",
        "복지정보-청년": "WELFARE_YOUTH",
        "보건/건강": "HEALTH_WELLNESS",
        "공지사항": "NOTICE",
        "보도자료": "PRESS_RELEASE",
        "문화소식": "CULTURE_NEWS",
        "시티투어": "CITY_TOUR",
        "관광-안내": "TOUR_GUIDE",
    }
    # 읍면동 공지사항은 모두 NOTICE로 귀속
    if category_name.endswith(" 공지사항"):
        return "NOTICE"
    return mapping.get(category_name, "UNKNOWN")


# 미리 가져와서 루프 중 import 비용 제거
try:
    from src.regions import REGIONS
except Exception:
    REGIONS = []


def _infer_region_from_category(category_name: str) -> str | None:
    """카테고리명에서 지역 추론: '해미면 공지사항' → '해미면'."""
    for r in REGIONS:
        if category_name.startswith(r + " "):
            return r
    return None


def save_to_db(items: list[dict], category_name: str):
    """
    크롤링한 게시글 목록 저장.
    - link UNIQUE 기준으로 UPSERT
    - views/department 등은 중복 시 업데이트
    """
    if not items:
        logging.info(f"저장할 항목이 없습니다. (카테고리: {category_name})")
        return

    category_enum = map_category(category_name)
    inferred_region_from_cat = _infer_region_from_category(category_name)

    # INSERT ... ON DUPLICATE KEY UPDATE
    insert_sql = """
        INSERT INTO post
            (title, content, link, pub_date, region, category, department, views, crawled_at)
        VALUES
            (%s, %s, %s, %s, %s, %s, %s, %s, %s)
        ON DUPLICATE KEY UPDATE
            title = VALUES(title),
            content = VALUES(content),
            pub_date = VALUES(pub_date),
            region = VALUES(region),
            category = VALUES(category),
            department = VALUES(department),
            views = GREATEST(views, VALUES(views)), -- 기존 값과 새 값 중 큰 값 유지(선택)
            crawled_at = VALUES(crawled_at)
    """

    now = datetime.datetime.now()
    params = []

    for it in items:
        # 조회수 안전 파싱
        try:
            views = int(it.get("views", 0))
        except (ValueError, TypeError):
            views = 0

        specific_region = it.get("specific_region")
        if not specific_region:
            specific_region = inferred_region_from_cat or "서산시 전체"

        params.append((
            it.get("title"),
            it.get("content"),
            it.get("link"),
            it.get("date"),
            specific_region,
            category_enum,
            it.get("department"),
            views,
            now,
        ))

    with db_cursor() as (conn, cur):
        if not cur:
            return
        try:
            cur.executemany(insert_sql, params)
            inserted = cur.rowcount  # UPSERT라 정확히 "신규 개수"는 아님(업데이트 포함)
            logging.info(f"{len(items)}건 처리 완료(신규+업데이트 포함). (카테고리: {category_name})")
        except mysql.connector.Error as err:
            logging.error(f"게시글 저장 중 오류: {err}", exc_info=True)


def get_last_crawled_link(category_name: str) -> str | None:
    with db_cursor() as (conn, cur):
        if not cur:
            return None
        cur.execute(
            "SELECT last_crawled_link FROM last_crawled_info WHERE category_name = %s",
            (category_name,),
        )
        row = cur.fetchone()
        return row[0] if row else None


def update_last_crawled_link(category_name: str, last_crawled_link: str):
    with db_cursor() as (conn, cur):
        if not cur:
            return
        cur.execute(
            """
            INSERT INTO last_crawled_info (category_name, last_crawled_link, last_crawled_at)
            VALUES (%s, %s, %s)
            ON DUPLICATE KEY UPDATE
                last_crawled_link = VALUES(last_crawled_link),
                last_crawled_at = VALUES(last_crawled_at)
            """,
            (category_name, last_crawled_link, datetime.datetime.now()),
        )


def get_content_statistics() -> dict | None:
    """
    전체/오늘/어제 수집 건수를 반환.
    """
    stats = {"total_count": 0, "today_count": 0, "yesterday_count": 0}
    with db_cursor() as (conn, cur):
        if not cur:
            return None
        try:
            cur.execute("SELECT COUNT(*) FROM post;")
            stats["total_count"] = cur.fetchone()[0]

            cur.execute("SELECT COUNT(*) FROM post WHERE DATE(crawled_at) = CURDATE();")
            stats["today_count"] = cur.fetchone()[0]

            cur.execute("SELECT COUNT(*) FROM post WHERE DATE(crawled_at) = CURDATE() - INTERVAL 1 DAY;")
            stats["yesterday_count"] = cur.fetchone()[0]
        except Exception as e:
            logging.error(f"통계 조회 오류: {e}", exc_info=True)
            return None
    return stats
