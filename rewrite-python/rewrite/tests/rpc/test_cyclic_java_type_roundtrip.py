# Copyright 2025 the original author or authors.
#
# Licensed under the Moderne Source Available License (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     https://docs.moderne.io/licensing/moderne-source-available-license
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

"""Cyclic JavaType graphs must preserve identity across the RPC round trip.

Real type graphs are almost always cyclic (a Class's methods point back at it
via declaringType, members via owner, etc.). The receiver pre-registers a
placeholder in `_refs` when it sees ADD-with-ref, so any back-reference that
resolves while the object is still being received returns that placeholder.
The codecs must therefore populate the placeholder instance in place (as the
Java and TypeScript receivers do) — a codec that builds a fresh instance leaves
every back-reference pointing at the empty placeholder.
"""
import ast

from rewrite.execution import InMemoryExecutionContext
from rewrite.java.support_types import JavaType as JT
from rewrite.python._parser_visitor import ParserVisitor
from rewrite.python.visitor import PythonVisitor
from rewrite.rpc.python_receiver import PythonRpcReceiver
from rewrite.rpc.receive_queue import RpcReceiveQueue
from rewrite.rpc.send_queue import RpcSendQueue
from rewrite.utils import replace_if_changed

_CU_TYPE = "org.openrewrite.python.tree.Py$CompilationUnit"


def _round_trip_type(java_type, field='_type'):
    """Attach `java_type` to the sole identifier in `x = 1` (as `_type` or
    `_field_type`), round trip the CU, and return that field on the received
    identifier."""
    cu = ParserVisitor("x = 1", "<m>", None).visit_Module(ast.parse("x = 1"))

    class _SetType(PythonVisitor):
        def visit_identifier(self, ident, p):
            return replace_if_changed(ident, **{field: java_type})

    cu = _SetType().visit(cu, InMemoryExecutionContext())

    data = list(RpcSendQueue(_CU_TYPE).generate(cu, None))

    def pull():
        out = data[:]
        data.clear()
        return out

    rebuilt = PythonRpcReceiver().receive(None, RpcReceiveQueue({}, _CU_TYPE, pull))

    received = []

    class _GetType(PythonVisitor):
        def visit_identifier(self, ident, p):
            received.append(getattr(ident, field))
            return ident

    _GetType().visit(rebuilt, InMemoryExecutionContext())
    assert len(received) == 1
    return received[0]


def _make_cyclic_class():
    """A Class whose method and member both point back at it, as in real type graphs."""
    cls = JT.Class()
    cls._flags_bit_map = 1
    cls._kind = JT.FullyQualified.Kind.Class
    cls._fully_qualified_name = 'my.Example'
    cls._type_parameters = None
    cls._supertype = None
    cls._owning_class = None
    cls._annotations = None
    cls._interfaces = None
    method = JT.Method(_declaring_type=cls, _name='run', _return_type=JT.Primitive.Void)
    member = JT.Variable(_name='self_ref', _owner=cls, _type=cls)
    cls._members = [member]
    cls._methods = [method]
    return cls


def test_class_with_back_referencing_method_and_member():
    received = _round_trip_type(_make_cyclic_class())

    assert isinstance(received, JT.Class)
    assert received.fully_qualified_name == 'my.Example'

    method = received._methods[0]
    assert method._name == 'run'
    assert method._declaring_type is received

    member = received._members[0]
    assert member._name == 'self_ref'
    assert member._owner is received
    assert member._type is received


def test_method_entered_before_its_declaring_class():
    """Same cycle entered from the Method side: the Class's methods list refs
    back to the Method that is still being received."""
    cls = _make_cyclic_class()
    method = cls._methods[0]

    received = _round_trip_type(method)

    assert isinstance(received, JT.Method)
    assert received._name == 'run'
    declaring = received._declaring_type
    assert isinstance(declaring, JT.Class)
    assert declaring.fully_qualified_name == 'my.Example'
    assert declaring._methods[0] is received


def test_cyclic_variable_as_identifier_field_type():
    """The cycle Variable → owner Class → members → Variable must survive the
    round trip when the Variable is carried by Identifier.field_type."""
    cls = _make_cyclic_class()
    member = cls._members[0]

    received = _round_trip_type(member, field='_field_type')

    assert isinstance(received, JT.Variable)
    assert received._name == 'self_ref'
    owner = received._owner
    assert isinstance(owner, JT.Class)
    assert owner.fully_qualified_name == 'my.Example'
    assert owner._members[0] is received
    assert received._type is owner


def test_generic_type_variable_recursive_bound():
    """T extends Comparable<T>: the bound's type parameter refs the GTV itself."""
    gtv = JT.GenericTypeVariable(_name='T', _variance=JT.GenericTypeVariable.Variance.Covariant)
    comparable = JT.Class()
    comparable._flags_bit_map = 1
    comparable._kind = JT.FullyQualified.Kind.Interface
    comparable._fully_qualified_name = 'java.lang.Comparable'
    comparable._type_parameters = None
    comparable._supertype = None
    comparable._owning_class = None
    comparable._annotations = None
    comparable._interfaces = None
    comparable._members = None
    comparable._methods = None
    parameterized = JT.Parameterized()
    parameterized._type = comparable
    parameterized._type_parameters = [gtv]
    gtv._bounds = [parameterized]

    received = _round_trip_type(gtv)

    assert isinstance(received, JT.GenericTypeVariable)
    assert received._name == 'T'
    bound = received._bounds[0]
    assert isinstance(bound, JT.Parameterized)
    assert bound._type.fully_qualified_name == 'java.lang.Comparable'
    assert bound._type_parameters[0] is received
