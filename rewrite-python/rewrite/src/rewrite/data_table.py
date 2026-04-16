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

"""Data table support for Python recipes."""

from __future__ import annotations

import hashlib
import os
import re
from abc import ABC, abstractmethod
from dataclasses import dataclass
from typing import Any, Dict, Generic, Iterator, List, Optional, Type, TypeVar, TYPE_CHECKING, cast

if TYPE_CHECKING:
    from rewrite.execution import ExecutionContext

DATA_TABLE_STORE = "org.openrewrite.dataTables.store"


@dataclass(frozen=True)
class ColumnDescriptor:
    """Descriptor for a data table column."""

    display_name: str
    description: str


def column(display_name: str, description: str) -> dict:
    """
    Create column metadata for a data table row field.

    Use this with dataclasses.field(metadata=column(...)) to define
    columns that will be included in the data table output.

    Args:
        display_name: Human-readable name for the column
        description: Description of what the column contains

    Returns:
        Metadata dictionary to pass to dataclasses.field()

    Example:
        @dataclass
        class MyRow:
            source_path: str = field(metadata=column(
                display_name="Source Path",
                description="The path of the file"
            ))
    """
    return {"column": ColumnDescriptor(display_name, description)}


def _sha256_prefix(s: str, hex_chars: int = 4) -> str:
    """Return the first hex_chars of a SHA-256 hash of s."""
    h = hashlib.sha256(s.encode('utf-8')).hexdigest()
    return h[:hex_chars]


def sanitize_scope(scope: str) -> str:
    """
    Sanitize a scope string for use in filenames.

    1. Lowercase
    2. Replace non-alphanumeric with '-'
    3. Collapse consecutive '-', trim leading/trailing
    4. Truncate to ~30 chars at last '-' boundary
    5. Append 4-char hash of original scope
    """
    # 1. lowercase
    s = scope.lower()
    # 2. replace non-alphanumeric with '-'
    s = re.sub(r'[^a-z0-9]', '-', s)
    # 3. collapse consecutive '-'
    s = re.sub(r'-+', '-', s).strip('-')
    # 4. truncate to ~30 chars at word boundary
    if len(s) > 30:
        s = s[:30]
        last_dash = s.rfind('-')
        if last_dash > 0:
            s = s[:last_dash]
    # 5. append hash
    h = _sha256_prefix(scope)
    return f"{s}-{h}"


class DataTableStore(ABC):
    """Interface for storing data table rows."""

    @abstractmethod
    def insert_row(
        self, data_table: DataTable, ctx: ExecutionContext, row: Any
    ) -> None:
        """Insert a row into the data table."""
        ...

    @abstractmethod
    def get_rows(self, data_table_name: str, scope: str) -> Iterator:
        """Stream rows for a specific table by name and scope."""
        ...

    @abstractmethod
    def get_data_tables(self) -> List[DataTable]:
        """Get all data tables that have received rows."""
        ...


class InMemoryDataTableStore(DataTableStore):
    """Stores data table rows in memory, keyed by (name, group or instanceName)."""

    def __init__(self):
        self._buckets: Dict[str, _Bucket] = {}

    def insert_row(
        self, data_table: DataTable, ctx: ExecutionContext, row: Any
    ) -> None:
        group = data_table.group
        suffix = group if group else data_table.instance_name
        key = f"{data_table.name}\0{suffix}"
        if key not in self._buckets:
            self._buckets[key] = _Bucket(data_table)
        self._buckets[key].rows.append(row)

    def get_rows(self, data_table_name: str, group: Optional[str] = None) -> Iterator:
        if group is not None:
            key = f"{data_table_name}\0{group}"
            bucket = self._buckets.get(key)
            return iter(list(bucket.rows)) if bucket else iter([])
        # For ungrouped, find by name with no group
        for bucket in self._buckets.values():
            if bucket.data_table.name == data_table_name and bucket.data_table.group is None:
                return iter(list(bucket.rows))
        return iter([])

    def get_data_tables(self) -> List[DataTable]:
        return [b.data_table for b in self._buckets.values()]


class _Bucket:
    def __init__(self, data_table: DataTable):
        self.data_table = data_table
        self.rows: List[Any] = []


