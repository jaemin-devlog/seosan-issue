# -*- coding: utf-8 -*-
import mysql.connector
from mysql.connector import errorcode
import datetime
import os
import logging
import time

# 로깅 설정
logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(levelname)s - %(message)s')

# MySQL 연결 설정 (환경 변수 사용)
db_config = {
    'host': os.environ.get('DB_HOST', 'localhost'),
    'user': os.environ.get('DB_USER', 'root'),
    'password': os.environ.get('DB_PASSWORD'),
    'database': os.environ.get('DB_NAME', 'seosan_issue_db')
}

def get_db_connection(max_retries=5, retry_delay=5):
    """MySQL 데이터베이스 연결을 생성하고 반환합니다. 재시도 로직 포함."""
    for i in range(max_retries):
        try:
            conn = mysql.connector.connect(**db_config, charset='utf8mb4', collation='utf8mb4_unicode_ci')
            logging.info(f"데이터베이스 연결 성공 (시도 {i+1}/{max_retries})")
            return conn
        except mysql.connector.Error as err:
            if err.errno == errorcode.ER_ACCESS_DENIED_ERROR:
                logging.error("MySQL 접근 권한이 없습니다. 사용자 이름과 비밀번호를 확인하세요.", exc_info=True)
                return None # 접근 거부는 재시도해도 소용없으므로 즉시 종료
            elif err.errno == errorcode.ER_BAD_DB_ERROR:
                logging.error(f"데이터베이스 '{db_config['database']}'가 존재하지 않습니다.", exc_info=True)
                return None # 잘못된 DB 이름은 재시도해도 소용없으므로 즉시 종료
            else:
                logging.warning(f"데이터베이스 연결 중 오류 발생 (시도 {i+1}/{max_retries}): {err}")
                if i < max_retries - 1:
                    logging.info(f"재시도 {retry_delay}초 후 다시 시도합니다...")
                    time.sleep(retry_delay)
                else:
                    logging.error(f"최대 재시도 횟수({max_retries}) 초과. 데이터베이스 연결 실패.", exc_info=True)
                    return None
    return None

def init_db():
    """데이터베이스와 최종 스키마의 테이블을 초기화합니다."""
    conn = get_db_connection()
    if not conn:
        return
        
    cursor = conn.cursor()
    
    # 최종 post 테이블 스키마 (emotion, welfare_category 제거)
    cursor.execute("""
        CREATE TABLE IF NOT EXISTS `post` (
          `id` bigint NOT NULL AUTO_INCREMENT,
          `crawled_at` datetime NOT NULL,
          `pub_date` varchar(255) NOT NULL,
          `views` int DEFAULT NULL,
          `category` varchar(255) NOT NULL,
          `content` text NOT NULL,
          `department` varchar(255) DEFAULT NULL,
          `link` varchar(255) NOT NULL,
          `region` varchar(255) NOT NULL,
          `title` varchar(255) NOT NULL,
          PRIMARY KEY (`id`),
          UNIQUE KEY `UK_post_link` (`link`)
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
    """)

    # last_crawled_info 테이블 생성 (크롤링 상태 관리)
    cursor.execute("""
        CREATE TABLE IF NOT EXISTS `last_crawled_info` (
            `category_name` VARCHAR(255) PRIMARY KEY,
            `last_crawled_link` VARCHAR(255),
            `last_crawled_at` DATETIME DEFAULT CURRENT_TIMESTAMP
        )
    """)

    logging.info(f"데이터베이스 '{db_config['database']}' 초기화 및 테이블 생성/업데이트 완료.")
    cursor.close()
    conn.close()

def map_category(category_name):
    """크롤링 카테고리 이름을 백엔드 Category Enum 값으로 매핑합니다."""
    # crawler_config.py와 일치하도록 키 값을 수정 ('/' -> '-')
    mapping = {
        "복지정보-어르신": "WELFARE_SENIOR",
        "복지정보-장애인": "WELFARE_DISABLED",
        "복지정보-여성가족": "WELFARE_WOMEN_FAMILY",
        "복지정보-아동청소년": "WELFARE_CHILD_YOUTH",
        "복지정보-청년": "WELFARE_YOUTH",
        "보건-건강": "HEALTH_WELLNESS",
        "공지사항": "NOTICE",
        "보도자료": "PRESS_RELEASE",
        "문화소식": "CULTURE_NEWS",
        "시티투어": "CITY_TOUR",
        "관광-안내": "TOUR_GUIDE"
    }
    return mapping.get(category_name, "UNKNOWN")

