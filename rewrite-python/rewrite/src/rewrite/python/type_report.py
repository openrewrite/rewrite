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

"""Print a parsed Python LST annotated with its type attribution.

The listing answers "what type did this expression get, and if none, where was
it lost?". See the README section "Inspecting type attribution" for the output
format and the CLI.
"""

from __future__ import annotations

import ast
import json
import sys
from dataclasses import dataclass, replace
from pathlib import Path
from typing import Any, Dict, List, Optional, Sequence, TextIO, Tuple

from rewrite.java import tree as j
from rewrite.java.support_types import JavaType, Space, TypedTree
from rewrite.python import tree as py
from rewrite.python.printer import PrintOutputCapture, PythonJavaPrinter, PythonPrinter
from rewrite.python.type_utils import _PRIMITIVE_KEYWORDS
from rewrite.visitor import Cursor

__all__ = [
    "TypeEntry",
    "TypeReport",
    "render_type",
    "render_method",
    "build_type_report",
    "print_types",
    "print_tree",
    "diff_ty",
    "parse_for_types",
]

# Rendered in place of a type so the two ways attribution can be absent stay
# distinguishable: the parser leaves a slot None, ty leaves JavaType.Unknown.
NONE = "<none>"
UNKNOWN = "<unknown>"

_MISSING = (NONE, UNKNOWN)

# Nodes whose type slot a recipe gates on. `all_nodes` lists the rest, which on
# a 900-line file is ~1500 rows, most of them identifiers.
_DEFAULT_NODES = (
    j.MethodInvocation,
    j.NewClass,
    j.MemberReference,
    j.MethodDeclaration,
    j.ClassDeclaration,
    j.VariableDeclarations.NamedVariable,
)

_MAX_TYPE_DEPTH = 3
_EXCERPT_WIDTH = 40



def render_type(type_: Optional[Any], _depth: int = 0, _seen: Optional[set] = None) -> str:
    """Render a JavaType by name, so the output is stable across runs.

    The graphs are cyclic, hence the seen-set; ``str(type)`` yields an object
    address, which is what makes it unusable here.
    """
    if type_ is None:
        return NONE
    if isinstance(type_, JavaType.Unknown):
        return UNKNOWN
    if isinstance(type_, JavaType.Primitive):
        return _PRIMITIVE_KEYWORDS.get(type_, type_.name)
    if _depth >= _MAX_TYPE_DEPTH:
        return "..."

    seen = set() if _seen is None else _seen
    if id(type_) in seen:
        return "..."
    seen = seen | {id(type_)}

    def nested(inner: Optional[Any]) -> str:
        return render_type(inner, _depth + 1, seen)

    if isinstance(type_, JavaType.Parameterized):
        params = ", ".join(nested(p) for p in (type_.type_parameters or []))
        return f"{nested(type_.type)}[{params}]" if params else nested(type_.type)
    if isinstance(type_, JavaType.Annotation):
        return f"@{nested(type_.type)}"
    if isinstance(type_, JavaType.Class):
        return type_.fully_qualified_name or UNKNOWN
    if isinstance(type_, (JavaType.Union, JavaType.Intersection)):
        joiner = " | " if isinstance(type_, JavaType.Union) else " & "
        # ty hands back unions whose bounds repeat (ten `str` overloads collapse
        # to ten `str` bounds), which reads as noise.
        bounds = list(dict.fromkeys(nested(b) for b in type_.bounds))
        return joiner.join(bounds) if bounds else UNKNOWN
    if isinstance(type_, JavaType.GenericTypeVariable):
        bounds = " & ".join(nested(b) for b in type_.bounds)
        return f"{type_.name}: {bounds}" if bounds else type_.name
    if isinstance(type_, JavaType.Array):
        return f"{nested(type_.elem_type)}[]"
    if isinstance(type_, JavaType.Method):
        return render_method(type_, _depth, seen)
    if isinstance(type_, JavaType.Variable):
        return f"{nested(type_.owner)}#{type_.name}: {nested(type_.type)}"
    if isinstance(type_, JavaType.FullyQualified):
        return getattr(type_, "fully_qualified_name", None) or UNKNOWN
    return type(type_).__name__


def render_method(method: Optional[JavaType.Method], _depth: int = 0,
                  _seen: Optional[set] = None) -> str:
    """Render a method type as ``<declaring type> <name>(..) -> <return type>``.

    The part before ``->`` is a MethodMatcher pattern. Its argument list is
    ``(..)`` because MethodMatcher matches a pattern against the *call site's*
    arguments, which the declared parameter types do not describe.
    """
    if method is None:
        return NONE
    declaring = render_type(method.declaring_type, _depth + 1, _seen)
    return f"{declaring} {method.name}(..) -> {render_type(method.return_type, _depth + 1, _seen)}"



