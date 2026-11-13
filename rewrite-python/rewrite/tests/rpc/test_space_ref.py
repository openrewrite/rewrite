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
import ast
import dataclasses

from rewrite.java import Space
from rewrite.python._parser_visitor import ParserVisitor
from rewrite.python.printer import PythonPrinter
from rewrite.rpc.python_receiver import PythonRpcReceiver
from rewrite.rpc.receive_queue import RpcReceiveQueue
from rewrite.rpc.send_queue import RpcSendQueue

_CU_TYPE = "org.openrewrite.python.tree.Py$CompilationUnit"
_SPACE_TYPE = "org.openrewrite.java.tree.Space"

SOURCE = "x = 1\ny = 2  # c\nz = (3," + " " * 51 + "4)\n"


def test_short_comment_free_space_is_sent_once_as_ref_and_others_by_value():
    cu = ParserVisitor(SOURCE, "<s>", None).visit_Module(ast.parse(SOURCE))
    data = list(RpcSendQueue(_CU_TYPE).generate(cu, None))

    by_value = [d for d in data if d.get('valueType') == _SPACE_TYPE and d.get('ref') is None]
    assert len(by_value) == 2, "only the commented and the long Space go by value"
    shared = {s.whitespace for s in _spaces(cu) if not s.comments and len(s.whitespace) <= 50}
    assert len([d for d in data if d.get('valueType') == _SPACE_TYPE and d.get('ref') is not None]) == len(shared)

    def pull():
        out = data[:]
        data.clear()
        return out

    rebuilt = PythonRpcReceiver().receive(None, RpcReceiveQueue({}, _CU_TYPE, pull))
    assert len({id(s) for s in _spaces(rebuilt) if s.whitespace == " "}) == 1
    assert PythonPrinter().print(rebuilt) == SOURCE


def _spaces(tree, seen=None):
    seen = set() if seen is None else seen
    if id(tree) in seen or isinstance(tree, (str, int, float, bool, type(None))):
        return
    seen.add(id(tree))
    if isinstance(tree, Space):
        yield tree
    children = tree if isinstance(tree, (list, tuple)) else [
        getattr(tree, f.name) for f in dataclasses.fields(tree)] if dataclasses.is_dataclass(tree) else []
    for child in children:
        yield from _spaces(child, seen)
