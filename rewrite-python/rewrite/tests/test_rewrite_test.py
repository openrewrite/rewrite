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
from types import SimpleNamespace
from typing import Any, Callable, Dict, List, Optional, Set
from uuid import UUID, uuid4

import pytest

from dataclasses import dataclass, field

from rewrite import (Cursor, ExecutionContext, Heavyweight, InMemoryExecutionContext, Recipe, RecipeRunException,
                     ScanningRecipe, SourceFile, TreeVisitor, option, random_id)
from rewrite.data_table import DATA_TABLE_STORE, DataTable, column
from rewrite.execution import Result
from rewrite.markers import Marker, Markers, SearchResult
from rewrite.test import RecipeSpec, python, pyproject, uv, from_visitor, dedent
from rewrite.test import probes
from rewrite.test.added_markers import assert_no_added_markers
from rewrite.test.spec import SourceSpec
from rewrite.python.markers import PythonResolutionResult, Quoted
from rewrite.python.template import capture, template
from rewrite.python.visitor import PythonVisitor
from rewrite.java.tree import Assignment, ClassDeclaration, Identifier, Literal, MethodInvocation, Try, Yield
from rewrite.python.format import AutoFormat, auto_format, maybe_auto_format
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
                if mi.name.simple_name == rename:
                    return mi
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

    def test_output_that_does_not_parse_is_left_to_the_check_that_names_it(self):
        with_lock = template('with lock:\n    {stmt}', stmt=capture('stmt'))

        class LockOneColumnOut(PythonVisitor[ExecutionContext]):
            def visit_method_invocation(self, mi: MethodInvocation, ctx: ExecutionContext) -> J:
                if mi.name.simple_name != "a" or self.cursor.first_enclosing(Try) is not None:
                    return mi
                # The markers the template's parse gave the `with` are explained only by a fresh parse.
                locked = with_lock.apply(self.cursor, values={'stmt': mi})
                return locked.replace(prefix=Space([], "\n   "))

        spec = RecipeSpec(recipe=from_visitor(LockOneColumnOut()), type_attribution=False)
        with pytest.raises(AssertionError) as failure:
            spec.rewrite_run(python(
                "def f():\n    a()\n    b()\n",
                "def f():\n   with lock:\n        a()\n    b()\n",
            ))

        assert "The recipe added markers" not in str(failure.value)
        assert "    the text it prints doesn't parse: unindent does not match any outer indentation level on " \
               "line 4, `b()`\n" in str(failure.value)

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


class TestGeneratedSources:
    """A source spec with no `before` describes a file the recipe creates."""

    COMPAT = 'def helper():\n    return None\n'

    @staticmethod
    def _recipe(file_name="pkg/_helper.py", contents=None, again=False):
        contents = contents if contents is not None else TestGeneratedSources.COMPAT

        class Generates(ScanningRecipe[Set[str]]):
            @property
            def name(self) -> str:
                return "org.openrewrite.test.Generates"

            @property
            def display_name(self) -> str:
                return "Generate a module"

            @property
            def description(self) -> str:
                return "Generate a module."

            def initial_value(self, ctx: ExecutionContext) -> Set[str]:
                return set()

            def scanner(self, acc: Set[str]) -> TreeVisitor:
                class Scan(PythonVisitor[ExecutionContext]):
                    def visit_compilation_unit(self, cu: CompilationUnit, ctx) -> J:
                        acc.add(str(cu.source_path))
                        return cu

                return Scan()

            def generate(self, acc: Set[str], ctx: ExecutionContext) -> List[SourceFile]:
                if not acc or (file_name in acc and not again):
                    return []
                import ast

                from rewrite.python._parser_visitor import ParserVisitor

                cu = ParserVisitor(contents, None, None).visit_Module(ast.parse(contents))
                return [cu.replace(_source_path=Path(file_name))]

        return Generates()

    def test_matched_on_its_source_path(self):
        RecipeSpec(recipe=self._recipe()).rewrite_run(
            python("import os", path=Path("pkg/client.py")),
            python(None, self.COMPAT, path=Path("pkg/_helper.py")),
        )

    def test_matched_on_its_contents_when_the_spec_gives_no_path(self):
        RecipeSpec(recipe=self._recipe()).rewrite_run(
            python("import os", path=Path("pkg/client.py")),
            python(None, self.COMPAT),
        )

    def test_the_after_recipe_hook_sees_it(self):
        seen = []
        RecipeSpec(recipe=self._recipe()).rewrite_run(
            python("import os", path=Path("pkg/client.py")),
            python(None, self.COMPAT, path=Path("pkg/_helper.py"),
                   after_recipe=lambda sf: seen.append(str(sf.source_path))),
        )
        assert seen == ["pkg/_helper.py"]

    def test_contents_that_do_not_match(self):
        with pytest.raises(AssertionError, match="does not match expected"):
            RecipeSpec(recipe=self._recipe()).rewrite_run(
                python("import os", path=Path("pkg/client.py")),
                python(None, "def other():\n    pass\n", path=Path("pkg/_helper.py")),
            )

    def test_a_path_nothing_was_generated_at(self):
        with pytest.raises(AssertionError,
                           match="Expected the recipe to generate a source file"):
            RecipeSpec(recipe=self._recipe()).rewrite_run(
                python("import os", path=Path("pkg/client.py")),
                python(None, self.COMPAT, path=Path("elsewhere/_helper.py")),
            )

    def test_a_generated_file_no_spec_describes(self):
        with pytest.raises(AssertionError, match="did not expect"):
            RecipeSpec(recipe=self._recipe()).rewrite_run(
                python("import os", path=Path("pkg/client.py")),
            )

    def test_a_spec_with_neither_before_nor_after(self):
        with pytest.raises(AssertionError, match="describes nothing"):
            RecipeSpec(recipe=self._recipe()).rewrite_run(
                python("import os", path=Path("pkg/client.py")),
                python(None, None, path=Path("pkg/_helper.py")),
            )

    def test_empty_contents_matched_on_its_source_path(self):
        RecipeSpec(recipe=self._recipe(contents="")).rewrite_run(
            python("import os", path=Path("pkg/client.py")),
            python(None, "", path=Path("pkg/_helper.py")),
        )

    def test_empty_contents_matched_when_the_spec_gives_no_path(self):
        RecipeSpec(recipe=self._recipe(contents="")).rewrite_run(
            python("import os", path=Path("pkg/client.py")),
            python(None, ""),
        )

    def test_empty_contents_that_do_not_match(self):
        with pytest.raises(AssertionError, match="does not match expected"):
            RecipeSpec(recipe=self._recipe()).rewrite_run(
                python("import os", path=Path("pkg/client.py")),
                python(None, "", path=Path("pkg/_helper.py")),
            )

    def test_empty_contents_without_a_path_match_no_generated_file(self):
        with pytest.raises(AssertionError, match="Expected the recipe to generate a source file"):
            RecipeSpec(recipe=self._recipe()).rewrite_run(
                python("import os", path=Path("pkg/client.py")),
                python(None, ""),
            )


