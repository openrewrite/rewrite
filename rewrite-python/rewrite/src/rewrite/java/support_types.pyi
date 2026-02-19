# Auto-generated stub file for IDE autocomplete support.
# Do not edit manually - regenerate with: python scripts/generate_stubs.py

from dataclasses import dataclass
from typing import Any, ClassVar, List, Optional, TypeVar, Generic
from typing_extensions import Self
from uuid import UUID
import weakref

P = TypeVar('P')
T = TypeVar('T')
J2 = TypeVar('J2', bound=J)
J3 = TypeVar('J3', bound=J)

from abc import abstractmethod, ABC
from enum import Enum, auto
from rewrite import Markers
from rewrite import Tree, SourceFile, TreeVisitor
from rewrite.utils import replace_if_changed

class J(Tree):
    @property
    def prefix(self) -> Space: ...
    def is_acceptable(self, v: TreeVisitor[Any, P], p: P) -> bool: ...
    def accept(self, v: TreeVisitor[Any, P], p: P) -> Optional[Any]: ...
    def accept_java(self, v: 'JavaVisitor[P]', p: P) -> Optional['J']: ...

class JavaSourceFile(J, SourceFile):
    pass

class Expression(J):
    @property
    def type(self) -> Optional[JavaType]: ...

class Statement(J):
    pass

class TypedTree(J):
    @property
    def type(self) -> Optional[JavaType]: ...

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
            Class: Kind
            Enum: Kind
            Interface: Kind
            Annotation: Kind
            Record: Kind



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
            Invariant: Variance
            Covariant: Variance
            Contravariant: Variance



    class Primitive(Enum):
        Boolean: Primitive
        Byte: Primitive
        Char: Primitive
        Double: Primitive
        Float: Primitive
        Int: Primitive
        Long: Primitive
        Short: Primitive
        Void: Primitive
        String: Primitive
        None_: Primitive
        Null: Primitive

    @dataclass(frozen=True)
    class Method:
        _flags_bit_map: int = ...
        _declaring_type: Optional[JavaType.FullyQualified] = ...
        _name: str = ...
        _return_type: Optional[JavaType] = ...
        _parameter_names: Optional[List[str]] = ...
        _parameter_types: Optional[List[JavaType]] = ...
        _thrown_exceptions: Optional[List[JavaType]] = ...
        _annotations: Optional[List[JavaType.FullyQualified]] = ...
        _default_value: Optional[List[str]] = ...
        _declared_formal_type_names: Optional[List[str]] = ...

        def replace(self, **kwargs: Any) -> Self: ...

        @property
        def flags_bit_map(self) -> int: ...
        @property
        def declaring_type(self) -> Optional[JavaType.FullyQualified]: ...
        @property
        def name(self) -> str: ...
        @property
        def return_type(self) -> Optional[JavaType]: ...
        @property
        def parameter_names(self) -> Optional[List[str]]: ...
        @property
        def parameter_types(self) -> Optional[List[JavaType]]: ...
        @property
        def thrown_exceptions(self) -> Optional[List[JavaType]]: ...
        @property
        def annotations(self) -> Optional[List[JavaType.FullyQualified]]: ...
        @property
        def default_value(self) -> Optional[List[str]]: ...
        @property
        def declared_formal_type_names(self) -> Optional[List[str]]: ...

    class Variable:
        pass

    class Array:
        pass


@dataclass(frozen=True)
class Comment(ABC):
    _text: str
    _suffix: str
    _markers: Markers

    def replace(self, **kwargs: Any) -> 'Comment': ...

    @property
    def multiline(self) -> bool: ...
    @property
    def text(self) -> str: ...
    @property
    def suffix(self) -> str: ...
    @property
    def markers(self) -> Markers: ...

@dataclass(frozen=True)
class TextComment(Comment):
    _multiline: bool

    def replace(self, **kwargs: Any) -> Self: ...

    @property
    def multiline(self) -> bool: ...

@dataclass(frozen=True)
class Space:
    EMPTY: ClassVar[Space]
    SINGLE_SPACE: ClassVar[Space]

    _comments: List[Comment]
    _whitespace: Optional[str]

    def replace(self, **kwargs: Any) -> 'Space': ...

    @classmethod
    def first_prefix(cls, trees: Optional[Iterable[J]]) -> Space: ...
    @classmethod
    def format_first_prefix(cls, trees: List[J2], prefix: Space) -> List[J2]: ...

    @property
    def comments(self) -> List[Comment]: ...
    @property
    def whitespace(self) -> str: ...
    @property
    def indent(self) -> str: ...
    @property
    def last_whitespace(self) -> str: ...

    def is_empty(self) -> bool: ...

@dataclass(frozen=True)
class JRightPadded(Generic[T]):
    _element: T
    _after: Space
    _markers: Markers

    def replace(self, **kwargs: Any) -> 'JRightPadded[T]': ...

    @classmethod
    def get_elements(cls, padded_list: List[JRightPadded[T]]) -> List[T]: ...
    @classmethod
    def merge_elements(cls, before: List[JRightPadded[J2]], elements: List[Union[J2, JRightPadded[J2]]]) -> List[JRightPadded[J2]]: ...

    @property
    def element(self) -> T: ...
    @property
    def after(self) -> Space: ...
    @property
    def markers(self) -> Markers: ...

@dataclass(frozen=True)
class JLeftPadded(Generic[T]):
    _before: Space
    _element: T
    _markers: Markers

    def replace(self, **kwargs: Any) -> 'JLeftPadded[T]': ...

    @property
    def before(self) -> Space: ...
    @property
    def element(self) -> T: ...
    @property
    def markers(self) -> Markers: ...

@dataclass(frozen=True)
class JContainer(Generic[J2]):
    @dataclass(frozen=True)
    class PaddingHelper(Generic[J3]):
        _t: JContainer[J3]

        def replace(self, **kwargs: Any) -> JContainer[J3]: ...

        @property
        def elements(self) -> List[JRightPadded[J3]]: ...

    _before: Space
    _elements: List[JRightPadded[J2]]
    _markers: Markers
    _padding: Optional[weakref.ReferenceType[JContainer.PaddingHelper[J2]]] = ...
    _EMPTY: Optional[JContainer[J]] = ...

    def replace(self, **kwargs: Any) -> 'JContainer[J2]': ...

    @classmethod
    def build_nullable(cls, before: Optional[JContainer[J2]], elements: Optional[List[J2]]) -> Optional[JContainer[J2]]: ...
    @classmethod
    def empty(cls) -> JContainer[J2]: ...

    @property
    def before(self) -> Space: ...
    @property
    def elements(self) -> List[J2]: ...
    @property
    def markers(self) -> Markers: ...
    @property
    def padding(self) -> JContainer.PaddingHelper[J2]: ...
