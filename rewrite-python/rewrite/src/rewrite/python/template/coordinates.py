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

"""Coordinates for template application positioning."""

from __future__ import annotations

from dataclasses import dataclass
from enum import Enum
from typing import Optional, Callable, TypeVar, TYPE_CHECKING

if TYPE_CHECKING:
    from rewrite.java import J

T = TypeVar('T', bound='J')


class CoordinateMode(Enum):
    """Mode for template application."""
    REPLACEMENT = "replacement"
    BEFORE = "before"
    AFTER = "after"


class CoordinateLocation(Enum):
    """Location hints for template insertion."""
    EXPRESSION_PREFIX = "expression_prefix"
    STATEMENT_PREFIX = "statement_prefix"
    BLOCK_END = "block_end"
    ANNOTATION_PREFIX = "annotation_prefix"
    DECORATOR_PREFIX = "decorator_prefix"
    IMPORT_PREFIX = "import_prefix"
    CLASS_BODY = "class_body"
    FUNCTION_BODY = "function_body"


@dataclass(frozen=True)
class PythonCoordinates:
    """
    Coordinates specifying where and how to apply a template.

    Attributes:
        tree: The target AST node for insertion/replacement.
        mode: How to apply the template (replace, before, or after).
        location: Optional location hint for precise positioning.
        comparator: Optional comparator for ordering when inserting.
    """

    tree: 'J'
    mode: CoordinateMode = CoordinateMode.REPLACEMENT
    location: Optional[CoordinateLocation] = None
    comparator: Optional[Callable[[T, T], int]] = None

    def is_replacement(self) -> bool:
        """Check if this is a replacement operation."""
        return self.mode == CoordinateMode.REPLACEMENT

    def is_before(self) -> bool:
        """Check if this is an insert-before operation."""
        return self.mode == CoordinateMode.BEFORE

    def is_after(self) -> bool:
        """Check if this is an insert-after operation."""
        return self.mode == CoordinateMode.AFTER

    @staticmethod
    def replace(tree: 'J') -> 'PythonCoordinates':
        """Create coordinates for replacing a node."""
        return PythonCoordinates(tree=tree, mode=CoordinateMode.REPLACEMENT)

    @staticmethod
    def before(tree: 'J') -> 'PythonCoordinates':
        """Create coordinates for inserting before a node."""
        return PythonCoordinates(tree=tree, mode=CoordinateMode.BEFORE)

    @staticmethod
    def after(tree: 'J') -> 'PythonCoordinates':
        """Create coordinates for inserting after a node."""
        return PythonCoordinates(tree=tree, mode=CoordinateMode.AFTER)
