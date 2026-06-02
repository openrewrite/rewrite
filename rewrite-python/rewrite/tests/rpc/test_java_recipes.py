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

    def test_change_simple_type(self, java_rpc):
        """Test changing a simple type reference."""
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
        """Test adding a literal argument to a method call."""
        from rewrite.python import AddLiteralMethodArgument

        spec = RecipeSpec(
            recipe=AddLiteralMethodArgument(
                method_pattern="datetime.datetime now()",
                argument_index=0,
                literal="datetime.UTC"
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

                now = datetime.now(datetime.UTC)
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
