import requests
import json
import sys
import os

from src.crawler_config import CRAWL_CONFIGS

output_filename = '../output.json'

# 데이터베이스 초기화 함수
def reset_db_crawl_history():
    reset_url = 'http://localhost:5001/reset'
    try:
        print(f"크롤링 기록을 {reset_url}을 통해 초기화 중...")
        response = requests.post(reset_url)
        response.raise_for_status()
        print(f"초기화 성공: {response.json().get('message')}")
    except requests.exceptions.RequestException as e:
        print(f"크롤링 기록 초기화 중 오류 발생: {e}")

# 모든 카테고리 크롤링 실행
if __name__ == "__main__":
    # 크롤링 시작 전 데이터베이스 초기화
    reset_db_crawl_history()

    all_crawled_data = {}

    for config in CRAWL_CONFIGS:
        category_name = config['category_name']
        crawl_url = 'http://localhost:5001/crawl'
        params = {'category': category_name}

        try:
            print(f"\n'{category_name}' 카테고리 데이터를 {crawl_url}에서 요청 중...")
            response = requests.get(crawl_url, params=params)
            response.raise_for_status()

            data = response.json()
            post_count = data.get('post_count', 0)
            all_crawled_data[category_name] = data

            print(f"'{category_name}' 크롤링 완료. {post_count}개의 게시글 발견.")

        except requests.exceptions.RequestException as e:
            print(f"'{category_name}' 카테고리 요청 중 오류 발생: {e}")
        except json.JSONDecodeError:
            print(f"'{category_name}' 카테고리 응답에서 JSON 디코딩 실패. 응답 텍스트:")
            print(response.text)
        except Exception as e:
            print(f"'{category_name}' 카테고리 처리 중 예상치 못한 오류 발생: {e}")

    # 모든 크롤링 데이터를 하나의 output.json 파일에 저장
    try:
        with open(output_filename, 'w', encoding='utf-8') as f:
            json.dump(all_crawled_data, f, ensure_ascii=False, indent=2)
        print(f"\n모든 크롤링 데이터가 {output_filename}에 저장되었습니다.")
    except Exception as e:
        print(f"모든 크롤링 데이터를 {output_filename}에 저장 중 오류 발생: {e}")