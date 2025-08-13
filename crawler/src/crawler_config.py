# -*- coding: utf-8 -*-


MAX_CRAWL_PAGES = 5  # 최대 크롤링 페이지 수 제한

# 요청 헤더
HEADERS = {
    "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36",
    "Accept-Language": "ko-KR,ko;q=0.9,en-US;q=0.8,en;q=0.7",
}

# 크롤링할 게시판 목록
CRAWL_CONFIGS = [
    # 서산시청
    {
        "category_name": "보건/건강",
        "base_url": "https://www.seosan.go.kr/www/selectBbsNttList.do?key=1379&bbsNo=45&pageUnit=10&searchCnd=all&searchKrwd=&integrDeptCode=&pageIndex="
    },
    {
        "category_name": "공지사항",
        "base_url": "https://www.seosan.go.kr/www/selectBbsNttList.do?bbsNo=97&key=1256&pageUnit=10&searchCnd=all&searchKrwd=&integrDeptCode=&pageIndex="
    },
    {
        "category_name": "보도자료",
        "base_url": "https://www.seosan.go.kr/www/selectBbsNttList.do?bbsNo=101&key=1260&pageUnit=10&searchCnd=all&searchKrwd=&integrDeptCode=&pageIndex="
    },
    # 복지정보
    {
        "category_name": "복지정보-어르신",
        "base_url": "https://www.seosan.go.kr/welfare/selectBbsNttList.do?key=2377&bbsNo=167&pageUnit=10&searchCnd=all&searchKrwd=&integrDeptCode=&pageIndex="
    },
    {
        "category_name": "복지정보-장애인",
        "base_url": "https://www.seosan.go.kr/welfare/selectBbsNttList.do?bbsNo=166&key=2378&pageUnit=10&searchCnd=all&searchKrwd=&integrDeptCode=&pageIndex="
    },
    {
        "category_name": "복지정보-여성가족",
        "base_url": "https://www.seosan.go.kr/welfare/selectBbsNttList.do?bbsNo=168&key=2379&pageUnit=10&searchCnd=all&searchKrwd=&integrDeptCode=&pageIndex="
    },
    {
        "category_name": "복지정보-아동청소년",
        "base_url": "https://www.seosan.go.kr/welfare/selectBbsNttList.do?bbsNo=51&key=2427&pageUnit=10&searchCnd=all&searchKrwd=&integrDeptCode=&pageIndex="
    },
    {
        "category_name": "복지정보-청년",
        "base_url": "https://www.seosan.go.kr/welfare/selectBbsNttList.do?bbsNo=2284&key=7634&pageUnit=10&searchCnd=all&searchKrwd=&integrDeptCode=&pageIndex="
    },
    # 문화관광
    {
        "category_name": "문화소식",
        "base_url": "https://www.seosan.go.kr/culture/selectBbsNttList.do?key=818&bbsNo=36&searchCtgry=&pageUnit=10&searchCnd=all&searchKrwd=&integrDeptCode=&pageIndex="
    },
    {
        "category_name": "시티투어",
        "base_url": "https://www.seosan.go.kr/tour/selectBbsNttList.do?bbsNo=23&key=971&pageUnit=10&searchCnd=all&searchKrwd=&integrDeptCode=&pageIndex="
    },
    {
        "category_name": "관광-안내",
        "base_url": "https://www.seosan.go.kr/tour/selectBbsNttList.do?bbsNo=24&key=976&pageUnit=10&searchCnd=all&searchKrwd=&integrDeptCode=&pageIndex="
    },

    {
        "category_name": "대산읍 공지사항",
        "base_url": "https://www.seosan.go.kr/emd/selectBbsNttList.do?bbsNo=178&key=1694&pageIndex=",
        "pages_to_crawl": 5
    },
    {
        "category_name": "인지면 공지사항",
        "base_url": "https://www.seosan.go.kr/emd/selectBbsNttList.do?bbsNo=187&key=1736&pageIndex=",
        "pages_to_crawl": 5
    },
    {
        "category_name": "부석면 공지사항",
        "base_url": "https://www.seosan.go.kr/emd/selectBbsNttList.do?bbsNo=196&key=1769&pageIndex=",
        "pages_to_crawl": 5
    },
    {
        "category_name": "팔봉면 공지사항",
        "base_url": "https://www.seosan.go.kr/emd/selectBbsNttList.do?bbsNo=206&key=1804&pageIndex=",
        "pages_to_crawl": 5
    },
    {
        "category_name": "지곡면 공지사항",
        "base_url": "https://www.seosan.go.kr/emd/selectBbsNttList.do?bbsNo=217&key=1835&pageIndex=",
        "pages_to_crawl": 5
    },
    {
        "category_name": "성연면 공지사항",
        "base_url": "https://www.seosan.go.kr/emd/selectBbsNttList.do?bbsNo=226&key=1863&pageIndex=",
        "pages_to_crawl": 5
    },
    {
        "category_name": "음암면 공지사항",
        "base_url": "https://www.seosan.go.kr/emd/selectBbsNttList.do?bbsNo=235&key=1892&pageIndex=",
        "pages_to_crawl": 5
    },
    {
        "category_name": "운산면 공지사항",
        "base_url": "https://www.seosan.go.kr/emd/selectBbsNttList.do?bbsNo=245&key=1925&pageIndex=",
        "pages_to_crawl": 5
    },
    {
        "category_name": "해미면 공지사항",
        "base_url": "https://www.seosan.go.kr/emd/selectBbsNttList.do?bbsNo=254&key=1956&pageIndex=",
        "pages_to_crawl": 5
    },
    {
        "category_name": "고북면 공지사항",
        "base_url": "https://www.seosan.go.kr/emd/selectBbsNttList.do?bbsNo=264&key=1990&pageIndex=",
        "pages_to_crawl": 5
    },
    {
        "category_name": "부춘동 공지사항",
        "base_url": "https://www.seosan.go.kr/emd/selectBbsNttList.do?bbsNo=254&key=1956&pageIndex=",
        "pages_to_crawl": 5
    },
    {
        "category_name": "동문1동 공지사항",
        "base_url": "https://www.seosan.go.kr/emd/selectBbsNttList.do?bbsNo=282&key=2046&pageIndex=",
        "pages_to_crawl": 5
    },
    {
        "category_name": "동문2동 공지사항",
        "base_url": "https://www.seosan.go.kr/emd/selectBbsNttList.do?bbsNo=291&key=2074&pageIndex=",
        "pages_to_crawl": 5
    },
    {
        "category_name": "수석동 공지사항",
        "base_url": "https://www.seosan.go.kr/emd/selectBbsNttList.do?bbsNo=300&key=2104&pageIndex=",
        "pages_to_crawl": 5
    },
    {
        "category_name": "석남동 공지사항",
        "base_url": "https://www.seosan.go.kr/emd/selectBbsNttList.do?bbsNo=309&key=2133&pageIndex=",
        "pages_to_crawl": 5
    }
]
