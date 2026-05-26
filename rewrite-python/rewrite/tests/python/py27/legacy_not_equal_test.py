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

"""Pin the :class:`LegacyNotEqual` marker → printer wiring.

The Py2 spelling ``<>`` for "not equal" is not actually reachable through
the current parso 0.7 pipeline (parso pre-rewrites ``<>`` to ``!=`` before
the visitor sees it), so the :class:`LegacyNotEqual` marker is dormant in
practice. The fold logic in :mod:`_py2_parser_visitor` and the printer
branch in :mod:`printer` are nonetheless wired up so a future parso
upgrade — or source pre-processing that injects a ``<>`` operator leaf —
will Just Work.

This test pins that wiring by constructing the LST node directly. If a
future change breaks the marker → ``<>`` rendering path, this test fails
loudly instead of silently letting the bug ship the day the parser
upstream gains ``<>`` support.
"""

from rewrite import random_id, Markers
from rewrite.java import Space, JLeftPadded
from rewrite.java import tree as j
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
