import logging
import os
import re
from concurrent.futures import ThreadPoolExecutor, as_completed
from typing import List, Dict, Optional
from urllib.parse import urljoin

import requests
from bs4 import BeautifulSoup, Comment
from requests.adapters import HTTPAdapter
from urllib3.util.retry import Retry

from src.crawler_config import HEADERS, MAX_CRAWL_PAGES
from src.database import get_last_crawled_link, update_last_crawled_link
from src.regions import REGIONS

# -----------------------------
# 컴파일된 패턴 (성능/안정성)
# -----------------------------
DATE_PATTERN = re.compile(r"\d{4}\.\d{2}\.\d{2}")  # YYYY.MM.DD
# 12,345 형태도 허용 (콤마/공백 제거 후 int 변환)
VIEWS_PATTERN = re.compile(r"^\d{1,3}(?:,\d{3})*$")

# -----------------------------
# requests 세션 (재사용)
# -----------------------------
session = requests.Session()
session.headers.update(HEADERS)

# urllib3 v2 기준: allowed_methods 는 set/frozenset 권장
retry = Retry(
    total=3,
    backoff_factor=0.5,
    status_forcelist=[429, 500, 502, 503, 504],
    allowed_methods={"GET"},
)
adapter = HTTPAdapter(max_retries=retry)
session.mount("http://", adapter)
session.mount("https://", adapter)

DEFAULT_TIMEOUT = (10, 10)  # (connect, read)

DETAIL_WORKERS = int(os.getenv("DETAIL_WORKERS", "3"))  # 기본값 3
DETAIL_TIMEOUT = float(os.getenv("DETAIL_TIMEOUT", "10.0"))  # 각 상세 요청 한도(초)


def _find_specific_region(title: str, content: str) -> Optional[str]:
    """제목/본문에서 REGIONS 중 가장 먼저 일치하는 지역명을 반환."""
    haystack = f"{title} {content}"
    for region in REGIONS:
        if region and region in haystack:
            return region
    return None


def _parse_detail_page(url: str) -> str:
    """상세 페이지에서 본문 텍스트만 추출."""
    try:
        res = session.get(url, timeout=DEFAULT_TIMEOUT)
        res.raise_for_status()
        # 인코딩 자동 감지 반영
        res.encoding = res.apparent_encoding
        soup = BeautifulSoup(res.text, "lxml")

        content_el = soup.select_one("#bbs_content, .bbs_content, td.bbs_content")
        if not content_el:
            logging.warning(f"[detail] content element not found: {url}")
            return ""

        # HTML 주석 제거
        for c in content_el.find_all(string=lambda t: isinstance(t, Comment)):
            c.extract()

        # 개행 구분자로 '\n'을 명시적으로 사용 (문자열 리터럴 개행 오타 수정)
        text = content_el.get_text("\n").strip()
        # 모든 공백류를 단일 공백으로
        text = re.sub(r"\s+", " ", text).strip()
        return text

    except requests.exceptions.RequestException as e:
        logging.error(f"[detail] RequestException {url}: {e}", exc_info=True)
        return ""
    except Exception as e:
        logging.error(f"[detail] Unexpected error {url}: {e}", exc_info=True)
        return ""


def _safe_int_from_views(raw: str) -> int:
    """'12,345' -> 12345; 숫자만 아니면 0."""
    s = raw.replace(",", "").strip()
    return int(s) if s.isdigit() else 0


