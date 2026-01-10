from __future__ import annotations

from abc import ABC, abstractmethod
from collections.abc import Callable
from dataclasses import dataclass
from typing import Any, ClassVar, TYPE_CHECKING, List, Optional

from .tree import SourceFile

if TYPE_CHECKING:
    from .visitor import TreeVisitor, Cursor


class ExecutionContext(ABC):
    REQUIRE_PRINT_EQUALS_INPUT: ClassVar[str] = "org.openrewrite.requirePrintEqualsInput"
    CHARSET: ClassVar[str] = "org.openrewrite.parser.charset"

    @abstractmethod
    def get_message(self, key: str, default_value=None) -> Any:
        ...

    @abstractmethod
    def put_message(self, key: str, value: Any):
        ...


class DelegatingExecutionContext(ExecutionContext):
    def __init__(self, delegate):
        self._delegate = delegate

    def get_message(self, key, default_value=None):
        return self._delegate.get_message(key, default_value)

    def put_message(self, key, value):
        self._delegate.put_message(key, value)


class InMemoryExecutionContext(ExecutionContext):
    _messages: dict[str, Any] = {}

    def get_message(self, key: str, default_value=None) -> Any:
        return self._messages[key] if key in self._messages else default_value

    def put_message(self, key: str, value: Any):
        self._messages[key] = value


class RecipeRunException(Exception):
    def __init__(self, cause: Exception, cursor=None):
        super().__init__()
        self._cause = cause
        self._cursor = cursor

    @property
    def cause(self):
        return self._cause

    @property
    def cursor(self):
        return self._cursor


class LargeSourceSet(ABC):
    def edit(self, map: Callable[[SourceFile], Optional[SourceFile]]) -> LargeSourceSet:
        ...

    def get_changeset(self) -> List[Result]:
        ...


class InMemoryLargeSourceSet(LargeSourceSet):
    _initial_state: Optional[InMemoryLargeSourceSet]
    _sources: List[SourceFile]
    _deletions: List[SourceFile]

    def __init__(self, sources: List[SourceFile], deletions: List[SourceFile] = None, initial_state: InMemoryLargeSourceSet = None):
        self._initial_state = initial_state
        self._sources = sources
        self._deletions = deletions or []

    def edit(self, map: Callable[[SourceFile], Optional[SourceFile]]):
        mapped: List[SourceFile] = []
        deleted = list(self._initial_state._deletions) if self._initial_state else []
        changed = False
        for source in self._sources:
            mapped_source = map(source)
            if mapped_source is not None:
                mapped.append(mapped_source)
                changed = mapped_source is not source
            else:
                deleted.append(source)
                changed = True
        return self if not changed else InMemoryLargeSourceSet(mapped, deleted, self._initial_state or self)

    def get_changeset(self) -> List[Result]:
        source_file_by_id = {sf.id: sf for sf in (self._initial_state or self)._sources}
        changes = []
        for source in self._sources:
            original = source_file_by_id.get(source.id)
            changes.append(Result(original, source))

        for source in self._deletions:
            changes.append(Result(source, None))

        return changes


@dataclass(frozen=True)
class Result:
    _before: Optional[SourceFile]
    _after: Optional[SourceFile]


class Recipe:
    def get_visitor(self):
        from .visitor import TreeVisitor
        return TreeVisitor.noop()

    def get_recipe_list(self) -> List[Recipe]:
        return []

    def run(self, before: LargeSourceSet, ctx: ExecutionContext) -> List[Result]:
        from .visitor import Cursor
        lss = self.run_internal(before, ctx, Cursor(None, Cursor.ROOT_VALUE))
        return lss.get_changeset()

    def run_internal(self, before: LargeSourceSet, ctx: ExecutionContext, root: Cursor) -> LargeSourceSet:
        after = before.edit(lambda before: self.get_visitor().visit(before, ctx, root))
        for recipe in self.get_recipe_list():
            after = recipe.run_internal(after, ctx, root)
        return after
