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

"""Tests for ChangeImport recipe."""

from rewrite.java.support_types import JavaType
from rewrite.java.tree import FieldAccess, Identifier, MethodInvocation
from rewrite.python.recipes.change_import import ChangeImport
from rewrite.python.tree import CompilationUnit
from rewrite.python.visitor import PythonVisitor
from rewrite.test import RecipeSpec, python


class TestChangeImport:
    """Tests for the ChangeImport recipe."""

    def test_change_from_import_module_and_name(self):
        """Change: from collections import Mapping -> from collections.abc import Mapping"""
        spec = RecipeSpec(recipe=ChangeImport(
            old_module='collections',
            old_name='Mapping',
            new_module='collections.abc',
            new_name='Mapping',
        ))
        spec.rewrite_run(
            python(
                """
                from collections import Mapping
                x: Mapping = {}
                """,
                """
                from collections.abc import Mapping
                x: Mapping = {}
                """,
            )
        )

    def test_change_import_only_statement(self):
        """Import is the only statement in the file — no leading newline in output."""
        spec = RecipeSpec(recipe=ChangeImport(
            old_module='typing',
            old_name='Callable',
            new_module='collections.abc',
        ))
        spec.rewrite_run(
            python(
                "from typing import Callable\n",
                "from collections.abc import Callable\n",
            )
        )

    def test_change_direct_import(self):
        """Change: import os -> import pathlib"""
        spec = RecipeSpec(recipe=ChangeImport(
            old_module='os',
            new_module='pathlib',
        ))
        spec.rewrite_run(
            python(
                """
                import os
                x = 1
                """,
                """
                import pathlib
                x = 1
                """,
            )
        )

    def test_no_change_when_import_not_present(self):
        """No change when the old import doesn't exist."""
        spec = RecipeSpec(recipe=ChangeImport(
            old_module='collections',
            old_name='Mapping',
            new_module='collections.abc',
            new_name='Mapping',
        ))
        spec.rewrite_run(
            python(
                """
                import sys
                x = 1
                """,
            )
        )

    def test_change_from_import_different_name(self):
        """Change: from os.path import join -> from pathlib import PurePath"""
        spec = RecipeSpec(recipe=ChangeImport(
            old_module='os.path',
            old_name='join',
            new_module='pathlib',
            new_name='PurePath',
        ))
        spec.rewrite_run(
            python(
                """
                from os.path import join
                x = 1
                """,
                """
                from pathlib import PurePath
                x = 1
                """,
            )
        )

    def test_change_removes_one_name_from_multi_import(self):
        """Change one name from 'from os.path import join, exists' leaves exists."""
        spec = RecipeSpec(recipe=ChangeImport(
            old_module='os.path',
            old_name='join',
            new_module='pathlib',
            new_name='PurePath',
        ))
        spec.rewrite_run(
            python(
                """
                from os.path import join, exists
                x = 1
                """,
                """
                from os.path import exists
                from pathlib import PurePath
                x = 1
                """,
            )
        )

    def test_change_qualified_method_call(self):
        """Change: import fractions / fractions.gcd() -> import math / math.gcd()"""
        spec = RecipeSpec(recipe=ChangeImport(
            old_module='fractions',
            old_name='gcd',
            new_module='math',
        ))
        spec.rewrite_run(
            python(
                """
                import fractions
                result = fractions.gcd(12, 8)
                """,
                """
                import math
                result = math.gcd(12, 8)
                """,
            )
        )

    def test_change_aliased_qualified_method_call(self):
        """Change: import fractions as f / f.gcd() -> import math / math.gcd()"""
        spec = RecipeSpec(recipe=ChangeImport(
            old_module='fractions',
            old_name='gcd',
            new_module='math',
        ))
        spec.rewrite_run(
            python(
                """
                import fractions as f
                result = f.gcd(12, 8)
                """,
                """
                import math
                result = math.gcd(12, 8)
                """,
            )
        )

    def test_change_qualified_ref_keeps_import_when_other_usages(self):
        """import fractions stays when fractions.Fraction is still used."""
        spec = RecipeSpec(recipe=ChangeImport(
            old_module='fractions',
            old_name='gcd',
            new_module='math',
        ))
        spec.rewrite_run(
            python(
                """
                import fractions
                result = fractions.gcd(12, 8)
                f = fractions.Fraction(1, 3)
                """,
                """
                import fractions
                import math
                result = math.gcd(12, 8)
                f = fractions.Fraction(1, 3)
                """,
            )
        )

    def test_change_qualified_field_access(self):
        """Change: import fractions / fn = fractions.gcd -> import math / fn = math.gcd"""
        spec = RecipeSpec(recipe=ChangeImport(
            old_module='fractions',
            old_name='gcd',
            new_module='math',
        ))
        spec.rewrite_run(
            python(
                """
                import fractions
                fn = fractions.gcd
                """,
                """
                import math
                fn = math.gcd
                """,
            )
        )

    def test_change_qualified_ref_with_different_new_name(self):
        """Qualified ref rewrite when new_name differs from old_name."""
        spec = RecipeSpec(recipe=ChangeImport(
            old_module='fractions',
            old_name='gcd',
            new_module='math',
            new_name='greatest_common_divisor',
        ))
        spec.rewrite_run(
            python(
                """
                import fractions
                result = fractions.gcd(12, 8)
                fn = fractions.gcd
                """,
                """
                import math
                result = math.greatest_common_divisor(12, 8)
                fn = math.greatest_common_divisor
                """,
            )
        )

    def test_type_attribution_updated_on_qualified_refs(self):
        """Type attribution is updated on rewritten method invocations and field accesses."""
        from dataclasses import replace as dc_replace
        from rewrite.python.recipes.change_import import _create_module_type

        fractions_type = _create_module_type('fractions')

        def inject_types(source_file):
            """Inject type info onto fractions references so the test doesn't depend on ty."""
            class TypeInjector(PythonVisitor):
                def visit_method_invocation(self, method, p):
                    method = super().visit_method_invocation(method, p)
                    if (isinstance(method, MethodInvocation)
                            and isinstance(method.select, Identifier)
                            and method.select.simple_name == 'fractions'):
                        new_select = method.select.replace(_type=fractions_type)
                        method = method.padding.replace(
                            _select=method.padding.select.replace(_element=new_select))
                        method = method.replace(_method_type=JavaType.Method(
                            _declaring_type=fractions_type,
                            _name='gcd',
                        ))
                    return method

                def visit_field_access(self, fa, p):
                    fa = super().visit_field_access(fa, p)
                    if (isinstance(fa, FieldAccess)
                            and isinstance(fa.target, Identifier)
                            and fa.target.simple_name == 'fractions'):
                        fa = fa.replace(_target=fa.target.replace(_type=fractions_type))
                    return fa

            return TypeInjector().visit(source_file, None)

        errors = []

        def check_types(source_file):
            assert isinstance(source_file, CompilationUnit)

            class TypeChecker(PythonVisitor):
                def visit_method_invocation(self, method, p):
                    if isinstance(method.select, Identifier) and method.select.simple_name == 'math':
                        if method.select.type is None:
                            errors.append("method select identifier has no type")
                        elif not isinstance(method.select.type, JavaType.FullyQualified):
                            errors.append(f"method select type is {type(method.select.type).__name__}, expected FullyQualified")
                        elif method.select.type._fully_qualified_name != 'math':
                            errors.append(f"method select type fqn is '{method.select.type._fully_qualified_name}', expected 'math'")
                        if method.method_type is None:
                            errors.append("method_type is None")
                        elif method.method_type.declaring_type is None:
                            errors.append("method_type.declaring_type is None")
                        elif method.method_type.declaring_type._fully_qualified_name != 'math':
                            errors.append(f"method_type declaring_type fqn is '{method.method_type.declaring_type._fully_qualified_name}', expected 'math'")
                        if method.method_type is not None and method.method_type.name != 'gcd':
                            errors.append(f"method_type.name is '{method.method_type.name}', expected 'gcd'")
                    return method

                def visit_field_access(self, fa, p):
                    if isinstance(fa.target, Identifier) and fa.target.simple_name == 'math' and fa.name.simple_name == 'gcd':
                        if fa.target.type is None:
                            errors.append("field access target identifier has no type")
                        elif not isinstance(fa.target.type, JavaType.FullyQualified):
                            errors.append(f"field access target type is {type(fa.target.type).__name__}, expected FullyQualified")
                        elif fa.target.type._fully_qualified_name != 'math':
                            errors.append(f"field access target type fqn is '{fa.target.type._fully_qualified_name}', expected 'math'")
                    return fa

            TypeChecker().visit(source_file, None)

        spec = RecipeSpec(recipe=ChangeImport(
            old_module='fractions',
            old_name='gcd',
            new_module='math',
        ))
        spec.rewrite_run(
            python(
                """
                import fractions
                result = fractions.gcd(12, 8)
                fn = fractions.gcd
                """,
                """
                import math
                result = math.gcd(12, 8)
                fn = math.gcd
                """,
                before_recipe=inject_types,
                after_recipe=check_types,
            )
        )
        assert not errors, "Type attribution errors:\n" + "\n".join(f"  - {e}" for e in errors)

    def test_change_from_import_renames_bare_references(self):
        """Change: from time import clock / clock() -> from time import perf_counter / perf_counter()"""
        spec = RecipeSpec(recipe=ChangeImport(
            old_module='time',
            old_name='clock',
            new_module='time',
            new_name='perf_counter',
        ))
        spec.rewrite_run(
            python(
                """
                from time import clock
                clock()
                """,
                """
                from time import perf_counter
                perf_counter()
                """,
            )
        )

    def test_change_from_import_renames_bare_reference_with_args(self):
        """Bare reference with arguments: encodestring(data) -> encodebytes(data)"""
        spec = RecipeSpec(recipe=ChangeImport(
            old_module='base64',
            old_name='encodestring',
            new_module='base64',
            new_name='encodebytes',
        ))
        spec.rewrite_run(
            python(
                """
                from base64 import encodestring
                encodestring(data)
                """,
                """
                from base64 import encodebytes
                encodebytes(data)
                """,
            )
        )

    def test_no_rename_when_name_unchanged(self):
        """No bare reference rename when only the module changes."""
        spec = RecipeSpec(recipe=ChangeImport(
            old_module='fractions',
            old_name='gcd',
            new_module='math',
        ))
        spec.rewrite_run(
            python(
                """
                from fractions import gcd
                gcd(12, 8)
                """,
                """
                from math import gcd
                gcd(12, 8)
                """,
            )
        )

    def test_no_rename_bare_ref_in_unrelated_code(self):
        """Only rename bare references that match old_name, not other identifiers."""
        spec = RecipeSpec(recipe=ChangeImport(
            old_module='time',
            old_name='clock',
            new_module='time',
            new_name='perf_counter',
        ))
        spec.rewrite_run(
            python(
                """
                from time import clock
                clock()
                x = 1
                """,
                """
                from time import perf_counter
                perf_counter()
                x = 1
                """,
            )
        )

    def test_no_rename_shadowed_in_function_scope(self):
        """Don't rename a local variable in function scope that shadows the imported name."""
        spec = RecipeSpec(recipe=ChangeImport(
            old_module='time',
            old_name='clock',
            new_module='time',
            new_name='perf_counter',
        ))
        spec.rewrite_run(
            python(
                """
                from time import clock

                def foo():
                    clock = 42
                    return clock

                clock()
                """,
                """
                from time import perf_counter

                def foo():
                    clock = 42
                    return clock

                perf_counter()
                """,
            )
        )

    def test_rename_bare_ref_at_module_level_even_if_typed(self):
        """Module-level bare references to an imported name should be renamed
        even when ty populates field_type (which indicates a variable)."""
        spec = RecipeSpec(recipe=ChangeImport(
            old_module='time',
            old_name='clock',
            new_module='time',
            new_name='perf_counter',
        ))
        spec.rewrite_run(
            python(
                """
                from time import clock
                result = clock()
                ref = clock
                """,
                """
                from time import perf_counter
                result = perf_counter()
                ref = perf_counter
                """,
            )
        )

    def test_pep585_typing_to_builtin_adds_no_builtins_import(self):
        """PEP 585: 'from typing import List' -> 'list[...]' must NOT add
        'from builtins import list' (builtins are always available)."""
        spec = RecipeSpec(recipe=ChangeImport(
            old_module='typing',
            old_name='List',
            new_module='builtins',
            new_name='list',
        ))
        spec.rewrite_run(
            python(
                """
                from typing import List, Dict

                x: List[str] = []
                y: Dict[str, int] = {}
                """,
                """
                from typing import Dict

                x: list[str] = []
                y: Dict[str, int] = {}
                """,
            )
        )

    def test_emptied_first_import_no_leading_blank_line(self):
        """Emptying the file's first import must not leave a leading blank line."""
        spec = RecipeSpec(recipe=ChangeImport(
            old_module='collections',
            old_name='Mapping',
            new_module='collections.abc',
        ))
        spec.rewrite_run(
            python(
                """
                from collections import Mapping
                from collections.abc import Callable
                d: Mapping = {}
                """,
                """
                from collections.abc import Callable, Mapping
                d: Mapping = {}
                """,
            )
        )

    def test_incrementally_emptied_import_no_leading_blank_line(self):
        """Successive ChangeImports that empty a multi-name import one name at a
        time must not leave a leading blank line."""
        spec = RecipeSpec().with_recipes(
            ChangeImport(old_module='collections', old_name='Callable', new_module='collections.abc'),
            ChangeImport(old_module='collections', old_name='Mapping', new_module='collections.abc'),
            ChangeImport(old_module='collections', old_name='Sequence', new_module='collections.abc'),
        )
        spec.rewrite_run(
            python(
                """
                from collections import Callable, Mapping, Sequence
                d: Mapping = {}
                """,
                """
                from collections.abc import Callable, Mapping, Sequence
                d: Mapping = {}
                """,
            )
        )

    def test_emptied_import_preserves_leading_comment(self):
        """A comment in the removed import's prefix moves to the next statement."""
        spec = RecipeSpec(recipe=ChangeImport(
            old_module='collections',
            old_name='Mapping',
            new_module='collections.abc',
        ))
        spec.rewrite_run(
            python(
                """
                # comment
                from collections import Mapping
                from collections.abc import Callable
                d: Mapping = {}
                """,
                """
                # comment
                from collections.abc import Callable, Mapping
                d: Mapping = {}
                """,
            )
        )

    def test_both_from_import_and_direct_import(self):
        """When a file has both 'from X import name' and 'import X', handle without duplicates."""
        spec = RecipeSpec(recipe=ChangeImport(
            old_module='fractions',
            old_name='gcd',
            new_module='math',
            new_name='gcd',
        ))
        spec.rewrite_run(
            python(
                """
                from fractions import gcd
                import fractions

                result = gcd(12, 8)
                """,
                # The old from-import is removed and new one added after
                # existing imports; import fractions is preserved.
                """
                import fractions
                from math import gcd

                result = gcd(12, 8)
                """,
            )
        )


