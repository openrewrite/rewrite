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

"""Parenthesization of expressions spliced into a slot that would reparse them."""

from __future__ import annotations

from dataclasses import dataclass
from typing import Optional
from uuid import UUID

from rewrite.java import J, Expression
from rewrite.java import tree as j
from rewrite.java.markers import OmitParentheses
from rewrite.java.support_types import JRightPadded, Space
from rewrite.markers import Markers
from rewrite.python import tree as py
from rewrite.utils import random_id, replace_if_changed


class Precedence:
    """Python operator precedence, as ordered by the reference grammar; higher binds tighter."""

    GENERATOR = 1
    """`x for x in y` without parentheses, legal only as a call's sole argument."""
    YIELD = 2
    """`yield x`, `yield from x`"""
    TUPLE = 3
    """`a, b` without parentheses."""
    NAMED_EXPRESSION = 4
    """`:=`"""
    LAMBDA = 5
    CONDITIONAL = 6
    """`a if c else b`"""
    OR = 7
    AND = 8
    NOT = 9
    COMPARISON = 10
    """`<` `>` `<=` `>=` `==` `!=` `in` `not in` `is` `is not`"""
    BIT_OR = 11
    BIT_XOR = 12
    BIT_AND = 13
    SHIFT = 14
    """`<<` `>>`"""
    ADDITIVE = 15
    """`+` `-`"""
    MULTIPLICATIVE = 16
    """`*` `@` `/` `//` `%`"""
    UNARY = 17
    """`+x` `-x` `~x`"""
    POWER = 18
    """`**`"""
    AWAIT = 19
    CALL = 20
    """`f(x)` `x[i]` `x.a`"""
    PRIMARY = 21
    """Literals, identifiers, and every display form that brackets itself."""


@dataclass(frozen=True)
class SlotConstraints:
    """What a slot demands of what sits in it, beyond what precedence alone can express."""

    precedence: int
    """The lowest precedence that can sit here unparenthesized."""

    allows_bare_generator: bool = False
    """The slot is a call's sole argument, the one place `f(x for x in y)` needs no parentheses."""

    followed_by_dot: bool = False
    """The slot is followed by `.`, which an integer literal would lex as a decimal point."""


_BINARY_PRECEDENCE = {
    j.Binary.Type.Or: Precedence.OR,
    j.Binary.Type.And: Precedence.AND,
    j.Binary.Type.Equal: Precedence.COMPARISON,
    j.Binary.Type.NotEqual: Precedence.COMPARISON,
    j.Binary.Type.LessThan: Precedence.COMPARISON,
    j.Binary.Type.GreaterThan: Precedence.COMPARISON,
    j.Binary.Type.LessThanOrEqual: Precedence.COMPARISON,
    j.Binary.Type.GreaterThanOrEqual: Precedence.COMPARISON,
    j.Binary.Type.BitOr: Precedence.BIT_OR,
    j.Binary.Type.BitXor: Precedence.BIT_XOR,
    j.Binary.Type.BitAnd: Precedence.BIT_AND,
    j.Binary.Type.LeftShift: Precedence.SHIFT,
    j.Binary.Type.RightShift: Precedence.SHIFT,
    j.Binary.Type.Addition: Precedence.ADDITIVE,
    j.Binary.Type.Subtraction: Precedence.ADDITIVE,
    j.Binary.Type.Multiplication: Precedence.MULTIPLICATIVE,
    j.Binary.Type.Division: Precedence.MULTIPLICATIVE,
    j.Binary.Type.Modulo: Precedence.MULTIPLICATIVE,
}

_PY_BINARY_PRECEDENCE = {
    py.Binary.Type.In: Precedence.COMPARISON,
    py.Binary.Type.NotIn: Precedence.COMPARISON,
    py.Binary.Type.Is: Precedence.COMPARISON,
    py.Binary.Type.IsNot: Precedence.COMPARISON,
    py.Binary.Type.FloorDivision: Precedence.MULTIPLICATIVE,
    py.Binary.Type.MatrixMultiplication: Precedence.MULTIPLICATIVE,
    py.Binary.Type.Power: Precedence.POWER,
    py.Binary.Type.StringConcatenation: Precedence.PRIMARY,
}

_UNARY_PRECEDENCE = {
    j.Unary.Type.Not: Precedence.NOT,
    j.Unary.Type.Negative: Precedence.UNARY,
    j.Unary.Type.Positive: Precedence.UNARY,
    j.Unary.Type.Complement: Precedence.UNARY,
}


