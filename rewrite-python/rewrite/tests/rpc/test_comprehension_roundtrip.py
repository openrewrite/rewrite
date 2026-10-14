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

"""An edit inside a comprehension's clauses reaches the peer as a delta."""
import ast

from rewrite.java import Space
from rewrite.python._parser_visitor import ParserVisitor
from rewrite.python.printer import PythonPrinter
from rewrite.python.visitor import PythonVisitor
from rewrite.rpc.python_receiver import PythonRpcReceiver
from rewrite.rpc.receive_queue import RpcReceiveQueue
from rewrite.rpc.send_queue import RpcSendQueue

_CU_TYPE = 'org.openrewrite.python.tree.Py$CompilationUnit'


def _parse(source: str):
    return ParserVisitor(source, None, None).visit_Module(ast.parse(source))


def _rpc_round_trip(before, after):
    batch = list(RpcSendQueue(_CU_TYPE).generate(after, before))

    def pull():
        out = batch[:]
        batch.clear()
        return out

    return PythonRpcReceiver().receive(before, RpcReceiveQueue({}, _CU_TYPE, pull))


class _WidenClausePrefix(PythonVisitor):
    def visit_comprehension_clause(self, clause, p):
        clause = super().visit_comprehension_clause(clause, p)
        return clause.with_prefix(Space.build([], "  "))


class _WidenConditionPrefix(PythonVisitor):
    def visit_comprehension_condition(self, condition, p):
        condition = super().visit_comprehension_condition(condition, p)
        return condition.with_prefix(Space.build([], "  "))


def test_edited_comprehension_clause_round_trips():
    # given
    cu = _parse("[x for x in xs if x]\n")
    edited = _WidenClausePrefix().visit(cu, 0)

    # when
    rebuilt = _rpc_round_trip(cu, edited)

    # then
    assert PythonPrinter().print(rebuilt) == "[x  for x in xs if x]\n"


def test_edited_comprehension_condition_round_trips():
    # given
    cu = _parse("[x for x in xs if x]\n")
    edited = _WidenConditionPrefix().visit(cu, 0)

    # when
    rebuilt = _rpc_round_trip(cu, edited)

    # then
    assert PythonPrinter().print(rebuilt) == "[x for x in xs  if x]\n"
