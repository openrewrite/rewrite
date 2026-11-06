"""
RPC Send Queue for serializing Python LST to RpcObjectData format.

This produces the same JSON format that Java expects:
[
  {"state": "ADD", "valueType": "org.openrewrite.python.tree.Py$CompilationUnit", "value": {...}},
  {"state": "CHANGE", "value": ...},
  {"state": "END_OF_OBJECT"}
]
"""
from enum import Enum
from pathlib import Path
from typing import Any, Dict, List, Optional, Callable, TypeVar
from uuid import UUID

from rewrite.java.support_types import JavaType
from rewrite.rpc.reference import ReferenceMap

# `receive_queue` imports RpcObjectState from this module, so importing its registry lookups back
# at module level is circular; they are bound once on first use instead. They used to be imported
# inside the two methods below, re-entering importlib for every element of every tree -- which the
# profiler put at 5.5% of the peer's time in `_handle_fromlist` alone.
_send_codec_of: Optional[Callable[[Any], Any]] = None
_java_type_name_of: Optional[Callable[[type], Optional[str]]] = None


def _bind_registry_lookups() -> None:
    global _send_codec_of, _java_type_name_of
    from rewrite.rpc import python_receiver  # noqa: F401 - importing it registers the codecs
    from rewrite.rpc.receive_queue import get_java_type_name, get_send_codec
    _send_codec_of, _java_type_name_of = get_send_codec, get_java_type_name


class RpcObjectState(str, Enum):
    NO_CHANGE = "NO_CHANGE"
    ADD = "ADD"
    DELETE = "DELETE"
    CHANGE = "CHANGE"
    END_OF_OBJECT = "END_OF_OBJECT"


ADDED_LIST_ITEM = -1

# Jackson reads a JSON number of at most 1000 characters
# (StreamReadConstraints.getMaxNumberLength), which 3300 bits stays under.
_MAX_WIRE_INT_BITS = 3300

T = TypeVar('T')

_UNRESOLVED: Any = object()
_PER_INSTANCE: Any = object()
# The valueType sent for each class's instances, as _get_value_type resolves it.
_VALUE_TYPES: Dict[type, Any] = {}


def _builtin_value_type(obj_type: type) -> Any:
    """The valueType of a class's instances where the codec registry has no say, else _UNRESOLVED."""
    if obj_type in (str, int, float, bool, type(None)):
        return None
    if issubclass(obj_type, dict):
        return _PER_INSTANCE
    if issubclass(obj_type, (list, UUID, Path)):
        return None

    if issubclass(obj_type, JavaType.Primitive):
        return 'org.openrewrite.java.tree.JavaType$Primitive'
    if issubclass(obj_type, JavaType.Method):
        return 'org.openrewrite.java.tree.JavaType$Method'
    if issubclass(obj_type, JavaType.Parameterized):
        return 'org.openrewrite.java.tree.JavaType$Parameterized'
    # ShallowClass extends Class, so it is checked first, or it would be sent as a Class.
    if issubclass(obj_type, JavaType.ShallowClass):
        return 'org.openrewrite.java.tree.JavaType$ShallowClass'
    if issubclass(obj_type, JavaType.Class):
        return 'org.openrewrite.java.tree.JavaType$Class'
    if issubclass(obj_type, JavaType.GenericTypeVariable):
        return 'org.openrewrite.java.tree.JavaType$GenericTypeVariable'
    if issubclass(obj_type, JavaType.Union):
        return 'org.openrewrite.java.tree.JavaType$MultiCatch'
    if issubclass(obj_type, JavaType.Intersection):
        return 'org.openrewrite.java.tree.JavaType$Intersection'

    # Other enums are serialized by name
    if issubclass(obj_type, Enum):
        return None
    return _UNRESOLVED


