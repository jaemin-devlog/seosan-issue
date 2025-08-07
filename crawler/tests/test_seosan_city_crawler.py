import pytest
from unittest.mock import MagicMock, patch
from bs4 import BeautifulSoup
import requests
from src.crawlers.seosan_city_crawler import parse_detail_page, get_post_info, crawl_all_pages, session
from src.crawler_config import HEADERS, MAX_CRAWL_PAGES
from src.database import get_last_crawled_link, update_last_crawled_link

# Mock requests.Session().get for parse_detail_page
@pytest.fixture
def mock_session_get_detail(mocker):
    mock_response = MagicMock()
    mock_response.status_code = 200
    mock_response.raise_for_status.return_value = None
    mock_response.apparent_encoding = 'utf-8'
    mock_response.text = """
    <html>
    <body>
        <div id="bbs_content">
            <p>This is the <b>main content</b> of the page.</p>
            <!-- Some comment -->
            <p>Another paragraph with &nbsp; spaces.</p>
            <script>alert('xss');</script>
        </div>
    </body>
    </html>
    """
    mocker.patch.object(session, 'get', return_value=mock_response)

def test_parse_detail_page_success(mock_session_get_detail):
    url = "http://example.com/detail/123"
    expected_content = "This is the main content of the page. Another paragraph with spaces."
    
    content = parse_detail_page(url)
    
    assert content.strip() == expected_content.strip()

def test_parse_detail_page_no_content_element(mocker):
    mock_response = MagicMock()
    mock_response.status_code = 200
    mock_response.raise_for_status.return_value = None
    mock_response.apparent_encoding = 'utf-8'
    mock_response.text = """
    <html>
    <body>
        <div id="other_content">
            <p>This content should not be parsed.</p>
        </div>
    </body>
    </html>
    """
    mocker.patch.object(session, 'get', return_value=mock_response)

    url = "http://example.com/detail/456"
    content = parse_detail_page(url)
    
    assert content == ""

def test_parse_detail_page_request_exception(mocker):
    mocker.patch.object(session, 'get', side_effect=requests.exceptions.RequestException("Network error"))

    url = "http://example.com/detail/789"
    content = parse_detail_page(url)
    
    assert content == ""

# Test for get_post_info
@pytest.fixture
def mock_session_get_post_info(mocker):
    mock_response = MagicMock()
    mock_response.status_code = 200
    mock_response.raise_for_status.return_value = None
    mock_response.apparent_encoding = 'utf-8'
    mock_response.text = """
    <html>
    <body>
        <table>
            <tbody>
                <tr>
                    <td>101</td>
                    <td><a href="/post/101">Post Title 1</a></td>
                    <td>Department A</td>
                    <td>2023-01-01</td>
                </tr>
                <tr>
                    <td>102</td>
                    <td><a href="/post/102">Post Title 2</a></td>
                    <td>Department B</td>
                    <td>2023-01-02</td>
                </tr>
            </tbody>
        </table>
    </body>
    </html>
    """
    mocker.patch.object(session, 'get', return_value=mock_response)

def test_get_post_info_success(mock_session_get_post_info, mocker):
    # Mock parse_detail_page to return a fixed content
    mocker.patch('src.crawlers.seosan_city_crawler.parse_detail_page', return_value="Parsed detail content.")

    page = 1
    base_url = "http://example.com/board/"
    category_name = "공지사항"

    posts = get_post_info(page, base_url, category_name)

    assert len(posts) == 2
    assert posts[0]['id'] == 101
    assert posts[0]['title'] == "Post Title 1"
    assert posts[0]['link'] == "http://example.com/post/101"
    assert posts[0]['content'] == "Parsed detail content."
    assert posts[0]['department'] == "Department A"
    assert posts[0]['date'] == "2023-01-01"

    assert posts[1]['id'] == 102
    assert posts[1]['title'] == "Post Title 2"
    assert posts[1]['link'] == "http://example.com/post/102"
    assert posts[1]['content'] == "Parsed detail content."
    assert posts[1]['department'] == "Department B"
    assert posts[1]['date'] == "2023-01-02"

def test_get_post_info_request_exception(mocker):
    mocker.patch.object(session, 'get', side_effect=requests.exceptions.RequestException("Network error"))

    page = 1
    base_url = "http://example.com/board/"
    category_name = "공지사항"

    posts = get_post_info(page, base_url, category_name)

    assert posts == []

def test_get_post_info_welfare_category(mock_session_get_post_info, mocker):
    # Mock parse_detail_page to return a fixed content
    mocker.patch('src.crawlers.seosan_city_crawler.parse_detail_page', return_value="Parsed detail content.")

    # Modify mock_session_get_post_info to return welfare-specific HTML
    mock_response = MagicMock()
    mock_response.status_code = 200
    mock_response.raise_for_status.return_value = None
    mock_response.apparent_encoding = 'utf-8'
    mock_response.text = """
    <html>
    <body>
        <table>
            <tbody>
                <tr>
                    <td>201</td>
                    <td><a href="/welfare/201">Welfare Post 1</a></td>
                    <td><img src="attachment.png"></td>
                    <td>123</td>
                    <td>2023-03-01</td>
                </tr>
            </tbody>
        </table>
    </body>
    </html>
    """
    mocker.patch.object(session, 'get', return_value=mock_response)

    page = 1
    base_url = "http://example.com/welfare/"
    category_name = "복지정보"

    posts = get_post_info(page, base_url, category_name)

    assert len(posts) == 1
    assert posts[0]['id'] == 201
    assert posts[0]['title'] == "Welfare Post 1"
    assert posts[0]['link'] == "http://example.com/welfare/201"
    assert posts[0]['content'] == "Parsed detail content."
    assert posts[0]['attachment'] == "Y"
    assert posts[0]['views'] == "123"
    assert posts[0]['date'] == "2023-03-01"

