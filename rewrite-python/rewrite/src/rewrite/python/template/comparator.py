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

"""
Layered comparator for matching pattern ASTs against target ASTs.

Architecture (3-layer):
  PythonComparatorVisitor        — Layer 1: Structural comparison via dataclasses.fields()
    └─ PythonSemanticComparator  — Layer 2: Type-aware + lenient matching
         └─ PatternMatchingComparator — Layer 3: Capture/placeholder handling
"""

from __future__ import annotations

import dataclasses
import functools
from typing import Dict, List, Optional, TYPE_CHECKING, Union, cast

from rewrite.java import J, JavaType, JContainer, JLeftPadded, JRightPadded, Space
from rewrite.java import tree as j
from .capture import Capture
from .placeholder import from_placeholder

if TYPE_CHECKING:
    from rewrite.visitor import Cursor

# ──────────────────────────────────────────────────────────────────────────────
# Layer 1: Structural comparison
# ──────────────────────────────────────────────────────────────────────────────

_SKIP_FIELDS = frozenset({'_id', '_prefix', '_markers', '_padding'})
_get_dataclass_fields = functools.lru_cache(maxsize=None)(dataclasses.fields)


class PythonComparatorVisitor:
    """
    Generic structural comparison of two AST trees.

    Iterates ``dataclasses.fields()`` on each node, skipping identity/formatting
    fields (_id, _prefix, _markers, _padding) and recursing into child nodes,
    padded wrappers, containers, and lists.
    """

    def __init__(self) -> None:
        self._debug = False

    def compare(
        self,
        tree1: J,
        tree2: J,
        cursor: Cursor,
        *,
        debug: bool = False
    ) -> bool:
        self._debug = debug
        return self._compare(tree1, tree2, cursor)

    # -- core dispatch --------------------------------------------------------

    def _compare(
        self,
        pattern: Optional[J],
        target: Optional[J],
        cursor: Cursor,
    ) -> bool:
        if pattern is None and target is None:
            return True
        if pattern is None or target is None:
            if self._debug:
                print(
                    f"None mismatch: pattern="
                    f"{'None' if pattern is None else type(pattern).__name__}, "
                    f"target="
                    f"{'None' if target is None else type(target).__name__}"
                )
            return False

        if type(pattern) != type(target):
            if self._debug:
                print(
                    f"Type mismatch: {type(pattern).__name__} "
                    f"vs {type(target).__name__}"
                )
            return False

        # Special cases
        if isinstance(pattern, j.Literal):
            return self._compare_literal(
                cast(j.Literal, pattern), cast(j.Literal, target)
            )
        if isinstance(pattern, j.Empty):
            return True

        # Default: generic structural comparison
        return self._compare_fields(pattern, target, cursor)

    # -- literal (special case) -----------------------------------------------

    def _compare_literal(
        self, pattern: j.Literal, target: j.Literal
    ) -> bool:
        """Compare two literals.

        Uses a two-level comparison:
        1. Value types must match (prevents cross-type false positives like
           None vs b"" where both might serialize to the same representation).
        2. When both values are None (Ellipsis, None keyword, or literals with
           unicode escapes all store value=None), fall back to value_source
           comparison to distinguish them.
        """
        if type(pattern.value) != type(target.value):
            return False
        if pattern.value is None:
            return pattern.value_source == target.value_source
        return pattern.value == target.value

    # -- generic field iteration ----------------------------------------------

    def _compare_fields(self, pattern, target, cursor: Cursor) -> bool:
        if not dataclasses.is_dataclass(pattern):
            return pattern == target
        for f in _get_dataclass_fields(type(pattern)):
            if f.name in _SKIP_FIELDS:
                continue
            p_val = getattr(pattern, f.name)
            t_val = getattr(target, f.name)
            if not self._compare_value(p_val, t_val, f.name, cursor):
                return False
        return True

    def _compare_value(self, p_val, t_val, field_name: str, cursor: Cursor) -> bool:
        # None
        if p_val is None and t_val is None:
            return True
        if p_val is None or t_val is None:
            if self._debug:
                print(f"None mismatch for field '{field_name}'")
            return False

        # Formatting — skip.  All Space-typed fields are purely formatting
        # (e.g., _negation on py.Binary is Optional[Space]).  This complements
        # _SKIP_FIELDS which handles the universal _prefix/_markers by name.
        if isinstance(p_val, Space):
            return True

        # Padded wrappers — compare element only
        if isinstance(p_val, JLeftPadded):
            return self._compare_value(
                p_val.element, t_val.element, field_name, cursor
            )
        if isinstance(p_val, JRightPadded):
            return self._compare_value(
                p_val.element, t_val.element, field_name, cursor
            )

        # Container — compare elements pairwise
        if isinstance(p_val, JContainer):
            return self._compare_container(p_val, t_val, field_name, cursor)

        # AST node — recursive structural compare
        if isinstance(p_val, J):
            return self._compare(p_val, t_val, cursor)

        # List — pairwise
        if isinstance(p_val, list):
            return self._compare_list(p_val, t_val, field_name, cursor)

        # Enum / primitive — direct equality
        return p_val == t_val

    def _compare_container(
        self,
        p_container: JContainer,
        t_container: JContainer,
        field_name: str,
        cursor: Cursor,
    ) -> bool:
        p_padded = p_container._elements
        t_padded = t_container._elements
        if len(p_padded) != len(t_padded):
            if self._debug:
                print(
                    f"Container size mismatch for '{field_name}': "
                    f"{len(p_padded)} vs {len(t_padded)}"
                )
            return False
        for p_rp, t_rp in zip(p_padded, t_padded):
            if not self._compare(p_rp.element, t_rp.element, cursor):
                return False
        return True

    def _compare_list(
        self,
        p_list: list,
        t_list: list,
        field_name: str,
        cursor: Cursor,
    ) -> bool:
        if len(p_list) != len(t_list):
            if self._debug:
                print(
                    f"List size mismatch for '{field_name}': "
                    f"{len(p_list)} vs {len(t_list)}"
                )
            return False
        for p_elem, t_elem in zip(p_list, t_list):
            if not self._compare_value(p_elem, t_elem, field_name, cursor):
                return False
        return True