class TestEmptyAfter:
    """An empty `after` expects the recipe to leave the file empty."""

    def test_an_empty_after_that_does_not_match(self):
        with pytest.raises(AssertionError, match="does not match expected"):
            RecipeSpec(recipe=_renaming_one_step("a", "b"), type_attribution=False).rewrite_run(
                python("a = 1\n", ""),
            )


def _renaming_one_step(*names: str) -> Recipe:
    """A recipe that renames each name in ``names`` to the one after it, a single step per run."""

    class RenameOneStep(PythonVisitor[ExecutionContext]):
        def visit_identifier(self, ident: Identifier, ctx: ExecutionContext) -> J:
            if ident.simple_name in names[:-1]:
                return ident.replace(simple_name=names[names.index(ident.simple_name) + 1])
            return ident

    return from_visitor(RenameOneStep())


@dataclass
class _FoundName:
    name: str = field(metadata=column("Name", "The name found"))


_FOUND_NAMES = DataTable[_FoundName]("org.openrewrite.test.FoundNames", "Found names", "Names found.", _FoundName)


def _finding_names(mark: bool = False) -> Recipe:
    class FindNames(PythonVisitor[ExecutionContext]):
        def visit_identifier(self, ident: Identifier, ctx: ExecutionContext) -> J:
            _FOUND_NAMES.insert_row(ctx, _FoundName(ident.simple_name))
            return ident.replace(markers=ident.markers.add(SearchResult(random_id(), None))) if mark else ident

    return from_visitor(FindNames())


class TestDataTables:
    """A spec asserts on the rows a recipe writes to its data tables."""

    def test_the_assertion_receives_the_rows_of_the_table(self):
        seen: List[List[_FoundName]] = []
        spec = RecipeSpec(recipe=_finding_names(), type_attribution=False).data_table(_FOUND_NAMES, seen.append)
        spec.rewrite_run(python("a = b\n"))
        assert seen == [[_FoundName("a"), _FoundName("b")]]

    def test_a_table_is_found_by_its_name(self):
        seen: List[List[_FoundName]] = []
        spec = RecipeSpec(recipe=_finding_names(), type_attribution=False).data_table(
            "org.openrewrite.test.FoundNames", seen.append)
        spec.rewrite_run(python("a = 1\n"))
        assert seen == [[_FoundName("a")]]

    def test_only_the_rows_of_the_first_cycle_are_asserted_on(self):
        seen: List[List[_FoundName]] = []
        spec = RecipeSpec(recipe=_finding_names(mark=True), type_attribution=False).data_table(
            _FOUND_NAMES, seen.append)
        spec.rewrite_run(python("a = 1\n", "/*~~>*/a = 1\n"))
        assert seen == [[_FoundName("a")]]

    def test_each_run_asserts_on_its_own_rows(self):
        seen: List[List[_FoundName]] = []
        spec = RecipeSpec(recipe=_finding_names(), type_attribution=False).data_table(_FOUND_NAMES, seen.append)
        spec.rewrite_run(python("a = 1\n"))
        spec.rewrite_run(python("b = 1\n"))
        assert seen == [[_FoundName("a")], [_FoundName("b")]]

    def test_a_failing_assertion_fails_the_run(self):
        def expect_b(rows: List[_FoundName]) -> None:
            assert rows == [_FoundName("b")]

        spec = RecipeSpec(recipe=_finding_names(), type_attribution=False).data_table(_FOUND_NAMES, expect_b)
        with pytest.raises(AssertionError, match="_FoundName\\(name='a'\\)"):
            spec.rewrite_run(python("a = 1\n"))

    def test_a_table_the_recipe_did_not_write_fails_the_run(self):
        spec = RecipeSpec(type_attribution=False).data_table(_FOUND_NAMES, lambda rows: None)
        with pytest.raises(AssertionError, match="^No data table named org.openrewrite.test.FoundNames$"):
            spec.rewrite_run(python("a = 1\n"))

    def test_a_missing_table_lists_the_tables_the_recipe_wrote(self):
        spec = RecipeSpec(recipe=_finding_names(), type_attribution=False).data_table(
            "org.openrewrite.test.Missing", lambda rows: None)
        with pytest.raises(AssertionError) as failure:
            spec.rewrite_run(python("a = b\n"))
        assert str(failure.value) == (
            "No data table named org.openrewrite.test.Missing\n"
            "Found data tables: org.openrewrite.test.FoundNames"
        )

    def test_adding_an_assertion_leaves_the_original_spec_alone(self):
        spec = RecipeSpec(recipe=_finding_names(), type_attribution=False)
        spec.data_table(_FOUND_NAMES, lambda rows: None)
        assert spec.data_table_assertions == []

    def test_every_assertion_on_a_table_runs(self):
        first: List[List[_FoundName]] = []
        second: List[List[_FoundName]] = []
        spec = (RecipeSpec(recipe=_finding_names(), type_attribution=False)
                .data_table(_FOUND_NAMES, first.append)
                .data_table("org.openrewrite.test.FoundNames", second.append))
        spec.rewrite_run(python("a = 1\n"))
        assert first == second == [[_FoundName("a")]]