def test_get_post_info_empty_tds(mock_session_get_post_info, mocker):
    # Mock parse_detail_page to return a fixed content
    mocker.patch('src.crawlers.seosan_city_crawler.parse_detail_page', return_value="Parsed detail content.")

    # Modify mock_session_get_post_info to return HTML with insufficient tds
    mock_response = MagicMock()
    mock_response.status_code = 200
    mock_response.raise_for_status.return_value = None
    mock_response.apparent_encoding = 'utf-8'
    mock_response.text = """
    <html>
    <body>
        <table>
            <tbody>
                <tr>
                    <td>101</td>
                    <td><a href="/post/101">Post Title 1</a></td>
                </tr>
            </tbody>
        </table>
    </body>
    </html>
    """
    mocker.patch.object(session, 'get', return_value=mock_response)

    page = 1
    base_url = "http://example.com/board/"
    category_name = "공지사항"

    posts = get_post_info(page, base_url, category_name)

    assert posts == []

def test_get_post_info_no_title_tag(mock_session_get_post_info, mocker):
    # Mock parse_detail_page to return a fixed content
    mocker.patch('src.crawlers.seosan_city_crawler.parse_detail_page', return_value="Parsed detail content.")

    # Modify mock_session_get_post_info to return HTML with no title tag
    mock_response = MagicMock()
    mock_response.status_code = 200
    mock_response.raise_for_status.return_value = None
    mock_response.apparent_encoding = 'utf-8'
    mock_response.text = """
    <html>
    <body>
        <table>
            <tbody>
                <tr>
                    <td>101</td>
                    <td><span>No Title</span></td>
                    <td>Department A</td>
                    <td>2023-01-01</td>
                </tr>
            </tbody>
        </table>
    </body>
    </html>
    """
    mocker.patch.object(session, 'get', return_value=mock_response)

    page = 1
    base_url = "http://example.com/board/"
    category_name = "공지사항"

    posts = get_post_info(page, base_url, category_name)

    assert posts == []

# Test for crawl_all_pages
@pytest.fixture
def mock_crawl_all_pages_dependencies(mocker):
    # Mock session.get for the first page to determine total_pages
    mock_first_page_response = MagicMock()
    mock_first_page_response.status_code = 200
    mock_first_page_response.raise_for_status.return_value = None
    mock_first_page_response.apparent_encoding = 'utf-8'
    mock_first_page_response.text = """
    <html>
    <body>
        <div class="pagination">
            <a href="?pageIndex=1">1</a>
            <a href="?pageIndex=2">2</a>
            <a href="?pageIndex=3">3</a>
            <a href="?pageIndex=10" class="next_end">Next End</a>
        </div>
        <table>
            <tbody>
                <tr>
                    <td>101</td>
                    <td><a href="/post/101">Post Title 1</a></td>
                    <td>Department A</td>
                    <td>2023-01-01</td>
                </tr>
            </tbody>
        </table>
    </body>
    </html>
    """
    mocker.patch.object(session, 'get', return_value=mock_first_page_response)

    # Mock get_post_info
    mocker.patch('src.crawlers.seosan_city_crawler.get_post_info', return_value=[
        {'id': 101, 'title': 'Mock Post', 'link': 'http://example.com/post/101', 'content': 'Mock Content'}
    ])

    # Mock database functions
    mocker.patch('src.database.get_last_crawled_link', return_value=None)
    mocker.patch('src.database.update_last_crawled_link')

def test_crawl_all_pages_success(mock_crawl_all_pages_dependencies, mocker):
    category_name = "공지사항"
    base_url = "http://example.com/board/"

    # Mock get_post_info and update_last_crawled_link for verification
    mock_get_post_info = mocker.patch('src.crawlers.seosan_city_crawler.get_post_info', return_value=[
        {'id': 101, 'title': 'Mock Post', 'link': 'http://example.com/post/101', 'content': 'Mock Content'}
    ])
    mock_update_last_crawled_link = mocker.patch('src.database.update_last_crawled_link')
    mock_get_last_crawled_link = mocker.patch('src.database.get_last_crawled_link', return_value=None)

    posts = crawl_all_pages(category_name, base_url)

    assert len(posts) > 0
    # Verify that get_post_info was called
    mock_get_post_info.assert_called_once()

    # Verify that update_last_crawled_link was called
    mock_update_last_crawled_link.assert_called_once()

def test_crawl_all_pages_no_new_posts(mock_crawl_all_pages_dependencies, mocker):
    # Mock get_last_crawled_link to return an existing link
    mock_get_last_crawled_link = mocker.patch('src.database.get_last_crawled_link', return_value="http://example.com/post/101")
    mock_get_post_info = mocker.patch('src.crawlers.seosan_city_crawler.get_post_info', return_value=[
        {'id': 101, 'title': 'Mock Post', 'link': 'http://example.com/post/101', 'content': 'Mock Content'}
    ])
    mock_update_last_crawled_link = mocker.patch('src.database.update_last_crawled_link')

    category_name = "공지사항"
    base_url = "http://example.com/board/"

    posts = crawl_all_pages(category_name, base_url)

    assert len(posts) == 0
    # Verify that update_last_crawled_link was NOT called
    mock_update_last_crawled_link.assert_not_called()

def test_crawl_all_pages_request_exception(mocker):
    mocker.patch.object(session, 'get', side_effect=requests.exceptions.RequestException("Network error"))

    category_name = "공지사항"
    base_url = "http://example.com/board/"

    posts = crawl_all_pages(category_name, base_url)

    assert posts == []