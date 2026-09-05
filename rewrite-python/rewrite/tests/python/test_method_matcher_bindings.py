# Copyright 2026 the original author or authors.
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

"""`MethodMatcher` resolving a receiver through import bindings.

The parser synthesises a class per imported module, and a file where any scope also binds
that name carries `JavaType.Unknown` on every call in it. Fixtures here therefore all hold a
shadowing binding, since that is the state a cursor is needed for.
"""

import ast
import dataclasses
from typing import List, Optional

from rewrite.java.support_types import JavaType
from rewrite.java.tree import MethodInvocation
from rewrite.python import MethodMatcher
from rewrite.python._parser_visitor import ParserVisitor
from rewrite.python.visitor import PythonVisitor

# A binding of `socket` in some other scope, which costs the whole file its module type.
SHADOW = 'def helper(socket):\n    return socket\n'


def matches(source: str, pattern: str, *, with_cursor: bool = True,
            match_unknown_types: bool = False,
            stamp_declaring: Optional[str] = None) -> List[bool]:
    """One entry per call in `source`, outermost first: whether `pattern` matched it.

    `stamp_declaring` replaces each call's declaring type with a resolved class, which lets a
    test disagree with what the imports say and see which of the two the matcher believed.
    """
    cu = ParserVisitor(source, None, None).visit(ast.parse(source))
    matcher = MethodMatcher.create(pattern)
    results: List[bool] = []

    class Collect(PythonVisitor):
        def visit_method_invocation(self, method: MethodInvocation, p) -> MethodInvocation:
            probe = method
            if stamp_declaring is not None and method.method_type is not None:
                declaring = JavaType.ShallowClass()
                declaring._flags_bit_map = 0
                declaring._fully_qualified_name = stamp_declaring
                declaring._kind = JavaType.FullyQualified.Kind.Class
                probe = method.replace(_method_type=dataclasses.replace(
                    method.method_type, _declaring_type=declaring))
            results.append(matcher.matches(probe, match_unknown_types,
                                           cursor=self.cursor if with_cursor else None))
            return super().visit_method_invocation(method, p)

    Collect().visit(cu, None)
    return results


def declaring_types(source: str) -> List[object]:
    """The declaring type of each call, to tell a lost module type from a resolved one."""
    cu = ParserVisitor(source, None, None).visit(ast.parse(source))
    found: List[object] = []

    class Collect(PythonVisitor):
        def visit_method_invocation(self, method: MethodInvocation, p) -> MethodInvocation:
            found.append(None if method.method_type is None else method.method_type.declaring_type)
            return super().visit_method_invocation(method, p)

    Collect().visit(cu, None)
    return found


def test_a_shadowing_binding_costs_the_file_its_module_type():
    # The premise the rest of this file rests on, and the reason the fallback exists.
    resolved = declaring_types('import socket\nsocket.getfqdn()\n')
    assert [type(t).__name__ for t in resolved] == ['ShallowClass']

    lost = declaring_types('import socket\nsocket.getfqdn()\n' + SHADOW)
    assert [type(t).__name__ for t in lost] == ['Unknown']


def test_module_receiver_resolves_through_its_import_and_alias():
    assert matches('import socket\nsocket.getfqdn()\n' + SHADOW, 'socket getfqdn(..)') == [True]
    assert matches('import socket as s\ns.getfqdn()\n' + SHADOW.replace('socket', 's'),
                   'socket getfqdn(..)') == [True]


def test_a_local_that_spells_the_module_is_not_the_module():
    source = ('import socket\n'
              'real = socket.getfqdn()\n'
              'def helper(socket):\n'
              '    return socket.getfqdn()\n')
    assert matches(source, 'socket getfqdn(..)') == [True, False]


def test_a_relative_import_is_never_the_module_of_the_same_name():
    source = 'from .socket import getfqdn\ngetfqdn()\ndef helper(getfqdn):\n    return getfqdn\n'
    assert matches(source, 'socket getfqdn(..)') == [False]


def test_an_import_that_only_exists_for_type_checking_is_not_called_at_runtime():
    source = ('from typing import TYPE_CHECKING\n'
              'if TYPE_CHECKING:\n'
              '    import socket\n'
              'socket.getfqdn()\n' + SHADOW)
    assert matches(source, 'socket getfqdn(..)') == [False]


def test_without_a_cursor_the_receiver_is_not_resolved():
    assert matches('import socket\nsocket.getfqdn()\n' + SHADOW, 'socket getfqdn(..)',
                   with_cursor=False) == [False]


def test_a_resolved_declaring_type_decides_it_against_the_imports_and_the_spelling():
    # The imports say `socket` and the declaring type says `json`.
    stamped = 'import socket\nsocket.getfqdn()\n'
    assert matches(stamped, 'json getfqdn(..)', stamp_declaring='json') == [True]
    assert matches(stamped, 'socket getfqdn(..)', stamp_declaring='json') == [False]

    # `match_unknown_types` reads the spelling, which names `socket`. Java falls through
    # to it here; a resolved declaring type decides instead.
    aliased = 'import json as socket\nsocket.dumps(1)\n'
    assert matches(aliased, 'socket dumps(..)', match_unknown_types=True) == [False]
    assert matches(aliased, 'json dumps(..)') == [True]


def test_a_from_imported_receiver_is_the_member_not_its_module():
    source = ('from datetime import datetime\n'
              'datetime.now()\n'
              'def helper(datetime):\n'
              '    return datetime\n')
    assert matches(source, 'datetime.datetime now(..)') == [True]

    assert matches(source, 'datetime now(..)') == [False]


def test_a_bare_call_reads_the_member_the_import_bound_not_the_local_name():
    plain = 'from socket import getfqdn\ngetfqdn()\ndef helper(getfqdn):\n    return getfqdn\n'
    assert matches(plain, 'socket getfqdn(..)') == [True]

    aliased = ('from socket import gethostname as getfqdn\n'
               'getfqdn()\n'
               'def helper(getfqdn):\n'
               '    return getfqdn\n')
    assert matches(aliased, 'socket gethostname(..)') == [True]
    assert matches(aliased, 'socket getfqdn(..)') == [False]


def test_a_dotted_import_binds_its_root_and_an_alias_binds_the_whole_path():
    plain = 'import os.path\nos.getcwd()\ndef helper(os):\n    return os\n'
    assert matches(plain, 'os getcwd(..)') == [True]
    assert matches(plain, 'os.path getcwd(..)') == [False]

    aliased = 'import os.path as p\np.join(1)\ndef helper(p):\n    return p\n'
    assert matches(aliased, 'os.path join(..)') == [True]
    assert matches(aliased, 'os join(..)') == [False]

    # An alias spelled like the root package still binds the whole path.
    shadowing_alias = 'import os.path as os\nos.join(1)\ndef helper(os):\n    return os\n'
    assert matches(shadowing_alias, 'os.path join(..)') == [True]
    assert matches(shadowing_alias, 'os join(..)') == [False]


def test_arguments_are_still_checked_when_the_receiver_resolves():
    source = 'import socket\nsocket.getfqdn(1)\n' + SHADOW
    assert matches(source, 'socket getfqdn(int)') == [True]

    assert matches(source, 'socket getfqdn(str)') == [False]

