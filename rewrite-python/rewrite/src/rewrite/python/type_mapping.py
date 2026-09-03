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
from dataclasses import dataclass, field
from pathlib import Path
from typing import Any, Dict, Iterator, List, Optional, Sequence, Set, Tuple

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

# ty-types descriptor kinds that map to JavaType.Method
_FUNCTION_KINDS = frozenset(('function', 'boundMethod', 'callable', 'wrapperDescriptor'))

# `os.path` binds whichever of these the running platform provides, so a symbol
# defined in one has no portable defining name. `os.path` names the same module
# object on every platform, so it is the one to key them by.
_ALIASED_MODULES: Dict[str, str] = {
    'posixpath': 'os.path',
    'ntpath': 'os.path',
    'genericpath': 'os.path',
}

# knownInstance descriptors carry no moduleName, and most of the singletons ty
# reports live in `typing`. These `knownInstanceKind`s are the ones that don't.
_KNOWN_INSTANCE_FQNS: Dict[str, str] = {
    'Range': 'range',
    'FunctoolsPartial': 'functools.partial',
    'FunctoolsPartialCall': 'functools.partial',
}


def _module_scope_statements(body: Sequence[ast.stmt]) -> Iterator[ast.stmt]:
    """``body`` plus the statements an `if` with no `else` nests, since those bind into
    the scope around it.

    `import_utils.unconditional_body` is the same rule over the LST, for `RemoveImport`;
    attribution runs during parse, where only `ast` exists. Keep the two in step.
    """
    for stmt in body:
        yield stmt
        if isinstance(stmt, ast.If) and not stmt.orelse:
            yield from _module_scope_statements(stmt.body)


def _module_all_names(tree: ast.Module) -> Optional[Set[str]]:
    """The names ``__all__`` declares via top-level literal list/tuple assignments
    (plain, annotated, or augmented), or None when the module has no such ``__all__``."""
    names: Optional[Set[str]] = None
    for stmt in tree.body:
        if isinstance(stmt, ast.Assign):
            targets, value = stmt.targets, stmt.value
        elif isinstance(stmt, (ast.AnnAssign, ast.AugAssign)):
            targets, value = [stmt.target], stmt.value
        else:
            continue
        if not isinstance(value, (ast.List, ast.Tuple)):
            continue
        if any(isinstance(t, ast.Name) and t.id == '__all__' for t in targets):
            if names is None:
                names = set()
            names.update(elt.value for elt in value.elts
                         if isinstance(elt, ast.Constant) and isinstance(elt.value, str))
    return names


def _is_public(name: str, all_names: Optional[Set[str]]) -> bool:
    """Whether ``name`` is part of the module's public surface: membership in
    ``__all__`` when declared, else the non-underscore convention."""
    return name in all_names if all_names is not None else not name.startswith('_')


