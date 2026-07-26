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

"""PEP 695 type-alias type parameters must survive the RPC round trip.

All four codecs (Python + Java, sender + receiver) previously omitted
TypeAlias.type_parameters, silently narrowing `type X[T] = ...` to `type X = ...`.
The loss is invisible to the in-process printer suite and baked into every LST,
so only a serialize/deserialize round trip catches it.
"""
import ast

import pytest

from rewrite.python._parser_visitor import ParserVisitor
from rewrite.python.printer import PythonPrinter
from rewrite.rpc.python_receiver import PythonRpcReceiver
from rewrite.rpc.receive_queue import RpcReceiveQueue
from rewrite.rpc.send_queue import RpcSendQueue

_CU_TYPE = "org.openrewrite.python.tree.Py$CompilationUnit"


def _round_trip_print(src):
    cu = ParserVisitor(src, "<m>", None).visit_Module(ast.parse(src))
    data = list(RpcSendQueue(_CU_TYPE).generate(cu, None))

    def pull():
        out = data[:]
        data.clear()
        return out

    rebuilt = PythonRpcReceiver().receive(None, RpcReceiveQueue({}, _CU_TYPE, pull))
    return PythonPrinter().print(cu), PythonPrinter().print(rebuilt)


# Spaced out to also exercise whitespace fidelity: JContainer.before (before `[`) and the
# element padding (inside the brackets, around the `:` bound and the commas).
@pytest.mark.parametrize("src", [
    "type X [ T ] = list[T]\n",
    "type X [ T : int ] = list[T]\n",
    "type F [ T , *Ts , **P ] = Callable[P, T]\n",
    "type X = int\n",  # no params — must remain a no-op
])
def test_type_alias_params_survive_rpc(src):
    before, after = _round_trip_print(src)
    assert after == before
