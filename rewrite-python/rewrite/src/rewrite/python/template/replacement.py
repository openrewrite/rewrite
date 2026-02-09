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

"""Placeholder replacement visitor for template substitution."""

from __future__ import annotations

from typing import Dict, Optional, List, TYPE_CHECKING

from rewrite.java import J, Space
from rewrite.java import tree as j
from rewrite.java.support_types import JContainer
from rewrite.python.visitor import PythonVisitor

from .placeholder import from_placeholder

if TYPE_CHECKING:
    pass


class PlaceholderReplacementVisitor(PythonVisitor[None]):
    """
    Visitor that replaces placeholder identifiers with actual values.

    This visitor traverses a template AST and replaces any identifiers
    that match the placeholder pattern (__placeholder_name__) with
    the corresponding captured values.
    """

    def __init__(self, values: Dict[str, J]):
        """
        Initialize the replacement visitor.

        Args:
            values: Dict mapping capture names to their AST values.
        """
        super().__init__()
        self._values = values

    def visit_identifier(self, identifier: j.Identifier, p: None) -> J:
        """
        Visit an identifier and replace if it's a placeholder.

        Args:
            identifier: The identifier node.
            p: Visitor parameter (unused).

        Returns:
            The replacement value if this is a placeholder, otherwise the identifier.
        """
        name = identifier.simple_name
        capture_name = from_placeholder(name)

        if capture_name is not None and capture_name in self._values:
            replacement = self._values[capture_name]

            # Preserve the placeholder's prefix (whitespace before)
            if hasattr(replacement, 'prefix'):
                replacement = replacement.replace(prefix=identifier.prefix)

            return replacement

        # Not a placeholder or no value provided, continue normally
        return super().visit_identifier(identifier, p)

    def visit_method_invocation(self, method: j.MethodInvocation, p: None) -> J:
        """
        Visit a method invocation.

        This handles cases where the method name itself might be a placeholder,
        or where arguments contain placeholders.
        """
        # First, check if the method select (receiver) needs replacement
        method = method.replace(
            prefix=self.visit_space(method.prefix, p)
        )
        method = method.replace(markers=self.visit_markers(method.markers, p))

        # Visit select (receiver expression) — _select is JRightPadded
        if method.select is not None:
            new_select = self.visit_and_cast(method.select, type(method.select), p)
            if new_select is not method.select:
                padded_select = method.padding.select
                method = method.padding.replace(
                    _select=padded_select.replace(element=new_select)
                )

        # Visit name
        new_name = self.visit_and_cast(method.name, j.Identifier, p)
        # Handle case where name was replaced with a non-identifier
        if isinstance(new_name, j.Identifier):
            method = method.replace(name=new_name)

        # Visit type parameters
        if method.type_parameters is not None:
            # Type parameters don't usually contain placeholders but handle anyway
            pass

        # Visit arguments — _arguments is JContainer[JRightPadded[Expression]]
        padded_args = method.padding.arguments
        if padded_args is not None:
            new_padded = []
            for rp in padded_args.padding.elements:
                new_elem = self.visit(rp.element, p)
                if new_elem is not None:
                    new_padded.append(rp.replace(element=new_elem))
            method = method.padding.replace(
                _arguments=JContainer(
                    padded_args.before, new_padded, padded_args.markers
                )
            )

        return method


class VariadicExpansionVisitor(PythonVisitor[None]):
    """
    Visitor that expands variadic captures in containers.

    When a variadic capture matches multiple elements (like function arguments),
    this visitor handles expanding them into the appropriate container structure.
    """

    def __init__(
        self,
        values: Dict[str, J],
        variadic_values: Dict[str, List[J]]
    ):
        """
        Initialize the expansion visitor.

        Args:
            values: Dict mapping capture names to single AST values.
            variadic_values: Dict mapping capture names to lists of AST values.
        """
        super().__init__()
        self._values = values
        self._variadic_values = variadic_values

    # TODO: Implement variadic expansion for:
    # - Function arguments
    # - List/tuple/set elements
    # - Dict key-value pairs
    # - Block statements
