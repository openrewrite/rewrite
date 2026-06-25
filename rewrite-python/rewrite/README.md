# OpenRewrite Python

OpenRewrite automated refactoring for Python source code. This package provides
the recipe framework, the Python Lossless Semantic Tree (LST), and the testing
helpers you use to author and test Python recipes.

## Installation

```bash
pip install openrewrite
```

## How it works

OpenRewrite for Python uses a split JVM/Python architecture. You author and
unit-test recipes in pure Python, but **running a recipe against a real codebase
is orchestrated by the JVM runtime** via the
[Moderne CLI](https://docs.moderne.io/user-documentation/moderne-cli/getting-started/cli-intro)
(with [Python support configured](https://docs.moderne.io/user-documentation/moderne-cli/how-to-guides/python))
over an RPC bridge. There is no standalone, in-process Python parser entry point.

## Quick start

The fastest way to author and exercise a recipe is the test harness, which
parses a *before* snippet, runs your recipe, and asserts the result matches
*after*:

```python
from rewrite.test import RecipeSpec, python

def test_renames_a_call():
    spec = RecipeSpec(recipe=RenameFunctionCall(
        old_name="assertEquals",
        new_name="assertEqual",
    ))
    spec.rewrite_run(
        python("assertEquals(a, b)", "assertEqual(a, b)"),
    )
```

`python(before, after)` asserts a change; `python(before)` asserts no change.

## Writing a recipe

A recipe is a `@dataclass` subclassing `Recipe` that returns a visitor from
`editor()`. Each option must have a default value, or the recipe cannot be
discovered or run.

```python
from dataclasses import dataclass, field

from rewrite import ExecutionContext, Recipe, TreeVisitor, option
from rewrite.java import J
from rewrite.java.tree import MethodInvocation
from rewrite.python.visitor import PythonVisitor


@dataclass
class RenameFunctionCall(Recipe):
    """Rename calls to a function from one name to another."""

    old_name: str = field(default="", metadata=option(
        display_name="Old function name",
        description="The name of the function whose calls should be renamed.",
        example="assertEquals",
    ))

    new_name: str = field(default="", metadata=option(
        display_name="New function name",
        description="The name to rename matching calls to.",
        example="assertEqual",
    ))

    @property
    def name(self) -> str:
        return "com.yourorg.RenameFunctionCall"

    @property
    def display_name(self) -> str:
        return "Rename a function call"

    @property
    def description(self) -> str:
        return "Rename calls to a function from one name to another."

    def editor(self) -> TreeVisitor[J, ExecutionContext]:
        old_name = self.old_name
        new_name = self.new_name

        class Visitor(PythonVisitor[ExecutionContext]):
            def visit_method_invocation(self, method: MethodInvocation, p: ExecutionContext) -> J:
                method = super().visit_method_invocation(method, p)
                if method.name.simple_name == old_name:
                    renamed = method.name.replace(_simple_name=new_name)
                    return method.replace(_name=renamed)
                return method

        return Visitor()
```

Returning `None` from a visit method removes the node entirely — which is how
recipes delete code.

## Running recipes with the Moderne CLI

Expose an `activate()` function so the CLI can discover your recipe:

```python
from rewrite.marketplace import RecipeMarketplace, Python

def activate(marketplace: RecipeMarketplace) -> None:
    marketplace.install(RenameFunctionCall, Python)
```

Then install and run it against a repository whose Python LSTs you've built,
passing each option as a `-P` parameter:

```bash
mod config recipes pip install .
mod build /path/to/your/repo
mod run /path/to/your/repo --recipe=com.yourorg.RenameFunctionCall \
    -Pold_name=assertEquals -Pnew_name=assertEqual
```

## Learn more

- [Python Recipe Starter](https://github.com/moderneinc/python-recipe-starter) —
  a complete project with example recipes, tests, and CI to clone and build on
- [Writing a Python refactoring recipe](https://docs.moderne.io/user-documentation/recipes/recipe-authoring/writing-python-recipes) —
  step-by-step authoring guide
- [`rewrite-python` module](https://github.com/openrewrite/rewrite/tree/main/rewrite-python) —
  the Python LST, parser, and built-in recipes
- [docs.openrewrite.org](https://docs.openrewrite.org) — full OpenRewrite documentation

## License

Moderne Source Available License - see [LICENSE.md](../LICENSE.md)