class CsvDataTableStore(DataTableStore):
    """
    Writes data table rows directly to CSV files (RFC 4180 format).

    Each data table bucket is written to a separate CSV file named using
    the data table's file-safe key.
    """

    def __init__(self, output_dir: str):
        self._output_dir = output_dir
        self._initialized_tables: set[str] = set()
        self._row_counts: Dict[str, int] = {}
        self._data_tables: Dict[str, DataTable] = {}
        os.makedirs(output_dir, exist_ok=True)

    def insert_row(
        self, data_table: DataTable, ctx: ExecutionContext, row: Any
    ) -> None:
        file_key = self._file_key(data_table)
        csv_path = os.path.join(self._output_dir, f"{file_key}.csv")

        # Write metadata comments and header on first row
        if file_key not in self._initialized_tables:
            self._initialized_tables.add(file_key)
            self._row_counts[file_key] = 0
            self._data_tables[file_key] = data_table
            descriptor = data_table.descriptor()
            headers = [
                self._escape_csv(col["displayName"]) for col in descriptor["columns"]
            ]
            with open(csv_path, "w") as f:
                f.write(f"# @name {data_table.name}\n")
                f.write(f"# @instanceName {data_table.instance_name}\n")
                f.write(f"# @group {data_table.group or ''}\n")
                f.write(",".join(headers) + "\n")

        # Write data row
        descriptor = data_table.descriptor()
        columns = descriptor["columns"]
        values = [
            self._escape_csv(getattr(row, col["name"], "")) for col in columns
        ]
        with open(csv_path, "a") as f:
            f.write(",".join(values) + "\n")
        self._row_counts[file_key] = self._row_counts.get(file_key, 0) + 1

    def get_rows(self, data_table_name: str, scope: str) -> Iterator:
        # CSV store writes to disk; reading back is not supported in this impl
        return iter([])

    def get_data_tables(self) -> List[DataTable]:
        return list(self._data_tables.values())

    @property
    def row_counts(self) -> Dict[str, int]:
        """Get the number of rows written for each data table."""
        return dict(self._row_counts)

    @property
    def table_names(self) -> List[str]:
        """Get the file-safe keys of all data tables that have been written to."""
        return list(self._initialized_tables)

    @staticmethod
    def _file_key(data_table: DataTable) -> str:
        suffix = data_table.group if data_table.group else data_table.instance_name
        if suffix == data_table.name:
            return data_table.name
        return f"{data_table.name}--{sanitize_scope(suffix)}"

    @staticmethod
    def _escape_csv(value: Any) -> str:
        """Escape a value for CSV output following RFC 4180."""
        if value is None:
            return '""'
        s = str(value)
        if "," in s or '"' in s or "\n" in s or "\r" in s:
            return '"' + s.replace('"', '""') + '"'
        return s


Row = TypeVar("Row")


class DataTable(Generic[Row]):
    """
    A data table for collecting structured data during recipe execution.

    Data tables allow recipes to output structured data (rows with columns)
    that can be displayed in the UI or exported to CSV files.

    Example:
        @dataclass
        class DeprecatedUsage:
            source_path: str = field(metadata=column("Source Path", "Path to the file"))
            symbol: str = field(metadata=column("Symbol", "The deprecated symbol"))

        class FindDeprecatedUsages(Recipe):
            table = DataTable[DeprecatedUsage](
                "org.openrewrite.python.table.DeprecatedUsages",
                "Deprecated Usages",
                "Usages of deprecated symbols.",
                DeprecatedUsage
            )

            @property
            def data_tables(self):
                return [self.table]
    """

    def __init__(
        self, name: str, display_name: str, description: str, row_type: Type[Row]
    ):
        self._name = name
        self._display_name = display_name
        self._description = description
        self._row_type = row_type
        self._group: Optional[str] = None
        self._instance_name: Optional[str] = None

    @property
    def name(self) -> str:
        """Fully qualified name of the data table."""
        return self._name

    @property
    def display_name(self) -> str:
        """Human-readable display name."""
        return self._display_name

    @property
    def description(self) -> str:
        """Description of what data the table contains."""
        return self._description

    @property
    def row_type(self) -> Type[Row]:
        """The dataclass type for rows."""
        return self._row_type

    @property
    def group(self) -> Optional[str]:
        return self._group

    @group.setter
    def group(self, value: str) -> None:
        self._group = value

    @property
    def instance_name(self) -> str:
        """The instance name. Defaults to display_name."""
        return self._instance_name if self._instance_name else self._display_name

    @instance_name.setter
    def instance_name(self, value: str) -> None:
        self._instance_name = value

    def descriptor(self) -> dict:
        """
        Get the data table descriptor with column metadata.
        """
        columns = []
        if hasattr(self._row_type, "__dataclass_fields__"):
            dc_fields: Dict[str, Any] = cast(Dict[str, Any], self._row_type.__dataclass_fields__)
            for field_name, field in dc_fields.items():
                if "column" in field.metadata:
                    col_desc = field.metadata["column"]
                    columns.append(
                        {
                            "name": field_name,
                            "displayName": col_desc.display_name,
                            "description": col_desc.description,
                        }
                    )
        return {
            "name": self._name,
            "displayName": self._display_name,
            "description": self._description,
            "columns": columns,
        }

    def insert_row(self, ctx: ExecutionContext, row: Row) -> None:
        """
        Insert a row into the data table.

        The row will be stored in the DataTableStore associated with the
        execution context. If no store exists, an InMemoryDataTableStore
        will be created.

        Args:
            ctx: The execution context
            row: The row to insert (must be an instance of row_type)
        """
        store = ctx.get_message(DATA_TABLE_STORE)
        if store is None:
            store = InMemoryDataTableStore()
            ctx.put_message(DATA_TABLE_STORE, store)
        store.insert_row(self, ctx, row)