def _extract_posts_from_list_page(page: int, base_url: str, category_name: str) -> List[Dict]:
    """목록 페이지에서 게시글 정보(제목/링크/부서/날짜/조회수/첨부)를 수집."""
    url = f"{base_url}{page}"
    try:
        res = session.get(url, timeout=DEFAULT_TIMEOUT)
        res.raise_for_status()
        res.encoding = res.apparent_encoding
        soup = BeautifulSoup(res.text, "lxml")
    except requests.exceptions.RequestException as e:
        logging.error(f"[list] RequestException page={page}, cat={category_name}: {e}", exc_info=True)
        return []
    except Exception as e:
        logging.error(f"[list] Unexpected error page={page}, cat={category_name}: {e}", exc_info=True)
        return []

    posts_on_page: List[Dict] = []
    links_to_fetch: List[str] = []

    for tr in soup.select("tbody > tr"):
        tds = tr.select("td")
        if not tds or len(tds) < 4:
            continue

        # id
        try:
            post_id = int(tds[0].get_text(strip=True))
        except (ValueError, IndexError):
            post_id = 0

        # title/link
        title_tag = tds[1].select_one("a")
        if not title_tag:
            logging.warning(f"[list] title tag missing: page={page}, cat={category_name}")
            continue

        title = title_tag.get_text(strip=True)
        link = urljoin(base_url, title_tag.get("href", "").strip())

        post_data: Dict = {
            "id": post_id,
            "title": title,
            "link": link,
            "content": "",
            "department": "",
            "date": "",
            "views": 0,
        }

        # '복지정보' 카테고리에서만 첨부 여부 표시
        has_attachment = "N"
        # tds[2:] 텍스트 추출
        td_texts = [td.get_text(strip=True) for td in tds[2:]]

        found_date = False
        found_views = False

        for i, text in enumerate(td_texts):
            if not found_date and DATE_PATTERN.match(text):
                post_data["date"] = text
                found_date = True
                continue

            if not found_views and (VIEWS_PATTERN.match(text) or text.isdigit()):
                post_data["views"] = _safe_int_from_views(text)
                found_views = True
                continue

            # 복지정보 첫 번째 셀에 첨부 아이콘(img) 있는지 체크
            if category_name.startswith("복지정보") and i == 0:
                if tds[2].select_one("img"):
                    has_attachment = "Y"

        # 부서(남는 텍스트들 결합)
        remaining: List[str] = []
        for text in td_texts:
            is_date = DATE_PATTERN.match(text)
            is_views = VIEWS_PATTERN.match(text) or text.isdigit()
            if not is_date and not is_views:
                remaining.append(text)

        if remaining:
            post_data["department"] = " ".join(remaining).strip()

        if category_name.startswith("복지정보"):
            post_data["attachment"] = has_attachment

        posts_on_page.append(post_data)
        links_to_fetch.append(link)

    # 상세 본문 병렬 수집
    if links_to_fetch:
        contents = [""] * len(links_to_fetch)
        with ThreadPoolExecutor(max_workers=DETAIL_WORKERS) as ex:
            future_to_idx = {ex.submit(_parse_detail_page, url): i for i, url in enumerate(links_to_fetch)}
            # as_completed에 전체 타임아웃을 주면 일부 미완료 Future에서 TimeoutError가 발생해
            # 남은 작업을 수집하지 못할 수 있음 → per-future timeout만 사용
            for fut in as_completed(future_to_idx):
                i = future_to_idx[fut]
                try:
                    contents[i] = fut.result(timeout=DETAIL_TIMEOUT)
                except Exception as e:
                    logging.warning(f"[detail] timeout/err on {links_to_fetch[i]}: {e}")

        for i, content in enumerate(contents):
            posts_on_page[i]["content"] = content
            posts_on_page[i]["specific_region"] = _find_specific_region(
                posts_on_page[i]["title"], content
            )

    return posts_on_page


def _infer_total_pages(soup: BeautifulSoup) -> int:
    """페이징 블록에서 최댓값 추정."""
    total_pages = 1
    links = soup.select(".pagination .page_wrap a")
    max_page = 0
    for a in links:
        href = a.get("href", "")
        m = re.search(r"pageIndex=(\d+)", href)
        if m:
            max_page = max(max_page, int(m.group(1)))

    next_end = soup.select_one(".pagination .next_end")
    if next_end:
        href = next_end.get("href", "")
        m = re.search(r"pageIndex=(\d+)", href)
        if m:
            max_page = max(max_page, int(m.group(1)))

    if max_page > 0:
        total_pages = max_page
    return total_pages


def crawl_all_pages(config: Dict) -> List[Dict]:
    """
    카테고리 설정(config)을 받아 새 게시글 목록을 반환.
    DB의 last_crawled_link를 만나면 중단.
    """
    category_name: str = config.get("category_name")
    base_url: str = config.get("base_url")
    pages_to_crawl_limit: int = config.get("pages_to_crawl", MAX_CRAWL_PAGES)

    logging.debug(f"[crawl] base_url={base_url}")
    newly_crawled_posts: List[Dict] = []

    last_crawled_link: Optional[str] = get_last_crawled_link(category_name)

    logging.info(f"--- {category_name} 크롤링 시작 ---")
    logging.info("첫 페이지에서 전체 페이지 수 파악 중...")

    first_page_url = f"{base_url}1"
    try:
        res = session.get(first_page_url, timeout=DEFAULT_TIMEOUT)
        res.raise_for_status()
        res.encoding = res.apparent_encoding
        soup = BeautifulSoup(res.text, "lxml")
    except requests.exceptions.RequestException as e:
        logging.error(f"[crawl] RequestException first page {category_name}: {e}", exc_info=True)
        return []
    except Exception as e:
        logging.error(f"[crawl] Unexpected error first page {category_name}: {e}", exc_info=True)
        return []

    total_pages = _infer_total_pages(soup)
    pages_to_crawl = min(total_pages, pages_to_crawl_limit)
    logging.info(f"총 {total_pages} 페이지 중 {pages_to_crawl} 페이지를 크롤링합니다.")

    for page in range(1, pages_to_crawl + 1):
        logging.info(f"{page} 페이지 크롤링 중... ({category_name})")

        posts_in_page = _extract_posts_from_list_page(page, base_url, category_name)
        found_last = False

        for post in posts_in_page:
            if last_crawled_link and post["link"] == last_crawled_link:
                logging.info(f"이전 마지막 링크 발견: {last_crawled_link} → 중단")
                found_last = True
                break
            newly_crawled_posts.append(post)

        if found_last:
            break

    if newly_crawled_posts:
        # 가장 최신(목록 1페이지 상단 가정) 링크로 갱신
        update_last_crawled_link(category_name, newly_crawled_posts[0]["link"])
        logging.info(f"새 게시글 {len(newly_crawled_posts)}개 발견 및 최신 링크 갱신 완료.")
    else:
        logging.info("새로운 게시글이 없습니다.")

    return newly_crawled_posts
