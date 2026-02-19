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

import pytest
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

    @pytest.mark.xfail(reason="ChangeImport does not yet perform scope analysis to avoid renaming shadowed locals")
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