# ──────────────────────────────────────────────────────────────────────────────
# Layer 2: Semantic / type-aware comparison
# ──────────────────────────────────────────────────────────────────────────────

_TYPE_FIELDS = frozenset({'_type', '_method_type', '_field_type'})


class PythonSemanticComparator(PythonComparatorVisitor):
    """
    Adds type attribution awareness on top of structural comparison.

    - Type fields (_type, _method_type, _field_type) are compared via
      ``_compare_types()`` which allows one-sided None in lenient mode.
    - MethodInvocations can be matched by FQN when ``method_type`` is
      available, allowing different import styles to still match.
    """

    def __init__(self, lenient_type_matching: bool = True) -> None:
        super().__init__()
        self._lenient_type_matching = lenient_type_matching

    # -- dispatch override (MethodInvocation) ---------------------------------

    def _compare(self, pattern, target, cursor: Cursor) -> bool:
        if (
            pattern is not None
            and target is not None
            and isinstance(pattern, j.MethodInvocation)
            and isinstance(target, j.MethodInvocation)
        ):
            return self._compare_method_invocation(
                cast(j.MethodInvocation, pattern),
                cast(j.MethodInvocation, target),
                cursor,
            )
        return super()._compare(pattern, target, cursor)

    # -- type field override --------------------------------------------------

    def _compare_value(self, p_val, t_val, field_name: str, cursor: Cursor) -> bool:
        if field_name in _TYPE_FIELDS:
            return self._compare_types(p_val, t_val)
        return super()._compare_value(p_val, t_val, field_name, cursor)

    # -- type comparison ------------------------------------------------------

    def _compare_types(self, p_type, t_type) -> bool:
        if p_type is None and t_type is None:
            return True
        if p_type is None or t_type is None:
            return self._lenient_type_matching

        # Both non-None
        if isinstance(p_type, JavaType.Primitive) and isinstance(
            t_type, JavaType.Primitive
        ):
            return p_type == t_type

        if isinstance(p_type, JavaType.Method) and isinstance(
            t_type, JavaType.Method
        ):
            return self._compare_method_types(p_type, t_type)

        # FullyQualified and subclasses
        if hasattr(p_type, 'fully_qualified_name') and hasattr(
            t_type, 'fully_qualified_name'
        ):
            return p_type.fully_qualified_name == t_type.fully_qualified_name

        return self._lenient_type_matching

    def _compare_method_types(
        self, p_type: JavaType.Method, t_type: JavaType.Method
    ) -> bool:
        if p_type.name != t_type.name:
            return False
        p_decl = p_type.declaring_type
        t_decl = t_type.declaring_type
        if p_decl is not None and t_decl is not None:
            if hasattr(p_decl, 'fully_qualified_name') and hasattr(
                t_decl, 'fully_qualified_name'
            ):
                return (
                    p_decl.fully_qualified_name == t_decl.fully_qualified_name
                )
        return self._lenient_type_matching

    # -- method invocation (FQN-aware) ----------------------------------------

    def _compare_method_invocation(
        self,
        pattern: j.MethodInvocation,
        target: j.MethodInvocation,
        cursor: Cursor,
    ) -> bool:
        p_mt = pattern.method_type
        t_mt = target.method_type
        if p_mt is not None and t_mt is not None:
            p_decl = p_mt.declaring_type
            t_decl = t_mt.declaring_type
            if (
                p_decl is not None
                and t_decl is not None
                and hasattr(p_decl, 'fully_qualified_name')
                and hasattr(t_decl, 'fully_qualified_name')
            ):
                if (
                    p_decl.fully_qualified_name == t_decl.fully_qualified_name
                    and p_mt.name == t_mt.name
                ):
                    # FQN match — skip select comparison (allows different
                    # import styles to match, e.g. os.path.join vs join).
                    return self._compare_arguments(
                        pattern.padding.arguments,
                        target.padding.arguments,
                        cursor,
                    )

        # Structural comparison
        if not self._compare(pattern.select, target.select, cursor):
            return False
        if not self._compare(pattern.name, target.name, cursor):
            return False
        return self._compare_arguments(
            pattern.padding.arguments, target.padding.arguments, cursor
        )

    def _compare_arguments(
        self,
        pattern_args: Optional[JContainer],
        target_args: Optional[JContainer],
        cursor: Cursor,
    ) -> bool:
        """Compare argument containers (overridable by Layer 3)."""
        if pattern_args is None and target_args is None:
            return True
        if pattern_args is None or target_args is None:
            return False
        return self._compare_container(
            pattern_args, target_args, '_arguments', cursor
        )


