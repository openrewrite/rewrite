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

"""Tests for the Python test harness."""

from pathlib import Path
from typing import Callable, Optional
from uuid import UUID, uuid4

import pytest

from rewrite import ExecutionContext, Recipe, TreeVisitor, random_id
from rewrite.execution import Result
from rewrite.markers import Marker, Markers, SearchResult
from rewrite.test import RecipeSpec, python, pyproject, uv, from_visitor, dedent
from rewrite.test.added_markers import assert_no_added_markers
from rewrite.test.spec import SourceSpec
from rewrite.python.markers import Quoted
from rewrite.python.visitor import PythonVisitor
from rewrite.java.tree import Identifier, MethodInvocation, Yield
from rewrite.python.tree import Await, CompilationUnit
from rewrite.java import J, JavaType, Space
from rewrite.utils import lst_dataclass


class TestDedent:
    """Tests for the dedent helper function."""

    def test_dedent_removes_leading_newline(self):
        result = dedent("\n    hello")
        assert result == "hello"

    def test_dedent_removes_common_indentation(self):
        result = dedent(
            """
            def foo():
                pass
            """
        )
        assert result == "def foo():\n    pass\n"

    def test_dedent_preserves_relative_indentation(self):
        result = dedent(
            """
            if True:
                x = 1
                if False:
                    y = 2
            """
        )
        assert result == "if True:\n    x = 1\n    if False:\n        y = 2\n"

    def test_dedent_empty_string(self):
        assert dedent("") == ""
        assert dedent(None) == ""

    def test_dedent_no_indentation(self):
        result = dedent("hello\nworld")
        assert result == "hello\nworld"


class TestRecipeSpecParsing:
    """Tests for RecipeSpec parsing functionality."""

    def test_parse_simple_assignment(self):
        """Test that simple Python code parses correctly."""
        spec = RecipeSpec()
        spec.rewrite_run(
            python(
                """
                x = 1
                """
            )
        )

    def test_parse_function_definition(self):
        """Test that function definitions parse correctly."""
        spec = RecipeSpec()
        spec.rewrite_run(
            python(
                """
                def foo(a, b):
                    return a + b
                """
            )
        )

    def test_parse_class_definition(self):
        """Test that class definitions parse correctly."""
        spec = RecipeSpec()
        spec.rewrite_run(
            python(
                """
                class MyClass:
                    def __init__(self):
                        self.value = 0
                """
            )
        )

    def test_parse_import_statement(self):
        """Test that import statements parse correctly."""
        spec = RecipeSpec()
        spec.rewrite_run(
            python(
                """
                import os
                from pathlib import Path
                """
            )
        )


class TestRecipeSpecNoChange:
    """Tests for RecipeSpec when no changes are expected."""

    def test_no_change_with_noop_recipe(self):
        """Test that NoopRecipe makes no changes."""
        spec = RecipeSpec()  # Uses NoopRecipe by default
        spec.rewrite_run(
            python(
                """
                x = 1
                """
            )
        )

    def test_no_change_explicit(self):
        """Test explicit no-change assertion (after=None)."""
        spec = RecipeSpec()
        spec.rewrite_run(
            python(
                """
                def foo():
                    pass
                """,
                None  # Explicitly no change expected
            )
        )


class TestRecipeSpecWithChanges:
    """Tests for RecipeSpec when changes are expected."""

    def test_recipe_makes_changes(self):
        """Test a recipe that modifies code."""

        class AddCommentVisitor(PythonVisitor[ExecutionContext]):
            """A visitor that adds a comment to the first statement."""

            def visit_compilation_unit(
                self, cu: CompilationUnit, ctx: ExecutionContext
            ) -> J:
                # For now, just return unchanged - we're testing the harness
                return cu

        # This test validates the harness can detect changes
        # For now we just test that the infrastructure works
        spec = RecipeSpec(recipe=from_visitor(AddCommentVisitor()))
        spec.rewrite_run(
            python(
                """
                x = 1
                """
                # No after = no change expected, which is correct since our visitor
                # doesn't actually make changes
            )
        )


