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
    parent: Optional[Cursor]
    value: object
    messages: Optional[Dict[str, object]] = None

    ROOT_VALUE: ClassVar[str] = "root"

    def get_message(self, key: str, default_value: O) -> O:
        return default_value if self.messages is None else cast(O, self.messages.get(key))

    def put_message(self, key: str, value: object) -> None:
        messages = self.messages
        if messages is None:
            messages = {}
            object.__setattr__(self, 'messages', messages)
        messages[key] = value

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
            raise ValueError(f"Expected to find enclosing {type.__name__}")
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

    # Maps a language-specific visitor base class (e.g. JavaVisitor, PythonVisitor)
    # to a class that wraps a generic TreeVisitor as that visitor type. Populated
    # by language modules at import time via :meth:`register_adapter`. See
    # :meth:`adapt` for the use-case.
    _adapter_registry: ClassVar[Dict[Type['TreeVisitor[Any, Any]'], Type['TreeVisitor[Any, Any]']]] = {}

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

    def set_cursor(self, cursor: Cursor) -> None:
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

            self.cursor = self._cursor.parent  # ty: ignore[invalid-assignment]  # property setter (ty#628)

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

    def visit_and_cast(self, tree: Optional[Tree], t_type: Type[T2], p: P) -> Optional[T2]:
        return cast(Optional[T2], self.visit(tree, p))

    def default_value(self, tree: Optional[Tree], p: P) -> Optional[Tree]:
        return tree

    def visit_markers(self, markers: Markers, p: P) -> Markers:
        if markers is None or markers is Markers.EMPTY:
            return Markers.EMPTY
        ms = markers._markers  # bypass @property in hot path
        if len(ms) == 0:
            return markers
        return markers.replace(markers=list_map(lambda m: self.visit_marker(m, p), ms))

    def visit_marker(self, marker: Marker, p: P) -> Marker:
        return marker

    def stop_after_pre_visit(self) -> None:
        """
        Stop visiting after pre_visit returns, preventing accept() and post_visit() from being called.
        Call this in pre_visit when you only want to process at a high level without traversing children.
        """
        self._cursor.put_message("STOP_AFTER_PRE_VISIT", True)

    def is_adaptable_to(self, adapt_to: Type['TreeVisitor[Any, Any]']) -> bool:
        """
        Check if this visitor can be adapted to the given visitor type.

        A visitor is adaptable if:
        1. It is already an instance of the target type, OR
        2. A registered adapter exists for the target type (the bare-TreeVisitor
           case — see :meth:`adapt`), OR
        3. The visitor is itself an adapter wrapping a generic TreeVisitor.
        """
        if isinstance(self, adapt_to):
            return True
        if getattr(self, '_wrapped', None) is not None:
            return True
        for cls in adapt_to.__mro__:
            if cls in TreeVisitor._adapter_registry:
                return True
        return False

    def adapt(self, tree_type, visitor_type: Type[TV]) -> TV:
        """Return a visitor of ``visitor_type`` that delegates to ``self``.

        Language-specific LST nodes route through this when their ``accept``
        is called with a non-language-specific visitor — e.g. ``J.accept`` does
        ``self.accept_java(v.adapt(J, JavaVisitor), p)``. Without an adapter,
        a bare ``TreeVisitor`` would flow into ``accept_java`` and fail with
        ``AttributeError`` on the first language-specific dispatch (e.g.
        ``visit_compilation_unit``), since the bare base class doesn't
        implement those methods.

        The returned adapter is-a ``visitor_type`` (so the language-specific
        ``visit_*`` defaults are available for child traversal), but it
        forwards ``pre_visit`` / ``post_visit`` / ``default_value`` /
        ``is_acceptable`` and the ``_cursor`` / ``_visit_count`` /
        ``_after_visit`` state to the wrapped visitor — so any user-defined
        logic on the original generic visitor still runs against the right
        cursor and observes traversal via its own ``pre_visit`` / ``post_visit``.

        If ``self`` is already an instance of ``visitor_type`` (the common case
        where the user passed a language-specific visitor), no adapter is
        created. If ``self`` is itself an adapter wrapping some other visitor,
        the wrapped visitor is unwrapped first so we don't stack adapters when
        a single recipe traversal crosses language boundaries (e.g. visiting a
        Py node from inside a JavaVisitor adapter chain).

        Adapter classes are registered by each language's visitor module via
        :meth:`register_adapter`; if no adapter is registered for the requested
        visitor type, ``self`` is returned unchanged for backwards
        compatibility.
        """
        if isinstance(self, visitor_type):
            return cast(TV, self)
        target: TreeVisitor = self
        wrapped = getattr(self, '_wrapped', None)
        if wrapped is not None:
            target = wrapped
            if isinstance(target, visitor_type):
                return cast(TV, target)
        for cls in visitor_type.__mro__:
            adapter_cls = TreeVisitor._adapter_registry.get(cls)
            if adapter_cls is not None:
                return cast(TV, adapter_cls(target))
        return cast(TV, self)

    @classmethod
    def register_adapter(cls, target_visitor_type: Type['TreeVisitor[Any, Any]'],
                         adapter_cls: Type['TreeVisitor[Any, Any]']) -> None:
        """Register an adapter class that wraps a generic TreeVisitor as ``target_visitor_type``.

        Each language module (``rewrite.java.visitor``, ``rewrite.python.visitor``,
        ...) calls this once at import time to make its language visitor reachable
        via :meth:`adapt`. The adapter must subclass ``target_visitor_type`` and
        accept a single ``wrapped`` argument in its constructor.
        """
        cls._adapter_registry[target_visitor_type] = adapter_cls


class NoopVisitor(TreeVisitor[Tree, P]):
    def visit(self, tree: Optional[Tree], p: P, parent: Optional[Cursor] = None) -> Optional[T]:
        return cast(T, tree) if tree else None
