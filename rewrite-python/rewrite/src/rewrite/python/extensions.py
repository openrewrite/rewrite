from typing import Optional, TypeVar, TYPE_CHECKING

from rewrite.java.tree import extensions, J, JContainer, JRightPadded, JLeftPadded, Space

if TYPE_CHECKING:
    from .visitor import PythonVisitor

T = TypeVar('T')
J2 = TypeVar('J2', bound=J)


def visit_container(v: 'PythonVisitor', container: Optional[JContainer[J2]], p) -> Optional[JContainer[J2]]:
    return extensions.visit_container(v, container, p)


def visit_right_padded(v: 'PythonVisitor', right: Optional[JRightPadded[T]], p) -> Optional[JRightPadded[T]]:
    return extensions.visit_right_padded(v, right, p)


def visit_left_padded(v: 'PythonVisitor', left: Optional[JLeftPadded[T]], p) -> Optional[JLeftPadded[T]]:
    return extensions.visit_left_padded(v, left, p)


def visit_space(v: 'PythonVisitor', space: Optional[Space], p):
    return extensions.visit_space(v, space, p)
