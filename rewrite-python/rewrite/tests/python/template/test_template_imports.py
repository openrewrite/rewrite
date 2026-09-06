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

"""Tests for the imports a spliced template brings into the file it lands in."""

import pytest

from rewrite import ExecutionContext, Recipe
from rewrite.execution import RecipeRunException
from rewrite.python.template import capture, pattern, template
from rewrite.python.visitor import PythonVisitor
from rewrite.test import RecipeSpec, python


def _recipe(pat, tmpl) -> Recipe:
    """A recipe replacing every match of ``pat`` with ``tmpl``."""

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
                    if match:
                        return tmpl.apply(self, values=match)
                    return method

            return Visitor()

    return Replace()


def test_context_import_reaches_the_target_file():
    arg = capture('arg')
    pat = pattern(f"os.popen({arg})", context=["import os"])
    tmpl = template(f"subprocess.run({arg}, shell=True)", context=["import subprocess"])

    RecipeSpec(recipe=_recipe(pat, tmpl)).rewrite_run(
        python(
            """
            import os
            out = os.popen('ls')
            """,
            """
            import os
            import subprocess
            out = subprocess.run('ls', shell=True)
            """,
        )
    )


def test_reference_uses_the_name_the_file_already_binds():
    """A file importing the module under an alias binds it once, under that alias."""
    arg = capture('arg')
    pat = pattern(f"os.popen({arg})", context=["import os"])
    tmpl = template(f"subprocess.run({arg}, shell=True)", context=["import subprocess"])

    RecipeSpec(recipe=_recipe(pat, tmpl)).rewrite_run(
        python(
            """
            import os
            import subprocess as sp
            out = os.popen('ls')
            """,
            """
            import os
            import subprocess as sp
            out = sp.run('ls', shell=True)
            """,
        )
    )


def test_member_import_does_not_bind_the_module():
    """``from subprocess import run`` binds ``run``, so ``subprocess.run`` still needs the module."""
    arg = capture('arg')
    pat = pattern(f"os.popen({arg})", context=["import os"])
    tmpl = template(f"subprocess.run({arg}, shell=True)", context=["import subprocess"])

    RecipeSpec(recipe=_recipe(pat, tmpl)).rewrite_run(
        python(
            """
            import os
            from subprocess import run
            out = os.popen('ls')
            """,
            """
            import os
            from subprocess import run
            import subprocess
            out = subprocess.run('ls', shell=True)
            """,
        )
    )


def test_conditional_import_does_not_bind_at_runtime():
    """An ``if TYPE_CHECKING`` import binds nothing the spliced call could reach."""
    arg = capture('arg')
    pat = pattern(f"os.popen({arg})", context=["import os"])
    tmpl = template(f"subprocess.run({arg}, shell=True)", context=["import subprocess"])

    RecipeSpec(recipe=_recipe(pat, tmpl)).rewrite_run(
        python(
            """
            import os
            from typing import TYPE_CHECKING

            if TYPE_CHECKING:
                import subprocess

            out = os.popen('ls')
            """,
            """
            import os
            from typing import TYPE_CHECKING
            import subprocess

            if TYPE_CHECKING:
                import subprocess

            out = subprocess.run('ls', shell=True)
            """,
        )
    )


def test_context_the_template_does_not_reference_is_not_imported():
    """Context typing a capture is not code the template splices, so it imports nothing —
    not even into a file that spells the same name for its own purposes."""
    arg = capture('arg')
    pat = pattern(f"os.popen({arg})", context=["import os"])
    tmpl = template(f"subprocess.run({arg}, shell=True)",
                    context=["import subprocess", "from typing import Any"])

    RecipeSpec(recipe=_recipe(pat, tmpl)).rewrite_run(
        python(
            """
            import os
            Any = 3
            print(Any)
            out = os.popen('ls')
            """,
            """
            import os
            import subprocess
            Any = 3
            print(Any)
            out = subprocess.run('ls', shell=True)
            """,
        )
    )


def test_import_lands_at_module_scope_for_a_splice_inside_a_function():
    arg = capture('arg')
    pat = pattern(f"os.popen({arg})", context=["import os"])
    tmpl = template(f"subprocess.run({arg}, shell=True)", context=["import subprocess"])

    RecipeSpec(recipe=_recipe(pat, tmpl)).rewrite_run(
        python(
            """
            import os


            def listing():
                return os.popen('ls')
            """,
            """
            import os
            import subprocess


            def listing():
                return subprocess.run('ls', shell=True)
            """,
        )
    )


