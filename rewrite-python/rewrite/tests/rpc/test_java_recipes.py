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

"""
Tests for Java recipe wrappers via RPC.

These tests verify that Python recipes can successfully delegate to Java
recipes like ChangeType, ChangeMethodName, etc.

Note: These tests require the Java RPC server infrastructure:
    ./gradlew :rewrite-python:generateTestClasspath

Or set the REWRITE_PYTHON_CLASSPATH environment variable.
"""

import pytest

from rewrite.test import RecipeSpec, python


@pytest.mark.requires_java_rpc
class TestChangeType:
    """Tests for the ChangeType Java recipe wrapper."""

    def test_change_type_with_unqualified_target(self, java_rpc):
        """Test changing a type reference to an unqualified target name
        (contrast ``builtins.list`` in the type-attribution test below).

        The unused ``from typing import List`` is removed: Java's ChangeType
        retires Python imports through ``PythonImportService``, which reaches
        the Python-side import visitors over the serving RPC connection when
        the JVM is the spawned server, as in this Python-hosted topology.
        """
        from rewrite.python import ChangeType

        spec = RecipeSpec(
            recipe=ChangeType(
                old_fully_qualified_type_name="typing.List",
                new_fully_qualified_type_name="list"
            )
        )
        spec.rewrite_run(
            python(
                """
                from typing import List

                def foo(items: List[str]) -> List[int]:
                    pass
                """,
                """
                def foo(items: list[str]) -> list[int]:
                    pass
                """
            )
        )


    def test_change_simple_type_updates_type_attribution(self, java_rpc):
        """The renamed identifiers must also carry the *new* JavaType, so that
        downstream type-driven logic (``uses_type``, MethodMatcher, a second
        recipe in a composite) matches on ``builtins.list``. As a consequence,
        a second run over the output must be a no-op: every former
        ``typing.List`` reference is already typed ``builtins.list``.
        """
        from rewrite import InMemoryExecutionContext
        from rewrite.java.support_types import JavaType
        from rewrite.python import ChangeType
        from rewrite.python.visitor import PythonVisitor

        def assert_types(source_file):
            list_idents = []

            class CollectTypes(PythonVisitor):
                def visit_identifier(self, identifier, p):
                    if identifier.simple_name == "list":
                        list_idents.append(identifier)
                    return identifier

            CollectTypes().visit(source_file, None)
            assert len(list_idents) == 2, \
                f"expected 2 'list' identifiers, found {len(list_idents)}"
            for ident in list_idents:
                t = ident.type
                assert isinstance(t, JavaType.FullyQualified), \
                    f"identifier 'list' has type {t!r}"
                assert t.fully_qualified_name == "builtins.list", \
                    f"identifier 'list' still typed as {t.fully_qualified_name}"

            editor = spec.recipe.recipe_list()[0].editor()
            run2 = editor.visit(source_file, InMemoryExecutionContext())
            assert run2 is source_file, "second run should be a no-op"

        spec = RecipeSpec(
            recipe=ChangeType(
                old_fully_qualified_type_name="typing.List",
                new_fully_qualified_type_name="builtins.list"
            )
        )
        spec.rewrite_run(
            python(
                """
                from typing import List

                def foo(items: List[str]) -> List[int]:
                    pass
                """,
                """
                def foo(items: list[str]) -> list[int]:
                    pass
                """,
                after_recipe=assert_types,
            )
        )


@pytest.mark.requires_java_rpc
class TestChangeMethodName:
    """Tests for the ChangeMethodName Java recipe wrapper."""

    def test_rename_method(self, java_rpc):
        """Test renaming a method call."""
        from rewrite.python import ChangeMethodName

        spec = RecipeSpec(
            recipe=ChangeMethodName(
                method_pattern="datetime.datetime utcnow()",
                new_method_name="now"
            )
        )
        spec.rewrite_run(
            python(
                """
                from datetime import datetime

                now = datetime.utcnow()
                """,
                """
                from datetime import datetime

                now = datetime.now()
                """
            )
        )


@pytest.mark.requires_java_rpc
class TestAddLiteralMethodArgument:
    """Tests for the AddLiteralMethodArgument Java recipe wrapper."""

    def test_add_argument(self, java_rpc):
        """Test adding a literal argument to a method call.

        The method pattern matches against the *declared* signature, where
        ``datetime.now`` carries its optional ``tz`` parameter — hence
        ``(..)`` rather than ``()``. A str option is inserted as a string
        literal, i.e. quoted.
        """
        from rewrite.python import AddLiteralMethodArgument

        spec = RecipeSpec(
            recipe=AddLiteralMethodArgument(
                method_pattern="datetime.datetime now(..)",
                argument_index=0,
                literal="UTC"
            )
        )
        spec.rewrite_run(
            python(
                """
                from datetime import datetime

                now = datetime.now()
                """,
                """
                from datetime import datetime

                now = datetime.now("UTC")
                """
            )
        )


@pytest.mark.requires_java_rpc
class TestDeleteMethodArgument:
    """Tests for the DeleteMethodArgument Java recipe wrapper."""

    def test_delete_argument(self, java_rpc):
        """Test removing an argument from a method call."""
        from rewrite.python import DeleteMethodArgument

        spec = RecipeSpec(
            recipe=DeleteMethodArgument(
                method_pattern="foo.bar baz(..)",
                argument_index=1
            )
        )
        spec.rewrite_run(
            python(
                """
                import foo

                foo.bar.baz(1, 2, 3)
                """,
                """
                import foo

                foo.bar.baz(1, 3)
                """
            )
        )


@pytest.mark.requires_java_rpc
class TestPrepareRecipe:
    """Tests for preparing recipes via RPC."""

    def test_prepare_text_find(self, java_rpc):
        """Test preparing and running a Java recipe via RPC."""
        # Directly use the RPC client to prepare a recipe
        result = java_rpc.send_request("PrepareRecipe", {
            "id": "org.openrewrite.text.Find",
            "options": {"find": "hello"}
        })

        assert result is not None
        assert "editVisitor" in result or "scanVisitor" in result
