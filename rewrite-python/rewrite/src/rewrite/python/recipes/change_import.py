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

"""Recipe to change Python imports from one module/name to another."""

from dataclasses import dataclass, field, replace as dc_replace
from typing import Any, List, Optional, Tuple

from rewrite import ExecutionContext, Recipe, TreeVisitor
from rewrite.category import CategoryDescriptor
from rewrite.decorators import categorize
from rewrite.marketplace import Python
from rewrite.recipe import option
from rewrite.java import J
from rewrite.java.support_types import JavaType, JContainer, JRightPadded, Statement
from rewrite.java.tree import FieldAccess, Identifier, If, Import, MethodInvocation, Space
from rewrite.markers import Markers
from rewrite.python.import_utils import (get_qualid_name, get_name_string, get_alias_name,
                                         module_scope_blocks, unconditional_body)
from rewrite.python.scope_utils import LocalBindings
from rewrite.python.tree import CompilationUnit, MultiImport
from rewrite.python.visitor import PythonVisitor
from rewrite.python.add_import import (AddImportOptions, create_import_element,
                                       create_import_statement, insert_member, maybe_add_import)
from rewrite.python.remove_import import RemoveImportOptions, maybe_remove_import, prefix_to_inherit


_Imports = [*Python, CategoryDescriptor(display_name="Imports")]

# An import to bind, as (module, name, alias); a None name means `import module`.
_Binding = Tuple[str, Optional[str], Optional[str]]


def _create_module_type(fqn: str) -> JavaType.Class:
    """Create a JavaType.Class for a module from its fully qualified name.

    JavaType.Class is not a dataclass, so fields are set directly after
    construction.  This matches the pattern used elsewhere in the codebase.
    """
    class_type = JavaType.Class()
    class_type._flags_bit_map = 0
    class_type._fully_qualified_name = fqn
    class_type._kind = JavaType.FullyQualified.Kind.Class
    return class_type


