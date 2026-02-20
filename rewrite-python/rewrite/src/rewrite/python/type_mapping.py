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
Python to JavaType mapping using ty for type inference.

This module provides type attribution for Python code by querying the ty
type checker via LSP. The type information is mapped to OpenRewrite's
JavaType model to enable Java recipes like FindMethods to work on Python.
"""

from __future__ import annotations

import ast
import re
from pathlib import Path
from typing import Dict, List, Optional, Tuple
from uuid import uuid4

from ..java import JavaType

# Try to import TyLspClient, but make it optional for parsing to work without ty
_TY_AVAILABLE = False
try:
    from .ty_client import TyLspClient
    _TY_AVAILABLE = True
except ImportError:
    TyLspClient = None  # type: ignore


# Mapping of Python builtin types to JavaType.Primitive
_PYTHON_PRIMITIVES: Dict[str, JavaType.Primitive] = {
    'str': JavaType.Primitive.String,
    'int': JavaType.Primitive.Int,
    'float': JavaType.Primitive.Double,
    'bool': JavaType.Primitive.Boolean,
    'None': JavaType.Primitive.None_,
    'NoneType': JavaType.Primitive.None_,
    'bytes': JavaType.Primitive.String,  # Close enough for matching
}

# Reverse mapping from JavaType.Primitive to Python type name
_PRIMITIVE_TO_PYTHON: Dict[JavaType.Primitive, str] = {
    JavaType.Primitive.String: 'str',
    JavaType.Primitive.Int: 'int',
    JavaType.Primitive.Double: 'float',
    JavaType.Primitive.Boolean: 'bool',
    JavaType.Primitive.None_: 'None',
}


class PythonTypeMapping:
    """Maps Python types to JavaType for recipe matching.

    This class uses ty's LSP server to infer types for Python code and
    converts them to JavaType objects that can be used by Java recipes
    like FindMethods, ChangeMethodName, etc.

    Usage:
        mapping = PythonTypeMapping(source, file_path="/path/to/file.py")
        method_type = mapping.method_invocation_type(call_node)
    """

    # Cache for type mappings to avoid repeated LSP queries
    _type_cache: Dict[str, JavaType] = {}

    def __init__(self, source: str, file_path: Optional[str] = None):
        """Initialize type mapping for a source file.

        Args:
            source: The Python source code.
            file_path: Optional file path for the source. If provided,
                      it will be used as the document URI for ty.
        """
        self._source = source
        self._file_path = file_path
        self._source_lines = source.splitlines()

        # Create a URI for this document
        if file_path:
            # Ensure we have an absolute path for the file URI
            path = Path(file_path)
            if not path.is_absolute():
                path = path.resolve()
            self._uri = path.as_uri()
        else:
            self._uri = f"untitled:{uuid4()}"

        # Discover venv from file path
        venv_path = self._discover_venv(file_path) if file_path else None

        # Open the document in ty if available
        self._ty_client: Optional[TyLspClient] = None
        if _TY_AVAILABLE:
            try:
                self._ty_client = TyLspClient.get(venv_path)
                self._ty_client.open_document(self._uri, source)
            except RuntimeError:
                # ty not installed
                self._ty_client = None

    def _get_module_from_type_definition(self, line: int, col: int) -> Optional[str]:
        """Get the module path where a type at the given position is defined.

        Uses ty's textDocument/typeDefinition to find the file where a type
        is defined, then extracts the module path from that file path.

        Args:
            line: Zero-based line number.
            col: Zero-based column offset.

        Returns:
            The module path (e.g., 'httpx' or 'httpx._models'), or None.
        """
        if self._ty_client is None:
            return None

        type_def_uri = self._ty_client.get_type_definition(self._uri, line, col)
        if not type_def_uri:
            return None

        return self._extract_module_from_uri(type_def_uri)

    def _extract_module_from_uri(self, uri: str) -> Optional[str]:
        """Extract a Python module path from a file URI.

        Converts a file URI like 'file:///path/to/httpx/httpx/_models.py'
        to a module path like 'httpx._models'. Uses package structure
        (presence of __init__.py) to determine the module hierarchy.

        Args:
            uri: A file URI pointing to a Python file.

        Returns:
            The module path, or None if it cannot be determined.
        """
        from urllib.parse import urlparse, unquote

        parsed = urlparse(uri)
        if parsed.scheme != 'file':
            return None

        file_path = Path(unquote(parsed.path))
        if not file_path.suffix == '.py':
            return None

        # Build module path by walking up and looking for __init__.py
        module_parts = []
        current = file_path

        # Add the filename (without .py) unless it's __init__
        if current.stem != '__init__':
            module_parts.insert(0, current.stem)

        current = current.parent

        # Walk up looking for __init__.py to find package boundaries
        for _ in range(20):  # Limit depth
            init_file = current / '__init__.py'
            if init_file.exists():
                module_parts.insert(0, current.name)
                current = current.parent
            else:
                # No __init__.py means we've left the package
                break

        if module_parts:
            # Clean up internal module names: httpx._models -> httpx
            # If the module has an underscore prefix, use the parent
            fqn = '.'.join(module_parts)
            # For patterns like 'httpx._models', just use 'httpx' as the public API
            parts = fqn.split('.')
            public_parts = []
            for part in parts:
                if part.startswith('_') and public_parts:
                    # Stop at private modules, use only public path
                    break
                public_parts.append(part)
            return '.'.join(public_parts) if public_parts else None

        return None

    def _discover_venv(self, file_path: str) -> Optional[Path]:
        """Discover the virtual environment for a file.

        Walks up from the file's directory looking for a .venv directory
        or a pyproject.toml with an associated .venv.

        Args:
            file_path: Path to the Python source file.

        Returns:
            Path to the .venv directory if found, None otherwise.
        """
        path = Path(file_path)
        if not path.is_absolute():
            path = path.resolve()

        # Walk up the directory tree looking for .venv
        current = path.parent
        for _ in range(20):  # Limit depth to avoid infinite loops
            venv = current / '.venv'
            if venv.is_dir():
                return venv

            # Also check for pyproject.toml as a project root indicator
            pyproject = current / 'pyproject.toml'
            if pyproject.exists():
                # If there's a pyproject.toml but no .venv, stop looking
                break

            parent = current.parent
            if parent == current:
                break
            current = parent

        return None

    def close(self) -> None:
        """Close the document in ty."""
        if self._ty_client is not None:
            self._ty_client.close_document(self._uri)

    def type(self, node: ast.AST) -> Optional[JavaType]:
        """Get the JavaType for an AST node.

        Args:
            node: The AST node to get the type for.

        Returns:
            The JavaType for the node, or None if the type cannot be determined.
        """
        if isinstance(node, ast.Constant):
            return self._constant_type(node)
        elif isinstance(node, ast.Call):
            return self.method_invocation_type(node)  # ty: ignore[invalid-return-type]
        elif isinstance(node, ast.Name):
            return self._name_type(node)
        elif isinstance(node, ast.Attribute):
            return self._attribute_type(node)

        return None

    def _constant_type(self, node: ast.Constant) -> Optional[JavaType]:
        """Get the type for a constant/literal node."""
        if isinstance(node.value, (str, bytes)):
            return JavaType.Primitive.String
        elif isinstance(node.value, bool):
            return JavaType.Primitive.Boolean
        elif isinstance(node.value, int):
            return JavaType.Primitive.Int
        elif isinstance(node.value, float):
            return JavaType.Primitive.Double
        elif node.value is None:
            return JavaType.Primitive.None_
        return None

    def _is_variable_hover(self, hover: str) -> bool:
        """Check if hover text describes a variable (not a function, class, or module).

        ty hover patterns:
        - Variable: just the type, e.g. ``int``, ``Literal[5]``, ``str``
        - Function: ``def func(args) -> return_type``
        - Class definition: ``class MyClass``
        - Module: ``<module 'name'>``
        """
        if not hover:
            return False
        clean = self._strip_markdown(hover)
        if not clean:
            return False
        if clean.startswith('def ') or clean.startswith('class ') or clean.startswith('<module'):
            return False
        if clean == 'Unknown':
            return False
        return True

    def _make_variable(self, name: str, var_type: Optional[JavaType],
                       owner: Optional[JavaType] = None) -> JavaType.Variable:
        """Create a JavaType.Variable instance."""
        return JavaType.Variable(_name=name, _type=var_type, _owner=owner)

    def name_type_info(self, node: ast.Name) -> Tuple[Optional[JavaType], Optional[JavaType.Variable]]:
        """Get expression type and variable type for a name reference.

        Returns (expression_type, variable_field_type) from a single hover query.
        """
        if self._ty_client is None:
            return None, None

        hover = self._ty_client.get_hover(
            self._uri,
            node.lineno - 1,  # LSP uses 0-based lines
            node.col_offset
        )
        if not hover:
            return None, None

        expr_type = self._parse_hover_type(hover)

        if self._is_variable_hover(hover):
            return expr_type, self._make_variable(node.id, expr_type)

        return expr_type, None

    def _name_type(self, node: ast.Name) -> Optional[JavaType]:
        """Get the type for a name reference."""
        if self._ty_client is None:
            return None

        hover = self._ty_client.get_hover(
            self._uri,
            node.lineno - 1,  # LSP uses 0-based lines
            node.col_offset
        )

        if hover:
            return self._parse_hover_type(hover)
        return None

    def attribute_type_info(self, node: ast.Attribute,
                            receiver_type: Optional[JavaType] = None
                            ) -> Tuple[Optional[JavaType], Optional[JavaType.Variable]]:
        """Get expression type and variable type for an attribute access.

        Args:
            node: The ast.Attribute node.
            receiver_type: The type of the receiver (e.g., type of 'self').

        Returns (expression_type, variable_field_type).

        Note:
            The hover column is computed as ``node.col_offset + len(receiver_text) + 1``.
            This may be inaccurate for multiline or parenthesized receiver expressions
            where ``_get_node_text`` returns a substring that differs from the full
            source span.
        """
        if self._ty_client is None:
            return None, None

        hover = self._ty_client.get_hover(
            self._uri, node.lineno - 1,
            node.col_offset + len(self._get_node_text(node.value)) + 1
        )
        if not hover:
            return None, None

        expr_type = self._parse_hover_type(hover)

        if self._is_variable_hover(hover):
            return expr_type, self._make_variable(node.attr, expr_type, owner=receiver_type)

        return expr_type, None

    def _attribute_type(self, node: ast.Attribute) -> Optional[JavaType]:
        """Get the type for an attribute access."""
        if self._ty_client is None:
            return None

        hover = self._ty_client.get_hover(
            self._uri,
            node.lineno - 1,
            node.col_offset + len(self._get_node_text(node.value)) + 1  # After the dot
        )

        if hover:
            return self._parse_hover_type(hover)
        return None

    def method_invocation_type(self, node: ast.Call) -> Optional[JavaType.Method]:
        """Get the method type for a function/method call.

        This returns a complete JavaType.Method with:
        - name: The method name
        - declaringType: The class/module containing the method
        - parameterNames: The names of the method parameters
        - parameterTypes: The types of the method parameters

        Args:
            node: The ast.Call node representing the method invocation.

        Returns:
            A JavaType.Method with full type information, or None if
            the type cannot be determined.
        """
        # Extract method name
        method_name = self._extract_method_name(node)
        if not method_name:
            return None

        # Get declaring type from ty
        declaring_type = self._get_declaring_type(node)

        # Get parameter names and types from method signature
        param_names, param_types = self._get_method_signature(node)

        # Get return type
        return_type = self._get_return_type(node)

        return JavaType.Method(
            _flags_bit_map=0,
            _declaring_type=declaring_type,
            _name=method_name,
            _return_type=return_type,
            _parameter_names=param_names if param_names else None,
            _parameter_types=param_types if param_types else None,
        )

    def _extract_method_name(self, node: ast.Call) -> Optional[str]:
        """Extract the method name from a Call node."""
        if isinstance(node.func, ast.Name):
            return node.func.id
        elif isinstance(node.func, ast.Attribute):
            return node.func.attr
        return None

    def _get_method_signature(self, node: ast.Call) -> Tuple[List[str], List[JavaType]]:
        """Get parameter names and types from the method signature.

        This method queries ty for the method signature and parses it to extract
        parameter names and types. If ty is unavailable or parsing fails, it
        falls back to generating placeholder names.

        Args:
            node: The ast.Call node representing the method invocation.

        Returns:
            A tuple of (parameter_names, parameter_types).
        """
        if self._ty_client is None:
            return self._generate_placeholder_names(node)

        # Get hover on the method name to get its signature
        hover = self._get_method_hover(node)
        if not hover:
            return self._generate_placeholder_names(node)

        # Parse the signature from hover
        names, types = self._parse_signature_from_hover(hover)
        if names:
            return names, types

        # Fall back to placeholder names
        return self._generate_placeholder_names(node)

    def _get_method_hover(self, node: ast.Call) -> Optional[str]:
        """Get the hover information for the method being called.

        Args:
            node: The ast.Call node.

        Returns:
            The hover string containing the method signature, or None.
        """
        if self._ty_client is None:
            return None

        if isinstance(node.func, ast.Attribute):
            # For method calls like obj.method(), hover on the method name
            # The method name starts after the receiver and the dot
            receiver_text = self._get_node_text(node.func.value)
            hover_col = node.func.col_offset + len(receiver_text) + 1  # +1 for the dot
            return self._ty_client.get_hover(
                self._uri,
                node.func.lineno - 1,
                hover_col
            )
        elif isinstance(node.func, ast.Name):
            # For function calls, hover on the function name
            return self._ty_client.get_hover(
                self._uri,
                node.func.lineno - 1,
                node.func.col_offset
            )

        return None

    def _parse_signature_from_hover(self, hover: str) -> Tuple[List[str], List[JavaType]]:
        """Parse parameter names and types from a method signature hover.

        Handles formats like:
        - def split(sep: str | None = ..., maxsplit: SupportsIndex = ...) -> list[str]
        - def upper() -> LiteralString

        Args:
            hover: The hover response containing a method signature.

        Returns:
            A tuple of (parameter_names, parameter_types).
        """
        hover = self._strip_markdown(hover)

        # Match: def method_name(params) -> return_type
        # Use DOTALL to handle multi-line signatures
        match = re.search(r'def \w+\s*\((.*?)\)\s*(?:->|$)', hover, re.DOTALL)
        if not match:
            return [], []

        params_str = match.group(1).strip()
        if not params_str:
            return [], []

        names: List[str] = []
        types: List[JavaType] = []

        # Parse each parameter, handling defaults and type annotations
        for param in self._split_params(params_str):
            param = param.strip()
            if not param or param in ('self', 'cls', '/', '*'):
                continue  # Skip self, cls, positional-only marker, and keyword-only marker

            # Handle *args and **kwargs
            if param.startswith('**'):
                param = param[2:]
            elif param.startswith('*'):
                param = param[1:]

            # Extract name (before : or =)
            name_match = re.match(r'(\w+)', param)
            if name_match:
                name = name_match.group(1)
                names.append(name)

                # Extract type (between : and = or end of param)
                type_match = re.search(r':\s*([^=]+?)(?:\s*=|$)', param)
                if type_match:
                    type_str = type_match.group(1).strip()
                    java_type = self._type_string_to_java_type(type_str)
                    types.append(java_type if java_type else JavaType.Unknown())
                else:
                    types.append(JavaType.Unknown())

        return names, types

    def _split_params(self, params_str: str) -> List[str]:
        """Split a parameter string handling nested brackets.

        Handles cases like:
        - "a, b, c"
        - "a: list[str], b: dict[str, int]"
        - "a: Callable[[int], str], b: int"

        Args:
            params_str: The parameter string from a function signature.

        Returns:
            A list of individual parameter strings.
        """
        params: List[str] = []
        current = []
        depth = 0

        for char in params_str:
            if char in '([{':
                depth += 1
                current.append(char)
            elif char in ')]}':
                depth -= 1
                current.append(char)
            elif char == ',' and depth == 0:
                params.append(''.join(current).strip())
                current = []
            else:
                current.append(char)

        # Don't forget the last parameter
        if current:
            params.append(''.join(current).strip())

        return params

    def _generate_placeholder_names(self, node: ast.Call) -> Tuple[List[str], List[JavaType]]:
        """Generate placeholder parameter names when signature parsing fails.

        Args:
            node: The ast.Call node.

        Returns:
            A tuple of (placeholder_names, parameter_types).
        """
        param_types = self._get_parameter_types(node) or []
        names = [f"arg{i}" for i in range(len(param_types))]
        return names, param_types

    def _get_declaring_type(self, node: ast.Call) -> Optional[JavaType.FullyQualified]:
        """Get the declaring type (class/module) for a method call."""
        if self._ty_client is None:
            return self._infer_declaring_type_from_ast(node)

        # Query ty for the type of the callee
        if isinstance(node.func, ast.Attribute):
            receiver = node.func.value

            # For chained calls like "hello".upper().split(), the receiver is a Call
            # We need to get the return type of the inner call
            if isinstance(receiver, ast.Call):
                return self._get_call_return_type(receiver)

            # For method calls like obj.method(), get the type of obj
            # For chained attributes like os.path, we need to hover at the end
            # of the chain (on 'path') rather than the start (on 'os')
            hover_col = receiver.col_offset
            if hasattr(receiver, 'end_col_offset') and receiver.end_col_offset:
                # Hover at the end of the expression (within the last attribute)
                hover_col = receiver.end_col_offset - 1

            # Get the type name from hover
            hover = self._ty_client.get_hover(
                self._uri,
                receiver.lineno - 1,
                hover_col
            )
            if hover:
                # Check if this is a module (e.g., httpx.get() where httpx is a module)
                hover_clean = self._strip_markdown(hover)
                module_match = re.match(r"<module\s+'([^']+)'", hover_clean)
                if module_match:
                    # The receiver is a module, so the FQN is just the module name
                    module_name = module_match.group(1)
                    return self._create_class_type(module_name)

                type_name = self._extract_type_name_from_hover(hover)
                if type_name and type_name != 'Unknown':
                    # Get the module FQN from typeDefinition
                    module = self._get_module_from_type_definition(
                        receiver.lineno - 1,
                        hover_col
                    )
                    if module:
                        # Create FQN: module.TypeName (e.g., httpx.Client)
                        fqn = f"{module}.{type_name}"
                        return self._create_class_type(fqn)
                    else:
                        # Fall back to just the type name
                        return self._create_class_type(type_name)

        elif isinstance(node.func, ast.Name):
            # For function calls, try to get the module
            hover = self._ty_client.get_hover(
                self._uri,
                node.func.lineno - 1,
                node.func.col_offset
            )
            if hover:
                return self._extract_declaring_type_from_function_hover(hover)

        return self._infer_declaring_type_from_ast(node)

    def _extract_type_name_from_hover(self, hover: str) -> Optional[str]:
        """Extract just the type name from a hover response.

        Args:
            hover: The raw hover response from ty.

        Returns:
            The type name (e.g., 'Client', 'Response'), or None.
        """
        hover = self._strip_markdown(hover)

        # Handle module type format: <module 'httpx'>
        # Return None so we use module-based FQN lookup instead
        module_match = re.match(r"<module\s+'([^']+)'", hover)
        if module_match:
            # Return the module name as the type - the FQN IS the module
            return None  # Signal that this is a module, not a class type

        # Handle "variable: Type" format
        if ': ' in hover and not hover.startswith('def '):
            type_str = hover.split(': ', 1)[1].strip()
            # Handle union types by taking the first non-None type
            if ' | ' in type_str:
                for part in type_str.split(' | '):
                    part = part.strip()
                    if part not in ('None', 'NoneType', 'Unknown'):
                        type_str = part
                        break
            # Handle generics - extract base type
            if '[' in type_str:
                type_str = type_str.split('[')[0]
            return type_str

        # Just a type name
        if hover and not hover.startswith('def '):
            type_str = hover.strip()
            if '[' in type_str:
                type_str = type_str.split('[')[0]
            return type_str

        return None

    def _get_call_return_type(self, call_node: ast.Call) -> Optional[JavaType.FullyQualified]:
        """Get the return type of a function/method call.

        For chained calls like "hello".upper().split(), this returns the type
        that .upper() returns (str), which is then the declaring type for .split().
        """
        if self._ty_client is None:
            return None

        # For method calls like obj.method(), hover on the method name
        if isinstance(call_node.func, ast.Attribute):
            # Hover on the method name to get the signature
            hover = self._ty_client.get_hover(
                self._uri,
                call_node.func.lineno - 1,
                call_node.func.col_offset + len(self._get_node_text(call_node.func.value)) + 1
            )
            if hover:
                return self._extract_return_type_as_class(hover)

        return None

    def _extract_return_type_as_class(self, hover: str) -> Optional[JavaType.FullyQualified]:
        """Extract the return type from a method signature hover and convert to class type.

        Handles formats like:
        - def upper() -> LiteralString
        - def split(...) -> list[str]
        """
        hover = self._strip_markdown(hover)

        # Extract return type from "def func(...) -> ReturnType"
        if ' -> ' in hover:
            return_type_str = hover.split(' -> ')[-1].strip()
            java_type = self._type_string_to_java_type(return_type_str)
            if isinstance(java_type, JavaType.Class):
                return java_type
            if isinstance(java_type, JavaType.Primitive):
                return self._create_class_type(_PRIMITIVE_TO_PYTHON.get(java_type, java_type.name.lower()))

        return None

    def _infer_declaring_type_from_ast(self, node: ast.Call) -> Optional[JavaType.FullyQualified]:
        """Infer declaring type from AST when ty is unavailable."""
        if isinstance(node.func, ast.Attribute):
            receiver = node.func.value

            # Handle Python builtin types from literals
            if isinstance(receiver, ast.Constant):
                if isinstance(receiver.value, str):
                    return self._create_class_type('str')
                elif isinstance(receiver.value, bytes):
                    return self._create_class_type('bytes')
                elif isinstance(receiver.value, (int, float, bool)):
                    # Numbers and bools don't have many methods but handle them
                    type_name = type(receiver.value).__name__
                    return self._create_class_type(type_name)
            elif isinstance(receiver, ast.List):
                return self._create_class_type('list')
            elif isinstance(receiver, ast.Dict):
                return self._create_class_type('dict')
            elif isinstance(receiver, ast.Set):
                return self._create_class_type('set')
            elif isinstance(receiver, ast.Tuple):
                return self._create_class_type('tuple')
            elif isinstance(receiver, ast.Call):
                # For chained calls like "hello".upper().split(), infer from inner call
                # This is a simplification - full type inference would require ty
                pass

            # Try to build a fully qualified name from the attribute chain
            # Only do this for actual attribute chains (like os.path), not simple names
            parts = []
            current = receiver
            while isinstance(current, ast.Attribute):
                parts.insert(0, current.attr)
                current = current.value
            if isinstance(current, ast.Name):
                parts.insert(0, current.id)

            # Only create a class type if we have an attribute chain (len > 1)
            # For simple variable names like 'client', return None (Unknown)
            # since we can't determine the actual type without ty
            if len(parts) > 1:
                fqn = '.'.join(parts)
                return self._create_class_type(fqn)
        return None

    def _get_parameter_types(self, node: ast.Call) -> Optional[List[JavaType]]:
        """Get the types of actual arguments in the call."""
        if not node.args and not node.keywords:
            return []

        param_types: List[JavaType] = []

        for arg in node.args:
            arg_type = self.type(arg)
            if arg_type:
                param_types.append(arg_type)
            else:
                param_types.append(JavaType.Unknown())

        # Handle keyword arguments as well
        for kw in node.keywords:
            kw_type = self.type(kw.value)
            if kw_type:
                param_types.append(kw_type)
            else:
                param_types.append(JavaType.Unknown())

        return param_types if param_types else None

    def _get_return_type(self, node: ast.Call) -> Optional[JavaType]:
        """Get the return type of a method call."""
        if self._ty_client is None:
            return None

        # Query ty for the type at the call expression
        hover = self._ty_client.get_hover(
            self._uri,
            node.lineno - 1,
            node.col_offset
        )

        if hover:
            return self._parse_hover_type(hover)
        return None

    def _parse_hover_type(self, hover: str) -> Optional[JavaType]:
        """Parse ty's hover response into a JavaType."""
        if not hover:
            return None

        hover = self._strip_markdown(hover)

        # Check for primitive types
        for py_type, java_type in _PYTHON_PRIMITIVES.items():
            if hover == py_type or hover.endswith(f': {py_type}'):
                return java_type  # ty: ignore[invalid-return-type]

        # Try to extract a type from various hover formats
        type_str = self._extract_type_from_hover(hover)
        if type_str:
            return self._type_string_to_java_type(type_str)

        return None

    def _extract_type_from_hover(self, hover: str) -> Optional[str]:
        """Extract the type string from a hover response."""
        # Pattern: "variable: Type"
        if ': ' in hover and not hover.startswith('def '):
            return hover.split(': ', 1)[1].strip()

        # Pattern: "def func(...) -> ReturnType"
        if ' -> ' in hover:
            return hover.split(' -> ')[-1].strip()

        # Pattern: just "Type"
        if hover and not hover.startswith('def '):
            return hover

        return None

    def _parse_hover_as_class_type(self, hover: str) -> Optional[JavaType.FullyQualified]:
        """Parse ty's hover response as a class type."""
        hover = self._strip_markdown(hover)
        type_str = self._extract_type_from_hover(hover)
        if type_str:
            java_type = self._type_string_to_java_type(type_str)
            if isinstance(java_type, JavaType.Class):
                return java_type
            # For primitives like str, create a class wrapper
            if isinstance(java_type, JavaType.Primitive):
                return self._create_class_type(_PRIMITIVE_TO_PYTHON.get(java_type, java_type.name.lower()))
        return None

    def _strip_markdown(self, hover: str) -> str:
        """Strip markdown code block formatting from ty hover response."""
        if not hover:
            return ''

        hover = hover.strip()
        if hover.startswith('```'):
            lines = hover.split('\n')
            # Remove first line (```python) and last line (```)
            content_lines = []
            for line in lines[1:]:
                if line.strip() == '```':
                    break
                content_lines.append(line)
            hover = '\n'.join(content_lines).strip()

        # If there's documentation (after ---), only use the type part
        if '\n---\n' in hover:
            hover = hover.split('\n---\n')[0].strip()

        return hover

    def _extract_declaring_type_from_function_hover(self, hover: str) -> Optional[JavaType.FullyQualified]:
        """Extract the declaring type from a function hover.

        For imported functions like 'requests.get', ty might show:
        "def get(...) -> Response" with module info
        """
        # This is a simplified implementation
        # In practice, we'd need to analyze the import context
        return None

    def _type_string_to_java_type(self, type_str: str) -> Optional[JavaType]:
        """Convert a Python type string to a JavaType."""
        type_str = type_str.strip()

        # Handle Unknown type
        if type_str == 'Unknown':
            return JavaType.Unknown()  # ty: ignore[invalid-return-type]

        # Handle module type: <module 'name'>
        module_match = re.match(r"<module\s+'([^']+)'", type_str)
        if module_match:
            module_name = module_match.group(1)
            return self._create_class_type(module_name)  # ty: ignore[invalid-return-type]

        # Handle LiteralString (from ty) - treat as str
        if type_str == 'LiteralString':
            return self._create_class_type('str')  # ty: ignore[invalid-return-type]

        # Check primitives first
        if type_str in _PYTHON_PRIMITIVES:
            return _PYTHON_PRIMITIVES[type_str]  # ty: ignore[invalid-return-type]

        # Handle Literal["value"] - extract base type from the value
        literal_match = re.match(r'Literal\[(.+)\]', type_str)
        if literal_match:
            literal_value = literal_match.group(1).strip()
            # Determine type from the literal value
            if (literal_value.startswith('"') and literal_value.endswith('"')) or \
               (literal_value.startswith("'") and literal_value.endswith("'")):
                return self._create_class_type('str')  # ty: ignore[invalid-return-type]
            elif literal_value in ('True', 'False'):
                return JavaType.Primitive.Boolean
            elif literal_value.isdigit() or (literal_value.startswith('-') and literal_value[1:].isdigit()):
                return JavaType.Primitive.Int
            # Default to treating literal as str
            return self._create_class_type('str')  # ty: ignore[invalid-return-type]

        # Handle Optional[T], List[T], etc.
        generic_match = re.match(r'(\w+)\[(.+)\]', type_str)
        if generic_match:
            base = generic_match.group(1).lower()
            # Normalize common type names
            type_mapping = {
                'list': 'list',
                'dict': 'dict',
                'set': 'set',
                'tuple': 'tuple',
                'frozenset': 'frozenset',
                'optional': None,  # Optional[T] should use T
            }
            if base in type_mapping:
                if type_mapping[base] is None:
                    # For Optional[T], recurse on T
                    inner = generic_match.group(2)
                    return self._type_string_to_java_type(inner)
                return self._create_class_type(type_mapping[base])  # ty: ignore[invalid-return-type]
            # Keep original casing for other types
            return self._create_class_type(generic_match.group(1))  # ty: ignore[invalid-return-type]

        # Handle union types: T | None, Union[T, None]
        if ' | ' in type_str:
            # Take the first non-None, non-Unknown type
            parts = [p.strip() for p in type_str.split(' | ')]
            for part in parts:
                if part not in ('None', 'NoneType', 'Unknown'):
                    return self._type_string_to_java_type(part)
            # If all parts are None/Unknown, return Unknown
            return JavaType.Unknown()  # ty: ignore[invalid-return-type]

        # Default to class type
        return self._create_class_type(type_str)  # ty: ignore[invalid-return-type]

    def _create_class_type(self, fqn: str) -> JavaType.Class:
        """Create a JavaType.Class from a fully qualified name.

        Args:
            fqn: The fully qualified type name (e.g., 'httpx.Client' or 'str').

        Returns:
            A JavaType.Class with the fully qualified name.
        """
        # Check cache
        if fqn in self._type_cache:
            cached = self._type_cache[fqn]
            if isinstance(cached, JavaType.Class):
                return cached

        # JavaType.Class is not a dataclass, so we create empty and set attrs
        class_type = JavaType.Class()
        class_type._flags_bit_map = 0
        class_type._fully_qualified_name = fqn
        class_type._kind = JavaType.FullyQualified.Kind.Class

        self._type_cache[fqn] = class_type  # ty: ignore[invalid-assignment]
        return class_type

    def _get_node_text(self, node: ast.expr) -> str:
        """Get the source text for an AST node."""
        if node.end_lineno is not None and node.end_col_offset is not None:
            if node.lineno == node.end_lineno:
                line = self._source_lines[node.lineno - 1] if node.lineno <= len(self._source_lines) else ""
                return line[node.col_offset:node.end_col_offset]

        # Fallback: just return from col_offset to end of line
        if node.lineno <= len(self._source_lines):
            return self._source_lines[node.lineno - 1][node.col_offset:]
        return ""

    @staticmethod
    def module_to_fqn(module_path: str) -> str:
        """Convert a Python module path to a fully qualified name.

        Args:
            module_path: The Python module path (e.g., "collections.abc").

        Returns:
            The fully qualified name suitable for MethodMatcher patterns.
        """
        # Python module paths are already in a format suitable for FQN
        return module_path
