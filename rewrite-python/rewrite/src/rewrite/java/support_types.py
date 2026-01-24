from __future__ import annotations

import weakref
from abc import abstractmethod, ABC
from dataclasses import dataclass, field
from enum import Enum, auto
from typing import List, Optional, TypeVar, Generic, ClassVar, Dict, Any, TYPE_CHECKING, Iterable, Union, cast
from uuid import UUID

from rewrite import Markers
from rewrite import Tree, SourceFile, TreeVisitor
from rewrite.utils import replace_if_changed

if TYPE_CHECKING:
    from .visitor import JavaVisitor

P = TypeVar('P')


class J(Tree):
    @property
    @abstractmethod
    def prefix(self) -> Space:
        ...

    def is_acceptable(self, v: TreeVisitor[Any, P], p: P) -> bool:
        from .visitor import JavaVisitor
        return v.is_adaptable_to(JavaVisitor)

    def accept(self, v: TreeVisitor[Any, P], p: P) -> Optional[Any]:
        from .visitor import JavaVisitor
        return self.accept_java(v.adapt(J, JavaVisitor), p)

    def accept_java(self, v: 'JavaVisitor[P]', p: P) -> Optional['J']:
        ...


@dataclass(frozen=True)
class Comment(ABC):
    @property
    @abstractmethod
    def multiline(self) -> bool:
        ...

    _text: str

    @property
    def text(self) -> str:
        return self._text

    _suffix: str

    @property
    def suffix(self) -> str:
        return self._suffix

    _markers: Markers

    @property
    def markers(self) -> Markers:
        return self._markers

    def replace(self, **kwargs) -> 'Comment':
        """Replace fields on this Comment, returning self if nothing changed."""
        return replace_if_changed(self, **kwargs)


@dataclass(frozen=True)
class TextComment(Comment):
    _multiline: bool

    @property
    def multiline(self) -> bool:
        return self._multiline

    # IMPORTANT: This explicit constructor aligns the parameter order with the Java side
    def __init__(self, _multiline: bool, _text: str, _suffix: str, _markers: Markers) -> None:
        object.__setattr__(self, '_multiline', _multiline)
        object.__setattr__(self, '_text', _text)
        object.__setattr__(self, '_suffix', _suffix)
        object.__setattr__(self, '_markers', _markers)


@dataclass(frozen=True)
class Space:
    _comments: List[Comment]

    @property
    def comments(self) -> List[Comment]:
        return self._comments

    _whitespace: Optional[str]

    @property
    def whitespace(self) -> str:
        return self._whitespace if self._whitespace is not None else ""

    def replace(self, **kwargs) -> 'Space':
        """Replace fields on this Space, returning self if nothing changed."""
        return replace_if_changed(self, **kwargs)

    def is_empty(self) -> bool:
        return len(self._comments) == 0 and (self._whitespace is None or self._whitespace == '')

    @classmethod
    def first_prefix(cls, trees: Optional[Iterable[J]]) -> Space:
        return Space.EMPTY if trees is None or not trees else next(iter(trees)).prefix

    @classmethod
    def format_first_prefix(cls, trees: List[J2], prefix: Space) -> List[J2]:
        if trees and next(iter(trees)).prefix != prefix:
            formatted_trees = list(trees)
            formatted_trees[0] = cast(J2, formatted_trees[0].replace(prefix=prefix))
            return formatted_trees
        return trees

    @property
    def indent(self) -> str:
        """
        The indentation after the last newline of either the last comment's suffix
        or the global whitespace if no comments exist.
        """
        return self._get_whitespace_indent(self.last_whitespace)

    @property
    def last_whitespace(self) -> str:
        """
        The raw suffix from the last comment if it exists, otherwise the global
        whitespace (or empty string if whitespace is None).
        """
        if self._comments:
            return self._comments[-1].suffix
        return self._whitespace if self._whitespace is not None else ""

    @staticmethod
    def _get_whitespace_indent(whitespace: Optional[str]) -> str:
        """
        A helper method that extracts everything after the last newline character
        in `whitespace`. If no newline is present, returns `whitespace` as-is.
        If the last newline is at the end, returns an empty string.
        """
        if not whitespace:
            return ""
        last_newline = whitespace.rfind('\n')
        return whitespace if last_newline == -1 else whitespace[last_newline + 1:]

    EMPTY: ClassVar[Space]
    SINGLE_SPACE: ClassVar[Space]


Space.EMPTY = Space([], '')
Space.SINGLE_SPACE = Space([], ' ')