class TestExtraCycles:
    """The harness runs the recipe again on its own output, which has to change nothing."""

    def test_a_recipe_that_changes_its_expected_result_again_fails(self):
        spec = RecipeSpec(recipe=_renaming_one_step("a", "b", "c"), type_attribution=False)
        with pytest.raises(AssertionError) as failure:
            spec.rewrite_run(python("a = 1\n", "b = 1\n", path=Path("a.py")))

        assert str(failure.value).startswith(
            "Expected recipe to complete in 1 cycle, but cycle 2 changed the sources again:\n"
            "  a.py\n"
            "Expected:\nb = 1\n\n\nActual:\nc = 1\n\n"
        )

    def test_expected_cycles_that_make_changes_compares_the_result_of_the_last_expected_cycle(self):
        spec = RecipeSpec(recipe=_renaming_one_step("a", "b", "c"), expected_cycles_that_make_changes=2,
                          type_attribution=False)
        spec.rewrite_run(python("a = 1\n", "c = 1\n"))

    def test_expected_cycles_that_make_changes_fails_a_recipe_that_settles_sooner(self):
        spec = RecipeSpec(recipe=_renaming_one_step("a", "b", "c"), expected_cycles_that_make_changes=2,
                          type_attribution=False)
        with pytest.raises(AssertionError,
                           match="Expected recipe to complete in 2 cycles, but it settled after 1 cycle."):
            spec.rewrite_run(python("b = 1\n", "c = 1\n"))

    def test_expected_cycles_that_make_changes_0_fails_a_recipe_that_changes(self):
        spec = RecipeSpec(recipe=_renaming_one_step("a", "b"), expected_cycles_that_make_changes=0,
                          type_attribution=False)
        with pytest.raises(AssertionError,
                           match="Expected recipe to complete in 0 cycles, but cycle 1 changed"):
            spec.rewrite_run(python("a = 1\n"))

    def test_fewer_cycles_than_expected_to_make_changes_is_rejected(self):
        spec = RecipeSpec(recipe=_renaming_one_step("a", "b", "c"), cycles=1, expected_cycles_that_make_changes=2,
                          type_attribution=False)
        with pytest.raises(ValueError, match="cycles=1 has to be at least 1 and at least"):
            spec.rewrite_run(python("a = 1\n", "c = 1\n"))

    def test_cycles_1_turns_the_check_off(self):
        spec = RecipeSpec(recipe=_renaming_one_step("a", "b", "c"), cycles=1, type_attribution=False)
        spec.rewrite_run(python("a = 1\n", "b = 1\n"))

    def test_a_recipe_that_changes_the_lst_again_without_changing_the_text_fails(self):
        class Upper(PythonVisitor[ExecutionContext]):
            def visit_identifier(self, ident: Identifier, ctx: ExecutionContext) -> J:
                return ident.replace(id=random_id(), simple_name=ident.simple_name.upper())

        spec = RecipeSpec(recipe=from_visitor(Upper()), type_attribution=False)
        with pytest.raises(AssertionError,
                           match="  a.py, whose LST it changed without changing the printed source"):
            spec.rewrite_run(python("a = 1\n", "A = 1\n", path=Path("a.py")))

    def test_a_recipe_that_generates_a_file_again_fails(self):
        with pytest.raises(AssertionError, match="  pkg/_helper.py, which it generated"):
            RecipeSpec(recipe=TestGeneratedSources._recipe(again=True)).rewrite_run(
                python("import os", path=Path("pkg/client.py")),
                python(None, TestGeneratedSources.COMPAT, path=Path("pkg/_helper.py")),
            )

    def test_data_tables_are_written_by_the_first_cycle_only(self):
        ctx = InMemoryExecutionContext()
        spec = RecipeSpec(recipe=_finding_names(mark=True), execution_context=ctx, type_attribution=False)
        spec.rewrite_run(python("a = 1\n", "/*~~>*/a = 1\n"))

        assert len(list(ctx.get_message(DATA_TABLE_STORE).get_rows(_FOUND_NAMES.name))) == 1

    def test_a_recipe_that_recognizes_its_output_only_by_the_shape_of_its_lst_fails_on_a_fresh_parse(self):
        class QualifyCalls(PythonVisitor[ExecutionContext]):
            def visit_method_invocation(self, mi: MethodInvocation, ctx: ExecutionContext) -> J:
                if "." in mi.name.simple_name:
                    return mi
                return mi.replace(name=mi.name.replace(simple_name="os." + mi.name.simple_name))

        # The parser makes a field access of the name the recipe writes, which the shape check would report first.
        spec = RecipeSpec(recipe=from_visitor(QualifyCalls()), check_parser_shaped_trees=False, type_attribution=False)
        with pytest.raises(AssertionError) as failure:
            spec.rewrite_run(python("foo()\n", "os.foo()\n", path=Path("a.py")))

        assert str(failure.value).startswith(
            "Running the recipe on a fresh parse of its output changed the sources again:\n"
            "  a.py\n"
            "Expected:\nos.foo()\n\n\nActual:\nos.os.foo()\n\n"
        )

    def test_a_fresh_parse_keeps_what_the_source_file_carries_from_outside_its_text(self):
        @lst_dataclass
        class Loud(Marker):
            _id: int | UUID

        def loud(cu: CompilationUnit) -> CompilationUnit:
            return cu.replace(markers=cu.markers.add(Loud(random_id())))

        class Case(PythonVisitor[ExecutionContext]):
            def visit_identifier(self, ident: Identifier, ctx: ExecutionContext) -> J:
                is_loud = self.cursor.first_enclosing_or_throw(CompilationUnit).markers.find_first(Loud) is not None
                name = ident.simple_name
                return ident.replace(simple_name=name.upper() if is_loud else name.lower())

        spec = RecipeSpec(recipe=from_visitor(Case()), type_attribution=False)
        spec.rewrite_run(python("hi = 1\n", "HI = 1\n", before_recipe=loud))

    @pytest.mark.skipif(not _ty_types_available(), reason="ty-types not installed")
    def test_a_fresh_parse_is_type_attributed(self):
        typed: List[bool] = []

        class RenameToPrint(PythonVisitor[ExecutionContext]):
            def visit_method_invocation(self, mi: MethodInvocation, ctx: ExecutionContext) -> J:
                if mi.name.simple_name == "foo":
                    return mi.replace(name=mi.name.replace(simple_name="print"))
                method_type = mi.method_type
                typed.append(method_type is not None and not isinstance(method_type.declaring_type, JavaType.Unknown))
                return mi

        RecipeSpec(recipe=from_visitor(RenameToPrint())).rewrite_run(python("foo()\n", "print()\n"))

        assert typed == [False, True]


def _remembering_names(rename: bool = False) -> Recipe:
    """A recipe that keeps the last name it saw on the execution context, and renames ``a`` when ``rename``."""

    class RememberNames(PythonVisitor[ExecutionContext]):
        def visit_identifier(self, ident: Identifier, ctx: ExecutionContext) -> J:
            ctx.put_message("org.openrewrite.test.lastName", ident.simple_name)
            return ident.replace(simple_name="b") if rename and ident.simple_name == "a" else ident

    return from_visitor(RememberNames())


class TestExecutionContextUnchanged:
    """The harness rejects a recipe that keeps state on the execution context."""

    def test_a_recipe_that_puts_a_message_fails(self):
        spec = RecipeSpec(recipe=_remembering_names(rename=True), type_attribution=False)
        with pytest.raises(AssertionError) as failure:
            spec.rewrite_run(python("a = 1\n", "b = 1\n"))

        message = str(failure.value)
        assert message.startswith(
            "Keep state that spans source files in a ScanningRecipe's accumulator, and state within one source file "
            "in cursor messages, e.g. self.cursor.put_message(...) and self.cursor.get_nearest_message(...).\n"
            "org.openrewrite.adhoc put messages on the execution context, which every recipe and source file in a run "
            "shares, so what it keeps there leaks into the others:\n"
            '  "org.openrewrite.test.lastName" at RememberNames.visit_identifier (test_rewrite_test.py:'
        )
        assert message.endswith(
            "A recipe that must change the execution context opts out with "
            "RecipeSpec(check_execution_context_unchanged=False)."
        )

    def test_a_put_made_while_scanning_fails(self):
        class CountFiles(ScanningRecipe[List[str]]):
            @property
            def name(self) -> str:
                return "org.openrewrite.test.CountFiles"

            @property
            def display_name(self) -> str:
                return "Count files"

            @property
            def description(self) -> str:
                return "Count files."

            def initial_value(self, ctx: ExecutionContext) -> List[str]:
                ctx.put_message("org.openrewrite.test.counted", True)
                return []

        spec = RecipeSpec(recipe=CountFiles(), type_attribution=False)
        with pytest.raises(AssertionError,
                           match='  "org.openrewrite.test.counted" at CountFiles.initial_value'):
            spec.rewrite_run(python("a = 1\n"))

    def test_data_tables_and_cursor_messages_pass(self):
        class NoteNames(PythonVisitor[ExecutionContext]):
            def visit_identifier(self, ident: Identifier, ctx: ExecutionContext) -> J:
                self.cursor.put_message("name", ident.simple_name)
                _FOUND_NAMES.insert_row(ctx, _FoundName(ident.simple_name))
                return ident.replace(markers=ident.markers.add(SearchResult(random_id(), None)))

        spec = RecipeSpec(recipe=from_visitor(NoteNames()), type_attribution=False)
        spec.rewrite_run(python("a = 1\n", "/*~~>*/a = 1\n"))

    def test_check_turned_off_passes(self):
        spec = RecipeSpec(recipe=_remembering_names(rename=True), check_execution_context_unchanged=False,
                          type_attribution=False)
        spec.rewrite_run(python("a = 1\n", "b = 1\n"))


