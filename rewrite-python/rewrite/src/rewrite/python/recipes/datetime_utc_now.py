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

"""Recipe to migrate deprecated datetime.utcnow() and datetime.utcfromtimestamp() calls."""

from typing import Any, List, Optional
from uuid import uuid4

from rewrite import ExecutionContext, Recipe, TreeVisitor
from rewrite.category import CategoryDescriptor
from rewrite.decorators import categorize
from rewrite.java import J
from rewrite.java.support_types import JContainer, JLeftPadded, JRightPadded, Space
from rewrite.java.tree import Empty, FieldAccess, Identifier, Import, MethodInvocation
from rewrite.markers import Markers
from rewrite.marketplace import Python
from rewrite.python.template import template
from rewrite.python.tree import CompilationUnit, MultiImport
from rewrite.python.visitor import PythonVisitor

_Migration = [*Python, CategoryDescriptor(display_name="Migration")]


def _get_import_style(cu: CompilationUnit) -> Optional[str]:
    """Determine how datetime is imported.

    Returns:
        "from" if `from datetime import datetime` is used,
        "direct" if `import datetime` is used,
        None if neither is found.
    """
    for stmt in cu.statements:
        if not isinstance(stmt, MultiImport):
            continue
        from_ = stmt.from_
        if from_ is not None:
            # from X import Y — check if X is "datetime"
            if isinstance(from_, Identifier) and from_.simple_name == "datetime":
                for imp in stmt.names:
                    if imp.qualid.name.simple_name == "datetime":
                        return "from"
            elif isinstance(from_, FieldAccess) and from_.name.simple_name == "datetime":
                for imp in stmt.names:
                    if imp.qualid.name.simple_name == "datetime":
                        return "from"
        else:
            # import X — check if any name is "datetime"
            for imp in stmt.names:
                if imp.qualid.name.simple_name == "datetime":
                    return "direct"
    return None


def _has_timezone_import(cu: CompilationUnit) -> bool:
    """Check if `timezone` is already imported from `datetime`."""
    for stmt in cu.statements:
        if not isinstance(stmt, MultiImport):
            continue
        from_ = stmt.from_
        if from_ is None:
            continue
        if isinstance(from_, Identifier) and from_.simple_name == "datetime":
            for imp in stmt.names:
                if imp.qualid.name.simple_name == "timezone":
                    return True
    return False


def _is_datetime_receiver(select: Optional[J], import_style: str) -> bool:
    """Check if the select expression matches the expected datetime receiver."""
    if select is None:
        return False
    if import_style == "from":
        return isinstance(select, Identifier) and select.simple_name == "datetime"
    elif import_style == "direct":
        if not isinstance(select, FieldAccess):
            return False
        return (
            isinstance(select.target, Identifier)
            and select.target.simple_name == "datetime"
            and select.name.simple_name == "datetime"
        )
    return False


def _make_identifier(name: str, prefix: Space = Space.EMPTY) -> Identifier:
    return Identifier(uuid4(), prefix, Markers.EMPTY, [], name, None, None)


_TEMPLATES = {
    ("utcnow", "from"): template("datetime.now(timezone.utc)"),
    ("utcnow", "direct"): template("datetime.datetime.now(datetime.timezone.utc)"),
    ("utcfromtimestamp", "from"): template("datetime.fromtimestamp(ts, tz=timezone.utc)"),
    ("utcfromtimestamp", "direct"): template("datetime.datetime.fromtimestamp(ts, tz=datetime.timezone.utc)"),
}


