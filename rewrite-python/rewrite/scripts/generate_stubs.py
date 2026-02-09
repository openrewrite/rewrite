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


def has_explicit_replace(node: ast.ClassDef) -> bool:
    """
    Check if a class has an explicit replace method defined.

    Returns True if the class defines its own replace method, False if it should
    inherit from parent class.
    """
    for item in node.body:
        if isinstance(item, ast.FunctionDef) and item.name == "replace":
            return True
    return False


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


def extract_abstract_methods(node: ast.ClassDef) -> List[Tuple[str, List[Tuple[str, str]], str]]:
    """
    Extract @abstractmethod methods from a class (excluding properties).

    Returns list of (name, params, return_type) tuples.
    """
    methods = []
    for item in node.body:
        if isinstance(item, ast.FunctionDef):
            is_abstract = any(
                isinstance(d, ast.Name) and d.id == "abstractmethod"
                for d in item.decorator_list
            )
            # Skip if also a property (those are handled by extract_property_methods)
            is_property = any(
                isinstance(d, ast.Name) and d.id == "property"
                for d in item.decorator_list
            )
            if is_abstract and not is_property and item.returns:
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


def is_dataclass(node: ast.ClassDef) -> bool:
    """Check if a class is decorated with @dataclass (any variant)."""
    for decorator in node.decorator_list:
        # @dataclass or @dataclass()
        if isinstance(decorator, ast.Name) and decorator.id == "dataclass":
            return True
        if isinstance(decorator, ast.Call):
            if isinstance(decorator.func, ast.Name) and decorator.func.id == "dataclass":
                return True
    return False


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


def extract_enum_members(node: ast.ClassDef) -> List[str]:
    """Extract enum member names from an Enum class."""
    members = []
    for item in node.body:
        if isinstance(item, ast.Assign) and len(item.targets) == 1:
            target = item.targets[0]
            if isinstance(target, ast.Name):
                members.append(target.id)
    return members


def generate_nested_class_stub(node: ast.ClassDef, indent: str = "") -> List[str]:
    """Generate stub for a nested class (enum, dataclass, or plain class)."""
    lines = []
    bases = get_class_bases(node)

    # Check what kind of class this is
    is_enum = any(isinstance(base, ast.Name) and base.id == 'Enum' for base in node.bases)

    if is_enum:
        if bases:
            lines.append(f"{indent}class {node.name}({bases}):")
        else:
            lines.append(f"{indent}class {node.name}:")

        # Extract and include enum members - members are instances of the enum class
        members = extract_enum_members(node)
        if members:
            for member in members:
                lines.append(f"{indent}    {member}: {node.name}")
        else:
            lines.append(f"{indent}    pass")
    elif is_dataclass(node):
        # Use the full dataclass stub generator
        lines.extend(generate_stub_class(node, indent))
    else:
        # Plain class - generate class header with fields, properties, and methods
        if bases:
            lines.append(f"{indent}class {node.name}({bases}):")
        else:
            lines.append(f"{indent}class {node.name}:")

        has_content = False

        # Check for nested classes
        for item in node.body:
            if isinstance(item, ast.ClassDef):
                lines.extend(generate_nested_class_stub(item, indent + "    "))
                lines.append("")
                has_content = True

        # Extract class-level field annotations (like _t: JContainer[J3])
        for item in node.body:
            if isinstance(item, ast.AnnAssign) and isinstance(item.target, ast.Name):
                type_str = ast.unparse(item.annotation)
                lines.append(f"{indent}    {item.target.id}: {type_str}")
                has_content = True

        if has_content:
            lines.append("")

        # Extract properties
        properties = extract_property_methods(node)
        for name, return_type in properties:
            lines.append(f"{indent}    @property")
            lines.append(f"{indent}    def {name}(self) -> {return_type}: ...")
            has_content = True

        # Extract methods (regular methods skip 'replace', so handle it separately)
        methods = extract_regular_methods(node)
        for name, params, return_type in methods:
            if params:
                params_str = ", ".join(f"{p[0]}: {p[1]}" for p in params)
                lines.append(f"{indent}    def {name}(self, {params_str}) -> {return_type}: ...")
            else:
                lines.append(f"{indent}    def {name}(self) -> {return_type}: ...")
            has_content = True

        # Handle replace method explicitly with its actual return type
        if has_explicit_replace(node):
            replace_return_type = get_replace_return_type(node)
            if replace_return_type:
                lines.append(f"{indent}    def replace(self, **kwargs: Any) -> {replace_return_type}: ...")
                has_content = True

        if not has_content:
            lines.append(f"{indent}    pass")

    return lines


