# -*- coding: utf-8 -*-
import sqlite3
from src.config import DB_NAME
import datetime

def init_db():
    conn = sqlite3.connect(DB_NAME)
    cursor = conn.cursor()
    # articles 테이블 생성 및 컬럼 검증/추가
    cursor.execute('''
        CREATE TABLE IF NOT EXISTS articles (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            category TEXT,
            title TEXT,
            link TEXT UNIQUE,
            department TEXT,
            date TEXT,
            content TEXT,
            crawled_at DATETIME DEFAULT CURRENT_TIMESTAMP,
            views TEXT,
            attachment TEXT
        )
    ''')
    
    # last_crawled_info 테이블 생성
    cursor.execute('''
        CREATE TABLE IF NOT EXISTS last_crawled_info (
            category_name TEXT PRIMARY KEY,
            last_post_id INTEGER,
            last_crawled_at DATETIME DEFAULT CURRENT_TIMESTAMP
        )
    ''')

    conn.commit()
    conn.close()
    print(f"데이터베이스 '{DB_NAME}' 초기화 및 테이블 생성/업데이트 완료.")

def save_to_db(data, category_name):
    conn = sqlite3.connect(DB_NAME)
    cursor = conn.cursor()
    new_posts_count = 0
    for item in data:
        try:
            cursor.execute('''
                INSERT INTO articles (category, title, link, department, date, content, views, attachment)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?) 
            ''', (
                category_name,
                item.get('title'),
                item.get('link'),
                item.get('department'),
                item.get('date'),
                item.get('content'),
                item.get('views'),
                item.get('attachment')
            ))
            new_posts_count += 1
        except sqlite3.IntegrityError:
            pass # 중복 링크는 건너뛰기
        except Exception as e:
            print(f"DEBUG: 데이터 삽입 중 오류 발생: {e} - {item.get('link')}")
    conn.commit()
    conn.close()
    print(f"{new_posts_count}개의 새로운 게시글을 데이터베이스에 저장 완료. (카테고리: {category_name})")

def get_last_crawled_post_id(category_name):
    conn = sqlite3.connect(DB_NAME)
    cursor = conn.cursor()
    cursor.execute('SELECT last_post_id FROM last_crawled_info WHERE category_name = ?', (category_name,))
    result = cursor.fetchone()
    conn.close()
    return result[0] if result else 0

def update_last_crawled_post_id(category_name, last_post_id):
    conn = sqlite3.connect(DB_NAME)
    cursor = conn.cursor()
    cursor.execute('''
        INSERT OR REPLACE INTO last_crawled_info (category_name, last_post_id, last_crawled_at)
        VALUES (?, ?, ?)
    ''', (category_name, last_post_id, datetime.datetime.now()))
    conn.commit()
    conn.close()