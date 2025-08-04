from flask import Flask, request, jsonify
from src.crawlers.seosan_city_crawler import crawl_all_pages
import json

app = Flask(__name__)

# JSON 응답이 UTF-8을 사용하도록 설정
app.config['JSON_AS_ASCII'] = False

@app.route('/crawl', methods=['GET'])
def start_crawling():
    """
    웹 크롤링을 실행하고 결과를 JSON으로 반환하는 API 엔드포인트.
    Query Parameters:
        - category (str): 크롤링할 게시판의 카테고리 이름.
        - url (str): 크롤링할 게시판의 기본 URL.
    """
    category = request.args.get('category')
    base_url = request.args.get('url')

    if not category or not base_url:
        return jsonify({"error": "Missing required query parameters: 'category' and 'url'"}), 400

    try:
        new_posts = crawl_all_pages(category, base_url)
        return jsonify({
            "category": category,
            "post_count": len(new_posts),
            "posts": new_posts
        }), 200
    except Exception as e:
        # 실제 운영 환경에서는 로깅 라이브러리를 사용해 에러를 기록하는 것이 좋습니다.
        print(f"Error during crawling: {e}")
        return jsonify({"error": "An error occurred during crawling."}), 500
