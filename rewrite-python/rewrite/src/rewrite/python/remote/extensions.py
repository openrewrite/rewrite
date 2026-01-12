from functools import lru_cache
from operator import attrgetter
from typing import Optional, Type, Callable, TypeVar

from rewrite_remote import SenderContext, ReceiverContext

from rewrite.java import *
from rewrite.python.tree import PyComment

T = TypeVar('T')

# IMPORTANT: This duplicates everything from Java's `extensions` module, because we use `PyComment` rather than `TextComment`
# A better solution to this would be nice

def receive_container(container: Optional[JContainer[T]], _: Optional[str], ctx: ReceiverContext) -> JContainer[T]:
    if container is not None:
        container = container.replace(before=ctx.receive_node(container.before, receive_space))
        container = container.padding.replace(elements=ctx.receive_nodes(container.padding.elements, receive_right_padded_tree))
        container = container.replace(markers=ctx.receive_node(container.markers, ctx.receive_markers))
    else:
        container = JContainer(
            ctx.receive_node(None, receive_space),
            ctx.receive_nodes(None, receive_right_padded_tree),
            ctx.receive_node(None, ctx.receive_markers)
        )
    return container


def send_container(container: JContainer[T], ctx: SenderContext):
    ctx.send_node(container, attrgetter('_before'), send_space)
    ctx.send_nodes(container, attrgetter('_elements'), send_right_padded, lambda t: t.element.id)
    ctx.send_node(container, attrgetter('_markers'), ctx.send_markers)


def send_left_padded(left_padded: JLeftPadded[T], ctx: SenderContext):
    ctx.send_node(left_padded, attrgetter('_before'), send_space)
    if isinstance(left_padded.element, (J, Space)):
        ctx.send_node(left_padded, attrgetter('_element'), ctx.send_tree)
    else:
        ctx.send_value(left_padded, attrgetter('_element'))
    ctx.send_node(left_padded, attrgetter('_markers'), ctx.send_markers)


def send_right_padded(right_padded: JRightPadded[T], ctx: SenderContext):
    if isinstance(right_padded.element, J):
        ctx.send_node(right_padded, attrgetter('_element'), ctx.send_tree)
    elif isinstance(right_padded.element, Space):
        ctx.send_node(right_padded, attrgetter('_element'), send_space)
    else:
        ctx.send_value(right_padded, attrgetter('_element'))
    ctx.send_node(right_padded, attrgetter('_after'), send_space)
    ctx.send_node(right_padded, attrgetter('_markers'), ctx.send_markers)


def receive_space(space: Optional[Space], _: Optional[str], ctx: ReceiverContext) -> Space:
    if space is not None:
        space = space.replace(comments=ctx.receive_nodes(space.comments, receive_comment))
        space = space.replace(whitespace=ctx.receive_value(space.whitespace, str))
    else:
        space = Space(
            ctx.receive_nodes(None, receive_comment),
            ctx.receive_value(None, str)
        )

    return space


def receive_comment(comment: Optional[Comment], _: Optional[str], ctx: ReceiverContext) -> Comment:
    if comment:
        comment = comment.replace(text=ctx.receive_value(comment.text, str))
        comment = comment.replace(suffix=ctx.receive_value(comment.suffix, str))
        comment = comment.replace(aligned_to_indent=ctx.receive_value(comment.aligned_to_indent, bool))
        comment = comment.replace(markers=ctx.receive_node(comment.markers, ctx.receive_markers))
    else:
        comment = PyComment(
            ctx.receive_value(None, str),
            ctx.receive_value(None, str),
            ctx.receive_value(None, bool),
            ctx.receive_node(None, ctx.receive_markers)
        )
    return comment


def send_space(space: Space, ctx: SenderContext):
    ctx.send_nodes(space, attrgetter('_comments'), send_comment, lambda x: x)
    ctx.send_value(space, attrgetter('_whitespace'))

def send_comment(comment: PyComment, ctx: SenderContext):
    ctx.send_value(comment, attrgetter('_text'))
    ctx.send_value(comment, attrgetter('_suffix'))
    ctx.send_value(comment, attrgetter('_aligned_to_indent'))
    ctx.send_node(comment, attrgetter('_markers'), ctx.send_markers)