class _RenameA(PythonVisitor[ExecutionContext]):
    def visit_identifier(self, ident: Identifier, ctx: ExecutionContext) -> J:
        return ident.replace(simple_name="b") if ident.simple_name == "a" else ident


def _renaming_scanned_files(scanner_edits: bool) -> Recipe:
    """A recipe that renames ``a`` to ``b`` in the files its scanner found an ``a`` in, and whose scanner renames
    it too when ``scanner_edits``."""

    class RenameScanned(ScanningRecipe[Set[str]]):
        @property
        def name(self) -> str:
            return "org.openrewrite.test.RenameScanned"

        @property
        def display_name(self) -> str:
            return "Rename scanned files"

        @property
        def description(self) -> str:
            return "Rename `a` in the files the scanner found it in."

        def initial_value(self, ctx: ExecutionContext) -> Set[str]:
            return set()

        def scanner(self, acc: Set[str]) -> TreeVisitor:
            if scanner_edits:
                return _RenameA()

            class Find(PythonVisitor[ExecutionContext]):
                def visit_identifier(self, ident: Identifier, ctx: ExecutionContext) -> J:
                    if ident.simple_name == "a":
                        acc.add(str(self.cursor.first_enclosing_or_throw(CompilationUnit).source_path))
                    return ident

            return Find()

        def editor_with_data(self, acc: Set[str]) -> TreeVisitor:
            class Rename(_RenameA):
                def visit_compilation_unit(self, cu: CompilationUnit, ctx: ExecutionContext) -> J:
                    return super().visit_compilation_unit(cu, ctx) if str(cu.source_path) in acc else cu

            return Rename()

    return RenameScanned()


class TestNoEditsInScanningPhase:
    """The harness rejects a ScanningRecipe whose scanner edits what it scans."""

    def test_a_scanner_that_edits_fails(self):
        spec = RecipeSpec(recipe=_renaming_scanned_files(scanner_edits=True), type_attribution=False)
        with pytest.raises(AssertionError) as failure:
            spec.rewrite_run(python("a = 1\n", "b = 1\n", path=Path("a.py")))

        assert str(failure.value) == (
            "Record what the scanner finds in the accumulator, and make the edits in editor_with_data(acc).\n"
            "org.openrewrite.test.RenameScanned's scanner changed a.py, and the scanning phase discards edits. "
            "A scanner that reuses an editing visitor opts out with "
            "RecipeSpec(check_no_edits_in_scanning_phase=False)."
        )

    def test_a_scanner_that_records_what_it_finds_passes(self):
        spec = RecipeSpec(recipe=_renaming_scanned_files(scanner_edits=False), type_attribution=False)
        spec.rewrite_run(python("a = 1\n", "b = 1\n"), python("c = 1\n"))

    def test_check_turned_off_passes(self):
        spec = RecipeSpec(recipe=_renaming_scanned_files(scanner_edits=True), check_no_edits_in_scanning_phase=False,
                          type_attribution=False)
        spec.rewrite_run(python("a = 1\n"))


def _keeping(initial: Callable[[], Any], scanned: Optional[Callable[[Any, CompilationUnit, Cursor], None]] = None,
             edited: Optional[Callable[[Any, CompilationUnit], None]] = None) -> Recipe:
    """A recipe whose accumulator starts as ``initial()``, and that ``scanned`` and ``edited`` add to."""

    class Keeps(ScanningRecipe[Any]):
        @property
        def name(self) -> str:
            return "org.openrewrite.test.Keeps"

        @property
        def display_name(self) -> str:
            return "Keep things"

        @property
        def description(self) -> str:
            return "Keep things in the accumulator."

        def initial_value(self, ctx: ExecutionContext) -> Any:
            return initial()

        def scanner(self, acc: Any) -> TreeVisitor:
            class Scan(PythonVisitor[ExecutionContext]):
                def visit_compilation_unit(self, cu: CompilationUnit, ctx: ExecutionContext) -> J:
                    if scanned is not None:
                        scanned(acc, cu, self.cursor)
                    return cu

            return Scan()

        def editor_with_data(self, acc: Any) -> TreeVisitor:
            class Edit(PythonVisitor[ExecutionContext]):
                def visit_compilation_unit(self, cu: CompilationUnit, ctx: ExecutionContext) -> J:
                    if edited is not None:
                        edited(acc, cu)
                    return cu

            return Edit()

    return Keeps()


