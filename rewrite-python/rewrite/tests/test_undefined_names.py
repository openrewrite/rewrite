# Copyright 2026 the original author or authors.
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

"""Guard every module against reading a global nothing defines -- like the 38 bare, unimported
``replace(...)`` calls ``rewrite/java/tree.py`` shipped -- by resolving names against each module's
real runtime namespace, which ``ruff``'s F821 skips whenever a star import is present."""

import ast
import builtins
import importlib
import symtable
from pathlib import Path

import pytest

import rewrite

PACKAGE_ROOT = Path(rewrite.__file__).parent

# Modules whose import has side effects or optional dependencies that make importing
# them from a bare test session unreliable.
SKIP = {"__main__.py"}


def _modules():
    return [
        p for p in sorted(PACKAGE_ROOT.rglob("*.py"))
        if p.name not in SKIP
    ]


def _module_name(path: Path) -> str:
    rel = path.relative_to(PACKAGE_ROOT.parent).with_suffix("")
    parts = list(rel.parts)
    if parts[-1] == "__init__":
        parts.pop()
    return ".".join(parts)


def _global_references(table: symtable.SymbolTable) -> set:
    """Names each scope reads but does not bind -- i.e. module-global lookups."""
    names = set()
    for symbol in table.get_symbols():
        if symbol.is_referenced() and symbol.is_global():
            names.add(symbol.get_name())
    for child in table.get_children():
        names |= _global_references(child)
    return names


def _module_bindings(table: symtable.SymbolTable) -> set:
    """Names bound at module scope, including platform-gated imports (``import msvcrt``) and PEP 709
    inlined-comprehension loop variables, both of which are intentional bindings rather than typos."""
    return {
        symbol.get_name() for symbol in table.get_symbols()
        if symbol.is_assigned() or symbol.is_imported() or symbol.is_local()
    }


def _executable_names(tree: ast.Module) -> set:
    """Every ``Name`` load outside an annotation."""
    annotation_nodes = set()
    for node in ast.walk(tree):
        if isinstance(node, (ast.FunctionDef, ast.AsyncFunctionDef)):
            targets = [node.returns]
            args = node.args
            targets += [a.annotation for a in (*args.posonlyargs, *args.args, *args.kwonlyargs,
                                               args.vararg, args.kwarg) if a is not None]
        elif isinstance(node, ast.AnnAssign):
            targets = [node.annotation]
        else:
            continue
        for t in targets:
            if t is not None:
                annotation_nodes.update(id(n) for n in ast.walk(t))

    return {
        n.id for n in ast.walk(tree)
        if isinstance(n, ast.Name) and isinstance(n.ctx, ast.Load) and id(n) not in annotation_nodes
    }


@pytest.mark.parametrize("path", _modules(), ids=lambda p: str(p.relative_to(PACKAGE_ROOT)))
def test_no_undefined_globals(path: Path):
    source = path.read_text(encoding="utf-8")
    try:
        module = importlib.import_module(_module_name(path))
    except ImportError as e:
        # Not a pass: the module's globals cannot be resolved without importing it.
        pytest.skip(f"module is not importable, so its globals cannot be checked: {e}")

    tree = ast.parse(source, filename=str(path))
    module_table = symtable.symtable(source, str(path), "exec")
    referenced = _global_references(module_table)
    executable = _executable_names(tree)
    bound = _module_bindings(module_table)

    undefined = sorted(
        name for name in referenced & executable
        if name not in bound and not hasattr(module, name) and not hasattr(builtins, name)
    )

    assert not undefined, (
        f"{path.relative_to(PACKAGE_ROOT)} reads global name(s) that resolve to nothing "
        f"at runtime: {', '.join(undefined)}"
    )
