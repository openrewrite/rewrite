"""Data tables produced by a composite's children must appear on the composite's descriptor.

Mirrors Java's ``Recipe#aggregateDataTableDescriptors``: a composite reports the
union of its own tables and every table its ``recipe_list()`` produces, so the
marketplace listing (and everything downstream of it) can resolve them.
"""

from dataclasses import dataclass, field
from typing import Any, List

from rewrite import ExecutionContext
from rewrite.data_table import DataTable, column
from rewrite.recipe import Recipe, ScanningRecipe
from rewrite.rpc import RpcRecipe


@dataclass
class UsageRow:
    source_path: str = field(metadata=column("source_path", "Path of the file."))
    library: str = field(metadata=column("library", "The library used."))


LEAF_TABLE_NAME = "org.example.table.LibraryUsage"
NESTED_TABLE_NAME = "org.example.table.NestedUsage"
DELEGATE_TABLE_NAME = "org.example.table.ComputeResource"


@dataclass
class FindLibraryUsage(ScanningRecipe[List[Any]]):
    """Leaf recipe that owns a data table."""

    _table = DataTable(LEAF_TABLE_NAME, "Library usage", "One row per file.", UsageRow)

    @property
    def name(self) -> str:
        return "org.example.FindLibraryUsage"

    @property
    def display_name(self) -> str:
        return "Find library usage"

    @property
    def description(self) -> str:
        return "Leaf recipe that owns a data table."

    @property
    def data_tables(self) -> List[DataTable]:
        return [self._table]

    def initial_value(self, ctx: ExecutionContext) -> List[Any]:
        return []


@dataclass
class FindLibraryUsageByResource(Recipe):
    """Composite that owns no table of its own."""

    @property
    def name(self) -> str:
        return "org.example.FindLibraryUsageByResource"

    @property
    def display_name(self) -> str:
        return "Find library usage by resource"

    @property
    def description(self) -> str:
        return "Composite whose data tables come only from its children."

    def recipe_list(self) -> List[Recipe]:
        return [FindLibraryUsage()]


@dataclass
class JsonPreset(Recipe):
    """Preset one level above the composite, to prove aggregation is recursive."""

    @property
    def name(self) -> str:
        return "org.example.JsonPreset"

    @property
    def display_name(self) -> str:
        return "Json preset"

    @property
    def description(self) -> str:
        return "Turnkey preset over the composite."

    def recipe_list(self) -> List[Recipe]:
        return [FindLibraryUsageByResource()]


def _names(descriptor) -> List[str]:
    return [dt["name"] for dt in descriptor.data_tables]


def test_leaf_reports_its_own_table():
    assert _names(FindLibraryUsage().descriptor()) == [LEAF_TABLE_NAME]


def test_composite_aggregates_child_data_tables():
    descriptor = FindLibraryUsageByResource().descriptor()
    assert _names(descriptor.recipe_list[0]) == [LEAF_TABLE_NAME]
    assert _names(descriptor) == [LEAF_TABLE_NAME]


def test_aggregation_is_recursive():
    assert _names(JsonPreset().descriptor()) == [LEAF_TABLE_NAME]


def test_aggregated_table_keeps_its_columns():
    (table,) = JsonPreset().descriptor().data_tables
    assert [c["name"] for c in table["columns"]] == ["source_path", "library"]


def test_own_table_precedes_child_tables_and_duplicates_collapse():
    @dataclass
    class Parent(Recipe):
        _table = DataTable(NESTED_TABLE_NAME, "Nested", "Parent's own table.", UsageRow)

        @property
        def name(self) -> str:
            return "org.example.Parent"

        @property
        def display_name(self) -> str:
            return "Parent"

        @property
        def description(self) -> str:
            return "Owns a table and also wraps children."

        @property
        def data_tables(self) -> List[DataTable]:
            return [self._table]

        def recipe_list(self) -> List[Recipe]:
            # Same leaf twice: the union must not duplicate it.
            return [FindLibraryUsage(), FindLibraryUsage()]

    assert _names(Parent().descriptor()) == [NESTED_TABLE_NAME, LEAF_TABLE_NAME]


def test_rpc_recipe_reports_declared_delegate_tables():
    """An RpcRecipe references a recipe on another peer by name only, so the
    delegate's tables cannot be introspected locally; the composite author
    declares them and they aggregate like any other child's."""
    delegate_table = DataTable(
        DELEGATE_TABLE_NAME, "Compute resources", "Resources from the delegate.", UsageRow
    )

    @dataclass
    class WithDelegate(Recipe):
        @property
        def name(self) -> str:
            return "org.example.WithDelegate"

        @property
        def display_name(self) -> str:
            return "With delegate"

        @property
        def description(self) -> str:
            return "Composite delegating to a recipe on another peer."

        def recipe_list(self) -> List[Recipe]:
            return [
                FindLibraryUsage(),
                RpcRecipe("org.example.java.FindComputeResources",
                          data_tables=[delegate_table]),
            ]

    assert _names(WithDelegate().descriptor()) == [LEAF_TABLE_NAME, DELEGATE_TABLE_NAME]


def test_rpc_recipe_without_declared_tables_reports_none():
    assert RpcRecipe("org.example.java.Whatever").data_tables == []