class TestLightweightAccumulators:
    """The harness rejects a ScanningRecipe whose accumulator keeps trees and what reaches them."""

    def test_a_source_file_kept_while_scanning_fails(self):
        @dataclass
        class Scanned:
            source_files: Dict[str, SourceFile] = field(default_factory=dict)

        def keep(acc: Scanned, cu: CompilationUnit, cursor: Cursor) -> None:
            acc.source_files[str(cu.source_path)] = cu

        spec = RecipeSpec(recipe=_keeping(Scanned, keep), type_attribution=False)
        with pytest.raises(AssertionError) as failure:
            spec.rewrite_run(python("a = 1\n", path=Path("a.py")))

        assert str(failure.value) == (
            "A ScanningRecipe's accumulator should keep the values the recipe needs, such as source paths, names and "
            "ids, and no Heavyweight objects, such as trees (a CompilationUnit or any node in it), cursors, visitors, "
            "execution contexts, PythonResolutionResult markers and their resolved dependencies:\n"
            "  org.openrewrite.test.Keeps keeps\n"
            "    Scanned.source_files['a.py'] -> CompilationUnit a.py, after scanning\n"
            "An accumulator stays in memory from the first source file scanned until the cycle ends, so what it keeps "
            "for each source file adds up over a large repository, and a PythonResolutionResult or a resolved "
            "dependency reaches the dependencies of a whole project. A recipe that must keep a Heavyweight object "
            "opts out with RecipeSpec(check_lightweight_accumulators=False)."
        )

    def test_a_cursor_kept_fails(self):
        spec = RecipeSpec(recipe=_keeping(list, lambda acc, cu, cursor: acc.append(cursor)), type_attribution=False)
        with pytest.raises(AssertionError, match=r"    list\[0\] -> Cursor, after scanning\n"):
            spec.rewrite_run(python("a = 1\n"))

    def test_a_visitor_and_an_execution_context_kept_fail(self):
        def keep(acc: List[Any], cu: CompilationUnit, cursor: Cursor) -> None:
            acc.extend([_RenameA(), InMemoryExecutionContext()])

        spec = RecipeSpec(recipe=_keeping(list, keep), type_attribution=False)
        with pytest.raises(AssertionError, match=r"    list\[0\] -> TreeVisitor, after scanning\n"
                                                 r"    list\[1\] -> ExecutionContext, after scanning\n"):
            spec.rewrite_run(python("a = 1\n"))

    def test_a_type_a_recipe_library_tags_heavyweight_fails(self):
        @lst_dataclass
        class Classpath(Marker, Heavyweight):
            _id: int | UUID
            _jars: List[str]

        def with_classpath(cu: CompilationUnit) -> CompilationUnit:
            return cu.replace(markers=cu.markers.add(Classpath(random_id(), ["a.jar"])))

        def keep(acc: Dict[Any, Any], cu: CompilationUnit, cursor: Cursor) -> None:
            acc["classpath"] = cu.markers.find_first(Classpath)

        spec = RecipeSpec(recipe=_keeping(dict, keep), type_attribution=False)
        with pytest.raises(AssertionError, match=r"    dict\['classpath'\] -> Classpath, after scanning\n"):
            spec.rewrite_run(python("a = 1\n", before_recipe=with_classpath))

    def test_a_source_file_kept_while_editing_fails(self):
        spec = RecipeSpec(recipe=_keeping(list, edited=lambda acc, cu: acc.append(cu)), type_attribution=False)
        with pytest.raises(AssertionError, match=r"    list\[0\] -> CompilationUnit a.py, after editing\n"):
            spec.rewrite_run(python("a = 1\n", path=Path("a.py")))

    def test_a_tree_kept_in_a_slot_fails(self):
        class Index:
            __slots__ = ("_statements",)

            def __init__(self) -> None:
                self._statements: List[J] = []

        def keep(acc: Index, cu: CompilationUnit, cursor: Cursor) -> None:
            acc._statements.extend(cu.statements)

        spec = RecipeSpec(recipe=_keeping(Index, keep), type_attribution=False)
        with pytest.raises(AssertionError, match=r"    Index._statements\[0\] -> Assignment, after scanning\n"):
            spec.rewrite_run(python("a = 1\n"))

    def test_a_collection_of_the_recipes_own_is_read_without_running_its_code(self):
        class Index(dict):
            def items(self):
                raise AssertionError("the walk ran the accumulator's own code")

            def __iter__(self):
                raise AssertionError("the walk ran the accumulator's own code")

        def keep(acc: Index, cu: CompilationUnit, cursor: Cursor) -> None:
            acc[str(cu.source_path)] = cu

        spec = RecipeSpec(recipe=_keeping(Index, keep), type_attribution=False)
        with pytest.raises(AssertionError, match=r"    Index\['a.py'\] -> CompilationUnit a.py, after scanning\n"):
            spec.rewrite_run(python("a = 1\n", path=Path("a.py")))

    def test_a_python_resolution_result_kept_fails(self):
        resolution = PythonResolutionResult(random_id(), "demo", "1.0", None, None, "pyproject.toml", None, None,
                                            [], [], {}, {}, [], [], [], None, None)

        def with_resolution(cu: CompilationUnit) -> CompilationUnit:
            return cu.replace(markers=cu.markers.add(resolution))

        def keep(acc: Dict[Any, Any], cu: CompilationUnit, cursor: Cursor) -> None:
            acc[cu.source_path] = cu.markers.find_first(PythonResolutionResult)

        spec = RecipeSpec(recipe=_keeping(dict, keep), type_attribution=False)
        with pytest.raises(AssertionError, match=r"    dict\[a.py\] -> PythonResolutionResult, after scanning\n"):
            spec.rewrite_run(python("a = 1\n", path=Path("a.py"), before_recipe=with_resolution))

    def test_a_resolved_dependency_kept_fails(self):
        requests = PythonResolutionResult.ResolvedDependency("requests", "2.32.3", None, None)
        resolution = PythonResolutionResult(random_id(), "demo", "1.0", None, None, "pyproject.toml", None, None,
                                            [], [], {}, {}, [], [], [requests], None, None)

        def with_resolution(cu: CompilationUnit) -> CompilationUnit:
            return cu.replace(markers=cu.markers.add(resolution))

        def keep(acc: Dict[Any, Any], cu: CompilationUnit, cursor: Cursor) -> None:
            acc["resolved"] = cu.markers.find_first(PythonResolutionResult).resolved_dependencies

        spec = RecipeSpec(recipe=_keeping(dict, keep), type_attribution=False)
        with pytest.raises(AssertionError, match=r"    dict\['resolved'\]\[0\] -> "
                                                 r"PythonResolutionResult.ResolvedDependency, after scanning\n"):
            spec.rewrite_run(python("a = 1\n", before_recipe=with_resolution))

    def test_values_pass(self):
        def keep(acc: Dict[Any, Any], cu: CompilationUnit, cursor: Cursor) -> None:
            statement = cu.statements[0]
            acc[(str(cu.source_path), cu.id)] = {
                "path": cu.source_path,
                "names": [type(statement).__name__],
                "marker": SearchResult(random_id(), "found"),
                "type": JavaType.Primitive.Int,
            }

        spec = RecipeSpec(recipe=_keeping(dict, keep), type_attribution=False)
        spec.rewrite_run(python("a = 1\n"))

    def test_check_turned_off_passes(self):
        spec = RecipeSpec(recipe=_keeping(list, lambda acc, cu, cursor: acc.append(cu)),
                          check_lightweight_accumulators=False, type_attribution=False)
        spec.rewrite_run(python("a = 1\n"))


# The recipes the harness probes are declared at module level, since it doesn't probe one defined in a function.
@dataclass
class _RenameFirstAssignment(Recipe):
    """Renames the variable the first statement of a file assigns, checking that it is an assignment when ``checked``."""

    checked: bool = field(default=False, metadata=option("Checked", "Check that the statement is an assignment."))

    @property
    def name(self) -> str:
        return "org.openrewrite.test.RenameFirstAssignment"

    @property
    def display_name(self) -> str:
        return "Rename the first assignment"

    @property
    def description(self) -> str:
        return "Rename the variable the first statement assigns."

    def editor(self) -> TreeVisitor:
        checked = self.checked

        class Rename(PythonVisitor[ExecutionContext]):
            target: Optional[UUID] = None

            def visit_compilation_unit(self, cu: CompilationUnit, ctx: ExecutionContext) -> J:
                first = cu.statements[0]
                if checked and not isinstance(first, Assignment):
                    return cu
                self.target = first.variable.id
                return super().visit_compilation_unit(cu, ctx)

            def visit_identifier(self, ident: Identifier, ctx: ExecutionContext) -> J:
                if ident.id == self.target and ident.simple_name != "renamed":
                    return ident.replace(simple_name="renamed")
                return ident

        return Rename()


class _RequiresMarker(Recipe):
    """Renames ``a`` to ``b`` in a file that carries a marker of ``marker_type``, and raises in one that doesn't."""

    def __init__(self, marker_type: type):
        self._marker_type = marker_type

    @property
    def name(self) -> str:
        return "org.openrewrite.test.RequiresMarker"

    @property
    def display_name(self) -> str:
        return "Require a marker"

    @property
    def description(self) -> str:
        return "Rename `a` in a file that carries the marker."

    def editor(self) -> TreeVisitor:
        marker_type = self._marker_type

        class Rename(_RenameA):
            def visit_compilation_unit(self, cu: CompilationUnit, ctx: ExecutionContext) -> J:
                if cu.markers.find_first(marker_type) is None:
                    raise ValueError(f"{cu.source_path} has no {marker_type.__name__}")
                return super().visit_compilation_unit(cu, ctx)

        return Rename()


