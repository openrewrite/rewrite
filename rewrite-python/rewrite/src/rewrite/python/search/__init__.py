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
from typing import Any, Optional

from rewrite.markers import SearchResult
from rewrite.python.method_matcher import MethodMatcher
from rewrite.tree import SourceFile, Tree
from rewrite.visitor import Cursor, TreeVisitor


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
        if self._tree_uses_type(tree):
            return SearchResult.found(tree)
        return tree

    def _tree_uses_type(self, tree: Tree) -> bool:
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
        return self._walk_for_type(tree)

    def _walk_for_type(self, tree: Tree) -> bool:
        """Fall back to walking every node looking for a typed reference
        whose fully-qualified name matches ``self._pattern``."""
        found = [False]

        def check(node: Any) -> None:
            if found[0]:
                return
            t = getattr(node, "type", None)
            if t is not None:
                fqn = _fully_qualified_name(t)
                if fqn and fnmatch.fnmatch(fqn, self._pattern):
                    found[0] = True

        _walk(tree, check)
        return found[0]


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
        if self._tree_uses_method(tree):
            return SearchResult.found(tree)
        return tree

    def _tree_uses_method(self, tree: Tree) -> bool:
        from rewrite.java.tree import MethodInvocation

        found = [False]

        def check(node: Any) -> None:
            if found[0]:
                return
            if isinstance(node, MethodInvocation) and self._matcher.matches(node):
                found[0] = True

        _walk(tree, check)
        return found[0]


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


def _walk(node: Any, visit_fn) -> None:
    """Iterative DFS over a tree, calling ``visit_fn`` on each node.

    Cheap reflection over public attributes — sufficient for matching
    against type / method-invocation attributes which are publicly
    exposed on LST nodes.
    """
    stack = [node]
    seen: set = set()
    while stack:
        cur = stack.pop()
        if cur is None:
            continue
        cur_id = id(cur)
        if cur_id in seen:
            continue
        seen.add(cur_id)
        visit_fn(cur)
        # Walk public attribute values that look tree-shaped
        for attr in dir(cur):
            if attr.startswith("_"):
                continue
            try:
                val = getattr(cur, attr)
            except Exception:
                continue
            if isinstance(val, (list, tuple)):
                for item in val:
                    if hasattr(item, "id") or hasattr(item, "markers"):
                        stack.append(item)
            elif hasattr(val, "id") or hasattr(val, "markers"):
                if val is not cur:
                    stack.append(val)


__all__ = ["IsSourceFile", "UsesType", "UsesMethod"]
