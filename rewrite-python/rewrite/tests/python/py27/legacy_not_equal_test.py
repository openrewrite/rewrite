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

"""The Py2 spelling ``<>`` of "not equal" is kept through a :class:`LegacyNotEqual` marker.

parso has no ``<>`` token, so the parser hands it ``!=`` in its place and puts the
marker on every comparison the source spelled ``<>``.
"""

import pytest

from rewrite import random_id, Markers
from rewrite.java import Space, JLeftPadded
from rewrite.java import tree as j
from rewrite.python._py2_parser_visitor import Py2ParserVisitor
from rewrite.python.markers import LegacyNotEqual
from rewrite.python.printer import PythonPrinter


def _identifier(name: str, prefix: str = "") -> j.Identifier:
    return j.Identifier(
        random_id(), Space([], prefix), Markers.EMPTY, [], name, None, None,
    )


def _binary_ne(left_name: str, right_name: str, *, legacy: bool) -> j.Binary:
    """Build ``a != b`` (or ``a <> b`` when ``legacy=True``) directly."""
    markers = Markers.EMPTY
    if legacy:
        markers = Markers.build(random_id(), [LegacyNotEqual(random_id())])
    return j.Binary(
        random_id(),
        Space.EMPTY,
        markers,
        _identifier(left_name),
        JLeftPadded(Space([], " "), j.Binary.Type.NotEqual, Markers.EMPTY),
        _identifier(right_name, prefix=" "),
        None,  # type
    )


def test_legacy_marker_emits_angle_brackets():
    """LegacyNotEqual marker on a ``NotEqual`` binary prints as ``<>``."""
    binary = _binary_ne("a", "b", legacy=True)
    assert PythonPrinter().print(binary) == "a <> b"


def test_no_marker_emits_bang_equals():
    """Without the marker, ``NotEqual`` prints as the Py3-style ``!=``."""
    binary = _binary_ne("a", "b", legacy=False)
    assert PythonPrinter().print(binary) == "a != b"


@pytest.mark.parametrize("source, legacy", [
    ("x = a <> b\n", 1),
    ("if a <> b and c<>d:\n    print \"<>\"  # <>\n", 2),
    ("x = a < b <> c != d\n", 1),
    ("x = a <> b\r\ny = 1 <>2\r\n", 2),
    ("x = a != b\n", 0),
])
def test_parser_keeps_the_spelling(source, legacy):
    cu = Py2ParserVisitor(source, "<test>", "2.7").parse()

    assert PythonPrinter().print(cu) == source
    assert _count_legacy(cu) == legacy


def _count_legacy(tree) -> int:
    from dataclasses import fields, is_dataclass
    count, seen, stack = 0, set(), [tree]
    while stack:
        o = stack.pop()
        if id(o) in seen:
            continue
        seen.add(id(o))
        if isinstance(o, j.Binary) and o.markers.find_first(LegacyNotEqual) is not None:
            count += 1
        if is_dataclass(o):
            stack.extend(getattr(o, f.name, None) for f in fields(o))
        elif isinstance(o, (list, tuple)):
            stack.extend(o)
    return count
