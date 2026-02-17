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
Infrastructure for calling Java recipes from Python via RPC.

This module provides the ability for Python recipes to delegate to Java recipes,
similar to how JavaScript recipes can call Java recipes. This enables Python
recipes to leverage the full Java recipe ecosystem, including recipes like
ChangeType, ChangeMethodName, etc.

Example:
    from rewrite.rpc.java_recipe import JavaRecipe

    class MyPythonRecipe(Recipe):
        def __init__(self):
            self._java_recipe = None

        async def editor(self):
            if self._java_recipe is None:
                self._java_recipe = await prepare_java_recipe(
                    "org.openrewrite.java.ChangeType",
                    {"oldFullyQualifiedTypeName": "ast.Num", "newFullyQualifiedTypeName": "ast.Constant"}
                )
            return JavaRecipeVisitor(self._java_recipe)
"""

import logging
from dataclasses import dataclass
from typing import Any, Dict, Optional

from rewrite.tree import Tree
from rewrite.visitor import TreeVisitor

logger = logging.getLogger(__name__)


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