class _NoteVisitedTexts(Recipe):
    """Renames ``a`` to ``b`` and notes the text of each file it visits on the execution context."""

    key = "org.openrewrite.test.visitedTexts"

    @property
    def name(self) -> str:
        return "org.openrewrite.test.NoteVisitedTexts"

    @property
    def display_name(self) -> str:
        return "Note visited texts"

    @property
    def description(self) -> str:
        return "Rename `a` and note the text of each file visited."

    def editor(self) -> TreeVisitor:
        key = self.key

        class Note(_RenameA):
            def visit_compilation_unit(self, cu: CompilationUnit, ctx: ExecutionContext) -> J:
                ctx.put_message(key, [*ctx.get_message(key, []), cu.print_all()])
                return super().visit_compilation_unit(cu, ctx)

        return Note()


class TestEmptySources:
    """The harness runs a recipe that changed a source on an empty file of its kind, where it has to raise nothing."""

    @pytest.fixture(autouse=True)
    def _probe_again(self):
        # A probe runs once in a process, and each of these tests needs its own.
        probes._PROBED.clear()

    def test_a_recipe_that_throws_on_an_empty_file_fails(self):
        @lst_dataclass
        class BuildMetadata(Marker):
            _id: int | UUID

        def with_metadata(cu: CompilationUnit) -> CompilationUnit:
            return cu.replace(markers=cu.markers.add(BuildMetadata(random_id())))

        spec = RecipeSpec(recipe=_RenameFirstAssignment(), type_attribution=False)
        with pytest.raises(AssertionError) as failure:
            spec.rewrite_run(python("a = 1\n", "renamed = 1\n", path=Path("a.py"), before_recipe=with_metadata))

        message = str(failure.value)
        assert message.startswith(
            "Check for the structure the recipe edits before using it, so that it handles an empty file or leaves it "
            "unchanged instead of throwing.\n"
            "org.openrewrite.test.RenameFirstAssignment threw when run on an empty CompilationUnit at a.py with the "
            "markers of the test's source (BuildMetadata):\n"
            "    AttributeError: 'Empty' object has no attribute 'variable'\n"
            "        at Rename.visit_compilation_unit (test_rewrite_test.py:"
        )
        assert message.endswith('Then add a test with an empty source, python(""), that states the intended result.')
        assert isinstance(failure.value.__cause__, RecipeRunException)

    def test_a_recipe_that_checks_the_structure_passes(self):
        spec = RecipeSpec(recipe=_RenameFirstAssignment(checked=True), type_attribution=False)
        spec.rewrite_run(python("a = 1\n", "renamed = 1\n"))

    def test_an_ad_hoc_recipe_is_not_probed(self):
        spec = RecipeSpec(recipe=from_visitor(_RenameFirstAssignment().editor()), type_attribution=False)
        spec.rewrite_run(python("a = 1\n", "renamed = 1\n"))

    def test_the_empty_file_has_the_markers_hooks_gave_the_source(self):
        @lst_dataclass
        class BuildMetadata(Marker):
            _id: int | UUID

        def with_metadata(cu: CompilationUnit) -> CompilationUnit:
            return cu.replace(markers=cu.markers.add(BuildMetadata(random_id())))

        spec = RecipeSpec(recipe=_RequiresMarker(BuildMetadata), type_attribution=False)
        spec.rewrite_run(python("a = 1\n", "b = 1\n", before_recipe=with_metadata))

    def test_the_probe_leaves_the_tests_execution_context_as_it_was(self):
        ctx = InMemoryExecutionContext()
        spec = RecipeSpec(recipe=_NoteVisitedTexts(), execution_context=ctx,
                          check_execution_context_unchanged=False, type_attribution=False)
        spec.rewrite_run(python("a = 1\n", "b = 1\n"))

        assert "" not in ctx.get_message(_NoteVisitedTexts.key)

    def test_a_probe_that_failed_does_not_run_again(self):
        spec = RecipeSpec(recipe=_RenameFirstAssignment(), type_attribution=False)
        with pytest.raises(AssertionError, match="threw when run on an empty CompilationUnit"):
            spec.rewrite_run(python("a = 1\n", "renamed = 1\n"))

        spec.rewrite_run(python("a = 1\n", "renamed = 1\n"))

    def test_a_recipe_probed_for_20_seconds_is_probed_no_more(self, monkeypatch):
        recipe = _RenameFirstAssignment()
        monkeypatch.setitem(probes._PROBE_SECONDS, probes._time_cap_key(recipe), 20.5)
        RecipeSpec(recipe=recipe, type_attribution=False).rewrite_run(python("a = 1\n", "renamed = 1\n"))


@dataclass
class _RenameToSettingsName(ScanningRecipe[Dict[str, str]]):
    """Renames ``a`` to the name settings.py first assigns, checking that the scanner found one when ``checked``."""

    checked: bool = field(default=False, metadata=option("Checked", "Check that the scanner found settings.py."))

    @property
    def name(self) -> str:
        return "org.openrewrite.test.RenameToSettingsName"

    @property
    def display_name(self) -> str:
        return "Rename to the settings name"

    @property
    def description(self) -> str:
        return "Rename `a` to the name settings.py first assigns."

    def initial_value(self, ctx: ExecutionContext) -> Dict[str, str]:
        return {}

    def scanner(self, acc: Dict[str, str]) -> TreeVisitor:
        class Find(PythonVisitor[ExecutionContext]):
            def visit_compilation_unit(self, cu: CompilationUnit, ctx: ExecutionContext) -> J:
                first = cu.statements[0]
                if str(cu.source_path) == "settings.py" and isinstance(first, Assignment):
                    acc["name"] = first.variable.simple_name
                return cu

        return Find()

    def editor_with_data(self, acc: Dict[str, str]) -> TreeVisitor:
        checked = self.checked

        class Rename(PythonVisitor[ExecutionContext]):
            def visit_identifier(self, ident: Identifier, ctx: ExecutionContext) -> J:
                if ident.simple_name != "a" or checked and "name" not in acc:
                    return ident
                return ident.replace(simple_name=acc["name"])

        return Rename()


class _DeclareSettingsName(ScanningRecipe[Dict[str, str]]):
    """Renames ``a`` to ``b``, and generates names.py, which assigns the name settings.py first assigns to ``NAME``."""

    @property
    def name(self) -> str:
        return "org.openrewrite.test.DeclareSettingsName"

    @property
    def display_name(self) -> str:
        return "Declare the settings name"

    @property
    def description(self) -> str:
        return "Generate names.py, which assigns the name settings.py first assigns to `NAME`."

    def initial_value(self, ctx: ExecutionContext) -> Dict[str, str]:
        return {}

    def scanner(self, acc: Dict[str, str]) -> TreeVisitor:
        class Find(PythonVisitor[ExecutionContext]):
            def visit_compilation_unit(self, cu: CompilationUnit, ctx: ExecutionContext) -> J:
                first = cu.statements[0]
                acc[str(cu.source_path)] = first.variable.simple_name if isinstance(first, Assignment) else ""
                return cu

        return Find()

    def generate(self, acc: Dict[str, str], ctx: ExecutionContext) -> List[SourceFile]:
        if "names.py" in acc:
            return []
        import ast

        from rewrite.python._parser_visitor import ParserVisitor

        text = f'NAME = "{acc["settings.py"]}"\n'
        return [ParserVisitor(text, None, None).visit_Module(ast.parse(text)).replace(source_path=Path("names.py"))]

    def editor_with_data(self, acc: Dict[str, str]) -> TreeVisitor:
        return _RenameA()


