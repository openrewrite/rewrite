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

"""Cross-construct regression sweep for the Py2 parser visitor.

Parses representative Py2 idioms covering the constructs each round in the
Phase 3 effort added, and asserts no ``<grammar_node>`` placeholder
identifiers survive anywhere in the resulting LST.
"""

import dataclasses
from typing import Iterator

import pytest

from rewrite.java import tree as j
from rewrite.python import tree as py
from rewrite.python._py2_parser_visitor import Py2ParserVisitor


def _walk(node) -> Iterator:
    """Yield every dataclass-field descendant of ``node`` exactly once,
    skipping non-tree containers gracefully."""
    if node is None:
        return
    yield node
    if dataclasses.is_dataclass(node):
        for f in dataclasses.fields(node):
            val = getattr(node, f.name, None)
            yield from _walk_value(val)


def _walk_value(val) -> Iterator:
    if val is None:
        return
    if isinstance(val, (list, tuple)):
        for item in val:
            yield from _walk_value(item)
        return
    # JRightPadded / JLeftPadded / JContainer all expose `.element` or
    # `.elements`.
    if hasattr(val, 'element') and not dataclasses.is_dataclass(val):
        yield from _walk_value(val.element)
        return
    if hasattr(val, 'elements') and not dataclasses.is_dataclass(val):
        yield from _walk_value(val.elements)
        return
    if dataclasses.is_dataclass(val):
        yield from _walk(val)


def _placeholder_names(tree) -> list:
    """Return any identifier name shaped like ``<grammar_node>``."""
    out = []
    for node in _walk(tree):
        if isinstance(node, j.Identifier):
            name = node.simple_name
            if name.startswith("<") and name.endswith(">"):
                out.append(name)
    return out


COMPREHENSIVE = '''\
# Copyright header — should be tolerated.
"""Module docstring."""

import os
import sys as system
from collections import OrderedDict
from os.path import join, dirname
from . import siblings
from .. import grandparent

CONST = 42
PI = 3.14
NAME = "hello"

GLOBAL_DICT = {"a": 1, "b": 2}
TUPLE_C = (1, 2, 3)
SET_C = {1, 2, 3}
EMPTY_LIST = []
EMPTY_DICT = {}
EMPTY_TUPLE = ()


def _private(x, y=1, *args, **kw):
    """Function with all parameter shapes."""
    total = x + y
    for arg in args:
        total += arg
    for key in kw:
        total += kw[key]
    return total


def consumer(items):
    yield items[0]
    yield items[1:]
    yield items[::2]
    yield items[1:10:2]


@cached
@logger.timing
def decorated(a, b):
    return _private(a, b=b)


class Old:
    pass


class Modern(Base):
    CLASS_ATTR = 1

    def __init__(self, value):
        self.value = value
        self.entries = [v * 2 for v in range(value) if v > 0]

    def method(self):
        try:
            result = self._compute()
        except ValueError, e:
            return None
        except (TypeError, KeyError) as err:
            raise RuntimeError("composite")
        else:
            return result
        finally:
            self._cleanup()


class Multi(A, B):
    @classmethod
    def factory(cls, **kwargs):
        return cls(**kwargs)

    @staticmethod
    def helper(*items):
        return sum(items)


def compound(xs):
    if not xs:
        return None
    elif len(xs) == 1:
        return xs[0]
    else:
        return sum(xs) / len(xs)


def memberships(items, valid):
    matches = [x for x in items if x in valid and x not in EXCLUDED]
    missing = {k: v for k, v in items if v is not None}
    return matches, missing


def streams():
    for i, j in enumerate(items):
        if i % 2 == 0:
            print "even", i, j
        else:
            print >> sys.stderr, "odd", i


def cleanup():
    global counter
    counter += 1
    del temp_a, temp_b
    assert counter > 0, "negative count"


with open("f") as fh:
    contents = fh.read()


with open("a") as a, open("b") as b:
    combined = a.read() + b.read()


f = lambda x, y=1: x * y
double = lambda *args: [a * 2 for a in args]


# Augmented assignments
x = 0
x += 1
x -= 1
x *= 2
x //= 2
x **= 2

# Chained assignment
a = b = c = 0


# Generator expression as a call argument
total = sum(v for v in values)
'''


def test_comprehensive_no_placeholders():
    cu = Py2ParserVisitor(COMPREHENSIVE, "<test>", "2.7").parse()
    placeholders = []
    for stmt in cu.statements:
        placeholders.extend(_placeholder_names(stmt))
    assert placeholders == [], (
        f"placeholders survived in the comprehensive Py2 sweep: "
        f"{sorted(set(placeholders))}"
    )


def test_compilation_unit_is_well_formed():
    cu = Py2ParserVisitor(COMPREHENSIVE, "<test>", "2.7").parse()
    assert isinstance(cu, py.CompilationUnit)
    # The full file should parse to many top-level statements.
    assert len(cu.statements) >= 20

    # Type spot-checks across the file.
    types = [type(s).__name__ for s in cu.statements]
    assert "MethodDeclaration" in types
    assert "ClassDeclaration" in types
    assert "Import" in types or "MultiImport" in types
    # An augmented assignment should appear.
    assert "AssignmentOperation" in types
    # A chained assignment should appear.
    assert "ChainedAssignment" in types
