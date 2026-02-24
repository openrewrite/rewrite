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

"""Template engine for parsing and caching templates."""

from __future__ import annotations

import ast
import textwrap
from dataclasses import dataclass
from typing import Dict, List, Optional, Tuple, TYPE_CHECKING

from rewrite import random_id
from rewrite.java import J, Expression, Statement
from rewrite.java import tree as j

if TYPE_CHECKING:
    from rewrite.python.tree import CompilationUnit
    from rewrite.visitor import Cursor

from .capture import Capture
from .placeholder import substitute_placeholders
from .coordinates import PythonCoordinates, CoordinateMode

# Wrapper function name used to make template code parseable
WRAPPER_FUNCTION_NAME = "__WRAPPER__"


@dataclass(frozen=True)
class TemplateOptions:
    """Configuration options for template parsing."""
    imports: Tuple[str, ...] = ()
    context_sensitive: bool = False


class TemplateEngine:
    """
    Core engine for parsing and processing templates.

    Handles:
    - Placeholder substitution: {name} -> __placeholder_name__
    - Wrapper generation: def __WRAPPER__(): <template>
    - AST parsing via ParserVisitor
    - Tree extraction from wrapper
    - LRU caching for parsed templates
    """

    # Global template cache (code -> parsed tree)
    _cache: Dict[str, J] = {}
    _cache_max_size: int = 100

    @classmethod
    def get_template_tree(
        cls,
        code: str,
        captures: Dict[str, Capture],
        options: Optional[TemplateOptions] = None
    ) -> J:
        """
        Parse template code and return the extracted AST.

        Args:
            code: Template code with {name} placeholders.
            captures: Dict mapping capture names to Capture objects.
            options: Optional template configuration.

        Returns:
            The parsed AST node representing the template content.

        Raises:
            SyntaxError: If the template code is invalid Python.
            ValueError: If placeholder validation fails.
        """
        options = options or TemplateOptions()

        # Create cache key
        cache_key = cls._make_cache_key(code, captures, options)

        # Check cache
        if cache_key in cls._cache:
            return cls._cache[cache_key]

        # Substitute placeholders
        substituted_code, placeholder_map = substitute_placeholders(code, captures)

        # Determine if original is an expression (needed for extraction)
        dedented = textwrap.dedent(substituted_code).strip()
        is_expression = cls._is_expression(dedented)

        # Generate wrapper and parse
        wrapper_code = cls._generate_wrapper(substituted_code, options)
        compilation_unit = cls._parse_code(wrapper_code)

        # Extract template content from wrapper
        extracted = cls._extract_from_wrapper(compilation_unit, is_expression)

        # Cache and return
        if len(cls._cache) < cls._cache_max_size:
            cls._cache[cache_key] = extracted

        return extracted

    @classmethod
    def apply_substitutions(
        cls,
        template_tree: J,
        values: Dict[str, J],
        cursor: 'Cursor',
        coordinates: Optional[PythonCoordinates] = None
    ) -> Optional[J]:
        """
        Substitute placeholder identifiers with actual values.

        Args:
            template_tree: The parsed template AST.
            values: Dict mapping capture names to their AST values.
            cursor: Current cursor position.
            coordinates: Where/how to apply the template.

        Returns:
            The template with placeholders replaced by actual values.
        """
        from .replacement import PlaceholderReplacementVisitor

        # Replace placeholders with actual values
        visitor = PlaceholderReplacementVisitor(values)
        result = visitor.visit(template_tree, None)

        # Apply coordinates (wrap in statement if needed, etc.)
        if coordinates and result is not None:
            result = cls._apply_coordinates(result, cursor, coordinates)

        return result

    @classmethod
    def _make_cache_key(
        cls,
        code: str,
        captures: Dict[str, Capture],
        options: TemplateOptions
    ) -> str:
        """Generate a cache key from template components."""
        capture_names = ",".join(sorted(captures.keys()))
        imports_key = ",".join(sorted(options.imports))
        return f"{code}::{capture_names}::{imports_key}"

    @classmethod
    def _generate_wrapper(cls, code: str, options: TemplateOptions) -> str:
        """
        Generate a parseable Python wrapper for the template code.

        Args:
            code: Template code (with placeholders already substituted).
            options: Template options including imports.

        Returns:
            Complete Python source that can be parsed.
        """
        lines: List[str] = []

        # Add imports
        for imp in options.imports:
            lines.append(imp)

        # Dedent the code to handle indented template strings
        dedented = textwrap.dedent(code).strip()

        # Determine if this looks like an expression or statement
        # Try parsing as expression first
        is_expression = cls._is_expression(dedented)

        # Generate wrapper function
        if is_expression:
            # Expression: wrap with return
            lines.append(f"def {WRAPPER_FUNCTION_NAME}():")
            lines.append(f"    return {dedented}")
        else:
            # Statement(s): wrap directly
            lines.append(f"def {WRAPPER_FUNCTION_NAME}():")
            # Indent each line of the template
            for line in dedented.split('\n'):
                lines.append(f"    {line}")

        return '\n'.join(lines)

    @classmethod
    def _is_expression(cls, code: str) -> bool:
        """Check if code is a single expression (vs statement)."""
        try:
            ast.parse(code, mode='eval')
            return True
        except SyntaxError:
            return False

    @classmethod
    def _parse_code(cls, code: str) -> 'CompilationUnit':
        """
        Parse Python code into an LST CompilationUnit.

        Args:
            code: Python source code.

        Returns:
            CompilationUnit LST node.
        """
        from rewrite.python._parser_visitor import ParserVisitor

        tree = ast.parse(code)
        visitor = ParserVisitor(code)
        return visitor.visit(tree)

    @classmethod
    def _extract_from_wrapper(cls, cu: 'CompilationUnit', is_expression: bool) -> J:
        """
        Extract the template content from the wrapper function.

        Args:
            cu: CompilationUnit containing the wrapper function.
            is_expression: Whether the original template was an expression.

        Returns:
            The extracted template content (expression or statement(s)).

        Raises:
            ValueError: If wrapper function not found or has unexpected structure.
        """
        # Find the wrapper function
        # Note: cu.statements returns unwrapped Statement objects
        for stmt in cu.statements:
            if isinstance(stmt, j.MethodDeclaration):
                if stmt.name.simple_name == WRAPPER_FUNCTION_NAME:
                    return cls._extract_from_method_body(stmt, is_expression)

        raise ValueError(f"Wrapper function '{WRAPPER_FUNCTION_NAME}' not found in parsed template")

    @classmethod
    def _extract_from_method_body(cls, method: j.MethodDeclaration, is_expression: bool) -> J:
        """
        Extract content from the wrapper method body.

        Args:
            method: The wrapper method declaration.
            is_expression: Whether the original template was an expression.

        Returns:
            The extracted content.
        """
        body = method.body
        if body is None:
            raise ValueError("Wrapper function has no body")

        statements = body.statements
        if not statements:
            raise ValueError("Wrapper function body is empty")

        # If single statement
        # Note: body.statements returns unwrapped Statement objects
        if len(statements) == 1:
            single = statements[0]

            # If it's a return statement AND the original was an expression,
            # extract the expression from the return wrapper
            if is_expression and isinstance(single, j.Return):
                return_stmt = single
                if return_stmt.expression is not None:
                    return return_stmt.expression

            # Otherwise return the statement itself
            return single

        # Multiple statements - return the block contents
        # We return a list as a special marker, or we could wrap in a synthetic block
        # For now, just return the first statement (most common case)
        # TODO: Handle multiple statements properly
        return statements[0]

    @classmethod
    def _apply_coordinates(
        cls,
        result: J,
        cursor: 'Cursor',
        coordinates: PythonCoordinates
    ) -> J:
        """
        Apply coordinate-based adjustments to the result.

        This handles wrapping expressions in statements when needed,
        preserving prefixes, etc.

        Args:
            result: The template result.
            cursor: Current cursor position.
            coordinates: Insertion coordinates.

        Returns:
            The adjusted result.
        """
        from rewrite.python import tree as py

        # For replacement mode, preserve the original node's prefix
        if coordinates.mode == CoordinateMode.REPLACEMENT:
            original = coordinates.tree
            if hasattr(original, 'prefix') and hasattr(result, 'prefix'):
                # Replace the prefix to match original
                result = result.replace(prefix=original.prefix)

        # Check if we need to wrap expression in statement
        # This happens when inserting an expression where a statement is expected
        target = coordinates.tree
        if isinstance(result, Expression) and isinstance(target, Statement):
            # Wrap expression in ExpressionStatement
            result = py.ExpressionStatement(
                random_id(),
                result,
            )

        # Auto-format the result
        try:
            from ..format import maybe_auto_format
            original = coordinates.tree
            result = maybe_auto_format(
                original, result, None, None,
                cursor.parent if cursor.parent is not None else None
            )
        except (ValueError, AttributeError):
            # No CompilationUnit in cursor ancestry — skip formatting
            pass

        return result

    @classmethod
    def clear_cache(cls) -> None:
        """Clear the template cache."""
        cls._cache.clear()
