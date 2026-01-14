from typing import Optional, TypeVar, TYPE_CHECKING

from rewrite.java.tree import extensions, J, JContainer, JRightPadded, JLeftPadded, Space
from .support_types import PyContainer, PyRightPadded, PyLeftPadded, PySpace

if TYPE_CHECKING:
    from .visitor import PythonVisitor

T = TypeVar('T')
J2 = TypeVar('J2', bound=J)


# noinspection PyUnusedLocal
def visit_container(v: 'PythonVisitor', container: Optional[JContainer[J2]],
                    loc: PyContainer.Location, p) -> Optional[JContainer[J2]]:
    return extensions.visit_container(v, container, JContainer.Location.LANGUAGE_EXTENSION, p)


# noinspection PyUnusedLocal
def visit_right_padded(v: 'PythonVisitor', right: Optional[JRightPadded[T]],
                       loc: PyRightPadded.Location, p) -> Optional[JRightPadded[T]]:
    return extensions.visit_right_padded(v, right, JRightPadded.Location.LANGUAGE_EXTENSION, p)


# noinspection PyUnusedLocal
def visit_left_padded(v: 'PythonVisitor', left: Optional[JLeftPadded[T]],
                      loc: PyLeftPadded.Location, p) -> Optional[JLeftPadded[T]]:
    return extensions.visit_left_padded(v, left, JLeftPadded.Location.LANGUAGE_EXTENSION, p)


# noinspection PyUnusedLocal
def visit_space(v: 'PythonVisitor', space: Optional[Space],
                loc: PySpace.Location, p):
    return extensions.visit_space(v, space, Space.Location.LANGUAGE_EXTENSION, p)