def test_applying_to_a_cursor_cannot_bind_and_says_so():
    tmpl = template("subprocess.run('ls')", context=["import subprocess"])

    with pytest.raises(ValueError, match="Pass the visitor"):
        tmpl.apply(cursor=None)


def test_dotted_module_binds_its_root():
    """``import os.path`` binds ``os``, which is the name the spliced code reads through."""
    arg = capture('arg')
    pat = pattern(f"os.popen({arg})", context=["import os"])
    tmpl = template(f"os.path.basename({arg})", context=["import os.path"])

    RecipeSpec(recipe=_recipe(pat, tmpl)).rewrite_run(
        python(
            """
            import os
            out = os.popen('ls')
            """,
            """
            import os
            import os.path
            out = os.path.basename('ls')
            """,
        )
    )


def test_a_name_the_template_binds_itself_is_not_a_context_reference():
    """The comprehension's ``run`` is the template's own, whatever the context calls the same name."""
    arg = capture('arg')
    pat = pattern(f"os.popen({arg})", context=["import os"])
    tmpl = template(f"[run(i) for run in {arg}]", context=["from subprocess import run"])

    RecipeSpec(recipe=_recipe(pat, tmpl)).rewrite_run(
        python(
            """
            import os
            from subprocess import run as r
            out = os.popen('ls')
            """,
            """
            import os
            from subprocess import run as r
            out = [run(i) for run in 'ls']
            """,
        )
    )


def test_a_scope_binding_the_name_to_something_else_refuses():
    arg = capture('arg')
    pat = pattern(f"os.popen({arg})", context=["import os"])
    tmpl = template(f"subprocess.run({arg}, shell=True)", context=["import subprocess"])

    with pytest.raises(RecipeRunException) as refusal:
        RecipeSpec(recipe=_recipe(pat, tmpl)).rewrite_run(
            python(
                """
                import os
                import subprocess

                def listing(subprocess):
                    return os.popen('ls')
                """,
            )
        )
    assert "rather than the module 'subprocess'" in str(refusal.value.cause)


def test_a_file_binding_the_name_to_another_module_refuses():
    arg = capture('arg')
    pat = pattern(f"os.popen({arg})", context=["import os"])
    tmpl = template(f"run({arg}, shell=True)", context=["from subprocess import run"])

    with pytest.raises(RecipeRunException) as refusal:
        RecipeSpec(recipe=_recipe(pat, tmpl)).rewrite_run(
            python(
                """
                import os
                from mylib import run
                print(run)
                out = os.popen('ls')
                """,
            )
        )
    assert "other than 'run' from 'subprocess'" in str(refusal.value.cause)


def test_an_aliased_member_merges_into_the_file_s_import_of_that_module():
    """The context's alias is the name imported, and the module's existing import carries it."""
    arg = capture('arg')
    pat = pattern(f"os.popen({arg})", context=["import os"])
    tmpl = template(f"r({arg})", context=["from subprocess import run as r"])

    RecipeSpec(recipe=_recipe(pat, tmpl)).rewrite_run(
        python(
            """
            import os
            from subprocess import Popen
            out = os.popen('ls')
            """,
            """
            import os
            from subprocess import Popen, run as r
            out = r('ls')
            """,
        )
    )


def test_a_local_import_covering_the_context_is_left_to_do_the_binding():
    """A function importing lazily binds the name where the splice lands."""
    arg = capture('arg')
    pat = pattern(f"os.popen({arg})", context=["import os"])
    tmpl = template(f"subprocess.run({arg}, shell=True)", context=["import subprocess"])

    RecipeSpec(recipe=_recipe(pat, tmpl)).rewrite_run(
        python(
            """
            import os


            def listing():
                import subprocess
                return os.popen('ls')
            """,
            """
            import os


            def listing():
                import subprocess
                return subprocess.run('ls', shell=True)
            """,
        )
    )


def test_a_relative_context_import_stays_relative():
    """``from . import util`` names a sibling module, never the standard library's."""
    arg = capture('arg')
    pat = pattern(f"os.popen({arg})", context=["import os"])
    tmpl = template(f"util.run({arg})", context=["from . import util"])

    RecipeSpec(recipe=_recipe(pat, tmpl)).rewrite_run(
        python(
            """
            import os
            out = os.popen('ls')
            """,
            """
            import os
            from . import util
            out = util.run('ls')
            """,
        )
    )
