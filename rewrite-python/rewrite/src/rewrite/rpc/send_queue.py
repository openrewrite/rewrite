"""
RPC Send Queue for serializing Python LST to RpcObjectData format.

This produces the same JSON format that Java expects:
[
  {"state": "ADD", "valueType": "org.openrewrite.python.tree.Py$CompilationUnit", "value": {...}},
  {"state": "CHANGE", "value": ...},
  {"state": "END_OF_OBJECT"}
]
"""
from dataclasses import is_dataclass
from enum import Enum
from pathlib import Path
from typing import Any, Dict, List, Optional, Callable, TypeVar
from uuid import UUID


class RpcObjectState(str, Enum):
    NO_CHANGE = "NO_CHANGE"
    ADD = "ADD"
    DELETE = "DELETE"
    CHANGE = "CHANGE"
    END_OF_OBJECT = "END_OF_OBJECT"


ADDED_LIST_ITEM = -1

T = TypeVar('T')


def to_java_type_name(py_type: type) -> Optional[str]:
    """Convert Python type name to Java type name.

    Returns None for Python built-in types that don't have Java equivalents.
    """
    module = py_type.__module__

    # Skip Python built-in types - they don't have Java equivalents
    if module == 'builtins':
        return None

    # Use __qualname__ to get full nested class path (e.g., "If.Else" instead of just "Else")
    qualname = py_type.__qualname__
    # Convert dots to $ for Java nested class naming (If.Else -> If$Else)
    java_name = qualname.replace('.', '$')

    # Handle rewrite.python.tree types
    if module.startswith('rewrite.python.tree'):
        return f"org.openrewrite.python.tree.Py${java_name}"
    elif module.startswith('rewrite.python.support_types'):
        # PyComment is in support_types but maps to tree package
        return f"org.openrewrite.python.tree.{java_name}"
    elif module.startswith('rewrite.python.markers'):
        return f"org.openrewrite.python.marker.{java_name}"
    elif module.startswith('rewrite.java.tree'):
        return f"org.openrewrite.java.tree.J${java_name}"
    elif module.startswith('rewrite.java.support_types'):
        if qualname == 'JavaType':
            return 'org.openrewrite.java.tree.JavaType'
        return f"org.openrewrite.java.tree.{java_name}"
    elif module.startswith('rewrite.java.markers'):
        return f"org.openrewrite.java.marker.{java_name}"
    elif module.startswith('rewrite.markers'):
        return f"org.openrewrite.marker.{java_name}"
    elif qualname == 'Markers':
        return 'org.openrewrite.marker.Markers'
    elif qualname == 'Space':
        return 'org.openrewrite.java.tree.Space'
    elif qualname == 'Comment':
        return 'org.openrewrite.java.tree.Comment'
    elif qualname == 'TextComment':
        return 'org.openrewrite.java.tree.Comment$TextComment'
    elif qualname == 'JRightPadded':
        return 'org.openrewrite.java.tree.JRightPadded'
    elif qualname == 'JLeftPadded':
        return 'org.openrewrite.java.tree.JLeftPadded'
    elif qualname == 'JContainer':
        return 'org.openrewrite.java.tree.JContainer'

    # Default handling
    return f"{module}.{java_name}"