def generate_abc_stub_class(node: ast.ClassDef, indent: str = "") -> List[str]:
    """Generate stub content for an ABC-like base class."""
    lines = []

    bases = get_class_bases(node)
    if bases:
        lines.append(f"{indent}class {node.name}({bases}):")
    else:
        lines.append(f"{indent}class {node.name}:")

    has_content = False

    # Generate nested classes first (enums, dataclasses, plain classes)
    for item in node.body:
        if isinstance(item, ast.ClassDef):
            nested_lines = generate_nested_class_stub(item, indent + "    ")
            lines.extend(nested_lines)
            lines.append("")
            has_content = True

    # Check for any methods that should be included
    methods = extract_regular_methods(node)
    properties = extract_property_methods(node)
    classmethods = extract_class_methods(node)
    abstract_methods = extract_abstract_methods(node)

    if methods or properties or classmethods or abstract_methods:
        for name, return_type in properties:
            lines.append(f"{indent}    @property")
            lines.append(f"{indent}    def {name}(self) -> {return_type}: ...")
        for name, params, return_type in classmethods:
            lines.append(f"{indent}    @classmethod")
            if params:
                params_str = ", ".join(f"{p[0]}: {p[1]}" for p in params)
                lines.append(f"{indent}    def {name}(cls, {params_str}) -> {return_type}: ...")
            else:
                lines.append(f"{indent}    def {name}(cls) -> {return_type}: ...")
        for name, params, return_type in abstract_methods:
            if params:
                params_str = ", ".join(f"{p[0]}: {p[1]}" for p in params)
                lines.append(f"{indent}    def {name}(self, {params_str}) -> {return_type}: ...")
            else:
                lines.append(f"{indent}    def {name}(self) -> {return_type}: ...")
        for name, params, return_type in methods:
            if params:
                params_str = ", ".join(f"{p[0]}: {p[1]}" for p in params)
                lines.append(f"{indent}    def {name}(self, {params_str}) -> {return_type}: ...")
            else:
                lines.append(f"{indent}    def {name}(self) -> {return_type}: ...")
        has_content = True

    # Add replace method stub for ABC base classes that explicitly define it
    # EXCEPT for the root Tree class - its replace(**kwargs) -> Tree signature
    # conflicts with typed replace() methods in subclasses, and all subclasses
    # either define their own replace or inherit from a class that does
    if has_explicit_replace(node) and node.name != 'Tree':
        replace_return_type = get_replace_return_type(node)
        if replace_return_type:
            lines.append(f"{indent}    def replace(self, **kwargs: Any) -> {replace_return_type}: ...")
            has_content = True

    if not has_content:
        lines.append(f"{indent}    pass")

    return lines


def get_replace_return_type(node: ast.ClassDef) -> Optional[str]:
    """
    Get the return type of an explicit replace method in a class.

    Returns the return type as a string, or None if not found.
    """
    for item in node.body:
        if isinstance(item, ast.FunctionDef) and item.name == "replace" and item.returns:
            return ast.unparse(item.returns)
    return None


def extract_typevars(tree: ast.Module) -> List[str]:
    """
    Extract TypeVar declarations from the module.

    Returns list of TypeVar declaration strings like "P = TypeVar('P')".
    """
    typevars = []
    for node in tree.body:
        if isinstance(node, ast.Assign) and len(node.targets) == 1:
            target = node.targets[0]
            if isinstance(target, ast.Name) and isinstance(node.value, ast.Call):
                if isinstance(node.value.func, ast.Name) and node.value.func.id == 'TypeVar':
                    # Found a TypeVar declaration
                    typevars.append(ast.unparse(node))
    return typevars


def _is_type_checking_guard(node: ast.If) -> bool:
    """Check if an if-statement is `if TYPE_CHECKING:`."""
    test = node.test
    if isinstance(test, ast.Name) and test.id == 'TYPE_CHECKING':
        return True
    if isinstance(test, ast.Attribute) and isinstance(test.value, ast.Name):
        return test.attr == 'TYPE_CHECKING'
    return False


