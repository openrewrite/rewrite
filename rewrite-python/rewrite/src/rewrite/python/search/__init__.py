# Copyright 2026 the original author or authors.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     https://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

"""In-process search visitors for use as preconditions.

These mirror :class:`org.openrewrite.java.search.HasType`,
``HasMethod``, and ``org.openrewrite.FindSourceFiles`` from rewrite-core
but run entirely in the Python process. They're bundled into the
``RecipeRef`` placeholders returned by
:mod:`rewrite.python.preconditions` so that:

* When connected to a Java host, the framework introspects the wrapper
  and ships the recipe identity (name + options) via the wire path; the
  host evaluates the gate locally and skips the visit RPC for non-matching
  files.
* When **not** connected to a Java host (unit tests, direct in-process
  callers), the bundled native visitor runs and provides real filtering
  instead of unconditionally short-circuiting to "always matches".

Each visitor returns a :class:`SearchResult`-marked tree on match and
the original tree otherwise — i.e., it produces a "different tree" sentinel
that :class:`Check` interprets as the gate matching.
"""

from __future__ import annotations

import fnmatch
import logging
from typing import Any, Optional, cast

from rewrite.java.support_types import J
from rewrite.java.tree import Import, MethodInvocation
from rewrite.markers import SearchResult
from rewrite.python.import_utils import get_name_string, get_qualid_name
from rewrite.python.method_matcher import MethodMatcher
from rewrite.python.support_types import Py
from rewrite.python.tree import MultiImport
from rewrite.python.visitor import PythonVisitor
from rewrite.tree import SourceFile, Tree
from rewrite.visitor import Cursor, TreeVisitor

logger = logging.getLogger(__name__)


class IsSourceFile(TreeVisitor[Tree, Any]):
    """Match :class:`SourceFile` trees by path glob.

    Mirrors ``org.openrewrite.FindSourceFiles``. Uses :func:`fnmatch.fnmatch`
    for glob matching (e.g. ``"**/*.py"``, ``"src/foo/*.py"``).
    """

    def __init__(self, file_pattern: str) -> None:
        self._pattern = file_pattern

    def visit(
        self,
        tree: Optional[Tree],
        p: Any,
        parent: Optional[Cursor] = None,
    ) -> Optional[Tree]:
        if not isinstance(tree, SourceFile):
            return tree
        path = getattr(tree, "source_path", None)
        if path is None:
            return tree
        path_str = str(path)
        if fnmatch.fnmatch(path_str, self._pattern):
            return SearchResult.found(tree)
        return tree


class UsesType(TreeVisitor[Tree, Any]):
    """Match files using a specific type, by walking the tree's
    ``TypesInUse`` (when available) or the per-node type attribution.

    Mirrors ``org.openrewrite.java.search.HasType``. ``fqn_pattern`` is
    matched against fully-qualified type names via :func:`fnmatch.fnmatch`,
    so ``java.util.*`` matches ``java.util.List`` etc.
    """

    def __init__(self, fully_qualified_type: str) -> None:
        self._pattern = fully_qualified_type

    def visit(
        self,
        tree: Optional[Tree],
        p: Any,
        parent: Optional[Cursor] = None,
    ) -> Optional[Tree]:
        if not isinstance(tree, SourceFile):
            return tree
        if self._tree_uses_type(tree, p):
            return SearchResult.found(tree)
        return tree

    def _tree_uses_type(self, tree: Tree, p: Any) -> bool:
        # Prefer TypesInUse on the source file when present (cheap O(N))
        types_in_use = getattr(tree, "types_in_use", None)
        if types_in_use is not None:
            types = getattr(types_in_use, "types_in_use", None) or getattr(
                types_in_use, "types", None
            )
            if types is not None:
                for t in types:
                    fqn = _fully_qualified_name(t)
                    if fqn and fnmatch.fnmatch(fqn, self._pattern):
                        return True
                return False
        return _TypeSearch(self._pattern).search(tree, p)


class UsesMethod(TreeVisitor[Tree, Any]):
    """Match files using a specific method.

    Mirrors ``org.openrewrite.java.search.HasMethod``. The ``method_pattern``
    follows the OpenRewrite method-pattern syntax
    (``<receiver-type> <method-name>(<args>)``) — e.g. ``"*..* tostring(..)"``.
    """

    def __init__(self, method_pattern: str) -> None:
        self._matcher = MethodMatcher.create(method_pattern)

    def visit(
        self,
        tree: Optional[Tree],
        p: Any,
        parent: Optional[Cursor] = None,
    ) -> Optional[Tree]:
        if not isinstance(tree, SourceFile):
            return tree
        if _MethodSearch(self._matcher).search(tree, p):
            return SearchResult.found(tree)
        return tree


