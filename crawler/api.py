import logging
from flask import Flask, request, jsonify
from src.crawlers.seosan_city_crawler import crawl_all_pages
from src.database import init_db, get_db_connection
from src.crawler_config import CRAWL_CONFIGS # CRAWL_CONFIGS 임포트
import json

app = Flask(__name__)

# JSON 응답이 UTF-8을 사용하도록 설정
app.config['JSON_AS_ASCII'] = False

# 로깅 설정
logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(levelname)s - %(message)s')

# 애플리케이션 시작 시 데이터베이스 초기화
with app.app_context():
    init_db()

@app.route('/reset', methods=['POST'])
def reset_crawl_history():
    """
    `last_crawled_info` 테이블의 모든 데이터를 삭제하여 크롤링 기록을 초기화합니다.
    """
    try:
        conn = get_db_connection()
        if not conn:
            return jsonify({"error": "Database connection failed."}), 500
        
        cursor = conn.cursor()
        cursor.execute("DELETE FROM last_crawled_info")
        conn.commit()
        
        deleted_rows = cursor.rowcount
        
        cursor.close()
        conn.close()
        
        logging.info(f"RESET SUCCESSFUL: {deleted_rows} rows deleted from last_crawled_info.")
        return jsonify({"message": f"RESET SUCCESSFUL. Deleted {deleted_rows} rows."}), 200
    except Exception as e:
        logging.error(f"Error while resetting crawl history: {e}", exc_info=True)
        return jsonify({"error": "An error occurred while resetting crawl history."}), 500

@app.route('/crawl', methods=['GET'])
def start_crawling():
    """
    웹 크롤링을 실행하고 결과를 JSON으로 반환하는 API 엔드포인트.
    Query Parameters:
        - category (str): 크롤링할 게시판의 카테고리 이름.
    """
    category_name = request.args.get('category')

    if not category_name:
        logging.warning("Missing required query parameter: 'category'")
        return jsonify({"error": "Missing required query parameter: 'category'"}), 400

    base_url = None
    for config in CRAWL_CONFIGS:
        if config['category_name'] == category_name:
            base_url = config['base_url']
            break

    if not base_url:
        logging.warning(f"Category '{category_name}' not found in CRAWL_CONFIGS.")
        return jsonify({"error": f"Category '{category_name}' not found."}), 404

    try:
        new_posts = crawl_all_pages(category_name, base_url)
        if new_posts:
            from src.database import save_to_db # save_to_db 임포트
            save_to_db(new_posts, category_name)
            logging.info(f"총 {len(new_posts)}개의 새로운 게시글을 데이터베이스에 저장 완료. (카테고리: {category_name})")
        else:
            logging.info(f"새로운 게시글이 없습니다. (카테고리: {category_name})")
        logging.info(f"Crawling completed for category: {category_name}, found {len(new_posts)} posts.")
        return jsonify({
            "category": category_name,
            "post_count": len(new_posts),
            "posts": new_posts
        }), 200
    except Exception as e:
        logging.error(f"Error during crawling for category {category_name}: {e}", exc_info=True)
        return jsonify({"error": "An error occurred during crawling."}), 500