class RpcSendQueue:
    """Queue for generating RpcObjectData array from Python LST using visitor pattern."""

    def __init__(self, source_file_type: Optional[str] = None,
                 refs: Optional[ReferenceMap] = None):
        self.q: List[Dict[str, Any]] = []
        # A caller-supplied map spans the peer connection; the default spans this queue.
        self.refs: ReferenceMap = refs if refs is not None else ReferenceMap()
        self.source_file_type = source_file_type
        self._before: Any = None

    def generate(self, after: Any, before: Any = None) -> List[Dict[str, Any]]:
        """Generate RpcObjectData array from an object using visitor pattern."""
        from rewrite.rpc.python_sender import PythonRpcSender

        sender = PythonRpcSender()
        sender.send(after, before, self)
        self.q.append({'state': RpcObjectState.END_OF_OBJECT.value})
        return self.q

    def put(self, data: Dict[str, Any]) -> None:
        """Add data to the queue.

        Kept for the senders and codecs outside this module; the methods here append directly.
        `RpcObjectState` subclasses `str`, so a member serialises to exactly the bytes its value
        does -- verified on 3.12 and 3.13, with and without compact separators -- which is why no
        enum-to-string conversion happens on the way in.
        """
        self.q.append(data)

    def get_and_send(self, parent: T, getter: Callable[[T], Any],
                     on_change: Optional[Callable[[Any], None]] = None) -> None:
        """Get a value from parent and send it, comparing with before state."""
        after = getter(parent)
        before = getter(self._before) if self._before is not None else None
        # Wrap on_change to capture `after` value - Java does the same:
        # () -> onChange.accept(after)
        wrapped = None if on_change is None or after is None else (lambda: on_change(after))
        self.send(after, before, wrapped)

    def get_and_send_as_ref(self, parent: T, getter: Callable[[T], Any],
                            on_change: Optional[Callable[[Any], None]] = None) -> None:
        """Get a value from parent and send it as a ref-tracked object."""
        after = getter(parent)
        before = getter(self._before) if self._before is not None else None
        wrapped = None if on_change is None or after is None else (lambda: on_change(after))
        self._send_as_ref(after, before, wrapped)

    def get_and_send_list(self, parent: T, getter: Callable[[T], Optional[List[Any]]],
                          id_getter: Callable[[Any], Any],
                          on_change: Optional[Callable[[Any], None]] = None) -> None:
        """Get a list from parent and send it with proper list protocol."""
        after = getter(parent)
        before = getter(self._before) if self._before is not None else None
        self.send_list(after, before, id_getter, on_change)

    def get_and_send_list_as_ref(self, parent: T, getter: Callable[[T], Optional[List[Any]]],
                                  id_getter: Callable[[Any], Any],
                                  on_change: Optional[Callable[[Any], None]] = None) -> None:
        """Get a list from parent and send it with ref tracking for each item."""
        after = getter(parent)
        before = getter(self._before) if self._before is not None else None
        self.send_list(after, before, id_getter, on_change, as_ref=True)

    def send(self, after: Any, before: Any = None,
             on_change: Optional[Callable[[], None]] = None) -> None:
        """Send a value, comparing with before if provided."""
        if before is after:
            self.q.append({'state': RpcObjectState.NO_CHANGE})
            return

        if before is None or (after is not None and type(after) != type(before)):
            # Treat as ADD when before is null OR types differ
            self._add(after, on_change)
            return

        if after is None:
            self.q.append({'state': RpcObjectState.DELETE})
            return

        # Changed value
        value_type = self._get_value_type(after)
        codec = self._get_rpc_codec(after)
        value = None if on_change is not None or codec is not None else self._get_primitive_value(after)
        if self._has_no_wire_form(value_type, value, on_change, codec):
            # a CHANGE that carries nothing leaves the peer holding the value it had
            self.q.append({'state': RpcObjectState.DELETE})
            return
        self.q.append({'state': RpcObjectState.CHANGE, 'valueType': value_type, 'value': value})
        self._do_change(after, before, on_change, codec)

    def _send_as_ref(self, after: Any, before: Any = None,
                     on_change: Optional[Callable[[], None]] = None) -> None:
        """Send a value as a ref-tracked object. If already sent, emit just the ref number."""
        if before is after:
            self.q.append({'state': RpcObjectState.NO_CHANGE})
            return

        if before is None or (after is not None and type(after) != type(before)):
            self._add_as_ref(after, on_change)
            return

        if after is None:
            self.q.append({'state': RpcObjectState.DELETE})
            return

        # A ref-deduplicated slot is resolved by the receiver against a persistent cache whose
        # instance may be aliased by any number of other slots and source files. A CHANGE would
        # be applied to that shared instance in place, corrupting every alias, so the new value
        # is re-added instead; the refs map collapses repeats of it into ref-only ADDs.
        self._add_as_ref(after, on_change)

    def send_list(self, after: Optional[List], before: Optional[List],
                  id_getter: Callable[[Any], Any],
                  on_change: Optional[Callable[[Any], None]] = None,
                  as_ref: bool = False) -> None:
        """Send a list with proper list protocol.

        on_change is a callback that takes a single list item and processes it.
        If as_ref is True, list items are ref-tracked to avoid resending duplicates.
        """
        if before is after:
            self.q.append({'state': RpcObjectState.NO_CHANGE})
            return

        if after is None:
            self.q.append({'state': RpcObjectState.DELETE})
            return

        add_fn = self._add_as_ref if as_ref else self._add

        # ADD for a new list, CHANGE for an existing one. The element walk below was a nested
        # closure invoked as the last statement of both branches, so it cost one function
        # object and one call per list-valued field for nothing.
        self.q.append({'state': RpcObjectState.ADD if before is None else RpcObjectState.CHANGE})
        assert after is not None
        if not before:
            # Every element is an addition, so the positions are a constant that needs
            # neither an index map nor a key computed per element.
            self.q.append({'state': RpcObjectState.CHANGE, 'value': [ADDED_LIST_ITEM] * len(after)})
            for item in after:
                add_fn(item, (lambda i=item: on_change(i)) if on_change else None)
            return

        before_idx = {}
        for i, item in enumerate(before):
            before_idx[id_getter(item)] = i

        positions = [before_idx.get(id_getter(item), ADDED_LIST_ITEM) for item in after]
        self.q.append({'state': RpcObjectState.CHANGE, 'value': positions})

        # Send each item
        for item, before_pos in zip(after, positions):
            # Wrap on_change to capture current item
            wrapped = (lambda i=item: on_change(i)) if on_change else None

            if before_pos == ADDED_LIST_ITEM:
                add_fn(item, wrapped)
            else:
                a_before = before[before_pos]
                if a_before is item:
                    self.q.append({'state': RpcObjectState.NO_CHANGE})
                elif as_ref or a_before is None or type(item) != type(a_before):
                    # Type changed, or a ref-deduplicated item, which is always re-added
                    # rather than CHANGEd (see _send_as_ref)
                    add_fn(item, wrapped)
                else:
                    codec = self._get_rpc_codec(item)
                    # Without an on_change callback or codec, no property messages follow, so the
                    # value must travel inline (as in send()) or the receiver keeps the stale element
                    value = None if wrapped is not None or codec is not None else self._get_primitive_value(item)
                    value_type = self._get_value_type(item)
                    if self._has_no_wire_form(value_type, value, wrapped, codec):
                        self.q.append({'state': RpcObjectState.DELETE})
                        continue
                    self.q.append({'state': RpcObjectState.CHANGE, 'valueType': value_type, 'value': value})
                    self._do_change(item, a_before, wrapped, codec)

    def _add(self, obj: Any, on_change: Optional[Callable[[], None]] = None) -> None:
        """Add a new object to the queue."""
        if obj is None:
            self.q.append({'state': RpcObjectState.DELETE})
            return

        value_type = self._get_value_type(obj)
        codec = self._get_rpc_codec(obj)
        value = None if on_change is not None or codec is not None else self._get_primitive_value(obj)
        if value is None and on_change is None and codec is None:
            self._require_wire_form(obj)
        if self._has_no_wire_form(value_type, value, on_change, codec):
            # an ADD that carries nothing is a broken message to the peer, where DELETE is null
            self.q.append({'state': RpcObjectState.DELETE})
            return
        self.q.append({'state': RpcObjectState.ADD, 'valueType': value_type, 'value': value})
        self._do_change(obj, None, on_change, codec)

    def _add_as_ref(self, obj: Any, on_change: Optional[Callable[[], None]] = None) -> None:
        """Add an object with ref tracking. If already sent, emit just the ref number."""
        if obj is None:
            self.q.append({'state': RpcObjectState.DELETE})
            return

        ref = self.refs.get(obj)
        if ref is not None:
            # Already sent — emit ref number only, no onChange
            self.q.append({'state': RpcObjectState.ADD, 'ref': ref})
            return

        # First time — assign ref number and serialize fully
        ref = self.refs.create(obj)

        value_type = self._get_value_type(obj)
        codec = self._get_rpc_codec(obj)
        value = None if on_change is not None or codec is not None else self._get_primitive_value(obj)
        if value is None and on_change is None and codec is None:
            self._require_wire_form(obj)
        self.q.append({'state': RpcObjectState.ADD, 'valueType': value_type, 'value': value, 'ref': ref})
        self._do_change(obj, None, on_change, codec)

    @staticmethod
    def _has_no_wire_form(value_type: Optional[str], value: Any, on_change: Any, codec: Any) -> bool:
        """True of a value nothing carries, a complex number for one: null is all the peer can be told."""
        return value is None and value_type is None and on_change is None and codec is None

    @staticmethod
    def _require_wire_form(obj: Any) -> None:
        from rewrite.markers import Marker
        # with nothing to send for it, a marker reaches the peer as null and is lost for good
        if isinstance(obj, Marker):
            raise TypeError(f"No RPC codec is registered for the marker {type(obj).__qualname__}")

    def _do_change(self, after: Any, before: Any,
                   on_change: Optional[Callable[[], None]] = None,
                   codec: Optional[Callable[[Any, 'RpcSendQueue'], None]] = None) -> None:
        """Execute the change callback with proper before state tracking."""
        if after is None:
            return

        last_before = self._before
        self._before = before
        try:
            if on_change is not None:
                on_change()
            elif codec is not None:
                codec(after, self)
        finally:
            self._before = last_before

    def _get_rpc_codec(self, obj: Any) -> Optional[Callable[[Any, 'RpcSendQueue'], None]]:
        """Get the RpcCodec serialization function for an object."""
        if _send_codec_of is None:
            _bind_registry_lookups()
        return _send_codec_of(obj)

    def _get_value_type(self, obj: Any) -> Optional[str]:
        """Get the Java type name for an object, or None for primitives."""
        obj_type = type(obj)
        value_type = _VALUE_TYPES.get(obj_type, _UNRESOLVED)
        if value_type is _UNRESOLVED:
            value_type = _builtin_value_type(obj_type)
            if value_type is _UNRESOLVED:
                if _java_type_name_of is None:
                    _bind_registry_lookups()
                value_type = _java_type_name_of(obj_type)
                if value_type is None:
                    # not cached, as a codec may yet be registered for the class
                    return None
            _VALUE_TYPES[obj_type] = value_type
        if value_type is _PER_INSTANCE:
            # An opaque value the remote side has a codec for but Python does not is received as
            # {'kind': <valueType>, **fields} (see RpcReceiveQueue._do_change). Re-emit its original
            # valueType so it round-trips unchanged; an ordinary dict (no 'kind') stays untyped.
            return obj.get('kind')
        return value_type

    def _get_primitive_value(self, obj: Any) -> Any:
        """Get the primitive value representation for serialization."""
        import math
        if obj is None:
            return None
        if isinstance(obj, dict) and 'kind' in obj:
            # Opaque value (see _get_value_type): its wire payload is every field except the
            # synthetic 'kind' tag the receiver added. Emit that back as the value.
            return {k: v for k, v in obj.items() if k != 'kind'}
        if isinstance(obj, dict):
            # A plain mapping is JSON-native and travels as itself, as it does on every other
            # peer. Sequences take the send_list protocol instead.
            return obj
        if isinstance(obj, list):
            # Sent as one value rather than through send_list, a list of scalars is JSON-native too.
            return obj if all(type(item) in (str, int, float, bool) for item in obj) else None
        if isinstance(obj, bool):
            return obj
        if isinstance(obj, int):
            # An integer travels as a number whatever its width, so that a receiver boxes it
            # as one; a string here is indistinguishable from a string literal's value.
            return obj if obj.bit_length() <= _MAX_WIRE_INT_BITS else None
        if isinstance(obj, str):
            return obj
        if isinstance(obj, float):
            # Special float values (inf, nan) are not valid JSON
            # Return None to indicate the value cannot be serialized
            if math.isinf(obj) or math.isnan(obj):
                return None
            return obj
        if isinstance(obj, complex):
            # No JSON number carries a complex, and a string is ruled out for the same
            # reason as a wide int, so valueSource carries the literal alone.
            return None
        if isinstance(obj, UUID):
            return str(obj)
        if isinstance(obj, Path):
            return str(obj)
        if isinstance(obj, Enum):
            return obj.name
        # Complex objects are serialized via visitor, not as values, but for the few sent whole
        from rewrite.rpc.receive_queue import get_value_writer
        writer = get_value_writer(obj)
        return writer(obj) if writer is not None else None
