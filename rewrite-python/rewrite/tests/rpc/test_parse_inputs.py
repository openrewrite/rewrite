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


@pytest.mark.parametrize("coding, charset, text", [
    ("latin-1", "iso-8859-1", "été"),
    ("koi8-r", "koi8-r", "жук"),
], ids=["latin-1", "koi8-r"])
def test_a_file_is_read_in_the_encoding_it_declares(tmp_path, coding, charset, text):
    source = f"# -*- coding: {coding} -*-\nname = '{text}'.upper()  # {text}\n\nvalue = 1\n"
    path = tmp_path / "declared.py"
    path.write_bytes(source.encode(coding))

    parsed = _parse(path)

    assert PythonPrinter().print(parsed) == source
    # the charset the host writes the file back in
    assert parsed.charset_name == charset


def test_an_encoding_the_bytes_do_not_depend_on_is_not_reported(tmp_path):
    # the host may not know the name, and writes the same bytes without it
    source = "# -*- coding: cp949 -*-\nvalue = 1\r\n"
    path = tmp_path / "declared.py"
    path.write_bytes(source.encode("cp949"))

    parsed = _parse(path)

    assert PythonPrinter().print(parsed) == source
    assert parsed.charset_name is None


@pytest.mark.parametrize("raw", [
    b"# -*- coding: uft-8 -*-\nvalue = 1\n",
    b"\xef\xbb\xbf# -*- coding: latin-1 -*-\nvalue = 1\n",
], ids=["unknown encoding", "byte order mark under another encoding"])
def test_a_coding_line_the_interpreter_rejects_leaves_the_file_read_as_utf_8(tmp_path, raw):
    path = tmp_path / "declared.py"
    path.write_bytes(raw)

    assert PythonPrinter().print(_parse(path)) == raw.decode("utf-8")


def test_a_coding_line_in_source_text_has_no_say_in_how_the_text_is_read():
    source = "# -*- coding: latin-1 -*-\nname = 'é'  # é\nvalue = 1\n"
    ids = handle_parse({"inputs": [{"text": source, "sourcePath": "declared.py"}]})

    assert PythonPrinter().print(local_objects[ids[0]]) == source


def test_every_input_gets_a_slot_whatever_is_wrong_with_it(crlf_file):
    # Latin-1 bytes in a file that declares no encoding, which is then UTF-8.
    undecodable = crlf_file.parent / "latin1.py"
    undecodable.write_bytes(b"x = 1\ny = 2\nz = '\xe9'\n")

    ids = handle_parse({"inputs": [{"sourcePath": str(crlf_file)},
                                   {"sourcePath": str(crlf_file.parent / "gone.py")},
                                   {"sourcePath": str(undecodable)},
                                   {"sourcePath": None}],
                        "relativeTo": str(crlf_file.parent)})

    assert len(ids) == 4
    assert PythonPrinter().print(local_objects[ids[0]]) == CRLF_SOURCE

    # An error result carries the same project-relative path a parsed one would.
    assert isinstance(local_objects[ids[1]], ParseError)
    assert str(local_objects[ids[1]].source_path) == "gone.py"

    assert isinstance(local_objects[ids[2]], ParseError)
    assert str(local_objects[ids[2]].source_path) == "latin1.py"

    assert isinstance(local_objects[ids[3]], ParseError)
    assert str(local_objects[ids[3]].source_path) == "<unknown>"


def test_parse_that_loses_source_becomes_a_parse_error(crlf_file, monkeypatch):
    monkeypatch.setattr(server, "PythonPrinter", _LosingPrinter)
    parsed = _parse(crlf_file)
    assert isinstance(parsed, ParseError)
    assert "is not print idempotent" in _exception_message(parsed)


def test_the_print_check_is_off_when_the_client_says_so(crlf_file, monkeypatch):
    monkeypatch.setattr(server, "PythonPrinter", _LosingPrinter)
    options = {"org.openrewrite.requirePrintEqualsInput": "false"}
    assert not isinstance(_parse(crlf_file, options), ParseError)


def test_a_project_parse_reports_a_file_it_cannot_read(crlf_file):
    # Latin-1 bytes the UTF-8 read rejects.
    (crlf_file.parent / "bad.py").write_bytes(b"x = '\xe9'\n")

    parsed = {item["sourcePath"]: item["sourceFileType"].rsplit(".", 1)[-1]
              for item in handle_parse_project({"projectPath": str(crlf_file.parent)})}

    assert parsed == {"crlf.py": "Py$CompilationUnit", "bad.py": "ParseError"}


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