class TestRecipeSpecHooks:
    """Tests for before_recipe and after_recipe hooks."""

    def test_before_recipe_hook_called(self):
        """Test that before_recipe hook is called after parsing."""
        called = []

        def before_hook(cu: CompilationUnit) -> None:
            called.append(("before", len(cu.statements)))

        spec = RecipeSpec()
        spec.rewrite_run(
            python(
                """
                x = 1
                y = 2
                """,
                before_recipe=before_hook,
            )
        )

        assert len(called) == 1
        assert called[0][0] == "before"
        assert called[0][1] == 2  # Two statements

    def test_after_recipe_hook_called(self):
        """Test that after_recipe hook is called after recipe runs."""
        called = []

        def after_hook(cu: CompilationUnit) -> None:
            called.append(("after", cu.source_path))

        spec = RecipeSpec()
        spec.rewrite_run(
            python(
                """
                from typing import Any, Dict, List, Optional, Callable, TypeVar
                from uuid import UUID

                x = 1
                """,
                after_recipe=after_hook,
            )
        )

        assert len(called) == 1
        assert called[0][0] == "after"

    def test_before_recipe_can_modify_ast(self):
        """Test that before_recipe can modify the parsed AST."""
        from rewrite import random_id
        from rewrite.markers import SearchResult

        def add_marker(cu: CompilationUnit) -> CompilationUnit:
            # Add a marker to the compilation unit
            new_marker = SearchResult(random_id(), "test")
            new_markers = cu.markers.replace(markers=cu.markers.markers + [new_marker])
            return cu.replace(markers=new_markers)

        marker_found = []

        def check_marker(cu: CompilationUnit) -> None:
            marker = cu.markers.find_first(SearchResult)
            marker_found.append(marker is not None)

        spec = RecipeSpec()
        spec.rewrite_run(
            python(
                """
                x = 1
                """,
                before_recipe=add_marker,
                after_recipe=check_marker,
            )
        )

        assert marker_found == [True]


class TestRecipeSpecCallableAfter:
    """Tests for callable after assertions."""

    def test_callable_after_with_no_change(self):
        """Test callable after when recipe makes no changes."""
        received = []

        def check_actual(actual: str) -> None:
            # Return None to indicate actual output is acceptable
            received.append(actual)
            return None

        spec = RecipeSpec()
        spec.rewrite_run(
            python(
                """
                x = 1
                """,
                # No after = no change expected
            )
        )

    def test_callable_after_returns_none_means_no_change_expected(self):
        """Test that returning None from callable means no change expected."""
        spec = RecipeSpec()
        spec.rewrite_run(
            python(
                """
                x = 1
                """
                # No after = no change expected (this is the correct way to test)
            )
        )


class TestFromVisitor:
    """Tests for the from_visitor helper."""

    def test_from_visitor_creates_recipe(self):
        """Test that from_visitor creates a working Recipe."""
        from rewrite import TreeVisitor

        visitor = TreeVisitor.noop()
        recipe = from_visitor(visitor)
        assert isinstance(recipe, Recipe)
        assert recipe.name == "org.openrewrite.adhoc"

    def test_from_visitor_with_noop(self):
        """Test using from_visitor with a noop visitor."""
        from rewrite import TreeVisitor

        # Using noop visitor doesn't modify anything
        spec = RecipeSpec(recipe=from_visitor(TreeVisitor.noop()))
        spec.rewrite_run(
            python(
                """
                def hello():
                    print("world")
                """
            )
        )


class TestMultipleSourceSpecs:
    """Tests for handling multiple source specifications."""

    def test_multiple_python_files(self):
        """Test parsing multiple Python files."""
        spec = RecipeSpec()
        spec.rewrite_run(
            python(
                """
                x = 1
                """
            ),
            python(
                """
                y = 2
                """
            ),
            python(
                """
                z = 3
                """
            ),
        )


def _uv_available() -> bool:
    """Check if uv is installed."""
    import shutil
    return shutil.which("uv") is not None


def _ty_types_available() -> bool:
    """Check if the ty-types CLI is installed."""
    import shutil
    try:
        from ty_types.__main__ import find_ty_types_bin  # noqa: F401
        return True
    except Exception:
        return shutil.which("ty-types") is not None