def extract_imports(tree: ast.Module, current_package: str = "") -> List[Tuple[str, bool]]:
    """
    Extract import statements from the module.

    Only considers module-level imports and imports inside `if TYPE_CHECKING:` blocks.
    Returns list of (import_statement, should_reexport) tuples, deduplicated.
    Sibling module imports (same package) should be re-exported.
    """
    imports = []
    seen = set()

    # Collect module-level import nodes, including those inside TYPE_CHECKING guards
    import_nodes = []
    for node in tree.body:
        if isinstance(node, (ast.Import, ast.ImportFrom)):
            import_nodes.append(node)
        elif isinstance(node, ast.If) and _is_type_checking_guard(node):
            for child in node.body:
                if isinstance(child, (ast.Import, ast.ImportFrom)):
                    import_nodes.append(child)

    for node in import_nodes:
        if isinstance(node, ast.Import):
            for alias in node.names:
                imp = f"import {alias.name}" + (f" as {alias.asname}" if alias.asname else "")
                if imp not in seen:
                    seen.add(imp)
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
                imp = f"from {level}{module} import {names}"
                if imp not in seen:
                    seen.add(imp)
                    imports.append((imp, is_sibling))
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


def get_typevar_names_from_file(source_path: Path) -> List[str]:
    """
    Get all TypeVar names from a Python file.

    Returns list of TypeVar names like ['P', 'T', 'J2'].
    """
    with open(source_path) as f:
        source = f.read()

    tree = ast.parse(source)
    typevar_names = []

    for node in tree.body:
        if isinstance(node, ast.Assign) and len(node.targets) == 1:
            target = node.targets[0]
            if isinstance(target, ast.Name) and isinstance(node.value, ast.Call):
                if isinstance(node.value.func, ast.Name) and node.value.func.id == 'TypeVar':
                    typevar_names.append(target.id)

    return typevar_names


def generate_stub_class(node: ast.ClassDef, indent: str = "") -> List[str]:
    """Generate stub content for a single dataclass."""
    lines = []

    # Add @dataclass decorator for frozen immutable instances
    lines.append(f"{indent}@dataclass(frozen=True)")

    # Class declaration
    bases = get_class_bases(node)
    if bases:
        lines.append(f"{indent}class {node.name}({bases}):")
    else:
        lines.append(f"{indent}class {node.name}:")

    fields = extract_dataclass_fields(node)
    classvar_fields = extract_classvar_fields(node)

    # Check for nested classes (dataclasses, enums, and plain classes)
    nested_dataclasses = []
    nested_enums = []
    nested_plain_classes = []
    for item in node.body:
        if isinstance(item, ast.ClassDef):
            if is_dataclass(item):
                nested_dataclasses.append(item)
            elif any(isinstance(base, ast.Name) and base.id == 'Enum' for base in item.bases):
                nested_enums.append(item)
            else:
                # Plain nested class (like PaddingHelper)
                nested_plain_classes.append(item)

    # Generate nested enum stubs first (with enum members)
    for nested in nested_enums:
        bases = get_class_bases(nested)
        if bases:
            lines.append(f"{indent}    class {nested.name}({bases}):")
        else:
            lines.append(f"{indent}    class {nested.name}:")

        # Extract and include enum members - members are instances of the enum class
        members = extract_enum_members(nested)
        if members:
            for member in members:
                lines.append(f"{indent}        {member}: {nested.name}")
        else:
            lines.append(f"{indent}        pass")
        lines.append("")

    # Generate nested dataclass stubs
    for nested in nested_dataclasses:
        nested_lines = generate_stub_class(nested, indent + "    ")
        lines.extend(nested_lines)
        lines.append("")

    # Generate nested plain class stubs (like PaddingHelper)
    for nested in nested_plain_classes:
        nested_lines = generate_nested_class_stub(nested, indent + "    ")
        lines.extend(nested_lines)
        lines.append("")

    # ClassVar declarations (like EMPTY)
    for name, type_str in classvar_fields:
        lines.append(f"{indent}    {name}: {type_str}")

    if classvar_fields:
        lines.append("")

    # Field declarations with underscore-prefixed names for dataclass
    # These are the actual field names the dataclass uses
    for name, type_str, has_default in fields:
        if has_default:
            lines.append(f"{indent}    {name}: {type_str} = ...")
        else:
            lines.append(f"{indent}    {name}: {type_str}")

    if fields:
        lines.append("")

    # Generate replace() method stub with actual return type
    if has_explicit_replace(node):
        replace_return_type = get_replace_return_type(node)
        if replace_return_type:
            lines.append(f"{indent}    def replace(self, **kwargs: Any) -> {replace_return_type}: ...")
        else:
            lines.append(f"{indent}    def replace(self, **kwargs: Any) -> Self: ...")
    else:
        lines.append(f"{indent}    def replace(self, **kwargs: Any) -> Self: ...")

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

    # Generate property stubs
    properties = extract_property_methods(node)
    if properties:
        lines.append("")
        for name, return_type in properties:
            lines.append(f"{indent}    @property")
            lines.append(f"{indent}    def {name}(self) -> {return_type}: ...")

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
    # Find 'src' followed by 'rewrite' to identify the package root
    # This handles absolute paths where 'rewrite' may appear multiple times
    for i, part in enumerate(parts):
        if part == 'src' and i + 1 < len(parts) and parts[i + 1] == 'rewrite':
            # Package is everything from 'rewrite' (after 'src') to parent of the file
            package_parts = parts[i + 1:-1]
            return '.'.join(package_parts)
    # Fallback: find last occurrence of 'rewrite' in path
    try:
        # Reverse search to find the last 'rewrite'
        for i in range(len(parts) - 1, -1, -1):
            if parts[i] == 'rewrite':
                package_parts = parts[i:-1]
                return '.'.join(package_parts)
    except ValueError:
        pass
    return ""