class TestChangeImportLeavesUnrelatedImports:
    """ChangeImport schedules a whole-module RemoveImport for the old module,
    which must not reach imports the recipe was never asked about."""

    def test_unrelated_canonically_related_import_survives(self):
        """`Iterable` is canonically typing.Iterable, but the file imports it
        from collections.abc and the recipe only concerns typing.List."""
        spec = RecipeSpec(recipe=ChangeImport(
            old_module='typing',
            old_name='List',
            new_module='mytypes',
            new_name='List',
        ))
        spec.rewrite_run(
            python(
                """
                from collections.abc import Iterable
                import typing

                def f(x: Iterable) -> typing.List:
                    return []
                """,
                """
                from collections.abc import Iterable
                import mytypes

                def f(x: Iterable) -> mytypes.List:
                    return []
                """,
            )
        )


class TestChangeImportFunctionScopedReferences:
    """References inside a function body are renamed unless the enclosing scope
    binds the name itself."""

    def test_rename_annotation_references_in_function(self):
        """Annotations are references to the imported name, not bindings of it,
        so they are renamed along with the import."""
        for type_attribution in (False, True):
            spec = RecipeSpec(recipe=ChangeImport(
                old_module='typing',
                old_name='Deque',
                new_module='collections',
                new_name='deque',
            ), type_attribution=type_attribution)
            spec.rewrite_run(
                python(
                    """\
                    from typing import Deque


                    def f(q: Deque[int]) -> Deque[int]:
                        local: Deque[int] = q
                        return local
                    """,
                    """\
                    from collections import deque


                    def f(q: deque[int]) -> deque[int]:
                        local: deque[int] = q
                        return local
                    """,
                )
            )

    def test_rename_plain_references_in_function(self):
        for type_attribution in (False, True):
            spec = RecipeSpec(recipe=ChangeImport(
                old_module='time',
                old_name='clock',
                new_module='time',
                new_name='perf_counter',
            ), type_attribution=type_attribution)
            spec.rewrite_run(
                python(
                    """\
                    from time import clock


                    def f():
                        return clock()
                    """,
                    """\
                    from time import perf_counter


                    def f():
                        return perf_counter()
                    """,
                )
            )

    def test_no_rename_of_names_bound_in_the_same_function(self):
        for type_attribution in (False, True):
            spec = RecipeSpec(recipe=ChangeImport(
                old_module='time',
                old_name='clock',
                new_module='time',
                new_name='perf_counter',
            ), type_attribution=type_attribution)
            spec.rewrite_run(
                python(
                    """\
                    from time import clock


                    def assigned():
                        clock = 1
                        return clock


                    def parameter(clock):
                        return clock


                    def loop_target():
                        for clock in range(3):
                            print(clock)


                    def as_clause():
                        with open("f") as clock:
                            return clock


                    def nested_def():
                        def clock():
                            pass
                        return clock


                    def uses_import():
                        return clock()
                    """,
                    """\
                    from time import perf_counter


                    def assigned():
                        clock = 1
                        return clock


                    def parameter(clock):
                        return clock


                    def loop_target():
                        for clock in range(3):
                            print(clock)


                    def as_clause():
                        with open("f") as clock:
                            return clock


                    def nested_def():
                        def clock():
                            pass
                        return clock


                    def uses_import():
                        return perf_counter()
                    """,
                )
            )

    def test_nested_function_shadowing_does_not_leak_outward(self):
        for type_attribution in (False, True):
            spec = RecipeSpec(recipe=ChangeImport(
                old_module='time',
                old_name='clock',
                new_module='time',
                new_name='perf_counter',
            ), type_attribution=type_attribution)
            spec.rewrite_run(
                python(
                    """\
                    from time import clock


                    def outer():
                        def inner():
                            clock = 1
                            return clock
                        return clock() + inner()
                    """,
                    """\
                    from time import perf_counter


                    def outer():
                        def inner():
                            clock = 1
                            return clock
                        return perf_counter() + inner()
                    """,
                )
            )


