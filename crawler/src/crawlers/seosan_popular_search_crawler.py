# -*- coding: utf-8 -*-
import logging
import re
import requests
from bs4 import BeautifulSoup
import chardet
from requests.adapters import HTTPAdapter
from urllib3.util.retry import Retry
from src.crawler_config import DEFAULT_TIMEOUT

SEARCH_URL = "https://search.seosan.go.kr/RSA/front/Search.jsp"

# 순번/불릿 제거용: "1", "10.", "1)" 등 앞자리 패턴 제거
LEADING_RANK_RE = re.compile(r"^\s*(?:\d{1,2}[\.\)]?\s*)")

def build_session() -> requests.Session:
    s = requests.Session()
    s.headers.update({
        "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
                      "AppleWebKit/537.36 (KHTML, like Gecko) "
                      "Chrome/126.0.0.0 Safari/537.36",
        "Accept-Language": "ko-KR,ko;q=0.9,en-US;q=0.8,en;q=0.7",
        "Referer": "https://search.seosan.go.kr/",
    })
    retry = Retry(
        total=3,
        backoff_factor=0.5,
        status_forcelist=[429, 500, 502, 503, 504],
        allowed_methods=["GET"],
        raise_on_status=False,
    )
    adapter = HTTPAdapter(max_retries=retry)
    s.mount("http://", adapter)
    s.mount("https://", adapter)
    return s

session = build_session()

def _detect_and_set_encoding(res: requests.Response) -> None:
    detected = chardet.detect(res.content)
    enc = (detected.get("encoding") or "").lower()
    if not enc:
        res.encoding = "utf-8"
        return
    # euc-kr 계열 정규화
    if enc in ["euc-kr", "cp949", "ks_c_5601-1987", "x-windows-949"]:
        res.encoding = "cp949"
    else:
        res.encoding = enc

def _extract_terms(container) -> list[str]:
    terms = []
    if not container:
        return terms
    # 다양성 확보: li > a, a.rank, span.txt 등 흔한 패턴 폴백
    candidates = container.select("li a, li span, a, span")
    for node in candidates:
        text = node.get_text(strip=True)
        if not text:
            continue
        # 앞자리 순번/불릿 제거
        text = LEADING_RANK_RE.sub("", text)
        if text and text not in terms:
            terms.append(text)
    return terms

def crawl_popular_search_terms() -> dict:
    url = SEARCH_URL
    popular_terms = {"daily": [], "weekly": []}

    try:
        res = session.get(url, timeout=DEFAULT_TIMEOUT)
        res.raise_for_status()
        _detect_and_set_encoding(res)
        html = res.text
        soup = BeautifulSoup(html, "lxml")

        # 기본 셀렉터
        daily = soup.select_one("#tab1c1")
        weekly = soup.select_one("#tab1c2")

        # 폴백: 탭 컨텐츠 컨테이너가 바뀌는 경우 대비
        if not daily:
            daily = soup.select_one("#tab1c1, #tabDaily, .tab_daily, [data-tab='daily']")
        if not weekly:
            weekly = soup.select_one("#tab1c2, #tabWeekly, .tab_weekly, [data-tab='weekly']")

        popular_terms["daily"] = _extract_terms(daily)
        popular_terms["weekly"] = _extract_terms(weekly)

        if not popular_terms["daily"] and not popular_terms["weekly"]:
            logging.warning("인기 검색어 섹션을 찾지 못했거나 항목이 비어 있습니다.")

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
