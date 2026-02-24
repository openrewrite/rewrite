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
import tempfile
from pathlib import Path
from typing import Any, Dict, List, Optional, Tuple

from ..java import JavaType


def compute_source_line_data(
        source: str,
) -> Tuple[List[str], List[int], Optional[Dict[int, List[int]]]]:
    """Split source into lines and compute byte offsets and byte-to-char mappings in one pass.

    Returns:
        source_lines: Lines with line endings stripped.
        line_byte_offsets: line_byte_offsets[i] is the byte offset of line i+1 (1-based).
        byte_to_char: Per-line byte-offset → char-offset mapping for non-ASCII lines only,
                      or None when the source is pure ASCII.
    """
    is_ascii = source.isascii()
    lines_with_endings = source.splitlines(True)
    source_lines: List[str] = []
    offsets: List[int] = [0]
    byte_to_char: Dict[int, List[int]] = {}

    for lineno, line in enumerate(lines_with_endings, start=1):
        # Strip line ending
        if line.endswith('\r\n'):
            source_lines.append(line[:-2])
        elif line.endswith(('\r', '\n')):
            source_lines.append(line[:-1])
        else:
            source_lines.append(line)

        if is_ascii:
            offsets.append(offsets[-1] + len(line))
        else:
            line_bytes = line.encode('utf-8')
            offsets.append(offsets[-1] + len(line_bytes))
            if len(line_bytes) != len(line):  # non-ASCII line — build byte→char index
                mapping: List[int] = []
                for char_idx, char in enumerate(line):
                    for _ in range(len(char.encode('utf-8'))):
                        mapping.append(char_idx)
                byte_to_char[lineno] = mapping

    return source_lines, offsets, (byte_to_char if byte_to_char else None)


# Shared Unknown singleton to avoid creating duplicate instances
_UNKNOWN = JavaType.Unknown()

