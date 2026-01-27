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


class RpcObjectState(str, Enum):
    NO_CHANGE = "NO_CHANGE"
    ADD = "ADD"
    DELETE = "DELETE"
    CHANGE = "CHANGE"
    END_OF_OBJECT = "END_OF_OBJECT"


ADDED_LIST_ITEM = -1

T = TypeVar('T')


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
        # Import python_receiver to ensure codecs are registered
        from rewrite.rpc import python_receiver  # noqa: F401 - triggers codec registration
        from rewrite.rpc.receive_queue import get_send_codec
        return get_send_codec(obj)

    def _get_value_type(self, obj: Any) -> Optional[str]:
        """Get the Java type name for an object, or None for primitives."""
        # Import python_receiver to ensure codecs are registered before get_java_type_name
        from rewrite.rpc import python_receiver  # noqa: F401 - triggers codec registration
        from rewrite.rpc.receive_queue import get_java_type_name

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
        if isinstance(obj, JavaType.Method):
            return 'org.openrewrite.java.tree.JavaType$Method'
        if isinstance(obj, JavaType.Class):
            return 'org.openrewrite.java.tree.JavaType$Class'

        # Other enums don't need type info (they're serialized by name)
        if isinstance(obj, Enum):
            return None

        # Look up Java type name from codec registry
        return get_java_type_name(obj_type)

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
