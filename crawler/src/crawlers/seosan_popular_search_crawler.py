# -*- coding: utf-8 -*-
import requests
from bs4 import BeautifulSoup
import logging
import chardet  # pip install chardet

session = requests.Session()
session.headers.update({
    "User-Agent": "Mozixlla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36",
    "Accept-Language": "ko-KR,ko;q=0.9,en-US;q=0.8,en;q=0.7",
})

def crawl_popular_search_terms():
    url = "https://search.seosan.go.kr/RSA/front/Search.jsp"
    popular_terms = {"daily": [], "weekly": []}

    try:
        res = session.get(url, timeout=(10, 20))
        res.raise_for_status()

        # 인코딩 자동 감지
        detected = chardet.detect(res.content)
        res.encoding = detected["encoding"] or "utf-8"

        # 간혹 euc-kr이 cp949로 나올 수 있음
        if res.encoding.lower() in ["euc-kr", "cp949"]:
            res.encoding = "cp949"

        soup = BeautifulSoup(res.text, "lxml")

        daily_ul = soup.select_one("#tab1c1")
        if daily_ul:
            for li in daily_ul.select("li"):
                a_tag = li.select_one("a")
                if a_tag:
                    popular_terms["daily"].append(a_tag.get_text(strip=True))

        weekly_ul = soup.select_one("#tab1c2")
        if weekly_ul:
            for li in weekly_ul.select("li"):
                a_tag = li.select_one("a")
                if a_tag:
                    popular_terms["weekly"].append(a_tag.get_text(strip=True))

    except requests.exceptions.RequestException as e:
        logging.error(f"RequestException: {e}", exc_info=True)
    except Exception as e:
        logging.error(f"Unexpected error: {e}", exc_info=True)

    return popular_terms

if __name__ == "__main__":
    logging.basicConfig(level=logging.INFO, format="%(asctime)s - %(levelname)s - %(message)s")
    terms = crawl_popular_search_terms()
    print("일간 인기 검색어:", terms["daily"])
    print("주간 인기 검색어:", terms["weekly"])