@categorize(_Imports)
@dataclass
class ChangeImport(Recipe):
    """
    Change a Python import from one module/name to another.

    This recipe is useful for:
    - Library migrations (e.g., moving from `urllib2` to `urllib.request`)
    - Module restructuring
    - Renaming imported members

    Examples:
        # Change: from collections import Mapping -> from collections.abc import Mapping
        ChangeImport(
            old_module="collections",
            old_name="Mapping",
            new_module="collections.abc",
            new_name="Mapping"
        )

        # Change: import urllib2 -> import urllib.request as urllib2
        ChangeImport(
            old_module="urllib2",
            new_module="urllib.request",
            new_alias="urllib2"
        )

        # Change: from os.path import join -> from pathlib import Path
        ChangeImport(
            old_module="os.path",
            old_name="join",
            new_module="pathlib",
            new_name="Path"
        )
    """

    old_module: str = field(default="", metadata=option(
        display_name="Old module",
        description="The module to change imports from",
        example="collections"
    ))

    old_name: Optional[str] = field(default=None, metadata=option(
        display_name="Old name",
        description="The name to change (for 'from X import name' style). Leave empty for direct imports.",
        example="Mapping",
        required=False
    ))

    new_module: str = field(default="", metadata=option(
        display_name="New module",
        description="The module to change imports to",
        example="collections.abc"
    ))

    new_name: Optional[str] = field(default=None, metadata=option(
        display_name="New name",
        description="The new name. If not specified, uses the old name.",
        example="Mapping",
        required=False
    ))

    new_alias: Optional[str] = field(default=None, metadata=option(
        display_name="New alias",
        description="Optional alias for the new import",
        required=False
    ))

    @property
    def name(self) -> str:
        return "org.openrewrite.python.ChangeImport"

    @property
    def display_name(self) -> str:
        return "Change import"

    @property
    def description(self) -> str:
        return "Change a Python import from one module/name to another, updating all type attributions."

    def editor(self) -> TreeVisitor[Any, ExecutionContext]:
        old_module = self.old_module
        old_name = self.old_name
        new_module = self.new_module
        new_name = self.new_name if self.new_name else old_name
        new_alias = self.new_alias

        class ChangeImportVisitor(PythonVisitor[ExecutionContext]):
            has_old_import: bool = False
            old_alias: Optional[str] = None
            has_direct_module_import: bool = False
            module_alias: Optional[str] = None
            rewrote_qualified_refs: bool = False
            new_module_type: Optional[JavaType.Class] = None
            local_bindings = LocalBindings()
            old_import_at_module_level: bool = False
            direct_module_import_at_module_level: bool = False

            def visit_compilation_unit(self, cu: CompilationUnit, p: ExecutionContext) -> J:
                self.has_old_import = False
                self.old_alias = None
                self.has_direct_module_import = False
                self.module_alias = None
                self.rewrote_qualified_refs = False
                self.new_module_type = None

                for stmt in cu.statements:
                    self._detect(stmt)
                # Where the old import is found decides where the replacement goes.
                self.old_import_at_module_level = self.has_old_import
                self.direct_module_import_at_module_level = self.has_direct_module_import
                for block in module_scope_blocks(cu.statements):
                    for stmt in block.statements:
                        self._detect(stmt)

                if not self.has_old_import and not self.has_direct_module_import:
                    return cu
                if old_name and (new_alias or self.old_alias or new_name) != \
                        (self.old_alias or old_name) and self._match_outside_module_scope(cu):
                    return cu

                # Visit to transform imports
                result = super().visit_compilation_unit(cu, p)
                if not isinstance(result, CompilationUnit):
                    return result

                result = self._transfer_removed_prefixes(cu, result)
                result = self._rewrite_block_imports(result)

                if self.old_import_at_module_level:
                    alias_to_use = new_alias or self.old_alias
                    if new_name:
                        maybe_add_import(self, AddImportOptions(
                            module=new_module,
                            name=new_name,
                            alias=alias_to_use,
                            only_if_referenced=False
                        ))
                    else:
                        maybe_add_import(self, AddImportOptions(
                            module=new_module,
                            alias=alias_to_use,
                            only_if_referenced=False
                        ))

                # If we rewrote qualified references, manage the direct import
                if self.rewrote_qualified_refs:
                    if self.direct_module_import_at_module_level:
                        maybe_add_import(self, AddImportOptions(
                            module=new_module,
                            alias=new_alias,
                            only_if_referenced=False
                        ))
                    maybe_remove_import(self, RemoveImportOptions(
                        module=old_module,
                    ))

                return result

            def _detect(self, stmt: Statement) -> None:
                """Record what `stmt` binds: the import being changed, and the module whose
                qualified references would be rewritten."""
                if isinstance(stmt, MultiImport):
                    if not self.has_old_import:
                        alias = self._check_for_old_import(stmt)
                        if alias is not None:
                            self.has_old_import = True
                            self.old_alias = alias if alias != "" else None
                    if old_name and not self.has_direct_module_import and stmt.from_ is None:
                        for imp in stmt.names:
                            if get_qualid_name(imp.qualid) == old_module:
                                self.has_direct_module_import = True
                                self.module_alias = get_alias_name(imp)
                                break
                elif isinstance(stmt, Import):
                    if not self.has_old_import:
                        alias = self._check_for_old_single_import(stmt)
                        if alias is not None:
                            self.has_old_import = True
                            self.old_alias = alias if alias != "" else None
                    if old_name and not self.has_direct_module_import:
                        if get_qualid_name(stmt.qualid) == old_module:
                            self.has_direct_module_import = True
                            self.module_alias = get_alias_name(stmt)

            def _match_outside_module_scope(self, cu: CompilationUnit) -> bool:
                """True when a match sits somewhere this recipe leaves alone. That import
                goes on binding the old name, so renaming the references it serves would
                leave them unresolved."""
                in_scope = {stmt.id for stmt in cu.statements}
                for block in module_scope_blocks(cu.statements):
                    in_scope.update(stmt.id for stmt in block.statements)
                found: List[bool] = []
                outer = self

                class Finder(PythonVisitor):
                    def visit_multi_import(self, multi: MultiImport, p) -> J:
                        if (multi.id not in in_scope and
                                outer._check_for_old_import(multi) is not None):
                            found.append(True)
                        return multi

                Finder().visit(cu, None)
                return bool(found)

            def _at_module_level(self) -> bool:
                """True for a statement of the compilation unit. Replacements are bound here
                or, by `_rewrite_block`, in a module-scope `if` body; a match deeper than that
                would be removed with nothing put in its place."""
                return isinstance(self.cursor.parent_tree_cursor().value, CompilationUnit)

            def visit_import(self, import_: Import, p: ExecutionContext) -> Optional[J]:  # ty: ignore[invalid-method-override]
                if not self.has_old_import or old_name:
                    return import_
                if not self._at_module_level():
                    return import_
                alias = self._check_for_old_single_import(import_)
                if alias is None:
                    return import_
                return None

            def visit_multi_import(self, multi: MultiImport, p: ExecutionContext) -> Optional[J]:  # ty: ignore[invalid-method-override]
                if not self.has_old_import:
                    return multi
                if not self._at_module_level():
                    return multi

                alias = self._check_for_old_import(multi)
                if alias is None:
                    return multi

                # Remove this import (or the specific name from it)
                if old_name:
                    # from X import name - remove specific name
                    return self._remove_name_from_import(multi, old_name)
                else:
                    # import X - remove entire import
                    return self._remove_module_from_import(multi, old_module)

            def visit_identifier(self, ident: Identifier, p: ExecutionContext) -> J:
                ident = super().visit_identifier(ident, p)  # ty: ignore[invalid-assignment]  # visitor covariance
                if not isinstance(ident, Identifier):
                    return ident
                if not old_name or not new_name or not self.has_old_import:
                    return ident
                old_ref_name = self.old_alias or old_name
                new_ref_name = new_alias or self.old_alias or new_name
                if old_ref_name == new_ref_name:
                    return ident
                if ident.simple_name != old_ref_name:
                    return ident
                # Skip identifiers inside import statements
                if self.cursor.first_enclosing(Import):
                    return ident
                # An attribute name resolves against its target object;
                # visit_field_access handles the qualified references.
                parent = self.cursor.parent_tree_cursor().value
                if isinstance(parent, FieldAccess) and parent.name.id == ident.id:
                    return ident
                if self.local_bindings.is_bound(self.cursor, old_ref_name):
                    return ident
                return ident.replace(_simple_name=new_ref_name)

            def visit_method_invocation(self, method: MethodInvocation, p: ExecutionContext) -> J:
                method = super().visit_method_invocation(method, p)  # ty: ignore[invalid-assignment]  # visitor covariance
                if not isinstance(method, MethodInvocation):
                    return method
                if not old_name or not self.has_direct_module_import:
                    return method
                # Only matches simple module.func() calls where the select is an
                # Identifier. Nested attribute chains like pkg.module.func()
                # (where select is a FieldAccess) are not currently handled.
                if not isinstance(method.select, Identifier):
                    return method
                if not isinstance(method.name, Identifier):
                    return method

                select_name = method.select.simple_name
                # For dotted modules without aliases (e.g. `import os.path`),
                # `old_module` is a dotted string like "os.path" which will
                # never match a simple Identifier name — but those cases are
                # already excluded by the `isinstance(method.select, Identifier)`
                # guard above (the select would be a FieldAccess instead).
                expected_name = self.module_alias or old_module
                if select_name != expected_name:
                    return method
                if method.name.simple_name != old_name:
                    return method

                self.rewrote_qualified_refs = True
                new_select_name = new_alias or new_module
                new_select = method.select.replace(_simple_name=new_select_name)
                # Update type attribution on the select identifier
                if method.select.type is not None:
                    new_select = new_select.replace(_type=self._get_new_module_type())
                padded_select = method.padding.select
                if padded_select is None:
                    return method
                new_padded_select = padded_select.replace(_element=new_select)
                result = method.padding.replace(_select=new_padded_select)
                if new_name and new_name != old_name:
                    result = result.replace(_name=result.name.replace(_simple_name=new_name))
                # A construction is owned by the class it builds and keeps the
                # model's `<constructor>` name; a function is owned by its module.
                if result.method_type is not None:
                    if result.method_type.is_constructor:
                        new_type = dc_replace(result.method_type, _declaring_type=
                            _create_module_type(f"{new_module}.{new_name or old_name}"))
                    else:
                        new_type = dc_replace(
                            result.method_type,
                            _declaring_type=self._get_new_module_type(),
                            _name=new_name or old_name,
                        )
                    result = result.replace(_method_type=new_type)
                return result

            def visit_field_access(self, field_access: FieldAccess, p: ExecutionContext) -> J:
                field_access = super().visit_field_access(field_access, p)  # ty: ignore[invalid-assignment]  # visitor covariance
                if not old_name or not self.has_direct_module_import:
                    return field_access
                if not isinstance(field_access, FieldAccess):
                    return field_access
                if not isinstance(field_access.target, Identifier):
                    return field_access

                existing_name = field_access.target.simple_name
                expected_name = self.module_alias or old_module
                if existing_name != expected_name:
                    return field_access
                if field_access.name.simple_name != old_name:
                    return field_access

                self.rewrote_qualified_refs = True
                new_target_name = new_alias or new_module
                new_target = field_access.target.replace(_simple_name=new_target_name)
                # Update type attribution on the target identifier
                if field_access.target.type is not None:
                    new_target = new_target.replace(_type=self._get_new_module_type())
                result = field_access.replace(_target=new_target)
                if new_name and new_name != old_name:
                    new_name_ident = result.name.replace(_simple_name=new_name)
                    result = result.padding.replace(_name=result.padding.name.replace(_element=new_name_ident))
                return result

            def _rewrite_block_imports(self, cu: CompilationUnit) -> CompilationUnit:
                """Rewrite a match inside an `if TYPE_CHECKING:`-style block where it stands.

                The replacement import is bound in the same block. Hoisting it to module level
                — what maybe_add_import would do — would run at import time an import the file
                deliberately deferred.
                """
                kept = self._rewrite_statements(cu.padding.statements)
                return cu if kept is None else cu.padding.replace(_statements=kept)

            def _rewrite_statements(self, padded_statements) -> Optional[List[JRightPadded]]:
                """`padded_statements` with every module-scope `if` body rewritten, or None
                when none of them held a match."""
                kept: List[JRightPadded] = []
                changed = False
                for padded in padded_statements:
                    stmt = padded.element
                    if isinstance(stmt, If):
                        rewritten = self._rewrite_if(stmt)
                        if rewritten is not stmt:
                            padded = padded.replace(_element=rewritten)
                            changed = True
                    kept.append(padded)
                return kept if changed else None

            def _rewrite_if(self, if_: If) -> If:
                body = unconditional_body(if_)
                if body is None:
                    return if_
                kept = self._rewrite_block(body.padding.statements)
                if kept is None:
                    return if_
                padded = if_.padding.then_part
                return if_.padding.replace(_then_part=JRightPadded(
                    body.padding.replace(_statements=kept), padded.after, padded.markers))

            def _rewrite_block(self, padded_statements) -> Optional[List[JRightPadded]]:
                """The block's statements with every match replaced by the new import, or None
                when nothing in it matched. Nested `if` bodies are rewritten too."""
                kept: List[JRightPadded] = []
                changed = False
                to_add: List[Tuple[_Binding, int, Space]] = []
                for padded in padded_statements:
                    stmt = padded.element
                    if isinstance(stmt, If):
                        rewritten = self._rewrite_if(stmt)
                        if rewritten is not stmt:
                            padded = padded.replace(_element=rewritten)
                            changed = True
                        kept.append(padded)
                        continue
                    reduced, binding = self._match_in_block(stmt)
                    if reduced is not stmt:
                        changed = True
                    if reduced is not None:
                        kept.append(padded if reduced is stmt else padded.replace(_element=reduced))
                    if binding is not None:
                        # A comment on the statement describes what it imports, so it
                        # travels only when the whole statement is replaced.
                        prefix = (stmt.prefix if reduced is None
                                  else Space([], stmt.prefix.whitespace))
                        to_add.append((binding, len(kept), prefix))
                # Back to front, so an insertion never shifts a pending position.
                for binding, at, prefix in reversed(to_add):
                    if self._place_import(kept, binding, at, prefix):
                        changed = True
                return kept if changed else None

            def _match_in_block(self, stmt: Statement) -> Tuple[Optional[Statement],
                                                                Optional[_Binding]]:
                """`(statement to keep, import to bind here)` for a match in a block, and
                `(stmt, None)` for anything else."""
                if isinstance(stmt, MultiImport):
                    alias = self._check_for_old_import(stmt)
                    if alias is not None:
                        bound = new_alias or (alias or None)
                        if old_name:
                            return (self._remove_name_from_import(stmt, old_name),
                                    (new_module, new_name, bound))
                        return (self._remove_module_from_import(stmt, old_module),
                                (new_module, None, bound))
                elif isinstance(stmt, Import):
                    alias = self._check_for_old_single_import(stmt)
                    if alias is not None:
                        # The module is the whole statement, so the statement goes.
                        return None, (new_module, None, new_alias or (alias or None))
                # `import old_module` behind references this recipe rewrote to the new module:
                # bind the new module here as well, and let RemoveImport drop the old one once
                # nothing refers to it.
                if (old_name and self.rewrote_qualified_refs and
                        not self.direct_module_import_at_module_level and
                        self._binds_module(stmt, old_module)):
                    return stmt, (new_module, None, new_alias)
                return stmt, None

            @staticmethod
            def _binds_module(stmt: Statement, module: str) -> bool:
                """True when `stmt` is an `import module` rather than a `from` import."""
                if isinstance(stmt, MultiImport):
                    return stmt.from_ is None and any(
                        get_qualid_name(imp.qualid) == module for imp in stmt.names)
                return isinstance(stmt, Import) and get_qualid_name(stmt.qualid) == module

            def _place_import(self, kept: List[JRightPadded], binding: _Binding,
                              at: int, prefix: Space) -> bool:
                """Bind `binding` in the block, merged into a sibling import from the same
                module when there is one and otherwise as a statement of its own at `at`.
                False when the block already binds it."""
                module, name, alias = binding
                if name is None:
                    if any(self._binds_module(p.element, module) for p in kept):
                        return False
                else:
                    bound = alias or name
                    for index, padded in enumerate(kept):
                        stmt = padded.element
                        if not isinstance(stmt, MultiImport) or stmt.from_ is None:
                            continue
                        if get_name_string(stmt.from_) != module:
                            continue
                        elements = list(stmt.padding.names.padding.elements)
                        if any((get_alias_name(e.element) or get_qualid_name(e.element.qualid))
                               == bound for e in elements):
                            return False
                        if prefix.comments:
                            # A merge has nowhere to carry that comment.
                            break
                        kept[index] = padded.replace(_element=stmt.padding.replace(
                            _names=JContainer(stmt.padding.names.before,
                                              insert_member(elements,
                                                            create_import_element(name, alias)),
                                              stmt.padding.names.markers)))
                        return True
                statement = create_import_statement(module, name, alias).replace(prefix=prefix)
                kept.insert(at, JRightPadded(statement, Space.EMPTY, Markers.EMPTY))
                return True

            def _transfer_removed_prefixes(self, before: CompilationUnit, after: CompilationUnit) -> CompilationUnit:
                """Dropping a statement discards its prefix; when it is worth rescuing
                (see prefix_to_inherit) hand it to the next surviving statement,
                mirroring RemoveImport._remove_import."""
                removed_ids = ({p.element.id for p in before.padding.statements}
                               - {p.element.id for p in after.padding.statements})
                if not removed_ids:
                    return after
                inherited = None
                prefix_by_id = {}
                for index, padded in enumerate(before.padding.statements):
                    stmt = padded.element
                    if stmt.id in removed_ids:
                        prefix = prefix_to_inherit(stmt, index)
                        if prefix is not None:
                            inherited = prefix
                    elif inherited is not None:
                        # A whitespace-only prefix is handed only to a following
                        # import: a following plain statement keeps its own
                        # separation, which AddImport's front insertion relies on
                        # when it places the replacement import before it.
                        if inherited.comments or isinstance(stmt, (Import, MultiImport)):
                            prefix_by_id[stmt.id] = inherited
                        inherited = None
                if not prefix_by_id:
                    return after
                new_padded = [
                    p.replace(_element=p.element.replace(prefix=prefix_by_id[p.element.id]))
                    if p.element.id in prefix_by_id else p
                    for p in after.padding.statements
                ]
                return after.padding.replace(_statements=new_padded)

            def _get_new_module_type(self) -> JavaType.Class:
                if self.new_module_type is None:
                    self.new_module_type = _create_module_type(new_module)
                return self.new_module_type

            def _check_for_old_single_import(self, imp: Import) -> Optional[str]:
                """Check if a standalone J.Import matches the old import."""
                if old_name:
                    return None
                name = get_qualid_name(imp.qualid)
                if name == old_module:
                    return get_alias_name(imp) or ""
                return None

            def _check_for_old_import(self, multi: MultiImport) -> Optional[str]:
                """Check if this MultiImport matches the old import.

                Returns:
                    None if no match
                    "" if match with no alias
                    alias string if match with alias
                """
                if old_name:
                    # Looking for: from old_module import old_name [as alias]
                    if multi.from_ is None:
                        return None
                    from_name = get_name_string(multi.from_)
                    if from_name != old_module:
                        return None
                    for imp in multi.names:
                        name = get_qualid_name(imp.qualid)
                        if name == old_name:
                            return get_alias_name(imp) or ""
                else:
                    # Looking for: import old_module [as alias]
                    if multi.from_ is not None:
                        return None
                    for imp in multi.names:
                        name = get_qualid_name(imp.qualid)
                        if name == old_module:
                            return get_alias_name(imp) or ""
                return None

            def _remove_name_from_import(self, multi: MultiImport,
                                         name_to_remove: str) -> Optional[MultiImport]:
                """Remove a specific name from a 'from X import a, b, c' statement."""
                from rewrite.java.support_types import JContainer
                from rewrite.java.tree import Space

                existing_padded = multi.padding.names.padding.elements
                new_padded = [
                    p for p in existing_padded
                    if get_qualid_name(p.element.qualid) != name_to_remove
                ]

                if len(new_padded) == 0:
                    return None
                if len(new_padded) < len(existing_padded):
                    # Fix up first element prefix
                    first = new_padded[0]
                    if first.element.prefix != Space.EMPTY:
                        new_padded[0] = first.replace(_element=first.element.replace(prefix=Space.EMPTY))
                    return multi.padding.replace(
                        _names=JContainer(
                            multi.padding.names.before,
                            new_padded,
                            multi.padding.names.markers
                        )
                    )
                return multi

            def _remove_module_from_import(self, multi: MultiImport,
                                           module_to_remove: str) -> Optional[MultiImport]:
                """Remove a module from an import statement."""
                from rewrite.java.support_types import JContainer
                from rewrite.java.tree import Space

                existing_padded = multi.padding.names.padding.elements
                new_padded = [
                    p for p in existing_padded
                    if get_qualid_name(p.element.qualid) != module_to_remove
                ]

                if len(new_padded) == 0:
                    return None
                if len(new_padded) < len(existing_padded):
                    first = new_padded[0]
                    if first.element.prefix != Space.EMPTY:
                        new_padded[0] = first.replace(_element=first.element.replace(prefix=Space.EMPTY))
                    return multi.padding.replace(
                        _names=JContainer(
                            multi.padding.names.before,
                            new_padded,
                            multi.padding.names.markers
                        )
                    )
                return multi

        if not old_module:
            return ChangeImportVisitor()
        # Gate on the as-written import: a file can only contain `import old_module`
        # or `from old_module import ...` if it imports old_module, so this is a
        # correct superset. uses_import (not uses_type) because the type checker
        # canonicalizes aliases and drops removed symbols, both of which would make
        # a type-based gate skip files this recipe must change.
        from rewrite import Preconditions
        from rewrite.python.preconditions import uses_import
        return Preconditions.check(uses_import(old_module), ChangeImportVisitor())
