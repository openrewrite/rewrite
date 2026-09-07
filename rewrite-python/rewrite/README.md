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

## Inspecting type attribution

Most recipe debugging is one question: what type did this expression get, and if none, where was it lost? Set `REWRITE_PYTHON_DUMP_TYPES` and any test prints the attribution its own parse produced — which is the attribution a `MethodMatcher` pattern written for that test has to match:

```
$ REWRITE_PYTHON_DUMP_TYPES=1 pytest tests/recipes/test_my_recipe.py -s

--- type attribution: my_recipe.py ---
line:col  kind                   source            type
4:1       MethodDeclaration      def f(arr)        my_recipe f(..) -> <none>
4:7       NamedVariable          arr               ⚠ <unknown>
5:5       MethodInvocation       socket.getfqdn()  socket getfqdn(..) -> str
6:12      MethodInvocation       arr.tostring()    ⚠ <unknown> tostring(..) -> <unknown>
6:12        └ select:Identifier  arr               <unknown>
```

The text before `->` is a pattern you can paste into `MethodMatcher.create(...)` or `uses_method(...)`. `socket.getfqdn()` resolves from the file's own imports, so it carries a declaring type; `arr.tostring()` does not, and the indented `select` line names the receiver that lost it. `<none>` is a slot the parser left empty and `<unknown>` is a `JavaType.Unknown` — worth keeping apart, since a `MethodInvocation` always carries *some* method type.

The variable accepts comma-separated flags: `missing` lists only unattributed nodes, `all` widens beyond calls and declarations, and `supertypes` shows each declaring type's ancestry — which bounds how general a pattern can be, since a type recording no supertype can only be matched by its own name or a wildcard.

A recipe gated on a type that never resolved is the usual reason a test sees no change, so that failure names the unattributed nodes without being asked:

```
Expected recipe to produce a change for:
def f(arr):
    return arr.tostring()

Nodes with no type attribution (a recipe gated on one of these cannot fire):
  1:7  NamedVariable  arr  -> <unknown>
  2:12  MethodInvocation  arr.tostring()  -> <unknown> tostring(..) -> <unknown>
```

### Against a file on disk

`rewrite-python-types <file.py>` runs the same report outside a test, for reading an existing project or sweeping a corpus. Note that it resolves types against the file's own directory, so a project laid out differently from your test workspace can attribute differently — prefer the test-harness output when writing a pattern for a test.

| flag | |
| --- | --- |
| `--ty` | attribute types with a ty client (off by default, so the zero-config invocation still runs) |
| `--only-missing` | list only the nodes whose type is missing |
| `--all` | every type-bearing node, not just calls and declarations |
| `--supertypes` | show each declaring type's ancestry |
| `--tree` | the nested structure with prefixes, for structural rather than type questions |
| `--json` | the listing as JSON, for a test or CI check that asserts a fixture gained attribution |
| `--diff-ty` | parse twice, with and without ty, and report the nodes that differ |

`--diff-ty` answers "does my recipe need type attribution to work?" — a recipe gated only on rows that read the same in both columns runs without a type check:

```
$ rewrite-python-types --diff-ty probe.py
line:col  kind               source            without ty                          with ty
4:1       MethodDeclaration  def probe(arr)    ⚠ <none> probe(..) -> <none>        probe probe(..) -> <none>
4:11      NamedVariable      arr               ⚠ <none>                            ⚠ <unknown>
5:5       MethodInvocation   socket.getfqdn()  socket getfqdn(..) -> <none>        socket getfqdn(..) -> str
6:12      MethodInvocation   arr.tostring()    ⚠ <unknown> tostring(..) -> <none>  ⚠ <unknown> tostring(..) -> <unknown>

4 of 4 nodes differ
```

An unannotated parameter leaves `arr.tostring()` unresolved even under ty.

From a test or a REPL, `print_types(source_file)` writes the same listing and `build_type_report(source_file)` returns it as data. Both are read-only, and the in-process parse behind the command is for diagnostics only.

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
# From your recipe project directory, install it into the CLI's marketplace:
mod config recipes pip install .

# Build the LSTs for the repository you want to refactor, then run the recipe:
mod build /path/to/your/repo
mod run /path/to/your/repo --recipe=com.yourorg.RenameFunctionCall \
    -P old_name=assertEquals -P new_name=assertEqual
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