class TestChangeImportPythonScopeRules:
    """Renaming follows Python's own scoping: a name is a local only where the
    interpreter would resolve it to one."""

    @staticmethod
    def _run(before, after=None):
        for type_attribution in (False, True):
            spec = RecipeSpec(recipe=ChangeImport(
                old_module='time',
                old_name='clock',
                new_module='time',
                new_name='perf_counter',
            ), type_attribution=type_attribution)
            spec.rewrite_run(python(before, after) if after else python(before))

    def test_class_attribute_does_not_shadow_inside_methods(self):
        """A class body is not part of the scope chain of its methods."""
        self._run(
            """\
            from time import clock


            class C:
                clock = 1

                def m(self):
                    return clock()
            """,
            """\
            from time import perf_counter


            class C:
                clock = 1

                def m(self):
                    return perf_counter()
            """,
        )

    def test_class_attribute_shadows_within_the_class_body(self):
        self._run(
            """\
            from time import clock


            class C:
                clock = 1
                alias = clock
            """,
            """\
            from time import perf_counter


            class C:
                clock = 1
                alias = clock
            """,
        )

    def test_attribute_and_subscript_targets_do_not_bind(self):
        """``self.clock = 1`` and ``d[clock] = 1`` assign through an object."""
        self._run(
            """\
            from time import clock


            class C:
                def m(self, d):
                    self.clock = 1
                    d[clock] = 2
                    return clock()
            """,
            """\
            from time import perf_counter


            class C:
                def m(self, d):
                    self.clock = 1
                    d[perf_counter] = 2
                    return perf_counter()
            """,
        )

    def test_tuple_unpacking_binds(self):
        self._run(
            """\
            from time import clock


            def f(pair):
                clock, other = pair
                return clock
            """,
            """\
            from time import perf_counter


            def f(pair):
                clock, other = pair
                return clock
            """,
        )

    def test_function_local_import_shadows(self):
        self._run(
            """\
            from time import clock


            def f():
                from mymod import clock
                return clock()
            """,
            """\
            from time import perf_counter


            def f():
                from mymod import clock
                return clock()
            """,
        )

    def test_decorator_resolves_in_the_enclosing_scope(self):
        self._run(
            """\
            from time import clock


            @clock
            def f():
                clock = 1
                return clock
            """,
            """\
            from time import perf_counter


            @perf_counter
            def f():
                clock = 1
                return clock
            """,
        )

    def test_parameter_default_resolves_in_the_enclosing_scope(self):
        self._run(
            """\
            from time import clock


            def f(x=clock):
                clock = 1
                return clock, x
            """,
            """\
            from time import perf_counter


            def f(x=perf_counter):
                clock = 1
                return clock, x
            """,
        )

    def test_global_declaration_is_not_a_local_binding(self):
        self._run(
            """\
            from time import clock


            def f():
                global clock
                clock = 1
            """,
            """\
            from time import perf_counter


            def f():
                global perf_counter
                perf_counter = 1
            """,
        )