@dataclass(frozen=True)
class TypeEntry:
    """One reported node: where it is, what it reads as, what type it got."""

    line: int
    column: int
    kind: str
    source: str
    type: str
    missing: bool
    note: Optional[str] = None
    cause: Optional[TypeEntry] = None

    def to_dict(self) -> Dict[str, Any]:
        out: Dict[str, Any] = {
            "line": self.line,
            "column": self.column,
            "kind": self.kind,
            "source": self.source,
            "type": self.type,
            "missing": self.missing,
        }
        if self.note:
            out["note"] = self.note
        if self.cause is not None:
            out["cause"] = self.cause.to_dict()
        return out


@dataclass(frozen=True)
class TypeReport:
    source_path: str
    entries: Tuple[TypeEntry, ...]

    @property
    def missing(self) -> Tuple[TypeEntry, ...]:
        return tuple(e for e in self.entries if e.missing)

    def to_dict(self) -> Dict[str, Any]:
        return {
            "sourcePath": self.source_path,
            "nodeCount": len(self.entries),
            "missingCount": len(self.missing),
            "entries": [e.to_dict() for e in self.entries],
        }



@dataclass
class _Located:
    node: Any
    depth: int
    start: int = 0          # offset of the node's prefix
    content: Optional[int] = None   # offset of its first source character
    end: int = 0


class _OffsetCapture(PrintOutputCapture):
    def __init__(self) -> None:
        super().__init__()
        self.offset = 0

    def append(self, text: Optional[str]) -> PrintOutputCapture:
        if text:
            self.offset += len(text)
        return super().append(text)


class _LocatingJavaPrinter(PythonJavaPrinter):
    @property
    def recorder(self) -> _LocatingPrinter:
        return self._python_printer  # ty: ignore[invalid-return-type]

    def visit(self, tree, p):
        if tree is None or isinstance(tree, py.Py):
            return super().visit(tree, p)
        return self.recorder.record(tree, p, lambda: super(_LocatingJavaPrinter, self).visit(tree, p))

    def _visit_space(self, space: Optional[Space], p) -> Space:
        result = super()._visit_space(space, p)
        self.recorder.prefix_printed(p)
        return result


class _LocatingPrinter(PythonPrinter):
    """Prints the tree, recording each node's source offsets as it goes.

    LST nodes carry no positions, so the only source of truth for one is the
    printer. Each node's own text starts where its prefix ends, which is the
    first ``_visit_space`` inside its visit.
    """

    def __init__(self) -> None:
        super().__init__()
        self._delegate = _LocatingJavaPrinter(self)
        self.located: List[_Located] = []
        self._pending: Optional[_Located] = None
        self._depth = 0

    def record(self, tree, p: _OffsetCapture, visit):
        self.prefix_printed(p, fallback=True)
        entry = _Located(node=tree, depth=self._depth, start=p.offset)
        self.located.append(entry)
        self._pending = entry
        self._depth += 1
        try:
            return visit()
        finally:
            self._depth -= 1
            self.prefix_printed(p, fallback=True)
            entry.end = p.offset

    def prefix_printed(self, p: _OffsetCapture, fallback: bool = False) -> None:
        if self._pending is not None:
            self._pending.content = self._pending.start if fallback else p.offset
            self._pending = None

    def visit(self, tree, p):
        if tree is None or not isinstance(tree, py.Py):
            return super().visit(tree, p)
        return self.record(tree, p, lambda: super(_LocatingPrinter, self).visit(tree, p))

    def _visit_space(self, space: Optional[Space], p) -> Space:
        result = super()._visit_space(space, p)
        self.prefix_printed(p)
        return result


def _locate(source_file) -> Tuple[List[_Located], str]:
    printer = _LocatingPrinter()
    capture = _OffsetCapture()
    printer.set_cursor(Cursor(None, Cursor.ROOT_VALUE))
    printer.visit(source_file, capture)
    return printer.located, capture.out


def _line_index(text: str) -> List[int]:
    starts = [0]
    for i, ch in enumerate(text):
        if ch == "\n":
            starts.append(i + 1)
    return starts


def _line_col(starts: Sequence[int], offset: int) -> Tuple[int, int]:
    import bisect
    line = bisect.bisect_right(starts, offset) - 1
    return line + 1, offset - starts[line] + 1


def _excerpt(text: str, width: int = _EXCERPT_WIDTH) -> str:
    collapsed = " ".join(text.split())
    return collapsed if len(collapsed) <= width else collapsed[: width - 3] + "..."


def _body_start(node) -> Optional[Any]:
    """The block a declaration owns, so its excerpt can stop at the signature."""
    body = getattr(node, "body", None)
    return body if isinstance(node, (j.MethodDeclaration, j.ClassDeclaration)) and \
        isinstance(body, j.Block) else None