def _settings_and_app() -> List[SourceSpec]:
    return [python("debug = 1\na = 2\n", "debug = 1\ndebug = 2\n", path=Path("settings.py")),
            python("a = 3\n", "debug = 3\n", path=Path("app.py"))]


class TestUnparsedSources:
    """The harness runs a recipe that changed a source without that source, as the recipe runs when the parser failed
    on the file, where the recipe has to raise nothing."""

    @pytest.fixture(autouse=True)
    def _probe_again(self):
        # A probe runs once in a process, and each of these tests needs its own.
        probes._PROBED.clear()

    def test_a_scanning_recipe_that_reads_what_its_scanner_did_not_find_fails(self):
        spec = RecipeSpec(recipe=_RenameToSettingsName(), type_attribution=False)
        with pytest.raises(AssertionError) as failure:
            spec.rewrite_run(*_settings_and_app())

        message = str(failure.value)
        assert message.startswith(
            "Check that the scanner found the files the recipe needs before using what it keeps of them, so that the "
            "recipe changes nothing instead of throwing when one is missing.\n"
            "org.openrewrite.test.RenameToSettingsName threw when settings.py was missing:\n"
            "    KeyError: 'name'\n"
            "        at Rename.visit_identifier (test_rewrite_test.py:"
        )
        assert message.endswith(
            "Besides the files the test gives it, a repository holds files the parser failed on. The host sends a "
            "Python recipe only the Python files it parsed, so a file its parser failed on is simply not there for "
            "the recipe.\n"
            "  - when the scanner found no file of the kind the recipe needs, generate and change nothing, instead of "
            "reading an empty accumulator\n"
            "  - don't catch the exception to make this pass"
        )
        assert isinstance(failure.value.__cause__, RecipeRunException)

    def test_a_scanning_recipe_that_generates_from_what_its_scanner_did_not_find_fails(self):
        spec = RecipeSpec(recipe=_DeclareSettingsName(), type_attribution=False)
        with pytest.raises(AssertionError, match=r"DeclareSettingsName threw when settings.py was missing:\n"
                                                 r"    KeyError: 'settings.py'\n"
                                                 r"        at _DeclareSettingsName.generate \(test_rewrite_test.py:"):
            spec.rewrite_run(
                python("debug = 1\na = 2\n", "debug = 1\nb = 2\n", path=Path("settings.py")),
                python(None, 'NAME = "debug"\n', path=Path("names.py")),
            )

    def test_a_scanning_recipe_that_checks_what_its_scanner_found_passes(self):
        spec = RecipeSpec(recipe=_RenameToSettingsName(checked=True), type_attribution=False)
        spec.rewrite_run(*_settings_and_app())

    def test_a_recipe_defined_in_a_test_function_is_not_probed(self):
        class RenameToSettingsName(_RenameToSettingsName):
            pass

        spec = RecipeSpec(recipe=RenameToSettingsName(), type_attribution=False)
        spec.rewrite_run(*_settings_and_app())

    def test_a_probe_that_passed_does_not_run_again(self):
        spec = RecipeSpec(recipe=_RenameToSettingsName(), type_attribution=False)
        settings, app = _settings_and_app()
        # The probe leaves out app.py, the first source the recipe changes, and the scanner still finds settings.py.
        spec.rewrite_run(app, settings)

        spec.rewrite_run(settings, app)

    def test_a_recipe_with_other_options_is_probed_again(self):
        checked = RecipeSpec(recipe=_RenameToSettingsName(checked=True), type_attribution=False)
        checked.rewrite_run(*_settings_and_app())

        spec = RecipeSpec(recipe=_RenameToSettingsName(), type_attribution=False)
        with pytest.raises(AssertionError, match="RenameToSettingsName threw when settings.py was missing"):
            spec.rewrite_run(*_settings_and_app())

    def test_each_probe_counts_its_time_toward_the_cap(self, monkeypatch):
        # The clock advances a second each time it is read, so each probe takes a second.
        clock = iter(range(100))
        monkeypatch.setattr(probes, "time", SimpleNamespace(perf_counter=lambda: float(next(clock))))
        recipe = _RenameToSettingsName(checked=True)
        monkeypatch.setitem(probes._PROBE_SECONDS, probes._time_cap_key(recipe), 0.0)
        RecipeSpec(recipe=recipe, type_attribution=False).rewrite_run(*_settings_and_app())

        assert probes._PROBE_SECONDS[probes._time_cap_key(recipe)] == 2.0

    def test_a_recipe_probed_for_20_seconds_is_probed_no_more(self, monkeypatch):
        recipe = _RenameToSettingsName()
        monkeypatch.setitem(probes._PROBE_SECONDS, probes._time_cap_key(recipe), 20.5)
        RecipeSpec(recipe=recipe, type_attribution=False).rewrite_run(*_settings_and_app())


class _NoneAsName(PythonVisitor[ExecutionContext]):
    """Replaces the literal ``1`` with a name that prints as ``None``, which the parser makes a literal of."""

    def visit_literal(self, literal: Literal, ctx: ExecutionContext) -> J:
        if literal.value != 1:
            return literal
        return Identifier(random_id(), literal.prefix, Markers.EMPTY, [], "None", None, None)


