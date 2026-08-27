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

"""Regression tests for the Python printer honoring the requested marker printer.

Assertions pin the *exact* rendered output for each marker printer so a test can't
pass merely because "some marker-ish text appeared":
  - FENCED            -> ``{{<uuid>}}x{{<uuid>}}`` (paired fences, description not leaked)
  - SANITIZED         -> marker fully stripped
  - SEARCH_MARKERS_ONLY / DEFAULT / absent -> ``/*~~(desc)~~>*/`` comment (unchanged)
and ``handle_print`` warns only on a genuinely unknown name.
"""

import ast
import logging
from pathlib import Path
from uuid import uuid4

from rewrite import Marker, Markers, Tree
from rewrite.markers import SearchResult, Markup
from rewrite.java import J, Identifier
from rewrite.python.visitor import PythonVisitor
from rewrite.python._parser_visitor import ParserVisitor
from rewrite.python.printer import PythonPrinter, PrintOutputCapture
from rewrite.tree import PrintOutputCapture as CorePrintOutputCapture

MP = CorePrintOutputCapture.MarkerPrinter


def _parse_python(source: str) -> Tree:
    visitor = ParserVisitor(source, None, None)
    cu = visitor.visit_Module(ast.parse(source))
    return cu.replace(source_path=Path("test.py"))


class _MarkIdentifier(PythonVisitor):
    """Attaches the given marker(s) to the identifier named ``x``."""

    def __init__(self, *markers: Marker):
        self._markers = markers

    def visit_identifier(self, ident: Identifier, p) -> J:
        ident = super().visit_identifier(ident, p)
        if ident.simple_name == "x":
            return ident.replace(markers=Markers(uuid4(), list(self._markers)))
        return ident


def _print(source: str, marker_printer, *markers: Marker) -> str:
    marked = _MarkIdentifier(*markers).visit(_parse_python(source), None)
    return PythonPrinter().print(marked, PrintOutputCapture(marker_printer))


# --- printer-level: exact output per marker printer --------------------------


def test_fenced_marker_printer_emits_paired_uuid_fences():
    marker = SearchResult(uuid4(), "FINDME")
    fence = "{{" + str(marker.id) + "}}"

    out = _print("x = 1\n", MP.FENCED, marker)

    # Exact paired open/close fences around the marked identifier; FENCED keys off the
    # uuid, so the marker's description must NOT leak and no comment marker appears.
    assert out == f"{fence}x{fence} = 1\n"
    assert "FINDME" not in out
    assert "/*~~" not in out


def test_fenced_marker_printer_fences_markup_markers():
    # _FencedMarkerPrinter fences Markup as well as SearchResult; cover that branch.
    marker = Markup.warn("deprecated")
    fence = "{{" + str(marker.id) + "}}"

    out = _print("x = 1\n", MP.FENCED, marker)

    assert out == f"{fence}x{fence} = 1\n"
    assert "deprecated" not in out


def test_fenced_marker_printer_multiple_markers_each_paired():
    m1 = SearchResult(uuid4(), None)
    m2 = SearchResult(uuid4(), None)
    f1, f2 = "{{" + str(m1.id) + "}}", "{{" + str(m2.id) + "}}"

    out = _print("x = 1\n", MP.FENCED, m1, m2)

    # Both markers fence the node, in marker order, on each side.
    assert out == f"{f1}{f2}x{f1}{f2} = 1\n"


def test_fenced_marker_printer_no_op_without_markers():
    # No marker on the tree => FENCED must not invent any fence.
    out = _print("y = 1\n", MP.FENCED)

    assert out == "y = 1\n"


def test_sanitized_marker_printer_strips_markers():
    marker = SearchResult(uuid4(), "FINDME")

    out = _print("x = 1\n", MP.SANITIZED, marker)

    # Marker and its description fully removed -> clean source.
    assert out == "x = 1\n"


def test_search_markers_only_renders_search_comment():
    marker = SearchResult(uuid4(), "FINDME")

    out = _print("x = 1\n", MP.SEARCH_MARKERS_ONLY, marker)

    # Honored (not stripped like SANITIZED, not fenced like FENCED): search marker as comment.
    assert out == "/*~~(FINDME)~~>*/x = 1\n"


def test_default_marker_printer_renders_comment_marker():
    marker = SearchResult(uuid4(), "FINDME")

    # None => python DefaultMarkerPrinter: renders the search marker as a comment (unchanged).
    out = _print("x = 1\n", None, marker)

    assert out == "/*~~(FINDME)~~>*/x = 1\n"


# --- handle_print (the RPC seam that maps the wire name to a printer) ---------


def _handle_print(marker_printer_name, monkeypatch, *markers: Marker) -> str:
    """Drive the RPC ``handle_print`` handler with a stubbed Java fetch."""
    from rewrite.rpc import server

    marked = _MarkIdentifier(*markers).visit(_parse_python("x = 1\n"), None)
    monkeypatch.setattr(server, "get_object_from_java", lambda *a, **k: marked)

    params = {"treeId": str(uuid4()), "sourceFileType": "python"}
    if marker_printer_name is not None:
        params["markerPrinter"] = marker_printer_name
    return server.handle_print(params)


def test_handle_print_honors_fenced(monkeypatch):
    marker = SearchResult(uuid4(), "FINDME")
    fence = "{{" + str(marker.id) + "}}"

    out = _handle_print("FENCED", monkeypatch, marker)

    assert out == f"{fence}x{fence} = 1\n"


def test_handle_print_honors_sanitized(monkeypatch):
    marker = SearchResult(uuid4(), "FINDME")

    out = _handle_print("SANITIZED", monkeypatch, marker)

    assert out == "x = 1\n"


def test_handle_print_defaults_when_marker_printer_absent(monkeypatch):
    marker = SearchResult(uuid4(), "FINDME")

    # No markerPrinter field (older Java peers) => default rendering, no crash.
    out = _handle_print(None, monkeypatch, marker)

    assert out == "/*~~(FINDME)~~>*/x = 1\n"


def test_handle_print_warns_only_on_unknown_marker_printer(monkeypatch, caplog):
    marker = SearchResult(uuid4(), "FINDME")

    # Every real wire value (and absent) is recognized -> no warning.
    with caplog.at_level(logging.WARNING):
        for known in ("FENCED", "SANITIZED", "SEARCH_MARKERS_ONLY", "DEFAULT", None):
            _handle_print(known, monkeypatch, marker)
    assert "Unknown markerPrinter" not in caplog.text

    # A genuine unknown (e.g. a FENCED typo) must warn AND fall back without crashing.
    caplog.clear()
    with caplog.at_level(logging.WARNING):
        out = _handle_print("FENCE_TYPO", monkeypatch, marker)
    assert "Unknown markerPrinter 'FENCE_TYPO'" in caplog.text
    assert out == "/*~~(FINDME)~~>*/x = 1\n"