def _method_type(node) -> Optional[JavaType.Method]:
    if isinstance(node, j.NewClass):
        return node.constructor_type
    return getattr(node, "method_type", None)


def _describe(node) -> Tuple[str, bool]:
    """Render the node's load-bearing type slot, and whether it is missing."""
    if isinstance(node, (j.MethodInvocation, j.NewClass, j.MemberReference, j.MethodDeclaration)):
        method = _method_type(node)
        rendered = render_method(method)
        # The declaring type is what MethodMatcher gates on, so it alone decides
        # whether the row is missing; an unresolved return type shows in the text.
        declaring = NONE if method is None else render_type(method.declaring_type)
        return rendered, declaring in _MISSING
    if isinstance(node, j.VariableDeclarations.NamedVariable):
        variable = node.variable_type
        rendered = NONE if variable is None else render_type(variable.type)
        return rendered, rendered in _MISSING
    if isinstance(node, j.Identifier) and node.field_type is not None:
        rendered = render_type(node.field_type.type)
        return rendered, rendered in _MISSING
    rendered = render_type(node.type) if isinstance(node, TypedTree) else NONE
    return rendered, rendered in _MISSING


def _matcher_note(node) -> Optional[str]:
    """Report a resolved call whose own rendered pattern does not match it."""
    if not isinstance(node, j.MethodInvocation):
        return None
    method = node.method_type
    if method is None or render_type(method.declaring_type) in _MISSING:
        return None
    pattern = f"{render_type(method.declaring_type)} {method.name}(..)"
    from rewrite.python.method_matcher import MethodMatcher
    try:
        matcher = MethodMatcher.create(pattern)
    except Exception as exc:
        return f"pattern unparseable: {exc}"
    try:
        return None if matcher.matches(node) else "pattern does not match this call"
    except Exception as exc:
        return f"matching this call raised: {exc}"


def _descendant(located: List[_Located], parent: int, child) -> Optional[int]:
    """Index of ``child`` within the node at ``parent``.

    A recipe can leave the same node object in two slots, so a child is
    identified by where this traversal reached it rather than by its identity.
    """
    if child is None:
        return None
    for i in range(parent + 1, len(located)):
        if located[i].start >= located[parent].end:
            break
        if located[i].node is child:
            return i
    return None


def build_type_report(source_file, *, only_missing: bool = False,
                all_nodes: bool = False) -> TypeReport:
    """Build a source-ordered report of the LST's type attribution."""
    located, printed = _locate(source_file)
    starts = _line_index(printed)

    built: List[TypeEntry] = []
    for i, item in enumerate(located):
        content = item.start if item.content is None else item.content
        line, column = _line_col(starts, content)
        rendered, missing = _describe(item.node)
        body = _descendant(located, i, _body_start(item.node))
        stop = item.end if body is None else located[body].start
        built.append(TypeEntry(
            line=line,
            column=column,
            kind=type(item.node).__name__,
            source=_excerpt(printed[content:stop]),
            type=rendered,
            missing=missing,
            note=_matcher_note(item.node),
        ))

    entries: List[TypeEntry] = []
    for i, item in enumerate(located):
        if not all_nodes and not isinstance(item.node, _DEFAULT_NODES):
            continue
        entry = built[i]
        if entry.missing:
            cause = _descendant(located, i, getattr(item.node, "select", None))
            if cause is not None:
                entry = replace(entry, cause=built[cause])
        if only_missing and not entry.missing:
            continue
        entries.append(entry)

    return TypeReport(
        source_path=str(getattr(source_file, "source_path", "<unknown>")),
        entries=tuple(entries),
    )



def _glyphs(out: TextIO) -> Tuple[str, str]:
    """``(warning, branch)``, downgraded when the stream cannot encode them."""
    encoding = getattr(out, "encoding", None) or "utf-8"
    try:
        "⚠└…".encode(encoding)
    except (LookupError, UnicodeEncodeError):
        return "!", "\\_"
    return "⚠", "└"


def print_types(source_file, *, out: Optional[TextIO] = None, only_missing: bool = False,
                all_nodes: bool = False, as_json: bool = False) -> TypeReport:
    """Print a flat, source-ordered listing of the LST's type attribution."""
    out = sys.stdout if out is None else out
    report = build_type_report(source_file, only_missing=only_missing, all_nodes=all_nodes)
    if as_json:
        json.dump(report.to_dict(), out, indent=2)
        out.write("\n")
        return report

    warning, branch = _glyphs(out)
    rows: List[Tuple[str, str, str, str]] = []
    for entry in report.entries:
        rows.append(_row(entry, warning))
        if entry.cause is not None:
            cause = entry.cause
            rows.append((
                f"{cause.line}:{cause.column}",
                f"  {branch} select:{cause.kind}",
                cause.source,
                cause.type,
            ))

    header = ("line:col", "kind", "source", "type")
    widths = [max(len(r[i]) for r in [header, *rows]) for i in range(3)]
    for row in [header, *rows]:
        line = "  ".join(cell.ljust(width) for cell, width in zip(row, widths))
        out.write(f"{line}  {row[3]}".rstrip() + "\n")
    return report


