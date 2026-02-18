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

"""Tests for data table support."""

import os
import tempfile
from dataclasses import dataclass, field

import pytest

from rewrite import InMemoryExecutionContext
from rewrite.data_table import (
    DataTable,
    column,
    InMemoryDataTableStore,
    CsvDataTableStore,
    DATA_TABLE_STORE,
)


@dataclass
class SampleRow:
    """A test row type for data tables."""

    source_path: str = field(metadata=column("Source Path", "The path of the file"))
    text: str = field(metadata=column("Text", "Some text content"))


class TestDataTable:
    """Tests for DataTable class."""

    def test_descriptor_has_columns(self):
        """Test that descriptor includes column metadata."""
        table = DataTable[SampleRow](
            "org.openrewrite.test.TestTable",
            "Test Table",
            "A test data table.",
            SampleRow,
        )

        descriptor = table.descriptor()

        assert descriptor["name"] == "org.openrewrite.test.TestTable"
        assert descriptor["displayName"] == "Test Table"
        assert descriptor["description"] == "A test data table."
        assert len(descriptor["columns"]) == 2

        col1 = descriptor["columns"][0]
        assert col1["name"] == "source_path"
        assert col1["displayName"] == "Source Path"
        assert col1["description"] == "The path of the file"

        col2 = descriptor["columns"][1]
        assert col2["name"] == "text"
        assert col2["displayName"] == "Text"
        assert col2["description"] == "Some text content"

    def test_insert_row_creates_store(self):
        """Test that inserting a row creates an InMemoryDataTableStore if none exists."""
        table = DataTable[SampleRow](
            "org.openrewrite.test.TestTable",
            "Test Table",
            "A test data table.",
            SampleRow,
        )
        ctx = InMemoryExecutionContext()

        # Initially no store
        assert ctx.get_message(DATA_TABLE_STORE) is None

        # Insert a row
        table.insert_row(ctx, SampleRow("src/foo.py", "hello"))

        # Now there should be a store
        store = ctx.get_message(DATA_TABLE_STORE)
        assert store is not None
        assert isinstance(store, InMemoryDataTableStore)


class TestInMemoryDataTableStore:
    """Tests for InMemoryDataTableStore."""

    def test_stores_rows_when_accepting(self):
        """Test that rows are stored when accept_rows is True."""
        store = InMemoryDataTableStore()
        table = DataTable[SampleRow](
            "org.openrewrite.test.TestTable",
            "Test Table",
            "A test data table.",
            SampleRow,
        )
        ctx = InMemoryExecutionContext()

        # By default, accept_rows is False
        store.insert_row(table, ctx, SampleRow("src/foo.py", "hello"))
        assert len(store.rows) == 0

        # Enable accepting rows
        store.accept_rows(True)
        store.insert_row(table, ctx, SampleRow("src/foo.py", "hello"))
        store.insert_row(table, ctx, SampleRow("src/bar.py", "world"))

        assert "org.openrewrite.test.TestTable" in store.rows
        assert len(store.rows["org.openrewrite.test.TestTable"]) == 2


class TestCsvDataTableStore:
    """Tests for CsvDataTableStore."""

    def test_writes_csv_with_header(self):
        """Test that CSV files are written with header and data rows."""
        with tempfile.TemporaryDirectory() as tmpdir:
            store = CsvDataTableStore(tmpdir)
            table = DataTable[SampleRow](
                "org.openrewrite.test.TestTable",
                "Test Table",
                "A test data table.",
                SampleRow,
            )
            ctx = InMemoryExecutionContext()

            store.accept_rows(True)
            store.insert_row(table, ctx, SampleRow("src/foo.py", "hello"))
            store.insert_row(table, ctx, SampleRow("src/bar.py", "world"))

            csv_path = os.path.join(tmpdir, "org.openrewrite.test.TestTable.csv")
            assert os.path.exists(csv_path)

            with open(csv_path, "r") as f:
                content = f.read()

            lines = content.strip().split("\n")
            assert len(lines) == 3  # header + 2 data rows
            assert lines[0] == "Source Path,Text"
            assert lines[1] == "src/foo.py,hello"
            assert lines[2] == "src/bar.py,world"

    def test_escapes_special_characters(self):
        """Test that special characters are properly escaped in CSV."""
        with tempfile.TemporaryDirectory() as tmpdir:
            store = CsvDataTableStore(tmpdir)
            table = DataTable[SampleRow](
                "org.openrewrite.test.TestTable",
                "Test Table",
                "A test data table.",
                SampleRow,
            )
            ctx = InMemoryExecutionContext()

            store.accept_rows(True)
            # Test comma, quote, and newline escaping
            store.insert_row(table, ctx, SampleRow("src/foo.py", "hello, world"))
            store.insert_row(table, ctx, SampleRow("src/bar.py", 'say "hi"'))
            store.insert_row(table, ctx, SampleRow("src/baz.py", "line1\nline2"))

            csv_path = os.path.join(tmpdir, "org.openrewrite.test.TestTable.csv")
            with open(csv_path, "r") as f:
                content = f.read()

            lines = content.strip().split("\n")
            # Note: newlines in values will cause extra lines
            assert 'src/foo.py,"hello, world"' in content
            assert 'src/bar.py,"say ""hi"""' in content

    def test_does_not_write_when_not_accepting(self):
        """Test that no rows are written when accept_rows is False."""
        with tempfile.TemporaryDirectory() as tmpdir:
            store = CsvDataTableStore(tmpdir)
            table = DataTable[SampleRow](
                "org.openrewrite.test.TestTable",
                "Test Table",
                "A test data table.",
                SampleRow,
            )
            ctx = InMemoryExecutionContext()

            # Don't enable accepting rows
            store.insert_row(table, ctx, SampleRow("src/foo.py", "hello"))

            csv_path = os.path.join(tmpdir, "org.openrewrite.test.TestTable.csv")
            assert not os.path.exists(csv_path)

    def test_tracks_row_counts(self):
        """Test that row counts are tracked correctly."""
        with tempfile.TemporaryDirectory() as tmpdir:
            store = CsvDataTableStore(tmpdir)
            table = DataTable[SampleRow](
                "org.openrewrite.test.TestTable",
                "Test Table",
                "A test data table.",
                SampleRow,
            )
            ctx = InMemoryExecutionContext()

            store.accept_rows(True)
            store.insert_row(table, ctx, SampleRow("src/foo.py", "hello"))
            store.insert_row(table, ctx, SampleRow("src/bar.py", "world"))

            assert store.row_counts["org.openrewrite.test.TestTable"] == 2
            assert "org.openrewrite.test.TestTable" in store.table_names
