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

        assert ctx.get_message(DATA_TABLE_STORE) is None

        table.insert_row(ctx, SampleRow("src/foo.py", "hello"))

        store = ctx.get_message(DATA_TABLE_STORE)
        assert store is not None
        assert isinstance(store, InMemoryDataTableStore)

    def test_instance_name_defaults_to_display_name(self):
        table = DataTable[SampleRow](
            "org.openrewrite.test.TestTable",
            "Test Table",
            "A test data table.",
            SampleRow,
        )
        assert table.instance_name == "Test Table"

    def test_instance_name_can_be_set(self):
        table = DataTable[SampleRow](
            "org.openrewrite.test.TestTable",
            "Test Table",
            "A test data table.",
            SampleRow,
        )
        table.instance_name = "Custom Name"
        assert table.instance_name == "Custom Name"

    def test_group_is_none_by_default(self):
        table = DataTable[SampleRow](
            "org.openrewrite.test.TestTable",
            "Test Table",
            "A test data table.",
            SampleRow,
        )
        assert table.group is None


class TestInMemoryDataTableStore:
    """Tests for InMemoryDataTableStore."""

    def test_stores_rows(self):
        """Test that rows are always stored."""
        store = InMemoryDataTableStore()
        table = DataTable[SampleRow](
            "org.openrewrite.test.TestTable",
            "Test Table",
            "A test data table.",
            SampleRow,
        )
        ctx = InMemoryExecutionContext()

        store.insert_row(table, ctx, SampleRow("src/foo.py", "hello"))
        store.insert_row(table, ctx, SampleRow("src/bar.py", "world"))

        rows = list(store.get_rows(table.name, None))
        assert len(rows) == 2
        assert len(store.get_data_tables()) == 1


class TestCsvDataTableStore:
    """Tests for CsvDataTableStore."""

    def test_writes_csv_with_comments_and_header(self):
        """Test that CSV files are written with metadata comments, header and data rows."""
        with tempfile.TemporaryDirectory() as tmpdir:
            store = CsvDataTableStore(tmpdir)
            table = DataTable[SampleRow](
                "org.openrewrite.test.TestTable",
                "Test Table",
                "A test data table.",
                SampleRow,
            )
            ctx = InMemoryExecutionContext()

            store.insert_row(table, ctx, SampleRow("src/foo.py", "hello"))
            store.insert_row(table, ctx, SampleRow("src/bar.py", "world"))

            file_key = store._file_key(table)
            csv_path = os.path.join(tmpdir, file_key + ".csv")
            assert os.path.exists(csv_path)

            with open(csv_path, "r") as f:
                content = f.read()

            lines = content.strip().split("\n")
            # 3 comment lines + 1 header + 2 data rows
            assert lines[0].startswith("# @name ")
            assert lines[1].startswith("# @instanceName ")
            assert lines[2].startswith("# @group")
            assert lines[3] == "Source Path,Text"
            assert lines[4] == "src/foo.py,hello"
            assert lines[5] == "src/bar.py,world"

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

            store.insert_row(table, ctx, SampleRow("src/foo.py", "hello, world"))
            store.insert_row(table, ctx, SampleRow("src/bar.py", 'say "hi"'))

            file_key = store._file_key(table)
            csv_path = os.path.join(tmpdir, file_key + ".csv")
            with open(csv_path, "r") as f:
                content = f.read()

            assert 'src/foo.py,"hello, world"' in content
            assert 'src/bar.py,"say ""hi"""' in content

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

            store.insert_row(table, ctx, SampleRow("src/foo.py", "hello"))
            store.insert_row(table, ctx, SampleRow("src/bar.py", "world"))

            file_key = store._file_key(table)
            assert store.row_counts[file_key] == 2
            assert file_key in store.table_names
