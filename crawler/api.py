import logging
import threading
from flask import Flask, request, jsonify
from bart import summarize_text, load_model, MODEL_READY
from src.crawlers.seosan_city_crawler import crawl_all_pages
from src.crawlers.seosan_popular_search_crawler import crawl_popular_search_terms
from src.database import init_db, get_db_connection
from src.crawler_config import CRAWL_CONFIGS

app = Flask(__name__)
app.config['JSON_AS_ASCII'] = False
logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(levelname)s - %(message)s')

@app.before_first_request
def initial_setup():
    """Before the first request, initialize the database and start model warmup."""
    app.logger.info("첫 요청 수신, 초기 설정 및 모델 워밍업을 시작합니다.")
    init_db()
    threading.Thread(target=load_model, daemon=True).start()

@app.route("/healthz")
def healthz():
    """Health check endpoint to report model readiness."""
    return jsonify({"ok": True, "model_ready": MODEL_READY})

@app.route('/crawl_all', methods=['GET'])
def crawl_all():
    logging.info("'/crawl_all' 엔드포인트 요청 수신")
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
    data = request.get_json()
    text = data.get('text', '')
    if not text:
        return jsonify({"error": "No text provided"}), 400
    
    summary = summarize_text(text)
    return jsonify({"summary": summary}), 200

@app.route('/crawl_popular_terms', methods=['GET'])
def get_popular_terms():
    popular_terms = crawl_popular_search_terms()
    return jsonify(popular_terms), 200

@app.route('/reset', methods=['POST'])
def reset_crawl_history():
    conn = get_db_connection()
    if not conn:
        return jsonify({"error": "Database connection failed."}), 500
    cursor = conn.cursor()
    cursor.execute("DELETE FROM last_crawled_info")
    conn.commit()
    deleted_rows = cursor.rowcount
    cursor.close()
    conn.close()
    return jsonify({"message": f"RESET SUCCESSFUL. Deleted {deleted_rows} rows."}), 200
