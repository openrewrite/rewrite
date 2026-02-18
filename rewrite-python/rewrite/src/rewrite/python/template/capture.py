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

"""Capture and RawCode classes for template placeholders."""

from __future__ import annotations

from dataclasses import dataclass
from typing import Callable, Optional, TypeVar, Generic, TYPE_CHECKING

if TYPE_CHECKING:
    from rewrite.java import J

T = TypeVar('T')


@dataclass(frozen=True)
class Capture(Generic[T]):
    """
    A capture specification for use in patterns and templates.

    Captures define named placeholders that can match AST nodes in patterns
    and be substituted in templates.

    Examples:
        # Simple capture
        expr = capture('expr')

        # Variadic capture (matches zero or more elements)
        args = capture('args', variadic=True)

        # Capture with constraint
        positive_int = capture('n', constraint=lambda n: is_positive_int(n))

        # Typed capture for documentation
        typed = capture('x', type_hint='int')
    """

    name: str
    variadic: bool = False
    min_count: Optional[int] = None
    max_count: Optional[int] = None
    constraint: Optional[Callable[[T], bool]] = None
    type_hint: Optional[str] = None

    def __hash__(self) -> int:
        # Exclude constraint from hash since functions aren't reliably hashable
        return hash((self.name, self.variadic, self.min_count, self.max_count, self.type_hint))

    def __eq__(self, other: object) -> bool:
        if not isinstance(other, Capture):
            return False
        return (
            self.name == other.name and
            self.variadic == other.variadic and
            self.min_count == other.min_count and
            self.max_count == other.max_count and
            self.type_hint == other.type_hint
        )


def capture(
    name: str,
    *,
    variadic: bool = False,
    min_count: Optional[int] = None,
    max_count: Optional[int] = None,
    constraint: Optional[Callable[[T], bool]] = None,
    type_hint: Optional[str] = None
) -> Capture[T]:
    """
    Create a capture specification for use in patterns and templates.

    Args:
        name: Name for the capture. Used to reference matched values.
        variadic: If True, matches zero or more elements (for argument lists, etc.).
        min_count: Minimum elements for variadic captures.
        max_count: Maximum elements for variadic captures.
        constraint: Predicate function to validate matched nodes.
        type_hint: Type annotation string for documentation.

    Returns:
        A Capture instance.

    Examples:
        # Named capture
        x = capture('x')

        # Variadic capture for function arguments
        args = capture('args', variadic=True)

        # Capture with constraint
        positive = capture('n', constraint=lambda n: n.value > 0)

        # Typed capture
        typed = capture('expr', type_hint='int')
    """
    return Capture(
        name=name,
        variadic=variadic,
        min_count=min_count,
        max_count=max_count,
        constraint=constraint,
        type_hint=type_hint,
    )


@dataclass(frozen=True)
class RawCode:
    """
    Raw code to be spliced into a template at construction time.

    Unlike captures (which are placeholders resolved at apply time),
    RawCode splices its content directly into the template string before
    parsing. This is useful for dynamic method names, operators, etc.

    Examples:
        # Dynamic method name from recipe options
        method_name = "warn"
        tmpl = template(f"logger.{raw(method_name)}(msg)")

        # Configurable operator
        op = ">="
        tmpl = template(f"x {raw(op)} y")
    """

    code: str


def raw(code: str) -> RawCode:
    """
    Create a RawCode instance for splice-time code insertion.

    Unlike captures (resolved at apply time), raw() splices code directly
    into the template string before parsing.

    Args:
        code: The code string to splice.

    Returns:
        A RawCode instance.

    Examples:
        # Dynamic method name
        method_name = "warn"
        tmpl = template(f"logger.{raw(method_name)}(msg)")

        # Configurable operator
        op = ">="
        tmpl = template(f"x {raw(op)} y")
    """
    return RawCode(code=code)
