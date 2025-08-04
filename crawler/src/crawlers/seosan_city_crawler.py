# -*- coding: utf-8 -*-
from bs4 import BeautifulSoup, Comment
import requests
import re
from urllib.parse import urljoin
from concurrent.futures import ThreadPoolExecutor
from src.config import HEADERS, MAX_CRAWL_PAGES
from src.database import get_last_crawled_post_id, update_last_crawled_post_id

# requests.Session() 생성 (전역 세션으로 관리)
session = requests.Session()
session.headers.update(HEADERS)

def parse_detail_page(url):
    try:
        res = session.get(url, timeout=(10, 20))
        res.raise_for_status()
        soup = BeautifulSoup(res.text, "lxml")
        content_el = soup.select_one("#bbs_content, .bbs_content, td.bbs_content")
        if not content_el:
            return ""
        for c in content_el.find_all(string=lambda t: isinstance(t, Comment)):
            c.extract()
        text = content_el.get_text("\n").strip()
        return text
    except requests.exceptions.RequestException:
        return ""
    except Exception:
        return ""

def get_post_info(page, base_url, category_name):
    url = f"{base_url}{page}"
    res = session.get(url, timeout=(10, 20))
    soup = BeautifulSoup(res.text, 'lxml')

    posts_on_page = []
    links_to_fetch = []

    for tr in soup.select('tbody > tr'):
        tds = tr.select('td')
        post_data = {}

        try:
            post_id = int(tds[0].text.strip())
        except ValueError:
            post_id = 0

        title_tag = tds[1].select_one('a')
        title = title_tag.text.strip()
        link = urljoin(base_url, title_tag['href'])

        post_data['id'] = post_id
        post_data['title'] = title
        post_data['link'] = link
        post_data['content'] = ''

        if category_name.startswith("복지정보"):
            # 복지정보 게시판: 파일, 조회수, 작성일
            post_data['attachment'] = 'Y' if tds[2].select_one('img') else 'N'
            post_data['views'] = tds[3].text.strip()
            post_data['date'] = tds[4].text.strip()
        else:
            # 그 외 게시판: 담당부서, 작성일
            post_data['department'] = tds[2].text.strip()
            post_data['date'] = tds[3].text.strip()

        posts_on_page.append(post_data)
        links_to_fetch.append(link)

    with ThreadPoolExecutor(max_workers=3) as executor:
        contents = list(executor.map(parse_detail_page, links_to_fetch))

    for i, content in enumerate(contents):
        posts_on_page[i]['content'] = content

    return posts_on_page

def crawl_all_pages(category_name, base_url):
    all_posts = []
    total_pages = 1
    new_posts_found = False
    # max_crawled_id = get_last_crawled_post_id(category_name) # 이 줄을 주석 처리
    max_crawled_id = 0 # 모든 게시글을 가져오기 위해 0으로 설정

    print(f'--- {category_name} 크롤링 시작 ---')
    print("첫 페이지에서 전체 페이지 수 파악 중...")
    first_page_url = f"{base_url}1"
    res = session.get(first_page_url, timeout=(10, 20))
    soup = BeautifulSoup(res.text, 'lxml')

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
    
    pages_to_crawl = min(total_pages, MAX_CRAWL_PAGES)
    print(f"총 {total_pages} 페이지 중 {pages_to_crawl} 페이지를 크롤링합니다.")

    current_max_id_in_run = max_crawled_id # 이번 실행에서 발견된 가장 큰 ID

    for page in range(1, pages_to_crawl + 1):
        print(f"{page} 페이지 크롤링 중... ({category_name})")
        posts_in_page = get_post_info(page, base_url, category_name)
        
        # found_old_post = False # 이 줄을 주석 처리
        for post in posts_in_page:
            # if post['id'] > max_crawled_id: # 이 조건문을 주석 처리
            all_posts.append(post)
            new_posts_found = True
            if post['id'] > current_max_id_in_run:
                current_max_id_in_run = post['id']
            # else: # 이 줄을 주석 처리
            #     found_old_post = True # 이 줄을 주석 처리
            #     break # 이미 크롤링된 게시글을 만나면 중단 # 이 줄을 주석 처리
        
        # if found_old_post: # 이 줄을 주석 처리
        #     print(f"기존 게시글을 발견하여 {page} 페이지에서 크롤링을 중단합니다.") # 이 줄을 주석 처리
        #     break # 이 줄을 주석 처리

    if new_posts_found:
        update_last_crawled_post_id(category_name, current_max_id_in_run)
        print(f"새로운 게시글 {len(all_posts)}개 발견 및 마지막 게시글 ID 업데이트: {current_max_id_in_run}")
    else:
        print("새로운 게시글이 없습니다.")

    return all_posts