class JavaSourceFile(J, SourceFile):
    pass


class Expression(J):
    @property
    def type(self) -> Optional[JavaType]:
        return None


class Statement(J):
    pass


class TypedTree(J):
    @property
    def type(self) -> Optional[JavaType]:
        return None


class NameTree(TypedTree):
    pass


class TypeTree(NameTree):
    pass


class Loop(Statement):
    pass


class MethodCall(Expression):
    pass


class JavaType(ABC):
    class FullyQualified:
        class Kind(Enum):
            Class = 0
            Enum = 1
            Interface = 2
            Annotation = 3
            Record = 4

    class Unknown(FullyQualified):
        pass

    class Class(FullyQualified):
        _flags_bit_map: int
        _fully_qualified_name: str
        _kind: FullyQualified.Kind
        _type_parameters: Optional[List[JavaType]]
        _supertype: Optional[JavaType.FullyQualified]
        _owning_class: Optional[JavaType.FullyQualified]
        _annotations: Optional[List[JavaType.FullyQualified]]
        _interfaces: Optional[List[JavaType.FullyQualified]]
        _members: Optional[List[JavaType.Variable]]
        _methods: Optional[List[JavaType.Method]]

    class ShallowClass(Class):
        pass

    class Parameterized(FullyQualified):
        _type: JavaType.FullyQualified
        _type_parameters: Optional[List[JavaType]]

    class GenericTypeVariable:
        class Variance(Enum):
            Invariant = 0
            Covariant = 1
            Contravariant = 2

    class Primitive(Enum):
        Boolean = 0
        Byte = 1
        Char = 2
        Double = 3
        Float = 4
        Int = 5
        Long = 6
        Short = 7
        Void = 8
        String = 9
        None_ = 10
        Null = 11

        @classmethod
        def _missing_(cls, value):
            if value is None:
                return cls.None_
            return super()._missing_(value)

    @dataclass
    class Method:
        _flags_bit_map: int = field(default=0)
        _declaring_type: Optional[JavaType.FullyQualified] = field(default=None)
        _name: str = field(default="")
        _return_type: Optional[JavaType] = field(default=None)
        _parameter_names: Optional[List[str]] = field(default=None)
        _parameter_types: Optional[List[JavaType]] = field(default=None)
        _thrown_exceptions: Optional[List[JavaType]] = field(default=None)
        _annotations: Optional[List[JavaType.FullyQualified]] = field(default=None)
        _default_value: Optional[List[str]] = field(default=None)
        _declared_formal_type_names: Optional[List[str]] = field(default=None)

        @property
        def flags_bit_map(self) -> int:
            return self._flags_bit_map

        @property
        def declaring_type(self) -> Optional[JavaType.FullyQualified]:
            return self._declaring_type

        @property
        def name(self) -> str:
            return self._name

        @property
        def return_type(self) -> Optional[JavaType]:
            return self._return_type

        @property
        def parameter_names(self) -> Optional[List[str]]:
            return self._parameter_names

        @property
        def parameter_types(self) -> Optional[List[JavaType]]:
            return self._parameter_types

        @property
        def thrown_exceptions(self) -> Optional[List[JavaType]]:
            return self._thrown_exceptions

        @property
        def annotations(self) -> Optional[List[JavaType.FullyQualified]]:
            return self._annotations

        @property
        def default_value(self) -> Optional[List[str]]:
            return self._default_value

        @property
        def declared_formal_type_names(self) -> Optional[List[str]]:
            return self._declared_formal_type_names

    class Variable:
        pass

    class Array:
        pass


T = TypeVar('T')
J2 = TypeVar('J2', bound=J)
J3 = TypeVar('J3', bound=J)


