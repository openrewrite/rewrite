from typing import Optional, TypeVar, TYPE_CHECKING

from rewrite.tree import Tree
from rewrite.utils import list_map
from .support_types import J, JRightPadded, JLeftPadded, JContainer, Space

if TYPE_CHECKING:
    from .visitor import JavaVisitor
    from .tree import *

P = TypeVar('P')
T = TypeVar('T')
J2 = TypeVar('J2', bound=J)


def visit_container(v: 'JavaVisitor', container: Optional[JContainer[J2]], p: P) -> Optional[JContainer[J2]]:
    if container is None:
        return None

    v.cursor = Cursor(v.cursor, container)  # type: ignore[invalid-assignment]  # property setter; ty#1379
    before = v.visit_space(container.before, p)
    js = list_map(lambda el: v.visit_right_padded(el, p), container.padding.elements)
    v.cursor = v.cursor.parent  # type: ignore[invalid-assignment]  # property setter; ty#1379

    return container if js is container.padding.elements and before is container.before else JContainer(before, js, container.markers)


def visit_right_padded(v: 'JavaVisitor', right: Optional[JRightPadded[T]], p: P) -> Optional[JRightPadded[T]]:
    if right is None:
        return None

    t = right.element
    v.cursor = Cursor(v.cursor, right)  # type: ignore[invalid-assignment]  # property setter; ty#1379
    if isinstance(t, Tree):
        t = v.visit_and_cast(t, T, p)  # type: ignore[arg-type]  # TypeVar as Type arg; see https://github.com/astral-sh/ty/issues/501
    v.cursor = v.cursor.parent  # type: ignore[invalid-assignment]  # property setter; ty#1379

    if t is None:
        return None

    right = right.replace(element=t)
    right = right.replace(after=v.visit_space(right.after, p))
    right = right.replace(markers=v.visit_markers(right.markers, p))
    return right


def visit_left_padded(v: 'JavaVisitor', left: Optional[JLeftPadded[T]], p: P) -> Optional[JLeftPadded[T]]:
    if left is None:
        return None

    v.cursor = Cursor(v.cursor, left)  # type: ignore[invalid-assignment]  # property setter; ty#1379
    before = v.visit_space(left.before, p)
    t = left.element
    if isinstance(t, Tree):
        t = v.visit_and_cast(t, T, p)  # type: ignore[arg-type]  # TypeVar as Type arg; see https://github.com/astral-sh/ty/issues/501
    v.cursor = v.cursor.parent  # type: ignore[invalid-assignment]  # property setter; ty#1379

    if left.element is t and before is left.before:
        return left
    elif t is None:
        return None

    return JLeftPadded(before, t, left.markers)


def visit_space(v: 'JavaVisitor', space: Optional[Space], p: P) -> Space:
    # FIXME support Javadoc
    return space if space is not None else Space.EMPTY


def with_name(method: 'MethodInvocation', name: 'Identifier') -> 'MethodInvocation':
    # FIXME add type attribution logic
    return method if name is method.name else replace(method, _name=name)
