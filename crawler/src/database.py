# -*- coding: utf-8 -*-
import mysql.connector
from mysql.connector import errorcode
import datetime
import os

# MySQL 연결 설정 (환경 변수 사용)
db_config = {
    'host': os.environ.get('DB_HOST', 'localhost'),
    'user': os.environ.get('DB_USER', 'root'),
    'password': os.environ.get('DB_PASSWORD'),
    'database': os.environ.get('DB_NAME', 'seosan_issue_db')
}

def get_db_connection():
    """MySQL 데이터베이스 연결을 생성하고 반환합니다."""
    try:
        conn = mysql.connector.connect(**db_config)
        return conn
    except mysql.connector.Error as err:
        if err.errno == errorcode.ER_ACCESS_DENIED_ERROR:
            print("MySQL 접근 권한이 없습니다. 사용자 이름과 비밀번호를 확인하세요.")
        elif err.errno == errorcode.ER_BAD_DB_ERROR:
            print(f"데이터베이스 '{db_config['database']}'가 존재하지 않습니다.")
        else:
            print(f"데이터베이스 연결 중 오류 발생: {err}")
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
          `crawled_at` datetime(6) NOT NULL,
          `pub_date` varchar(255) NOT NULL,
          `views` int DEFAULT NULL,
          `category` enum('NEWS','WELFARE','PUBLIC_INSTITUTION','CULTURE','CAFE','BLOG') NOT NULL,
          `content` text NOT NULL,
          `department` varchar(255) DEFAULT NULL,
          `emotion` enum('FEAR','SURPRISE','ANGER','SADNESS','NEUTRAL','HAPPINESS','DISGUST') DEFAULT NULL,
          `link` varchar(255) NOT NULL,
          `region` varchar(255) NOT NULL,
          `title` varchar(255) NOT NULL,
          `welfare_category` enum('SENIOR','DISABLED','WOMEN_FAMILY','YOUTH','CHILD') DEFAULT NULL,
          PRIMARY KEY (`id`),
          UNIQUE KEY `UK_post_link` (`link`)
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
    """)

    # last_crawled_info 테이블 생성 (크롤링 상태 관리)
    cursor.execute("""
        CREATE TABLE IF NOT EXISTS `last_crawled_info` (
            `category_name` TEXT PRIMARY KEY,
            `last_post_id` INTEGER,
            `last_crawled_at` DATETIME DEFAULT CURRENT_TIMESTAMP
        )
    """)

    print(f"데이터베이스 '{db_config['database']}' 초기화 및 테이블 생성/업데이트 완료.")
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
        try:
            # 'views'를 정수형으로 변환, 실패 시 0으로 설정
            try:
                views = int(item.get('views', 0))
            except (ValueError, TypeError):
                views = 0

            insert_query = """
                INSERT INTO post (title, content, link, pub_date, region, category, welfare_category, emotion, department, views, crawled_at)
                VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s)
            """
            # ON DUPLICATE KEY UPDATE 구문 추가 (link가 UNIQUE일 때)
            # link가 이미 존재하면 아무것도 하지 않도록 설정
            insert_query += " ON DUPLICATE KEY UPDATE link=link;"


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
            
            cursor.execute(insert_query, post_data)
            if cursor.rowcount > 0: # 실제로 삽입된 경우
                new_posts_count += 1

        except mysql.connector.Error as err:
            # 중복 오류(1062)는 무시
            if err.errno != 1062:
                 print(f"DEBUG: 데이터 삽입 중 오류 발생: {err} - {item.get('link')}")
        except Exception as e:
            print(f"DEBUG: 데이터 삽입 중 알 수 없는 오류 발생: {e} - {item.get('link')}")

    conn.commit()
    print(f"{new_posts_count}개의 새로운 게시글을 데이터베이스에 저장 완료. (카테고리: {category_name})")
    cursor.close()
    conn.close()

def get_last_crawled_post_id(category_name):
    """특정 카테고리의 마지막으로 크롤링된 게시글 ID를 가져옵니다."""
    conn = get_db_connection()
    if not conn:
        return 0
        
    cursor = conn.cursor()
    query = "SELECT last_post_id FROM last_crawled_info WHERE category_name = %s"
    cursor.execute(query, (category_name,))
    result = cursor.fetchone()
    cursor.close()
    conn.close()
    return result[0] if result else 0

def update_last_crawled_post_id(category_name, last_post_id):
    """마지막으로 크롤링된 게시글 ID를 업데이트합니다."""
    conn = get_db_connection()
    if not conn:
        return

    cursor = conn.cursor()
    query = """
        INSERT INTO last_crawled_info (category_name, last_post_id, last_crawled_at)
        VALUES (%s, %s, %s)
        ON DUPLICATE KEY UPDATE last_post_id = VALUES(last_post_id), last_crawled_at = VALUES(last_crawled_at)
    """
    cursor.execute(query, (category_name, last_post_id, datetime.datetime.now()))
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
        return "NEWS" # 기본값

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