@dataclass(frozen=True)
class JRightPadded(Generic[T]):
    _element: T

    @property
    def element(self) -> T:
        return self._element

    _after: Space

    @property
    def after(self) -> Space:
        return self._after

    _markers: Markers

    @property
    def markers(self) -> Markers:
        return self._markers

    def __eq__(self, other) -> bool:
        if isinstance(other, JRightPadded):
            return self._element == other._element
        return self._element == other

    def __hash__(self) -> int:
        return hash(self._element)

    def replace(self, **kwargs) -> 'JRightPadded[T]':
        """Replace fields on this JRightPadded, returning self if nothing changed."""
        return replace_if_changed(self, **kwargs)

    @classmethod
    def get_elements(cls, padded_list: List[JRightPadded[T]]) -> List[T]:
        return [x.element for x in padded_list]

    @classmethod
    def merge_elements(cls, before: List[JRightPadded[J2]], elements: List[Union[J2, JRightPadded[J2]]]) -> List[JRightPadded[J2]]:
        # Helper to extract element - handles both wrapped JRightPadded and unwrapped elements
        def get_element(t):
            return t.element if isinstance(t, JRightPadded) else t

        # a cheaper check for the most common case when there are no changes
        if len(elements) == len(before):
            has_changes = False
            for i in range(len(before)):
                if before[i].element is not get_element(elements[i]):
                    has_changes = True
                    break
            if not has_changes:
                return before
        elif not elements:
            return []

        after: List[JRightPadded[J2]] = []
        before_by_id: Dict[UUID, JRightPadded[J2]] = {}

        for j in before:
            if j.element.id in before_by_id:
                raise Exception("Duplicate key")
            before_by_id[j.element.id] = j

        for t in elements:
            elem = get_element(t)
            found = before_by_id.get(elem.id)
            if found is not None:
                after.append(found.replace(element=elem))
            else:
                after.append(JRightPadded(elem, Space.EMPTY, Markers.EMPTY))

        return after



@dataclass(frozen=True)
class JLeftPadded(Generic[T]):
    _before: Space

    @property
    def before(self) -> Space:
        return self._before

    _element: T

    @property
    def element(self) -> T:
        return self._element

    _markers: Markers

    @property
    def markers(self) -> Markers:
        return self._markers

    def __eq__(self, other) -> bool:
        if isinstance(other, JLeftPadded):
            return self._element == other._element
        return self._element == other

    def __hash__(self) -> int:
        return hash(self._element)

    def replace(self, **kwargs) -> 'JLeftPadded[T]':
        """Replace fields on this JLeftPadded, returning self if nothing changed."""
        return replace_if_changed(self, **kwargs)



@dataclass(frozen=True)
class JContainer(Generic[J2]):
    _before: Space

    @property
    def before(self) -> Space:
        return self._before

    _elements: List[JRightPadded[J2]]

    @property
    def elements(self) -> List[J2]:
        return JRightPadded.get_elements(self._elements)

    _markers: Markers

    @property
    def markers(self) -> Markers:
        return self._markers

    def __eq__(self, other) -> bool:
        if isinstance(other, JContainer):
            return self._elements == other._elements
        if isinstance(other, list):
            return self.elements == other
        return False

    def __hash__(self) -> int:
        return hash(tuple(self._elements))

    def replace(self, **kwargs) -> 'JContainer[J2]':
        """Replace fields on this JContainer, returning self if nothing changed."""
        return replace_if_changed(self, **kwargs)

    @dataclass
    class PaddingHelper(Generic[J3]):
        _t: JContainer[J3]

        @property
        def elements(self) -> List[JRightPadded[J3]]:
            return self._t._elements

        def replace(self, **kwargs) -> JContainer[J3]:
            """Replace fields of the container using keyword arguments."""
            if 'elements' in kwargs:
                elements = kwargs['elements']
                if self._t._elements is elements:
                    return self._t
                return JContainer(self._t._before, elements, self._t._markers)
            return self._t

    _padding: Optional[weakref.ReferenceType[JContainer.PaddingHelper[J2]]] = None

    @property
    def padding(self) -> JContainer.PaddingHelper[J2]:
        p: Optional[JContainer.PaddingHelper[J2]]
        if self._padding is None:
            p = JContainer.PaddingHelper(self)
            object.__setattr__(self, '_padding', weakref.ref(p))
        else:
            p = self._padding()
            # noinspection PyProtectedMember
            if p is None or p._t != self:
                p = JContainer.PaddingHelper(self)
                object.__setattr__(self, '_padding', weakref.ref(p))
        return p

    @classmethod
    def build_nullable(cls, before: Optional[JContainer[J2]], elements: Optional[List[J2]]) -> Optional[
        JContainer[J2]]:
        if elements is None or elements == []:
            return None
        if before is None:
            return JContainer(Space.EMPTY, JRightPadded.merge_elements([], elements), Markers.EMPTY)
        return before.padding.replace(elements=JRightPadded.merge_elements(before._elements, elements))

    _EMPTY: Optional[JContainer[J]] = None

    @classmethod
    def empty(cls) -> JContainer[J2]:
        if cls._EMPTY is None:
            cls._EMPTY = JContainer(Space.EMPTY, [], Markers.EMPTY)
        return cls._EMPTY  # type: ignore[return-value]  # _EMPTY is JContainer[J] but J2 is bound to J

