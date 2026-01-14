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
import sys
from pathlib import Path
from typing import List, Tuple, Optional


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


def get_class_bases(node: ast.ClassDef) -> str:
    """Get the base classes as a string."""
    if not node.bases:
        return ""
    return ", ".join(ast.unparse(base) for base in node.bases)


def extract_imports(tree: ast.Module) -> List[str]:
    """Extract import statements from the module."""
    imports = []
    for node in ast.walk(tree):
        if isinstance(node, ast.Import):
            for alias in node.names:
                imports.append(f"import {alias.name}" + (f" as {alias.asname}" if alias.asname else ""))
        elif isinstance(node, ast.ImportFrom):
            module = node.module or ""
            names = ", ".join(
                alias.name + (f" as {alias.asname}" if alias.asname else "")
                for alias in node.names
            )
            level = "." * node.level
            imports.append(f"from {level}{module} import {names}")
    return imports


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

    # Field declarations
    for name, type_str, has_default in fields:
        lines.append(f"{indent}    {name}: {type_str}")

    if fields:
        lines.append("")

    # Generate typed replace() method
    lines.append(f"{indent}    def replace(")
    lines.append(f"{indent}        self,")
    lines.append(f"{indent}        *,")
    for name, type_str, has_default in fields:
        lines.append(f"{indent}        {name}: {type_str} = ...,")
    lines.append(f"{indent}    ) -> Self: ...")

    return lines


def generate_stub_content(source_path: Path) -> str:
    """Generate .pyi stub content for a Python source file."""
    with open(source_path) as f:
        source = f.read()

    tree = ast.parse(source)

    stub_lines = [
        "# Auto-generated stub file for IDE autocomplete support.",
        "# Do not edit manually - regenerate with: python scripts/generate_stubs.py",
        "",
        "from typing import Any, ClassVar, List, Optional, Self",
        "from uuid import UUID",
        "",
    ]

    # Find additional imports we might need
    imports = extract_imports(tree)
    for imp in imports:
        # Skip __future__ imports and typing imports (we add our own)
        if "__future__" in imp or "from typing import" in imp:
            continue
        # Add imports that might be needed for type annotations
        if "from rewrite" in imp or "from pathlib" in imp or "from enum" in imp:
            stub_lines.append(imp)

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
