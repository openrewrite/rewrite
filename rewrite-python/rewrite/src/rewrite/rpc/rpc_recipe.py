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
Referencing and delegating to recipes hosted on another peer over RPC.

The public surface is :class:`RpcRecipe` — a reference to a recipe reached over
the RPC bridge (today: a Java recipe on the JVM), usable as an entry in a
composite's ``recipe_list()`` (see its docstring). This lets Python recipes
leverage the full Java recipe ecosystem (``ChangeType``, ``ChangeMethodName``,
etc.) without reimplementing them.

``prepare_java_recipe`` + ``JavaRecipeVisitor`` + ``PreparedJavaRecipe`` are the
lower-level helpers that splice a Java recipe's *edit* visitor in over RPC. They
are used internally by :meth:`RpcRecipe.editor` (the Python-host face) and are no
longer called directly by any recipe.
"""

import logging
from dataclasses import dataclass
from typing import Any, Dict, Optional

from rewrite.recipe import Recipe
from rewrite.tree import Tree
from rewrite.visitor import TreeVisitor

logger = logging.getLogger(__name__)


class RpcRecipe(Recipe):
    """A reference to a recipe reached over the RPC bridge, usable as an entry
    in a composite's ``recipe_list()``.

    This lets a pure-composite Python recipe (one that overrides only
    ``recipe_list()`` and has no ``editor()`` of its own) include a recipe that
    is implemented on another peer (today: a Java recipe on the JVM),
    identified by its fully-qualified id. Options are passed as kwargs using the
    target recipe's own option names, verbatim — no name translation — so they
    round-trip to any ecosystem (e.g. a Java recipe's camelCase
    ``oldFullyQualifiedTypeName``)::

        class UpgradePydantic(Recipe):
            @property
            def name(self):
                return "io.moderne.example.UpgradePydantic"

            def recipe_list(self):
                return [
                    ReplacePopulateByNameWithValidateByName(),  # Python recipe
                    RpcRecipe("org.openrewrite.python.UpgradeDependencyVersion",
                              packageName="pydantic", newVersion=">=2.11.0"),
                ]

    The reference carries only an id + options, and the framework resolves it
    according to who orchestrates the run — the same author-written
    ``RpcRecipe`` has two faces:

      * **JVM hosts**: when the JVM materializes the composite over RPC and
        round-trips ``PrepareRecipe`` for this child's id, the Python peer
        answers with a ``delegatesTo`` response carrying the id + options (see
        ``handle_prepare_recipe`` in ``rpc/server.py``). The JVM instantiates
        the real recipe natively, running its full lifecycle — including
        ``ScanningRecipe`` scan+edit phases and non-Python source files such as
        ``pyproject.toml``. ``editor()`` is never called in this mode (the
        server short-circuits to ``delegatesTo`` first).

      * **Python hosts**: when Python orchestrates the run in-process,
        ``editor()`` splices the target recipe's *edit* visitor in over RPC
        (via :func:`prepare_java_recipe`). This is single-pass over the shared
        Python/Java LST — it does not run a ``ScanningRecipe``'s scan phase or
        touch non-Python files; a full scan+generate proxy (the mirror of
        ``RpcRecipe`` on the Java and JavaScript sides) is future work.
    """

    def __init__(self, name: str, **options: Any):
        self._name = name
        # ``java_recipe_name`` / ``delegates_to_options`` are the (internal)
        # attribute names the server's delegatesTo production reads; see
        # ``handle_prepare_recipe`` in ``rpc/server.py``. The over-the-wire
        # delegatesTo payload is the ecosystem-neutral ``{recipeName, options}``.
        self.java_recipe_name = name
        self.delegates_to_options: Dict[str, Any] = dict(options)
        self._prepared: Optional["PreparedJavaRecipe"] = None

    @property
    def name(self) -> str:
        return self._name

    @property
    def display_name(self) -> str:
        return self._name

    @property
    def description(self) -> str:
        return f"Delegates over RPC to the recipe `{self._name}`."

    def editor(self) -> TreeVisitor[Tree, Any]:
        # Python-host face: splice the target recipe's edit visitor in over RPC.
        # Never reached when the JVM orchestrates (the server short-circuits to
        # delegatesTo before editor() is introspected).
        if self._prepared is None:
            self._prepared = prepare_java_recipe(self._name, self.delegates_to_options)
        return JavaRecipeVisitor(self._prepared)


@dataclass
class PreparedJavaRecipe:
    """Represents a Java recipe that has been prepared for execution via RPC.

    This is returned by prepare_java_recipe() and contains the information
    needed to execute the Java recipe's visitors.
    """
    id: str
    name: str
    edit_visitor: Optional[str]
    scan_visitor: Optional[str]
    descriptor: Dict[str, Any]


def prepare_java_recipe(recipe_name: str, options: Optional[Dict[str, Any]] = None) -> PreparedJavaRecipe:
    """Prepare a Java recipe for execution via RPC.

    This sends a PrepareRecipe RPC request to the Java side, which instantiates
    the recipe and returns visitor information that can be used to execute it.

    Args:
        recipe_name: Fully qualified name of the Java recipe (e.g., "org.openrewrite.java.ChangeType")
        options: Optional dict of recipe options

    Returns:
        PreparedJavaRecipe containing the visitor information

    Raises:
        RuntimeError: If the recipe cannot be prepared (not found, invalid options, etc.)

    Example:
        recipe = prepare_java_recipe(
            "org.openrewrite.java.ChangeType",
            {"oldFullyQualifiedTypeName": "ast.Num", "newFullyQualifiedTypeName": "ast.Constant"}
        )
    """
    from .server import send_request

    params = {'id': recipe_name}
    if options:
        params['options'] = options

    logger.info(f"Preparing Java recipe: {recipe_name} with options: {options}")

    result = send_request('PrepareRecipe', params)

    if result is None:
        raise RuntimeError(f"Failed to prepare Java recipe: {recipe_name}")

    return PreparedJavaRecipe(
        id=result.get('id'),
        name=recipe_name,
        edit_visitor=result.get('editVisitor'),
        scan_visitor=result.get('scanVisitor'),
        descriptor=result.get('descriptor', {})
    )


def visit_with_java_recipe(
    prepared_recipe: PreparedJavaRecipe,
    tree_id: str,
    source_file_type: str,
    context_id: Optional[str] = None,
    phase: str = 'edit'
) -> bool:
    """Execute a prepared Java recipe's visitor on a tree.

    This sends a Visit RPC request to Java to execute the recipe's visitor
    on the specified tree.

    Args:
        prepared_recipe: The prepared Java recipe from prepare_java_recipe()
        tree_id: ID of the tree to visit
        source_file_type: Type of the source file (e.g., "org.openrewrite.python.tree.Py$CompilationUnit")
        context_id: Optional execution context ID
        phase: 'edit' or 'scan'

    Returns:
        True if the tree was modified, False otherwise
    """
    from .server import send_request

    visitor_name = prepared_recipe.edit_visitor if phase == 'edit' else prepared_recipe.scan_visitor
    if visitor_name is None:
        raise RuntimeError(f"No {phase} visitor available for recipe: {prepared_recipe.name}")

    params = {
        'visitor': visitor_name,
        'treeId': tree_id,
        'sourceFileType': source_file_type,
    }
    if context_id:
        params['p'] = context_id

    result = send_request('Visit', params)
    return result.get('modified', False) if result else False


class JavaRecipeVisitor(TreeVisitor[Tree, Any]):
    """A visitor that delegates to a prepared Java recipe via RPC.

    This visitor can be used as the editor for a Python recipe that wants
    to delegate its work to a Java recipe.

    Example:
        class MyRecipe(Recipe):
            def editor(self):
                java_recipe = prepare_java_recipe("org.openrewrite.java.ChangeType", {...})
                return JavaRecipeVisitor(java_recipe)
    """

    def __init__(self, prepared_recipe: PreparedJavaRecipe, phase: str = 'edit'):
        super().__init__()
        self._prepared_recipe = prepared_recipe
        self._phase = phase

    def visit(self, tree: Optional[Tree], p: Any, parent: Any = None) -> Any:
        """Visit a tree by delegating to the Java recipe.

        This method sends the tree to Java for processing and retrieves
        the result.
        """
        if tree is None:
            return None

        from .server import local_objects, get_object_from_java

        # Ensure tree is in local_objects so Java can fetch it
        tree_id = str(tree.id)
        local_objects[tree_id] = tree

        # Determine source file type
        source_file_type = f"{tree.__class__.__module__}.{tree.__class__.__name__}"
        # Convert Python module path to Java-style
        if 'rewrite.python' in source_file_type:
            source_file_type = 'org.openrewrite.python.tree.Py$CompilationUnit'

        # Execute the Java visitor
        modified = visit_with_java_recipe(
            self._prepared_recipe,
            tree_id,
            source_file_type,
            phase=self._phase
        )

        if modified:
            # Fetch the modified tree from Java
            result = get_object_from_java(tree_id, source_file_type)
            return result if result is not None else tree

        return tree
