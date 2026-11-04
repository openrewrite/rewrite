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
    sanitize_scope,
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
            # Column NAMES, not display names (matches the Java writer).
            assert lines[3] == "source_path,text"
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

    def test_writes_prefix_and_suffix_columns(self):
        """Prefix/suffix columns appear in the header and on every row."""
        with tempfile.TemporaryDirectory() as tmpdir:
            store = CsvDataTableStore(
                tmpdir,
                {"repositoryOrigin": "github.com/acme/example"},
                {"organization": "acme"},
            )
            table = DataTable[SampleRow](
                "org.openrewrite.test.TestTable",
                "Test Table",
                "A test data table.",
                SampleRow,
            )
            ctx = InMemoryExecutionContext()

            store.insert_row(table, ctx, SampleRow("src/foo.py", "written"))

            file_key = store._file_key(table)
            with open(os.path.join(tmpdir, file_key + ".csv")) as f:
                content = f.read()

            lines = content.strip().split("\n")
            assert lines[3] == "repositoryOrigin,source_path,text,organization"
            assert lines[4] == "github.com/acme/example,src/foo.py,written,acme"

    def test_two_stores_share_one_file_with_single_header(self):
        """Two writers share one file; the header is written once (keyed on file existence)."""
        with tempfile.TemporaryDirectory() as tmpdir:
            a = CsvDataTableStore(tmpdir, {"repositoryOrigin": "github.com/acme/x"})
            b = CsvDataTableStore(tmpdir, {"repositoryOrigin": "github.com/acme/x"})
            table = DataTable[SampleRow](
                "org.openrewrite.test.TestTable",
                "Test Table",
                "A test data table.",
                SampleRow,
            )
            ctx = InMemoryExecutionContext()

            a.insert_row(table, ctx, SampleRow("a.py", "A"))
            b.insert_row(table, ctx, SampleRow("b.py", "B"))

            file_key = a._file_key(table)
            with open(os.path.join(tmpdir, file_key + ".csv")) as f:
                content = f.read()

            header_lines = [
                line for line in content.split("\n")
                if line.startswith("repositoryOrigin,")
            ]
            assert len(header_lines) == 1
            assert "a.py" in content
            assert "b.py" in content


class TestFileKeyMatchesJava:
    """File key must mirror org.openrewrite.CsvDataTableStore#fileKey so Java and Python recipes share a filename."""

    def test_default_table_uses_bare_fully_qualified_name(self):
        """A default table (instance_name == display_name) resolves to the bare FQN, not '<name>--<suffix>'."""
        table = DataTable[SampleRow](
            "org.openrewrite.table.TextMatches",
            "Text matches",
            "Text matches.",
            SampleRow,
        )
        assert table.instance_name == table.display_name
        assert CsvDataTableStore._file_key(table) == "org.openrewrite.table.TextMatches"

    def test_custom_instance_name_appends_sanitized_suffix(self):
        """A customized instance name gets a '--<sanitize(instanceName)>' suffix."""
        table = DataTable[SampleRow](
            "org.openrewrite.test.TestTable",
            "Test Table",
            "A test data table.",
            SampleRow,
        )
        table.instance_name = "Custom Name"
        # 'Custom Name' -> 'custom-name' + '-' + first 4 hex of sha256('Custom Name')
        assert (
            CsvDataTableStore._file_key(table)
            == "org.openrewrite.test.TestTable--custom-name-3ee9"
        )

    def test_grouped_table_appends_sanitized_group(self):
        """A grouped table keys off the group, which takes precedence over the instance name."""
        table = DataTable[SampleRow](
            "org.openrewrite.test.TestTable",
            "Test Table",
            "A test data table.",
            SampleRow,
        )
        table.group = "org.openrewrite.table.SharedDeprecations"
        table.instance_name = "Custom Name"
        assert (
            CsvDataTableStore._file_key(table)
            == "org.openrewrite.test.TestTable--org-openrewrite-table-dca0"
        )

    def test_group_equal_to_name_uses_bare_name(self):
        """When the group equals the table's own name, the bare name is returned (no suffix)."""
        table = DataTable[SampleRow](
            "org.openrewrite.test.TestTable",
            "Test Table",
            "A test data table.",
            SampleRow,
        )
        table.group = "org.openrewrite.test.TestTable"
        assert CsvDataTableStore._file_key(table) == "org.openrewrite.test.TestTable"

    def test_sanitize_scope_is_byte_identical_to_java(self):
        """sanitize() must be byte-identical to Java: truncate-to-30-at-last-dash + 4-hex sha256 of the original value."""
        assert sanitize_scope("Custom Name") == "custom-name-3ee9"
        assert (
            sanitize_scope("org.openrewrite.table.SharedDeprecations")
            == "org-openrewrite-table-dca0"
        )
        assert (
            sanitize_scope(
                "A very long custom instance name that exceeds thirty characters"
            )
            == "a-very-long-custom-instance-321f"
        )


class TestSetDataTableStoreHandler:
    """Tests for the SetDataTableStore RPC handshake (only configuration is conveyed, not rows)."""

    def test_reconstructs_csv_store_that_writes_prefix_columns(self):
        from rewrite.rpc import server

        with tempfile.TemporaryDirectory() as tmpdir:
            assert server.handle_set_data_table_store({
                "kind": "CSV",
                "outputDir": tmpdir,
                "prefixColumns": {"repositoryOrigin": "github.com/acme/example"},
                "suffixColumns": {},
            }) is True

            store = server._configured_data_table_store
            assert isinstance(store, CsvDataTableStore)

            table = DataTable[SampleRow](
                "org.openrewrite.test.TestTable",
                "Test Table",
                "A test data table.",
                SampleRow,
            )
            ctx = InMemoryExecutionContext()
            store.insert_row(table, ctx, SampleRow("src/foo.py", "written"))

            file_key = store._file_key(table)
            with open(os.path.join(tmpdir, file_key + ".csv")) as f:
                content = f.read()

            assert "repositoryOrigin" in content
            assert "github.com/acme/example,src/foo.py,written" in content

    def test_noop_does_not_write_to_disk(self):
        from rewrite.rpc import server

        with tempfile.TemporaryDirectory() as tmpdir:
            assert server.handle_set_data_table_store({"kind": "NOOP"}) is True

            store = server._configured_data_table_store
            assert isinstance(store, InMemoryDataTableStore)

            table = DataTable[SampleRow](
                "org.openrewrite.test.TestTable",
                "Test Table",
                "A test data table.",
                SampleRow,
            )
            ctx = InMemoryExecutionContext()
            store.insert_row(table, ctx, SampleRow("src/foo.py", "dropped"))

            assert [f for f in os.listdir(tmpdir) if f.endswith(".csv")] == []
