# Copyright 2025 the original author or authors.
# <p>
# Licensed under the Moderne Source Available License (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
# <p>
# https://docs.moderne.io/licensing/moderne-source-available-license
# <p>
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

"""
RPC Receive Queue for deserializing RpcObjectData from Java into Python LST.

This processes the same JSON format that Python sends:
[
  {"state": "ADD", "valueType": "org.openrewrite.python.tree.Py$CompilationUnit", "value": {...}},
  {"state": "CHANGE", "value": ...},
  {"state": "END_OF_OBJECT"}
]
"""
from dataclasses import dataclass
from typing import Any, Callable, Dict, List, Optional, TypeVar, cast

from rewrite import Markers
from rewrite.rpc.send_queue import RpcObjectState

T = TypeVar('T')


@dataclass
class RpcObjectData:
    """Data structure for RPC object messages."""
    state: RpcObjectState
    value_type: Optional[str] = None
    value: Any = None
    ref: Optional[int] = None
    trace: Optional[str] = None


class RpcReceiveQueue:
    """Queue for receiving and deserializing RpcObjectData from Java.

    This mirrors the JavaScript RpcReceiveQueue implementation, supporting:
    - State-based deserialization (NO_CHANGE, ADD, DELETE, CHANGE, END_OF_OBJECT)
    - Reference tracking for cyclic object graphs
    - Codec-based deserialization for complex types
    - List synchronization with position tracking
    """

    def __init__(
        self,
        refs: Dict[int, Any],
        source_file_type: Optional[str],
        pull: Callable[[], List[Dict[str, Any]]],
        trace: bool = False
    ):
        """Initialize the receive queue.

        Args:
            refs: Dictionary mapping reference IDs to objects (for cyclic graphs)
            source_file_type: The source file type for codec lookup
            pull: Function to fetch the next batch of RpcObjectData from Java
            trace: Whether to enable trace logging
        """
        self._batch: List[Dict[str, Any]] = []
        self._refs = refs
        self._source_file_type = source_file_type
        self._pull = pull
        self._trace = trace

    def take(self) -> RpcObjectData:
        """Take the next message from the queue, fetching more if needed."""
        if not self._batch:
            self._batch = self._pull()

        if not self._batch:
            raise RuntimeError("RPC receive queue is empty and pull returned no data")

        raw = self._batch.pop(0)
        return self._parse_message(raw)

    def _parse_message(self, raw: Dict[str, Any]) -> RpcObjectData:
        """Parse a raw message dict into RpcObjectData."""
        state_str = raw.get('state', 'NO_CHANGE')
        state = RpcObjectState(state_str) if isinstance(state_str, str) else state_str

        return RpcObjectData(
            state=state,
            value_type=raw.get('valueType'),
            value=raw.get('value'),
            ref=raw.get('ref'),
            trace=raw.get('trace')
        )

    def receive_defined(
        self,
        before: Optional[T] = None,
        on_change: Optional[Callable[[Optional[T]], T]] = None
    ) -> T:
        """Receive and deserialize an object, asserting result is not None.

        Use this when the protocol guarantees a non-null result.

        Args:
            before: The previous state of the object (for delta updates)
            on_change: Optional callback to handle deserialization manually

        Returns:
            The deserialized object (never None)

        Raises:
            AssertionError: If the result is None
        """
        result = self.receive(before, on_change)
        assert result is not None, "Expected non-null result from receive"
        return result

    def receive(
        self,
        before: Optional[T] = None,
        on_change: Optional[Callable[[Optional[T]], T]] = None
    ) -> Optional[T]:
        """Receive and deserialize an object.

        Args:
            before: The previous state of the object (for delta updates)
            on_change: Optional callback to handle deserialization manually

        Returns:
            The deserialized object, or None if deleted
        """
        message = self.take()
        ref: Optional[int] = None

        if message.state == RpcObjectState.NO_CHANGE:
            return before

        elif message.state == RpcObjectState.DELETE:
            return None

        elif message.state == RpcObjectState.ADD:
            ref = message.ref
            if ref is not None and message.value_type is None and message.value is None:
                # Pure reference to existing object
                if ref in self._refs:
                    return self._refs[ref]
                else:
                    raise RuntimeError(f"Received reference to unknown object: {ref}")
            else:
                # New object or forward declaration with ref
                if message.value_type is None:
                    before = message.value
                else:
                    before = self._new_obj(message.value_type)

                if ref is not None:
                    # Store for future references (handles cyclic graphs)
                    self._refs[ref] = before

            # Fall through to CHANGE for field-by-field deserialization
            return self._do_change(before, on_change, message, ref)

        elif message.state == RpcObjectState.CHANGE:
            return self._do_change(before, on_change, message, message.ref)

        else:
            raise RuntimeError(f"Unknown state type: {message.state}")

    def _do_change(
        self,
        before: Optional[T],
        on_change: Optional[Callable[[Optional[T]], T]],
        message: RpcObjectData,
        ref: Optional[int]
    ) -> Optional[T]:
        """Handle CHANGE state for an object."""
        after: Any

        if on_change is not None:
            after = on_change(before)
        else:
            codec = self._get_codec(before)
            if codec is not None:
                after = codec(before, self)
            elif message.value is not None:
                if message.value_type:
                    after = {'kind': message.value_type, **message.value} if isinstance(message.value, dict) else message.value
                else:
                    after = message.value
            else:
                after = before

        if ref is not None:
            self._refs[ref] = after

        return cast(Optional[T], after)

    def receive_list_defined(
        self,
        before: Optional[List[T]] = None,
        on_change: Optional[Callable[[Optional[T]], T]] = None
    ) -> List[T]:
        """Receive and deserialize a list, asserting result is not None.

        Use this when the protocol guarantees a non-null result.

        Args:
            before: The previous state of the list
            on_change: Optional callback to handle item deserialization

        Returns:
            The deserialized list (never None)

        Raises:
            AssertionError: If the result is None
        """
        result = self.receive_list(before, on_change)
        assert result is not None, "Expected non-null result from receive_list"
        return result

    def receive_list(
        self,
        before: Optional[List[T]] = None,
        on_change: Optional[Callable[[Optional[T]], T]] = None
    ) -> Optional[List[T]]:
        """Receive and deserialize a list.

        Args:
            before: The previous state of the list
            on_change: Optional callback to handle item deserialization

        Returns:
            The deserialized list, or None if deleted
        """
        message = self.take()

        if message.state == RpcObjectState.NO_CHANGE:
            return before

        elif message.state == RpcObjectState.DELETE:
            return None

        elif message.state == RpcObjectState.ADD:
            before = []
            # Fall through to CHANGE

        elif message.state != RpcObjectState.CHANGE:
            raise RuntimeError(f"{message.state} is not supported for lists")

        # Next message should be CHANGE with positions array
        positions_msg = self.take()
        positions = positions_msg.value

        if not isinstance(positions, list):
            raise RuntimeError(f"Expected positions array but got: {positions_msg}")

        after: List[T] = []
        for i, before_idx in enumerate(positions):
            if before_idx >= 0 and before is not None:
                b = before[before_idx]
            else:
                b = None

            item = self.receive(b, on_change)
            after.append(item)

        return after

    def receive_markers(self, markers: Optional['Markers'] = None) -> 'Markers':
        """Receive and deserialize Markers.

        Args:
            markers: The previous markers state

        Returns:
            The deserialized Markers object
        """
        from rewrite import Markers

        if markers is None:
            markers = Markers.EMPTY

        def on_change(m: Optional[Markers]) -> Markers:
            if m is None:
                m = Markers.EMPTY

            new_id = self.receive_defined(m.id)
            new_markers_list = self.receive_list(list(m.markers) if m.markers else None)

            return Markers(new_id, new_markers_list or [])

        return self.receive(markers, on_change) or Markers.EMPTY

    def _new_obj(self, value_type: str) -> Any:
        """Create a new instance using registered factory.

        All types should be registered via codec registration at module load time.
        """
        factory = _get_codec_factory(value_type, self._source_file_type)
        if factory is not None:
            return factory()

        # Fallback: return a dict with kind field
        return {'kind': value_type}

    def _get_codec(self, obj: Any) -> Optional[Callable[[Any, 'RpcReceiveQueue'], Any]]:
        """Get the deserializer codec for an object.

        Uses registry lookup by Python class name only - no AST imports.
        This keeps the RPC layer independent of specific AST models.
        """
        if obj is None:
            return None

        # Look up by Python class name - no isinstance checks needed
        obj_type = type(obj).__qualname__
        return _get_receive_codec(obj_type, self._source_file_type)


