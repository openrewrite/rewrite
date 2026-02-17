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

"""Template class and builder for generating Python AST."""

from __future__ import annotations

from typing import Dict, List, Optional, Union, TYPE_CHECKING, cast

from rewrite.java import J
from .capture import Capture
from .coordinates import PythonCoordinates
from .engine import TemplateEngine, TemplateOptions

if TYPE_CHECKING:
    from rewrite.visitor import Cursor
    from .pattern import MatchResult


class Template:
    """
    Template for generating Python AST nodes.

    Templates parse a code string with placeholders and produce AST nodes
    that can be substituted into the tree.

    Examples:
        # Simple template
        tmpl = template("x + 1")
        result = tmpl.apply(cursor)

        # Template with capture from pattern match
        expr = capture('expr')
        tmpl = template("print({expr})", expr=expr)
        result = tmpl.apply(cursor, values=match_result)

        # Template with imports
        tmpl = template(
            "datetime.now()",
            imports=["from datetime import datetime"]
        )
    """

    def __init__(
        self,
        code: str,
        captures: Optional[Dict[str, Capture]] = None,
        imports: Optional[List[str]] = None,
    ):
        """
        Initialize a template.

        Args:
            code: Python code with {name} placeholders.
            captures: Dict mapping capture names to Capture objects.
            imports: Import statements to include for parsing.
        """
        self._code = code
        self._captures = captures or {}
        self._options = TemplateOptions(
            imports=tuple(imports) if imports else (),
        )
        self._cached_tree: Optional[J] = None

    @property
    def code(self) -> str:
        """The template code string."""
        return self._code

    @property
    def captures(self) -> Dict[str, Capture]:
        """The captures defined for this template."""
        return self._captures

    def get_tree(self) -> J:
        """
        Get the parsed template tree (cached).

        Returns:
            The parsed AST node representing the template.
        """
        if self._cached_tree is None:
            self._cached_tree = TemplateEngine.get_template_tree(
                self._code,
                self._captures,
                self._options,
            )
        return self._cached_tree

    def apply(
        self,
        cursor: 'Cursor',
        *,
        values: Optional[Union['MatchResult', Dict[str, J]]] = None,
        coordinates: Optional[PythonCoordinates] = None,
    ) -> Optional[J]:
        """
        Apply this template, returning the generated AST node.

        Args:
            cursor: Current position in the AST.
            values: Captured values from a pattern match, or a dict of values.
            coordinates: Where/how to insert (default: replace current).

        Returns:
            The generated AST node.

        Examples:
            # Simple application
            result = tmpl.apply(cursor)

            # With values from pattern match
            result = tmpl.apply(cursor, values=match)

            # With explicit coordinates
            result = tmpl.apply(cursor, coordinates=PythonCoordinates.after(node))
        """
        # Get the template tree
        template_tree = self.get_tree()

        # Convert MatchResult to dict if needed
        values_dict: Dict[str, J] = {}
        if values is not None:
            if isinstance(values, dict):
                values_dict = cast(Dict[str, J], values)
            else:
                # Assume it's a MatchResult
                values_dict = values.as_dict()

        # Apply substitutions
        if values_dict:
            result = TemplateEngine.apply_substitutions(
                template_tree,
                values_dict,
                cursor,
                coordinates,
            )
        else:
            result = template_tree

        # If no explicit coordinates, create default replacement coordinates
        if coordinates is None and cursor is not None:
            tree = cursor.value
            if tree is not None:
                result = TemplateEngine.apply_substitutions(
                    result if values_dict else template_tree,
                    values_dict,
                    cursor,
                    PythonCoordinates.replace(tree),
                )

        return result

    @staticmethod
    def builder() -> 'TemplateBuilder':
        """
        Create a new template builder.

        Returns:
            A TemplateBuilder instance.

        Examples:
            tmpl = (Template.builder()
                .code("logger.")
                .param(capture('method'))
                .code("(")
                .param(capture('msg'))
                .code(")")
                .build())
        """
        return TemplateBuilder()


class TemplateBuilder:
    """
    Builder for creating templates programmatically.

    Use when template structure is not known at compile time,
    or when building templates dynamically from recipe options.

    Examples:
        # Conditional construction
        builder = Template.builder().code("print(")
        if include_prefix:
            builder.code("'PREFIX: ' + ")
        builder.code("{msg})").param(capture('msg'))
        tmpl = builder.build()

        # Dynamic method name
        method = "warn"  # from recipe options
        tmpl = (Template.builder()
            .code("logger.")
            .raw(method)
            .code("({msg})")
            .param(capture('msg'))
            .build())
    """

    def __init__(self):
        """Initialize an empty builder."""
        self._parts: List[str] = []
        self._captures: Dict[str, Capture] = {}
        self._imports: List[str] = []

    def code(self, code: str) -> 'TemplateBuilder':
        """
        Add a static code segment.

        Args:
            code: Python code string (may contain {name} placeholders).

        Returns:
            This builder for chaining.
        """
        self._parts.append(code)
        return self

    def param(self, cap: Capture) -> 'TemplateBuilder':
        """
        Add a capture parameter.

        The capture's name will be used as a placeholder in the template.

        Args:
            cap: A Capture instance.

        Returns:
            This builder for chaining.
        """
        self._captures[cap.name] = cap
        return self

    def raw(self, code: str) -> 'TemplateBuilder':
        """
        Add raw code spliced at build time.

        Unlike regular code segments, this is not parsed for placeholders.
        Use for dynamic method names, operators, etc.

        Args:
            code: Raw code to splice.

        Returns:
            This builder for chaining.
        """
        self._parts.append(code)
        return self

    def imports(self, *import_statements: str) -> 'TemplateBuilder':
        """
        Add import statements for type resolution.

        Args:
            *import_statements: Import statements.

        Returns:
            This builder for chaining.
        """
        self._imports.extend(import_statements)
        return self

    def build(self) -> Template:
        """
        Build the template.

        Returns:
            A Template instance.
        """
        code = ''.join(self._parts)
        return Template(
            code=code,
            captures=self._captures,
            imports=self._imports,
        )


def template(
    code,
    *,
    imports: Optional[List[str]] = None,
    **captures: Capture
) -> Template:
    """
    Create a template from Python code.

    This is the primary factory function for creating templates.

    Args:
        code: Python code with {name} placeholders, or a t-string
              (Python 3.14+) with Capture/RawCode interpolations.
        imports: Optional import statements for type resolution.
        **captures: Named capture specifications (not allowed with t-strings).

    Returns:
        A Template instance.

    Examples:
        # Simple template
        tmpl = template("x + 1")

        # Template with captures
        expr = capture('expr')
        tmpl = template("print({expr})", expr=expr)

        # With t-string (Python 3.14+)
        expr = capture('expr')
        tmpl = template(t"print({expr})")

        # With imports
        tmpl = template(
            "datetime.now()",
            imports=["from datetime import datetime"]
        )
    """
    from ._tstring_support import is_tstring, convert_tstring

    if is_tstring(code):
        if captures:
            raise TypeError(
                "Cannot pass keyword captures when using a t-string; "
                "interpolate Capture objects directly in the t-string instead"
            )
        code, captures = convert_tstring(code)

    return Template(
        code=code,
        captures=captures,
        imports=imports,
    )
