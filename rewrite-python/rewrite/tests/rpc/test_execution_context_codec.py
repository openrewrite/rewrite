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

"""The wire format for serving an ExecutionContext over GetObject.

When Python hosts a run and delegates a Visit to a Java peer, Java fetches
the ``p`` value (the execution context) back over GetObject. This asserts
the ``ADD`` + ``END_OF_OBJECT`` wire format that
``_register_execution_context_codec`` (see ``python_receiver.py``) produces.
"""
import ast
from pathlib import Path

import pytest

from rewrite import DelegatingExecutionContext, InMemoryExecutionContext
from rewrite.markers import SearchResult
from rewrite.python._parser_visitor import ParserVisitor
from rewrite.rpc import server


def test_handle_get_object_serializes_execution_context():
    ctx = InMemoryExecutionContext()
    server.local_objects['ctx-test'] = ctx
    try:
        batch = server.handle_get_object({'id': 'ctx-test'})
    finally:
        server.handle_reset({})

    assert batch[0]['state'] == 'ADD'
    assert batch[0]['valueType'] == 'org.openrewrite.InMemoryExecutionContext'
    assert batch[0].get('value') is None
    assert batch[-1]['state'] == 'END_OF_OBJECT'


def test_a_context_of_any_class_is_named_for_the_host():
    # sent with no type, there is nothing in the message for the host to build a context from
    ctx = DelegatingExecutionContext(InMemoryExecutionContext())
    server.local_objects['ctx-test'] = ctx
    try:
        batch = server.handle_get_object({'id': 'ctx-test'})
    finally:
        server.handle_reset({})

    assert batch == [{'state': 'ADD', 'valueType': 'org.openrewrite.InMemoryExecutionContext'},
                     {'state': 'END_OF_OBJECT'}]


@pytest.mark.requires_java_rpc
def test_the_host_runs_a_recipe_under_a_context_of_any_class(java_rpc):
    from rewrite.rpc.rpc_recipe import JavaRecipeVisitor, prepare_java_recipe

    source = "x = 1\n"
    cu = ParserVisitor(source, None, None).visit_Module(ast.parse(source)).replace(source_path=Path("a.py"))
    find = JavaRecipeVisitor(prepare_java_recipe('org.openrewrite.FindSourceFiles', {'filePattern': '**/*.py'}))

    found = find.visit(cu, DelegatingExecutionContext(InMemoryExecutionContext()))

    assert found.markers.find_first(SearchResult) is not None
