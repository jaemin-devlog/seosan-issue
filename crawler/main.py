# -*- coding: utf-8 -*-
import pandas as pd
import sys
from src.database import init_db, save_to_db
from src.crawlers.seosan_city_crawler import crawl_all_pages
from src.config import CRAWL_CONFIGS

def main(target_category=None):
    init_db()
    
    all_crawled_data = []
    configs_to_crawl = []

    if target_category:
        # 특정 카테고리만 선택
        for config in CRAWL_CONFIGS:
            if config["category_name"] == target_category:
                configs_to_crawl.append(config)
                break
        if not configs_to_crawl:
            print(f"오류: '{target_category}' 카테고리를 찾을 수 없습니다.")
            print(f"사용 가능한 카테고리: {[c['category_name'] for c in CRAWL_CONFIGS]}")
            return
    else:
        # 모든 카테고리 크롤링
        configs_to_crawl = CRAWL_CONFIGS

    for config in configs_to_crawl:
        category_name = config["category_name"]
        base_url = config["base_url"]
        
        crawled_data = crawl_all_pages(category_name, base_url)
        all_crawled_data.extend(crawled_data)
        
        # 각 카테고리별로 DB에 저장
        save_to_db(crawled_data, category_name)

    df = pd.DataFrame(all_crawled_data)
    
    if df.empty:
        print("수집된 행이 없습니다. 엑셀 저장 생략")
    else:
        output_filename = f"seosan_city_{target_category}.xlsx" if target_category else "seosan_city_total.xlsx"
        print("\n--- 전체 크롤링 결과 ---")
        # print(df.head()) # 인코딩 오류 방지를 위해 주석 처리
        df.to_excel(output_filename, index=False, engine="openpyxl")
        print(f"\n크롤링 완료! {output_filename} 파일로 저장되었습니다.")

    print("\n모든 작업 완료!")

if __name__ == '__main__':
    if len(sys.argv) > 1:
        # 커맨드 라인에서 인자로 받은 카테고리만 실행
        main(target_category=sys.argv[1])
    else:
        # 인자가 없으면 모든 카테고리 실행
        main()