def _row(entry: TypeEntry, warning: str) -> Tuple[str, str, str, str]:
    type_text = f"{warning} {entry.type}" if entry.missing else entry.type
    if entry.note:
        type_text = f"{type_text}  [{warning} {entry.note}]"
    return f"{entry.line}:{entry.column}", entry.kind, entry.source, type_text


def print_tree(source_file, *, out: Optional[TextIO] = None) -> None:
    """Print the full nested structure with prefixes, for structural questions."""
    out = sys.stdout if out is None else out
    located, printed = _locate(source_file)
    starts = _line_index(printed)
    for item in located:
        content = item.start if item.content is None else item.content
        line, column = _line_col(starts, content)
        prefix = printed[item.start:content]
        rendered, _ = _describe(item.node)
        out.write(
            f"{'  ' * item.depth}{type(item.node).__name__}"
            f"  prefix={prefix!r}  {_excerpt(printed[content:item.end], 30)!r}"
            f"  [{line}:{column}]  {rendered}\n"
        )


def diff_ty(path: str, *, project_root: Optional[str] = None,
            out: Optional[TextIO] = None, all_nodes: bool = False) -> List[Tuple[TypeEntry, TypeEntry]]:
    """Parse ``path`` with and without a ty client and report the nodes that differ.

    Answers "does my recipe need type attribution to work?" — a recipe gated only
    on rows that are identical in both columns runs without a type check.
    """
    out = sys.stdout if out is None else out
    without = build_type_report(parse_for_types(path, with_types=False, project_root=project_root),
                          all_nodes=all_nodes)
    with_ty = build_type_report(parse_for_types(path, with_types=True, project_root=project_root),
                          all_nodes=all_nodes)

    if len(without.entries) != len(with_ty.entries):
        raise RuntimeError(
            f"the two parses produced different trees "
            f"({len(without.entries)} vs {len(with_ty.entries)} nodes)"
        )

    warning, _ = _glyphs(out)
    differing = [(a, b) for a, b in zip(without.entries, with_ty.entries) if a.type != b.type]

    def mark(entry: TypeEntry) -> str:
        return f"{warning} {entry.type}" if entry.missing else entry.type

    rows = [(f"{a.line}:{a.column}", a.kind, a.source, mark(a), mark(b)) for a, b in differing]
    header = ("line:col", "kind", "source", "without ty", "with ty")
    widths = [max(len(r[i]) for r in [header, *rows]) for i in range(4)]
    for row in [header, *rows]:
        line = "  ".join(cell.ljust(width) for cell, width in zip(row, widths))
        out.write(f"{line}  {row[4]}".rstrip() + "\n")
    out.write(f"\n{len(differing)} of {len(without.entries)} nodes differ\n")
    return differing



def parse_for_types(path: str, *, with_types: bool = True,
                    project_root: Optional[str] = None) -> py.CompilationUnit:
    """Parse a file into a CompilationUnit for inspection.

    This is a diagnostic entry point: running recipes over a real codebase is
    still orchestrated by the JVM runtime over the RPC bridge.
    """
    resolved = Path(path).resolve()
    source = resolved.read_text(encoding="utf-8")
    root = str(Path(project_root).resolve()) if project_root else str(resolved.parent)

    from rewrite.python._parser_visitor import ParserVisitor
    from rewrite.python._version_detect import detect_from_project, ty_python_version

    client = None
    try:
        if with_types:
            from rewrite.python.ty_client import TyTypesClient
            client = TyTypesClient(python_version=ty_python_version(detect_from_project(root)))
            if not client.initialize(root):
                # Carrying on would report a fully unattributed file, which reads
                # as "this code needs no type attribution".
                raise RuntimeError(f"ty could not initialize against {root}")
        # ParserVisitor strips the BOM itself and records it for printing;
        # ast.parse does not accept one.
        stripped = source[1:] if source.startswith("\ufeff") else source
        visitor = ParserVisitor(source, str(resolved), client)
        cu = visitor.visit_Module(ast.parse(stripped, str(resolved)))
    finally:
        if client is not None:
            client.shutdown()

    try:
        return cu.replace(source_path=resolved.relative_to(root))
    except ValueError:
        return cu