# Codec registry
_codecs: Dict[str, Dict[str, Callable[[Any, RpcReceiveQueue], Any]]] = {}
_codec_factories: Dict[str, Dict[str, Callable[[], Any]]] = {}
# Send codec registry: Python class -> sender function
_send_codecs: Dict[type, Callable[[Any, Any], None]] = {}
# Reverse mapping: Python class -> Java type name (used by sender)
_python_to_java_type: Dict[type, str] = {}


def register_receive_codec(
    type_name: str,
    codec: Callable[[Any, RpcReceiveQueue], Any],
    factory: Optional[Callable[[], Any]] = None,
    source_file_type: Optional[str] = None
) -> None:
    """Register a receive codec for a type.

    Args:
        type_name: The type name (Python class name or Java type name)
        codec: Function to deserialize the type: (before, queue) -> after
        factory: Optional function to create a new instance of the type
        source_file_type: Optional source file type for language-specific codecs
    """
    key = source_file_type or ''
    if key not in _codecs:
        _codecs[key] = {}
        _codec_factories[key] = {}

    _codecs[key][type_name] = codec
    if factory is not None:
        _codec_factories[key][type_name] = factory


def _get_receive_codec(
    type_name: str,
    source_file_type: Optional[str]
) -> Optional[Callable[[Any, RpcReceiveQueue], Any]]:
    """Get the receive codec for a type."""
    # Try source-file-specific codec first
    if source_file_type and source_file_type in _codecs:
        codec = _codecs[source_file_type].get(type_name)
        if codec is not None:
            return codec

    # Fall back to global codecs
    return _codecs.get('', {}).get(type_name)


