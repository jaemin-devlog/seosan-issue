import logging
from dotenv import load_dotenv

# .env 파일에서 환경 변수 로드
load_dotenv()

from flask import Flask, request, jsonify
from src.crawlers.seosan_city_crawler import crawl_all_pages
from src.crawlers.seosan_popular_search_crawler import crawl_popular_search_terms
from src.database import init_db, get_content_statistics
from src.crawler_config import CRAWL_CONFIGS
from bart import summarize_text
import os

app = Flask(__name__)
app.config['JSON_AS_ASCII'] = False

# 애플리케이션 시작 시 데이터베이스 초기화
with app.app_context():
    init_db()



@app.route('/health', methods=['GET'])
def health():
    return jsonify({"ok": True}), 200

@app.route('/crawl', methods=['GET'])
def start_crawling():
    category_name = request.args.get('category')
    if not category_name:
        return jsonify({"error": "Missing required query parameter: 'category'"}), 400

    matched_cfg = next((cfg for cfg in CRAWL_CONFIGS if cfg['category_name'] == category_name), None)
    if not matched_cfg:
        return jsonify({"error": f"Category '{category_name}' not found."}), 404

    try:
        pages_override = request.args.get("pages")
        pages_override_int = None
        if pages_override:
            try:
                pages_override_int = max(1, int(pages_override))
            except ValueError:
                pages_override_int = None

        crawl_config_for_this_run = matched_cfg.copy()
        if pages_override_int is not None:
            crawl_config_for_this_run["pages_to_crawl"] = pages_override_int

        new_posts = crawl_all_pages(crawl_config_for_this_run)
        if new_posts:
            from src.database import save_to_db
            save_to_db(new_posts, category_name)
        
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
    try:
        summary = []
        total_new = 0
        pages_override = request.args.get('pages')
        pages_override_int = None
        if pages_override:
            try:
                pages_override_int = max(1, int(pages_override))
            except ValueError:
                pages_override_int = None

        last_cat = None
        for cfg in CRAWL_CONFIGS:
            category_name = cfg.get('category_name')
            if not category_name:
                continue
            
            last_cat = category_name
            run_cfg = cfg.copy()
            if pages_override_int is not None:
                run_cfg["pages_to_crawl"] = pages_override_int

            posts = crawl_all_pages(run_cfg)
            if posts:
                from src.database import save_to_db
                save_to_db(posts, category_name)
            
            summary.append({"category": category_name, "new": len(posts)})
            total_new += len(posts)
            
        return jsonify({"ok": True, "total_new": total_new, "summary": summary}), 200
    except Exception as e:
        logging.error(f"crawl_all failed | last_category=%s | err=%s", last_cat, e, exc_info=True)
        return jsonify({"ok": False, "error": f"'{last_cat}' 크롤링 중 오류가 발생했습니다."}), 500

@app.route('/summarize', methods=['POST'])
def summarize():
    try:
        data = request.get_json()
        text = data.get('text', '')
        if not text:
            return jsonify({"error": "No text provided"}), 400
        summary = summarize_text(text)
        return jsonify({"summary": summary}), 200
    except Exception as e:
        logging.error(f"Error during summarization: {e}", exc_info=True)
        return jsonify({"error": "An error occurred during summarization."}), 500

@app.route('/crawl_popular_terms', methods=['GET'])
def get_popular_terms():
    try:
        popular_terms = crawl_popular_search_terms()
        return jsonify(popular_terms), 200
    except Exception as e:
        logging.error(f"Error during crawling popular terms: {e}", exc_info=True)
        return jsonify({"error": "An error occurred during crawling popular terms."}), 500

@app.route('/content_stats', methods=['GET'])
def get_content_stats():
    try:
        stats = get_content_statistics()
        if stats is None:
            return jsonify({"error": "Failed to retrieve content statistics."}), 500

        total_count = stats["total_count"]
        today_count = stats["today_count"]
        yesterday_count = stats["yesterday_count"]

        percentage_increase = 0.0
        if yesterday_count > 0:
            percentage_increase = ((today_count - yesterday_count) / yesterday_count) * 100
        elif today_count > 0:
            percentage_increase = 100.0

        response = {
            "total_content_count": total_count,
            "today_collected_count": today_count,
            "yesterday_collected_count": yesterday_count,
            "percentage_increase_from_yesterday": round(percentage_increase, 2)
        }
        return jsonify(response), 200
    except Exception as e:
        logging.error(f"Error getting content statistics: {e}", exc_info=True)
        return jsonify({"error": "An error occurred while fetching content statistics."}), 500

if __name__ == "__main__":
    debug_mode = os.environ.get('FLASK_ENV') == 'development'
    app.run(host="0.0.0.0", port=5000, debug=debug_mode)
