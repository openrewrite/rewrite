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

"""Category descriptor for recipe organization."""

from __future__ import annotations

from dataclasses import dataclass, field
from typing import FrozenSet

# Priority constants matching Java's CategoryDescriptor
LOWEST_PRECEDENCE = -1
DEFAULT_PRECEDENCE = 0
HIGHEST_PRECEDENCE = 2147483647  # Integer.MAX_VALUE


@dataclass(frozen=True)
class CategoryDescriptor:
    """
    Descriptor for a recipe category.

    Categories organize recipes hierarchically in the marketplace.
    Recipes are placed in categories based on their package structure
    or explicit category assignments.

    Attributes:
        display_name: Human-readable name for the category
        package_name: Dot-separated package path (e.g., "org.openrewrite.python")
        description: Markdown description of the category
        tags: Set of tags for filtering
        root: If True, this is a root category that may be hidden in display
        priority: Lower values have higher priority for conflict resolution
        synthetic: True if auto-generated, False if explicitly defined
    """

    display_name: str
    package_name: str = ""
    description: str = ""
    tags: FrozenSet[str] = field(default_factory=frozenset)
    root: bool = False
    priority: int = LOWEST_PRECEDENCE
    synthetic: bool = False
