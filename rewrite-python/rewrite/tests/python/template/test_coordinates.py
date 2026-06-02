# Copyright 2025 the original author or authors.
# <p>
# Licensed under the Moderne Source Available License (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
# <p>
# https://docs.moderne.io/licensing/moderne-source-available-license
# <p>
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

"""Tests for coordinates module."""

import dataclasses
from uuid import uuid4

from rewrite.java import tree as j
from rewrite.java.support_types import Space
from rewrite.markers import Markers
from rewrite.python.template.coordinates import PythonCoordinates, CoordinateMode, CoordinateLocation


def _make_tree():
    return j.Identifier(uuid4(), Space.EMPTY, Markers.EMPTY, [], "x", None, None)


class TestCoordinateMode:
    """Tests for CoordinateMode enum."""

    def test_all_enum_values_exist(self):
        assert hasattr(CoordinateMode, 'REPLACEMENT')
        assert hasattr(CoordinateMode, 'BEFORE')
        assert hasattr(CoordinateMode, 'AFTER')

    def test_values_are_strings(self):
        assert CoordinateMode.REPLACEMENT.value == "replacement"
        assert CoordinateMode.BEFORE.value == "before"
        assert CoordinateMode.AFTER.value == "after"


class TestCoordinateLocation:
    """Tests for CoordinateLocation enum."""

    def test_all_enum_values_exist(self):
        assert hasattr(CoordinateLocation, 'EXPRESSION_PREFIX')
        assert hasattr(CoordinateLocation, 'STATEMENT_PREFIX')
        assert hasattr(CoordinateLocation, 'BLOCK_END')
        assert hasattr(CoordinateLocation, 'ANNOTATION_PREFIX')
        assert hasattr(CoordinateLocation, 'DECORATOR_PREFIX')
        assert hasattr(CoordinateLocation, 'IMPORT_PREFIX')
        assert hasattr(CoordinateLocation, 'CLASS_BODY')
        assert hasattr(CoordinateLocation, 'FUNCTION_BODY')


class TestPythonCoordinates:
    """Tests for PythonCoordinates dataclass."""

    def test_replace_creates_replacement_mode(self):
        tree = _make_tree()
        coords = PythonCoordinates.replace(tree)
        assert coords.mode == CoordinateMode.REPLACEMENT

    def test_before_creates_before_mode(self):
        tree = _make_tree()
        coords = PythonCoordinates.before(tree)
        assert coords.mode == CoordinateMode.BEFORE

    def test_after_creates_after_mode(self):
        tree = _make_tree()
        coords = PythonCoordinates.after(tree)
        assert coords.mode == CoordinateMode.AFTER

    def test_is_replacement(self):
        tree = _make_tree()
        coords = PythonCoordinates.replace(tree)
        assert coords.is_replacement() is True
        assert coords.is_before() is False
        assert coords.is_after() is False

    def test_is_before(self):
        tree = _make_tree()
        coords = PythonCoordinates.before(tree)
        assert coords.is_replacement() is False
        assert coords.is_before() is True
        assert coords.is_after() is False

    def test_is_after(self):
        tree = _make_tree()
        coords = PythonCoordinates.after(tree)
        assert coords.is_replacement() is False
        assert coords.is_before() is False
        assert coords.is_after() is True

    def test_frozen_dataclass(self):
        tree = _make_tree()
        coords = PythonCoordinates.replace(tree)
        with __import__('pytest').raises(dataclasses.FrozenInstanceError):
            coords.mode = CoordinateMode.BEFORE  # type: ignore

    def test_default_mode_is_replacement(self):
        tree = _make_tree()
        coords = PythonCoordinates(tree=tree)
        assert coords.mode == CoordinateMode.REPLACEMENT

    def test_location_defaults_to_none(self):
        tree = _make_tree()
        coords = PythonCoordinates(tree=tree)
        assert coords.location is None

    def test_comparator_defaults_to_none(self):
        tree = _make_tree()
        coords = PythonCoordinates(tree=tree)
        assert coords.comparator is None