class UsesImport(TreeVisitor[Tree, Any]):
    """Match files that import a given module, by import *syntax* rather than
    type attribution.

    Mirrors ``org.openrewrite.python.search.UsesImport``. Reads the as-written
    import path off ``Py.MultiImport`` statements, so it is robust to two
    failure modes that make :class:`UsesType` unusable for gating
    import-migration recipes:

    * the type checker canonicalizes aliases (``from typing import List``
      resolves to ``list``), erasing the deprecated import path; and
    * removed/unresolvable symbols (``from base64 import encodestring``) get
      no type attribution at all.

    ``module`` is a dotted module path (e.g. ``"datetime"``, ``"os.path"``).
    A file matches if it imports that module, a submodule of it, or a parent
    module of it — a generous superset, which is what a precondition wants
    (over-matching merely runs the gated visitor; under-matching would skip a
    file the recipe should change).
    """

    def __init__(self, module: str) -> None:
        self._module = module

    def visit(
        self,
        tree: Optional[Tree],
        p: Any,
        parent: Optional[Cursor] = None,
    ) -> Optional[Tree]:
        if not isinstance(tree, SourceFile):
            return tree
        if _ImportSearch(self._module).search(tree, p):
            return SearchResult.found(tree)
        return tree


def _import_path_matches(imported: str, query: str) -> bool:
    """True when an as-written import path references the queried module.

    Matches the module exactly, a submodule of it (``import os.path`` for
    query ``os``), or a parent of it (``import os`` for query ``os.path``,
    since ``os.path`` is then reachable). Dotted-boundary aware so ``os`` does
    not match ``ossaudiodev``.
    """
    if not imported or not query:
        return False
    return (
        imported == query
        or imported.startswith(query + ".")
        or query.startswith(imported + ".")
    )


def _fully_qualified_name(type_obj: Any) -> Optional[str]:
    """Best-effort accessor for a ``JavaType.FullyQualified``-shaped
    object's fully-qualified name. Returns ``None`` when the type isn't
    fully qualified or hasn't been attributed."""
    if type_obj is None:
        return None
    fqn = getattr(type_obj, "fully_qualified_name", None)
    if isinstance(fqn, str):
        return fqn
    inner = getattr(type_obj, "type", None)
    if inner is not None and inner is not type_obj:
        return _fully_qualified_name(inner)
    return None


class _FindFirst(PythonVisitor[Any]):
    """Traversal that stops at the first node :meth:`matches` accepts.

    Dispatches straight to ``accept`` because a search reads nodes and never
    rewrites them, so nothing consumes the cursor the base ``visit`` builds.
    """

    def __init__(self) -> None:
        self.found = False

    def matches(self, tree: Tree) -> bool:
        raise NotImplementedError

    def visit(
        self,
        tree: Optional[Tree],
        p: Any,
        parent: Optional[Cursor] = None,
    ) -> Optional[J]:
        if tree is None or self.found:
            return cast(Optional[J], tree)
        if self.matches(tree):
            self.found = True
            return cast(J, tree)
        return tree.accept(self, p)

    def search(self, tree: Tree, p: Any) -> bool:
        """Whether ``tree`` or any node under it matches.

        Only the Python model is searchable: a source file of another language
        reaching a Python precondition is a file its recipe cannot edit anyway.
        """
        if not isinstance(tree, Py):
            return False
        try:
            self.visit(tree, p)
        except RecursionError:
            # A tree deeper than the interpreter stack (generated data files
            # nest thousands of implicit string concatenations) is one the gated
            # visitor cannot edit either. Answering "no match" leaves no other
            # trace of the file, hence the log line.
            logger.warning(
                "%s nests too deeply to search; treating it as no match",
                getattr(tree, "source_path", tree),
            )
            return False
        return self.found


class _TypeSearch(_FindFirst):
    def __init__(self, pattern: str) -> None:
        super().__init__()
        self._pattern = pattern

    def matches(self, tree: Tree) -> bool:
        # Read the attribution off any node that carries it: `Expression`
        # declares its own `type` alongside `TypedTree` rather than under it,
        # so a class check on either one alone misses the other's nodes.
        fqn = _fully_qualified_name(getattr(tree, "type", None))
        return fqn is not None and fnmatch.fnmatch(fqn, self._pattern)


class _MethodSearch(_FindFirst):
    def __init__(self, matcher: MethodMatcher) -> None:
        super().__init__()
        self._matcher = matcher

    def matches(self, tree: Tree) -> bool:
        return isinstance(tree, MethodInvocation) and self._matcher.matches(tree)


class _ImportSearch(_FindFirst):
    def __init__(self, module: str) -> None:
        super().__init__()
        self._module = module

    def visit(
        self,
        tree: Optional[Tree],
        p: Any,
        parent: Optional[Cursor] = None,
    ) -> Optional[J]:
        if isinstance(tree, MultiImport):
            # A MultiImport decides the whole statement: under
            # `from <module> import a, b` its children are names, not modules.
            self.found = self.found or self._multi_import_matches(tree)
            return tree
        return super().visit(tree, p, parent)

    def _multi_import_matches(self, multi: MultiImport) -> bool:
        if multi.from_ is not None:
            # `from <module> import ...` — the module is the `from` clause.
            return _import_path_matches(get_name_string(multi.from_), self._module)
        # `import <a>, <b>` — each name's qualid is itself a module.
        return any(
            _import_path_matches(get_qualid_name(imp.qualid), self._module)
            for imp in multi.names
        )

    def matches(self, tree: Tree) -> bool:
        # A J.Import outside a MultiImport is `import <module>`.
        return isinstance(tree, Import) and _import_path_matches(
            get_qualid_name(tree.qualid), self._module
        )


__all__ = ["IsSourceFile", "UsesType", "UsesMethod", "UsesImport"]
