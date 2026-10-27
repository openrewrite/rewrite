# Copyright 2026 the original author or authors.
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

"""Every marker-free node shares ``Markers.EMPTY``, so its Markers travel as a ref: sent
in full once per connection and cited by a one-message ADD afterwards."""
import ast

from rewrite.python._parser_visitor import ParserVisitor
from rewrite.rpc.python_receiver import PythonRpcReceiver
from rewrite.rpc.receive_queue import RpcReceiveQueue
from rewrite.rpc.send_queue import RpcSendQueue

_CU_TYPE = "org.openrewrite.python.tree.Py$CompilationUnit"
_MARKERS_TYPE = "org.openrewrite.marker.Markers"

SOURCE = "x = 1\ny = 2\n"


def test_empty_markers_cross_the_wire_once_and_are_shared_on_receipt():
    cu = ParserVisitor(SOURCE, "<m>", None).visit_Module(ast.parse(SOURCE))
    data = list(RpcSendQueue(_CU_TYPE).generate(cu, None))

    full = [d for d in data if d.get('valueType') == _MARKERS_TYPE]
    assert len(full) == 1
    ref = full[0]['ref']
    assert ref is not None
    hits = [d for d in data
            if d.get('ref') == ref and d.get('valueType') is None and d.get('value') is None]
    assert len(hits) > 1, "the other nodes' Markers were not cited as refs"

    def pull():
        out = data[:]
        data.clear()
        return out

    rebuilt = PythonRpcReceiver().receive(None, RpcReceiveQueue({}, _CU_TYPE, pull))
    first, second = rebuilt.statements
    assert first.markers is second.markers
    assert rebuilt.markers is first.markers