class RpcSendQueue:
    """Queue for generating RpcObjectData array from Python LST using visitor pattern."""

    def __init__(self, source_file_type: Optional[str] = None):
        self.q: List[Dict[str, Any]] = []
        self.refs: Dict[int, int] = {}  # id(obj) -> ref number
        self.next_ref: int = 0
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
        """Add data to the queue."""
        # Convert state enum to string value
        if 'state' in data and isinstance(data['state'], RpcObjectState):
            data['state'] = data['state'].value
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

    def get_and_send_list(self, parent: T, getter: Callable[[T], Optional[List[Any]]],
                          id_getter: Callable[[Any], Any],
                          on_change: Optional[Callable[[Any], None]] = None) -> None:
        """Get a list from parent and send it with proper list protocol."""
        after = getter(parent)
        before = getter(self._before) if self._before is not None else None
        self.send_list(after, before, id_getter, on_change)

    def send(self, after: Any, before: Any = None,
             on_change: Optional[Callable[[], None]] = None) -> None:
        """Send a value, comparing with before if provided."""
        if before is after:
            self.put({'state': RpcObjectState.NO_CHANGE})
            return

        if before is None or (after is not None and type(after) != type(before)):
            # Treat as ADD when before is null OR types differ
            self._add(after, on_change)
            return

        if after is None:
            self.put({'state': RpcObjectState.DELETE})
            return

        # Changed value
        value_type = self._get_value_type(after)
        codec = self._get_rpc_codec(after)
        value = None if on_change is not None or codec is not None else self._get_primitive_value(after)
        self.put({'state': RpcObjectState.CHANGE, 'valueType': value_type, 'value': value})
        self._do_change(after, before, on_change, codec)

    def send_list(self, after: Optional[List], before: Optional[List],
                  id_getter: Callable[[Any], Any],
                  on_change: Optional[Callable[[Any], None]] = None) -> None:
        """Send a list with proper list protocol.

        on_change is a callback that takes a single list item and processes it.
        """
        if before is after:
            self.put({'state': RpcObjectState.NO_CHANGE})
            return

        if after is None:
            self.put({'state': RpcObjectState.DELETE})
            return

        def list_change():
            assert after is not None
            # Build before index map
            before_idx = {}
            if before is not None:
                for i, item in enumerate(before):
                    before_idx[id_getter(item)] = i

            # Send positions array
            positions = []
            for item in after:
                item_id = id_getter(item)
                if item_id in before_idx:
                    positions.append(before_idx[item_id])
                else:
                    positions.append(ADDED_LIST_ITEM)
            self.put({'state': RpcObjectState.CHANGE, 'value': positions})

            # Send each item
            for item in after:
                item_id = id_getter(item)
                before_pos = before_idx.get(item_id)
                # Wrap on_change to capture current item
                wrapped = (lambda i=item: on_change(i)) if on_change else None

                if before_pos is None:
                    self._add(item, wrapped)
                else:
                    a_before = before[before_pos] if before else None
                    if a_before is item:
                        self.put({'state': RpcObjectState.NO_CHANGE})
                    elif a_before is None or type(item) != type(a_before):
                        self._add(item, wrapped)
                    else:
                        self.put({'state': RpcObjectState.CHANGE, 'valueType': self._get_value_type(item)})
                        self._do_change(item, a_before, wrapped)

        if before is None:
            # ADD for new list
            self.put({'state': RpcObjectState.ADD})
            list_change()
        else:
            # CHANGE for existing list
            self.put({'state': RpcObjectState.CHANGE})
            list_change()

    def _add(self, obj: Any, on_change: Optional[Callable[[], None]] = None) -> None:
        """Add a new object to the queue."""
        if obj is None:
            self.put({'state': RpcObjectState.DELETE})
            return

        value_type = self._get_value_type(obj)
        codec = self._get_rpc_codec(obj)
        value = None if on_change is not None or codec is not None else self._get_primitive_value(obj)
        self.put({'state': RpcObjectState.ADD, 'valueType': value_type, 'value': value})
        self._do_change(obj, None, on_change, codec)

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
        from rewrite import Markers
        from rewrite.java.markers import OmitParentheses, Semicolon, TrailingComma
        if isinstance(obj, Markers):
            return self._rpc_send_markers
        if isinstance(obj, OmitParentheses):
            return self._rpc_send_omit_parens
        if isinstance(obj, Semicolon):
            return self._rpc_send_semicolon
        if isinstance(obj, TrailingComma):
            return self._rpc_send_trailing_comma
        return None

    def _rpc_send_omit_parens(self, marker: 'OmitParentheses', q: 'RpcSendQueue') -> None:
        """RpcCodec implementation for OmitParentheses."""
        q.get_and_send(marker, lambda x: x.id)

    def _rpc_send_semicolon(self, marker: 'Semicolon', q: 'RpcSendQueue') -> None:
        """RpcCodec implementation for Semicolon."""
        q.get_and_send(marker, lambda x: x.id)

    def _rpc_send_trailing_comma(self, marker: 'TrailingComma', q: 'RpcSendQueue') -> None:
        """RpcCodec implementation for TrailingComma."""
        from rewrite.rpc.python_sender import PythonRpcSender
        sender = PythonRpcSender()
        q.get_and_send(marker, lambda x: x.id)
        q.get_and_send(marker, lambda x: x.suffix, lambda s: sender._visit_space(s, q))

    def _rpc_send_markers(self, markers: 'Markers', q: 'RpcSendQueue') -> None:
        """RpcCodec implementation for Markers."""
        q.get_and_send(markers, lambda x: x.id)
        # Send markers list as ref (simplified - no ref tracking for now)
        q.get_and_send_list(markers, lambda x: x.markers,
                           lambda m: m.id, None)

    def _get_value_type(self, obj: Any) -> Optional[str]:
        """Get the Java type name for an object, or None for primitives."""
        if obj is None:
            return None

        obj_type = type(obj)

        # Primitives and built-ins don't need type info
        if obj_type in (str, int, float, bool, type(None)):
            return None
        if isinstance(obj, (list, dict)):
            return None
        if isinstance(obj, UUID):
            return None
        if isinstance(obj, Path):
            return None

        # JavaType.Primitive is a special Enum that needs its Java type name
        # Check for JavaType.Primitive before the general Enum check
        from rewrite.java.support_types import JavaType
        if isinstance(obj, JavaType.Primitive):
            return 'org.openrewrite.java.tree.JavaType$Primitive'

        # Other enums don't need type info (they're serialized by name)
        if isinstance(obj, Enum):
            return None

        # Dataclasses need their Java type name
        if is_dataclass(obj) and not isinstance(obj, type):
            return to_java_type_name(obj_type)

        return None

    def _get_primitive_value(self, obj: Any) -> Any:
        """Get the primitive value representation for serialization."""
        import math
        if obj is None:
            return None
        if isinstance(obj, bool):
            return obj
        if isinstance(obj, (int, str)):
            return obj
        if isinstance(obj, float):
            # Special float values (inf, nan) are not valid JSON
            # Return None to indicate the value cannot be serialized
            if math.isinf(obj) or math.isnan(obj):
                return None
            return obj
        if isinstance(obj, UUID):
            return str(obj)
        if isinstance(obj, Path):
            return str(obj)
        if isinstance(obj, Enum):
            return obj.name
        # Complex objects are serialized via visitor, not as values
        return None