def _add_timezone_to_from_import(cu: CompilationUnit) -> CompilationUnit:
    """Add `timezone` to an existing `from datetime import ...` statement."""
    padded_stmts: List[JRightPadded] = cu.padding.statements
    for i, padded_stmt in enumerate(padded_stmts):
        stmt = padded_stmt.element
        if not isinstance(stmt, MultiImport):
            continue
        from_ = stmt.from_
        if from_ is None:
            continue
        if not (isinstance(from_, Identifier) and from_.simple_name == "datetime"):
            continue
        # Found the `from datetime import ...` — check if it has `datetime` in names
        has_datetime = any(
            imp.qualid.name.simple_name == "datetime" for imp in stmt.names
        )
        if not has_datetime:
            continue

        # Create a new Import element for `timezone`
        tz_qualid = FieldAccess(
            uuid4(), Space.EMPTY, Markers.EMPTY,
            Empty(uuid4(), Space.EMPTY, Markers.EMPTY),
            JLeftPadded(Space.EMPTY, _make_identifier("timezone"), Markers.EMPTY),
            None,
        )
        tz_import = Import(
            uuid4(), Space([], ' '), Markers.EMPTY,
            JLeftPadded(Space.EMPTY, False, Markers.EMPTY),
            tz_qualid, None,
        )

        # Append to existing names, moving trailing space from last to new element
        existing_padded_names = stmt.padding.names.padding.elements
        new_padded_names = list(existing_padded_names)
        if new_padded_names:
            last = new_padded_names[-1]
            new_padded_names[-1] = last.replace(after=Space.EMPTY)
        new_padded_names.append(JRightPadded(tz_import, last.after if existing_padded_names else Space.EMPTY, Markers.EMPTY))

        new_names_container = JContainer(
            stmt.padding.names.before,
            new_padded_names,
            stmt.padding.names.markers,
        )
        new_multi = MultiImport(
            stmt.id, stmt.prefix, stmt.markers,
            stmt.padding.from_,
            stmt.parenthesized,
            new_names_container,
        )

        new_padded_stmts = list(padded_stmts)
        new_padded_stmts[i] = padded_stmt.replace(element=new_multi)
        return cu.padding.replace(_statements=new_padded_stmts)

    return cu


@categorize(_Migration)
class DatetimeUtcNow(Recipe):
    """
    Migrate deprecated `datetime.utcnow()` and `datetime.utcfromtimestamp()`.

    Python 3.12 deprecated `datetime.utcnow()` and `datetime.utcfromtimestamp()`.
    This recipe rewrites them to `datetime.now(timezone.utc)` and
    `datetime.fromtimestamp(ts, tz=timezone.utc)` respectively.
    """

    @property
    def name(self) -> str:
        return "org.openrewrite.python.migrate.DatetimeUtcNow"

    @property
    def display_name(self) -> str:
        return "Replace deprecated `datetime.utcnow()` and `datetime.utcfromtimestamp()`"

    @property
    def description(self) -> str:
        return (
            "Replace `datetime.utcnow()` with `datetime.now(timezone.utc)` and "
            "`datetime.utcfromtimestamp(ts)` with `datetime.fromtimestamp(ts, tz=timezone.utc)` "
            "as recommended since Python 3.12."
        )

    def editor(self) -> TreeVisitor[Any, ExecutionContext]:
        class Visitor(PythonVisitor[ExecutionContext]):
            def __init__(self):
                super().__init__()
                self._needs_timezone_import = False

            def visit_compilation_unit(self, cu: CompilationUnit, p: ExecutionContext) -> J:
                self._needs_timezone_import = False
                cu = super().visit_compilation_unit(cu, p)
                if not isinstance(cu, CompilationUnit):
                    return cu
                if self._needs_timezone_import and not _has_timezone_import(cu):
                    cu = _add_timezone_to_from_import(cu)
                return cu

            def visit_method_invocation(self, method: MethodInvocation, p: ExecutionContext) -> J:
                method = super().visit_method_invocation(method, p)
                if not isinstance(method, MethodInvocation):
                    return method

                method_name = method.name.simple_name
                if method_name not in ("utcnow", "utcfromtimestamp"):
                    return method

                cu = self.cursor.first_enclosing(CompilationUnit)
                if cu is None:
                    return method

                import_style = _get_import_style(cu)
                if import_style is None:
                    return method

                if not _is_datetime_receiver(method.select, import_style):
                    return method

                if import_style == "from":
                    self._needs_timezone_import = True

                tmpl_tree = _TEMPLATES[(method_name, import_style)].get_tree()
                if not isinstance(tmpl_tree, MethodInvocation):
                    return method

                new_name = tmpl_tree.name.replace(prefix=method.name.prefix)
                tmpl_args = tmpl_tree.padding.arguments

                if method_name == "utcnow":
                    new_args = JContainer(
                        method.padding.arguments.before,
                        tmpl_args.padding.elements,
                        tmpl_args.markers,
                    )
                else:
                    # Splice original argument(s) into template args,
                    # replacing the placeholder first arg (ts) with the original
                    tmpl_padded_args = list(tmpl_args.padding.elements)
                    orig_padded_args = method.padding.arguments.padding.elements
                    if orig_padded_args:
                        tmpl_padded_args[0] = tmpl_padded_args[0].replace(
                            element=orig_padded_args[0].element
                        )
                    new_args = JContainer(
                        method.padding.arguments.before,
                        tmpl_padded_args,
                        tmpl_args.markers,
                    )

                return method.padding.replace(
                    _name=new_name,
                    _arguments=new_args,
                    _method_type=None,
                )

        return Visitor()