def precedence_of(expression: J) -> int:
    """The precedence of `expression` as printed; a kind not modelled here counts as PRIMARY."""
    if isinstance(expression, j.Binary):
        return _BINARY_PRECEDENCE.get(expression.operator, Precedence.PRIMARY)
    if isinstance(expression, py.Binary):
        return _PY_BINARY_PRECEDENCE.get(expression.operator, Precedence.PRIMARY)
    if isinstance(expression, j.Unary):
        return _UNARY_PRECEDENCE.get(expression.operator, Precedence.PRIMARY)
    if isinstance(expression, j.Ternary):
        return Precedence.CONDITIONAL
    if isinstance(expression, j.Lambda):
        return Precedence.LAMBDA
    if isinstance(expression, j.Assignment):
        # An assignment prints as `:=` wherever a slot holds it rather than a statement list
        return Precedence.NAMED_EXPRESSION
    if isinstance(expression, j.Yield) or isinstance(expression, py.YieldFrom):
        return Precedence.YIELD
    if isinstance(expression, py.Await):
        return Precedence.AWAIT
    if isinstance(expression, py.TypeHintedExpression):
        return Precedence.NAMED_EXPRESSION
    if isinstance(expression, py.CollectionLiteral):
        return Precedence.TUPLE if _omits_parentheses(expression.padding.elements) else Precedence.PRIMARY
    if isinstance(expression, py.ComprehensionExpression):
        return Precedence.GENERATOR if _omits_parentheses(expression) else Precedence.PRIMARY
    if isinstance(expression, (j.FieldAccess, j.ArrayAccess, j.MethodInvocation)):
        return Precedence.CALL
    if isinstance(expression, py.ExpressionStatement):
        return precedence_of(expression.expression)
    if isinstance(expression, py.StatementExpression):
        # `yield x` is parsed as a J.Yield inside a Py.StatementExpression
        return precedence_of(expression.statement)
    return Precedence.PRIMARY


def slot_constraints(parent: J, child_id: UUID) -> Optional[SlotConstraints]:
    """What the slot of `parent` holding `child_id` demands, or None for a slot not modelled here."""
    if isinstance(parent, (j.Parentheses, j.ControlParentheses)):
        return SlotConstraints(0) if _id_of(parent.tree) == child_id else None

    if isinstance(parent, (j.Binary, py.Binary)):
        return _binary_slot(parent, child_id)

    if isinstance(parent, j.Unary):
        if _id_of(parent.expression) != child_id:
            return None
        return SlotConstraints(_UNARY_PRECEDENCE.get(parent.operator, Precedence.UNARY))

    if isinstance(parent, j.Ternary):
        # Both the condition and the true part are `or_test`s; only the else part takes a `test`
        if _id_of(parent.condition) == child_id or _id_of(parent.true_part) == child_id:
            return SlotConstraints(Precedence.OR)
        return SlotConstraints(Precedence.LAMBDA) if _id_of(parent.false_part) == child_id else None

    if isinstance(parent, j.Assignment):
        if _id_of(parent.variable) == child_id:
            return SlotConstraints(Precedence.CALL)
        return SlotConstraints(Precedence.LAMBDA) if _id_of(parent.assignment) == child_id else None

    if isinstance(parent, j.Lambda):
        return SlotConstraints(Precedence.LAMBDA) if _id_of(parent.body) == child_id else None

    if isinstance(parent, py.Await):
        return SlotConstraints(Precedence.CALL) if _id_of(parent.expression) == child_id else None

    if isinstance(parent, j.FieldAccess):
        return SlotConstraints(Precedence.CALL, followed_by_dot=True) \
            if _id_of(parent.target) == child_id else None

    if isinstance(parent, j.ArrayAccess):
        if _id_of(parent.indexed) == child_id:
            return SlotConstraints(Precedence.CALL, followed_by_dot=False)
        # A subscript reads its index as an expression list, so `a[b, c]` is a tuple rather than two slots
        return SlotConstraints(Precedence.TUPLE) \
            if parent.dimension is not None and _id_of(parent.dimension.index) == child_id else None

    if isinstance(parent, j.MethodInvocation):
        if _id_of(parent.select) == child_id:
            return SlotConstraints(Precedence.CALL, followed_by_dot=True)
        arguments = parent.arguments or []
        if any(_id_of(argument) == child_id for argument in arguments):
            return SlotConstraints(Precedence.NAMED_EXPRESSION, allows_bare_generator=len(arguments) == 1)
        return None

    if isinstance(parent, (py.CollectionLiteral, py.DictLiteral)):
        return SlotConstraints(Precedence.NAMED_EXPRESSION) \
            if any(_id_of(element) == child_id for element in parent.elements) else None

    if isinstance(parent, py.KeyValue):
        # A dict entry and a keyword argument each take an `expression`, which a `:=` is not
        return SlotConstraints(Precedence.LAMBDA) \
            if child_id in (_id_of(parent.key), _id_of(parent.value)) else None

    if isinstance(parent, py.NamedArgument):
        return SlotConstraints(Precedence.LAMBDA) if _id_of(parent.value) == child_id else None

    if isinstance(parent, py.ComprehensionExpression):
        # A clause is visited without a cursor entry of its own, so its slots are answered here
        if _id_of(parent.result) == child_id:
            return SlotConstraints(Precedence.NAMED_EXPRESSION)
        for clause in parent.clauses:
            # `for v in x` and `if x` both read an `or_test`, so a conditional here dangles its `else`
            if _id_of(clause.iterated_list) == child_id or \
                    any(_id_of(condition.expression) == child_id for condition in clause.conditions or []):
                return SlotConstraints(Precedence.OR)
        return None

    if isinstance(parent, py.Star):
        return SlotConstraints(Precedence.OR) if _id_of(parent.expression) == child_id else None

    if isinstance(parent, j.Return):
        return SlotConstraints(Precedence.TUPLE) if _id_of(parent.expression) == child_id else None

    if isinstance(parent, (j.Yield, py.YieldFrom)):
        held = parent.value if isinstance(parent, j.Yield) else parent.expression
        return SlotConstraints(Precedence.TUPLE) if _id_of(held) == child_id else None

    if isinstance(parent, py.ExpressionStatement):
        # A statement may be a bare tuple or a `yield`, but never a bare generator
        return SlotConstraints(Precedence.YIELD) if _id_of(parent.expression) == child_id else None

    return None


