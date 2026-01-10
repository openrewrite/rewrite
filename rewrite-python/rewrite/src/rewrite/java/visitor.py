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

from typing import TypeVar, Optional

from rewrite.java.support_types import J, Space
from rewrite.visitor import TreeVisitor

P = TypeVar("P")


class JavaVisitor(TreeVisitor[J, P]):
    """
    Base visitor for Java LST nodes.

    This visitor provides visit methods for all Java AST node types.
    Subclass this to implement Java-specific transformations.
    """

    def visit_space(self, space: Optional[Space], loc: Space.Location, p: P) -> Space:
        """Visit a space (whitespace and comments)."""
        if space is None:
            return Space.EMPTY
        return space

    # Many more visit methods would be defined here for each J type
    # For example: visit_identifier, visit_literal, visit_method_invocation, etc.
    # These will be added as we port more of the J tree types
