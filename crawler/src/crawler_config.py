# -*- coding: utf-8 -*-


MAX_CRAWL_PAGES = 10  # 최대 크롤링 페이지 수 제한

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
    }
]