# Mapping of Python builtin types to JavaType.Primitive
_PYTHON_PRIMITIVES: Dict[str, JavaType.Primitive] = {
    'str': JavaType.Primitive.String,
    'int': JavaType.Primitive.Int,
    'float': JavaType.Primitive.Double,
    'bool': JavaType.Primitive.Boolean,
    'None': JavaType.Primitive.None_,
    'NoneType': JavaType.Primitive.None_,
    'bytes': JavaType.Primitive.String,  # Close enough for matching
    'LiteralString': JavaType.Primitive.String,
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

    def __init__(self, source: str, file_path: Optional[str] = None, ty_client=None,
                 source_lines: Optional[List[str]] = None,
                 line_byte_offsets: Optional[List[int]] = None):
        """Initialize type mapping for a source file.

        Args:
            source: The Python source code.
            file_path: Optional file path for the source. If provided,
                      it will be used for ty-types queries.
            ty_client: Optional TyTypesClient instance. If provided and
                      already initialized, fetches types from ty-types.
            source_lines: Pre-computed list of lines (no endings). When provided
                         together with line_byte_offsets, avoids re-splitting source.
            line_byte_offsets: Pre-computed cumulative byte offsets per line.
        """
        self._source = source
        self._file_path = file_path
        self._temp_file: Optional[Path] = None
        self._type_cache: Dict[str, JavaType] = {}  # FQN -> JavaType (per-instance)

        # Use pre-computed values when available (e.g. supplied by ParserVisitor),
        # otherwise compute them here.
        if source_lines is not None and line_byte_offsets is not None:
            self._source_lines = source_lines
            self._line_byte_offsets = line_byte_offsets
        else:
            self._source_lines, self._line_byte_offsets, _ = compute_source_line_data(source)

        # Caches for byte offset and type ID lookups
        self._byte_offset_cache: Dict[Tuple[int, int], int] = {}
        self._lookup_cache: Dict[tuple, Optional[int]] = {}

        # ty-types data: populated by _build_index
        self._node_index: Dict[Tuple[int, int], Tuple[int, str]] = {}  # (start, end) -> (type_id, node_kind)
        self._node_index_by_start: Dict[int, List[Tuple[int, int, str]]] = {}  # start -> [(end, type_id, node_kind)]
        self._type_registry: Dict[int, Dict[str, Any]] = {}  # type_id -> TypeDescriptor
        self._call_signature_index: Dict[Tuple[int, int], Dict[str, Any]] = {}  # (start, end) -> callSignature
        self._type_id_cache: Dict[int, JavaType] = {}  # type_id -> resolved JavaType
        self._declaring_type_id_cache: Dict[int, JavaType.FullyQualified] = {}  # type_id -> resolved declaring type
        self._resolving_type_ids: set = set()  # type_ids currently being resolved (cycle detection)
        self._resolving_declaring_type_ids: set = set()
        self._cycle_placeholders: Dict[int, JavaType.Class] = {}  # placeholders created on cycle detection
        self._declaring_cycle_placeholders: Dict[int, JavaType.Class] = {}
        self._class_literal_index: Dict[str, int] = {}  # className -> classLiteral type_id

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
        When file_path is given but doesn't exist, writes source there.
        Callers are responsible for providing safe paths (e.g. within a temp directory).
        """
        if file_path:
            path = Path(file_path)
            if not path.is_absolute():
                path = path.resolve()
            if path.exists():
                return str(path)
            # File path given but doesn't exist — write source there.
            # The parent directory must already exist (caller should ensure this).
            try:
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

    def _pos_to_byte_offset(self, lineno: int, col_offset: int) -> int:
        """Convert AST (lineno, col_offset) to an absolute byte offset.

        Python's ast uses 1-based lineno and 0-based character col_offset (Python 3.8+).
        ty-types uses absolute byte offsets (ruff convention).
        Results are cached since the same position is often queried multiple times.
        """
        key = (lineno, col_offset)
        cached = self._byte_offset_cache.get(key)
        if cached is not None:
            return cached
        line_start = self._line_byte_offsets[lineno - 1]
        line_text = self._source_lines[lineno - 1] if lineno <= len(self._source_lines) else ""
        byte_col = len(line_text[:col_offset].encode('utf-8'))
        result = line_start + byte_col
        self._byte_offset_cache[key] = result
        return result

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
            tid = int(type_id_str)
            self._type_registry[tid] = descriptor
            # Index classLiterals by className for kind inference
            if descriptor.get('kind') == 'classLiteral':
                cn = descriptor.get('className', '')
                if cn:
                    self._class_literal_index[cn] = tid

    def _lookup_type_id(self, node: ast.AST) -> Optional[int]:
        """Look up a node's type ID by converting AST position to byte offset.

        Results are cached by node position to avoid redundant byte-offset
        conversions when the same node is queried from multiple call sites.
        """
        if not hasattr(node, 'lineno') or node.lineno is None:
            return None

        end_lineno = getattr(node, 'end_lineno', None)
        end_col_offset = getattr(node, 'end_col_offset', None)
        cache_key = (node.lineno, node.col_offset, end_lineno, end_col_offset)
        if cache_key in self._lookup_cache:
            return self._lookup_cache[cache_key]

        start = self._pos_to_byte_offset(node.lineno, node.col_offset)
        end = self._pos_to_byte_offset(end_lineno, end_col_offset) if end_lineno is not None else None

        result = None
        if end is not None:
            match = self._node_index.get((start, end))
            if match:
                result = match[0]  # type_id

        if result is None:
            # Fuzzy match by start offset — prefer the entry whose end is closest
            entries = self._node_index_by_start.get(start, [])
            if entries:
                if end is not None:
                    best = min(entries, key=lambda e: abs(e[0] - end))
                    result = best[1]  # type_id
                else:
                    result = entries[0][1]  # type_id of first match

        self._lookup_cache[cache_key] = result
        return result

    def _resolve_type(self, type_id: int) -> Optional[JavaType]:
        """Resolve a type ID to a JavaType, maximizing object reuse.

        Caches resolved types so the same type_id always returns the same
        object. Breaks cyclic type references by creating a placeholder Class
        only when a cycle is actually detected — the placeholder is updated
        in-place once resolution completes.
        """
        if type_id in self._type_id_cache:
            return self._type_id_cache[type_id]

        # Cycle detected — create a placeholder that will be updated later
        if type_id in self._resolving_type_ids:
            if type_id not in self._cycle_placeholders:
                placeholder = JavaType.Class()
                placeholder._flags_bit_map = 0
                placeholder._kind = JavaType.FullyQualified.Kind.Class
                placeholder._fully_qualified_name = ''
                self._cycle_placeholders[type_id] = placeholder
            return self._cycle_placeholders[type_id]

        descriptor = self._type_registry.get(type_id)
        if not descriptor:
            return None

        self._resolving_type_ids.add(type_id)
        try:
            result = self._descriptor_to_java_type(descriptor)
        finally:
            self._resolving_type_ids.discard(type_id)

        if result is None:
            return None

        # If a cycle created a placeholder for this type_id, update it in-place
        if type_id in self._cycle_placeholders:
            placeholder = self._cycle_placeholders.pop(type_id)
            if isinstance(result, JavaType.Class):
                placeholder._fully_qualified_name = result.fully_qualified_name
                placeholder._kind = result._kind
                # Copy enriched fields so cycle placeholders retain supertypes/methods
                for attr in ('_supertype', '_methods', '_type_parameters', '_interfaces',
                             '_members', '_owning_class', '_annotations'):
                    val = getattr(result, attr, None)
                    if val is not None:
                        setattr(placeholder, attr, val)
            elif isinstance(result, JavaType.Parameterized):
                if hasattr(result._type, 'fully_qualified_name'):
                    placeholder._fully_qualified_name = result._type.fully_qualified_name
            self._type_id_cache[type_id] = placeholder
            return placeholder

        # No cycle — cache the actual result directly for maximum reuse.
        # For Class types this preserves the object from _create_class_type,
        # ensuring FQN-based deduplication across type_ids.
        self._type_id_cache[type_id] = result
        return result

    def _descriptor_to_java_type(self, descriptor: Dict[str, Any]) -> Optional[JavaType]:
        """Convert a ty-types TypeDescriptor to a JavaType."""
        kind = descriptor.get('kind')

        if kind == 'instance':
            class_name = descriptor.get('className', '')
            if class_name in _PYTHON_PRIMITIVES:
                return _PYTHON_PRIMITIVES[class_name]

            # Resolve base class: prefer classId (enriched with supertypes/methods)
            class_id = descriptor.get('classId')
            if class_id is None:
                # Look up classLiteral by className to get kind/supertypes/methods
                class_id = self._class_literal_index.get(class_name)

            if class_id is not None:
                base_class = self._resolve_type(class_id)
                if not isinstance(base_class, JavaType.Class):
                    base_class = self._create_class_type(class_name)
            else:
                module_name = descriptor.get('moduleName')
                if module_name and module_name != 'builtins':
                    base_class = self._create_class_type(f"{module_name}.{class_name}")
                else:
                    base_class = self._create_class_type(class_name)

            # If typeArgs present, wrap in Parameterized
            type_args = descriptor.get('typeArgs')
            if type_args:
                resolved_args = []
                for arg_id in type_args:
                    arg_type = self._resolve_type(arg_id)
                    if arg_type is not None:
                        resolved_args.append(arg_type)
                if resolved_args:
                    param = JavaType.Parameterized()
                    param._type = base_class
                    param._type_parameters = resolved_args
                    return param

            return base_class

        elif kind == 'intLiteral':
            return JavaType.Primitive.Int

        elif kind == 'boolLiteral':
            return JavaType.Primitive.Boolean

        elif kind in ('stringLiteral', 'literalString'):
            return JavaType.Primitive.String

        elif kind == 'bytesLiteral':
            return JavaType.Primitive.String

        elif kind == 'union':
            # Resolve all non-None members into a Union type.
            # For Optional[X] (= X | None) with a single real member, unwrap to just X.
            resolved_bounds = []
            for member_id in descriptor.get('members', []):
                member = self._type_registry.get(member_id)
                if member:
                    member_kind = member.get('kind')
                    # Skip None/NoneType members
                    if member_kind == 'instance' and member.get('className') in ('None', 'NoneType'):
                        continue
                    resolved = self._resolve_type(member_id)
                    if resolved is not None:
                        resolved_bounds.append(resolved)
            if not resolved_bounds:
                return _UNKNOWN
            if len(resolved_bounds) == 1:
                return resolved_bounds[0]
            return JavaType.Union(_bounds=resolved_bounds)

        elif kind == 'module':
            module_name = descriptor.get('moduleName', '')
            return self._create_class_type(module_name)

        elif kind in ('function', 'boundMethod'):
            # Use structured return type if available
            return_type_id = descriptor.get('returnType')
            if return_type_id is not None:
                result = self._resolve_type(return_type_id)
                if result is not None:
                    return result
            return _UNKNOWN

        elif kind == 'classLiteral':
            class_name = descriptor.get('className', '')
            module_name = descriptor.get('moduleName')
            if module_name and module_name != 'builtins':
                fqn = f"{module_name}.{class_name}"
            else:
                fqn = class_name
            class_type = self._create_class_type(fqn)

            # Infer Kind from supertypes before resolving them
            supertypes = descriptor.get('supertypes', [])
            for st_id in supertypes:
                st_desc = self._type_registry.get(st_id)
                if st_desc:
                    st_kind = st_desc.get('kind')
                    st_name = st_desc.get('className', '')
                    if st_kind == 'classLiteral' and st_name == 'Enum':
                        class_type._kind = JavaType.FullyQualified.Kind.Enum
                        break
                    elif st_kind == 'specialForm' and st_desc.get('name', '') == 'typing.Protocol':
                        class_type._kind = JavaType.FullyQualified.Kind.Interface
                        break

            # Populate supertypes: first → _supertype, rest → _interfaces
            if supertypes and getattr(class_type, '_supertype', None) is None:
                super_type = self._resolve_type(supertypes[0])
                if isinstance(super_type, JavaType.FullyQualified):
                    class_type._supertype = super_type

                if len(supertypes) > 1 and getattr(class_type, '_interfaces', None) is None:
                    interfaces = []
                    for st_id in supertypes[1:]:
                        iface = self._resolve_type(st_id)
                        if isinstance(iface, JavaType.FullyQualified):
                            interfaces.append(iface)
                    if interfaces:
                        class_type._interfaces = interfaces

            # Populate type parameters from typeVar descriptors
            type_params = descriptor.get('typeParameters', [])
            if type_params and getattr(class_type, '_type_parameters', None) is None:
                resolved_type_params = []
                for tp_id in type_params:
                    tp_type = self._resolve_type(tp_id)
                    if tp_type is not None:
                        resolved_type_params.append(tp_type)
                if resolved_type_params:
                    class_type._type_parameters = resolved_type_params

            # Populate methods from function/boundMethod members
            members = descriptor.get('members', [])
            if members and getattr(class_type, '_methods', None) is None:
                methods = []
                for member in members:
                    member_type_id = member.get('typeId') if isinstance(member, dict) else member
                    if member_type_id is None:
                        continue
                    member_desc = self._type_registry.get(member_type_id)
                    if member_desc and member_desc.get('kind') in ('function', 'boundMethod'):
                        method = self._create_method_from_descriptor(member_desc, class_type)
                        if method:
                            methods.append(method)
                class_type._methods = methods if methods else None

            return class_type

        elif kind == 'typedDict':
            name = descriptor.get('name', '')
            if name:
                return self._create_class_type(name)
            return _UNKNOWN

        elif kind == 'subclassOf':
            base_id = descriptor.get('base')
            if base_id is not None:
                result = self._resolve_type(base_id)
                if result is not None:
                    return result
            return _UNKNOWN

        elif kind == 'newType':
            name = descriptor.get('name', '')
            if name:
                return self._create_class_type(name)
            return _UNKNOWN

        elif kind == 'intersection':
            for member_id in descriptor.get('positive', []):
                result = self._resolve_type(member_id)
                if result is not None:
                    return result
            return _UNKNOWN

        elif kind in ('dynamic', 'never'):
            return _UNKNOWN

        elif kind == 'enumLiteral':
            class_name = descriptor.get('className', '')
            class_type = self._create_class_type(class_name)
            class_type._kind = JavaType.FullyQualified.Kind.Enum
            return class_type

        elif kind == 'property':
            return _UNKNOWN

        elif kind == 'typeVar':
            name = descriptor.get('name', '')
            if not name:
                return _UNKNOWN
            variance_str = descriptor.get('variance', 'invariant')
            variance_map = {
                'covariant': JavaType.GenericTypeVariable.Variance.Covariant,
                'contravariant': JavaType.GenericTypeVariable.Variance.Contravariant,
            }
            variance = variance_map.get(variance_str, JavaType.GenericTypeVariable.Variance.Invariant)
            bounds = None
            upper_bound_id = descriptor.get('upperBound')
            if upper_bound_id is not None:
                bound_type = self._resolve_type(upper_bound_id)
                if bound_type is not None:
                    bounds = [bound_type]
            return JavaType.GenericTypeVariable(_name=name, _variance=variance, _bounds=bounds)

        else:
            return _UNKNOWN

    def close(self) -> None:
        """Clean up temporary files."""
        if self._temp_file and self._temp_file.exists():
            try:
                self._temp_file.unlink()
            except OSError:
                pass

    def type(self, node: ast.AST) -> Optional[JavaType]:
        """Get the expression type for an AST node.

        For call expressions this returns the return type of the call,
        NOT a JavaType.Method. Use method_invocation_type() when you
        need the full method signature.

        Args:
            node: The AST node to get the type for.

        Returns:
            The JavaType for the node, or None if the type cannot be determined.
        """
        if isinstance(node, ast.Constant):
            return self._constant_type(node)

        # Try to look up in ty-types index
        type_id = self._lookup_type_id(node)
        if type_id is not None:
            return self._resolve_type(type_id)

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

    def _is_variable_descriptor(self, descriptor: Dict[str, Any]) -> bool:
        """Check if a type descriptor represents a variable (not a function, class, or module)."""
        kind = descriptor.get('kind')
        return kind not in ('function', 'boundMethod', 'module', 'classLiteral')

    def name_type_info(self, node: ast.Name) -> Tuple[Optional[JavaType], Optional[JavaType.Variable]]:
        """Get expression type and variable type for a name reference.

        Returns (expression_type, variable_field_type).
        """
        type_id = self._lookup_type_id(node)
        if type_id is None:
            return None, None

        expr_type = self._resolve_type(type_id)
        descriptor = self._type_registry.get(type_id)
        if descriptor and self._is_variable_descriptor(descriptor):
            return expr_type, JavaType.Variable(_name=node.id, _type=expr_type)
        return expr_type, None

    def param_type_info(self, node: ast.arg) -> Tuple[Optional[JavaType], Optional[JavaType.Variable]]:
        """Get expression type and variable type for a function parameter.

        Returns (expression_type, variable_field_type).
        """
        expr_type = self.type(node)
        if expr_type is None:
            return None, None
        return expr_type, JavaType.Variable(_name=node.arg, _type=expr_type)

    def attribute_type_info(self, node: ast.Attribute,
                            receiver_type: Optional[JavaType] = None
                            ) -> Tuple[Optional[JavaType], Optional[JavaType.Variable]]:
        """Get expression type and variable type for an attribute access.

        Returns (expression_type, variable_field_type).
        """
        type_id = self._lookup_type_id(node)
        if type_id is None:
            return None, None

        expr_type = self._resolve_type(type_id)
        descriptor = self._type_registry.get(type_id)
        if descriptor and self._is_variable_descriptor(descriptor):
            return expr_type, JavaType.Variable(_name=node.attr, _type=expr_type, _owner=receiver_type)
        return expr_type, None

    def method_declaration_type(self, node: ast.FunctionDef) -> Optional[JavaType.Method]:
        """Get the method type for a function/method declaration.

        Builds a JavaType.Method from the function's type descriptor when
        available (parameters + returnType fields), falling back to resolving
        parameter annotations and return annotation individually via ty-types.

        Args:
            node: The ast.FunctionDef or ast.AsyncFunctionDef node.

        Returns:
            A JavaType.Method, or None if types cannot be determined.
        """
        # First try: use structured data from the function descriptor
        type_id = self._lookup_type_id(node)
        if type_id is not None:
            descriptor = self._type_registry.get(type_id)
            if descriptor and descriptor.get('kind') in ('function', 'boundMethod'):
                # If the descriptor has parameters/returnType, use them directly
                params = descriptor.get('parameters')
                ret_id = descriptor.get('returnType')
                if params is not None or ret_id is not None:
                    return self._method_from_function_descriptor(
                        descriptor, node.name)

        # Fallback: build from individual parameter/return annotation types
        param_names: List[str] = []
        param_types: List[JavaType] = []
        for arg in node.args.args:
            if arg.arg in ('self', 'cls'):
                continue
            param_names.append(arg.arg)
            if arg.annotation is not None:
                t = self.type(arg.annotation)
                param_types.append(t if t is not None else _UNKNOWN)
            else:
                param_types.append(_UNKNOWN)

        return_type = None
        if node.returns is not None:
            return_type = self.type(node.returns)

        # Extract type parameter names from Python 3.12+ type_params
        type_param_names: List[str] = []
        for tp in getattr(node, 'type_params', []) or []:
            if hasattr(tp, 'name'):
                type_param_names.append(tp.name)

        if not param_names and return_type is None and not type_param_names:
            return None

        return JavaType.Method(
            _flags_bit_map=0,
            _declaring_type=None,
            _name=node.name,
            _return_type=return_type,
            _parameter_names=param_names if param_names else None,
            _parameter_types=param_types if param_types else None,
            _declared_formal_type_names=type_param_names if type_param_names else None,
        )

    def _method_from_function_descriptor(
            self, descriptor: Dict[str, Any], name: str
    ) -> JavaType.Method:
        """Build a JavaType.Method from a function descriptor with parameters/returnType."""
        param_names: List[str] = []
        param_types: List[JavaType] = []
        for param in descriptor.get('parameters', []):
            p_name = param.get('name', '')
            if p_name in ('self', 'cls'):
                continue
            param_names.append(p_name)
            param_types.append(self._resolve_param_type(param))

        return_type = None
        ret_id = descriptor.get('returnType')
        if ret_id is not None:
            return_type = self._resolve_type(ret_id)

        type_param_names = self._extract_type_param_names(descriptor)

        return JavaType.Method(
            _flags_bit_map=0,
            _declaring_type=None,
            _name=name,
            _return_type=return_type,
            _parameter_names=param_names if param_names else None,
            _parameter_types=param_types if param_types else None,
            _declared_formal_type_names=type_param_names if type_param_names else None,
        )

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

        # Extract type parameter names from function descriptor
        type_param_names: List[str] = []
        func_type_id = self._lookup_func_type_id(node)
        if func_type_id is not None:
            func_desc = self._type_registry.get(func_type_id)
            if func_desc:
                type_param_names = self._extract_type_param_names(func_desc)

        return JavaType.Method(
            _flags_bit_map=0,
            _declaring_type=declaring_type,
            _name=method_name,
            _return_type=return_type,
            _parameter_names=param_names if param_names else None,
            _parameter_types=param_types if param_types else None,
            _declared_formal_type_names=type_param_names if type_param_names else None,
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
            result = self._resolve_type(type_id)
            if result is not None:
                return result
        return _UNKNOWN

    def _get_method_signature(self, node: ast.Call) -> Tuple[List[str], List[JavaType]]:
        """Get parameter names and types from the method signature.

        Uses structured call signature from ty-types when available,
        then falls back to function/method descriptor parameters,
        then to placeholder names.
        """
        # Try structured call signature from ty-types (most specific)
        sig = self._lookup_call_signature(node)
        if sig:
            params = sig.get('parameters', [])
            if params:
                names = [p['name'] for p in params
                         if p['name'] not in ('self', 'cls')]
                types = [self._resolve_param_type(p) for p in params
                         if p['name'] not in ('self', 'cls')]
                return names, types

        # Try function/method descriptor parameters
        func_type_id = self._lookup_func_type_id(node)
        if func_type_id is not None:
            descriptor = self._type_registry.get(func_type_id)
            if descriptor:
                params = descriptor.get('parameters', [])
                if params:
                    names = [p['name'] for p in params
                             if p['name'] not in ('self', 'cls')]
                    types = [self._resolve_param_type(p) for p in params
                             if p['name'] not in ('self', 'cls')]
                    return names, types

        # Fall back to placeholder names
        return self._generate_placeholder_names(node)

    def _lookup_func_type_id(self, node: ast.Call) -> Optional[int]:
        """Look up the type ID of the function/method being called."""
        if isinstance(node.func, ast.Attribute):
            return self._lookup_type_id(node.func)
        elif isinstance(node.func, ast.Name):
            return self._lookup_type_id(node.func)
        return None

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
                return self._resolve_declaring_type(type_id)

        elif isinstance(node.func, ast.Name):
            # For function calls, look up the function name
            type_id = self._lookup_type_id(node.func)
            if type_id is not None:
                descriptor = self._type_registry.get(type_id)
                if descriptor:
                    kind = descriptor.get('kind')
                    if kind == 'module':
                        return self._create_class_type(descriptor.get('moduleName', ''))
                    elif kind in ('function', 'boundMethod'):
                        module_name = descriptor.get('moduleName')
                        if module_name and module_name != 'builtins':
                            return self._create_class_type(module_name)

        return self._infer_declaring_type_from_ast(node)

    def _resolve_declaring_type(self, type_id: int) -> Optional[JavaType.FullyQualified]:
        """Resolve a type ID to a declaring type, maximizing object reuse.

        NOTE: The cycle-detection pattern here mirrors _resolve_type intentionally.
        They use separate caches and placeholder dicts because declaring types are
        resolved independently (often to a simpler Class without methods/members).
        """
        if type_id in self._declaring_type_id_cache:
            return self._declaring_type_id_cache[type_id]

        if type_id in self._resolving_declaring_type_ids:
            if type_id not in self._declaring_cycle_placeholders:
                placeholder = JavaType.Class()
                placeholder._flags_bit_map = 0
                placeholder._kind = JavaType.FullyQualified.Kind.Class
                placeholder._fully_qualified_name = ''
                self._declaring_cycle_placeholders[type_id] = placeholder
            return self._declaring_cycle_placeholders[type_id]

        descriptor = self._type_registry.get(type_id)
        if not descriptor:
            return None

        self._resolving_declaring_type_ids.add(type_id)
        try:
            result = self._declaring_type_from_descriptor(descriptor)
        finally:
            self._resolving_declaring_type_ids.discard(type_id)

        if result is None:
            return None

        if type_id in self._declaring_cycle_placeholders:
            placeholder = self._declaring_cycle_placeholders.pop(type_id)
            if isinstance(result, JavaType.Class):
                placeholder._fully_qualified_name = result.fully_qualified_name
                placeholder._kind = result._kind
            self._declaring_type_id_cache[type_id] = placeholder
            return placeholder

        self._declaring_type_id_cache[type_id] = result
        return result

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

        elif kind == 'typedDict':
            name = descriptor.get('name', '')
            if name:
                return self._create_class_type(name)
            return None

        elif kind == 'subclassOf':
            base_id = descriptor.get('base')
            if base_id is not None:
                return self._resolve_declaring_type(base_id)
            return None

        elif kind == 'newType':
            name = descriptor.get('name', '')
            if name:
                return self._create_class_type(name)
            return None

        elif kind == 'intersection':
            for member_id in descriptor.get('positive', []):
                result = self._resolve_declaring_type(member_id)
                if result is not None:
                    return result
            return None

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
                    return self._resolve_declaring_type(member_id)

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
            java_type = self._resolve_type(type_id)
            if isinstance(java_type, JavaType.Class):
                return java_type
            if isinstance(java_type, JavaType.Parameterized):
                # For declaring type, unwrap to base class
                return java_type._type if isinstance(java_type._type, JavaType.FullyQualified) else java_type
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
                parts.append(current.attr)
                current = current.value
            if isinstance(current, ast.Name):
                parts.append(current.id)

            # Only create a class type if we have an attribute chain (len > 1)
            if len(parts) > 1:
                parts.reverse()
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
                param_types.append(_UNKNOWN)

        # Handle keyword arguments as well
        for kw in node.keywords:
            kw_type = self.type(kw.value)
            if kw_type:
                param_types.append(kw_type)
            else:
                param_types.append(_UNKNOWN)

        return param_types if param_types else None

    def _get_return_type(self, node: ast.Call) -> Optional[JavaType]:
        """Get the return type of a method call.

        Prefers the call-site-specific returnTypeId from the call signature
        (which gives resolved types like int instead of generic T),
        then tries the ExprCall node type, then falls back to the function
        descriptor's returnType field.
        """
        # Prefer call-site-specific return type from call signature
        sig = self._lookup_call_signature(node)
        if sig:
            ret_id = sig.get('returnTypeId')
            if ret_id is not None:
                result = self._resolve_type(ret_id)
                if result is not None:
                    return result

        # The type of an ExprCall node in ty-types IS the return type
        type_id = self._lookup_type_id(node)
        if type_id is not None:
            return self._resolve_type(type_id)

        # Fall back to function descriptor's structured returnType
        func_type_id = self._lookup_func_type_id(node)
        if func_type_id is not None:
            func_desc = self._type_registry.get(func_type_id)
            if func_desc:
                ret_id = func_desc.get('returnType')
                if ret_id is not None:
                    return self._resolve_type(ret_id)
        return None

    def _create_method_from_descriptor(self, descriptor: Dict[str, Any],
                                        declaring_type: JavaType.FullyQualified) -> Optional[JavaType.Method]:
        """Create a JavaType.Method from a ty-types function/boundMethod descriptor."""
        name = descriptor.get('name', '')
        if not name:
            return None

        # Resolve return type
        return_type = None
        return_type_id = descriptor.get('returnType')
        if return_type_id is not None:
            return_type = self._resolve_type(return_type_id)

        # Resolve parameters (skip self/cls)
        param_names = []
        param_types = []
        for param in descriptor.get('parameters', []):
            p_name = param.get('name', '')
            if p_name in ('self', 'cls'):
                continue
            param_names.append(p_name)
            p_type_id = param.get('typeId')
            if p_type_id is not None:
                p_type = self._resolve_type(p_type_id)
                param_types.append(p_type if p_type else _UNKNOWN)
            else:
                param_types.append(_UNKNOWN)

        type_param_names = self._extract_type_param_names(descriptor)

        return JavaType.Method(
            _flags_bit_map=0,
            _declaring_type=declaring_type,
            _name=name,
            _return_type=return_type,
            _parameter_names=param_names if param_names else None,
            _parameter_types=param_types if param_types else None,
            _declared_formal_type_names=type_param_names if type_param_names else None,
        )

    def _extract_type_param_names(self, descriptor: Dict[str, Any]) -> List[str]:
        """Extract type parameter names from a descriptor's typeParameters list."""
        names: List[str] = []
        for tp_id in descriptor.get('typeParameters', []):
            tp_desc = self._type_registry.get(tp_id)
            if tp_desc and tp_desc.get('kind') == 'typeVar':
                name = tp_desc.get('name', '')
                if name:
                    names.append(name)
        return names

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

    @staticmethod
    def module_to_fqn(module_path: str) -> str:
        """Convert a Python module path to a fully qualified name."""
        return module_path