def required_precedence(parent: J, child_id: UUID) -> Optional[int]:
    """The precedence the slot of `parent` holding `child_id` demands; see `slot_constraints`."""
    constraints = slot_constraints(parent, child_id)
    return constraints.precedence if constraints is not None else None


def maybe_parenthesize(parent: Optional[J], child_id: UUID, expression: J) -> J:
    """Parenthesizes `expression` if the slot of `parent` holding `child_id` would reparse it."""
    if parent is None or not isinstance(expression, Expression):
        return expression

    constraints = slot_constraints(parent, child_id)
    if constraints is None:
        return expression

    if precedence_of(expression) < constraints.precedence:
        if constraints.allows_bare_generator and precedence_of(expression) == Precedence.GENERATOR:
            return expression
        return parenthesize(expression)
    if constraints.followed_by_dot and _is_dot_adjacent_number(expression):
        return parenthesize(expression)
    return expression


def parenthesize(expression: Expression) -> j.Parentheses:
    """Wraps in `J.Parentheses`, moving the prefix out so the surrounding whitespace survives."""
    return j.Parentheses(
        _id=random_id(),
        _prefix=expression.prefix,
        _markers=Markers.EMPTY,
        _tree=JRightPadded(
            replace_if_changed(expression, _prefix=Space([], '')),
            Space([], ''),
            Markers.EMPTY,
        ),
    )


def enclosing_tree(cursor) -> Optional[J]:
    """The nearest enclosing LST node in a cursor path, skipping the padding wrappers visitors push."""
    while cursor is not None:
        if isinstance(cursor.value, J):
            return cursor.value
        cursor = cursor.parent
    return None


def _binary_slot(parent, child_id: UUID) -> Optional[SlotConstraints]:
    precedence = _BINARY_PRECEDENCE.get(parent.operator) if isinstance(parent, j.Binary) \
        else _PY_BINARY_PRECEDENCE.get(parent.operator)
    if precedence is None:
        return None

    is_left = _id_of(parent.left) == child_id
    if not is_left and _id_of(parent.right) != child_id:
        return None

    if precedence == Precedence.COMPARISON:
        # Comparisons chain rather than associate, so `(a < b) < c` and `a < b < c` differ
        return SlotConstraints(precedence + 1)
    if precedence == Precedence.POWER:
        # `**` is right-associative, and `-a ** b` means `-(a ** b)`, so its left operand takes no unary
        return SlotConstraints(Precedence.AWAIT if is_left else Precedence.UNARY)
    return SlotConstraints(precedence if is_left else precedence + 1)


def _omits_parentheses(node) -> bool:
    return node is not None and any(isinstance(m, OmitParentheses) for m in node.markers.markers)


def _is_dot_adjacent_number(expression: J) -> bool:
    """Whether a following `.` lexes into the number: `1.bit_length()` fails, `1.5`/`0x10` do not."""
    if not isinstance(expression, j.Literal) or expression.value_source is None:
        return False
    source = expression.value_source
    return source[0].isdigit() and source.replace('_', '').isdigit()


def _id_of(node) -> Optional[UUID]:
    return node.id if node is not None else None
