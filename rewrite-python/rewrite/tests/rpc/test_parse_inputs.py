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

"""Parse inputs that name a file the server reads itself.

The parser and printer carry ``\\r\\n`` through when handed source text, so
only an input read off disk covers the read that can lose it.
"""
import pytest

from rewrite.parser import ParseError
from rewrite.python.printer import PythonPrinter
from rewrite.rpc import server
from rewrite.rpc.server import handle_parse, handle_parse_project, local_objects

CRLF_SOURCE = "import sys\r\n\r\n\r\ndef greet(name):\r\n    # a comment\r\n    print(name)\r\n"


@pytest.fixture
def crlf_file(tmp_path):
    path = tmp_path / "crlf.py"
    path.write_bytes(CRLF_SOURCE.encode("utf-8"))
    return path


def _parse(path, options=None):
    # The shape a JVM peer sends for a file input: a path, and no source text.
    ids = handle_parse({"inputs": [{"sourcePath": str(path)}],
                        "relativeTo": str(path.parent),
                        "options": options or {}})
    assert len(ids) == 1
    return local_objects[ids[0]]


@pytest.mark.parametrize("newline", ["\r\n", "\r"], ids=["crlf", "cr"])
def test_a_file_keeps_its_own_line_endings(tmp_path, newline):
    source = CRLF_SOURCE.replace("\r\n", newline)
    path = tmp_path / "endings.py"
    path.write_bytes(source.encode("utf-8"))

    assert PythonPrinter().print(_parse(path)) == source


def test_an_unreadable_file_costs_only_its_own_slot(crlf_file):
    ids = handle_parse({"inputs": [{"sourcePath": str(crlf_file)},
                                   {"sourcePath": str(crlf_file.parent / "gone.py")}],
                        "relativeTo": str(crlf_file.parent)})

    assert len(ids) == 2
    assert PythonPrinter().print(local_objects[ids[0]]) == CRLF_SOURCE
    assert isinstance(local_objects[ids[1]], ParseError)


def test_parse_that_loses_source_becomes_a_parse_error(crlf_file, monkeypatch):
    monkeypatch.setattr(server, "PythonPrinter", _LosingPrinter)
    parsed = _parse(crlf_file)
    assert isinstance(parsed, ParseError)
    assert "is not print idempotent" in _exception_message(parsed)


def test_the_print_check_is_off_when_the_client_says_so(crlf_file, monkeypatch):
    monkeypatch.setattr(server, "PythonPrinter", _LosingPrinter)
    options = {"org.openrewrite.requirePrintEqualsInput": "false"}
    assert not isinstance(_parse(crlf_file, options), ParseError)


def test_a_project_parse_honours_the_print_check_option(crlf_file, monkeypatch):
    monkeypatch.setattr(server, "PythonPrinter", _LosingPrinter)

    def types(options):
        return [item["sourceFileType"]
                for item in handle_parse_project({"projectPath": str(crlf_file.parent),
                                                  "options": options})]

    assert types({}) == ["org.openrewrite.tree.ParseError"]

    assert types({"org.openrewrite.requirePrintEqualsInput": "false"}) == [
        "org.openrewrite.python.tree.Py$CompilationUnit"]


class _LosingPrinter:
    """Stands in for a printer that drops part of its input."""

    def print(self, _cu):
        return ""


def _exception_message(parse_error):
    return parse_error.markers.markers[0].message
