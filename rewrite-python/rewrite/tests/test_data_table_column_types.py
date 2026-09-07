# Copyright 2026 the original author or authors.
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

"""Tests that column descriptors report a Java-compatible ``type``.

``ColumnDescriptor`` declares ``name`` and ``type`` non-nullable, so every
column a Python recipe reports must carry a type for Java consumers to render
the table.
"""

from dataclasses import dataclass, field
from typing import Optional

import pytest

from rewrite.data_table import (DEFAULT_COLUMN_TYPE, DataTable, _java_type_name,
                                column)
from rewrite.recipe import Recipe, RecipeDescriptor


@dataclass
class ScalarRow:
    """Covers each annotation with a distinct Java analogue."""

    text: str = field(metadata=column("Text", "A string column"))
    count: int = field(metadata=column("Count", "An integer column"))
    enabled: bool = field(metadata=column("Enabled", "A boolean column"))
    ratio: float = field(metadata=column("Ratio", "A floating point column"))


def columns_by_name(data_table: DataTable) -> dict:
    return {c["name"]: c for c in data_table.descriptor()["columns"]}


def test_every_column_reports_a_type():
    table = DataTable[ScalarRow]("org.example.Scalars", "Scalars", "d", ScalarRow)

    untyped = [c["name"] for c in table.descriptor()["columns"] if not c.get("type")]
    assert untyped == []


@pytest.mark.parametrize(
    "field_name,expected",
    [
        ("text", "String"),
        ("count", "Long"),
        ("enabled", "Boolean"),
        ("ratio", "Double"),
    ],
)
def test_annotation_maps_to_java_type_name(field_name, expected):
    table = DataTable[ScalarRow]("org.example.Scalars", "Scalars", "d", ScalarRow)

    assert columns_by_name(table)[field_name]["type"] == expected


@dataclass
class OptionalRow:
    maybe_text: Optional[str] = field(metadata=column("Maybe Text", "Nullable"))
    maybe_count: Optional[int] = field(metadata=column("Maybe Count", "Nullable"))


def test_optional_unwraps_to_the_underlying_type():
    """Nullability is not a type; Java names a nullable column by its type."""
    table = DataTable[OptionalRow]("org.example.Optionals", "Optionals", "d", OptionalRow)

    columns = columns_by_name(table)
    assert columns["maybe_text"]["type"] == "String"
    assert columns["maybe_count"]["type"] == "Long"


@dataclass
class CustomTypeRow:
    payload: complex = field(metadata=column("Payload", "No Java analogue"))


def test_unrecognized_annotation_falls_back_to_the_default():
    """An unmapped annotation must still yield a type, never a null."""
    table = DataTable[CustomTypeRow]("org.example.Custom", "Custom", "d", CustomTypeRow)

    assert columns_by_name(table)["payload"]["type"] == DEFAULT_COLUMN_TYPE


def test_type_is_preserved_alongside_the_other_column_metadata():
    table = DataTable[ScalarRow]("org.example.Scalars", "Scalars", "d", ScalarRow)

    assert columns_by_name(table)["text"] == {
        "name": "text",
        "type": "String",
        "displayName": "Text",
        "description": "A string column",
    }


CHILD_TABLE = DataTable[ScalarRow]("org.example.Child", "Child", "d", ScalarRow)


class ChildRecipe(Recipe):
    @property
    def name(self) -> str:
        return "org.example.ChildRecipe"

    @property
    def display_name(self) -> str:
        return "Child recipe"

    @property
    def description(self) -> str:
        return "Produces a data table."

    @property
    def data_tables(self):
        return [CHILD_TABLE]


class CompositeRecipe(Recipe):
    @property
    def name(self) -> str:
        return "org.example.CompositeRecipe"

    @property
    def display_name(self) -> str:
        return "Composite recipe"

    @property
    def description(self) -> str:
        return "Runs a child recipe."

    def recipe_list(self):
        return [ChildRecipe()]


def test_types_survive_aggregation_onto_a_composite():
    """A composite reports its children's tables; those columns keep their types."""
    descriptor = RecipeDescriptor.from_recipe(CompositeRecipe())

    aggregated = descriptor.data_tables[0]["columns"]
    assert [c["type"] for c in aggregated] == ["String", "Long", "Boolean", "Double"]


@pytest.mark.parametrize(
    "annotation,expected",
    [
        ("str", "String"),
        ("int", "Long"),
        ("Optional[int]", "Long"),
        ("typing.Optional[int]", "Long"),
        ("str | None", "String"),
        ("int | None", "Long"),
        ("SomeUnmappedType", DEFAULT_COLUMN_TYPE),
    ],
)
def test_string_annotations_are_mapped(annotation, expected):
    """Under `from __future__ import annotations` the annotation is source text."""
    assert _java_type_name(annotation) == expected
