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

"""Java visitor for traversing and transforming Java LST nodes."""

from __future__ import annotations

from typing import TypeVar, Optional, TYPE_CHECKING, List, Any, cast

from rewrite.java.support_types import J, Space, JRightPadded
from rewrite.visitor import TreeVisitor

if TYPE_CHECKING:
    from rewrite.java.tree import MethodDeclaration, Block, Statement

P = TypeVar("P")


def list_map(func, items):
    """Map a function over a list, filtering out None results."""
    if items is None:
        return None
    result = [func(item) for item in items]
    return [item for item in result if item is not None]


class JavaVisitor(TreeVisitor[J, P]):
    """
    Base visitor for Java LST nodes.

    This visitor provides visit methods for all Java AST node types.
    Subclass this to implement Java-specific transformations.
    """

    def __getattr__(self, name: str):
        """Provide default implementations for missing visit_* methods.

        This allows the visitor to handle all Java tree node types without
        requiring explicit implementations for each one. Missing visit_*
        methods return a lambda that calls visit_and_cast with the element.
        """
        if name.startswith('visit_') and not name.startswith('visit_space'):
            def default_visit(elem, p):
                # Default behavior: traverse children via visit()
                return self.visit_children(elem, p)
            return default_visit
        raise AttributeError(f"'{type(self).__name__}' object has no attribute '{name}'")

    def visit_children(self, tree: J, p: P) -> J:
        """Default implementation that traverses all children.

        This is a fallback that recursively visits child nodes. For proper
        traversal, specific visit_* methods should be implemented.
        """
        # Default: return the tree unchanged
        # Subclasses can override specific visit methods for behavior
        return tree

    def visit_space(self, space: Optional[Space], loc: Space.Location, p: P) -> Space:
        """Visit a space (whitespace and comments)."""
        if space is None:
            return Space.EMPTY
        return space

    def visit_right_padded(self, right: Optional[JRightPadded], loc, p: P) -> Optional[JRightPadded]:
        """Visit a right-padded element."""
        if right is None:
            return None
        element = right.element
        if isinstance(element, J):
            element = self.visit(element, p)
        if element is None:
            return None
        if element is right.element:
            return right
        return right.replace(element=element)

    def visit_method_declaration(self, method: 'MethodDeclaration', p: P) -> J:
        """Visit a method declaration, including its body."""
        from rewrite.java.tree import MethodDeclaration
        # Visit the method body if present
        if method.body is not None:
            body = self.visit(method.body, p)
            if body is not method.body:
                method = method.replace(body=body)
        return method

    def visit_block(self, block: 'Block', p: P) -> J:
        """Visit a block of statements."""
        from rewrite.java.tree import Block
        # Visit each statement in the block
        new_statements = list_map(
            lambda rp: self.visit_right_padded(rp, None, p),
            block.padding.statements
        )
        if new_statements is not None:
            # Check if any statements were modified
            changed = len(new_statements) != len(block.padding.statements)
            if not changed:
                for i, stmt in enumerate(new_statements):
                    if stmt is not block.padding.statements[i]:
                        changed = True
                        break
            if changed:
                block = block.padding.replace(statements=new_statements)
        return block