def generate_stub_content(source_path: Path) -> str:
    """Generate .pyi stub content for a Python source file."""
    with open(source_path) as f:
        source = f.read()

    tree = ast.parse(source)
    current_package = get_package_from_path(source_path)

    # Check if we need TypeVar
    typevars = extract_typevars(tree)
    typevar_import = ", TypeVar, Generic" if typevars else ""

    stub_lines = [
        "# Auto-generated stub file for IDE autocomplete support.",
        "# Do not edit manually - regenerate with: python scripts/generate_stubs.py",
        "",
        "from dataclasses import dataclass",
        f"from typing import Any, ClassVar, List, Optional{typevar_import}",
        "from typing_extensions import Self",
        "from uuid import UUID",
        "import weakref",
        "",
    ]

    # Add TypeVar declarations
    if typevars:
        for tv in typevars:
            stub_lines.append(tv)
        stub_lines.append("")

    # Find additional imports we might need
    imports = extract_imports(tree, current_package)
    for imp, _ in imports:
        # Skip __future__ imports and typing imports (we add our own)
        if "__future__" in imp or "from typing import" in imp:
            continue
        # Add imports that might be needed for type annotations
        # Include: rewrite modules, pathlib, enum, datetime, abc, and relative module imports (from . import X)
        if ("from rewrite" in imp or "from pathlib" in imp or "from enum" in imp or
            "from datetime" in imp or "from abc" in imp or imp.startswith("from . import")):
            stub_lines.append(imp)

    stub_lines.append("")

    # Handle star imports - import and re-export classes and TypeVars from the source module
    # Using "X as X" pattern tells type checkers to re-export the names
    star_import_source = find_star_import_source(tree, source_path)
    if star_import_source:
        # Get the relative module name
        module_name = star_import_source.stem  # e.g., "support_types"
        exported_classes = get_classes_from_file(star_import_source)
        exported_typevars = get_typevar_names_from_file(star_import_source)

        # Combine classes and TypeVars for re-export
        all_exports = []
        for tv in exported_typevars:
            all_exports.append(f"{tv} as {tv}")
        for name, _ in exported_classes:
            all_exports.append(f"{name} as {name}")

        if all_exports:
            # Add explicit import with re-export pattern
            stub_lines.append(f"from .{module_name} import (")
            for exp in all_exports:
                stub_lines.append(f"    {exp},")
            stub_lines.append(")")
            stub_lines.append("")

    # Process all top-level classes in order, handling each type appropriately
    for node in tree.body:
        if not isinstance(node, ast.ClassDef):
            continue

        if is_abc_base_class(node):
            class_lines = generate_abc_stub_class(node)
            stub_lines.extend(class_lines)
            stub_lines.append("")
        elif is_frozen_dataclass(node):
            class_lines = generate_stub_class(node)
            stub_lines.extend(class_lines)
            stub_lines.append("")
        elif not is_dataclass(node):
            # Handle Enums, simple mixin classes, and other non-dataclass classes
            # These are needed in stubs when referenced as base classes
            class_lines = generate_nested_class_stub(node)
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
    """Find all tree.py, support_types.py, markers.py, and parser.py files in the package."""
    files = []
    for pattern in ["**/tree.py", "**/support_types.py", "**/markers.py", "**/parser.py"]:
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
