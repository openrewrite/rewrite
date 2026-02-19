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
Python to JavaType mapping using ty-types for type inference.

This module provides type attribution for Python code by querying the ty-types
CLI for structured type descriptors. All node types for a file are fetched in
a single batch call, then looked up by byte offset. The type information is
mapped to OpenRewrite's JavaType model to enable Java recipes like FindMethods
to work on Python.
"""

from __future__ import annotations

import ast
import os
import re
import tempfile
from pathlib import Path
from typing import Any, Dict, List, Optional, Tuple

from ..java import JavaType



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

    This class uses the ty-types CLI to infer types for Python code and
    converts them to JavaType objects that can be used by Java recipes
    like FindMethods, ChangeMethodName, etc.

    All types for a file are fetched in a single batch call during __init__,
    then individual nodes are looked up by byte offset.

    Usage:
        mapping = PythonTypeMapping(source, file_path="/path/to/file.py")
        method_type = mapping.method_invocation_type(call_node)
    """

    # Cache for type mappings to avoid repeated class type creation
    _type_cache: Dict[str, JavaType] = {}

    def __init__(self, source: str, file_path: Optional[str] = None, ty_client=None):
        """Initialize type mapping for a source file.

        Args:
            source: The Python source code.
            file_path: Optional file path for the source. If provided,
                      it will be used for ty-types queries.
            ty_client: Optional TyTypesClient instance. If provided and
                      already initialized, fetches types from ty-types.
        """
        self._source = source
        self._file_path = file_path
        self._source_lines = source.splitlines()
        self._temp_file: Optional[Path] = None

        # Compute line byte offsets for position conversion
        self._line_byte_offsets = self._compute_line_byte_offsets(source)

        # ty-types data: populated by _build_index
        self._node_index: Dict[Tuple[int, int], Tuple[int, str]] = {}  # (start, end) -> (type_id, node_kind)
        self._node_index_by_start: Dict[int, List[Tuple[int, int, str]]] = {}  # start -> [(end, type_id, node_kind)]
        self._type_registry: Dict[int, Dict[str, Any]] = {}  # type_id -> TypeDescriptor
        self._call_signature_index: Dict[Tuple[int, int], Dict[str, Any]] = {}  # (start, end) -> callSignature

        # Fetch all types in one batch call
        if ty_client is not None:
            try:
                self._fetch_types(source, file_path, ty_client)
            except RuntimeError:
                pass

    def _fetch_types(self, source: str, file_path: Optional[str], client) -> None:
        """Fetch all types for this file from ty-types.

        The client must already be initialized with a project root.
        """
        if not client.is_available:
            return

        # Determine the actual file path on disk
        actual_file = self._ensure_file_on_disk(source, file_path)
        if actual_file is None:
            return

        # Fetch all types in one call
        result = client.get_types(actual_file)
        if result:
            self._build_index(result)

    def _ensure_file_on_disk(self, source: str, file_path: Optional[str]) -> Optional[str]:
        """Ensure the source is available as a file on disk for ty-types.

        Returns the absolute file path, or None if unavailable.
        """
        if file_path:
            path = Path(file_path)
            if not path.is_absolute():
                path = path.resolve()
            if path.exists():
                return str(path)
            # File path given but doesn't exist — write source there
            try:
                path.parent.mkdir(parents=True, exist_ok=True)
                path.write_text(source, encoding='utf-8')
                self._temp_file = path
                return str(path)
            except OSError:
                return None
        else:
            # No file path — create temp file
            try:
                fd, tmp_path = tempfile.mkstemp(suffix='.py')
                os.close(fd)
                Path(tmp_path).write_text(source, encoding='utf-8')
                self._temp_file = Path(tmp_path)
                return tmp_path
            except OSError:
                return None

    @staticmethod
    def _compute_line_byte_offsets(source: str) -> List[int]:
        """Compute the byte offset of the start of each line.

        Returns a list where index i is the byte offset of line i+1 (1-based).
        """
        offsets = [0]
        for line in source.splitlines(True):
            offsets.append(offsets[-1] + len(line.encode('utf-8')))
        return offsets

    def _pos_to_byte_offset(self, lineno: int, col_offset: int) -> int:
        """Convert AST (lineno, col_offset) to an absolute byte offset.

        Python's ast uses 1-based lineno and character-based col_offset.
        ty-types uses absolute byte offsets (ruff convention).
        """
        line_start = self._line_byte_offsets[lineno - 1]
        line_text = self._source_lines[lineno - 1] if lineno <= len(self._source_lines) else ""
        byte_col = len(line_text[:col_offset].encode('utf-8'))
        return line_start + byte_col

    def _build_index(self, result: Dict[str, Any]) -> None:
        """Build the byte-offset lookup index from a getTypes response."""
        for node in result.get('nodes', []):
            type_id = node.get('typeId')
            if type_id is not None:
                start = node['start']
                end = node['end']
                kind = node['nodeKind']
                self._node_index[(start, end)] = (type_id, kind)
                if start not in self._node_index_by_start:
                    self._node_index_by_start[start] = []
                self._node_index_by_start[start].append((end, type_id, kind))

            # Index call signature data for ExprCall nodes
            call_sig = node.get('callSignature')
            if call_sig is not None:
                self._call_signature_index[(node['start'], node['end'])] = call_sig

        # Merge types into registry (keys are strings in JSON)
        for type_id_str, descriptor in result.get('types', {}).items():
            self._type_registry[int(type_id_str)] = descriptor

    def _lookup_type_id(self, node: ast.AST) -> Optional[int]:
        """Look up a node's type ID by converting AST position to byte offset."""
        if not hasattr(node, 'lineno') or node.lineno is None:
            return None

        start = self._pos_to_byte_offset(node.lineno, node.col_offset)

        if hasattr(node, 'end_lineno') and node.end_lineno is not None:
            end = self._pos_to_byte_offset(node.end_lineno, node.end_col_offset)
            # Try exact match first
            result = self._node_index.get((start, end))
            if result:
                return result[0]  # type_id

        # Fuzzy match by start offset
        entries = self._node_index_by_start.get(start, [])
        if entries:
            return entries[0][1]  # type_id of first match

        return None

    def _descriptor_to_java_type(self, descriptor: Dict[str, Any]) -> Optional[JavaType]:
        """Convert a ty-types TypeDescriptor to a JavaType."""
        kind = descriptor.get('kind')

        if kind == 'instance':
            class_name = descriptor.get('className', '')
            if class_name in _PYTHON_PRIMITIVES:
                return _PYTHON_PRIMITIVES[class_name]
            module_name = descriptor.get('moduleName')
            if module_name and module_name != 'builtins':
                return self._create_class_type(f"{module_name}.{class_name}")
            return self._create_class_type(class_name)

        elif kind == 'intLiteral':
            return JavaType.Primitive.Int

        elif kind == 'boolLiteral':
            return JavaType.Primitive.Boolean

        elif kind in ('stringLiteral', 'literalString'):
            return JavaType.Primitive.String

        elif kind == 'bytesLiteral':
            return JavaType.Primitive.String

        elif kind == 'union':
            # Unwrap union: take first non-None type
            for member_id in descriptor.get('members', []):
                member = self._type_registry.get(member_id)
                if member:
                    member_kind = member.get('kind')
                    # Skip None/NoneType members
                    if member_kind == 'instance' and member.get('className') in ('None', 'NoneType'):
                        continue
                    return self._descriptor_to_java_type(member)
            return JavaType.Unknown()

        elif kind == 'module':
            module_name = descriptor.get('moduleName', '')
            return self._create_class_type(module_name)

        elif kind in ('function', 'boundMethod'):
            # Return type based on display string
            display = descriptor.get('display', '')
            if display:
                return self._type_string_to_java_type(display)
            return JavaType.Unknown()

        elif kind == 'classLiteral':
            class_name = descriptor.get('className', '')
            return self._create_class_type(class_name)

        elif kind in ('dynamic', 'never'):
            return JavaType.Unknown()

        else:
            # Try display string as fallback
            display = descriptor.get('display', '')
            if display:
                return self._type_string_to_java_type(display)
            return JavaType.Unknown()

    def close(self) -> None:
        """Clean up temporary files."""
        if self._temp_file and self._temp_file.exists():
            try:
                self._temp_file.unlink()
            except OSError:
                pass

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
            return self.method_invocation_type(node)

        # Try to look up in ty-types index
        type_id = self._lookup_type_id(node)
        if type_id is not None:
            descriptor = self._type_registry.get(type_id)
            if descriptor:
                return self._descriptor_to_java_type(descriptor)

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

        # Get declaring type
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

    def _lookup_call_signature(self, node: ast.Call) -> Optional[Dict[str, Any]]:
        """Look up structured call signature data for a Call node."""
        if not hasattr(node, 'lineno') or node.lineno is None:
            return None

        start = self._pos_to_byte_offset(node.lineno, node.col_offset)
        if hasattr(node, 'end_lineno') and node.end_lineno is not None:
            end = self._pos_to_byte_offset(node.end_lineno, node.end_col_offset)
            return self._call_signature_index.get((start, end))
        return None

    def _resolve_param_type(self, param: Dict[str, Any]) -> JavaType:
        """Resolve a ParameterInfo's typeId to a JavaType."""
        type_id = param.get('typeId')
        if type_id is not None:
            descriptor = self._type_registry.get(type_id)
            if descriptor:
                java_type = self._descriptor_to_java_type(descriptor)
                if java_type is not None:
                    return java_type
        return JavaType.Unknown()

    def _get_method_signature(self, node: ast.Call) -> Tuple[List[str], List[JavaType]]:
        """Get parameter names and types from the method signature.

        Uses structured call signature from ty-types when available,
        otherwise falls back to placeholder names.
        """
        # Try structured call signature from ty-types
        sig = self._lookup_call_signature(node)
        if sig:
            params = sig.get('parameters', [])
            if params:
                names = [p['name'] for p in params]
                types = [self._resolve_param_type(p) for p in params]
                return names, types

        # Fall back to placeholder names
        return self._generate_placeholder_names(node)

    def _generate_placeholder_names(self, node: ast.Call) -> Tuple[List[str], List[JavaType]]:
        """Generate placeholder parameter names when signature parsing fails."""
        param_types = self._get_parameter_types(node) or []
        names = [f"arg{i}" for i in range(len(param_types))]
        return names, param_types

    def _get_declaring_type(self, node: ast.Call) -> Optional[JavaType.FullyQualified]:
        """Get the declaring type (class/module) for a method call."""
        if isinstance(node.func, ast.Attribute):
            receiver = node.func.value

            # For chained calls like "hello".upper().split(), the receiver is a Call
            if isinstance(receiver, ast.Call):
                return self._get_call_return_type(receiver)

            # Try to look up receiver type in ty-types index
            type_id = self._lookup_type_id(receiver)
            if type_id is not None:
                descriptor = self._type_registry.get(type_id)
                if descriptor:
                    return self._declaring_type_from_descriptor(descriptor)

        elif isinstance(node.func, ast.Name):
            # For function calls, look up the function name
            type_id = self._lookup_type_id(node.func)
            if type_id is not None:
                descriptor = self._type_registry.get(type_id)
                if descriptor:
                    kind = descriptor.get('kind')
                    if kind == 'module':
                        return self._create_class_type(descriptor.get('moduleName', ''))

        return self._infer_declaring_type_from_ast(node)

    def _declaring_type_from_descriptor(self, descriptor: Dict[str, Any]) -> Optional[JavaType.FullyQualified]:
        """Extract a declaring type (class/module) from a TypeDescriptor."""
        kind = descriptor.get('kind')

        if kind == 'module':
            module_name = descriptor.get('moduleName', '')
            return self._create_class_type(module_name)

        elif kind == 'instance':
            class_name = descriptor.get('className', '')
            module_name = descriptor.get('moduleName')
            if module_name and module_name != 'builtins':
                return self._create_class_type(f"{module_name}.{class_name}")
            return self._create_class_type(class_name)

        elif kind in ('stringLiteral', 'literalString'):
            return self._create_class_type('str')

        elif kind == 'intLiteral':
            return self._create_class_type('int')

        elif kind == 'boolLiteral':
            return self._create_class_type('bool')

        elif kind == 'bytesLiteral':
            return self._create_class_type('bytes')

        elif kind == 'union':
            # Unwrap union: use first non-None member as declaring type
            for member_id in descriptor.get('members', []):
                member = self._type_registry.get(member_id)
                if member:
                    if member.get('kind') == 'instance' and member.get('className') in ('None', 'NoneType'):
                        continue
                    return self._declaring_type_from_descriptor(member)

        elif kind == 'classLiteral':
            class_name = descriptor.get('className', '')
            return self._create_class_type(class_name)

        return None

    def _get_call_return_type(self, call_node: ast.Call) -> Optional[JavaType.FullyQualified]:
        """Get the return type of a function/method call as a class type.

        For chained calls like "hello".upper().split(), this returns the type
        that .upper() returns (str), which is then the declaring type for .split().
        """
        # The type of an ExprCall IS the return type
        type_id = self._lookup_type_id(call_node)
        if type_id is not None:
            descriptor = self._type_registry.get(type_id)
            if descriptor:
                java_type = self._descriptor_to_java_type(descriptor)
                if isinstance(java_type, JavaType.Class):
                    return java_type
                if isinstance(java_type, JavaType.Primitive):
                    return self._create_class_type(
                        _PRIMITIVE_TO_PYTHON.get(java_type, java_type.name.lower())
                    )
        return None

    def _infer_declaring_type_from_ast(self, node: ast.Call) -> Optional[JavaType.FullyQualified]:
        """Infer declaring type from AST when ty-types data is unavailable."""
        if isinstance(node.func, ast.Attribute):
            receiver = node.func.value

            # Handle Python builtin types from literals
            if isinstance(receiver, ast.Constant):
                if isinstance(receiver.value, str):
                    return self._create_class_type('str')
                elif isinstance(receiver.value, bytes):
                    return self._create_class_type('bytes')
                elif isinstance(receiver.value, (int, float, bool)):
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

            # Try to build a fully qualified name from the attribute chain
            parts = []
            current = receiver
            while isinstance(current, ast.Attribute):
                parts.insert(0, current.attr)
                current = current.value
            if isinstance(current, ast.Name):
                parts.insert(0, current.id)

            # Only create a class type if we have an attribute chain (len > 1)
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
        """Get the return type of a method call.

        The type of an ExprCall node in ty-types IS the return type.
        """
        type_id = self._lookup_type_id(node)
        if type_id is not None:
            descriptor = self._type_registry.get(type_id)
            if descriptor:
                return self._descriptor_to_java_type(descriptor)
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
            if (literal_value.startswith('"') and literal_value.endswith('"')) or \
               (literal_value.startswith("'") and literal_value.endswith("'")):
                return self._create_class_type('str')  # ty: ignore[invalid-return-type]
            elif literal_value in ('True', 'False'):
                return JavaType.Primitive.Boolean
            elif literal_value.isdigit() or (literal_value.startswith('-') and literal_value[1:].isdigit()):
                return JavaType.Primitive.Int
            return self._create_class_type('str')

        # Handle Optional[T], List[T], etc.
        generic_match = re.match(r'(\w+)\[(.+)\]', type_str)
        if generic_match:
            base = generic_match.group(1).lower()
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
                    inner = generic_match.group(2)
                    return self._type_string_to_java_type(inner)
                return self._create_class_type(type_mapping[base])
            return self._create_class_type(generic_match.group(1))

        # Handle union types: T | None, Union[T, None]
        if ' | ' in type_str:
            parts = [p.strip() for p in type_str.split(' | ')]
            for part in parts:
                if part not in ('None', 'NoneType', 'Unknown'):
                    return self._type_string_to_java_type(part)
            return JavaType.Unknown()

        # Default to class type
        return self._create_class_type(type_str)  # ty: ignore[invalid-return-type]

    def _create_class_type(self, fqn: str) -> JavaType.Class:
        """Create a JavaType.Class from a fully qualified name."""
        if fqn in self._type_cache:
            cached = self._type_cache[fqn]
            if isinstance(cached, JavaType.Class):
                return cached

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

    def _discover_venv(self, file_path: str) -> Optional[Path]:
        """Discover the virtual environment for a file.

        Walks up from the file's directory looking for a .venv directory.
        """
        path = Path(file_path)
        if not path.is_absolute():
            path = path.resolve()

        current = path.parent
        for _ in range(20):
            venv = current / '.venv'
            if venv.is_dir():
                return venv

            pyproject = current / 'pyproject.toml'
            if pyproject.exists():
                break

            parent = current.parent
            if parent == current:
                break
            current = parent

        return None

    @staticmethod
    def module_to_fqn(module_path: str) -> str:
        """Convert a Python module path to a fully qualified name."""
        return module_path