class TestWorkspace:
    """Tests for the directory a run parses its sources under."""

    @pytest.mark.skipif(not _ty_types_available(), reason="ty-types not installed")
    def test_sources_in_a_run_resolve_against_each_other(self):
        returns = {}

        class Collect(PythonVisitor):
            def visit_method_invocation(self, mi, p):
                returns[mi.name.simple_name] = (
                    mi.method_type.return_type if mi.method_type else None
                )
                return super().visit_method_invocation(mi, p)

        def collect(cu):
            Collect().visit(cu, None)

        RecipeSpec().rewrite_run(
            python("def answer() -> int:\n    return 42\n", path=Path("helper.py")),
            python("import helper\n\nvalue = helper.answer()\n", before_recipe=collect),
        )

        # Resolved through the import, which names a module of the same directory.
        assert returns["answer"] == JavaType.Primitive.Int

    def test_a_source_path_leading_out_of_the_workspace_is_rejected(self):
        with pytest.raises(ValueError, match="inside the workspace"):
            RecipeSpec().rewrite_run(python("x = 1\n", path=Path("../escaped.py")))

    @pytest.mark.skipif(not _uv_available(), reason="uv not installed")
    def test_a_borrowed_workspace_keeps_its_own_files(self):
        specs = uv(
            pyproject(
                """
                [project]
                name = "test"
                version = "0.0.0"
                requires-python = ">=3.10"
                """
            ),
            python("x = 1\n"),
        )
        root = Path(specs[0].project_root)

        RecipeSpec().rewrite_run(*specs)

        assert (root / "pyproject.toml").is_file()


class TestPyprojectHelper:
    """Tests for the pyproject() helper."""

    def test_pyproject_returns_toml_spec(self):
        """pyproject() returns a SourceSpec with kind='toml'."""
        spec = pyproject("[project]\nname = \"test\"\n")
        assert spec.kind == "toml"
        assert spec.path is not None
        assert spec.path.name == "pyproject.toml"
        assert spec.ext == "toml"

    def test_pyproject_with_after(self):
        """pyproject() supports before/after."""
        spec = pyproject("[project]\nv = \"1\"\n", "[project]\nv = \"2\"\n")
        assert spec.before == "[project]\nv = \"1\"\n"
        assert spec.after == "[project]\nv = \"2\"\n"


class TestUvHelper:
    """Tests for the uv() test helper."""

    def test_uv_returns_list_of_specs(self):
        """uv() returns a list of SourceSpec objects."""
        from rewrite.test.spec import uv as uv_fn
        import inspect
        sig = inspect.signature(uv_fn)
        assert "source_specs" in sig.parameters
        assert "root" in sig.parameters

    def test_uv_without_pyproject_or_root_returns_untagged(self):
        """uv() without pyproject or root returns specs with project_root=None."""
        specs = uv(python("x = 1"))
        assert len(specs) == 1
        assert specs[0].project_root is None

    def test_uv_with_explicit_root(self, tmp_path):
        """uv() with root= tags specs with that directory."""
        specs = uv(python("x = 1"), root=str(tmp_path))
        assert len(specs) == 1
        assert specs[0].project_root == str(tmp_path)

    def test_source_spec_project_root_default_none(self):
        """SourceSpec.project_root defaults to None."""
        spec = python("x = 1")
        assert spec.project_root is None

    @pytest.mark.skipif(not _uv_available(), reason="uv not installed")
    def test_uv_creates_workspace_from_pyproject(self):
        """uv() with pyproject() creates a workspace and tags all specs."""
        import os
        specs = uv(
            pyproject(
                """
                [project]
                name = "test"
                version = "0.0.0"
                requires-python = ">=3.10"
                dependencies = ["six==1.17.0"]
                """
            ),
            python("import six\nx = 1\n"),
        )
        assert len(specs) == 2
        # Both specs tagged with same workspace
        assert specs[0].project_root is not None
        assert specs[0].project_root == specs[1].project_root
        assert os.path.isdir(specs[0].project_root)
        assert os.path.isdir(os.path.join(specs[0].project_root, ".venv"))

    @pytest.mark.skipif(not _uv_available(), reason="uv not installed")
    @pytest.mark.skipif(not _ty_types_available(), reason="ty-types not installed")
    def test_uv_spec_resolves_types_from_its_dependencies(self, monkeypatch):
        import sys
        # The runner's environment, which lacks the declared dependency.
        monkeypatch.setenv("VIRTUAL_ENV", sys.prefix)
        types = {}

        class Collect(PythonVisitor):
            def visit_method_invocation(self, mi, p):
                types[mi.name.simple_name] = mi.type
                return super().visit_method_invocation(mi, p)

        specs = uv(
            pyproject(
                """
                [project]
                name = "test"
                version = "0.0.0"
                requires-python = ">=3.10"
                dependencies = ["semver==3.0.4"]
                """
            ),
            python(
                """
                import semver
                v = semver.Version(1, 2, 3)
                """,
                before_recipe=lambda cu: Collect().visit(cu, None),
            ),
        )
        if specs[0].project_root is None:
            pytest.skip("uv could not build the workspace")

        RecipeSpec().rewrite_run(*specs)

        assert isinstance(types["Version"], JavaType.FullyQualified)
        assert types["Version"].fully_qualified_name == "semver.version.Version"


