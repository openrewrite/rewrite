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
from rewrite.rpc.server import handle_parse, local_objects

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


def test_file_input_is_read_from_disk_preserving_crlf(crlf_file):
    assert PythonPrinter().print(_parse(crlf_file)) == CRLF_SOURCE


def test_parse_that_loses_source_becomes_a_parse_error(crlf_file, monkeypatch):
    monkeypatch.setattr(server, "PythonPrinter", _LosingPrinter)
    parsed = _parse(crlf_file)
    assert isinstance(parsed, ParseError)
    assert "is not print idempotent" in _exception_message(parsed)


def test_the_print_check_is_off_when_the_client_says_so(crlf_file, monkeypatch):
    monkeypatch.setattr(server, "PythonPrinter", _LosingPrinter)
    options = {"org.openrewrite.requirePrintEqualsInput": "false"}
    assert not isinstance(_parse(crlf_file, options), ParseError)


class _LosingPrinter:
    """Stands in for a printer that drops part of its input."""

    def print(self, _cu):
        return ""


def _exception_message(parse_error):
    return parse_error.markers.markers[0].message
