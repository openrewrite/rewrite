import sys

import pytest


def pytest_runtest_setup(item):
    if sys.version_info < (3, 11):
        pytest.skip("requires Python 3.11+")
