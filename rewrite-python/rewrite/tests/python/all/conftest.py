"""Shared fixtures for tests/python/all/.

These tests validate parse/print idempotence and don't need type attribution,
so we disable it by default to avoid the ty-types subprocess overhead.
Tests that need type attribution can opt in with RecipeSpec(type_attribution=True).
"""

import pytest

from rewrite.test.rewrite_test import RecipeSpec

_original_init = RecipeSpec.__init__


@pytest.fixture(autouse=True)
def _no_type_attribution(monkeypatch):
    def init_with_no_types(self, *args, type_attribution=False, **kwargs):
        _original_init(self, *args, type_attribution=type_attribution, **kwargs)

    monkeypatch.setattr(RecipeSpec, '__init__', init_with_no_types)
