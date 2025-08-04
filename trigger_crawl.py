import requests

params = {
    'category': '공지사항',
    'url': 'https://www.seosan.go.kr/www/selectBbsNttList.do?bbsNo=97&key=1256&pageUnit=10&searchCnd=all&searchKrwd=&integrDeptCode=&pageIndex='
}

response = requests.get('http://localhost:5001/crawl', params=params)

print(response.status_code)
print(response.json())