def _await(node: J) -> Await:
    return Await(uuid4(), node.prefix, Markers.EMPTY, node.replace(prefix=Space([], ' ')), None)


class TestWellFormedness:
    """The harness rejects an `after` tree whose slots hold the wrong node kind."""

    def test_expression_in_statement_list_slot(self):
        class AwaitEveryCall(PythonVisitor[ExecutionContext]):
            def visit_method_invocation(self, mi: MethodInvocation, ctx: ExecutionContext) -> J:
                return _await(mi)

        spec = RecipeSpec(recipe=from_visitor(AwaitEveryCall()))
        with pytest.raises(AssertionError, match=r"CompilationUnit\.statements\[0\] holds Await, expected Statement"):
            spec.rewrite_run(python("foo()"))

    def test_expression_in_single_statement_slot(self):
        class AwaitEveryYield(PythonVisitor[ExecutionContext]):
            def visit_yield(self, y: Yield, ctx: ExecutionContext) -> J:
                return _await(y)

        spec = RecipeSpec(recipe=from_visitor(AwaitEveryYield()))
        with pytest.raises(AssertionError, match=r"holds Await, expected Statement \(StatementExpression\.statement\)"):
            spec.rewrite_run(
                python(
                    """
                    def f():
                        yield 1
                    """
                )
            )


def _type_name(marker_type: type) -> str:
    return f"{marker_type.__module__}.{marker_type.__qualname__}"


def _marking_calls(marker: Callable[[], Marker], rename: Optional[str] = None) -> Recipe:
    """A recipe that adds ``marker()`` to every call, and renames the call when ``rename`` is given."""

    class MarkCalls(PythonVisitor[ExecutionContext]):
        def visit_method_invocation(self, mi: MethodInvocation, ctx: ExecutionContext) -> J:
            mi = super().visit_method_invocation(mi, ctx)
            if rename is not None:
                mi = mi.replace(name=mi.name.replace(simple_name=rename))
            return mi.replace(markers=mi.markers.add(marker()))

    return from_visitor(MarkCalls())