def save_to_db(data, category_name):
    """크롤링한 데이터를 최종 스키마에 맞게 MySQL에 저장합니다."""
    conn = get_db_connection()
    if not conn:
        return

    cursor = conn.cursor()
    new_posts_count = 0
    category_enum = map_category(category_name)

    for item in data:
        try:
            views = int(item.get('views', 0))
        except (ValueError, TypeError):
            views = 0

        # 최종 스키마에 맞는 INSERT 쿼리
        insert_query = """
            INSERT INTO post (title, content, link, pub_date, region, category, department, views, crawled_at)
            VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s)
        """
        specific_region = item.get('specific_region')
        region_to_save = specific_region if specific_region else '서산시 전체'
        post_data = (
            item.get('title'),
            item.get('content'),
            item.get('link'),
            item.get('date'),
            region_to_save,
            category_enum,
            item.get('department'),
            views,
            datetime.datetime.now()
        )
        
        try:
            cursor.execute(insert_query, post_data)
            new_posts_count += 1
        except mysql.connector.Error as err:
            if err.errno == errorcode.ER_DUP_ENTRY:
                logging.debug(f"중복 링크 발견, 건너뛰기: {item.get('link')}")
            else:
                logging.error(f"데이터 삽입 중 오류 발생: {err} - {item.get('link')}", exc_info=True)

    conn.commit()
    logging.info(f"{new_posts_count}개의 새로운 게시글을 데이터베이스에 저장 완료. (카테고리: {category_name})")
    cursor.close()
    conn.close()

def get_last_crawled_link(category_name):
    """특정 카테고리의 마지막으로 크롤링된 게시글 링크를 가져옵니다."""
    conn = get_db_connection()
    if not conn:
        return None
    cursor = conn.cursor()
    query = "SELECT last_crawled_link FROM last_crawled_info WHERE category_name = %s"
    cursor.execute(query, (category_name,))
    result = cursor.fetchone()
    cursor.close()
    conn.close()
    return result[0] if result else None

def update_last_crawled_link(category_name, last_crawled_link):
    """마지막으로 크롤링된 게시글 링크를 업데이트합니다."""
    conn = get_db_connection()
    if not conn:
        return
    cursor = conn.cursor()
    query = """
        INSERT INTO last_crawled_info (category_name, last_crawled_link, last_crawled_at)
        VALUES (%s, %s, %s)
        ON DUPLICATE KEY UPDATE last_crawled_link = VALUES(last_crawled_link), last_crawled_at = VALUES(last_crawled_at)
    """
    cursor.execute(query, (category_name, last_crawled_link, datetime.datetime.now()))
    conn.commit()
    cursor.close()
    conn.close()

def get_content_statistics():
    """
    전체 콘텐츠 수, 오늘의 수집 수, 전날 수집 수를 반환합니다.
    """
    conn = get_db_connection()
    if not conn:
        return None

    cursor = conn.cursor()
    stats = {
        "total_count": 0,
        "today_count": 0,
        "yesterday_count": 0
    }

    try:
        # 전체 콘텐츠 수
        cursor.execute("SELECT COUNT(*) FROM post;")
        stats["total_count"] = cursor.fetchone()[0]

        # 오늘의 수집 수
        cursor.execute("SELECT COUNT(*) FROM post WHERE DATE(crawled_at) = CURDATE();")
        stats["today_count"] = cursor.fetchone()[0]

        # 전날 수집 수
        cursor.execute("SELECT COUNT(*) FROM post WHERE DATE(crawled_at) = CURDATE() - INTERVAL 1 DAY;")
        stats["yesterday_count"] = cursor.fetchone()[0]

    except Exception as e:
        logging.error(f"Error fetching content statistics: {e}", exc_info=True)
        return None
    finally:
        cursor.close()
        conn.close()

    return stats
