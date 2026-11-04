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

"""A `case` guard on a value/capture pattern desyncs the parser's source cursor.

Value patterns (`case 1`, `case Enum.X`) and capture patterns (`case v`) are returned bare, so
visit_match_case never consumed their guard's ` if <cond>` source: the cursor drifted and every
later node parsed against misaligned text, yielding children with `_id = None`. RpcSendQueue then
crashes at `el.element._id` on the None child, so the tree transfer fails. `case Enum.X if cond:`
is idiomatic 3.10+ code, absent from older corpora — which is why it went unnoticed.
"""
import ast
from dataclasses import fields, is_dataclass

from rewrite.python._parser_visitor import ParserVisitor
from rewrite.python.printer import PythonPrinter
from rewrite.rpc.python_receiver import PythonRpcReceiver
from rewrite.rpc.receive_queue import RpcReceiveQueue
from rewrite.rpc.send_queue import RpcSendQueue

_CU_TYPE = "org.openrewrite.python.tree.Py$CompilationUnit"

# A guarded value pattern (matching the home-assistant / customer construct), then any later node
# whose children the cursor desync would null out.
GUARDED = '''\
def f(x):
    match x:
        case 1 if x > 0:
            pass

def g(y):
    return y in ("a", "b", "c", "d")
'''


def _parse(src):
    return ParserVisitor(src, "<m>", None).visit_Module(ast.parse(src))


def test_guarded_value_pattern_has_no_null_padded_elements():
    """No JRightPadded/JLeftPadded may wrap a None element — that is the cursor-desync signature.
    Walks the raw dataclass tree (padding wrappers aren't visited by the LST visitor)."""
    cu = _parse(GUARDED)

    bad = []
    seen = set()
    stack = [cu]
    while stack:
        o = stack.pop()
        if id(o) in seen:
            continue
        seen.add(id(o))
        if is_dataclass(o):
            cls = type(o).__name__
            if cls in ("JRightPadded", "JLeftPadded") and getattr(o, "_element", "x") is None:
                bad.append(cls)
            stack.extend(getattr(o, f.name, None) for f in fields(o))
        elif isinstance(o, (list, tuple)):
            stack.extend(o)
    assert not bad, f"cursor desync produced padded wrappers with a None element: {bad}"


def test_guarded_value_pattern_round_trips_over_rpc():
    """The tree must serialise and rebuild — a None child crashes RpcSendQueue at el.element._id."""
    cu = _parse(GUARDED)
    data = list(RpcSendQueue(_CU_TYPE).generate(cu, None))

    def pull():
        out = data[:]
        data.clear()
        return out

    rebuilt = PythonRpcReceiver().receive(None, RpcReceiveQueue({}, _CU_TYPE, pull))
    assert PythonPrinter().print(rebuilt) == PythonPrinter().print(cu)


# A parenthesized (GROUP) sub-pattern as a class keyword-argument value. visit_MatchClass used
# __convert for kwarg values (unlike positional patterns), which doesn't consume the parens — same
# cursor-drift class as the guard bug, but guard-independent.
PAREN_KWARG = '''\
def f(x):
    match x:
        case Foo(k=(A() | B())):
            pass

def g(y):
    return y in ("a", "b", "c", "d")
'''


def test_parenthesized_class_kwarg_round_trips_over_rpc():
    cu = _parse(PAREN_KWARG)
    data = list(RpcSendQueue(_CU_TYPE).generate(cu, None))

    def pull():
        out = data[:]
        data.clear()
        return out

    rebuilt = PythonRpcReceiver().receive(None, RpcReceiveQueue({}, _CU_TYPE, pull))
    assert PythonPrinter().print(rebuilt) == PythonPrinter().print(cu)
