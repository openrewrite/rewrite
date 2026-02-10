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

import os
from abc import ABC, abstractmethod
from dataclasses import dataclass
from typing import Any, Dict, Generic, List, Type, TypeVar, TYPE_CHECKING, cast

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


class DataTableStore(ABC):
    """Interface for storing data table rows."""

    @abstractmethod
    def insert_row(
        self, data_table: DataTable, ctx: ExecutionContext, row: Any
    ) -> None:
        """Insert a row into the data table."""
        ...

    @abstractmethod
    def accept_rows(self, accept: bool) -> None:
        """Enable or disable row acceptance."""
        ...


class InMemoryDataTableStore(DataTableStore):
    """Stores data table rows in memory."""

    def __init__(self):
        self._accept_rows = False
        self._data_tables: Dict[str, DataTable] = {}
        self._rows: Dict[str, List[Any]] = {}

    def insert_row(
        self, data_table: DataTable, ctx: ExecutionContext, row: Any
    ) -> None:
        if self._accept_rows:
            name = data_table.name
            self._data_tables[name] = data_table
            if name not in self._rows:
                self._rows[name] = []
            self._rows[name].append(row)

    def accept_rows(self, accept: bool) -> None:
        self._accept_rows = accept

    @property
    def rows(self) -> Dict[str, List[Any]]:
        """Get all stored rows by data table name."""
        return self._rows

    @property
    def data_tables(self) -> Dict[str, DataTable]:
        """Get all data tables that have rows."""
        return self._data_tables


class CsvDataTableStore(DataTableStore):
    """
    Writes data table rows directly to CSV files (RFC 4180 format).

    Each data table is written to a separate CSV file named after the
    data table's fully qualified name.
    """

    def __init__(self, output_dir: str):
        self._output_dir = output_dir
        self._accept_rows = False
        self._initialized_tables: set[str] = set()
        self._row_counts: Dict[str, int] = {}
        os.makedirs(output_dir, exist_ok=True)

    def insert_row(
        self, data_table: DataTable, ctx: ExecutionContext, row: Any
    ) -> None:
        if not self._accept_rows:
            return

        descriptor = data_table.descriptor()
        table_name = descriptor["name"]
        csv_path = os.path.join(self._output_dir, f"{table_name}.csv")

        # Write header on first row
        if table_name not in self._initialized_tables:
            self._initialized_tables.add(table_name)
            self._row_counts[table_name] = 0
            headers = [
                self._escape_csv(col["displayName"]) for col in descriptor["columns"]
            ]
            with open(csv_path, "w") as f:
                f.write(",".join(headers) + "\n")

        # Write data row
        columns = descriptor["columns"]
        values = [
            self._escape_csv(getattr(row, col["name"], "")) for col in columns
        ]
        with open(csv_path, "a") as f:
            f.write(",".join(values) + "\n")
        self._row_counts[table_name] = self._row_counts.get(table_name, 0) + 1

    def accept_rows(self, accept: bool) -> None:
        self._accept_rows = accept

    @property
    def row_counts(self) -> Dict[str, int]:
        """Get the number of rows written for each data table."""
        return dict(self._row_counts)

    @property
    def table_names(self) -> List[str]:
        """Get the names of all data tables that have been written to."""
        return list(self._initialized_tables)

    @staticmethod
    def _escape_csv(value: Any) -> str:
        """Escape a value for CSV output following RFC 4180."""
        if value is None:
            return '""'
        s = str(value)
        # If the value contains comma, quote, or newline, wrap in quotes and escape internal quotes
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
        """
        Create a new data table.

        Args:
            name: Fully qualified name for the data table (e.g., "org.openrewrite.python.table.MyTable")
            display_name: Human-readable display name
            description: Description of what data the table contains
            row_type: The dataclass type for rows (must have fields with column metadata)
        """
        self._name = name
        self._display_name = display_name
        self._description = description
        self._row_type = row_type

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

    def descriptor(self) -> dict:
        """
        Get the data table descriptor with column metadata.

        Returns a dictionary suitable for JSON serialization with the
        table's metadata and column definitions.
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
