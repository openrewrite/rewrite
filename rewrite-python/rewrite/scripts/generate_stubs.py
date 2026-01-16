#!/usr/bin/env python3
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
Generate .pyi stub files for LST dataclasses.

This script parses Python source files containing frozen dataclasses and
generates corresponding .pyi stub files with typed replace() method signatures.
This enables IDE autocomplete for the replace() method's kwargs.

Usage:
    python scripts/generate_stubs.py [source_file.py ...]

If no files are specified, generates stubs for all tree.py and support_types.py
files in the rewrite package.
"""

import ast
import keyword
import sys
from pathlib import Path
from typing import List, Tuple, Optional


# Python reserved keywords that can't be used as parameter names
PYTHON_RESERVED = set(keyword.kwlist) | {'type'}  # 'type' is soft keyword in 3.12+


def to_public_name(name: str) -> str:
    """
    Convert internal field name to public parameter name.
    Strips underscore prefix, adding trailing underscore for reserved keywords.
    """
    public_name = name.lstrip('_')
    if public_name in PYTHON_RESERVED:
        return public_name + '_'  # Add trailing underscore per Python convention
    return public_name


def extract_dataclass_fields(node: ast.ClassDef) -> List[Tuple[str, str, bool]]:
    """
    Extract field information from a dataclass.

    Returns list of (name, type_annotation, has_default) tuples.
    """
    fields = []
    for item in node.body:
        if isinstance(item, ast.AnnAssign) and isinstance(item.target, ast.Name):
            field_name = item.target.id
            # Skip ClassVar fields (like 'kind')
            if isinstance(item.annotation, ast.Subscript):
                if isinstance(item.annotation.value, ast.Name):
                    if item.annotation.value.id == "ClassVar":
                        continue
            field_type = ast.unparse(item.annotation)
            has_default = item.value is not None
            fields.append((field_name, field_type, has_default))
    return fields


def extract_classvar_fields(node: ast.ClassDef) -> List[Tuple[str, str]]:
    """
    Extract ClassVar field information from a dataclass.

    Returns list of (name, type_annotation) tuples for ClassVar fields.
    """
    fields = []
    for item in node.body:
        if isinstance(item, ast.AnnAssign) and isinstance(item.target, ast.Name):
            field_name = item.target.id
            if isinstance(item.annotation, ast.Subscript):
                if isinstance(item.annotation.value, ast.Name):
                    if item.annotation.value.id == "ClassVar":
                        field_type = ast.unparse(item.annotation)
                        fields.append((field_name, field_type))
    return fields


def extract_explicit_init(node: ast.ClassDef) -> Optional[List[Tuple[str, str]]]:
    """
    Extract parameters from an explicit __init__ method if present.

    Returns list of (name, type_annotation) tuples, or None if no explicit __init__.
    """
    for item in node.body:
        if isinstance(item, ast.FunctionDef) and item.name == "__init__":
            params = []
            for arg in item.args.args:
                if arg.arg == "self":
                    continue
                if arg.annotation:
                    param_type = ast.unparse(arg.annotation)
                else:
                    param_type = "Any"
                params.append((arg.arg, param_type))
            return params
    return None


def extract_property_methods(node: ast.ClassDef) -> List[Tuple[str, str]]:
    """
    Extract @property methods from a class.

    Returns list of (name, return_type) tuples.
    """
    properties = []
    for item in node.body:
        if isinstance(item, ast.FunctionDef):
            # Check if it's a property
            is_property = any(
                isinstance(d, ast.Name) and d.id == "property"
                for d in item.decorator_list
            )
            if is_property and item.returns:
                return_type = ast.unparse(item.returns)
                properties.append((item.name, return_type))
    return properties


def extract_class_methods(node: ast.ClassDef) -> List[Tuple[str, List[Tuple[str, str]], str]]:
    """
    Extract @classmethod methods from a class.

    Returns list of (name, params, return_type) tuples.
    """
    methods = []
    for item in node.body:
        if isinstance(item, ast.FunctionDef):
            is_classmethod = any(
                isinstance(d, ast.Name) and d.id == "classmethod"
                for d in item.decorator_list
            )
            if is_classmethod and item.returns:
                params = []
                for arg in item.args.args[1:]:  # Skip 'cls'
                    if arg.annotation:
                        param_type = ast.unparse(arg.annotation)
                    else:
                        param_type = "Any"
                    params.append((arg.arg, param_type))
                return_type = ast.unparse(item.returns)
                methods.append((item.name, params, return_type))
    return methods


def extract_regular_methods(node: ast.ClassDef) -> List[Tuple[str, List[Tuple[str, str]], str]]:
    """
    Extract regular methods from a class (not __init__, __eq__, etc.).

    Returns list of (name, params, return_type) tuples.
    """
    methods = []
    skip_methods = {'__init__', '__eq__', '__hash__', 'replace'}
    for item in node.body:
        if isinstance(item, ast.FunctionDef):
            if item.name in skip_methods or item.name.startswith('_'):
                continue
            # Skip property and classmethod
            is_special = any(
                isinstance(d, ast.Name) and d.id in ("property", "classmethod", "staticmethod", "abstractmethod")
                for d in item.decorator_list
            )
            if is_special:
                continue
            if item.returns:
                params = []
                for arg in item.args.args[1:]:  # Skip 'self'
                    if arg.annotation:
                        param_type = ast.unparse(arg.annotation)
                    else:
                        param_type = "Any"
                    params.append((arg.arg, param_type))
                return_type = ast.unparse(item.returns)
                methods.append((item.name, params, return_type))
    return methods


def is_frozen_dataclass(node: ast.ClassDef) -> bool:
    """Check if a class is decorated with @dataclass(frozen=True)."""
    for decorator in node.decorator_list:
        if isinstance(decorator, ast.Call):
            if isinstance(decorator.func, ast.Name) and decorator.func.id == "dataclass":
                for keyword in decorator.keywords:
                    if keyword.arg == "frozen":
                        if isinstance(keyword.value, ast.Constant):
                            return keyword.value.value is True
    return False


def is_abc_base_class(node: ast.ClassDef) -> bool:
    """
    Check if a class is an ABC-like base class (inherits from known base types
    like J, Statement, Expression, TypedTree, etc.) but is not a dataclass.

    These are typically abstract base classes like TypeTree, NameTree, TypedTree.
    """
    if is_frozen_dataclass(node):
        return False

    # Known base types that indicate this is an ABC or generic class
    known_bases = {
        'J', 'Statement', 'Expression', 'TypedTree', 'NameTree', 'TypeTree', 'Loop', 'MethodCall',
        'ABC', 'Py', 'PyStatement', 'PyExpression',
        'Tree', 'SourceFile', 'Generic'  # For rewrite/tree.py classes
    }

    for base in node.bases:
        if isinstance(base, ast.Name) and base.id in known_bases:
            return True
        # Handle Generic[T] style bases
        if isinstance(base, ast.Subscript) and isinstance(base.value, ast.Name):
            if base.value.id in known_bases:
                return True
    return False


def get_class_bases(node: ast.ClassDef) -> str:
    """Get the base classes as a string."""
    if not node.bases:
        return ""
    return ", ".join(ast.unparse(base) for base in node.bases)


def generate_abc_stub_class(node: ast.ClassDef, indent: str = "") -> List[str]:
    """Generate stub content for an ABC-like base class (no fields, just pass)."""
    lines = []

    bases = get_class_bases(node)
    if bases:
        lines.append(f"{indent}class {node.name}({bases}):")
    else:
        lines.append(f"{indent}class {node.name}:")

    # Check for any methods that should be included
    methods = extract_regular_methods(node)
    properties = extract_property_methods(node)

    if methods or properties:
        for name, return_type in properties:
            lines.append(f"{indent}    @property")
            lines.append(f"{indent}    def {name}(self) -> {return_type}: ...")
        for name, params, return_type in methods:
            if params:
                params_str = ", ".join(f"{p[0]}: {p[1]}" for p in params)
                lines.append(f"{indent}    def {name}(self, {params_str}) -> {return_type}: ...")
            else:
                lines.append(f"{indent}    def {name}(self) -> {return_type}: ...")
    else:
        lines.append(f"{indent}    pass")

    return lines


def extract_imports(tree: ast.Module, current_package: str = "") -> List[Tuple[str, bool]]:
    """
    Extract import statements from the module.

    Returns list of (import_statement, should_reexport) tuples.
    Sibling module imports (same package) should be re-exported.
    """
    imports = []
    for node in ast.walk(tree):
        if isinstance(node, ast.Import):
            for alias in node.names:
                imp = f"import {alias.name}" + (f" as {alias.asname}" if alias.asname else "")
                imports.append((imp, False))
        elif isinstance(node, ast.ImportFrom):
            module = node.module or ""
            level = "." * node.level

            # Check if this is a sibling module import (same package)
            # e.g., for current_package="rewrite.python", module="rewrite.python.support_types" is sibling
            is_sibling = False
            if current_package and level == "":  # absolute import
                is_sibling = module.startswith(current_package + ".")
            elif level == ".":  # relative import from same package
                is_sibling = True

            # Build import statement, using X as X pattern for re-exports
            if is_sibling:
                names = ", ".join(
                    f"{alias.name} as {alias.asname or alias.name}"
                    for alias in node.names
                    if alias.name != "*"  # star imports handled separately
                )
            else:
                names = ", ".join(
                    alias.name + (f" as {alias.asname}" if alias.asname else "")
                    for alias in node.names
                )

            if names:  # Skip if only star import
                imports.append((f"from {level}{module} import {names}", is_sibling))
    return imports


def find_star_import_source(tree: ast.Module, source_path: Path) -> Optional[Path]:
    """
    Find the source file for a relative star import (from .module import *).

    Returns the path to the source file, or None if no star import found.
    """
    for node in tree.body:
        if isinstance(node, ast.ImportFrom) and node.level == 1:  # relative import
            for alias in node.names:
                if alias.name == "*":
                    # Found `from .module import *`
                    module_name = node.module
                    if module_name:
                        sibling = source_path.parent / f"{module_name}.py"
                        if sibling.exists():
                            return sibling
    return None


def get_classes_from_file(source_path: Path) -> List[Tuple[str, str]]:
    """
    Get all class names and their base classes from a Python file.

    Returns list of (class_name, bases_string) tuples.
    """
    with open(source_path) as f:
        source = f.read()

    tree = ast.parse(source)
    classes = []

    for node in tree.body:
        if isinstance(node, ast.ClassDef):
            bases = get_class_bases(node)
            classes.append((node.name, bases))

    return classes


def generate_stub_class(node: ast.ClassDef, indent: str = "") -> List[str]:
    """Generate stub content for a single dataclass."""
    lines = []

    # Class declaration
    bases = get_class_bases(node)
    if bases:
        lines.append(f"{indent}class {node.name}({bases}):")
    else:
        lines.append(f"{indent}class {node.name}:")

    fields = extract_dataclass_fields(node)
    classvar_fields = extract_classvar_fields(node)

    # Check for nested classes that are also dataclasses
    nested_classes = []
    for item in node.body:
        if isinstance(item, ast.ClassDef) and is_frozen_dataclass(item):
            nested_classes.append(item)

    # Generate nested class stubs first
    for nested in nested_classes:
        nested_lines = generate_stub_class(nested, indent + "    ")
        lines.extend(nested_lines)
        lines.append("")

    # ClassVar declarations (like EMPTY)
    for name, type_str in classvar_fields:
        lines.append(f"{indent}    {name}: {type_str}")

    if classvar_fields:
        lines.append("")

    # Field declarations using public names for attribute access
    # Type checkers will use these for attribute access like `obj.id`
    for name, type_str, has_default in fields:
        public_name = to_public_name(name)
        lines.append(f"{indent}    {public_name}: {type_str}")

    if fields:
        lines.append("")

    # Check for explicit __init__ method
    explicit_init = extract_explicit_init(node)

    # Generate __init__ method for dataclass constructor
    # Use actual field names (with underscore) since that's what dataclass generates
    lines.append(f"{indent}    def __init__(")
    lines.append(f"{indent}        self,")
    if explicit_init is not None:
        # Use parameters from explicit __init__
        for name, type_str in explicit_init:
            lines.append(f"{indent}        {name}: {type_str},")
    else:
        # Use dataclass field names (with underscore prefix)
        for name, type_str, has_default in fields:
            if has_default:
                lines.append(f"{indent}        {name}: {type_str} = ...,")
            else:
                lines.append(f"{indent}        {name}: {type_str},")
    lines.append(f"{indent}    ) -> None: ...")
    lines.append("")

    # Generate typed replace() method with public names
    lines.append(f"{indent}    def replace(")
    lines.append(f"{indent}        self,")
    lines.append(f"{indent}        *,")
    for name, type_str, has_default in fields:
        public_name = to_public_name(name)
        lines.append(f"{indent}        {public_name}: {type_str} = ...,")
    lines.append(f"{indent}    ) -> Self: ...")

    # Generate classmethod stubs
    classmethods = extract_class_methods(node)
    if classmethods:
        lines.append("")
        for name, params, return_type in classmethods:
            lines.append(f"{indent}    @classmethod")
            if params:
                params_str = ", ".join(f"{p[0]}: {p[1]}" for p in params)
                lines.append(f"{indent}    def {name}(cls, {params_str}) -> {return_type}: ...")
            else:
                lines.append(f"{indent}    def {name}(cls) -> {return_type}: ...")

    # Generate regular method stubs
    methods = extract_regular_methods(node)
    if methods:
        lines.append("")
        for name, params, return_type in methods:
            if params:
                params_str = ", ".join(f"{p[0]}: {p[1]}" for p in params)
                lines.append(f"{indent}    def {name}(self, {params_str}) -> {return_type}: ...")
            else:
                lines.append(f"{indent}    def {name}(self) -> {return_type}: ...")

    return lines


def get_package_from_path(source_path: Path) -> str:
    """
    Determine the Python package name from the source file path.

    E.g., .../src/rewrite/python/tree.py -> rewrite.python
    """
    parts = source_path.parts
    try:
        # Find 'rewrite' in the path and build package from there
        rewrite_idx = parts.index('rewrite')
        # Package is everything from 'rewrite' to parent of the file (excluding file itself)
        package_parts = parts[rewrite_idx:-1]
        return '.'.join(package_parts)
    except ValueError:
        return ""


def generate_stub_content(source_path: Path) -> str:
    """Generate .pyi stub content for a Python source file."""
    with open(source_path) as f:
        source = f.read()

    tree = ast.parse(source)
    current_package = get_package_from_path(source_path)

    stub_lines = [
        "# Auto-generated stub file for IDE autocomplete support.",
        "# Do not edit manually - regenerate with: python scripts/generate_stubs.py",
        "",
        "from typing import Any, ClassVar, List, Optional",
        "from typing_extensions import Self",
        "from uuid import UUID",
        "",
    ]

    # Find additional imports we might need
    imports = extract_imports(tree, current_package)
    for imp, is_reexport in imports:
        # Skip __future__ imports and typing imports (we add our own)
        if "__future__" in imp or "from typing import" in imp:
            continue
        # Add imports that might be needed for type annotations
        # Include: rewrite modules, pathlib, enum, and relative module imports (from . import X)
        if "from rewrite" in imp or "from pathlib" in imp or "from enum" in imp or imp.startswith("from . import"):
            stub_lines.append(imp)

    stub_lines.append("")

    # Handle star imports - import and re-export classes from the source module
    # Using "X as X" pattern tells type checkers to re-export the names
    star_import_source = find_star_import_source(tree, source_path)
    if star_import_source:
        # Get the relative module name
        module_name = star_import_source.stem  # e.g., "support_types"
        exported_classes = get_classes_from_file(star_import_source)
        if exported_classes:
            # Add explicit import with re-export pattern
            class_names = [f"{name} as {name}" for name, _ in exported_classes]
            stub_lines.append(f"from .{module_name} import (")
            for cn in class_names:
                stub_lines.append(f"    {cn},")
            stub_lines.append(")")
            stub_lines.append("")

    # Find all ABC-like base classes at module level (before dataclasses)
    for node in tree.body:
        if isinstance(node, ast.ClassDef) and is_abc_base_class(node):
            class_lines = generate_abc_stub_class(node)
            stub_lines.extend(class_lines)
            stub_lines.append("")

    # Find all frozen dataclasses at module level
    for node in tree.body:
        if isinstance(node, ast.ClassDef) and is_frozen_dataclass(node):
            class_lines = generate_stub_class(node)
            stub_lines.extend(class_lines)
            stub_lines.append("")

    return "\n".join(stub_lines)


def generate_stub_file(source_path: Path) -> Optional[Path]:
    """Generate a .pyi stub file for the given Python source file."""
    if not source_path.exists():
        print(f"Warning: {source_path} does not exist", file=sys.stderr)
        return None

    stub_content = generate_stub_content(source_path)
    stub_path = source_path.with_suffix(".pyi")

    # Only write if content is meaningful (has at least one class)
    if "class " not in stub_content:
        print(f"Skipping {source_path} - no frozen dataclasses found")
        return None

    with open(stub_path, "w") as f:
        f.write(stub_content)

    print(f"Generated {stub_path}")
    return stub_path


def find_lst_files(base_path: Path) -> List[Path]:
    """Find all tree.py and support_types.py files in the package."""
    files = []
    for pattern in ["**/tree.py", "**/support_types.py", "**/markers.py"]:
        files.extend(base_path.glob(pattern))
    return files


def main():
    if len(sys.argv) > 1:
        # Process specified files
        files = [Path(f) for f in sys.argv[1:]]
    else:
        # Find all LST files in the package
        script_dir = Path(__file__).parent
        src_dir = script_dir.parent / "src" / "rewrite"
        files = find_lst_files(src_dir)

    if not files:
        print("No files to process")
        return

    generated = 0
    for source_file in files:
        if generate_stub_file(source_file):
            generated += 1

    print(f"\nGenerated {generated} stub file(s)")


if __name__ == "__main__":
    main()
