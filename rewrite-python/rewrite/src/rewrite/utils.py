import os
from random import Random
from dataclasses import dataclass, fields as _dataclass_fields, is_dataclass as _is_dataclass, replace as dataclass_replace
from typing import Any, Callable, Dict, TypeVar, List, Tuple, Union, cast, dataclass_transform
from uuid import UUID

T = TypeVar('T')


@dataclass_transform(frozen_default=True, eq_default=False)
def lst_dataclass(cls: type[T]) -> type[T]:
    """An LST node: read-only to a type checker, ordinary to the interpreter.

    `frozen=True` would put every field of every node through
    `object.__setattr__`, which costs about five times a plain assignment. The
    read-only contract is the type checker's to keep, as it is for the Java and
    TypeScript models, neither of which enforces it at runtime either.
    """
    return dataclass(eq=False, slots=True)(cls)


@dataclass_transform(frozen_default=True)
def lst_value_dataclass(cls: type[T]) -> type[T]:
    """An LST node compared by value, otherwise as `lst_dataclass`.

    Whitespace and comments carry no id, so `prefix == Space.EMPTY` and the
    like have to compare fields.
    """
    built = dataclass(slots=True)(cls)
    # `dataclass` drops __hash__ wherever it generates __eq__ without freezing, so
    # the hash these nodes' equality implies is put back explicitly.
    names = tuple(f.name for f in _dataclass_fields(built) if f.compare)
    built.__hash__ = lambda self: hash(tuple(getattr(self, n) for n in names))
    return built

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


_MUTABLE_CACHE: Dict[type, bool] = {}


def _accepts_assignment(cls: type) -> bool:
    mutable = _MUTABLE_CACHE.get(cls)
    if mutable is None:
        mutable = _is_dataclass(cls) and not cls.__dataclass_params__.frozen  # type: ignore[attr-defined]
        _MUTABLE_CACHE[cls] = mutable
    return mutable


def _init_fields(cls: type) -> Tuple[str, ...]:
    init_fields = _INIT_FIELDS_CACHE.get(cls)
    if init_fields is None:
        init_fields = tuple(f.name for f in _dataclass_fields(cls) if f.init)
        _INIT_FIELDS_CACHE[cls] = init_fields
    return init_fields


# Per-class map from a keyword accepted by `replace_if_changed`/`assign_fields` to
# the field it sets. Properties are public (`prefix`) where fields are private
# (`_prefix`), and a field colliding with a keyword is spelled with a trailing
# underscore (`from_` for `_from`); all three reach the same field.
_FIELD_FOR_KWARG: Dict[type, Dict[str, str]] = {}


def _field_for_kwarg(cls: type) -> Dict[str, str]:
    mapping = _FIELD_FOR_KWARG.get(cls)
    if mapping is None:
        mapping = {}
        for field in _init_fields(cls):
            mapping[field] = field
            if field.startswith('_'):
                public = field[1:]
                mapping.setdefault(public, field)
                mapping.setdefault(public + '_', field)
        _FIELD_FOR_KWARG[cls] = mapping
    return mapping


def _resolve_field(cls: type, key: str) -> str:
    field = _field_for_kwarg(cls).get(key)
    if field is None:
        raise TypeError(f"{cls.__name__} has no field for keyword '{key}'")
    return field


def assign_fields(obj: T, **kwargs) -> T:
    """Set fields on an object the caller solely owns, mapping names as
    `replace_if_changed` does."""
    cls = type(obj)
    if not _accepts_assignment(cls):
        return replace_if_changed(obj, **kwargs)

    for key, value in kwargs.items():
        field = _resolve_field(cls, key)
        if field == '_id':
            value = id_to_int(value)
        setattr(obj, field, value)
    return obj


def replace_if_changed(obj: T, **kwargs) -> T:
    """Replace fields on a dataclass, returning the original if nothing changed.

    Keywords are named as `_FIELD_FOR_KWARG` describes.

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
    if cls not in _INIT_FIELDS_CACHE and not _is_dataclass(cls):
        # Non-dataclass fallback path — should never hit on the LST hot path,
        # but preserves the original semantics.
        return cast(T, dataclass_replace(cast(Any, obj), **kwargs))
    init_fields = _init_fields(cls)

    mapped_kwargs: Dict[str, Any] = {}
    changed = False
    for key, value in kwargs.items():
        field = _resolve_field(cls, key)
        if field == '_id':
            # Ids are stored as a 128-bit int; normalise UUID/str callers.
            value = id_to_int(value)
        mapped_kwargs[field] = value
        # Use 'or' for short-circuit evaluation - skips check once changed is True
        changed = changed or _is_changed(getattr(obj, field), value)

    if not changed:
        return obj

    # Direct construction from cached init-field names + overlay — avoids
    # dataclasses.replace's per-call walk of __dataclass_fields__. Reads existing
    # values via getattr so this works for both __dict__-based and __slots__-based
    # (slots=True) LST dataclasses.
    new_kwargs = {
        name: mapped_kwargs[name] if name in mapped_kwargs else getattr(obj, name)
        for name in init_fields
    }
    return cls(**new_kwargs)


# Ids come from a generator of our own, not the `random` module's shared one: an
# id is the list-diff key, so a `random.seed()` call anywhere in the process, or a
# fork, must not make two trees draw the same sequence.
_ids = Random()
if hasattr(os, 'register_at_fork'):
    os.register_at_fork(after_in_child=_ids.seed)


def random_id() -> int:
    # Ids are stored as a 128-bit int (the UUID's own representation) to avoid the
    # ~64-byte `uuid.UUID` wrapper; the `.id` properties rebuild a UUID lazily, so
    # the API is unchanged. The value comes from userspace because an id names a
    # node and carries no secret, while the kernel's costs a syscall per id and a
    # tree has one per node -- Java draws these from ThreadLocalRandom.
    return _ids.getrandbits(128)


def id_to_str(value: int) -> str:
    """Format a 128-bit int id as a canonical UUID string without building a UUID.

    Equivalent to ``str(UUID(int=value))`` but skips the wrapper allocation and
    ``UUID.__str__``'s re-derivation — this is the hot path when serialising ids
    over the RPC wire (one per node).
    """
    h = '%032x' % value
    return f'{h[:8]}-{h[8:12]}-{h[12:16]}-{h[16:20]}-{h[20:]}'


def id_to_int(value) -> int:
    """Normalise an id (UUID, canonical string, or int) to its 128-bit int form.

    Used at the few boundaries where an id can arrive as something other than the
    internal int representation (e.g. deserialised from the RPC wire as a string,
    or passed as a ``uuid.UUID`` by older callers).
    """
    if type(value) is int:
        return value
    if isinstance(value, UUID):
        return value.int
    # Canonical UUID string from the wire ("xxxxxxxx-xxxx-..."); base-16 parsing
    # hits CPython's power-of-two fast path.
    return int(value.replace('-', ''), 16)


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
