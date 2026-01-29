import inspect
from dataclasses import replace as dataclass_replace
from typing import Any, Callable, TypeVar, List, Union, cast
from uuid import UUID, uuid4

T = TypeVar('T')


def replace_if_changed(obj: T, **kwargs) -> T:
    """Replace fields on a dataclass, returning the original if nothing changed.

    Handles the convention where properties use public names (e.g., 'prefix')
    but dataclass fields use private names (e.g., '_prefix').

    Also handles Python keyword conflicts where parameters use trailing underscore
    (e.g., 'from_' maps to field '_from').

    This is critical for performance - visitor traversals call replace() on every
    node, and returning the same object when nothing changes avoids unnecessary
    allocations and GC pressure.

    Args:
        obj: The dataclass instance to potentially replace
        **kwargs: Field names and their new values (public or private names)

    Returns:
        The original object if no values changed, otherwise a new instance
    """
    if not kwargs:
        return obj

    # Map public property names to private field names and check for changes
    mapped_kwargs = {}
    changed = False
    for key, value in kwargs.items():
        if not key.startswith('_'):
            # Handle Python keyword conflicts: from_ -> _from
            base_key = key.rstrip('_')
            private_key = f'_{base_key}'
            if hasattr(obj, private_key):
                mapped_kwargs[private_key] = value
                # Use 'or' for short-circuit evaluation - skips getattr() once changed is True
                changed = changed or getattr(obj, private_key) is not value
            else:
                mapped_kwargs[key] = value
                changed = changed or getattr(obj, key) is not value
        else:
            mapped_kwargs[key] = value
            changed = changed or getattr(obj, key) is not value

    # cast needed because Python lacks a public Dataclass protocol (see cpython#102395)
    return cast(T, dataclass_replace(cast(Any, obj), **mapped_kwargs)) if changed else obj


def random_id() -> UUID:
    return uuid4()


# Define a type that allows both single and two-argument callables
FnType = Union[Callable[[T], Union[T, None]], Callable[[T, int], Union[T, None]]]
FlatMapFnType = Union[Callable[[T], Union[T, List[T]]], Callable[[T, int], Union[T, List[T]]]]

def list_find(lst: List[T], t: T) -> int:
    for i, x in enumerate(lst):
        if x is t:
            return i
    return -1  # or raise ValueError to match list.index() behavior


def list_map(fn: FnType[T], lst: List[T]) -> List[T]:
    changed = False
    mapped_lst = None

    with_index = len(inspect.signature(fn).parameters) == 2
    for index, original in enumerate(lst):
        new = fn(original, index) if with_index else fn(original)  # type: ignore
        if new is None:
            if mapped_lst is None:
                mapped_lst = lst[:index]
            changed = True
        elif new is not original:
            if mapped_lst is None:
                mapped_lst = lst[:index]
            mapped_lst.append(new)
            changed = True
        elif mapped_lst is not None:
            mapped_lst.append(original)

    if changed:
        assert mapped_lst is not None
        return mapped_lst
    return lst


def list_flat_map(fn: FlatMapFnType[T], lst: List[T]) -> List[T]:
    changed = False
    result: List[T] = []

    with_index = len(inspect.signature(fn).parameters) == 2
    for index, item in enumerate(lst):
        new_items = fn(item, index) if with_index else fn(item)  # type: ignore
        if new_items is None:
            changed = True
            continue

        if isinstance(new_items, list) and (len(new_items) != 1 or new_items[0] is not item):
            changed = True
            result.extend(new_items)
        elif not isinstance(new_items, list):
            if changed or new_items is not item:
                result.append(new_items)
            changed = True

    return result if changed else lst


def list_map_last(fn: Callable[[T], Union[T, None]], lst: List[T]) -> List[T]:
    if not lst:
        return lst
    last = lst[-1]
    new_last = fn(last)
    if new_last is not last:
        if new_last is None:
            return lst[:-1]
        else:
            return lst[:-1] + [new_last]
    return lst
