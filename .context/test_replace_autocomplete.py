"""Test file to check IDE autocomplete behavior with dataclasses.replace()"""

from dataclasses import dataclass, replace
from typing import Optional
from uuid import UUID


@dataclass(frozen=True, slots=True)
class Space:
    whitespace: str = ""


@dataclass(frozen=True, slots=True)
class Markers:
    id: UUID
    markers: list = None


@dataclass(frozen=True, slots=True)
class Await:
    id: UUID
    prefix: Space
    markers: Markers
    expression: "Expression"
    type: Optional[str] = None


# Test autocomplete behavior
await_expr = Await(
    id=UUID("12345678-1234-5678-1234-567812345678"),
    prefix=Space(),
    markers=Markers(id=UUID("12345678-1234-5678-1234-567812345678")),
    expression=None,
    type=None
)

# Does the IDE autocomplete 'expression', 'prefix', etc. here?
# In PyCharm/VS Code with Pylance, place cursor after replace(await_expr,
new_await = replace(await_expr, )  # <-- cursor here

# Compare to with_* method approach:
# new_await = await_expr.with_expression(new_expr)  # IDE shows .with_expression, .with_prefix, etc.