class TestParserShapedTrees:
    """The harness rejects a recipe whose output holds nodes the parser builds differently from the text they print."""

    def test_a_name_where_the_parser_makes_a_literal_fails(self):
        spec = RecipeSpec(recipe=from_visitor(_NoneAsName()), type_attribution=False)
        with pytest.raises(AssertionError) as failure:
            spec.rewrite_run(python("x = 1\n", "x = None\n", path=Path("a.py")))

        assert str(failure.value) == (
            "Write code with a template (rewrite.python.template) or the Python parser, or build the nodes the way "
            "the parser builds the text they print.\n"
            "The output of org.openrewrite.adhoc holds nodes that the parser makes differently of the same text:\n"
            "  a.py:\n"
            "    `None` is a J.Identifier where the parser makes a J.Literal\n"
            "A recipe that runs after it matches the tree, not the text, so it misses or mistakes a node that parsed "
            "code never holds. If the output is meant to differ from parsed code, opt out with "
            "RecipeSpec(check_parser_shaped_trees=False)."
        )

    def test_a_dotted_name_where_the_parser_makes_a_field_access_fails(self):
        class QualifyCalls(PythonVisitor[ExecutionContext]):
            def visit_method_invocation(self, mi: MethodInvocation, ctx: ExecutionContext) -> J:
                if mi.name.simple_name != "foo":
                    return mi
                return mi.replace(name=mi.name.replace(simple_name="os.getcwd"))

        spec = RecipeSpec(recipe=from_visitor(QualifyCalls()), type_attribution=False)
        with pytest.raises(AssertionError,
                           match=r"\n    `os.getcwd\(\)` holds J.Identifier where the parser makes J.Identifier, "
                                 r"J.Identifier\n"):
            spec.rewrite_run(python("foo()\n", "os.getcwd()\n"))

    def test_output_that_does_not_parse_fails(self):
        class Unspeakable(PythonVisitor[ExecutionContext]):
            def visit_identifier(self, ident: Identifier, ctx: ExecutionContext) -> J:
                return ident.replace(simple_name="not valid") if ident.simple_name == "a" else ident

        spec = RecipeSpec(recipe=from_visitor(Unspeakable()), type_attribution=False)
        with pytest.raises(AssertionError,
                           match="\n    the text it prints doesn't parse: cannot assign to expression on line 1, "
                                 "`not valid = 1`\n"):
            spec.rewrite_run(python("a = 1\n", "not valid = 1\n"))

    def test_output_the_parser_builds_the_same_passes(self):
        spec = RecipeSpec(recipe=from_visitor(_RenameA()), type_attribution=False)
        spec.rewrite_run(python("a = 1\nprint(a)\n", "b = 1\nprint(b)\n"))

    def test_check_turned_off_passes(self):
        spec = RecipeSpec(recipe=from_visitor(_NoneAsName()), check_parser_shaped_trees=False,
                          type_attribution=False)
        spec.rewrite_run(python("x = 1\n", "x = None\n"))


class _RenameThenFormatTheFile(_RenameA):
    def visit_compilation_unit(self, cu: CompilationUnit, ctx: ExecutionContext) -> J:
        renamed = super().visit_compilation_unit(cu, ctx)
        return cu if renamed is cu else auto_format(renamed, ctx)


class TestMinimalFormatting:
    """The harness rejects a recipe that auto-formats a whole file or class that existed before the run."""

    def test_formatting_the_whole_file_after_a_one_line_edit_fails(self):
        spec = RecipeSpec(recipe=from_visitor(_RenameThenFormatTheFile()), type_attribution=False)
        with pytest.raises(AssertionError) as failure:
            spec.rewrite_run(python("a = 1\nprint(a)\n", "b = 1\nprint(b)\n", path=Path("a.py")))

        message = str(failure.value)
        assert message.startswith(
            "Format only what the recipe changed, not the whole class or file around it.\n"
            "org.openrewrite.adhoc auto-formatted a whole file or class that existed before the run, with no "
            "stop_after:\n"
            "  _RenameThenFormatTheFile.visit_compilation_unit (test_rewrite_test.py:"
        )
        assert ") called auto_format on\n    all of a.py\nThat rewrites the whitespace of code" in message
        assert message.endswith("opts out with RecipeSpec(check_minimal_formatting=False).")

    def test_formatting_the_whole_class_after_renaming_a_method_fails(self):
        class RenameThenFormatTheClass(PythonVisitor[ExecutionContext]):
            def visit_class_declaration(self, cd: ClassDeclaration, ctx: ExecutionContext) -> J:
                renamed = _RenameA().visit(cd, ctx, self.cursor.parent_tree_cursor())
                return cd if renamed is cd else auto_format(renamed, ctx, cursor=self.cursor.parent_tree_cursor())

        spec = RecipeSpec(recipe=from_visitor(RenameThenFormatTheClass()), type_attribution=False)
        with pytest.raises(AssertionError, match=r"\) called auto_format on\n    class `A` of a.py\n"):
            spec.rewrite_run(python(
                "class A:\n    def a(self):\n        pass\n",
                "class A:\n    def b(self):\n        pass\n",
                path=Path("a.py"),
            ))

    def test_formatting_only_the_changed_element_passes(self):
        class RenameThenFormatIt(PythonVisitor[ExecutionContext]):
            def visit_identifier(self, ident: Identifier, ctx: ExecutionContext) -> J:
                if ident.simple_name != "a":
                    return ident
                return maybe_auto_format(ident, ident.replace(simple_name="b"), ctx,
                                         cursor=self.cursor.parent_tree_cursor())

        spec = RecipeSpec(recipe=from_visitor(RenameThenFormatIt()), type_attribution=False)
        spec.rewrite_run(python("a = 1\nprint(a)\n", "b = 1\nprint(b)\n"))

    def test_formatting_only_the_class_header_passes(self):
        class RenameThenFormatTheHeader(PythonVisitor[ExecutionContext]):
            def visit_class_declaration(self, cd: ClassDeclaration, ctx: ExecutionContext) -> J:
                if cd.name.simple_name != "A":
                    return cd
                renamed = cd.replace(name=cd.name.replace(simple_name="B"))
                return maybe_auto_format(cd, renamed, ctx, stop_after=renamed.name,
                                         cursor=self.cursor.parent_tree_cursor())

        spec = RecipeSpec(recipe=from_visitor(RenameThenFormatTheHeader()), type_attribution=False)
        # The body keeps the spacing and indentation that formatting it would change.
        spec.rewrite_run(python(
            "class A:\n  def f(self,x):\n    return x\n",
            "class B:\n  def f(self,x):\n    return x\n",
        ))

    def test_formatting_the_class_with_its_body_set_aside_passes(self):
        class RenameThenFormatWithoutTheBody(PythonVisitor[ExecutionContext]):
            def visit_class_declaration(self, cd: ClassDeclaration, ctx: ExecutionContext) -> J:
                if cd.name.simple_name != "A":
                    return cd
                renamed = cd.replace(name=cd.name.replace(simple_name="B"))
                body = renamed.body
                formatted = maybe_auto_format(cd, renamed.replace(body=body.replace(statements=[])), ctx,
                                              cursor=self.cursor.parent_tree_cursor())
                return formatted.replace(body=body)

        spec = RecipeSpec(recipe=from_visitor(RenameThenFormatWithoutTheBody()), type_attribution=False)
        spec.rewrite_run(python(
            "class A:\n  def f(self,x):\n    return x\n",
            "class B:\n  def f(self,x):\n    return x\n",
        ))

    def test_formatting_that_changes_only_whitespace_passes(self):
        class FormatTheFile(PythonVisitor[ExecutionContext]):
            def visit_compilation_unit(self, cu: CompilationUnit, ctx: ExecutionContext) -> J:
                return auto_format(cu, ctx)

        spec = RecipeSpec(recipe=from_visitor(FormatTheFile()), type_attribution=False)
        spec.rewrite_run(python("x=1\n", "x = 1\n"))

    def test_a_recipe_run_with_a_formatting_recipe_passes(self):
        spec = RecipeSpec(type_attribution=False).with_recipes(from_visitor(_RenameThenFormatTheFile()), AutoFormat())
        spec.rewrite_run(python("a = 1\nprint(a)\n", "b = 1\nprint(b)\n"))

    def test_check_turned_off_passes(self):
        spec = RecipeSpec(recipe=from_visitor(_RenameThenFormatTheFile()), check_minimal_formatting=False,
                          type_attribution=False)
        spec.rewrite_run(python("a = 1\nprint(a)\n", "b = 1\nprint(b)\n"))
