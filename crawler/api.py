import logging
from dotenv import load_dotenv

# .env 파일에서 환경 변수 로드
load_dotenv()

from flask import Flask, request, jsonify
from src.crawlers.seosan_city_crawler import crawl_all_pages
from src.crawlers.seosan_popular_search_crawler import crawl_popular_search_terms
from src.database import init_db, get_db_connection
from src.crawler_config import CRAWL_CONFIGS # CRAWL_CONFIGS 임포트
from bart import summarize_text  # Import summarize_text function

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


@app.route('/crawl_all', methods=['GET'])
def crawl_all():
    """모든 카테고리를 순차적으로 크롤링합니다."""
    try:
        summary = []
        total_new = 0
        for cfg in CRAWL_CONFIGS:
            category_name = cfg.get('category_name')
            base_url = cfg.get('base_url')
            if not category_name or not base_url:
                continue
            posts = crawl_all_pages(category_name, base_url)
            if posts:
                from src.database import save_to_db
                save_to_db(posts, category_name)
            summary.append({"category": category_name, "new": len(posts)})
            total_new += len(posts)
        return jsonify({"ok": True, "total_new": total_new, "summary": summary}), 200
    except Exception as e:
        logging.error(f"Error during crawl_all: {e}", exc_info=True)
        return jsonify({"ok": False, "error": "An error occurred during crawl_all."}), 500

@app.route('/summarize', methods=['POST'])
def summarize():
    """
    요약 API 엔드포인트. POST 요청으로 텍스트를 받아 요약된 결과를 반환합니다.
    """
    try:
        data = request.get_json()
        text = data.get('text', '')

        if not text:
            return jsonify({"error": "No text provided"}), 400

        # Summarize the text using the function from bart.py
        summary = summarize_text(text)

        return jsonify({"summary": summary}), 200
    except Exception as e:
        logging.error(f"Error during summarization: {e}", exc_info=True)
        return jsonify({"error": "An error occurred during summarization."}), 500

@app.route('/crawl_popular_terms', methods=['GET'])
def get_popular_terms():
    """
    인기 검색어(일간, 주간)를 크롤링하여 반환하는 API 엔드포인트.
    """
    try:
        popular_terms = crawl_popular_search_terms()
        logging.info(f"Crawled popular search terms: {len(popular_terms['daily'])} daily, {len(popular_terms['weekly'])} weekly.")
        return jsonify(popular_terms), 200
    except Exception as e:
        logging.error(f"Error during crawling popular terms: {e}", exc_info=True)
        return jsonify({"error": "An error occurred during crawling popular terms."}), 500

if __name__ == "__main__":
    app.run(host="0.0.0.0", port=5000, debug=True)