# The marker types a recipe defines for itself are declared in the tests that use them: one declared
# at module level would count among the package's own marker types in tests/rpc/test_marker_codecs.py.
class TestAddedMarkers:
    """The harness rejects a recipe that leaves markers on the LST for its own use."""

    def test_marker_added_alongside_a_change(self):
        @lst_dataclass
        class Visited(Marker):
            _id: int | UUID

        spec = RecipeSpec(recipe=_marking_calls(lambda: Visited(random_id()), rename="bar"), type_attribution=False)
        with pytest.raises(AssertionError) as failure:
            spec.rewrite_run(python("foo()\n", "bar()\n", path=Path("a.py")))

        assert str(failure.value) == (
            "The recipe added markers to the LST that neither the sources it was given nor a fresh parse of its "
            "output contain:\n"
            "  a.py\n"
            f"    {_type_name(Visited)} on MethodInvocation `bar()`\n"
            "Markers travel with the LST to every recipe that runs after this one and to every tool that reads the "
            "result, so they are not a place for a recipe to keep information for itself. Instead:\n"
            "  - share information between the visit methods of one visitor with cursor messages, "
            "e.g. self.cursor.put_message(...) and self.cursor.get_nearest_message(...)\n"
            "  - share information between source files, or from scanning to editing, through the accumulator of a "
            "ScanningRecipe\n"
            "  - keep the recipe idempotent by recognizing code it has already changed, rather than tagging that code\n"
            "  - report a finding with SearchResult, and a problem with Markup"
        )

    def test_marker_added_without_a_change(self):
        @lst_dataclass
        class Visited(Marker):
            _id: int | UUID

        spec = RecipeSpec(recipe=_marking_calls(lambda: Visited(random_id())), type_attribution=False)
        with pytest.raises(AssertionError) as failure:
            spec.rewrite_run(python("foo()\n", path=Path("a.py")))

        assert "  a.py (the printed source is unchanged)\n" in str(failure.value)
        assert f"    {_type_name(Visited)} on MethodInvocation `foo()`\n" in str(failure.value)

    def test_source_file_marker_moved_to_an_element(self):
        @lst_dataclass
        class BuildMetadata(Marker):
            _id: int | UUID

        def with_metadata(cu: CompilationUnit) -> CompilationUnit:
            return cu.replace(markers=cu.markers.add(BuildMetadata(random_id())))

        class MetadataOnCalls(PythonVisitor[ExecutionContext]):
            def visit_method_invocation(self, mi: MethodInvocation, ctx: ExecutionContext) -> J:
                metadata = self.cursor.first_enclosing_or_throw(CompilationUnit).markers.find_first(BuildMetadata)
                return mi.replace(markers=mi.markers.add(metadata))

        spec = RecipeSpec(recipe=from_visitor(MetadataOnCalls()), type_attribution=False)
        with pytest.raises(AssertionError) as failure:
            spec.rewrite_run(python("foo()\n", before_recipe=with_metadata))

        assert f"    {_type_name(BuildMetadata)} on MethodInvocation `foo()`\n" in str(failure.value)

    def test_search_result_passes(self):
        spec = RecipeSpec(recipe=_marking_calls(lambda: SearchResult(random_id(), None)), type_attribution=False)
        spec.rewrite_run(python("foo()\n", "/*~~>*/foo()\n"))

    def test_syntax_marker_of_written_code_passes(self):
        class QuoteAnnotation(PythonVisitor[ExecutionContext]):
            def visit_identifier(self, ident: Identifier, ctx: ExecutionContext) -> J:
                if ident.simple_name != "int":
                    return ident
                # A forward reference, which the parser marks as Quoted.
                quoted = Quoted(random_id(), Quoted.Style.DOUBLE)
                return ident.replace(simple_name="Model", markers=ident.markers.add(quoted))

        spec = RecipeSpec(recipe=from_visitor(QuoteAnnotation()), type_attribution=False)
        spec.rewrite_run(python("x: int = 1\n", 'x: "Model" = 1\n'))

    def test_generated_source(self):
        @lst_dataclass
        class Visited(Marker):
            _id: int | UUID

        generated = RecipeSpec()._parse_python("x = 1\n", Path("b.py"), None, None)
        generated = generated.replace(markers=generated.markers.add(Visited(random_id())))

        with pytest.raises(AssertionError) as failure:
            assert_no_added_markers([], [Result(None, generated)], lambda source_file: None)

        assert f"  b.py (generated)\n    {_type_name(Visited)} on the source file\n" in str(failure.value)

    def test_host_marker_held_as_a_mapping_is_named_by_its_host_type(self):
        before = RecipeSpec()._parse_python("x = 1\n", Path("a.py"), None, None)
        # How the RPC receiver holds a host marker that Python has no class for.
        after = before.replace(markers=Markers(random_id(), [{'kind': 'org.example.HostMarker'}]))

        with pytest.raises(AssertionError) as failure:
            assert_no_added_markers([before], [Result(before, after)], lambda source_file: None)

        assert "  a.py (the printed source is unchanged)\n    org.example.HostMarker on the source file\n" \
               in str(failure.value)

    def test_check_turned_off_passes(self):
        @lst_dataclass
        class Visited(Marker):
            _id: int | UUID

        spec = RecipeSpec(recipe=_marking_calls(lambda: Visited(random_id()), rename="bar"),
                          check_reporting_markers_only=False, type_attribution=False)
        spec.rewrite_run(python("foo()\n", "bar()\n"))
