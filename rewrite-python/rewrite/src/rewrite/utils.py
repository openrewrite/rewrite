from dataclasses import fields as _dataclass_fields, is_dataclass as _is_dataclass, replace as dataclass_replace
from typing import Any, Callable, Dict, TypeVar, List, Tuple, Union, cast
from uuid import UUID, uuid4

T = TypeVar('T')

# Per-class cache of init-field names. `dataclasses.replace` re-walks
# `__dataclass_fields__` on every call to fill in missing fields via getattr;
# we'd rather pay the introspection once per class and then construct directly
# from `__dict__`. ~16.7M `replace_if_changed` calls per medium sequential run.
_INIT_FIELDS_CACHE: Dict[type, Tuple[str, ...]] = {}


def _is_changed(old, new) -> bool:
    """Check if a value has changed, using identity for complex objects and equality for primitives.

    Identity (``is``) is the right check for most AST node types because visitors
    intentionally create new wrapper objects to signal a change.  But for *leaf*
    values — strings, numbers, booleans, ``None`` — a newly constructed value that
    is equal to the original should be treated as unchanged.  Without this,
    normalisation visitors that rebuild a string identical to the original would
    cause a spurious "change" on every node they touch.
    """
    if old is new:
        return False
    if isinstance(new, (str, int, float, bool, type(None))):
        return old != new
    return True  # different identity → changed


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

    cls = type(obj)
    init_fields = _INIT_FIELDS_CACHE.get(cls)
    if init_fields is None:
        if not _is_dataclass(cls):
            # Non-dataclass fallback path — should never hit on the LST hot path,
            # but preserves the original semantics.
            return cast(T, dataclass_replace(cast(Any, obj), **kwargs))
        init_fields = tuple(f.name for f in _dataclass_fields(cls) if f.init)
        _INIT_FIELDS_CACHE[cls] = init_fields

    # Map public property names to private field names and check for changes
    mapped_kwargs: Dict[str, Any] = {}
    changed = False
    for key, value in kwargs.items():
        if not key.startswith('_'):
            # Handle Python keyword conflicts: from_ -> _from
            private_key = f'_{key.rstrip("_")}'
            if private_key in init_fields:
                mapped_kwargs[private_key] = value
                # Use 'or' for short-circuit evaluation - skips check once changed is True
                changed = changed or _is_changed(getattr(obj, private_key), value)
            else:
                mapped_kwargs[key] = value
                changed = changed or _is_changed(getattr(obj, key), value)
        else:
            mapped_kwargs[key] = value
            changed = changed or _is_changed(getattr(obj, key), value)

    if not changed:
        return obj

    # Direct construction from __dict__ + overlay — avoids dataclasses.replace's
    # per-call walk of __dataclass_fields__. Frozen LST dataclasses still have
    # __dict__ (no __slots__), so vars() is safe here.
    obj_dict = obj.__dict__
    new_kwargs = {
        name: mapped_kwargs[name] if name in mapped_kwargs else obj_dict[name]
        for name in init_fields
    }
    return cast(T, cls(**new_kwargs))


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


def _callable_arg_count(fn: Any) -> int:
    """Get the number of expected arguments for a callable (function or bound method)."""
    arg_count: int = fn.__code__.co_argcount
    if hasattr(fn, '__self__'):  # bound method — co_argcount includes self
        arg_count -= 1
    return arg_count


def list_map(fn: FnType[T], lst: List[T]) -> List[T]:
    changed = False
    mapped_lst = None

    arg_count = _callable_arg_count(fn)
    with_index = arg_count == 2
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

    arg_count = _callable_arg_count(fn)
    with_index = arg_count == 2
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
