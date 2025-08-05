# -*- coding: utf-8 -*-
import mysql.connector
from mysql.connector import errorcode
import datetime
import os
import logging
import time

# 로깅 설정 (api.py에서 이미 설정되었으므로 여기서는 기본 레벨만 설정)
logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(levelname)s - %(message)s')

# MySQL 연결 설정 (환경 변수 사용)
db_config = {
    'host': os.environ.get('DB_HOST', 'localhost'),
    'user': 'root',
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
        except Exception as e:
            logging.error(f"데이터베이스 연결 중 알 수 없는 오류 발생 (시도 {i+1}/{max_retries}): {e}", exc_info=True)
            if i < max_retries - 1:
                logging.info(f"재시도 {retry_delay}초 후 다시 시도합니다...")
                time.sleep(retry_delay)
            else:
                logging.error(f"최대 재시도 횟수({max_retries}) 초과. 데이터베이스 연결 실패.", exc_info=True)
                return None
    return None

def init_db():
    """데이터베이스와 테이블을 초기화합니다."""
    conn = get_db_connection()
    if not conn:
        return
        
    cursor = conn.cursor()
    
    # post 테이블 생성 (backend의 Post 엔티티와 일치)
    cursor.execute("""
        CREATE TABLE IF NOT EXISTS `post` (
          `id` bigint NOT NULL AUTO_INCREMENT,
          `crawled_at` datetime NOT NULL,
          `pub_date` varchar(255) NOT NULL,
          `views` int DEFAULT NULL,
          `category` varchar(255) NOT NULL,
          `content` text NOT NULL,
          `department` varchar(255) DEFAULT NULL,
          `emotion` varchar(255) DEFAULT NULL,
          `link` varchar(255) NOT NULL,
          `region` varchar(255) NOT NULL,
          `title` varchar(255) NOT NULL,
          `welfare_category` varchar(255) DEFAULT NULL,
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

def save_to_db(data, category_name):
    """크롤링한 데이터를 MySQL에 저장합니다."""
    conn = get_db_connection()
    if not conn:
        return

    cursor = conn.cursor()
    new_posts_count = 0

    # 카테고리 매핑
    category_enum = map_category(category_name)
    welfare_category_enum = map_welfare_category(category_name)

    for item in data:
        # 'views'를 정수형으로 변환, 실패 시 0으로 설정
        try:
            views = int(item.get('views', 0))
        except (ValueError, TypeError):
            views = 0
            logging.warning(f"Invalid views value for item: {item.get('views')}. Setting to 0.")

        insert_query = """
            INSERT INTO post (title, content, link, pub_date, region, category, welfare_category, emotion, department, views, crawled_at)
            VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s)
        """

        # 데이터 준비
        post_data = (
            item.get('title'),
            item.get('content'),
            item.get('link'),
            item.get('date'),
            '서산시 전체',  # 크롤링 데이터는 '서산시 전체'로 고정
            category_enum,
            welfare_category_enum,
            'NEUTRAL',  # 감정 분석은 NEUTRAL로 고정
            item.get('department'),
            views,
            datetime.datetime.now()
        )
        
        try: # Try block for database insertion
            cursor.execute(insert_query, post_data)
            new_posts_count += 1
        except mysql.connector.Error as err:
            # 중복 오류(1062)는 무시하고 다음 항목으로 진행
            if err.errno == errorcode.ER_DUP_ENTRY:
                logging.debug(f"중복 링크 발견, 건너뛰기: {item.get('link')}")
            else:
                logging.error(f"데이터 삽입 중 오류 발생: {err} - {item.get('link')}", exc_info=True)
        except Exception as e:
            logging.error(f"데이터 삽입 중 알 수 없는 오류 발생: {e} - {item.get('link')}", exc_info=True)

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

def map_category(category_name):
    """크롤링 카테고리 이름을 백엔드 Category Enum 값으로 매핑합니다."""
    if category_name in ["고시/공고", "공지사항", "보도자료"]:
        return "PUBLIC_INSTITUTION"
    elif category_name.startswith("복지정보"):
        return "WELFARE"
    elif category_name == "문화소식":
        return "CULTURE"
    else:
        return "UNKNOWN" # 기본값

def map_welfare_category(category_name):
    """복지정보 카테고리 이름을 백엔드 WelfareCategory Enum 값으로 매핑합니다."""
    if "어르신" in category_name:
        return "SENIOR"
    elif "장애인" in category_name:
        return "DISABLED"
    elif "여성가족" in category_name:
        return "WOMEN_FAMILY"
    elif "아동청소년" in category_name:
        return "CHILD"
    elif "청년" in category_name:
        return "YOUTH"
    else:
        return None