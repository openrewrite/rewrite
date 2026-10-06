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

"""Tests for captures whose ``type_hint`` restricts what they match."""

import pytest

from rewrite import ExecutionContext, Recipe
from rewrite.python.template import capture, pattern, template
from rewrite.python.visitor import PythonVisitor
from rewrite.test import RecipeSpec, python


def _recipe(pat, tmpl) -> Recipe:
    """A recipe replacing every call matching ``pat`` with ``tmpl``."""

    class Replace(Recipe):
        @property
        def name(self):
            return "test.Replace"

        @property
        def display_name(self):
            return "Replace"

        @property
        def description(self):
            return "Replace."

        def editor(self):
            class Visitor(PythonVisitor[ExecutionContext]):
                def visit_method_invocation(self, method, p):
                    method = super().visit_method_invocation(method, p)
                    match = pat.match(method, self.cursor)
                    return tmpl.apply(self.cursor, values=match) if match else method

            return Visitor()

    return Replace()


def test_typed_capture_matches_only_assignable_targets():
    p = capture('p', type_hint='pathlib.PurePath')
    RecipeSpec(recipe=_recipe(
        pattern(f"str({p})", context=["import pathlib"]),
        template(f"{p}.as_posix()"),
    )).rewrite_run(
        python(
            """
            import pathlib
            def f(path: pathlib.Path, s: str, either: pathlib.Path | str):
                str(path)
                str(s)
                str(either)
                str(undefined)
            """,
            """
            import pathlib
            def f(path: pathlib.Path, s: str, either: pathlib.Path | str):
                path.as_posix()
                str(s)
                str(either)
                str(undefined)
            """,
        )
    )


def test_expressions_around_a_typed_capture_match_by_assignability():
    p = capture('p', type_hint='pathlib.PurePath')
    RecipeSpec(recipe=_recipe(
        pattern(f"str({p}.parent)", context=["import pathlib"]),
        template(f"{p}.parent.as_posix()"),
    )).rewrite_run(
        python(
            """
            import pathlib
            def f(path: pathlib.Path):
                str(path.parent)
            """,
            """
            import pathlib
            def f(path: pathlib.Path):
                path.parent.as_posix()
            """,
        )
    )

    x = capture('x', type_hint='float')
    RecipeSpec(recipe=_recipe(
        pattern(f"round({x} * 2)"),
        template(f"round({x} * 2.0)"),
    )).rewrite_run(
        python(
            """
            def f(n: int):
                round(n * 2)
            """,
            """
            def f(n: int):
                round(n * 2.0)
            """,
        )
    )


def test_union_hint_matches_any_member():
    x = capture('x', type_hint='int | str')
    RecipeSpec(recipe=_recipe(
        pattern(f"print({x})"),
        template(f"log({x})"),
    )).rewrite_run(
        python(
            """
            def f(n: int, s: str, l: list[int]):
                print(n)
                print(s)
                print(l)
            """,
            """
            def f(n: int, s: str, l: list[int]):
                log(n)
                log(s)
                print(l)
            """,
        )
    )


def test_typed_variadic_capture_requires_every_element_to_match():
    args = capture('args', variadic=True, type_hint='str')
    RecipeSpec(recipe=_recipe(
        pattern(f"print({args})"),
        template(f"log({args})"),
    )).rewrite_run(
        python(
            """
            def f(a: str, b: str, n: int):
                print(a, b)
                print(a, n)
            """,
            """
            def f(a: str, b: str, n: int):
                log(a, b)
                print(a, n)
            """,
        )
    )


def test_any_hint_constrains_nothing():
    x = capture('x', type_hint='Any')
    RecipeSpec(recipe=_recipe(
        pattern(f"str({x})", context=["from typing import Any"]),
        template(f"repr({x})"),
    )).rewrite_run(
        python(
            """
            str(undefined)
            """,
            """
            repr(undefined)
            """,
        )
    )


def test_type_hint_that_does_not_resolve_is_rejected():
    # The same code with a resolving hint is parsed first, so a cache keyed without the hint would answer for it
    pattern(f"len({capture('x', type_hint='str')})").get_tree()
    x = capture('x', type_hint='NoSuchType')
    with pytest.raises(ValueError, match="NoSuchType"):
        pattern(f"len({x})").get_tree()


def test_placeholder_type_syntax_is_rejected():
    with pytest.raises(ValueError, match="type_hint"):
        pattern("len({x:int})", x=capture('x')).get_tree()
