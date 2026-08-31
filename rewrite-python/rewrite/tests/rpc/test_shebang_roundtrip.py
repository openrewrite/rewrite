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

"""A leading ``#!`` line survives the RPC round trip as a Py.Shebang statement.

The in-process printer suite exercises parse/print, but only a serialize/
deserialize round trip covers the sender/receiver codecs for the new node.
"""
import ast

import pytest

from rewrite.python import Shebang
from rewrite.python._parser_visitor import ParserVisitor
from rewrite.python.printer import PythonPrinter
from rewrite.rpc.python_receiver import PythonRpcReceiver
from rewrite.rpc.receive_queue import RpcReceiveQueue
from rewrite.rpc.send_queue import RpcSendQueue

_CU_TYPE = "org.openrewrite.python.tree.Py$CompilationUnit"


def _round_trip(src):
    cu = ParserVisitor(src, "<m>", None).visit_Module(ast.parse(src))
    data = list(RpcSendQueue(_CU_TYPE).generate(cu, None))

    def pull():
        out = data[:]
        data.clear()
        return out

    rebuilt = PythonRpcReceiver().receive(None, RpcReceiveQueue({}, _CU_TYPE, pull))
    return cu, rebuilt


@pytest.mark.parametrize("src", [
    "#!/usr/bin/env python3\nprint(\"hi\")\n",
    "#!/usr/bin/env python3\n\nx = 1\n",
    "#!/usr/bin/env python3\n# a comment\nx = 1\n",
    "#!/usr/bin/env python3\n",
])
def test_shebang_survives_rpc(src):
    cu, rebuilt = _round_trip(src)
    assert isinstance(rebuilt.statements[0], Shebang)
    assert rebuilt.statements[0].text == "#!/usr/bin/env python3"
    assert PythonPrinter().print(rebuilt) == PythonPrinter().print(cu) == src
