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

"""Pattern matching comparator for comparing pattern and target ASTs."""

from __future__ import annotations

from typing import Dict, Optional, TYPE_CHECKING, cast

from rewrite.java import J
from rewrite.java import tree as j
from rewrite.python import tree as py
from .capture import Capture
from .placeholder import from_placeholder

if TYPE_CHECKING:
    from rewrite.visitor import Cursor


class PatternMatchingComparator:
    """
    Compares a pattern AST against a target AST, extracting captures.

    The comparator walks both trees in parallel, comparing node structures
    and types. When it encounters a placeholder identifier in the pattern,
    it captures the corresponding subtree from the target.

    Examples:
        comparator = PatternMatchingComparator(captures)
        result = comparator.match(pattern_tree, target_tree, cursor)
        if result:
            # result is a dict of capture_name -> captured_value
            x_value = result['x']
    """

    def __init__(
        self,
        captures: Dict[str, Capture],
        lenient_type_matching: bool = True
    ):
        """
        Initialize the comparator.

        Args:
            captures: Dict mapping capture names to Capture objects.
            lenient_type_matching: If True, ignore type differences when comparing.
        """
        self._captures = captures
        self._lenient_type_matching = lenient_type_matching
        self._captured: Dict[str, J] = {}
        self._debug = False

    def match(
        self,
        pattern: J,
        target: J,
        cursor: 'Cursor',
        *,
        debug: bool = False
    ) -> Optional[Dict[str, J]]:
        """
        Match pattern against target, returning captures if successful.

        Args:
            pattern: The pattern AST to match.
            target: The target AST to match against.
            cursor: Cursor at the target's position.
            debug: Enable debug logging.

        Returns:
            Dict of captured values if matched, None otherwise.
        """
        self._captured = {}
        self._debug = debug

        if self._compare(pattern, target, cursor):
            # Validate all required captures were matched
            for name, cap in self._captures.items():
                if name not in self._captured and not cap.variadic:
                    if self._debug:
                        print(f"Required capture '{name}' was not matched")
                    return None

            # Validate constraints
            for name, value in self._captured.items():
                cap = self._captures.get(name)
                if cap and cap.constraint:
                    if not cap.constraint(value):
                        if self._debug:
                            print(f"Capture '{name}' failed constraint")
                        return None

            return self._captured
        return None

    def _compare(
        self,
        pattern: Optional[J],
        target: Optional[J],
        cursor: 'Cursor'
    ) -> bool:
        """
        Recursively compare pattern and target nodes.

        Args:
            pattern: Pattern node (may contain placeholders).
            target: Target node to compare against.
            cursor: Current cursor position.

        Returns:
            True if nodes match.
        """
        # Both None is a match
        if pattern is None and target is None:
            return True

        # Only one None is not a match
        if pattern is None or target is None:
            return False

        # Check if pattern is a capture placeholder
        if isinstance(pattern, j.Identifier):
            capture_name = from_placeholder(pattern.simple_name)
            if capture_name is not None:
                return self._capture_node(capture_name, target)

        # Types must match (unless lenient)
        if type(pattern) != type(target):
            if self._debug:
                print(f"Type mismatch: {type(pattern).__name__} vs {type(target).__name__}")
            return False

        # Dispatch to type-specific comparison
        if isinstance(pattern, j.Identifier):
            return self._compare_identifier(pattern, cast(j.Identifier, target))
        elif isinstance(pattern, j.Literal):
            return self._compare_literal(pattern, cast(j.Literal, target))
        elif isinstance(pattern, j.MethodInvocation):
            return self._compare_method_invocation(pattern, cast(j.MethodInvocation, target), cursor)
        elif isinstance(pattern, j.FieldAccess):
            return self._compare_field_access(pattern, cast(j.FieldAccess, target), cursor)
        elif isinstance(pattern, j.Binary):
            return self._compare_binary(pattern, cast(j.Binary, target), cursor)
        elif isinstance(pattern, j.Unary):
            return self._compare_unary(pattern, cast(j.Unary, target), cursor)
        elif isinstance(pattern, j.Assignment):
            return self._compare_assignment(pattern, cast(j.Assignment, target), cursor)
        elif isinstance(pattern, j.Parentheses):
            return self._compare_parentheses(pattern, cast(j.Parentheses, target), cursor)
        elif isinstance(pattern, j.Return):
            return self._compare_return(pattern, cast(j.Return, target), cursor)
        elif isinstance(pattern, py.ExpressionStatement):
            return self._compare_expression_statement(pattern, cast(py.ExpressionStatement, target), cursor)
        elif isinstance(pattern, py.Binary):
            return self._compare_python_binary(pattern, cast(py.Binary, target), cursor)
        elif isinstance(pattern, py.CollectionLiteral):
            return self._compare_collection_literal(pattern, cast(py.CollectionLiteral, target), cursor)
        elif isinstance(pattern, py.DictLiteral):
            return self._compare_dict_literal(pattern, cast(py.DictLiteral, target), cursor)
        else:
            # Default: no deep comparison, types matched
            if self._debug:
                print(f"No specific comparison for {type(pattern).__name__}, assuming match")
            return True

    def _capture_node(self, name: str, target: J) -> bool:
        """
        Capture a target node for a placeholder.

        Args:
            name: The capture name.
            target: The target node to capture.

        Returns:
            True if capture succeeded.
        """
        if name not in self._captures:
            if self._debug:
                print(f"Unknown capture: {name}")
            return False

        cap = self._captures[name]

        # Check if this capture already has a value
        if name in self._captured:
            # Must match the existing captured value
            existing = self._captured[name]
            # For now, require exact same node (by id)
            # TODO: implement semantic equality
            if self._debug:
                print(f"Capture '{name}' already has value, checking match")
            return existing.id == target.id

        # New capture
        self._captured[name] = target
        if self._debug:
            print(f"Captured '{name}': {type(target).__name__}")
        return True

    def _compare_identifier(self, pattern: j.Identifier, target: j.Identifier) -> bool:
        """Compare two identifiers."""
        return pattern.simple_name == target.simple_name

    def _compare_literal(self, pattern: j.Literal, target: j.Literal) -> bool:
        """Compare two literals."""
        return pattern.value == target.value

    def _compare_method_invocation(
        self,
        pattern: j.MethodInvocation,
        target: j.MethodInvocation,
        cursor: 'Cursor'
    ) -> bool:
        """Compare two method invocations."""
        # Compare select (receiver)
        if not self._compare(pattern.select, target.select, cursor):
            return False

        # Compare method name
        if not self._compare(pattern.name, target.name, cursor):
            return False

        # Compare arguments
        return self._compare_arguments(pattern.padding.arguments, target.padding.arguments, cursor)

    def _compare_arguments(
        self,
        pattern_args: Optional[j.JContainer],
        target_args: Optional[j.JContainer],
        cursor: 'Cursor'
    ) -> bool:
        """Compare argument containers."""
        if pattern_args is None and target_args is None:
            return True
        if pattern_args is None or target_args is None:
            return False

        pattern_elements = pattern_args.elements
        target_elements = target_args.elements

        # Check for variadic capture
        if len(pattern_elements) == 1:
            pattern_arg = pattern_elements[0].element
            if isinstance(pattern_arg, j.Identifier):
                cap_name = from_placeholder(pattern_arg.simple_name)
                if cap_name and self._captures.get(cap_name, Capture(name=cap_name)).variadic:
                    # Variadic capture - capture all target arguments
                    # TODO: implement proper variadic handling
                    return True

        # Non-variadic: must have same number of arguments
        if len(pattern_elements) != len(target_elements):
            if self._debug:
                print(f"Argument count mismatch: {len(pattern_elements)} vs {len(target_elements)}")
            return False

        # Compare each argument
        for p_elem, t_elem in zip(pattern_elements, target_elements):
            if not self._compare(p_elem.element, t_elem.element, cursor):
                return False

        return True

    def _compare_field_access(
        self,
        pattern: j.FieldAccess,
        target: j.FieldAccess,
        cursor: 'Cursor'
    ) -> bool:
        """Compare two field accesses."""
        # Compare target (receiver)
        if not self._compare(pattern.target, target.target, cursor):
            return False

        # Compare name
        return self._compare(pattern.name, target.name, cursor)

    def _compare_binary(
        self,
        pattern: j.Binary,
        target: j.Binary,
        cursor: 'Cursor'
    ) -> bool:
        """Compare two Java binary expressions."""
        # Compare operator
        if pattern.operator != target.operator:
            return False

        # Compare operands
        if not self._compare(pattern.left, target.left, cursor):
            return False

        return self._compare(pattern.right, target.right, cursor)

    def _compare_python_binary(
        self,
        pattern: py.Binary,
        target: py.Binary,
        cursor: 'Cursor'
    ) -> bool:
        """Compare two Python binary expressions."""
        # Compare operator
        if pattern.operator != target.operator:
            return False

        # Compare operands
        if not self._compare(pattern.left, target.left, cursor):
            return False

        return self._compare(pattern.right, target.right, cursor)

    def _compare_unary(
        self,
        pattern: j.Unary,
        target: j.Unary,
        cursor: 'Cursor'
    ) -> bool:
        """Compare two unary expressions."""
        if pattern.operator != target.operator:
            return False

        return self._compare(pattern.expression, target.expression, cursor)

    def _compare_assignment(
        self,
        pattern: j.Assignment,
        target: j.Assignment,
        cursor: 'Cursor'
    ) -> bool:
        """Compare two assignments."""
        if not self._compare(pattern.variable, target.variable, cursor):
            return False

        return self._compare(pattern.assignment, target.assignment, cursor)

    def _compare_parentheses(
        self,
        pattern: j.Parentheses,
        target: j.Parentheses,
        cursor: 'Cursor'
    ) -> bool:
        """Compare two parenthesized expressions."""
        return self._compare(pattern.tree, target.tree, cursor)

    def _compare_return(
        self,
        pattern: j.Return,
        target: j.Return,
        cursor: 'Cursor'
    ) -> bool:
        """Compare two return statements."""
        return self._compare(pattern.expression, target.expression, cursor)

    def _compare_expression_statement(
        self,
        pattern: py.ExpressionStatement,
        target: py.ExpressionStatement,
        cursor: 'Cursor'
    ) -> bool:
        """Compare two Python expression statements."""
        return self._compare(pattern.expression, target.expression, cursor)

    def _compare_collection_literal(
        self,
        pattern: py.CollectionLiteral,
        target: py.CollectionLiteral,
        cursor: 'Cursor'
    ) -> bool:
        """Compare two Python collection literals."""
        # Compare kind (list, set, tuple)
        if pattern.kind != target.kind:
            return False

        # Compare elements
        pattern_elements = pattern.elements
        target_elements = target.elements

        if len(pattern_elements) != len(target_elements):
            return False

        for p_elem, t_elem in zip(pattern_elements, target_elements):
            if not self._compare(p_elem, t_elem, cursor):
                return False

        return True

    def _compare_dict_literal(
        self,
        pattern: py.DictLiteral,
        target: py.DictLiteral,
        cursor: 'Cursor'
    ) -> bool:
        """Compare two Python dict literals."""
        pattern_elements = pattern.elements
        target_elements = target.elements

        if len(pattern_elements) != len(target_elements):
            return False

        for p_elem, t_elem in zip(pattern_elements, target_elements):
            if not self._compare(p_elem, t_elem, cursor):
                return False

        return True
