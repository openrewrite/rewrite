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

"""RPC codec extensibility for a host-registered custom JavaType.Class subclass.

These tests prove the Python RPC send/receive registries are open to a host
(e.g. the moderne-cli addon) registering codecs for its own ``value_type`` — a
type-table proxy that subclasses ``JavaType.Class`` — and that send dispatch
actually picks them up rather than flattening the proxy to a built-in
``JavaType$Class``. The receive side was already registry-driven; the send side
gained two guarded hooks (``RpcSendQueue._get_value_type`` consults the registry,
``PythonRpcSender._visit_type`` defers to a registered send codec).

No proxy/codec semantics are exercised here beyond a trivial FQN+kind payload;
the real proxy and its table-backed resolution live in the host addon.
"""

import copy

import pytest

from rewrite.java.support_types import JavaType
from rewrite.rpc.python_sender import PythonRpcSender
from rewrite.rpc.receive_queue import (
    RpcReceiveQueue,
    register_codec_with_both_names,
)
from rewrite.rpc.send_queue import RpcSendQueue

# A stand-in for a host's type-table proxy: a custom JavaType.Class subclass with
# its own value_type discriminator.
_PROXY_VALUE_TYPE = "io.moderne.cli.TypeTableClassProxy"


class _ProxyClass(JavaType.Class):
    pass


@pytest.fixture
def isolated_codec_registry():
    """Snapshot and restore the process-global codec registries so a test's
    registrations don't leak into others."""
    from rewrite.rpc import receive_queue as rq
    snap = (
        copy.deepcopy(rq._codecs),
        copy.deepcopy(rq._codec_factories),
        dict(rq._send_codecs),
        dict(rq._python_to_java_type),
    )
    try:
        yield
    finally:
        for live, saved in (
            (rq._codecs, snap[0]),
            (rq._codec_factories, snap[1]),
            (rq._send_codecs, snap[2]),
            (rq._python_to_java_type, snap[3]),
        ):
            live.clear()
            live.update(saved)


def _kind_str(k):
    return k.value if hasattr(k, 'value') else str(k)


def _register_proxy_codecs(seen=None):
    """Register a symmetric send/receive codec pair for ``_ProxyClass`` carrying
    a Class-only FQN + kind payload (the agreed wire shape)."""
    def _send(obj, q):
        if seen is not None:
            seen.append('send')
        q.get_and_send(obj, lambda x: x._fully_qualified_name)
        q.get_and_send(obj, lambda x: _kind_str(x._kind))

    def _recv(before, q):
        if seen is not None:
            seen.append('recv')
        before._fully_qualified_name = q.receive(None)
        q.receive(None)  # kind (kept simple; not re-parsed back to the enum here)
        return before

    def _factory():
        p = _ProxyClass()
        p._flags_bit_map = 0
        p._kind = JavaType.FullyQualified.Kind.Class
        return p

    register_codec_with_both_names(
        _PROXY_VALUE_TYPE, _ProxyClass, _recv, _factory, sender=_send)


def _make_proxy(fqn="pkg.Foo"):
    p = _ProxyClass()
    p._flags_bit_map = 0
    p._fully_qualified_name = fqn
    p._kind = JavaType.FullyQualified.Kind.Class
    return p


def test_value_type_preserves_registered_subclass_discriminator(isolated_codec_registry):
    _register_proxy_codecs()
    assert RpcSendQueue()._get_value_type(_make_proxy()) == _PROXY_VALUE_TYPE


def test_value_type_flattens_plain_class_without_codec():
    # A plain JavaType.Class (no registered send codec) keeps the built-in name —
    # the hook is inert for the built-in types.
    c = JavaType.Class()
    c._flags_bit_map = 0
    c._fully_qualified_name = "pkg.Foo"
    c._kind = JavaType.FullyQualified.Kind.Class
    assert RpcSendQueue()._get_value_type(c) == 'org.openrewrite.java.tree.JavaType$Class'


def test_visit_type_defers_to_registered_send_codec(isolated_codec_registry):
    seen = []
    _register_proxy_codecs(seen)
    q = RpcSendQueue()
    PythonRpcSender()._visit_type(_make_proxy(), q)
    assert seen == ['send'], "registered send codec should serialize the proxy"


def test_visit_type_falls_through_for_plain_class():
    # Without a registered codec, _visit_type uses the hardcoded Class branch.
    c = JavaType.Class()
    c._flags_bit_map = 0
    c._fully_qualified_name = "pkg.Foo"
    c._kind = JavaType.FullyQualified.Kind.Class
    q = RpcSendQueue()
    PythonRpcSender()._visit_type(c, q)
    # The hardcoded Class serialization emits the FQN as one of its fields.
    assert any(m.get('value') == 'pkg.Foo' for m in q.q)


def test_round_trip_custom_javatype_class_subclass(isolated_codec_registry):
    """End-to-end: a proxy sent as a node's `.type` field survives send→receive
    with its value_type discriminator preserved and both codecs invoked."""
    seen = []
    _register_proxy_codecs(seen)
    proxy = _make_proxy("pkg.Foo")

    sender = PythonRpcSender()
    q = RpcSendQueue()

    class _Holder:
        pass

    holder = _Holder()
    holder.type = proxy
    # Mirrors how python_sender sends a node's `.type` field.
    q.get_and_send_as_ref(holder, lambda x: x.type,
                          lambda t: sender._visit_type(t, q))
    messages = q.q + [{'state': 'END_OF_OBJECT'}]

    # Discriminator preserved on the wire (not flattened to JavaType$Class).
    assert messages[0].get('valueType') == _PROXY_VALUE_TYPE

    rq = RpcReceiveQueue(refs={}, source_file_type=None,
                         pull=lambda: list(messages))
    result = rq.receive(None)

    assert seen == ['send', 'recv']
    assert isinstance(result, _ProxyClass)
    assert result._fully_qualified_name == "pkg.Foo"
