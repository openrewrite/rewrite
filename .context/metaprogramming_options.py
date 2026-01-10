"""
Exploring compile-time metaprogramming options for typed replace() methods.

Goal: Keep LST source clean and readable, but provide IDE autocomplete for replace().
"""

from dataclasses import dataclass, fields, replace
from typing import Optional, Self, dataclass_transform, TypeVar, get_type_hints
from uuid import UUID
import functools


# =============================================================================
# OPTION 1: Custom decorator that adds a typed replace() method at import time
# =============================================================================
# The decorator inspects fields and dynamically creates a replace() method
# with the correct signature. BUT - IDEs won't see the generated signature.

def add_replace_method(cls):
    """Decorator that adds a replace() method to a frozen dataclass."""
    def _replace(self, **changes):
        return replace(self, **changes)
    cls.replace = _replace
    return cls


# =============================================================================
# OPTION 2: .pyi stub files (RECOMMENDED)
# =============================================================================
# Generate .pyi stub files that declare the typed replace() signature.
# The runtime code uses plain replace(), but IDEs read the .pyi for types.
#
# tree.py (runtime - clean and simple):
#
#     @dataclass(frozen=True, slots=True)
#     class Await(Py, Expression):
#         id: UUID
#         prefix: Space
#         markers: Markers
#         expression: Expression
#         type: Optional[JavaType] = None
#
# tree.pyi (stub file - provides IDE autocomplete):
#
#     class Await(Py, Expression):
#         id: UUID
#         prefix: Space
#         markers: Markers
#         expression: Expression
#         type: Optional[JavaType]
#
#         def replace(
#             self,
#             *,
#             id: UUID = ...,
#             prefix: Space = ...,
#             markers: Markers = ...,
#             expression: Expression = ...,
#             type: Optional[JavaType] = ...,
#         ) -> Self: ...
#
# The stub generator script reads tree.py and generates tree.pyi automatically.


# =============================================================================
# OPTION 3: __init_subclass__ with dataclass_transform
# =============================================================================
# Use PEP 681 dataclass_transform to tell type checkers about our pattern.
# This is how attrs, pydantic, and other libraries get IDE support.

@dataclass_transform(frozen_default=True)
class LstBase:
    """Base class for all LST types that adds replace() method."""

    def __init_subclass__(cls, **kwargs):
        super().__init_subclass__(**kwargs)
        # Apply dataclass decorator
        dataclass(frozen=True, slots=True, eq=False)(cls)

    def replace(self, **changes) -> Self:
        """Create a copy with the specified fields replaced."""
        return replace(self, **changes)


# Usage would be:
# class Await(LstBase, Py, Expression):
#     id: UUID
#     prefix: Space
#     ...
#
# BUT: dataclass_transform alone doesn't give us typed kwargs on replace()


# =============================================================================
# OPTION 4: Protocol + stub file hybrid
# =============================================================================
# Define a Protocol that declares replace() exists, generate stubs for specifics

from typing import Protocol

class Replaceable(Protocol):
    def replace(self, **kwargs) -> Self: ...


# =============================================================================
# OPTION 5: Use typing.overload in stub files
# =============================================================================
# In .pyi files, we can use @overload to provide multiple typed signatures.
# This is the most IDE-friendly approach.
#
# tree.pyi:
#
#     from typing import overload
#
#     class Await(Py, Expression):
#         @overload
#         def replace(self) -> Await: ...
#         @overload
#         def replace(self, *, id: UUID) -> Await: ...
#         @overload
#         def replace(self, *, prefix: Space) -> Await: ...
#         @overload
#         def replace(self, *, id: UUID, prefix: Space) -> Await: ...
#         # ... combinatorial explosion, not practical
#
# Better: Just use keyword-only args with defaults in stub:
#
#     def replace(
#         self,
#         *,
#         id: UUID = ...,
#         prefix: Space = ...,
#     ) -> Await: ...


# =============================================================================
# STUB GENERATOR SCRIPT (for Option 2)
# =============================================================================
import ast
import inspect
from pathlib import Path


def generate_stubs_for_module(source_path: Path) -> str:
    """Generate .pyi stub content for a dataclass module with replace() methods."""
    with open(source_path) as f:
        source = f.read()

    tree = ast.parse(source)
    stub_lines = []

    # Add imports
    stub_lines.append("from typing import Optional, Self")
    stub_lines.append("from uuid import UUID")
    stub_lines.append("# ... other imports")
    stub_lines.append("")

    for node in ast.walk(tree):
        if isinstance(node, ast.ClassDef):
            # Check if it's a dataclass (has @dataclass decorator)
            is_dataclass = any(
                (isinstance(d, ast.Name) and d.id == 'dataclass') or
                (isinstance(d, ast.Call) and isinstance(d.func, ast.Name) and d.func.id == 'dataclass')
                for d in node.decorator_list
            )

            if is_dataclass:
                # Extract fields (annotated assignments)
                fields_info = []
                for item in node.body:
                    if isinstance(item, ast.AnnAssign) and isinstance(item.target, ast.Name):
                        field_name = item.target.id
                        # Get the annotation as string
                        field_type = ast.unparse(item.annotation)
                        has_default = item.value is not None
                        fields_info.append((field_name, field_type, has_default))

                # Generate stub class
                bases = ", ".join(ast.unparse(b) for b in node.bases)
                stub_lines.append(f"class {node.name}({bases}):")

                # Add field declarations
                for name, type_str, _ in fields_info:
                    stub_lines.append(f"    {name}: {type_str}")

                stub_lines.append("")

                # Add typed replace() method
                stub_lines.append("    def replace(")
                stub_lines.append("        self,")
                stub_lines.append("        *,")
                for name, type_str, _ in fields_info:
                    stub_lines.append(f"        {name}: {type_str} = ...,")
                stub_lines.append("    ) -> Self: ...")
                stub_lines.append("")

    return "\n".join(stub_lines)


# =============================================================================
# DEMONSTRATION
# =============================================================================

if __name__ == "__main__":
    # Example LST class (clean source)
    @dataclass(frozen=True, slots=True, eq=False)
    class Space:
        whitespace: str = ""
        comments: list = None

    @dataclass(frozen=True, slots=True, eq=False)
    class Markers:
        id: UUID
        markers: list = None

    @dataclass(frozen=True, slots=True, eq=False)
    class Await:
        id: UUID
        prefix: Space
        markers: Markers
        expression: object  # Would be Expression
        type: Optional[str] = None

        # Simple replace method - runtime works, but IDE doesn't know the kwargs
        def replace(self, **changes) -> "Await":
            return replace(self, **changes)

    # Usage
    a = Await(
        id=UUID("12345678-1234-5678-1234-567812345678"),
        prefix=Space(),
        markers=Markers(id=UUID("12345678-1234-5678-1234-567812345678")),
        expression=None
    )

    # This works at runtime, but IDE won't autocomplete 'expression'
    a2 = a.replace(expression="new_expr")
    print(f"Original: {a}")
    print(f"Replaced: {a2}")

    # Generate stub for this file
    print("\n--- Generated Stub ---")
    print(generate_stubs_for_module(Path(__file__)))