@lru_cache(maxsize=10)
def left_padded_value_receiver(type_: Type) -> Callable[[Optional[JLeftPadded[T]], Optional[str], ReceiverContext], JLeftPadded[T]]:
    def receiver(left_padded: Optional[JLeftPadded[T]], _: Optional[str], ctx: ReceiverContext) -> JLeftPadded[T]:
        if left_padded is not None:
            left_padded = left_padded.replace(before=ctx.receive_node(left_padded.before, receive_space))
            left_padded = left_padded.replace(element=ctx.receive_value(left_padded.element, type_))
            left_padded = left_padded.replace(markers=ctx.receive_node(left_padded.markers, ctx.receive_markers))
        else:
            left_padded = JLeftPadded(
                ctx.receive_node(None, receive_space),
                ctx.receive_value(None, type_),
                ctx.receive_node(None, ctx.receive_markers)
            )
        return left_padded
    return receiver


@lru_cache(maxsize=10)
def left_padded_node_receiver(type_: Type) -> Callable[[Optional[JLeftPadded[T]], Optional[str], ReceiverContext], JLeftPadded[T]]:
    if type_ is Space:
        def space_receiver(left_padded: Optional[JLeftPadded[T]], _: Optional[str], ctx: ReceiverContext) -> JLeftPadded[T]:
            if left_padded is not None:
                left_padded = left_padded.replace(before=ctx.receive_node(left_padded.before, receive_space))
                left_padded = left_padded.replace(element=ctx.receive_node(left_padded.element, receive_space))
                left_padded = left_padded.replace(markers=ctx.receive_node(left_padded.markers, ctx.receive_markers))
            else:
                left_padded = JLeftPadded(
                    ctx.receive_node(None, receive_space),
                    ctx.receive_node(None, receive_space),
                    ctx.receive_node(None, ctx.receive_markers)
                )
            return left_padded
        return space_receiver
    else:
        raise ValueError("Only Space is supported")


def receive_left_padded_tree(left_padded: Optional[JLeftPadded[T]], _: Optional[str], ctx: ReceiverContext) -> JLeftPadded[T]:
    if left_padded is not None:
        left_padded = left_padded.replace(before=ctx.receive_node(left_padded.before, receive_space))
        left_padded = left_padded.replace(element=ctx.receive_node(left_padded.element, ctx.receive_tree))
        left_padded = left_padded.replace(markers=ctx.receive_node(left_padded.markers, ctx.receive_markers))
    else:
        left_padded = JLeftPadded(
            ctx.receive_node(None, receive_space),
            ctx.receive_node(None, ctx.receive_tree),
            ctx.receive_node(None, ctx.receive_markers)
        )
    return left_padded


@lru_cache(maxsize=10)
def right_padded_value_receiver(type_: Type) -> Callable[[Optional[JRightPadded[T]], Optional[str], ReceiverContext], JRightPadded[T]]:
    def receiver(right_padded: Optional[JRightPadded[T]], _: Optional[str], ctx: ReceiverContext) -> JRightPadded[T]:
        if right_padded is not None:
            right_padded = right_padded.replace(element=ctx.receive_value(right_padded.element, type_))
            right_padded = right_padded.replace(after=ctx.receive_node(right_padded.after, receive_space))
            right_padded = right_padded.replace(markers=ctx.receive_node(right_padded.markers, ctx.receive_markers))
        else:
            right_padded = JRightPadded(
                ctx.receive_value(None, type_),
                ctx.receive_node(None, receive_space),
                ctx.receive_node(None, ctx.receive_markers)
            )
        return right_padded
    return receiver


@lru_cache(maxsize=10)
def right_padded_node_receiver(type_: Type) -> Callable[[Optional[JRightPadded[T]], Optional[str], ReceiverContext], JRightPadded[T]]:
    raise ValueError("Not implemented")


def receive_right_padded_tree(right_padded: Optional[JRightPadded[T]], _: Optional[str],
                              ctx: ReceiverContext) -> JRightPadded[T]:
    if right_padded is not None:
        right_padded = right_padded.replace(element=ctx.receive_node(right_padded.element, ctx.receive_tree))
        right_padded = right_padded.replace(after=ctx.receive_node(right_padded.after, receive_space))
        right_padded = right_padded.replace(markers=ctx.receive_node(right_padded.markers, ctx.receive_markers))
    else:
        right_padded = JRightPadded(
            ctx.receive_node(None, ctx.receive_tree),
            ctx.receive_node(None, receive_space),
            ctx.receive_node(None, ctx.receive_markers)
        )
    return right_padded
