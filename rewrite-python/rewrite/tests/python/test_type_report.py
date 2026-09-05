# Copyright 2025 the original author or authors.
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

"""Tests for the type-attribution report (``rewrite.python.type_report``)."""

import io
import shutil
from textwrap import dedent
from unittest.mock import patch

import pytest

from rewrite.java import JavaType
from rewrite.python.__main__ import main
from rewrite.python.type_report import (
    parse_for_types,
    print_types,
    render_type,
    build_type_report,
)

requires_ty_types_cli = pytest.mark.skipif(
    shutil.which("ty-types") is None,
    reason="ty-types CLI is not installed (ensure ty-types binary is on PATH)",
)

# A module-qualified call resolves from the file's own imports, an instance
# receiver does not — the two arms the report exists to tell apart.
SAMPLE = dedent(
    """\
    import socket


    # a comment, so the declaration below carries a non-empty prefix
    def probe(arr):
        socket.getfqdn()
        return arr.tostring()
    """
)


def _write(tmp_path, source=SAMPLE):
    path = tmp_path / "sample.py"
    path.write_text(source)
    return str(path)


def test_lists_calls_in_source_order_with_positions_and_matcher_syntax(tmp_path):
    cu = parse_for_types(_write(tmp_path), with_types=False)
    out = io.StringIO()
    report = print_types(cu, out=out)

    resolved, unresolved = (e for e in report.entries if e.kind == "MethodInvocation")

    # Positions are printer-derived, so they survive a comment in the prefix.
    assert (resolved.line, resolved.column) == (6, 5)
    assert resolved.source == "socket.getfqdn()"
    assert resolved.type.startswith("socket getfqdn(..)")
    assert not resolved.missing
    # A note here would mean the rendered pattern fails to match the call it came from.
    assert resolved.note is None

    assert (unresolved.line, unresolved.column) == (7, 12)
    assert unresolved.missing
    assert unresolved.cause is not None
    assert (unresolved.cause.kind, unresolved.cause.source) == ("Identifier", "arr")

    listing = out.getvalue()
    assert "⚠ <unknown> tostring(..)" in listing
    assert "└ select:Identifier" in listing
    assert "0x" not in listing

    assert report.to_dict()["missingCount"] == len(report.missing)


def test_only_missing_keeps_the_receiver_and_all_nodes_widens_the_listing(tmp_path):
    cu = parse_for_types(_write(tmp_path), with_types=False)

    default = build_type_report(cu)
    only_missing = build_type_report(cu, only_missing=True)
    every_node = build_type_report(cu, all_nodes=True)

    assert only_missing.entries == default.missing
    assert all(e.missing for e in only_missing.entries)
    assert any(e.cause is not None for e in only_missing.entries)
    assert len(every_node.entries) > len(default.entries)


def test_renders_a_cyclic_type_by_name_without_repeating_union_bounds():
    owner = JavaType.Class()
    owner._fully_qualified_name = "pkg.Node"
    owner._type_parameters = None
    # Shaped like what ty produces for a recursive generic and an overload set.
    parameterized = JavaType.Parameterized()
    parameterized._type = owner
    parameterized._type_parameters = [parameterized]
    union = JavaType.Union(_bounds=[JavaType.Primitive.String, JavaType.Primitive.String, owner])

    assert render_type(parameterized) == "pkg.Node[...]"
    assert render_type(union) == "str | pkg.Node"


def test_a_node_object_reused_in_two_slots_is_located_at_each_use(tmp_path):
    cu = parse_for_types(_write(tmp_path), with_types=False)
    padded = cu.padding.statements
    duplicated = cu.padding.replace(_statements=list(padded) + [padded[-1]])

    def call_lines(tree):
        return [e.line for e in build_type_report(tree).entries if e.kind == "MethodInvocation"]

    lines = call_lines(duplicated)
    # One object in two slots is two uses, each at its own position.
    assert lines[:2] == call_lines(cu)
    shift = lines[2] - lines[0]
    assert shift > 0 and lines[2:] == [line + shift for line in lines[:2]]


def test_cli_prints_a_listing_and_rejects_flags_its_mode_ignores(tmp_path, capsys):
    assert main([_write(tmp_path)]) == 0
    assert "MethodInvocation" in capsys.readouterr().out

    with pytest.raises(SystemExit):
        main(["--tree", "--json", _write(tmp_path)])


def test_a_failed_ty_initialization_raises_rather_than_reporting_an_untyped_file(tmp_path):
    path = _write(tmp_path)
    with patch("rewrite.python.ty_client.TyTypesClient.initialize", return_value=False):
        with pytest.raises(RuntimeError):
            parse_for_types(path, with_types=True, project_root=str(tmp_path))


@requires_ty_types_cli
def test_diff_ty_separates_the_import_resolved_call_from_the_instance_receiver(tmp_path):
    from rewrite.python.type_report import diff_ty

    path = _write(tmp_path)
    out = io.StringIO()
    differing = {(a.source, a.type, b.type) for a, b in
                 diff_ty(path, project_root=str(tmp_path), out=out)}

    # `socket.getfqdn()` has a declaring type either way; only its return type
    # is new. `arr.tostring()` has no declaring type until ty runs, and an
    # unannotated parameter does not give it one.
    assert ("socket.getfqdn()", "socket getfqdn(..) -> <none>",
            "socket getfqdn(..) -> str") in differing
    assert not any(source == "arr.tostring()" and not after.startswith("<unknown>")
                   for source, _, after in differing)