# ──────────────────────────────────────────────────────────────────────────────
# Layer 3: Pattern matching with captures
# ──────────────────────────────────────────────────────────────────────────────


class PatternMatchingComparator(PythonSemanticComparator):
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
        lenient_type_matching: bool = True,
    ) -> None:
        super().__init__(lenient_type_matching)
        self._captures = captures
        self._captured: Dict[str, Union[J, List[J]]] = {}

    def match(
        self,
        pattern: J,
        target: J,
        cursor: Cursor,
        *,
        debug: bool = False,
    ) -> Optional[Dict[str, Union[J, List[J]]]]:
        """
        Match pattern against target, returning captures if successful.

        Args:
            pattern: The pattern AST to match.
            target: The target AST to match against.
            cursor: Cursor at the target's position.
            debug: Enable debug logging.

        Returns:
            Dict of captured values if matched, None otherwise.
            Scalar captures map to a single ``J`` node; variadic captures
            map to a ``List[J]`` of matched elements.
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

    # -- dispatch override (capture check first) ------------------------------

    def _compare(self, pattern, target, cursor: Cursor) -> bool:
        # Check for capture placeholder BEFORE structural comparison
        if pattern is not None and isinstance(pattern, j.Identifier):
            capture_name = from_placeholder(pattern.simple_name)
            if capture_name is not None:
                if target is None:
                    if self._debug:
                        print(f"Capture '{capture_name}' matched against None target")
                    return False
                return self._capture_node(capture_name, target)

        return super()._compare(pattern, target, cursor)

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

        # Check if this capture already has a value.  Uses AST node identity
        # (UUID), not structural equality — intentionally strict so that the
        # same capture in a pattern must refer to the exact same target node.
        if name in self._captured:
            existing = self._captured[name]
            if self._debug:
                print(f"Capture '{name}' already has value, checking match")
            return existing.id == target.id

        # New capture
        self._captured[name] = target
        if self._debug:
            print(f"Captured '{name}': {type(target).__name__}")
        return True

    # -- argument override (variadic capture) ---------------------------------

    def _compare_arguments(
        self,
        pattern_args: Optional[JContainer],
        target_args: Optional[JContainer],
        cursor: Cursor,
    ) -> bool:
        if pattern_args is None and target_args is None:
            return True
        if pattern_args is None or target_args is None:
            return False

        p_padded = pattern_args._elements
        t_padded = target_args._elements

        # Variadic capture: matches any number of arguments and records
        # them as a List[J] in self._captured so they can be extracted
        # from the match result and spliced into templates.
        if len(p_padded) == 1:
            pattern_arg = p_padded[0].element
            if isinstance(pattern_arg, j.Identifier):
                cap_name = from_placeholder(pattern_arg.simple_name)
                if cap_name:
                    cap = self._captures.get(cap_name)
                    if cap is not None and cap.variadic:
                        self._captured[cap_name] = [
                            rp.element for rp in t_padded
                            if not isinstance(rp.element, j.Empty)
                        ]
                        if self._debug:
                            print(
                                f"Variadic capture '{cap_name}': "
                                f"{len(t_padded)} elements"
                            )
                        return True

        # Non-variadic: must have same number of arguments
        if len(p_padded) != len(t_padded):
            if self._debug:
                print(
                    f"Argument count mismatch: "
                    f"{len(p_padded)} vs {len(t_padded)}"
                )
            return False

        for p_rp, t_rp in zip(p_padded, t_padded):
            if not self._compare(p_rp.element, t_rp.element, cursor):
                return False

        return True