def _get_codec_factory(
    value_type: str,
    source_file_type: Optional[str]
) -> Optional[Callable[[], Any]]:
    """Get the factory for creating a new instance of a type."""
    # Try source-file-specific factory first
    if source_file_type and source_file_type in _codec_factories:
        factory = _codec_factories[source_file_type].get(value_type)
        if factory is not None:
            return factory

    # Fall back to global factories
    return _codec_factories.get('', {}).get(value_type)


def register_send_codec(
    python_class: type,
    codec: Callable[[Any, Any], None]
) -> None:
    """Register a send codec for a Python class.

    Args:
        python_class: The Python class to register
        codec: Function to serialize the type: (obj, queue) -> None
    """
    _send_codecs[python_class] = codec


def get_send_codec(obj: Any) -> Optional[Callable[[Any, Any], None]]:
    """Get the send codec for an object.

    Args:
        obj: The object to get the codec for

    Returns:
        The send codec function, or None if not registered
    """
    return _send_codecs.get(type(obj))


def _receive_markers(markers: 'Markers', q: RpcReceiveQueue) -> 'Markers':
    """Codec for receiving Markers."""
    from rewrite import Markers

    new_id = q.receive_defined(markers.id)
    new_markers_list = q.receive_list(list(markers.markers) if markers.markers else None)

    return Markers(new_id, new_markers_list or [])


# ============================================================================
# Codec registration helpers
# ============================================================================

def register_codec_with_both_names(
    java_type: str,
    python_class: type,
    codec,
    factory,
    sender: Optional[Callable[[Any, Any], None]] = None
):
    """Register codec with both Java type name and Python class name.

    Args:
        java_type: Java type name (e.g., 'org.openrewrite.java.tree.J$Identifier')
        python_class: The Python class
        codec: Function to deserialize: (before, queue) -> after
        factory: Function to create new instance: () -> instance
        sender: Optional function to serialize: (obj, queue) -> None
    """
    # Register by Java type name (for _new_obj factory lookup)
    register_receive_codec(java_type, codec, factory)
    # Register by Python class name (for _get_codec lookup)
    python_name = python_class.__qualname__
    register_receive_codec(python_name, codec, factory)
    # Register reverse mapping for sender
    _python_to_java_type[python_class] = java_type
    # Register send codec if provided
    if sender is not None:
        register_send_codec(python_class, sender)


def get_java_type_name(python_class: type) -> Optional[str]:
    """Get the Java type name for a Python class.

    This uses the codec registry populated during initialization.
    Returns None if the class is not registered.
    """
    return _python_to_java_type.get(python_class)


def get_all_tree_classes(module, module_name: str):
    """Recursively find all dataclass AST types including nested classes.

    Args:
        module: The module to search (e.g., rewrite.python.tree)
        module_name: The module's name for filtering (e.g., 'rewrite.python.tree')

    Returns:
        List of (name, class) tuples where name includes nested path (e.g., 'Lambda.Parameters')
    """
    import inspect
    from dataclasses import is_dataclass

    def get_nested(cls, prefix):
        results = []
        for name, nested in inspect.getmembers(cls, inspect.isclass):
            if nested.__module__ != module_name:
                continue
            # Skip padding helper classes
            if name == 'PaddingHelper':
                continue
            full_name = f'{prefix}.{name}'
            if is_dataclass(nested):
                results.append((full_name, nested))
            results.extend(get_nested(nested, full_name))
        return results

    all_classes = []
    for name, cls in inspect.getmembers(module, inspect.isclass):
        if cls.__module__ != module_name:
            continue
        if not is_dataclass(cls):
            continue
        all_classes.append((name, cls))
        all_classes.extend(get_nested(cls, name))

    return all_classes


def make_dataclass_factory(cls):
    """Create a factory that instantiates a dataclass with all fields set to None.

    This is needed because the receiver code may access properties (like id, prefix, markers)
    before they are set during deserialization. By calling the constructor with None values
    for all fields, we create a valid frozen dataclass instance that can have its fields
    replaced during deserialization.
    """
    from dataclasses import fields, is_dataclass

    if is_dataclass(cls):
        field_names = [f.name for f in fields(cls)]

        def factory():
            kwargs = {name: None for name in field_names}
            return cls(**kwargs)
        return factory
    else:
        return lambda: object.__new__(cls)


