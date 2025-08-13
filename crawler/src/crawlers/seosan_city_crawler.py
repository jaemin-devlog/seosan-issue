# -*- coding: utf-8 -*
from bs4 import BeautifulSoup, Comment
import requests
import re
from urllib.parse import urljoin
from concurrent.futures import ThreadPoolExecutor
from src.crawler_config import HEADERS, MAX_CRAWL_PAGES
from src.database import get_last_crawled_link, update_last_crawled_link
import logging
from src.regions import REGIONS # Added this line

def find_specific_region(title, content):
    """
    제목과 내용에서 특정 지역명을 찾아 반환합니다.
    가장 먼저 발견되는 지역명을 반환합니다.
    """
    text_to_search = title + " " + content
    for region in REGIONS:
        # '동문1동' 같은 경우 '동문'만 검색하면 안되므로 정확한 지역명으로 검색
        if region in text_to_search:
            return region
    return None # 특정 지역을 찾지 못한 경우

# requests.Session() 생성 (전역 세션으로 관리)
session = requests.Session()
session.headers.update(HEADERS)

def parse_detail_page(url):
    try:
        res = session.get(url, timeout=(10, 20))
        res.raise_for_status()
        res.encoding = res.apparent_encoding  # 인코딩 자동 감지 사용
        soup = BeautifulSoup(res.text, "lxml")
        
        content_el = soup.select_one("#bbs_content, .bbs_content, td.bbs_content")
        if not content_el:
            logging.warning(f"Content element not found for URL: {url}")
            return ""

        for c in content_el.find_all(string=lambda t: isinstance(t, Comment)):
            c.extract()

        text = content_el.get_text("\n").strip()
        text = re.sub(r'\s+', ' ', text).strip() # 모든 공백 문자를 하나의 공백으로 줄이고 양쪽 공백 제거
        text = text.replace('. ', '.\n') # 마침표 뒤에 공백이 오는 경우 줄바꿈 처리

        return text
    except requests.exceptions.RequestException as e:
        logging.error(f"RequestException while parsing detail page {url}: {e}", exc_info=True)
        return ""
    except Exception as e:
        logging.error(f"Unexpected error while parsing detail page {url}: {e}", exc_info=True)
        return ""

def get_post_info(page, base_url, category_name):
    url = f"{base_url}{page}"
    try:
        res = session.get(url, timeout=(10, 20))
        res.raise_for_status()
        res.encoding = res.apparent_encoding # 인코딩 자동 감지 사용
        soup = BeautifulSoup(res.text, 'lxml')
    except requests.exceptions.RequestException as e:
        logging.error(f"RequestException while fetching post info for page {page}, category {category_name}: {e}", exc_info=True)
        return []
    except Exception as e:
        logging.error(f"Unexpected error while fetching post info for page {page}, category {category_name}: {e}", exc_info=True)
        return []

    posts_on_page = []
    links_to_fetch = []

    for tr in soup.select('tbody > tr'):
        tds = tr.select('td')
        post_data = {}

        try:
            post_id = int(tds[0].text.strip())
        except (ValueError, IndexError):
            post_id = 0
        
        if not tds or len(tds) < 4:
            continue

        title_tag = tds[1].select_one('a')
        if not title_tag:
            logging.warning(f"Title tag not found for a row on page {page}, category {category_name}")
            continue
        
        title = title_tag.text.strip()
        link = urljoin(base_url, title_tag['href'])

        post_data['id'] = post_id
        post_data['title'] = title
        post_data['link'] = link
        post_data['content'] = ''

        if category_name.startswith("복지정보"):
            post_data['attachment'] = 'Y' if tds[2].select_one('img') else 'N'
            post_data['views'] = tds[3].text.strip()
            post_data['date'] = tds[4].text.strip()
        else:
            post_data['department'] = tds[2].text.strip()
            post_data['date'] = tds[3].text.strip()

        posts_on_page.append(post_data)
        links_to_fetch.append(link)

    with ThreadPoolExecutor(max_workers=5) as executor:
        contents = list(executor.map(parse_detail_page, links_to_fetch))

    for i, content in enumerate(contents):
        posts_on_page[i]['content'] = content
        posts_on_page[i]['specific_region'] = find_specific_region(posts_on_page[i]['title'], content)

    return posts_on_page

def crawl_all_pages(config):
    category_name = config.get('category_name')
    base_url = config.get('base_url')
    pages_to_crawl_limit = config.get('pages_to_crawl', MAX_CRAWL_PAGES)
    logging.debug(f"crawl_all_pages received base_url: {base_url}")
    newly_crawled_posts = []
    
    last_crawled_link = get_last_crawled_link(category_name) # 데이터베이스에서 마지막 크롤링 링크 가져오기

    logging.info(f'--- {category_name} 크롤링 시작 ---')
    logging.info("첫 페이지에서 전체 페이지 수 파악 중...")
    first_page_url = f"{base_url}1"
    
    try:
        res = session.get(first_page_url, timeout=(10, 20))
        res.raise_for_status()
        res.encoding = res.apparent_encoding # 인코딩 자동 감지 사용
        soup = BeautifulSoup(res.text, 'lxml')
    except requests.exceptions.RequestException as e:
        logging.error(f"RequestException while fetching first page for {category_name}: {e}", exc_info=True)
        return []
    except Exception as e:
        logging.error(f"Unexpected error while fetching first page for {category_name}: {e}", exc_info=True)
        return []

    total_pages = 1
    pagination_links = soup.select('.pagination .page_wrap a')
    if pagination_links:
        max_page_from_links = 0
        for link_tag in pagination_links:
            href = link_tag.get('href', '')
            match = re.search(r'pageIndex=(\d+)', href)
            if match:
                max_page_from_links = max(max_page_from_links, int(match.group(1)))
        
        next_end_link = soup.select_one('.pagination .next_end')
        if next_end_link:
            href = next_end_link.get('href', '')
            match = re.search(r'pageIndex=(\d+)', href)
            if match:
                max_page_from_links = max(max_page_from_links, int(match.group(1)))

        if max_page_from_links > 0:
            total_pages = max_page_from_links
    
    pages_to_crawl = min(total_pages, pages_to_crawl_limit)
    logging.info(f"총 {total_pages} 페이지 중 {pages_to_crawl} 페이지를 크롤링합니다.")

    for page in range(1, pages_to_crawl + 1):
        logging.info(f"{page} 페이지 크롤링 중... ({category_name})")
        posts_in_page = get_post_info(page, base_url, category_name)
        
        found_last_crawled = False
        for post in posts_in_page:
            if last_crawled_link and post['link'] == last_crawled_link:
                logging.info(f"Found last crawled post ({last_crawled_link}), stopping crawling for {category_name}.")
                found_last_crawled = True
                break
            else: # 새로운 게시글인 경우에만 추가
                newly_crawled_posts.append(post)
        
        if found_last_crawled:
            break

    if newly_crawled_posts:
        update_last_crawled_link(category_name, newly_crawled_posts[0]['link'])
        logging.info(f"총 {len(newly_crawled_posts)}개의 새로운 게시글을 발견했습니다.")
    else:
        logging.info("새로운 게시글이 없습니다.")

    return newly_crawled_posts