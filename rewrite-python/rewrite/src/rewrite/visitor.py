from __future__ import annotations

from abc import ABC
from dataclasses import dataclass
from typing import TypeVar, Optional, Dict, List, Any, cast, Type, ClassVar, Generic, Generator

from .execution import RecipeRunException
from .markers import Marker, Markers
from .tree import SourceFile, Tree
from .utils import list_map

O = TypeVar('O')
T = TypeVar('T', bound='Tree')
T2 = TypeVar('T2', bound='Tree')
TV = TypeVar('TV', bound='TreeVisitor[Any, Any]')
P = TypeVar('P')


@dataclass(frozen=True)
class Cursor:
    ROOT_VALUE: ClassVar[str] = "root"

    parent: Optional[Cursor]
    value: object
    messages: Optional[Dict[str, object]] = None

    def get_message(self, key: str, default_value: O) -> O:
        return default_value if self.messages is None else cast(O, self.messages.get(key))

    def put_message(self, key: str, value: object) -> None:
        if self.messages is None:
            object.__setattr__(self, 'messages', {})
        self.messages[key] = value  # type: ignore

    def parent_tree_cursor(self) -> Cursor:
        c = self.parent
        while c is not None:
            if isinstance(c.value, Tree) or c.value == Cursor.ROOT_VALUE:
                return c
            c = c.parent
        raise ValueError("Expected to find parent tree cursor for " + str(self))

    def first_enclosing_or_throw(self, type: Type[P]) -> P:
        result = self.first_enclosing(type)
        if result is None:
            raise ValueError(f"Expected to find enclosing {T.__name__}")
        return result

    def first_enclosing(self, type_: Type[P]) -> Optional[P]:
        c: Optional[Cursor] = self
        while c is not None:
            if isinstance(c.value, type_):
                return c.value
            c = c.parent
        return None

    def fork(self) -> Cursor:
        return Cursor(self.parent.fork(), self.value) if self.parent else self

    def get_path(self) -> Generator[Any]:
        c = self
        while c is not None:
            yield c.value
            c = c.parent

    def get_path_as_cursors(self) -> Generator[Cursor]:
        c = self
        while c is not None:
            yield c
            c = c.parent

    def get_nearest_message(self, key: str) -> Optional[object]:
        for c in self.get_path_as_cursors():
            if c.messages is not None and key in c.messages:
                return c.messages[key]
        return None

    def poll_nearest_message(self, key: str, default_value=None) -> Optional[object]:
        for c in self.get_path_as_cursors():
            if c.messages is not None and key in c.messages:
                return c.messages.pop(key)
        return default_value

    @property
    def parent_or_throw(self) -> Cursor:
        if self.parent is None:
            raise ValueError("Cursor is expected to have a parent:", self)
        return self.parent


class TreeVisitor(ABC, Generic[T, P]):
    _visit_count: int = 0
    _cursor: Cursor = Cursor(None, "root")
    _after_visit: Optional[List[TreeVisitor[Any, P]]] = None

    @classmethod
    def noop(cls):
        return NoopVisitor()

    def is_acceptable(self, source_file: SourceFile, p: P) -> bool:
        return True

    @property
    def cursor(self) -> Cursor:
        return self._cursor

    @cursor.setter
    def cursor(self, cursor: Cursor) -> None:
        self._cursor = cursor

    def pre_visit(self, tree: T, p: P) -> Optional[T]:
        return cast(Optional[T], self.default_value(tree, p))

    def post_visit(self, tree: T, p: P) -> Optional[T]:
        return cast(Optional[T], self.default_value(tree, p))

    def visit(self, tree: Optional[Tree], p: P, parent: Optional[Cursor] = None) -> Optional[T]:
        if parent is not None:
            self._cursor = parent

        if tree is None:
            return cast(Optional[T], self.default_value(None, p))

        top_level = False
        if self._visit_count == 0:
            top_level = True

        self._visit_count += 1
        self.cursor = Cursor(self._cursor, tree)

        t: Optional[T] = None
        is_acceptable = tree.is_acceptable(self, p) and (
                not isinstance(tree, SourceFile) or self.is_acceptable(tree, p))

        try:
            if is_acceptable:
                t = self.pre_visit(cast(T, tree), p)
                if not self._cursor.get_message("STOP_AFTER_PRE_VISIT", False):
                    if t is not None:
                        t = t.accept(self, p)
                    if t is not None:
                        t = self.post_visit(t, p)

            self.cursor = self._cursor.parent  # type: ignore

            if top_level:
                if t is not None and self._after_visit is not None:
                    for v in self._after_visit:
                        if v is not None:
                            v.cursor = self.cursor
                            t = v.visit(t, p)

                self._after_visit = None
                self._visit_count = 0

        except Exception as e:
            if isinstance(e, RecipeRunException):
                raise e

            raise RecipeRunException(e, self.cursor)

        return t if is_acceptable else cast(Optional[T], tree)

    def visit_and_cast(self, tree: Optional[Tree], t_type: Type[T2], p: P) -> T2:
        return cast(T2, self.visit(tree, p))

    def default_value(self, tree: Optional[Tree], p: P) -> Optional[Tree]:
        return tree

    def visit_markers(self, markers: Markers, p: P) -> Markers:
        if markers is None or markers is Markers.EMPTY:
            return Markers.EMPTY
        elif len(markers.markers) == 0:
            return markers
        return markers.with_markers(list_map(lambda m: self.visit_marker(m, p), markers.markers))

    def visit_marker(self, marker: Marker, p: P) -> Marker:
        return marker

    def adapt(self, tree_type, visitor_type: Type[TV]) -> TV:
        # FIXME implement the visitor adapting
        return cast(TV, self)


class NoopVisitor(TreeVisitor[Tree, P]):
    def visit(self, tree: Optional[Tree], p: P, parent: Optional[Cursor] = None) -> Optional[T]:
        return cast(T, tree) if tree else tree
