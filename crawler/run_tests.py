import sys
import os
import pytest

# Add the 'src' directory to the Python path
sys.path.insert(0, os.path.abspath(os.path.join(os.path.dirname(__file__), 'src')))

# Run pytest
pytest.main(['tests/test_seosan_city_crawler.py'])
