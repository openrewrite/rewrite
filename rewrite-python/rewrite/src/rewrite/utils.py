import inspect
from typing import Callable, TypeVar, List, Union
from uuid import UUID, uuid4


def random_id() -> UUID:
    return uuid4()

T = TypeVar('T')

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

    return mapped_lst if changed else lst  # type: ignore


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