@dataclass
class SessionTypeCache:
    """The JavaType instances every file of one ty session shares.

    ty type ids are stable for a session's lifetime and its descriptor table is
    cumulative across files (see ``TyTypesClient.session_types``), so an id denotes
    the same type in every file of the parse and needs only one JavaType.
    """

    by_type_id: Dict[int, JavaType] = field(default_factory=dict)
    declaring_by_type_id: Dict[int, JavaType.FullyQualified] = field(default_factory=dict)
    by_fqn: Dict[str, JavaType] = field(default_factory=dict)

    def clear(self) -> None:
        self.by_type_id.clear()
        self.declaring_by_type_id.clear()
        self.by_fqn.clear()


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

        # Lazily populated by _module_ast / _import_bindings
        self._module_ast_parsed = False
        self._module_ast_tree: Optional[ast.Module] = None
        self._import_binding_index: Optional[Dict[str, str]] = None
        self._from_import_member_index: Optional[Dict[str, Tuple[str, str]]] = None
        self._shadowed_names: Optional[Set[str]] = None

        # ty-types data: populated by _build_index
        self._node_index: Dict[Tuple[int, int], Tuple[int, str]] = {}  # (start, end) -> (type_id, node_kind)
        self._node_index_by_start: Dict[int, List[Tuple[int, int, str]]] = {}  # start -> [(end, type_id, node_kind)]
        self._type_registry: Dict[int, Dict[str, Any]] = {}  # type_id -> TypeDescriptor
        self._call_signature_index: Dict[Tuple[int, int], Dict[str, Any]] = {}  # (start, end) -> callSignature
        self._binding_index: Dict[Tuple[int, int], Dict[str, str]] = {}  # (start, end) -> BindingInfo
        session_java_types = getattr(ty_client, 'java_types', None) or SessionTypeCache()
        self._type_cache: Dict[str, JavaType] = session_java_types.by_fqn
        self._type_id_cache: Dict[int, JavaType] = session_java_types.by_type_id
        self._declaring_type_id_cache: Dict[int, JavaType.FullyQualified] = \
            session_java_types.declaring_by_type_id
        # Cycle detection tracks one file's in-progress resolutions, so unlike the
        # resolved types above it belongs to this instance rather than the session.
        self._resolving_type_ids: set = set()  # type_ids currently being resolved (cycle detection)
        self._resolving_declaring_type_ids: set = set()
        self._cycle_placeholders: Dict[int, JavaType.Class] = {}  # placeholders created on cycle detection
        self._declaring_cycle_placeholders: Dict[int, JavaType.Class] = {}
        self._class_literal_index: Dict[str, int] = {}  # className -> classLiteral type_id

        # Cumulative, session-wide type table shared by every file parsed through
        # the same ty ``--serve`` session. ty deduplicates descriptors across a
        # session, so a type first seen in an earlier file is omitted from later
        # files' responses; this table restores those descriptors as a fallback
        # (see TyTypesClient.session_types and _build_index back-fill).
        self._session_types: Dict[int, Dict[str, Any]] = (
            ty_client.session_types if ty_client is not None
            and hasattr(ty_client, 'session_types') else {}
        )

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
        result = client.get_types(actual_file, include_bindings=True)
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

            binding = node.get('binding')
            if binding is not None:
                self._binding_index[(node['start'], node['end'])] = binding

        # Merge types into registry (keys are strings in JSON)
        for type_id_str, descriptor in result.get('types', {}).items():
            tid = int(type_id_str)
            self._type_registry[tid] = descriptor
            # Index classLiterals by className for kind inference
            if descriptor.get('kind') == 'classLiteral':
                cn = descriptor.get('className', '')
                if cn:
                    self._class_literal_index[cn] = tid

        # Back-fill descriptors that this file's response omitted because ty's
        # shared --serve session already emitted them for an earlier file. The
        # session table is keyed by ty's session-stable ids, so a referenced id
        # absent from this file's registry resolves through the cumulative table.
        # File-local descriptors take precedence (setdefault) — they are the
        # authoritative, full descriptors for types this file is the first to see.
        for tid, descriptor in self._session_types.items():
            if self._type_registry.setdefault(tid, descriptor) is descriptor and \
                    descriptor.get('kind') == 'classLiteral':
                cn = descriptor.get('className', '')
                if cn:
                    self._class_literal_index.setdefault(cn, tid)

    def _lookup_type_id(self, node: ast.AST) -> Optional[int]:
        """Look up a node's type ID by converting AST position to byte offset.

        Results are cached by node position to avoid redundant byte-offset
        conversions when the same node is queried from multiple call sites.
        """
        if not hasattr(node, 'lineno') or node.lineno is None:
            return None

        end_lineno = getattr(node, 'end_lineno', None)
        end_col_offset = getattr(node, 'end_col_offset', None)
        cache_key = (node.lineno, node.col_offset, end_lineno, end_col_offset)  # ty: ignore[unresolved-attribute]  # AST nodes with lineno always have col_offset
        if cache_key in self._lookup_cache:
            return self._lookup_cache[cache_key]

        start = self._decorated_start(node)
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

    def _decorated_start(self, node: ast.AST) -> int:
        """Return the byte offset ty uses as this node's start.

        CPython's ``ast`` places a decorated class/function's ``lineno``/
        ``col_offset`` at the ``class``/``def`` keyword, but ruff (which ty is
        built on) starts the statement's range at the first decorator's ``@``.
        Realign to that ``@`` so the ``(start, end)`` lookup hits; otherwise
        every decorated class or function (e.g. ``@dataclass``) resolves to no
        type.
        """
        decorators = getattr(node, 'decorator_list', None)
        if decorators:
            first = decorators[0]
            line = self._source_lines[first.lineno - 1] if first.lineno <= len(self._source_lines) else ""
            at_col = line.rfind('@', 0, first.col_offset)
            if at_col != -1:
                return self._pos_to_byte_offset(first.lineno, at_col)
        return self._pos_to_byte_offset(node.lineno, node.col_offset)  # ty: ignore[unresolved-attribute]  # AST nodes with lineno always have col_offset

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

    def _class_fqn(self, descriptor: Dict[str, Any], name_key: str = 'className') -> str:
        """The fully-qualified name a class-bearing descriptor is keyed by.

        ty's ``qualifiedName`` is the dotted path of the enclosing modules *and*
        classes, so it is authoritative where present; ``moduleName`` alone leaves
        a nested class named as if it were top-level. ``builtins`` is stripped
        either way, keeping ``int`` and ``list`` unqualified.
        """
        qualified = descriptor.get('qualifiedName')
        member = descriptor.get('memberName') or (
            descriptor.get('name') if descriptor.get('kind') in _FUNCTION_KINDS else None)
        if qualified and member:
            # The descriptor names a member — an enum member, a method — but the type
            # wanted is its owning class, one segment up. Only trust the reduction when
            # that segment really is `className`, so an enum member sharing its class's
            # name (`class Color(Enum): Color = 1`) doesn't strip the class away.
            owner, _, last = qualified.rpartition('.')
            class_name = descriptor.get('className')
            if last == member:
                qualified = (owner if class_name and owner.rsplit('.', 1)[-1] == class_name
                             else None)
        if qualified:
            return (qualified[len('builtins.'):]
                    if qualified.startswith('builtins.') else qualified)
        # Not every descriptor kind carries `qualifiedName`: `function`, `boundMethod`
        # and `knownInstance` never do, and a few instances and TypedDicts lack it too.
        name = descriptor.get(name_key) or ''
        if not name:
            return ''
        module_name = descriptor.get('moduleName')
        if module_name and module_name != 'builtins':
            return f"{module_name}.{name}"
        return name

    def _resolved_class(self, descriptor: Dict[str, Any],
                        fqn: str) -> Optional[JavaType.Class]:
        """The enriched classLiteral naming this descriptor's class, if there is one.

        A ``classId`` on the descriptor names that class outright, so it is
        authoritative for the FQN even when the descriptor's own name is less
        qualified. The by-simple-name index is a guess — two modules may declare
        the same class name — so a match found that way is used only when its FQN
        agrees with ``fqn`` — or when ``fqn`` is the bare class name, which the
        index hit can only improve on.
        """
        class_name = descriptor.get('className', '')
        class_id = descriptor.get('classId')
        authoritative = class_id is not None
        if class_id is None:
            class_id = self._class_literal_index.get(class_name)
        if class_id is None:
            return None
        resolved = self._resolve_type(class_id)
        # An empty name is a placeholder mid-cycle, which cannot name the class yet.
        if not isinstance(resolved, JavaType.Class) or not resolved.fully_qualified_name:
            return None
        if authoritative or resolved.fully_qualified_name == fqn or fqn == class_name:
            return resolved
        return None

    def _descriptor_to_java_type(self, descriptor: Dict[str, Any]) -> Optional[JavaType]:
        """Convert a ty-types TypeDescriptor to a JavaType."""
        kind = descriptor.get('kind')

        if kind == 'instance':
            class_name = descriptor.get('className', '')
            if class_name in _PYTHON_PRIMITIVES:
                return _PYTHON_PRIMITIVES[class_name]

            module_name = descriptor.get('moduleName')

            # Prefer the classLiteral, which carries kind/supertypes/methods. Shared
            # with the declaring-type path so an expression and its declaring type
            # cannot drift apart.
            base_class = self._class_reference(descriptor)

            # `tuple` has a single generic parameter, so typeArgs conflates
            # `tuple[int, str]` with `tuple[int | str, ...]`. Subclasses inherit
            # elements from `tuple` without being generic, so only `tuple` takes them.
            tuple_elements = (descriptor.get('tupleElements')
                              if class_name == 'tuple' and module_name == 'builtins'
                              else None)
            if isinstance(tuple_elements, list):
                arg_ids = [element.get('typeId') for element in tuple_elements
                           if isinstance(element, dict)]
            else:
                arg_ids = descriptor.get('typeArgs') or []

            resolved_args = []
            for arg_id in arg_ids:
                arg_type = self._resolve_type(arg_id) if arg_id is not None else None
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
            return self._module_class(descriptor.get('moduleName', ''))

        elif kind in _FUNCTION_KINDS:
            # Use structured return type if available
            return_type_id = descriptor.get('returnType')
            if return_type_id is not None:
                result = self._resolve_type(return_type_id)
                if result is not None:
                    return result
            return _UNKNOWN

        elif kind == 'classLiteral':
            fqn = self._class_fqn(descriptor)

            # Create a fresh JavaType.Class per type_id rather than deduplicating
            # by FQN. ty-types can emit multiple classLiterals with the same FQN
            # (e.g., class Pair(namedtuple('Pair', ...))) and collapsing them
            # would cause self-referential supertypes.
            class_type = JavaType.Class()
            class_type._flags_bit_map = 0
            class_type._fully_qualified_name = fqn
            class_type._kind = JavaType.FullyQualified.Kind.Class

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
            if supertypes:
                super_type = self._resolve_type(supertypes[0])
                if isinstance(super_type, JavaType.FullyQualified):
                    class_type._supertype = super_type

                if len(supertypes) > 1:
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
                    if member_desc and member_desc.get('kind') in _FUNCTION_KINDS:
                        method = self._create_method_from_descriptor(member_desc, class_type)
                        if method:
                            methods.append(method)
                class_type._methods = methods if methods else None

            # Populate members (attributes / class & instance variables) from the
            # non-function members. ty emits a member's *name* on the entry itself
            # and its *type* via `typeId`; for a field with a default it emits both
            # the declared type and the default-value literal under the same name,
            # so de-duplicate by name keeping the first (declared) occurrence. A
            # member typed as the owning class resolves through the same cycle
            # guard `_resolve_type` uses for methods, so self-references don't
            # recurse infinitely.
            if members and getattr(class_type, '_members', None) is None:
                variables = []
                seen_names = set()
                for member in members:
                    if not isinstance(member, dict):
                        continue
                    member_name = member.get('name')
                    member_type_id = member.get('typeId')
                    if not member_name or member_type_id is None or member_name in seen_names:
                        continue
                    member_desc = self._type_registry.get(member_type_id)
                    # Skip function-kinds (handled as methods above) and nested
                    # classes/modules — only true variables become members.
                    if member_desc is None or not self._is_variable_descriptor(member_desc):
                        continue
                    member_type = self._resolve_type(member_type_id)
                    if member_type is None:
                        continue
                    seen_names.add(member_name)
                    variables.append(JavaType.Variable(
                        _name=member_name, _type=member_type, _owner=class_type))
                class_type._members = variables if variables else None

            return class_type

        elif kind == 'typedDict':
            # Map a TypedDict to a nominal class type by name and populate its
            # members from the descriptor's `fields`. Each field carries its own
            # `name` and `typeId` (the same shape as a classLiteral member), so
            # we reuse the variable-building path. Two TypedDicts sharing a name
            # within a file collapse (see _typed_dict_key).
            #
            # We still drop the PEP 728 `closed` / `extraItems` openness fields
            # and the per-field `required` / `readOnly` flags; and linking a
            # subscript use `m["name"]` back to its field declaration (a
            # J.ArrayAccess LST change) remains deferred — that value type is
            # already attributed on the access node.
            name = descriptor.get('name', '')
            if not name:
                return _UNKNOWN
            class_type = self._create_class_type(
                self._class_fqn(descriptor, 'name'), shallow=False,
                cache_key=self._typed_dict_key(descriptor, name))
            fields = descriptor.get('fields', [])
            if fields and getattr(class_type, '_members', None) is None:
                variables = []
                seen_names = set()
                for field in fields:
                    if not isinstance(field, dict):
                        continue
                    field_name = field.get('name')
                    field_type_id = field.get('typeId')
                    if not field_name or field_type_id is None or field_name in seen_names:
                        continue
                    field_type = self._resolve_type(field_type_id)
                    if field_type is None:
                        continue
                    seen_names.add(field_name)
                    variables.append(JavaType.Variable(
                        _name=field_name, _type=field_type, _owner=class_type))
                if variables:
                    class_type._members = variables
            return class_type

        elif kind == 'subclassOf':
            # `subclassOf X` is ty's representation of `type[X]`: a *class
            # object*, not an instance of X. Model it as `type[X]` so it stays
            # distinct from an instance of X (see _make_class_object_type).
            base_id = descriptor.get('base')
            if base_id is not None:
                result = self._resolve_type(base_id)
                if result is not None:
                    return self._make_class_object_type(result)
            return _UNKNOWN

        elif kind == 'typeForm':
            # PEP 747 TypeForm[T] (ty-types >= 0.0.44): a type expression used as
            # a runtime value. Like `subclassOf` (`type[X]`), the value is the
            # type T, not an instance of T, so wrap it as a class object.
            type_arg_id = descriptor.get('typeArgument')
            if type_arg_id is not None:
                result = self._resolve_type(type_arg_id)
                if result is not None:
                    return self._make_class_object_type(result)
            return _UNKNOWN

        elif kind == 'newType':
            if descriptor.get('name'):
                return self._create_class_type(self._class_fqn(descriptor, 'name'))
            return _UNKNOWN

        elif kind == 'intersection':
            for member_id in descriptor.get('positive', []):
                result = self._resolve_type(member_id)
                if result is not None:
                    return result
            return _UNKNOWN

        elif kind in ('dynamic', 'never'):
            return _UNKNOWN

        elif kind in ('enumLiteral', 'enumComplement'):
            # An enum member is keyed by its enum *class*, so that a member reference
            # and the class itself resolve to the same FQN.
            class_type = self._class_reference(descriptor)
            class_type._kind = JavaType.FullyQualified.Kind.Enum
            return class_type

        elif kind == 'property':
            return _UNKNOWN

        elif kind == 'specialForm':
            name = descriptor.get('name', '')
            if name:
                return self._create_class_type(name)
            return _UNKNOWN

        elif kind == 'knownInstance':
            fqn = _KNOWN_INSTANCE_FQNS.get(descriptor.get('knownInstanceKind', ''))
            if fqn is None:
                class_name = descriptor.get('className', '')
                if not class_name:
                    return _UNKNOWN
                fqn = f"typing.{class_name}"
            return self._create_class_type(fqn)

        elif kind == 'typeAlias':
            # Resolve through to the underlying value type when available
            value_type_id = descriptor.get('valueType')
            if value_type_id is not None:
                result = self._resolve_type(value_type_id)
                if result is not None:
                    return result
            # Fall back to creating a class from the alias name
            if descriptor.get('name'):
                return self._create_class_type(self._class_fqn(descriptor, 'name'))
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
            # Use constraints as bounds if no upper bound
            if bounds is None:
                constraint_ids = descriptor.get('constraints', [])
                if constraint_ids:
                    resolved_constraints = []
                    for c_id in constraint_ids:
                        c_type = self._resolve_type(c_id)
                        if c_type is not None:
                            resolved_constraints.append(c_type)
                    if resolved_constraints:
                        bounds = resolved_constraints
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
        elif isinstance(node.value, complex):
            return self._create_class_type('complex')
        elif node.value is None:
            return JavaType.Primitive.None_
        return None

    def _is_variable_descriptor(self, descriptor: Dict[str, Any]) -> bool:
        """Check if a type descriptor represents a variable (not a function, class, or module)."""
        kind = descriptor.get('kind')
        return kind not in _FUNCTION_KINDS and kind not in ('module', 'classLiteral')

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

    def decorator_type(self, node: ast.expr) -> Optional[JavaType]:
        """The type naming the decorator ``node`` applies, the way Java names an
        annotation: the decorating function or class itself, under the module defining
        it — not the type applying it returns.

        A decorator no descriptor names is named by where it is bound.
        """
        type_id = self._lookup_type_id(node)
        descriptor = self._type_registry.get(type_id) if type_id is not None else None
        if descriptor is not None:
            if descriptor.get('kind') == 'classLiteral':
                # A classLiteral carries the class body; an FQN alone mints a shell
                return self._resolve_type(type_id)
            name = descriptor.get('name')
            module = descriptor.get('moduleName')
            if descriptor.get('kind') in _FUNCTION_KINDS and module and name \
                    and not descriptor.get('className'):
                return self._create_class_type(
                    f"{_ALIASED_MODULES.get(module, module)}.{name}")
        bound = self._binding_owner(node)
        fqn = (f"{_ALIASED_MODULES.get(bound[0], bound[0])}.{bound[1]}" if bound
               else self._import_binding_fqn(node))
        if fqn:
            return self._create_class_type(fqn)
        return self.type(node)

    def _binding_owner(self, node: ast.expr) -> Optional[Tuple[str, str]]:
        """The ``(module, symbol)`` ty binds a reference to, following re-export chains to
        where the symbol is declared. ty reports a scope's first declaration, so a name
        the file binds a second time is not read from one.
        """
        if self._writes_a_rebound_name(node):
            return None
        binding = self._lookup_binding(node)
        if binding is None:
            return None
        module, qualified = binding.get('definedIn'), binding.get('qualifiedName')
        if not module or not qualified or not qualified.startswith(f"{module}."):
            return None
        symbol = qualified[len(module) + 1:]
        # Only what the module itself declares. A deeper path is a class member, owned by
        # that class rather than the module — `"".join` binds `builtins.str.join` — or is
        # under a scope `qualifiedName` spells as ty does (`<locals of function 'f'>`).
        return (module, symbol) if '.' not in symbol else None

    def _writes_a_rebound_name(self, node: ast.expr) -> bool:
        """Whether the name ``node`` is written under is one the file binds a second time."""
        while isinstance(node, ast.Attribute):
            node = node.value
        self._import_bindings()
        return isinstance(node, ast.Name) and node.id in (self._shadowed_names or ())

    def _lookup_binding(self, node: ast.expr) -> Optional[Dict[str, str]]:
        """The BindingInfo ty attached to ``node``, by byte range."""
        end_lineno = getattr(node, 'end_lineno', None)
        end_col_offset = getattr(node, 'end_col_offset', None)
        if getattr(node, 'lineno', None) is None or end_lineno is None or end_col_offset is None:
            return None
        return self._binding_index.get(
            (self._decorated_start(node), self._pos_to_byte_offset(end_lineno, end_col_offset)))

    def _import_binding_fqn(self, node: ast.expr) -> Optional[str]:
        """The FQN an unshadowed module-level import gives ``node``'s written name."""
        suffix: List[str] = []
        while isinstance(node, ast.Attribute):
            suffix.append(node.attr)
            node = node.value
        if not isinstance(node, ast.Name):
            return None
        bound = self._import_bindings().get(node.id)
        return '.'.join([bound, *reversed(suffix)]) if bound else None

    def _import_bindings(self) -> Dict[str, str]:
        """The FQN each of this file's absolute module-level imports names, keyed by the
        name it binds, minus every name :meth:`_rebound_names` reports. The same scan
        yields the ``from M import f`` bindings that :meth:`_bound_from_import_member` reads.

        Python binds a name once per scope, so an import nothing else rebinds says what a
        reference to that name means whether or not ty could type it.
        """
        if self._import_binding_index is None:
            tree = self._module_ast()
            bindings: Dict[str, str] = {}
            members: Dict[str, Tuple[str, str]] = {}
            module_scope = list(_module_scope_statements(tree.body if tree else ()))
            module_level_aliases = {id(alias) for stmt in module_scope
                                    if isinstance(stmt, (ast.Import, ast.ImportFrom))
                                    for alias in stmt.names}
            shadowed = self._rebound_names(tree, module_level_aliases)

            def bind(name: str, fqn: Optional[str]) -> None:
                # `import a.b` after `import a` re-binds the root to the same FQN
                if fqn is None or bindings.get(name, fqn) != fqn:
                    shadowed.add(name)
                else:
                    bindings[name] = fqn

            for stmt in module_scope:
                if isinstance(stmt, (ast.Import, ast.ImportFrom)):
                    # A relative import's written form is not a module path
                    relative = isinstance(stmt, ast.ImportFrom) and (stmt.level or not stmt.module)
                    for alias in stmt.names:
                        if isinstance(stmt, ast.Import):
                            # A non-aliased `import a.b` binds the root package `a`
                            root = alias.name.split('.')[0]
                            bind(alias.asname or root, alias.name if alias.asname else root)
                        elif alias.name == '*':
                            # A star import can bind any name, so nothing here is decidable
                            self._import_binding_index = {}
                            self._from_import_member_index = {}
                            return self._import_binding_index
                        elif relative:
                            bind(alias.asname or alias.name, None)
                        else:
                            bind(alias.asname or alias.name, f"{stmt.module}.{alias.name}")
                            members[alias.asname or alias.name] = (stmt.module, alias.name)

            self._import_binding_index = {name: fqn for name, fqn in bindings.items()
                                          if name not in shadowed}
            self._from_import_member_index = {name: member for name, member in members.items()
                                              if name not in shadowed}
        return self._import_binding_index

    def _rebound_names(self, tree: Optional[ast.Module],
                       module_level_aliases: Set[int]) -> Set[str]:
        """Every name this file binds somewhere other than one of its own module-level
        imports. Coarse on purpose: a name bound twice is one whose references cannot be
        read off an import, and declining to attribute costs less than attributing wrong.
        """
        if self._shadowed_names is None:
            names: Set[str] = set()
            for node in ast.walk(tree) if tree else ():
                if isinstance(node, ast.Name):
                    if isinstance(node.ctx, (ast.Store, ast.Del)):
                        names.add(node.id)
                elif isinstance(node, (ast.FunctionDef, ast.AsyncFunctionDef, ast.ClassDef)):
                    names.add(node.name)
                elif isinstance(node, ast.arg):
                    names.add(node.arg)
                elif isinstance(node, ast.ExceptHandler) and node.name:
                    names.add(node.name)
                elif isinstance(node, (ast.Global, ast.Nonlocal)):
                    names.update(node.names)
                elif isinstance(node, ast.alias) and id(node) not in module_level_aliases:
                    names.add(node.asname or node.name.split('.')[0])
            self._shadowed_names = names
        return set(self._shadowed_names)

    def _bound_from_import_member(self, node: ast.expr) -> Optional[Tuple[str, str]]:
        """The ``(module, member)`` an unshadowed module-level ``from M import f`` binds
        ``node``'s name to. A receiver-less call spells the binding's name, which an alias
        makes different from the member's, so both halves come from the import.
        """
        if not isinstance(node, ast.Name):
            return None
        self._import_bindings()
        return (self._from_import_member_index or {}).get(node.id)

    def import_alias_type(self, node: ast.alias) -> Optional[JavaType]:
        """The type of the symbol an import name binds, named under the module defining
        it — the module a call to it names too, whichever module the source imported
        through. Functions become a whole :class:`JavaType.Method`, not the return type
        expression positions use, so that callers can derive an FQN from declaring type
        plus name.
        """
        type_id = self._lookup_type_id(node)
        if type_id is None:
            return None
        descriptor = self._type_registry.get(type_id)
        if descriptor is None:
            return None
        kind = descriptor.get('kind')
        if kind == 'module':
            module_name = descriptor.get('moduleName', '')
            # ty types a non-aliased dotted `import a.b.c` as the bound root
            # package `a`, while the qualid names the full dotted path.
            if not node.asname and '.' in node.name and node.name != module_name:
                module_name = node.name
            return self._module_class(module_name) if module_name else None
        if kind in _FUNCTION_KINDS:
            return self._create_method_from_descriptor(
                descriptor, self._get_declaration_declaring_type(descriptor))
        return self._resolve_type(type_id)

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
            if descriptor and descriptor.get('kind') in _FUNCTION_KINDS:
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
        param_names, param_types = self._process_method_params(
            descriptor.get('parameters', []))

        return_type = None
        ret_id = descriptor.get('returnType')
        if ret_id is not None:
            return_type = self._resolve_type(ret_id)

        type_param_names = self._extract_type_param_names(descriptor)

        return JavaType.Method(
            _flags_bit_map=0,
            _declaring_type=self._get_declaration_declaring_type(descriptor),
            _name=name,
            _return_type=return_type,
            _parameter_names=param_names if param_names else None,
            _parameter_types=param_types if param_types else None,
            _declared_formal_type_names=type_param_names if type_param_names else None,
        )

    def _module_ast(self) -> Optional[ast.Module]:
        """This file's AST, parsed once, or None when the source doesn't parse."""
        if not self._module_ast_parsed:
            self._module_ast_parsed = True
            try:
                self._module_ast_tree = ast.parse(self._source)
            except SyntaxError:
                self._module_ast_tree = None
        return self._module_ast_tree

    def module_type(self, module_fqn: str) -> Optional[JavaType.Class]:
        """A JavaType.Class for the module itself: FQN = the module name, public
        top-level functions as methods, public top-level constants as members.

        This is the definition half of how the type model represents module-level
        API — attribution declares a reference like ``click.echo`` or ``os.sep``
        under a class named after the module (see
        ``_declaring_type_from_descriptor``), so an enumeration of the module's
        public types must define that class for such references to resolve.
        A re-exported callable (``from .utils import echo`` in ``__init__.py``) is a
        method of the module defining it, which is where attribution names it; a
        re-exported value is a member here, since a reference to it resolves through
        the module that binds it. The public surface is ``__all__`` when declared,
        else the non-underscore names. Returns None when the module has no public
        module-level symbols.
        """
        tree = self._module_ast()
        if tree is None:
            return None

        all_names = _module_all_names(tree)
        # Shallow until a body is found: promotion would advertise a defined
        # (rather than merely referenced) class even for an empty module.
        module_class = self._create_class_type(module_fqn)
        methods: Dict[str, JavaType.Method] = {}
        members: Dict[str, JavaType.Variable] = {}

        def add_binding(name: str, node: ast.AST, defines: bool = True) -> None:
            if not _is_public(name, all_names) or name in methods or name in members:
                return
            type_id = self._lookup_type_id(node)
            descriptor = self._type_registry.get(type_id)
            if descriptor is None:
                return
            if descriptor.get('kind') in _FUNCTION_KINDS:
                if not defines:
                    return
                method = self._create_method_from_descriptor(descriptor, module_class, name=name)
                if method is not None:
                    methods[name] = method
            elif self._is_variable_descriptor(descriptor):
                member_type = self._resolve_type(type_id)
                if member_type is not None:
                    members[name] = JavaType.Variable(
                        _name=name, _type=member_type, _owner=module_class)
            # classLiteral / module bindings keep their own FQN — not module members.

        for stmt in tree.body:
            if isinstance(stmt, (ast.FunctionDef, ast.AsyncFunctionDef)):
                add_binding(stmt.name, stmt)
            elif isinstance(stmt, ast.Assign):
                for target in stmt.targets:
                    if isinstance(target, ast.Name):
                        add_binding(target.id, target)
            elif isinstance(stmt, ast.AnnAssign) and isinstance(stmt.target, ast.Name):
                add_binding(stmt.target.id, stmt.target)
            elif isinstance(stmt, ast.ImportFrom):
                for alias in stmt.names:
                    if alias.name != '*':
                        add_binding(alias.asname or alias.name, alias, defines=False)
            # plain `import x` binds a module object, never module-level API

        if not methods and not members:
            return None
        # Promote in place, so references minted earlier from this file share the instance.
        module_class = self._create_class_type(module_fqn, shallow=False)
        module_class._methods = list(methods.values()) or None
        module_class._members = list(members.values()) or None
        return module_class

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
        method_name = self._extract_method_name(node)
        if not method_name:
            return None
        if self._constructed_class(node) is not None:
            method_name = '<constructor>'
        else:
            method_name = self._callee_declared_name(node) or method_name

        # Get declaring type
        declaring_type = self._get_declaring_type(node)
        if isinstance(declaring_type, JavaType.Unknown):
            # Nothing resolved the callee, so the import binding it is what names it.
            bound = self._bound_from_import_member(node.func)
            if bound:
                declaring_type, method_name = self._module_class(bound[0]), bound[1]

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

    def _callee_declared_name(self, node: ast.Call) -> Optional[str]:
        """The name the callee carries where it is defined. Its owner is read off the
        callee, so its name is too: a binding that renames a symbol renames neither
        half — ``from platform import system as s`` calls ``platform system(..)``.
        """
        callee_id = self._lookup_func_type_id(node)
        callee = self._type_registry.get(callee_id) if callee_id is not None else None
        if callee is not None and callee.get('kind') in _FUNCTION_KINDS:
            return callee.get('name') or None
        bound = self._binding_owner(node.func)
        return bound[1] if bound else None

    def _extract_method_name(self, node: ast.Call) -> Optional[str]:
        """The name a Call node spells."""
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

    def _process_method_params(
            self, params: List[Dict[str, Any]]
    ) -> Tuple[List[str], List[JavaType]]:
        """Normalize a ParameterInfo list into (names, types) for JavaType.Method.

        Applies:
        - Skip `self` / `cls`.
        - Collapse the synthetic `*args` / `**kwargs` pair emitted for a
          `ParamSpec` tail (both carry the same ``paramSpecName``) into a
          single entry whose name is the ParamSpec's name and whose type
          is `_UNKNOWN`. This avoids exposing `P.args` / `P.kwargs` as two
          distinct variadic parameters on the produced method.
        - Treat `concatenatePrefix` params as ordinary positional params.
        """
        names: List[str] = []
        types: List[JavaType] = []
        last_spec_emitted: Optional[str] = None
        for p in params:
            p_name = p.get('name', '')
            if p_name in ('self', 'cls'):
                continue
            spec_name = p.get('paramSpecName')
            if spec_name is not None:
                if spec_name == last_spec_emitted:
                    continue
                names.append(spec_name)
                types.append(_UNKNOWN)
                last_spec_emitted = spec_name
                continue
            last_spec_emitted = None
            names.append(p_name)
            types.append(self._resolve_param_type(p))
        return names, types

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
                return self._process_method_params(params)

        # Try function/method descriptor parameters
        func_type_id = self._lookup_func_type_id(node)
        if func_type_id is not None:
            descriptor = self._type_registry.get(func_type_id)
            if descriptor:
                params = descriptor.get('parameters', [])
                if params:
                    return self._process_method_params(params)

        # Fall back to placeholder names
        return self._generate_placeholder_names(node)

    def _constructed_class(self, node: ast.Call) -> Optional[Dict[str, Any]]:
        """The descriptor of the class ``node`` constructs, or None when it calls
        something else. Python has no constructor node — a construction is a call to
        the class name — so this is what tells the two apart."""
        callee_id = self._lookup_func_type_id(node)
        callee = self._type_registry.get(callee_id) if callee_id is not None else None
        return callee if callee is not None and callee.get('kind') == 'classLiteral' else None

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

    def _get_declaring_type(self, node: ast.Call) -> JavaType.FullyQualified:
        """Get the declaring type (class/module) for a method call.

        Always returns a non-null FullyQualified — falls back to
        :data:`_UNKNOWN` (a shared ``JavaType.Unknown`` singleton) when
        Ty can't resolve the receiver and AST inference doesn't yield a
        recognizable type. This keeps ``JavaType.Method.declaring_type``
        non-null for every method invocation, which is what
        ``org.openrewrite.java.search.HasMethod`` / ``UsesMethod`` /
        ``MethodMatcher`` expect: those gates accept ``JavaType.Unknown``
        receivers under wildcard patterns (``*..*``) but reject method
        types whose declaring type is null.

        Without this, a precondition like
        ``Preconditions.check(uses_method("*..* tostring(..)"), V())``
        was failing on unattributed Python sources (e.g. test fixtures
        that don't import the receiver type), because the host's
        wire-side HasMethod gate could not find a matching method use
        in ``TypesInUse``.
        """
        constructed = self._constructed_class(node)
        if constructed is not None:
            # A construction is owned by the class, so one pattern covers constructing
            # a type and calling its members. The module declares only its functions —
            # `module_type` leaves a class to its own FQN.
            return self._class_reference(constructed)

        # The callee names what is called; a receiver names only the module a call was
        # reached through, which differs for a re-exported function. A value receiver
        # owns its calls, so it is read below instead: `"x".upper()` is `str`'s.
        callee_id = (self._lookup_func_type_id(node)
                     if not isinstance(node.func, ast.Attribute)
                     or self._names_a_module(node.func.value) else None)
        callee = self._type_registry.get(callee_id) if callee_id is not None else None
        if callee is not None:
            kind = callee.get('kind')
            if kind == 'module':
                return self._module_class(callee.get('moduleName', ''))
            if kind in _FUNCTION_KINDS:
                # boundMethod has className — use it for declaring type
                if callee.get('className'):
                    return self._class_reference(callee)
                # A function is owned by its module, `builtins` included —
                # only a *type* drops that qualification (`_class_reference`).
                module_name = callee.get('moduleName')
                if module_name:
                    return self._module_class(module_name)

        bound = self._binding_owner(node.func)
        if bound:
            return self._module_class(bound[0])

        if isinstance(node.func, ast.Attribute):
            receiver = node.func.value

            # For chained calls like "hello".upper().split(), the receiver is a Call
            if isinstance(receiver, ast.Call):
                resolved = self._get_call_return_type(receiver)
                if resolved is not None:
                    return resolved

            # Try to look up receiver type in ty-types index
            type_id = self._lookup_type_id(receiver)
            if type_id is not None:
                resolved = self._resolve_declaring_type(type_id)
                if resolved is not None:
                    return resolved

        if isinstance(node.func, ast.Attribute):
            # A receiver whose root an unshadowed import binds is that module, spelled as
            # the import names it. A written chain alone cannot tell a module from a value.
            module = self._import_binding_fqn(node.func.value)
            if module:
                return self._module_class(module)

        inferred = self._infer_declaring_type_from_ast(node)
        return inferred if inferred is not None else _UNKNOWN

    def _names_a_module(self, node: ast.expr) -> bool:
        type_id = self._lookup_type_id(node)
        descriptor = self._type_registry.get(type_id) if type_id is not None else None
        return descriptor is not None and descriptor.get('kind') == 'module'

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

    def _class_reference(self, descriptor: Dict[str, Any]) -> JavaType.Class:
        """Resolve a descriptor's class through its classLiteral so annotation,
        expression, and declaring-type positions all share one enriched object."""
        fqn = self._class_fqn(descriptor)
        return self._resolved_class(descriptor, fqn) or self._create_class_type(fqn)

    def _declaring_type_from_descriptor(self, descriptor: Dict[str, Any]) -> Optional[JavaType.FullyQualified]:
        """Extract a declaring type (class/module) from a TypeDescriptor."""
        kind = descriptor.get('kind')

        if kind == 'module':
            return self._module_class(descriptor.get('moduleName', ''))

        elif kind == 'instance':
            return self._class_reference(descriptor)

        elif kind == 'typedDict':
            name = descriptor.get('name', '')
            if name:
                return self._create_class_type(
                    self._class_fqn(descriptor, 'name'),
                    cache_key=self._typed_dict_key(descriptor, name))
            return None

        elif kind == 'subclassOf':
            base_id = descriptor.get('base')
            if base_id is not None:
                return self._resolve_declaring_type(base_id)
            return None

        elif kind == 'typeForm':
            type_arg_id = descriptor.get('typeArgument')
            if type_arg_id is not None:
                return self._resolve_declaring_type(type_arg_id)
            return None

        elif kind == 'newType':
            if descriptor.get('name'):
                return self._create_class_type(self._class_fqn(descriptor, 'name'))
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
            return self._class_reference(descriptor)

        elif kind == 'boundMethod':
            if descriptor.get('className'):
                return self._class_reference(descriptor)

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
                                        declaring_type: JavaType.FullyQualified,
                                        name: Optional[str] = None) -> Optional[JavaType.Method]:
        """Create a JavaType.Method from a ty-types function/boundMethod descriptor.
        ``name`` overrides the descriptor's own name (e.g. for aliased re-exports)."""
        name = name or descriptor.get('name', '')
        if not name:
            return None

        # Resolve return type
        return_type = None
        return_type_id = descriptor.get('returnType')
        if return_type_id is not None:
            return_type = self._resolve_type(return_type_id)

        # Resolve parameters (skip self/cls, collapse ParamSpec *args/**kwargs pairs)
        param_names, param_types = self._process_method_params(
            descriptor.get('parameters', []))

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

    def _make_class_object_type(self, base: JavaType) -> JavaType:
        """Wrap a resolved type ``X`` as the class-object type ``type[X]``.

        A value of type ``type[X]`` (ty's ``subclassOf X``, and PEP 747
        ``TypeForm[X]``) is the *class* ``X`` itself, not an instance of ``X``.
        It is modelled as a :class:`JavaType.Parameterized` over ``type`` with
        ``X`` as its sole type parameter — mirroring how ``list[X]`` is modelled
        — so that ``is_assignable_to(X, type[X])`` is ``False`` (the raw name is
        ``type``, not ``X``) while the wrapped ``X`` remains recoverable via
        ``type_parameters[0]`` for attribute/classmethod resolution.
        """
        param = JavaType.Parameterized()
        param._type = self._create_class_type('type')
        param._type_parameters = [base]
        return param

    def _typed_dict_key(self, descriptor: Dict[str, Any], name: str) -> Optional[str]:
        """The session cache key for a TypedDict named ``name``, or None to key it
        by its FQN. An unqualified name is scoped to this file so the same name in
        two modules stays apart; a ``qualifiedName`` is already unique, and scoping
        it would keep two files from sharing one interned type."""
        return None if descriptor.get('qualifiedName') else f"{self._file_path}#{name}"

    def _create_class_type(self, fqn: str, shallow: bool = True,
                           cache_key: Optional[str] = None) -> JavaType.Class:
        """Create a class type from a fully qualified name. Stubs minted here are
        body-less references (ShallowClass); pass shallow=False when filling a body."""
        key = fqn if cache_key is None else cache_key
        if key in self._type_cache:
            cached = self._type_cache[key]
            if isinstance(cached, JavaType.Class):
                if not shallow and type(cached) is JavaType.ShallowClass:
                    # Promote in place so earlier references see the full class.
                    cached.__class__ = JavaType.Class
                return cached

        class_type = JavaType.ShallowClass() if shallow else JavaType.Class()
        class_type._flags_bit_map = 0
        class_type._fully_qualified_name = fqn
        class_type._kind = JavaType.FullyQualified.Kind.Class

        self._type_cache[key] = class_type
        return class_type

    def _module_class(self, module_name: str) -> JavaType.Class:
        """The class standing for a module, under its portable name."""
        return self._create_class_type(_ALIASED_MODULES.get(module_name, module_name))

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

    def _get_declaration_declaring_type(self, descriptor: Dict[str, Any]) -> Optional[JavaType.FullyQualified]:
        """Get the declaring type for a function declaration.

        Mirrors the invocation-side logic from _get_declaring_type() to ensure
        declarations and invocations produce matching FQNs.
        """
        if descriptor.get('className'):
            return self._class_reference(descriptor)
        module_name = descriptor.get('moduleName')
        if module_name:
            return self._module_class(module_name)
        return None
