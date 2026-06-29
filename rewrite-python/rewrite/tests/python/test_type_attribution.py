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

"""Tests for Python type attribution using ty-types.

These tests verify that the PythonTypeMapping class correctly:
1. Starts and communicates with the ty-types CLI
2. Maps Python types to JavaType objects
3. Returns proper JavaType.Method with declaring_type and parameter_types

These tests require ty-types to be installed and on PATH.
"""

import ast
import pytest
import tempfile
import os
from pathlib import Path

from rewrite.java import JavaType

# Import type mapping modules
from rewrite.python.type_mapping import PythonTypeMapping, compute_source_line_data
from rewrite.python.ty_client import TyTypesClient
from rewrite.python._parser_visitor import ParserVisitor
from rewrite.python.visitor import PythonVisitor
from rewrite.java.tree import MethodInvocation, ClassDeclaration


def _ty_types_cli_available() -> bool:
    """Check if the ty-types CLI is available on PATH."""
    import shutil
    return shutil.which('ty-types') is not None


_TY_TYPES_CLI_INSTALLED = _ty_types_cli_available()


# Mark for tests that require ty-types CLI to be installed
requires_ty_types_cli = pytest.mark.skipif(
    not _TY_TYPES_CLI_INSTALLED,
    reason="ty-types CLI is not installed (ensure ty-types binary is on PATH)"
)


@requires_ty_types_cli
class TestTyTypesClient:
    """Tests for the TyTypesClient.

    These tests require the ty-types CLI to be installed.
    """

    def test_client_initializes(self):
        """Test that TyTypesClient starts the ty-types process."""
        with TyTypesClient() as client:
            assert client is not None

            with tempfile.TemporaryDirectory() as tmpdir:
                assert client.initialize(tmpdir)
                assert client.is_available

    def test_client_context_manager(self):
        """Test that TyTypesClient shuts down on context exit."""
        with TyTypesClient() as client:
            assert client._process is not None
        assert client._process is None

    def test_get_types_on_simple_file(self):
        """Test that get_types returns node types for a file."""
        with TyTypesClient() as client:
            with tempfile.TemporaryDirectory() as tmpdir:
                file_path = os.path.join(tmpdir, 'test.py')
                with open(file_path, 'w') as f:
                    f.write('x: int = 42\n')

                client.initialize(tmpdir)
                result = client.get_types(file_path)

                assert result is not None
                assert 'nodes' in result
                assert 'types' in result
                assert len(result['nodes']) > 0
                assert len(result['types']) > 0


class TestPythonTypeMappingPrimitives:
    """Tests for mapping Python primitive types to JavaType."""


    def test_string_constant(self):
        """Test that string constants map to JavaType.Primitive.String."""
        source = '"hello"'
        tree = ast.parse(source)
        mapping = PythonTypeMapping(source)

        # The tree has an Expr with a Constant value
        const = tree.body[0].value
        result = mapping.type(const)

        mapping.close()
        assert result == JavaType.Primitive.String

    def test_int_constant(self):
        """Test that int constants map to JavaType.Primitive.Int."""
        source = '42'
        tree = ast.parse(source)
        mapping = PythonTypeMapping(source)

        const = tree.body[0].value
        result = mapping.type(const)

        mapping.close()
        assert result == JavaType.Primitive.Int

    def test_float_constant(self):
        """Test that float constants map to JavaType.Primitive.Double."""
        source = '3.14'
        tree = ast.parse(source)
        mapping = PythonTypeMapping(source)

        const = tree.body[0].value
        result = mapping.type(const)

        mapping.close()
        assert result == JavaType.Primitive.Double

    def test_bool_constant(self):
        """Test that bool constants map to JavaType.Primitive.Boolean."""
        source = 'True'
        tree = ast.parse(source)
        mapping = PythonTypeMapping(source)

        const = tree.body[0].value
        result = mapping.type(const)

        mapping.close()
        assert result == JavaType.Primitive.Boolean

    def test_none_constant(self):
        """Test that None maps to JavaType.Primitive.None_."""
        source = 'None'
        tree = ast.parse(source)
        mapping = PythonTypeMapping(source)

        const = tree.body[0].value
        result = mapping.type(const)

        mapping.close()
        assert result == JavaType.Primitive.None_


class TestMethodInvocationType:
    """Tests for method_invocation_type returning proper JavaType.Method."""


    def test_method_invocation_returns_method_type(self):
        """Test that a method call returns a JavaType.Method."""
        source = '"hello".upper()'
        tree = ast.parse(source)
        mapping = PythonTypeMapping(source)

        call = tree.body[0].value  # The Call node
        result = mapping.method_invocation_type(call)

        mapping.close()

        assert result is not None
        assert isinstance(result, JavaType.Method)
        assert result._name == 'upper'

    def test_method_invocation_has_declaring_type(self):
        """Test that method_invocation_type includes declaring type."""
        source = '"hello".split(",")'

        with tempfile.NamedTemporaryFile(suffix='.py', delete=False, mode='w') as f:
            f.write(source)
            file_path = f.name

        try:
            tree = ast.parse(source)
            with TyTypesClient() as client:
                mapping = PythonTypeMapping(source, file_path, client)

                call = tree.body[0].value
                result = mapping.method_invocation_type(call)

                mapping.close()

            assert result is not None
            assert isinstance(result, JavaType.Method)
            assert result._name == 'split'

            # The declaring type should be str or similar
            declaring = result._declaring_type
            if declaring is not None:
                assert isinstance(declaring, JavaType.FullyQualified)
        finally:
            os.unlink(file_path)

    def test_method_invocation_has_parameter_types(self):
        """Test that method_invocation_type includes parameter types."""
        source = '"hello".split(",")'
        tree = ast.parse(source)
        mapping = PythonTypeMapping(source)

        call = tree.body[0].value
        result = mapping.method_invocation_type(call)

        mapping.close()

        assert result is not None
        assert result._parameter_types is not None
        # ty-types returns all signature params (sep, maxsplit), not just the called args
        assert len(result._parameter_types) >= 1

    def test_builtin_function_call(self):
        """Test that builtin function calls work."""
        source = 'len("hello")'
        tree = ast.parse(source)
        mapping = PythonTypeMapping(source)

        call = tree.body[0].value
        result = mapping.method_invocation_type(call)

        mapping.close()

        assert result is not None
        assert isinstance(result, JavaType.Method)
        assert result._name == 'len'
        assert result._parameter_types is not None
        assert len(result._parameter_types) >= 1

    def test_chained_method_call(self):
        """Test chained method calls like 'hello'.upper().split()."""
        source = '"hello".upper().split()'
        tree = ast.parse(source)
        mapping = PythonTypeMapping(source)

        call = tree.body[0].value  # The outer Call (split)
        result = mapping.method_invocation_type(call)

        mapping.close()

        assert result is not None
        assert isinstance(result, JavaType.Method)
        assert result._name == 'split'

    def test_method_with_multiple_args(self):
        """Test method with multiple arguments."""
        source = '"hello world".replace("hello", "goodbye")'
        tree = ast.parse(source)
        mapping = PythonTypeMapping(source)

        call = tree.body[0].value
        result = mapping.method_invocation_type(call)

        mapping.close()

        assert result is not None
        assert result._name == 'replace'
        assert result._parameter_types is not None
        # ty-types returns all signature params (old, new, count), not just the called args
        assert len(result._parameter_types) >= 2

    def test_method_with_mixed_arg_types(self):
        """Test method with different argument types."""
        source = '"hello".center(10, "-")'
        tree = ast.parse(source)
        mapping = PythonTypeMapping(source)

        call = tree.body[0].value
        result = mapping.method_invocation_type(call)

        mapping.close()

        assert result is not None
        assert result._name == 'center'
        assert result._parameter_types is not None
        assert len(result._parameter_types) >= 2


class TestTypeAttributionWithImports:
    """Tests for type attribution on imported modules."""


    def test_stdlib_function_call(self):
        """Test type attribution for stdlib function calls."""
        source = '''import os
os.getcwd()
'''
        with tempfile.NamedTemporaryFile(suffix='.py', delete=False, mode='w') as f:
            f.write(source)
            file_path = f.name

        try:
            tree = ast.parse(source)
            with TyTypesClient() as client:
                mapping = PythonTypeMapping(source, file_path, client)

                call = tree.body[1].value
                result = mapping.method_invocation_type(call)

                mapping.close()

            assert result is not None
            assert isinstance(result, JavaType.Method)
            assert result._name == 'getcwd'
        finally:
            os.unlink(file_path)

    def test_stdlib_method_on_result(self):
        """Test calling methods on stdlib function results."""
        source = '''import os
os.getcwd().split("/")
'''
        with tempfile.NamedTemporaryFile(suffix='.py', delete=False, mode='w') as f:
            f.write(source)
            file_path = f.name

        try:
            tree = ast.parse(source)
            with TyTypesClient() as client:
                mapping = PythonTypeMapping(source, file_path, client)

                call = tree.body[1].value
                result = mapping.method_invocation_type(call)

                mapping.close()

            assert result is not None
            assert result._name == 'split'
            assert result._parameter_types is not None
        finally:
            os.unlink(file_path)


@requires_ty_types_cli
class TestModuleFunctionDeclaringType:
    """Tests that module-level function calls produce the correct declaring type."""

    def test_os_getcwd_declaring_type_is_os(self):
        """import os; os.getcwd() → declaring type FQN should be 'os'."""
        source = '''import os
os.getcwd()
'''
        mapping, tree, tmpdir, client = _make_mapping(source)
        try:
            call = tree.body[1].value
            result = mapping.method_invocation_type(call)
            assert result is not None
            assert result._name == 'getcwd'
            assert result._declaring_type is not None
            assert isinstance(result._declaring_type, JavaType.FullyQualified)
            assert result._declaring_type.fully_qualified_name == 'os'
        finally:
            _cleanup_mapping(mapping, tmpdir, client)

    def test_os_path_join_declaring_type(self):
        """import os; os.path.join() → declaring type FQN should contain 'os.path' or 'posixpath'."""
        source = '''import os
os.path.join("/tmp", "file.txt")
'''
        mapping, tree, tmpdir, client = _make_mapping(source)
        try:
            call = tree.body[1].value
            result = mapping.method_invocation_type(call)
            assert result is not None
            assert result._name == 'join'
            assert result._declaring_type is not None
            assert isinstance(result._declaring_type, JavaType.FullyQualified)
            # os.path is an alias for posixpath/ntpath depending on platform
            fqn = result._declaring_type.fully_qualified_name
            assert 'path' in fqn.lower() or fqn == 'os.path'
        finally:
            _cleanup_mapping(mapping, tmpdir, client)

    def test_json_dumps_declaring_type(self):
        """import json; json.dumps() → declaring type FQN should be 'json'."""
        source = '''import json
json.dumps({"key": "value"})
'''
        mapping, tree, tmpdir, client = _make_mapping(source)
        try:
            call = tree.body[1].value
            result = mapping.method_invocation_type(call)
            assert result is not None
            assert result._name == 'dumps'
            assert result._declaring_type is not None
            assert isinstance(result._declaring_type, JavaType.FullyQualified)
            assert result._declaring_type.fully_qualified_name == 'json'
        finally:
            _cleanup_mapping(mapping, tmpdir, client)


class TestTypeAttributionEdgeCases:
    """Tests for edge cases in type attribution."""


    def test_no_args(self):
        """Test method with no arguments."""
        source = '"hello".upper()'
        tree = ast.parse(source)
        mapping = PythonTypeMapping(source)

        call = tree.body[0].value
        result = mapping.method_invocation_type(call)

        mapping.close()

        assert result is not None
        assert result._name == 'upper'
        # No parameters in the signature
        assert result._parameter_types is None or result._parameter_types == []

    def test_keyword_args(self):
        """Test method with keyword arguments."""
        source = 'print("hello", end="")'
        tree = ast.parse(source)
        mapping = PythonTypeMapping(source)

        call = tree.body[0].value
        result = mapping.method_invocation_type(call)

        mapping.close()

        assert result is not None
        assert result._name == 'print'
        assert result._parameter_types is not None
        # ty-types returns all signature params for print (values, sep, end, file, flush)
        assert len(result._parameter_types) >= 2

    def test_lambda_call(self):
        """Test calling a lambda expression."""
        source = '(lambda x: x)(42)'
        tree = ast.parse(source)
        mapping = PythonTypeMapping(source)

        call = tree.body[0].value
        result = mapping.method_invocation_type(call)

        mapping.close()

        # Lambda calls don't have a simple method name
        assert result is None or result._name is None

    def test_dict_method(self):
        """Test dict method calls."""
        source = '{"a": 1}.get("a")'
        tree = ast.parse(source)
        mapping = PythonTypeMapping(source)

        call = tree.body[0].value
        result = mapping.method_invocation_type(call)

        mapping.close()

        assert result is not None
        assert result._name == 'get'
        assert result._parameter_types is not None
        assert result._parameter_types[0] == JavaType.Primitive.String

    def test_list_method(self):
        """Test list method calls."""
        source = '[1, 2, 3].append(4)'
        tree = ast.parse(source)
        mapping = PythonTypeMapping(source)

        call = tree.body[0].value
        result = mapping.method_invocation_type(call)

        mapping.close()

        assert result is not None
        assert result._name == 'append'
        assert result._parameter_types is not None
        assert result._parameter_types[0] == JavaType.Primitive.Int


class TestByteOffsetConversion:
    """Tests for byte offset conversion utilities."""

    def test_simple_ascii(self):
        """Test byte offset conversion for simple ASCII source."""
        source = 'x = 42\ny = 10\n'
        mapping = PythonTypeMapping(source)

        # Line 1, col 0 -> byte 0
        assert mapping._pos_to_byte_offset(1, 0) == 0
        # Line 1, col 4 -> byte 4
        assert mapping._pos_to_byte_offset(1, 4) == 4
        # Line 2, col 0 -> byte 7 (after 'x = 42\n')
        assert mapping._pos_to_byte_offset(2, 0) == 7

        mapping.close()

    def test_multibyte_chars(self):
        """Test byte offset conversion with UTF-8 multi-byte characters."""
        source = 'x = "héllo"\n'  # é is 2 bytes in UTF-8
        mapping = PythonTypeMapping(source)

        # Line 1, col 0 -> byte 0
        assert mapping._pos_to_byte_offset(1, 0) == 0
        # Line 1, col 5 -> byte 5 (x, space, =, space, ")
        assert mapping._pos_to_byte_offset(1, 5) == 5
        # Line 1, col 6 -> byte 7 (h, é takes 2 bytes)
        assert mapping._pos_to_byte_offset(1, 6) == 6
        # After é: col 7 is the character after é, byte is 8
        assert mapping._pos_to_byte_offset(1, 7) == 8

        mapping.close()

    def test_line_byte_offsets(self):
        """Test line start byte offset computation."""
        source = 'abc\ndef\n'
        source_lines, offsets, _ = compute_source_line_data(source)
        # Line 1 starts at byte 0, line 2 at byte 4 (after 'abc\n')
        assert offsets[0] == 0
        assert offsets[1] == 4
        assert source_lines == ['abc', 'def']

        mapping = PythonTypeMapping(source)
        mapping.close()


@requires_ty_types_cli
class TestStructuredCallSignatures:
    """Tests for structured call signature data from ty-types."""

    def test_split_has_structured_signature(self):
        """Test that str.split() returns structured parameter data."""
        source = '"hello".split(",")'
        tree = ast.parse(source)
        with tempfile.TemporaryDirectory() as tmpdir:
            file_path = os.path.join(tmpdir, 'test.py')
            with open(file_path, 'w') as f:
                f.write(source)

            with TyTypesClient() as client:
                client.initialize(tmpdir)
                mapping = PythonTypeMapping(source, file_path, ty_client=client)

                call = tree.body[0].value
                result = mapping.method_invocation_type(call)

                mapping.close()

        assert result is not None
        assert result._parameter_names is not None
        assert 'sep' in result._parameter_names
        assert result._parameter_types is not None
        assert len(result._parameter_types) == len(result._parameter_names)

    def test_structured_signature_has_correct_types(self):
        """Test that structured signature resolves parameter types."""
        source = '"hello".split(",")'
        tree = ast.parse(source)
        with tempfile.TemporaryDirectory() as tmpdir:
            file_path = os.path.join(tmpdir, 'test.py')
            with open(file_path, 'w') as f:
                f.write(source)

            with TyTypesClient() as client:
                client.initialize(tmpdir)
                mapping = PythonTypeMapping(source, file_path, ty_client=client)

                call = tree.body[0].value
                sig = mapping._lookup_call_signature(call)

                mapping.close()

        assert sig is not None, "Expected structured callSignature data"
        params = sig['parameters']
        assert len(params) >= 1
        assert params[0]['name'] == 'sep'
        assert params[0]['typeId'] is not None
        assert params[0]['hasDefault'] is True

    def test_builtin_len_structured_signature(self):
        """Test structured signature for builtin len()."""
        source = 'len("hello")'
        tree = ast.parse(source)
        with tempfile.TemporaryDirectory() as tmpdir:
            file_path = os.path.join(tmpdir, 'test.py')
            with open(file_path, 'w') as f:
                f.write(source)

            with TyTypesClient() as client:
                client.initialize(tmpdir)
                mapping = PythonTypeMapping(source, file_path, ty_client=client)

                call = tree.body[0].value
                sig = mapping._lookup_call_signature(call)

                mapping.close()

        assert sig is not None, "Expected structured callSignature for len()"
        params = sig['parameters']
        assert len(params) >= 1

    def test_no_signature_for_non_call(self):
        """Test that non-call nodes don't have call signature data."""
        source = 'x = 42'
        tree = ast.parse(source)
        mapping = PythonTypeMapping(source)

        assert len(mapping._call_signature_index) == 0

        mapping.close()


def _make_mapping(source: str) -> tuple:
    """Helper: create a temp file, TyTypesClient, and PythonTypeMapping.

    Returns (mapping, tree, tmpdir_path) inside active context managers.
    Use with _with_mapping() instead for automatic cleanup.
    """
    tree = ast.parse(source)
    tmpdir = tempfile.mkdtemp()
    file_path = os.path.join(tmpdir, 'test.py')
    with open(file_path, 'w') as f:
        f.write(source)
    client = TyTypesClient()
    client.initialize(tmpdir)
    mapping = PythonTypeMapping(source, file_path, ty_client=client)
    return mapping, tree, tmpdir, client


def _cleanup_mapping(mapping, tmpdir, client):
    """Helper: clean up resources from _make_mapping."""
    mapping.close()
    client.shutdown()
    import shutil
    shutil.rmtree(tmpdir, ignore_errors=True)


class TestDeclaringTypeFromLiterals:
    """Tests for declaring type inference from literal receivers (no CLI needed)."""

    def test_list_literal_declaring_type(self):
        source = '[1, 2, 3].append(4)'
        tree = ast.parse(source)
        mapping = PythonTypeMapping(source)

        result = mapping.method_invocation_type(tree.body[0].value)
        mapping.close()

        assert result is not None
        assert result._name == 'append'
        assert result._declaring_type is not None
        assert result._declaring_type._fully_qualified_name == 'list'

    def test_dict_literal_declaring_type(self):
        source = '{"a": 1}.keys()'
        tree = ast.parse(source)
        mapping = PythonTypeMapping(source)

        result = mapping.method_invocation_type(tree.body[0].value)
        mapping.close()

        assert result is not None
        assert result._name == 'keys'
        assert result._declaring_type is not None
        assert result._declaring_type._fully_qualified_name == 'dict'

    def test_set_literal_declaring_type(self):
        source = '{1, 2, 3}.add(4)'
        tree = ast.parse(source)
        mapping = PythonTypeMapping(source)

        result = mapping.method_invocation_type(tree.body[0].value)
        mapping.close()

        assert result is not None
        assert result._name == 'add'
        assert result._declaring_type is not None
        assert result._declaring_type._fully_qualified_name == 'set'

    def test_tuple_literal_declaring_type(self):
        source = '(1, 2, 3).count(1)'
        tree = ast.parse(source)
        mapping = PythonTypeMapping(source)

        result = mapping.method_invocation_type(tree.body[0].value)
        mapping.close()

        assert result is not None
        assert result._name == 'count'
        assert result._declaring_type is not None
        assert result._declaring_type._fully_qualified_name == 'tuple'

    def test_bytes_literal_declaring_type(self):
        source = 'b"hello".decode()'
        tree = ast.parse(source)
        mapping = PythonTypeMapping(source)

        result = mapping.method_invocation_type(tree.body[0].value)
        mapping.close()

        assert result is not None
        assert result._name == 'decode'
        assert result._declaring_type is not None
        assert result._declaring_type._fully_qualified_name == 'bytes'

    def test_int_literal_declaring_type(self):
        source = '(42).bit_length()'
        tree = ast.parse(source)
        mapping = PythonTypeMapping(source)

        result = mapping.method_invocation_type(tree.body[0].value)
        mapping.close()

        assert result is not None
        assert result._name == 'bit_length'
        assert result._declaring_type is not None
        assert result._declaring_type._fully_qualified_name == 'int'


@requires_ty_types_cli
class TestReturnTypes:
    """Tests for return type resolution via ty-types."""

    def test_str_upper_returns_str(self):
        source = '"hello".upper()'
        mapping, tree, tmpdir, client = _make_mapping(source)
        try:
            result = mapping.method_invocation_type(tree.body[0].value)
            assert result is not None
            assert result._return_type == JavaType.Primitive.String
        finally:
            _cleanup_mapping(mapping, tmpdir, client)

    def test_len_returns_int(self):
        source = 'len("hello")'
        mapping, tree, tmpdir, client = _make_mapping(source)
        try:
            result = mapping.method_invocation_type(tree.body[0].value)
            assert result is not None
            assert result._return_type == JavaType.Primitive.Int
        finally:
            _cleanup_mapping(mapping, tmpdir, client)

    def test_str_split_returns_list(self):
        source = '"hello world".split()'
        mapping, tree, tmpdir, client = _make_mapping(source)
        try:
            result = mapping.method_invocation_type(tree.body[0].value)
            assert result is not None
            assert result._return_type is not None
            assert isinstance(result._return_type, (JavaType.Class, JavaType.Parameterized))
            assert result._return_type.fully_qualified_name == 'list'
        finally:
            _cleanup_mapping(mapping, tmpdir, client)

    def test_str_startswith_returns_bool(self):
        source = '"hello".startswith("he")'
        mapping, tree, tmpdir, client = _make_mapping(source)
        try:
            result = mapping.method_invocation_type(tree.body[0].value)
            assert result is not None
            assert result._return_type == JavaType.Primitive.Boolean
        finally:
            _cleanup_mapping(mapping, tmpdir, client)

    def test_str_find_returns_int(self):
        source = '"hello".find("l")'
        mapping, tree, tmpdir, client = _make_mapping(source)
        try:
            result = mapping.method_invocation_type(tree.body[0].value)
            assert result is not None
            assert result._return_type == JavaType.Primitive.Int
        finally:
            _cleanup_mapping(mapping, tmpdir, client)


@requires_ty_types_cli
class TestDeclaringTypeWithTyTypes:
    """Tests for declaring type resolution using ty-types inference."""

    def test_variable_method_declaring_type(self):
        """Calling a method on a typed variable resolves the declaring type."""
        source = 'x: str = "hello"\nx.upper()'
        mapping, tree, tmpdir, client = _make_mapping(source)
        try:
            call = tree.body[1].value
            result = mapping.method_invocation_type(call)
            assert result is not None
            assert result._name == 'upper'
            assert result._declaring_type is not None
            assert result._declaring_type._fully_qualified_name == 'str'
        finally:
            _cleanup_mapping(mapping, tmpdir, client)

    def test_chained_call_declaring_type(self):
        """Chained calls resolve the intermediate return type as declaring type."""
        source = '"hello world".split().copy()'
        mapping, tree, tmpdir, client = _make_mapping(source)
        try:
            call = tree.body[0].value  # .copy()
            result = mapping.method_invocation_type(call)
            assert result is not None
            assert result._name == 'copy'
            assert result._declaring_type is not None
            assert result._declaring_type._fully_qualified_name.startswith('list')
        finally:
            _cleanup_mapping(mapping, tmpdir, client)

    def test_user_defined_class_method(self):
        """Method call on a user-defined class instance."""
        source = '''class Greeter:
    def greet(self, name: str) -> str:
        return "Hello, " + name

g = Greeter()
g.greet("World")
'''
        mapping, tree, tmpdir, client = _make_mapping(source)
        try:
            call = tree.body[2].value  # g.greet("World")
            result = mapping.method_invocation_type(call)
            assert result is not None
            assert result._name == 'greet'
            assert result._declaring_type is not None
            assert result._declaring_type._fully_qualified_name.endswith('Greeter')
        finally:
            _cleanup_mapping(mapping, tmpdir, client)

    def test_user_defined_class_method_return_type(self):
        """Return type from a user-defined class method."""
        source = '''class Greeter:
    def greet(self, name: str) -> str:
        return "Hello, " + name

g = Greeter()
g.greet("World")
'''
        mapping, tree, tmpdir, client = _make_mapping(source)
        try:
            call = tree.body[2].value
            result = mapping.method_invocation_type(call)
            assert result is not None
            assert result._return_type == JavaType.Primitive.String
        finally:
            _cleanup_mapping(mapping, tmpdir, client)

    def test_stdlib_os_path_join(self):
        """os.path.join() resolves correctly."""
        source = '''import os
os.path.join("/tmp", "file.txt")
'''
        mapping, tree, tmpdir, client = _make_mapping(source)
        try:
            call = tree.body[1].value
            result = mapping.method_invocation_type(call)
            assert result is not None
            assert result._name == 'join'
            assert result._return_type == JavaType.Primitive.String
        finally:
            _cleanup_mapping(mapping, tmpdir, client)


@requires_ty_types_cli
class TestUnionTypes:
    """Tests for union / Optional type resolution."""

    def test_optional_str_variable_method(self):
        """Calling a method on an Optional[str] variable that's been narrowed."""
        source = '''from typing import Optional
x: Optional[str] = "hello"
if x is not None:
    x.upper()
'''
        mapping, tree, tmpdir, client = _make_mapping(source)
        try:
            # x.upper() is inside the if body
            call = tree.body[2].body[0].value
            result = mapping.method_invocation_type(call)
            assert result is not None
            assert result._name == 'upper'
            assert result._declaring_type is not None
            assert result._declaring_type._fully_qualified_name == 'str'
        finally:
            _cleanup_mapping(mapping, tmpdir, client)

    def test_union_return_type(self):
        """Function returning Optional[str] should have str or Unknown return type."""
        source = '''from typing import Optional
def maybe_name() -> Optional[str]:
    return "Alice"

maybe_name()
'''
        mapping, tree, tmpdir, client = _make_mapping(source)
        try:
            call = tree.body[2].value  # maybe_name()
            result = mapping.method_invocation_type(call)
            assert result is not None
            assert result._name == 'maybe_name'
            # Return type should resolve to str (unwrapping Optional).
            # May be Unknown if ty-types doesn't emit descriptors for union members.
            if result._return_type is not None and not isinstance(result._return_type, JavaType.Unknown):
                assert result._return_type == JavaType.Primitive.String
        finally:
            _cleanup_mapping(mapping, tmpdir, client)


@requires_ty_types_cli
class TestVariableTypes:
    """Tests for type() on variable/expression nodes."""

    def test_typed_variable(self):
        """type() on a variable with explicit annotation."""
        source = 'x: int = 42\nx'
        mapping, tree, tmpdir, client = _make_mapping(source)
        try:
            name_node = tree.body[1].value  # the bare 'x' expression
            result = mapping.type(name_node)
            assert result == JavaType.Primitive.Int
        finally:
            _cleanup_mapping(mapping, tmpdir, client)

    def test_inferred_str_variable(self):
        """type() on a variable inferred as str."""
        source = 'x = "hello"\nx'
        mapping, tree, tmpdir, client = _make_mapping(source)
        try:
            name_node = tree.body[1].value
            result = mapping.type(name_node)
            assert result == JavaType.Primitive.String
        finally:
            _cleanup_mapping(mapping, tmpdir, client)

    def test_type_on_call_node_returns_return_type(self):
        """type() on a Call node returns the return type, not JavaType.Method."""
        source = 'len("hello")'
        mapping, tree, tmpdir, client = _make_mapping(source)
        try:
            call = tree.body[0].value
            result = mapping.type(call)
            assert result is not None
            assert not isinstance(result, JavaType.Method), \
                "type() should return the expression type, not JavaType.Method"
            assert result == JavaType.Primitive.Int
        finally:
            _cleanup_mapping(mapping, tmpdir, client)

    def test_assignment_from_call_has_return_type(self):
        """type() on `"a-b-c".split("-", 1)` returns list type, not JavaType.Method."""
        source = 'parts = "a-b-c".split("-", 1)'
        mapping, tree, tmpdir, client = _make_mapping(source)
        try:
            call = tree.body[0].value  # the Call node
            result = mapping.type(call)
            assert result is not None
            assert not isinstance(result, JavaType.Method), \
                "type() should return the expression type, not JavaType.Method"
            assert isinstance(result, (JavaType.Class, JavaType.Parameterized))
            assert result.fully_qualified_name == 'list'
        finally:
            _cleanup_mapping(mapping, tmpdir, client)

    def test_split_method_has_list_return_type(self):
        """method_invocation_type() for str.split() has list type as return type."""
        source = 'parts = "a-b-c".split("-", 1)'
        mapping, tree, tmpdir, client = _make_mapping(source)
        try:
            call = tree.body[0].value  # the Call node
            result = mapping.method_invocation_type(call)
            assert result is not None
            assert isinstance(result, JavaType.Method)
            assert result._name == 'split'
            assert result._return_type is not None
            assert not isinstance(result._return_type, JavaType.Unknown), \
                "Return type should not be Unknown"
            assert isinstance(result._return_type, (JavaType.Class, JavaType.Parameterized))
            assert result._return_type.fully_qualified_name == 'list'
        finally:
            _cleanup_mapping(mapping, tmpdir, client)


@requires_ty_types_cli
class TestParameterizedTypes:
    """Tests for Parameterized type creation (e.g., list[str])."""

    def test_split_returns_parameterized_list(self):
        """str.split() should return Parameterized(list, [str])."""
        source = '"hello world".split()'
        mapping, tree, tmpdir, client = _make_mapping(source)
        try:
            result = mapping.method_invocation_type(tree.body[0].value)
            assert result is not None
            rt = result._return_type
            assert rt is not None
            assert isinstance(rt, JavaType.Parameterized), \
                f"Expected Parameterized, got {type(rt).__name__}"
            assert rt._type._fully_qualified_name == 'list'
            assert rt._type_parameters is not None
            assert len(rt._type_parameters) >= 1
        finally:
            _cleanup_mapping(mapping, tmpdir, client)

    def test_parameterized_fqn_delegates(self):
        """Parameterized.fully_qualified_name should delegate to base type."""
        source = '"hello world".split()'
        mapping, tree, tmpdir, client = _make_mapping(source)
        try:
            result = mapping.method_invocation_type(tree.body[0].value)
            assert result is not None
            rt = result._return_type
            assert isinstance(rt, JavaType.Parameterized)
            assert rt.fully_qualified_name == 'list'
        finally:
            _cleanup_mapping(mapping, tmpdir, client)

    def test_list_class_has_supertype(self):
        """The list class should have a supertype populated from classLiteral."""
        source = '"hello world".split()'
        mapping, tree, tmpdir, client = _make_mapping(source)
        try:
            result = mapping.method_invocation_type(tree.body[0].value)
            assert result is not None
            rt = result._return_type
            assert isinstance(rt, JavaType.Parameterized)
            base = rt._type
            assert isinstance(base, JavaType.Class)
            assert getattr(base, '_supertype', None) is not None, \
                "list class should have a supertype"
        finally:
            _cleanup_mapping(mapping, tmpdir, client)

    def test_list_class_has_methods(self):
        """The list class should have methods populated from classLiteral."""
        source = '"hello world".split()'
        mapping, tree, tmpdir, client = _make_mapping(source)
        try:
            result = mapping.method_invocation_type(tree.body[0].value)
            assert result is not None
            rt = result._return_type
            assert isinstance(rt, JavaType.Parameterized)
            base = rt._type
            assert isinstance(base, JavaType.Class)
            methods = getattr(base, '_methods', None)
            assert methods is not None, "list class should have methods"
            assert len(methods) > 0
            method_names = [m._name for m in methods]
            assert 'copy' in method_names
            assert 'append' in method_names
        finally:
            _cleanup_mapping(mapping, tmpdir, client)


@requires_ty_types_cli
class TestMethodSignatureDetails:
    """Tests for parameter name and type resolution in method signatures."""

    def test_user_function_param_names(self):
        """Parameter names are resolved from a user-defined function."""
        source = '''def add(a: int, b: int) -> int:
    return a + b

add(1, 2)
'''
        mapping, tree, tmpdir, client = _make_mapping(source)
        try:
            call = tree.body[1].value
            result = mapping.method_invocation_type(call)
            assert result is not None
            assert result._name == 'add'
            assert result._parameter_names is not None
            assert 'a' in result._parameter_names
            assert 'b' in result._parameter_names
        finally:
            _cleanup_mapping(mapping, tmpdir, client)

    def test_user_function_param_types(self):
        """Parameter types are resolved from a user-defined function."""
        source = '''def add(a: int, b: int) -> int:
    return a + b

add(1, 2)
'''
        mapping, tree, tmpdir, client = _make_mapping(source)
        try:
            call = tree.body[1].value
            result = mapping.method_invocation_type(call)
            assert result is not None
            assert result._parameter_types is not None
            assert len(result._parameter_types) == 2
            assert result._parameter_types[0] == JavaType.Primitive.Int
            assert result._parameter_types[1] == JavaType.Primitive.Int
        finally:
            _cleanup_mapping(mapping, tmpdir, client)

    def test_method_filters_self_param(self):
        """Method signature should not include 'self' in parameter names."""
        source = '''class Calculator:
    def add(self, a: int, b: int) -> int:
        return a + b

c = Calculator()
c.add(1, 2)
'''
        mapping, tree, tmpdir, client = _make_mapping(source)
        try:
            call = tree.body[2].value  # c.add(1, 2)
            result = mapping.method_invocation_type(call)
            assert result is not None
            assert result._name == 'add'
            if result._parameter_names is not None:
                assert 'self' not in result._parameter_names
        finally:
            _cleanup_mapping(mapping, tmpdir, client)

    def test_default_param_still_listed(self):
        """Parameters with defaults are still included."""
        source = '''def greet(name: str, greeting: str = "Hello") -> str:
    return f"{greeting}, {name}!"

greet("World")
'''
        mapping, tree, tmpdir, client = _make_mapping(source)
        try:
            call = tree.body[1].value
            result = mapping.method_invocation_type(call)
            assert result is not None
            assert result._parameter_names is not None
            assert 'name' in result._parameter_names
            assert 'greeting' in result._parameter_names
        finally:
            _cleanup_mapping(mapping, tmpdir, client)


@requires_ty_types_cli
class TestTypingAliases:
    """Tests for typing module type aliases (e.g. typing.Text → str)."""

    def test_typing_text_resolves_to_str(self):
        """typing.Text (deprecated alias for str) should resolve to Primitive.String.

        ty-types resolves typing.Text to str at the type level, so our type_mapping
        receives className='str' and maps it to JavaType.Primitive.String automatically.
        """
        source = 'from typing import Text\nx: Text = "hello"\nx\n'
        mapping, tree, tmpdir, client = _make_mapping(source)
        try:
            name_node = tree.body[2].value  # bare 'x' expression
            result = mapping.type(name_node)
            assert result == JavaType.Primitive.String, \
                f"typing.Text should resolve to Primitive.String, got {result}"
        finally:
            _cleanup_mapping(mapping, tmpdir, client)


class TestCyclicTypeResolution:
    """Tests that cyclic type references don't cause infinite recursion."""

    def test_cyclic_subclass_of_does_not_recurse(self):
        """Cyclic subclassOf references should return a placeholder Class, not crash."""
        mapping = PythonTypeMapping("", file_path=None)

        # Simulate two subclassOf descriptors that reference each other
        mapping._type_registry[10] = {'kind': 'subclassOf', 'base': 11}
        mapping._type_registry[11] = {'kind': 'subclassOf', 'base': 10}

        # This should NOT raise RecursionError
        result = mapping._resolve_type(10)

        # Should get a Class (possibly empty) instead of crashing
        assert result is not None
        assert isinstance(result, JavaType.Class)

    def test_cyclic_union_does_not_recurse(self):
        """Cyclic union members should not cause infinite recursion."""
        mapping = PythonTypeMapping("", file_path=None)

        mapping._type_registry[20] = {'kind': 'union', 'members': [21]}
        mapping._type_registry[21] = {'kind': 'subclassOf', 'base': 20}

        result = mapping._resolve_type(20)
        assert result is not None

    def test_resolved_type_is_cached(self):
        """Resolved types should be cached by type_id."""
        mapping = PythonTypeMapping("", file_path=None)

        mapping._type_registry[30] = {
            'kind': 'instance',
            'className': 'MyClass',
            'moduleName': 'mymod',
        }

        result1 = mapping._resolve_type(30)
        result2 = mapping._resolve_type(30)

        # Should return the same cached object
        assert result1 is result2
        assert isinstance(result1, JavaType.Class)

    def test_class_objects_reuse_fqn_dedup(self):
        """Different type_ids with same FQN should share the same Class object."""
        mapping = PythonTypeMapping("", file_path=None)

        # Two different type_ids that resolve to the same FQN
        mapping._type_registry[40] = {
            'kind': 'instance',
            'className': 'Shared',
            'moduleName': 'pkg',
        }
        mapping._type_registry[41] = {
            'kind': 'instance',
            'className': 'Shared',
            'moduleName': 'pkg',
        }

        result1 = mapping._resolve_type(40)
        result2 = mapping._resolve_type(41)

        # Both should resolve to the same Class object (FQN-based dedup)
        assert result1 is result2
        assert isinstance(result1, JavaType.Class)
        assert result1._fully_qualified_name == 'pkg.Shared'

    def test_primitive_not_wrapped_in_class(self):
        """Primitive types should not create unnecessary Class wrappers."""
        mapping = PythonTypeMapping("", file_path=None)

        mapping._type_registry[50] = {
            'kind': 'instance',
            'className': 'int',
        }

        result = mapping._resolve_type(50)
        assert result is JavaType.Primitive.Int


class TestParamSpecAndConcatenate:
    """Unit tests for the ty-types 0.0.31 `paramSpecName` / `concatenatePrefix` fields.

    Use mock descriptors so the logic is exercised without the ty-types CLI.
    End-to-end tests against real source live in
    TestParamSpecAndConcatenateIntegration below.
    """

    def test_paramspec_pair_collapses_to_single_entry(self):
        """The synthetic `*args` + `**kwargs` pair is folded into one entry
        whose name is the ParamSpec's name and whose type is Unknown."""
        mapping = PythonTypeMapping("", file_path=None)
        params = [
            {'name': 'args', 'kind': 'variadic', 'paramSpecName': 'P'},
            {'name': 'kwargs', 'kind': 'keywordVariadic', 'paramSpecName': 'P'},
        ]
        names, types = mapping._process_method_params(params)
        assert names == ['P']
        assert len(types) == 1
        assert isinstance(types[0], JavaType.Unknown)

    def test_concatenate_prefix_is_treated_as_positional(self):
        """A param flagged `concatenatePrefix: True` is emitted as-is;
        the trailing ParamSpec pair still collapses behind it."""
        mapping = PythonTypeMapping("", file_path=None)
        mapping._type_registry[7] = {'kind': 'instance', 'className': 'int'}
        params = [
            {'name': '', 'kind': 'positionalOnly', 'typeId': 7,
             'concatenatePrefix': True},
            {'name': 'args', 'kind': 'variadic', 'paramSpecName': 'P'},
            {'name': 'kwargs', 'kind': 'keywordVariadic', 'paramSpecName': 'P'},
        ]
        names, types = mapping._process_method_params(params)
        assert names == ['', 'P']
        assert types[0] is JavaType.Primitive.Int
        assert isinstance(types[1], JavaType.Unknown)

    def test_plain_params_unchanged(self):
        """Regression: descriptors without the new fields still produce
        one entry per parameter with the declared type."""
        mapping = PythonTypeMapping("", file_path=None)
        mapping._type_registry[1] = {'kind': 'instance', 'className': 'int'}
        mapping._type_registry[2] = {'kind': 'instance', 'className': 'int'}
        params = [
            {'name': 'a', 'typeId': 1},
            {'name': 'b', 'typeId': 2},
        ]
        names, types = mapping._process_method_params(params)
        assert names == ['a', 'b']
        assert types == [JavaType.Primitive.Int, JavaType.Primitive.Int]

    def test_self_and_cls_still_filtered(self):
        """Filtering of self/cls still works when new fields are present."""
        mapping = PythonTypeMapping("", file_path=None)
        params = [
            {'name': 'self'},
            {'name': 'args', 'kind': 'variadic', 'paramSpecName': 'P'},
            {'name': 'kwargs', 'kind': 'keywordVariadic', 'paramSpecName': 'P'},
        ]
        names, _ = mapping._process_method_params(params)
        assert names == ['P']


@requires_ty_types_cli
class TestParamSpecAndConcatenateIntegration:
    """End-to-end tests exercising ty-types 0.0.31 ParamSpec/Concatenate output."""

    def test_callable_paramspec_collapses_in_invocation(self):
        """`cb()` where `cb: Callable[P, R]` yields a method type with a
        single collapsed `P` parameter rather than two variadic entries."""
        source = '''from typing import Callable, ParamSpec, TypeVar
P = ParamSpec('P')
R = TypeVar('R')

def run(cb: Callable[P, R]) -> R:
    return cb()

run(lambda: 42)
'''
        mapping, tree, tmpdir, client = _make_mapping(source)
        try:
            cb_call = tree.body[3].body[0].value  # cb() inside run
            result = mapping.method_invocation_type(cb_call)
            assert result is not None
            assert result._parameter_names == ['P']
            assert result._parameter_types is not None
            assert len(result._parameter_types) == 1
            assert isinstance(result._parameter_types[0], JavaType.Unknown)
        finally:
            _cleanup_mapping(mapping, tmpdir, client)

    def test_concatenate_keeps_prefix_and_collapses_tail(self):
        """`cb(1)` where `cb: Callable[Concatenate[int, P], R]` yields a
        method type with the leading `int` plus a single collapsed `P`."""
        source = '''from typing import Callable, Concatenate, ParamSpec, TypeVar
P = ParamSpec('P')
R = TypeVar('R')

def run(cb: Callable[Concatenate[int, P], R]) -> R:
    return cb(1)

run(lambda x: x)
'''
        mapping, tree, tmpdir, client = _make_mapping(source)
        try:
            cb_call = tree.body[3].body[0].value  # cb(1) inside run
            result = mapping.method_invocation_type(cb_call)
            assert result is not None
            assert result._parameter_names is not None
            assert len(result._parameter_names) == 2
            assert result._parameter_names[-1] == 'P'
            assert result._parameter_types is not None
            assert result._parameter_types[0] == JavaType.Primitive.Int
            assert isinstance(result._parameter_types[1], JavaType.Unknown)
        finally:
            _cleanup_mapping(mapping, tmpdir, client)

    def test_plain_function_method_type_unchanged(self):
        """Regression: a function with no ParamSpec/Concatenate produces the
        same (name, type) pairs it did before the 0.0.31 field additions."""
        source = '''def add(a: int, b: int) -> int:
    return a + b

add(1, 2)
'''
        mapping, tree, tmpdir, client = _make_mapping(source)
        try:
            call = tree.body[1].value
            result = mapping.method_invocation_type(call)
            assert result is not None
            assert result._parameter_names == ['a', 'b']
            assert result._parameter_types == [
                JavaType.Primitive.Int, JavaType.Primitive.Int]
        finally:
            _cleanup_mapping(mapping, tmpdir, client)


@requires_ty_types_cli
class TestClassKind:
    """Tests for JavaType.Class.Kind inference from ty-types data."""

    def test_enum_class_has_enum_kind(self):
        """A class inheriting from Enum should have Kind.Enum."""
        source = '''from enum import Enum

class Color(Enum):
    RED = 1
    GREEN = 2

x = Color.RED
'''
        mapping, tree, tmpdir, client = _make_mapping(source)
        try:
            # x = Color.RED — the type of x should be Color
            name_node = tree.body[2].targets[0]  # x
            result = mapping.type(name_node)
            assert result is not None
            assert isinstance(result, JavaType.Class)
            assert result._fully_qualified_name.endswith('Color')
            assert result._kind == JavaType.FullyQualified.Kind.Enum
        finally:
            _cleanup_mapping(mapping, tmpdir, client)

    def test_protocol_class_has_interface_kind(self):
        """A class inheriting from Protocol should have Kind.Interface."""
        source = '''from typing import Protocol

class Greeter(Protocol):
    def greet(self, name: str) -> str: ...

x: Greeter
'''
        mapping, tree, tmpdir, client = _make_mapping(source)
        try:
            name_node = tree.body[2].target  # x
            result = mapping.type(name_node)
            assert result is not None
            assert isinstance(result, JavaType.Class)
            assert result._fully_qualified_name.endswith('Greeter')
            assert result._kind == JavaType.FullyQualified.Kind.Interface
        finally:
            _cleanup_mapping(mapping, tmpdir, client)

    def test_regular_class_has_class_kind(self):
        """A regular class should have Kind.Class."""
        source = '''class MyClass:
    pass

x = MyClass()
'''
        mapping, tree, tmpdir, client = _make_mapping(source)
        try:
            name_node = tree.body[1].targets[0]  # x
            result = mapping.type(name_node)
            assert result is not None
            assert isinstance(result, JavaType.Class)
            assert result._fully_qualified_name.endswith('MyClass')
            assert result._kind == JavaType.FullyQualified.Kind.Class
        finally:
            _cleanup_mapping(mapping, tmpdir, client)

    def test_multiple_supertypes_stored_as_interfaces(self):
        """Python multiple inheritance: first supertype → _supertype, rest → _interfaces."""
        source = '''class Mixin:
    def mix(self) -> str:
        return 'mixed'

class Base:
    def base_method(self) -> int:
        return 42

class Multi(Base, Mixin):
    pass

x = Multi()
'''
        mapping, tree, tmpdir, client = _make_mapping(source)
        try:
            name_node = tree.body[3].targets[0]  # x
            result = mapping.type(name_node)
            assert result is not None
            assert isinstance(result, JavaType.Class)
            assert result._fully_qualified_name.endswith('Multi')
            # First supertype → _supertype
            assert getattr(result, '_supertype', None) is not None, \
                "Multi should have a supertype"
            assert result._supertype._fully_qualified_name.endswith('Base')
            # Remaining supertypes → _interfaces
            interfaces = getattr(result, '_interfaces', None)
            assert interfaces is not None, \
                "Multi should have interfaces for additional supertypes"
            assert len(interfaces) >= 1
            iface_names = [i._fully_qualified_name for i in interfaces]
            assert any(n.endswith('Mixin') for n in iface_names)
        finally:
            _cleanup_mapping(mapping, tmpdir, client)


class TestGenericTypeVariable:
    """Tests for typeVar → GenericTypeVariable conversion."""

    def test_plain_typevar_creates_generic_type_variable(self):
        """A typeVar descriptor with just a name should create a GenericTypeVariable."""
        mapping = PythonTypeMapping("", file_path=None)
        mapping._type_registry[100] = {
            'kind': 'typeVar',
            'name': 'T',
            'variance': 'invariant',
        }

        result = mapping._resolve_type(100)
        assert isinstance(result, JavaType.GenericTypeVariable)
        assert result.name == 'T'
        assert result.variance == JavaType.GenericTypeVariable.Variance.Invariant
        assert result.bounds == []

    def test_covariant_typevar(self):
        """A typeVar with covariant variance should map correctly."""
        mapping = PythonTypeMapping("", file_path=None)
        mapping._type_registry[100] = {
            'kind': 'typeVar',
            'name': 'T_co',
            'variance': 'covariant',
        }

        result = mapping._resolve_type(100)
        assert isinstance(result, JavaType.GenericTypeVariable)
        assert result.name == 'T_co'
        assert result.variance == JavaType.GenericTypeVariable.Variance.Covariant

    def test_contravariant_typevar(self):
        """A typeVar with contravariant variance should map correctly."""
        mapping = PythonTypeMapping("", file_path=None)
        mapping._type_registry[100] = {
            'kind': 'typeVar',
            'name': 'T_contra',
            'variance': 'contravariant',
        }

        result = mapping._resolve_type(100)
        assert isinstance(result, JavaType.GenericTypeVariable)
        assert result.name == 'T_contra'
        assert result.variance == JavaType.GenericTypeVariable.Variance.Contravariant

    def test_typevar_with_upper_bound(self):
        """A typeVar with an upperBound should have a bounds list."""
        mapping = PythonTypeMapping("", file_path=None)
        mapping._type_registry[100] = {
            'kind': 'typeVar',
            'name': 'T',
            'variance': 'invariant',
            'upperBound': 101,
        }
        mapping._type_registry[101] = {
            'kind': 'instance',
            'className': 'int',
        }

        result = mapping._resolve_type(100)
        assert isinstance(result, JavaType.GenericTypeVariable)
        assert result.name == 'T'
        assert len(result.bounds) == 1
        assert result.bounds[0] is JavaType.Primitive.Int

    def test_typevar_with_class_upper_bound(self):
        """A typeVar bounded by a class should resolve the bound."""
        mapping = PythonTypeMapping("", file_path=None)
        mapping._type_registry[100] = {
            'kind': 'typeVar',
            'name': 'T',
            'variance': 'covariant',
            'upperBound': 101,
        }
        mapping._type_registry[101] = {
            'kind': 'instance',
            'className': 'Comparable',
            'moduleName': 'builtins',
        }

        result = mapping._resolve_type(100)
        assert isinstance(result, JavaType.GenericTypeVariable)
        assert result.variance == JavaType.GenericTypeVariable.Variance.Covariant
        assert len(result.bounds) == 1
        assert isinstance(result.bounds[0], JavaType.Class)
        assert result.bounds[0].fully_qualified_name == 'Comparable'

    def test_typevar_without_variance_defaults_to_invariant(self):
        """A typeVar without explicit variance should default to Invariant."""
        mapping = PythonTypeMapping("", file_path=None)
        mapping._type_registry[100] = {
            'kind': 'typeVar',
            'name': 'T',
        }

        result = mapping._resolve_type(100)
        assert isinstance(result, JavaType.GenericTypeVariable)
        assert result.name == 'T'
        assert result.variance == JavaType.GenericTypeVariable.Variance.Invariant

    def test_typevar_without_name_returns_unknown(self):
        """A typeVar without a name should return Unknown."""
        mapping = PythonTypeMapping("", file_path=None)
        mapping._type_registry[100] = {
            'kind': 'typeVar',
            'name': '',
        }

        result = mapping._resolve_type(100)
        assert isinstance(result, JavaType.Unknown)

    def test_typevar_cached_by_type_id(self):
        """Resolved GenericTypeVariable should be cached by type_id."""
        mapping = PythonTypeMapping("", file_path=None)
        mapping._type_registry[100] = {
            'kind': 'typeVar',
            'name': 'T',
            'variance': 'invariant',
        }

        result1 = mapping._resolve_type(100)
        result2 = mapping._resolve_type(100)
        assert result1 is result2


@requires_ty_types_cli
class TestImportedNameTypeAttribution:
    """Tests for type attribution of names imported via 'from X import Y'.

    When a name is imported (e.g. `from typing import Callable`) and then
    used in a type annotation, the identifier's field_type should resolve
    to a JavaType.Class with the fully qualified name, not JavaType.Unknown.
    """

    def test_typing_callable_has_fqn(self):
        """typing.Callable in a type annotation should resolve to typing.Callable, not Unknown."""
        source = '''from typing import Callable
handler: Callable[[int], str] = lambda x: str(x)
'''
        mapping, tree, tmpdir, client = _make_mapping(source)
        try:
            # handler: Callable[...] — the annotation target 'handler'
            ann_node = tree.body[1]  # AnnAssign
            # The annotation itself is the Subscript: Callable[[int], str]
            annotation = ann_node.annotation  # ast.Subscript
            # annotation.value is the Name node for 'Callable'
            callable_name = annotation.value
            result = mapping.type(callable_name)
            assert result is not None, "Callable name node should have a type"
            assert not isinstance(result, JavaType.Unknown), \
                f"Expected typing.Callable to resolve to a Class, got Unknown"
            assert isinstance(result, (JavaType.Class, JavaType.FullyQualified)), \
                f"Expected JavaType.Class, got {type(result).__qualname__}"
            assert 'typing' in result._fully_qualified_name, \
                f"Expected FQN containing 'typing', got '{result._fully_qualified_name}'"
        finally:
            _cleanup_mapping(mapping, tmpdir, client)

    def test_typing_callable_qualified_has_fqn(self):
        """typing.Callable via qualified access should also resolve correctly."""
        source = '''import typing
handler: typing.Callable[[int], str] = lambda x: str(x)
'''
        mapping, tree, tmpdir, client = _make_mapping(source)
        try:
            ann_node = tree.body[1]  # AnnAssign
            annotation = ann_node.annotation  # ast.Subscript
            # annotation.value is the Attribute node: typing.Callable
            attr_node = annotation.value  # ast.Attribute
            result = mapping.type(attr_node)
            assert result is not None, "typing.Callable attribute node should have a type"
            assert not isinstance(result, JavaType.Unknown), \
                f"Expected typing.Callable to resolve to a Class, got Unknown"
        finally:
            _cleanup_mapping(mapping, tmpdir, client)


class TestCallableDescriptor:
    """Tests for the callable kind descriptor (Callable[[int], str])."""

    def test_callable_with_return_type(self):
        """A callable descriptor with returnType should resolve to the return type."""
        mapping = PythonTypeMapping("", file_path=None)
        mapping._type_registry[200] = {
            'kind': 'callable',
            'parameters': [
                {'name': '', 'kind': 'positionalOnly', 'typeId': 201, 'hasDefault': False},
            ],
            'returnType': 202,
        }
        mapping._type_registry[201] = {'kind': 'instance', 'className': 'int'}
        mapping._type_registry[202] = {'kind': 'instance', 'className': 'str'}

        result = mapping._resolve_type(200)
        assert result == JavaType.Primitive.String

    def test_callable_without_return_type(self):
        """A callable descriptor without returnType should return Unknown."""
        mapping = PythonTypeMapping("", file_path=None)
        mapping._type_registry[200] = {
            'kind': 'callable',
            'parameters': [],
        }

        result = mapping._resolve_type(200)
        assert isinstance(result, JavaType.Unknown)

    def test_callable_is_not_variable(self):
        """Callable descriptors should not be treated as variables."""
        mapping = PythonTypeMapping("", file_path=None)
        descriptor = {'kind': 'callable', 'parameters': [], 'returnType': None}
        assert not mapping._is_variable_descriptor(descriptor)


class TestWrapperDescriptor:
    """Tests for the wrapperDescriptor kind."""

    def test_wrapper_descriptor_with_return_type(self):
        """A wrapperDescriptor with returnType should resolve to the return type."""
        mapping = PythonTypeMapping("", file_path=None)
        mapping._type_registry[300] = {
            'kind': 'wrapperDescriptor',
            'descriptorKind': 'Get',
            'parameters': [
                {'name': 'self', 'kind': 'positionalOrKeyword', 'typeId': 301, 'hasDefault': False},
            ],
            'returnType': 302,
        }
        mapping._type_registry[301] = {'kind': 'instance', 'className': 'MyClass'}
        mapping._type_registry[302] = {'kind': 'instance', 'className': 'int'}

        result = mapping._resolve_type(300)
        assert result == JavaType.Primitive.Int

    def test_wrapper_descriptor_is_not_variable(self):
        """wrapperDescriptor should not be treated as a variable."""
        mapping = PythonTypeMapping("", file_path=None)
        descriptor = {'kind': 'wrapperDescriptor', 'descriptorKind': 'Set'}
        assert not mapping._is_variable_descriptor(descriptor)


class TestKnownInstanceDescriptor:
    """Tests for the knownInstance kind."""

    def test_known_instance_resolves_to_class(self):
        """A knownInstance with className should resolve to a class type."""
        mapping = PythonTypeMapping("", file_path=None)
        mapping._type_registry[400] = {
            'kind': 'knownInstance',
            'className': 'TypeVar',
        }

        result = mapping._resolve_type(400)
        assert isinstance(result, JavaType.Class)
        assert result._fully_qualified_name == 'typing.TypeVar'

    def test_known_instance_special_form(self):
        """A knownInstance for _SpecialForm should resolve correctly."""
        mapping = PythonTypeMapping("", file_path=None)
        mapping._type_registry[400] = {
            'kind': 'knownInstance',
            'className': '_SpecialForm',
        }

        result = mapping._resolve_type(400)
        assert isinstance(result, JavaType.Class)
        assert 'typing' in result._fully_qualified_name

    def test_known_instance_empty_classname_returns_unknown(self):
        """A knownInstance without className should return Unknown."""
        mapping = PythonTypeMapping("", file_path=None)
        mapping._type_registry[400] = {
            'kind': 'knownInstance',
            'className': '',
        }

        result = mapping._resolve_type(400)
        assert isinstance(result, JavaType.Unknown)


class TestTypeAliasDescriptor:
    """Tests for the enriched typeAlias kind."""

    def test_type_alias_with_value_type(self):
        """A typeAlias with valueType should resolve to the underlying type."""
        mapping = PythonTypeMapping("", file_path=None)
        mapping._type_registry[500] = {
            'kind': 'typeAlias',
            'name': 'MyAlias',
            'valueType': 501,
        }
        mapping._type_registry[501] = {'kind': 'instance', 'className': 'int'}

        result = mapping._resolve_type(500)
        assert result == JavaType.Primitive.Int

    def test_type_alias_without_value_type(self):
        """A typeAlias without valueType should fall back to class from name."""
        mapping = PythonTypeMapping("", file_path=None)
        mapping._type_registry[500] = {
            'kind': 'typeAlias',
            'name': 'MyAlias',
        }

        result = mapping._resolve_type(500)
        assert isinstance(result, JavaType.Class)
        assert result._fully_qualified_name == 'MyAlias'

    def test_type_alias_with_class_value_type(self):
        """A typeAlias pointing to a class should resolve to that class."""
        mapping = PythonTypeMapping("", file_path=None)
        mapping._type_registry[500] = {
            'kind': 'typeAlias',
            'name': 'StringList',
            'valueType': 501,
            'typeParameters': [],
        }
        mapping._type_registry[501] = {
            'kind': 'instance',
            'className': 'list',
        }

        result = mapping._resolve_type(500)
        assert isinstance(result, JavaType.Class)
        assert result._fully_qualified_name == 'list'


class TestTypeVarConstraints:
    """Tests for typeVar constraints field."""

    def test_typevar_with_constraints(self):
        """A typeVar with constraints should have them as bounds."""
        mapping = PythonTypeMapping("", file_path=None)
        mapping._type_registry[600] = {
            'kind': 'typeVar',
            'name': 'T',
            'variance': 'invariant',
            'constraints': [601, 602],
        }
        mapping._type_registry[601] = {'kind': 'instance', 'className': 'int'}
        mapping._type_registry[602] = {'kind': 'instance', 'className': 'str'}

        result = mapping._resolve_type(600)
        assert isinstance(result, JavaType.GenericTypeVariable)
        assert result.name == 'T'
        assert len(result.bounds) == 2
        assert result.bounds[0] is JavaType.Primitive.Int
        assert result.bounds[1] is JavaType.Primitive.String

    def test_typevar_upper_bound_takes_precedence_over_constraints(self):
        """upperBound should be used instead of constraints when both present."""
        mapping = PythonTypeMapping("", file_path=None)
        mapping._type_registry[600] = {
            'kind': 'typeVar',
            'name': 'T',
            'variance': 'invariant',
            'upperBound': 601,
            'constraints': [602, 603],
        }
        mapping._type_registry[601] = {'kind': 'instance', 'className': 'float'}
        mapping._type_registry[602] = {'kind': 'instance', 'className': 'int'}
        mapping._type_registry[603] = {'kind': 'instance', 'className': 'str'}

        result = mapping._resolve_type(600)
        assert isinstance(result, JavaType.GenericTypeVariable)
        assert len(result.bounds) == 1
        assert result.bounds[0] is JavaType.Primitive.Double  # float -> Double


class TestBoundMethodClassName:
    """Tests for the boundMethod className field."""

    def test_bound_method_declaring_type_from_classname(self):
        """boundMethod with className should resolve declaring type."""
        mapping = PythonTypeMapping("", file_path=None)
        mapping._type_registry[700] = {
            'kind': 'boundMethod',
            'name': 'method',
            'className': 'MyClass',
            'moduleName': 'mymod',
            'parameters': [],
            'returnType': 701,
        }
        mapping._type_registry[701] = {'kind': 'instance', 'className': 'str'}

        result = mapping._declaring_type_from_descriptor(mapping._type_registry[700])
        assert result is not None
        assert isinstance(result, JavaType.Class)
        assert result._fully_qualified_name == 'mymod.MyClass'

    def test_bound_method_declaring_type_builtins_no_module(self):
        """boundMethod from builtins should use just className."""
        mapping = PythonTypeMapping("", file_path=None)
        mapping._type_registry[700] = {
            'kind': 'boundMethod',
            'name': 'upper',
            'className': 'str',
            'moduleName': 'builtins',
            'parameters': [],
            'returnType': 701,
        }
        mapping._type_registry[701] = {'kind': 'instance', 'className': 'str'}

        result = mapping._declaring_type_from_descriptor(mapping._type_registry[700])
        assert result is not None
        assert result._fully_qualified_name == 'str'


@requires_ty_types_cli
class TestNewDescriptorsWithTyTypes:
    """Integration tests for new ty-types 0.0.20 descriptor kinds using live ty-types."""

    def test_callable_annotation_resolves(self):
        """A Callable[[int], str] annotation should resolve."""
        source = '''from typing import Callable
my_func: Callable[[int], str] = str
my_func
'''
        mapping, tree, tmpdir, client = _make_mapping(source)
        try:
            name_node = tree.body[2].value  # bare 'my_func' expression
            result = mapping.type(name_node)
            # The type of my_func should be a callable — resolve to its return type (str)
            # or to a class type. Either way, not Unknown.
            assert result is not None
            assert not isinstance(result, JavaType.Unknown), \
                "Callable annotation should not resolve to Unknown"
        finally:
            _cleanup_mapping(mapping, tmpdir, client)

    def test_bound_method_has_classname_declaring_type(self):
        """Bound method on user class should produce declaring type from className."""
        source = '''class Calculator:
    def add(self, a: int, b: int) -> int:
        return a + b

c = Calculator()
c.add(1, 2)
'''
        mapping, tree, tmpdir, client = _make_mapping(source)
        try:
            call = tree.body[2].value  # c.add(1, 2)
            result = mapping.method_invocation_type(call)
            assert result is not None
            assert result._name == 'add'
            assert result._declaring_type is not None
            assert result._declaring_type._fully_qualified_name.endswith('Calculator')
        finally:
            _cleanup_mapping(mapping, tmpdir, client)

    def test_typevar_with_constraints_from_ty_types(self):
        """TypeVar with constraints should produce GenericTypeVariable with bounds."""
        source = '''from typing import TypeVar
T = TypeVar('T', int, str)
x: T
'''
        mapping, tree, tmpdir, client = _make_mapping(source)
        try:
            # Look up the type of 'x' which should be T
            name_node = tree.body[2].target  # x in 'x: T'
            result = mapping.type(name_node)
            # T should be a GenericTypeVariable
            if isinstance(result, JavaType.GenericTypeVariable):
                assert result.name == 'T'
                # With constraints, bounds should have int and str
                assert len(result.bounds) >= 1
        finally:
            _cleanup_mapping(mapping, tmpdir, client)

    def test_type_alias_resolves_through_value(self):
        """A type alias should resolve through to its value type."""
        source = '''type MyInt = int
x: MyInt = 42
x
'''
        mapping, tree, tmpdir, client = _make_mapping(source)
        try:
            name_node = tree.body[2].value  # bare 'x'
            result = mapping.type(name_node)
            # x: MyInt should resolve to int
            assert result is not None
            # Could be Primitive.Int or a Class('int') or Class('MyInt')
            # The key is it shouldn't be Unknown
            assert not isinstance(result, JavaType.Unknown), \
                "type alias should not resolve to Unknown"
        finally:
            _cleanup_mapping(mapping, tmpdir, client)

    def test_known_instance_typevar_resolves_to_class(self):
        """The TypeVar constructor name should resolve to a class, not Unknown."""
        source = '''from typing import TypeVar
TypeVar
'''
        mapping, tree, tmpdir, client = _make_mapping(source)
        try:
            name_node = tree.body[1].value  # bare 'TypeVar'
            result = mapping.type(name_node)
            assert result is not None
            assert not isinstance(result, JavaType.Unknown), \
                "TypeVar name should resolve to a Class via knownInstance"
            assert isinstance(result, JavaType.Class)
            assert 'typing' in result._fully_qualified_name
        finally:
            _cleanup_mapping(mapping, tmpdir, client)


class TestDeclarationDeclaringType:
    """Tests for declaring type on function declarations."""

    def test_declaration_declaring_type_no_ty_types(self):
        """Without ty-types, declaring type remains None."""
        source = 'def greet(name: str) -> str:\n    return name\n'
        tree = ast.parse(source)
        mapping = PythonTypeMapping(source)
        func_node = tree.body[0]
        result = mapping.method_declaration_type(func_node)
        assert result is not None
        assert result._declaring_type is None

    @requires_ty_types_cli
    def test_declaration_declaring_type_with_ty_types(self):
        """With ty-types, a function declaration should get a declaring type from the descriptor."""
        source = 'def greet(name: str) -> str:\n    return name\n'
        mapping, tree, tmpdir, client = _make_mapping(source)
        try:
            func_node = tree.body[0]
            result = mapping.method_declaration_type(func_node)
            assert result is not None
            assert isinstance(result, JavaType.Method)
            assert result._declaring_type is not None, \
                "Declaration should have a declaring type, not None"
            assert isinstance(result._declaring_type, JavaType.Class)
            assert result._declaring_type._fully_qualified_name != "<unknown>"
        finally:
            _cleanup_mapping(mapping, tmpdir, client)


class TestCyclicTypeResolution:
    """Tests that FQN-deduplicated types don't produce self-referential supertypes.

    Python's namedtuple pattern ``class Pair(namedtuple('Pair', ...))`` creates
    two types with the same FQN. The type mapping deduplicates by FQN, which can
    cause the single JavaType.Class object to have itself as its own supertype.
    """

    def test_namedtuple_subclass_no_self_supertype(self):
        """A class extending a namedtuple with the same name must not have
        itself as its own supertype.

        This is the pattern from importlib_metadata._collections.Pair that
        caused StackOverflowError in TypeUtils.isAssignableTo() on Moderne.
        """
        source = 'x = 1'
        mapping = PythonTypeMapping(source)

        # Simulate what ty-types produces for:
        #   class Pair(collections.namedtuple('Pair', 'name value')): ...
        # Two classLiteral descriptors with the same FQN but different type_ids.
        # type_id 1: the namedtuple-generated Pair (no supertypes of its own here)
        # type_id 2: the class Pair, whose supertype is type_id 1
        mapping._type_registry[1] = {
            'kind': 'classLiteral',
            'className': 'Pair',
            'moduleName': 'mymodule',
            'supertypes': [],
        }
        mapping._type_registry[2] = {
            'kind': 'classLiteral',
            'className': 'Pair',
            'moduleName': 'mymodule',
            'supertypes': [1],
        }

        type_pair = mapping._resolve_type(2)

        assert type_pair is not None
        assert isinstance(type_pair, JavaType.FullyQualified)
        assert type_pair.fully_qualified_name == 'mymodule.Pair'

        # The supertype must NOT be itself
        supertype = getattr(type_pair, '_supertype', None)
        assert supertype is not type_pair, \
            "Pair has itself as its own supertype — would cause StackOverflowError in Java"


def _parse_with_types(files: dict, main_filename: str = 'm.py'):
    """Parse ``main_filename`` with type attribution enabled.

    Sibling modules in ``files`` are written into the same workspace so ty can
    resolve cross-module references. Returns ``(cu, tmpdir, client)``; the
    caller must call :func:`_cleanup_parse`.
    """
    tmpdir = tempfile.mkdtemp()
    for name, src in files.items():
        with open(os.path.join(tmpdir, name), 'w') as f:
            f.write(src)
    client = TyTypesClient()
    client.initialize(tmpdir)
    main_src = files[main_filename]
    main_path = os.path.join(tmpdir, main_filename)
    cu = ParserVisitor(main_src, main_path, client).visit_Module(ast.parse(main_src))
    return cu, tmpdir, client


def _cleanup_parse(tmpdir, client):
    """Helper: tear down resources from :func:`_parse_with_types`."""
    client.shutdown()
    import shutil
    shutil.rmtree(tmpdir, ignore_errors=True)


def _collect_method_invocations(cu) -> list:
    """Collect all J.MethodInvocation nodes in a parsed tree."""
    found: list = []

    class _Collector(PythonVisitor):
        def visit_method_invocation(self, mi, p):
            found.append(mi)
            return super().visit_method_invocation(mi, p)

    _Collector().visit(cu, None)
    return found


def _collect_class_declarations(cu) -> dict:
    """Collect J.ClassDeclaration nodes in a parsed tree, keyed by simple name."""
    found: dict = {}

    class _Collector(PythonVisitor):
        def visit_class_declaration(self, cd, p):
            found[cd.name.simple_name] = cd
            return super().visit_class_declaration(cd, p)

    _Collector().visit(cu, None)
    return found


@requires_ty_types_cli
class TestMethodInvocationResultType:
    """J.MethodInvocation.type must reflect the callee's return type.

    Mirrors the Java contract where ``J.MethodInvocation.getType()`` returns
    ``methodType == null ? null : methodType.getReturnType()``. Without this,
    receivers like ``get_user().attr`` are untyped and recipes cannot reason
    about the result of a call.
    """

    def test_method_invocation_type_is_return_type(self):
        src = (
            "class Widget:\n"
            "    pass\n"
            "\n"
            "def make_widget() -> Widget:\n"
            "    return Widget()\n"
            "\n"
            "w = make_widget()\n"
        )
        cu, tmpdir, client = _parse_with_types({'m.py': src})
        try:
            make = [c for c in _collect_method_invocations(cu)
                    if c.name.simple_name == 'make_widget']
            assert make, "expected a make_widget() invocation in the tree"
            t = make[0].type
            assert t is not None, \
                "MethodInvocation.type must reflect the method's return type, not None"
            assert isinstance(t, JavaType.Class)
            assert t.fully_qualified_name == 'm.Widget'
        finally:
            _cleanup_parse(tmpdir, client)

    def test_method_invocation_type_matches_method_return_type(self):
        """The derived ``type`` equals the underlying ``method_type.return_type``."""
        src = (
            "class Widget:\n"
            "    pass\n"
            "\n"
            "def make_widget() -> Widget:\n"
            "    return Widget()\n"
            "\n"
            "w = make_widget()\n"
        )
        cu, tmpdir, client = _parse_with_types({'m.py': src})
        try:
            make = [c for c in _collect_method_invocations(cu)
                    if c.name.simple_name == 'make_widget']
            assert make
            mi = make[0]
            assert mi.method_type is not None
            assert mi.type is mi.method_type.return_type
        finally:
            _cleanup_parse(tmpdir, client)


@requires_ty_types_cli
class TestSupertypeChainResolution:
    """A subclass chain must be walkable across module boundaries.

    ty-types (>= 0.0.39) surfaces a base class as a classLiteral with its real
    fully-qualified name even when the base lives in another module, so
    ``ClassDeclaration.type`` carries a resolved ``_supertype`` chain rather
    than dead-ending at ``JavaType.Unknown``.
    """

    def test_cross_module_supertype_chain(self):
        base = "class Base:\n    pass\n"
        main = (
            "from basemod import Base\n"
            "\n"
            "class Mid(Base):\n"
            "    pass\n"
            "\n"
            "class Leaf(Mid):\n"
            "    pass\n"
        )
        cu, tmpdir, client = _parse_with_types(
            {'basemod.py': base, 'm.py': main}, main_filename='m.py')
        try:
            classes = _collect_class_declarations(cu)

            mid = classes['Mid'].type
            assert isinstance(mid, JavaType.Class)
            assert isinstance(mid._supertype, JavaType.FullyQualified)
            assert not isinstance(mid._supertype, JavaType.Unknown), \
                "cross-module base must resolve to a real class, not Unknown"
            assert mid._supertype.fully_qualified_name == 'basemod.Base'

            leaf = classes['Leaf'].type
            assert isinstance(leaf, JavaType.Class)
            assert leaf._supertype.fully_qualified_name == 'm.Mid'
            # Transitive: Leaf -> Mid -> Base, crossing the module boundary.
            grand = leaf._supertype._supertype
            assert grand is not None
            assert grand.fully_qualified_name == 'basemod.Base'
        finally:
            _cleanup_parse(tmpdir, client)


class TestSubprocessEnvironment:
    """TyTypesClient must point ty-types at the running interpreter's
    environment so third-party imports (and their supertypes) resolve, rather
    than relying on an activated venv being present in the ambient environment.
    """

    def test_injects_virtualenv_when_in_venv_and_unset(self):
        env = TyTypesClient._subprocess_env(
            base_env={'PATH': '/usr/bin'},
            prefix='/proj/.venv',
            base_prefix='/usr',
        )
        assert env['VIRTUAL_ENV'] == '/proj/.venv'
        # The venv's bin dir is prepended so ty discovers the right interpreter.
        assert env['PATH'].split(os.pathsep)[0] == os.path.join('/proj/.venv', 'bin')
        assert '/usr/bin' in env['PATH']

    def test_respects_existing_virtualenv(self):
        env = TyTypesClient._subprocess_env(
            base_env={'VIRTUAL_ENV': '/already/set', 'PATH': '/usr/bin'},
            prefix='/proj/.venv',
            base_prefix='/usr',
        )
        # An explicitly chosen environment must never be overridden.
        assert env['VIRTUAL_ENV'] == '/already/set'
        assert env['PATH'] == '/usr/bin'

    def test_no_injection_outside_venv(self):
        env = TyTypesClient._subprocess_env(
            base_env={'PATH': '/usr/bin'},
            prefix='/usr',
            base_prefix='/usr',
        )
        # Not a virtual environment — leave discovery to ty's defaults.
        assert 'VIRTUAL_ENV' not in env
        assert env['PATH'] == '/usr/bin'


@requires_ty_types_cli
class TestMethodDeclarationResultType:
    """J.MethodDeclaration.type must reflect the declared return type
    (mirrors Java J.MethodDeclaration.getType() == methodType.returnType)."""

    def test_method_declaration_type_is_return_type(self):
        src = (
            "class Widget:\n"
            "    pass\n"
            "\n"
            "def make_widget() -> Widget:\n"
            "    return Widget()\n"
        )
        cu, tmpdir, client = _parse_with_types({'m.py': src})
        try:
            decls = []

            class _Collector(PythonVisitor):
                def visit_method_declaration(self, md, p):
                    decls.append(md)
                    return super().visit_method_declaration(md, p)

            _Collector().visit(cu, None)
            make = [d for d in decls if d.name.simple_name == 'make_widget']
            assert make, "expected a make_widget() declaration in the tree"
            md = make[0]
            assert md.method_type is not None
            t = md.type
            assert t is not None, \
                "MethodDeclaration.type must reflect the declared return type, not None"
            assert isinstance(t, JavaType.Class)
            assert t.fully_qualified_name == 'm.Widget'
            assert t is md.method_type.return_type
        finally:
            _cleanup_parse(tmpdir, client)


class TestSubclassOfDescriptor:
    """Unit tests for the ty-types `subclassOf` descriptor kind (``type[X]``).

    ty surfaces a *class object* ``type[X]`` as a ``subclassOf`` descriptor whose
    ``base`` is ``X``. A value of this type is the class ``X`` itself, NOT an
    instance of ``X``, so it must resolve to a representation distinct from an
    instance of ``X``: a ``JavaType.Parameterized`` over ``type`` carrying ``X``
    as its sole type parameter (mirroring how ``list[X]`` is modelled). This
    keeps ``is_assignable_to(X, type[X])`` False while leaving the wrapped class
    recoverable through ``type_parameters[0]``.

    Use mock descriptors so the logic is exercised without the ty-types CLI.
    End-to-end tests against real source live in TestClassObjectTypeAttribution.
    """

    def test_subclass_of_resolves_to_parameterized_type_over_base(self):
        """``type[M]`` resolves to ``Parameterized`` over ``type`` with ``M``."""
        mapping = PythonTypeMapping("", file_path=None)
        mapping._type_registry[5] = {
            'kind': 'classLiteral', 'className': 'M', 'moduleName': 'mymod',
        }
        mapping._type_registry[10] = {
            'kind': 'subclassOf', 'display': 'type[M]', 'base': 5,
        }
        result = mapping._resolve_type(10)
        assert isinstance(result, JavaType.Parameterized)
        assert result.type.fully_qualified_name == 'type'
        assert result.type_parameters is not None
        assert len(result.type_parameters) == 1
        wrapped = result.type_parameters[0]
        assert isinstance(wrapped, JavaType.FullyQualified)
        assert wrapped.fully_qualified_name == 'mymod.M'

    def test_subclass_of_not_assignable_to_base_instance(self):
        """A ``type[M]`` value is NOT assignable to the instance type ``M``."""
        from rewrite.python.type_utils import is_assignable_to
        mapping = PythonTypeMapping("", file_path=None)
        mapping._type_registry[5] = {
            'kind': 'classLiteral', 'className': 'M', 'moduleName': 'mymod',
        }
        mapping._type_registry[10] = {'kind': 'subclassOf', 'base': 5}
        result = mapping._resolve_type(10)
        assert not is_assignable_to('mymod.M', result)

    def test_subclass_of_assignable_to_type(self):
        """A ``type[M]`` value IS assignable to ``type``."""
        from rewrite.python.type_utils import is_assignable_to
        mapping = PythonTypeMapping("", file_path=None)
        mapping._type_registry[5] = {
            'kind': 'classLiteral', 'className': 'M', 'moduleName': 'mymod',
        }
        mapping._type_registry[10] = {'kind': 'subclassOf', 'base': 5}
        result = mapping._resolve_type(10)
        assert is_assignable_to('type', result)

    def test_instance_is_assignable_to_base(self):
        """Contrast: an *instance* of ``M`` stays assignable to ``M``."""
        from rewrite.python.type_utils import is_assignable_to
        mapping = PythonTypeMapping("", file_path=None)
        mapping._type_registry[5] = {
            'kind': 'classLiteral', 'className': 'M', 'moduleName': 'mymod',
        }
        mapping._type_registry[6] = {
            'kind': 'instance', 'className': 'M', 'moduleName': 'mymod', 'classId': 5,
        }
        result = mapping._resolve_type(6)
        assert is_assignable_to('mymod.M', result)

    def test_subclass_of_without_base_is_unknown(self):
        """A ``subclassOf`` descriptor lacking ``base`` degrades to Unknown."""
        mapping = PythonTypeMapping("", file_path=None)
        mapping._type_registry[12] = {'kind': 'subclassOf', 'display': 'type[?]'}
        result = mapping._resolve_type(12)
        assert isinstance(result, JavaType.Unknown)

    def test_subclass_of_declaring_type_resolves_to_base(self):
        """The declaring-type path resolves ``type[M]`` through to ``M``.

        A classmethod/attribute reached through a ``type[M]`` value is declared
        on ``M`` (or its metaclass), so the declaring type must remain ``M`` — a
        ``FullyQualified``, not the ``type`` wrapper.
        """
        mapping = PythonTypeMapping("", file_path=None)
        mapping._type_registry[5] = {
            'kind': 'instance', 'className': 'M', 'moduleName': 'mymod',
        }
        mapping._type_registry[10] = {'kind': 'subclassOf', 'base': 5}
        result = mapping._resolve_declaring_type(10)
        assert result is not None
        assert result.fully_qualified_name == 'mymod.M'


class TestTypeFormDescriptor:
    """Unit tests for the ty-types 0.0.44 `typeForm` descriptor kind.

    PEP 747 `TypeForm[T]` values (a type expression used as a runtime value,
    e.g. ``x: TypeForm[str] = str``) are surfaced by ty-types >= 0.0.44 as a
    first-class ``typeForm`` descriptor carrying a ``typeArgument`` reference,
    rather than the older ``other`` fallback. Like ``subclassOf`` (``type[X]``),
    a ``TypeForm[T]`` value is the *type* ``T`` used as a value, not an instance
    of ``T``, so it resolves to a class-object representation: a
    ``Parameterized`` over ``type`` wrapping ``T`` (the wrapped argument stays
    recoverable through ``type_parameters[0]``).

    Use mock descriptors so the logic is exercised without the ty-types CLI.
    End-to-end tests against real source live in TestTypeFormIntegration below.
    """

    def test_type_form_resolves_to_class_object_over_primitive_argument(self):
        """`TypeForm[str]` resolves to a class object wrapping its argument (str)."""
        mapping = PythonTypeMapping("", file_path=None)
        mapping._type_registry[3] = {'kind': 'instance', 'className': 'str'}
        mapping._type_registry[10] = {
            'kind': 'typeForm', 'display': 'TypeForm[str]', 'typeArgument': 3,
        }
        result = mapping._resolve_type(10)
        assert isinstance(result, JavaType.Parameterized)
        assert result.type.fully_qualified_name == 'type'
        assert result.type_parameters == [JavaType.Primitive.String]

    def test_type_form_resolves_to_class_object_over_class_argument(self):
        """`TypeForm[MyClass]` wraps the resolved class type as a class object."""
        mapping = PythonTypeMapping("", file_path=None)
        mapping._type_registry[5] = {
            'kind': 'classLiteral', 'className': 'MyClass', 'moduleName': 'mymod',
        }
        mapping._type_registry[11] = {
            'kind': 'typeForm', 'display': 'TypeForm[MyClass]', 'typeArgument': 5,
        }
        result = mapping._resolve_type(11)
        assert isinstance(result, JavaType.Parameterized)
        assert result.type.fully_qualified_name == 'type'
        assert result.type_parameters is not None
        assert result.type_parameters[0].fully_qualified_name == 'mymod.MyClass'

    def test_type_form_declaring_type_resolves_to_type_argument(self):
        """Declaring-type resolution unwraps `typeForm` to its argument too,
        mirroring `subclassOf`."""
        mapping = PythonTypeMapping("", file_path=None)
        mapping._type_registry[5] = {
            'kind': 'instance', 'className': 'MyClass', 'moduleName': 'mymod',
        }
        mapping._type_registry[11] = {
            'kind': 'typeForm', 'display': 'TypeForm[MyClass]', 'typeArgument': 5,
        }
        result = mapping._resolve_declaring_type(11)
        assert result is not None
        assert result.fully_qualified_name == 'mymod.MyClass'

    def test_type_form_without_type_argument_is_unknown(self):
        """A `typeForm` descriptor lacking `typeArgument` degrades to Unknown
        rather than raising."""
        mapping = PythonTypeMapping("", file_path=None)
        mapping._type_registry[12] = {'kind': 'typeForm', 'display': 'TypeForm[?]'}
        result = mapping._resolve_type(12)
        assert isinstance(result, JavaType.Unknown)


@requires_ty_types_cli
class TestTypeFormIntegration:
    """End-to-end tests exercising ty-types 0.0.44 TypeForm output.

    Requires ty-types >= 0.0.44, which surfaces PEP 747 `TypeForm[T]` as a
    first-class `typeForm` descriptor whose `typeArgument` is the wrapped type
    (for `TypeForm[str]`, an `instance` of `str`). Without the `typeForm`
    handling in type_mapping this regresses to Unknown on 0.0.44; with it, the
    reference resolves to `str`. CLIs predating the TypeForm variant infer the
    reference as the `str` *class* instead, so this assertion is version-gated
    (same contract as the ParamSpec/Concatenate integration tests).
    """

    def test_type_form_value_resolves_to_class_object_over_argument(self):
        """Reading a `TypeForm[str]` value resolves to a class object wrapping `str`."""
        source = '''from typing_extensions import TypeForm
string_form: TypeForm[str] = str
ref = string_form
'''
        mapping, tree, tmpdir, client = _make_mapping(source)
        try:
            ref = tree.body[2].value  # `string_form` on the RHS of `ref = string_form`
            result = mapping.type(ref)
            assert isinstance(result, JavaType.Parameterized)
            assert result.type.fully_qualified_name == 'type'
            assert result.type_parameters == [JavaType.Primitive.String]
        finally:
            _cleanup_mapping(mapping, tmpdir, client)


@requires_ty_types_cli
class TestClassObjectTypeAttribution:
    """End-to-end: a ``type[X]`` class object must be distinct from an instance.

    A parameter ``c: type[M]`` is a *class object*; a parameter ``inst: M`` is an
    *instance*. Before the fix both resolved to the identical ``JavaType.Class``
    for ``M``, so recipes could not tell class access from instance access (e.g.
    ``ReplaceModelFieldsInstanceAccess`` rewrote class access ``settings_cls.``
    ``model_fields`` as if it were an instance). After the fix only the instance
    is assignable to ``M``; the class object is assignable to ``type`` instead,
    with ``M`` still recoverable.

    First-party only — no third-party dependency environment required.
    """

    _SOURCE = (
        "class M:\n"
        "    x: int = 0\n"
        "\n"
        "def via_type_param(c: type[M]):\n"
        "    return c\n"
        "\n"
        "def via_instance_param(inst: M):\n"
        "    return inst\n"
    )

    @staticmethod
    def _collect_ident_types(cu, name):
        from rewrite.java.tree import Identifier
        collected = []

        class _Collector(PythonVisitor):
            def visit_identifier(self, ident: Identifier, p):
                if ident.simple_name == name and ident.type is not None:
                    collected.append(ident.type)
                return super().visit_identifier(ident, p)

        _Collector().visit(cu, None)
        return collected

    def test_type_param_is_class_object_not_instance(self):
        from rewrite.python.type_utils import is_assignable_to
        cu, tmpdir, client = _parse_with_types({'m.py': self._SOURCE}, 'm.py')
        try:
            c_types = self._collect_ident_types(cu, 'c')
            inst_types = self._collect_ident_types(cu, 'inst')
            assert c_types, "no typed `c` identifier found in parsed LST"
            assert inst_types, "no typed `inst` identifier found in parsed LST"

            # The instance parameter IS assignable to M.
            assert all(is_assignable_to('m.M', t) for t in inst_types), \
                "an instance of M must be assignable to M"
            # The class-object parameter is NOT assignable to the instance type M …
            assert not any(is_assignable_to('m.M', t) for t in c_types), \
                "a type[M] class object must NOT be assignable to instance type M"
            # … but IS assignable to `type`.
            assert all(is_assignable_to('type', t) for t in c_types), \
                "a type[M] class object must be assignable to `type`"
        finally:
            _cleanup_parse(tmpdir, client)

    def test_type_param_wrapped_class_recoverable(self):
        cu, tmpdir, client = _parse_with_types({'m.py': self._SOURCE}, 'm.py')
        try:
            c_types = self._collect_ident_types(cu, 'c')
            param = next((t for t in c_types if isinstance(t, JavaType.Parameterized)), None)
            assert param is not None, "type[M] must resolve to a Parameterized type"
            assert param.type.fully_qualified_name == 'type'
            assert param.type_parameters and len(param.type_parameters) == 1
            assert param.type_parameters[0].fully_qualified_name == 'm.M'
        finally:
            _cleanup_parse(tmpdir, client)

    def test_classmethod_reached_through_type_param_resolves(self):
        """A classmethod reached via a ``type[M]`` value resolves to ``M``.

        Requirement 2: the wrapped class stays usable — the declaring-type path
        attributes ``c.make()`` to a method declared on ``M``, not on ``type``.
        """
        source = (
            "class M:\n"
            "    @classmethod\n"
            "    def make(cls) -> 'M':\n"
            "        return cls()\n"
            "\n"
            "def use(c: type[M]):\n"
            "    return c.make()\n"
        )
        cu, tmpdir, client = _parse_with_types({'m.py': source}, 'm.py')
        try:
            calls = [mi for mi in _collect_method_invocations(cu)
                     if mi.name.simple_name == 'make']
            assert calls, "expected a c.make() invocation in the tree"
            mt = calls[0].method_type
            assert mt is not None, \
                "a classmethod reached through type[M] must still resolve"
            assert mt.declaring_type is not None
            # Declared on the class M (its FQN), not on the `type` wrapper.
            fqn = mt.declaring_type.fully_qualified_name
            assert fqn in ('m.M', 'M'), fqn
            assert fqn != 'type'
        finally:
            _cleanup_parse(tmpdir, client)

    def test_classmethod_cls_param_unchanged(self):
        """The implicit ``cls`` of a @classmethod stays a class object.

        ty already treats ``cls`` as a class object (not assignable to the
        instance type); the fix must not regress that to instance assignability.
        """
        from rewrite.python.type_utils import is_assignable_to
        source = (
            "class M:\n"
            "    @classmethod\n"
            "    def make(cls):\n"
            "        return cls\n"
        )
        cu, tmpdir, client = _parse_with_types({'m.py': source}, 'm.py')
        try:
            cls_types = self._collect_ident_types(cu, 'cls')
            assert cls_types, "no typed `cls` identifier found in parsed LST"
            assert not any(is_assignable_to('m.M', t) for t in cls_types), \
                "the classmethod `cls` is a class object, not an instance of M"
        finally:
            _cleanup_parse(tmpdir, client)


def _uv_available() -> bool:
    import shutil
    return shutil.which('uv') is not None


requires_uv = pytest.mark.skipif(
    not _uv_available(),
    reason="uv is not installed (needed to build a dependency workspace)"
)


class TestSubprocessEnvVirtualEnv:
    """Unit tests for TyTypesClient._subprocess_env explicit virtual_env handling.

    The normal CLI parse path resolves a project's third-party dependencies into
    a cached workspace venv and points ty-types at it via ``virtual_env``. An
    explicit ``virtual_env`` must win over both the running interpreter's
    ``sys.prefix`` fallback and any inherited ``VIRTUAL_ENV``.
    """

    def test_explicit_virtual_env_is_set_and_on_path(self):
        env = TyTypesClient._subprocess_env(
            base_env={'PATH': '/usr/bin'},
            virtual_env='/tmp/ws/.venv',
        )
        assert env['VIRTUAL_ENV'] == '/tmp/ws/.venv'
        bin_dir = os.path.join('/tmp/ws/.venv', 'Scripts' if os.name == 'nt' else 'bin')
        assert env['PATH'].split(os.pathsep)[0] == bin_dir

    def test_explicit_virtual_env_overrides_inherited(self):
        env = TyTypesClient._subprocess_env(
            base_env={'PATH': '/usr/bin', 'VIRTUAL_ENV': '/inherited/.venv'},
            virtual_env='/tmp/ws/.venv',
        )
        assert env['VIRTUAL_ENV'] == '/tmp/ws/.venv'

    def test_explicit_virtual_env_overrides_sys_prefix_fallback(self):
        # Even when the running interpreter looks like a venv (prefix != base),
        # an explicit virtual_env takes precedence over the sys.prefix fallback.
        env = TyTypesClient._subprocess_env(
            base_env={'PATH': '/usr/bin'},
            prefix='/some/dev/.venv',
            base_prefix='/usr',
            virtual_env='/tmp/ws/.venv',
        )
        assert env['VIRTUAL_ENV'] == '/tmp/ws/.venv'

    def test_no_virtual_env_keeps_existing_sys_prefix_behavior(self):
        env = TyTypesClient._subprocess_env(
            base_env={'PATH': '/usr/bin'},
            prefix='/dev/.venv',
            base_prefix='/usr',
        )
        assert env['VIRTUAL_ENV'] == '/dev/.venv'


class TestDependencyPathForwarding:
    """The parse RPC handlers forward the caller-supplied dependency path to
    ty-types via ``TyTypesClient(virtual_env=...)`` and do NOT provision a
    dependency environment themselves.

    Provisioning is the caller's responsibility — the CLI build step for a
    ``mod build``, or the test/template helper in-repo — and the path is handed
    to the handler explicitly as the ``dependencyPath`` request option. This
    keeps the handler a pure consumer with a single injection seam, and lets
    unit tests point ty-types at a workspace they built themselves (overriding
    the build convention) instead of relying on an in-handler auto-build.
    """

    _captured: list = []

    class _StubTyClient:
        def __init__(self, virtual_env=None):
            TestDependencyPathForwarding._captured.append(virtual_env)

        def initialize(self, project_root):
            return True

        @property
        def is_available(self):
            # False so PythonTypeMapping skips real type lookups: we only care
            # which virtual_env the handler constructed the client with.
            return False

        def get_types(self, *args, **kwargs):
            return None

        def shutdown(self):
            pass

    @pytest.fixture(autouse=True)
    def _patch_client(self, monkeypatch):
        import rewrite.python.ty_client as ty_mod
        TestDependencyPathForwarding._captured = []
        monkeypatch.setattr(ty_mod, 'TyTypesClient',
                            TestDependencyPathForwarding._StubTyClient)
        yield

    def test_handle_parse_forwards_dependency_path(self, tmp_path):
        from rewrite.rpc import server
        (tmp_path / "m.py").write_text("x = 1\n")
        server.handle_parse({
            "inputs": [str(tmp_path / "m.py")],
            "relativeTo": str(tmp_path),
            "dependencyPath": "/deps/proj/.venv",
        })
        assert self._captured == ["/deps/proj/.venv"]

    def test_handle_parse_project_forwards_dependency_path(self, tmp_path):
        from rewrite.rpc import server
        (tmp_path / "m.py").write_text("x = 1\n")
        server.handle_parse_project({
            "projectPath": str(tmp_path),
            "dependencyPath": "/deps/proj/.venv",
        })
        assert self._captured == ["/deps/proj/.venv"]

    def test_handle_parse_without_dependency_path_does_not_auto_provision(self, tmp_path, monkeypatch):
        # A pyproject.toml with dependencies present must NOT trigger an
        # in-handler dependency build when no dependencyPath is forwarded.
        # Stub the workspace builder so that, if the handler ever reached for it,
        # we'd capture a non-None venv (failing the assert) — without running uv.
        import importlib
        from rewrite.rpc import server
        autows = tmp_path / "autows"
        (autows / ".venv").mkdir(parents=True)
        dw_mod = importlib.import_module("rewrite.python.template.dependency_workspace")
        monkeypatch.setattr(
            dw_mod.DependencyWorkspace,
            "get_or_create_from_pyproject",
            staticmethod(lambda content: str(autows)),
        )
        (tmp_path / "pyproject.toml").write_text(
            '[project]\nname = "x"\nversion = "0"\ndependencies = ["pydantic"]\n')
        (tmp_path / "m.py").write_text("x = 1\n")
        server.handle_parse({
            "inputs": [str(tmp_path / "m.py")],
            "relativeTo": str(tmp_path),
        })
        assert self._captured == [None]


@requires_ty_types_cli
@requires_uv
class TestExternalSupertypeResolutionInParsePath:
    """The normal parse path must resolve first-party classes' supertypes that
    come from installed third-party dependencies (e.g. ``class User(BaseModel)``
    where ``BaseModel`` is ``pydantic``), even when the interpreter running the
    parse does NOT have those dependencies installed.

    This is the CLI ``mod build`` scenario: the RPC server runs on an
    interpreter that has only ``openrewrite`` + ``ty-types`` installed (not the
    project's deps), and the project directory has no ``.venv``. The parse path
    must build a dependency workspace from the project's ``pyproject.toml`` so
    ty-types can resolve ``pydantic`` and surface ``User``'s supertype.

    NOTE: this test is only meaningful when ``pydantic`` is NOT importable in
    the test interpreter; otherwise ty-types would resolve it ambiently.
    """

    _SOURCE = (
        "from pydantic import BaseModel\n"
        "\n"
        "\n"
        "class User(BaseModel):\n"
        "    name: str\n"
        "\n"
        "    def field_names(self):\n"
        "        return list(self.model_fields.keys())\n"
    )
    _PYPROJECT = (
        "[project]\n"
        'name = "tyrepro"\n'
        'version = "0.0.0"\n'
        'requires-python = ">=3.10"\n'
        'dependencies = ["pydantic>=2.11"]\n'
    )

    @pytest.fixture(autouse=True)
    def _skip_if_pydantic_ambient(self):
        try:
            import pydantic  # noqa: F401
            pytest.skip("pydantic is importable in the test interpreter; "
                        "ty-types would resolve it ambiently, masking the fix")
        except ImportError:
            pass

    def _dependency_venv(self):
        # The caller (here, the test; in production, the CLI build step) builds
        # the dependency environment and forwards its path. The handler itself
        # never provisions.
        from rewrite.python.template.dependency_workspace import DependencyWorkspace
        workspace = DependencyWorkspace.get_or_create_from_pyproject(self._PYPROJECT)
        return os.path.join(workspace, ".venv")

    def _parse_project(self, tmp_path, dependency_path):
        from rewrite.rpc import server
        proj = tmp_path / "proj"
        proj.mkdir()
        (proj / "pyproject.toml").write_text(self._PYPROJECT)
        (proj / "models.py").write_text(self._SOURCE)
        params = {
            "inputs": [str(proj / "models.py")],
            "relativeTo": str(proj),
        }
        if dependency_path is not None:
            params["dependencyPath"] = dependency_path
        ids = server.handle_parse(params)
        assert ids, "handle_parse returned no results"
        return server.local_objects[ids[0]]

    def _collect_self_types(self, cu):
        from rewrite.java.tree import Identifier
        collected = []

        class _Collector(PythonVisitor):
            def visit_identifier(self, ident: Identifier, p):
                if ident.simple_name == 'self':
                    collected.append(ident.type)
                return super().visit_identifier(ident, p)

        _Collector().visit(cu, None)
        return collected

    def test_self_receiver_is_assignable_to_basemodel(self, tmp_path):
        from rewrite.python.type_utils import is_assignable_to
        cu = self._parse_project(tmp_path, self._dependency_venv())
        self_types = self._collect_self_types(cu)
        assert self_types, "no `self` identifiers found in parsed LST"
        assert any(
            is_assignable_to("pydantic.main.BaseModel", t) for t in self_types
        ), (
            "the `self` receiver's type does not resolve as a subclass of "
            "pydantic.main.BaseModel; the first-party class supertype was not "
            "populated from the forwarded dependency environment"
        )

    def test_without_dependency_path_supertype_is_not_resolved(self, tmp_path):
        # No dependency path forwarded → the handler must not provision pydantic
        # itself, so ty-types cannot resolve the external base class and the
        # `self` receiver does not resolve as a BaseModel subclass.
        from rewrite.python.type_utils import is_assignable_to
        cu = self._parse_project(tmp_path, None)
        self_types = self._collect_self_types(cu)
        assert self_types, "no `self` identifiers found in parsed LST"
        assert not any(
            is_assignable_to("pydantic.main.BaseModel", t) for t in self_types
        ), (
            "without a forwarded dependency path the parser must not auto-provision "
            "pydantic, so the supertype must not resolve to BaseModel"
        )


@requires_ty_types_cli
@requires_uv
class TestProjectParseSupertypeAcrossFiles:
    """Multi-file project parse path, exactly as the CLI's ``mod build`` uses it.

    ``mod build`` calls ``rpc.parseProject`` -> ``handle_parse_project``, which
    parses every ``.py`` file in the project through a SINGLE shared ty-types
    session. ty's ``--serve`` session emits each type descriptor only once per
    session, so a type first seen in an earlier file (e.g. ``pydantic.BaseModel``)
    is not re-sent in a later file's ``getTypes`` response. Because the parser
    builds a fresh per-file type registry, a first-party class in any file but the
    first loses its third-party supertype, and ``self`` stops resolving as a
    ``BaseModel`` subclass.

    Two peer model files make the failure order-independent: whichever file ty
    processes second drops its base, so requiring BOTH to resolve fails regardless
    of the directory walk order. This is the gap that single-file ``handle_parse``
    tests (see ``TestExternalSupertypeResolutionInParsePath``) cannot catch,
    because a fresh session per parse never triggers the dedup.
    """

    _PYPROJECT = (
        "[project]\n"
        'name = "tyrepro"\n'
        'version = "0.0.0"\n'
        'requires-python = ">=3.10"\n'
        'dependencies = ["pydantic>=2.11"]\n'
    )
    _A = (
        "from pydantic import BaseModel\n"
        "\n"
        "\n"
        "class A(BaseModel):\n"
        "    x: int\n"
        "\n"
        "    def fa(self):\n"
        "        return self.model_fields\n"
    )
    _B = (
        "from pydantic import BaseModel\n"
        "\n"
        "\n"
        "class B(BaseModel):\n"
        "    y: int\n"
        "\n"
        "    def fb(self):\n"
        "        return self.model_fields\n"
    )

    @pytest.fixture(autouse=True)
    def _skip_if_pydantic_ambient(self):
        try:
            import pydantic  # noqa: F401
            pytest.skip("pydantic is importable in the test interpreter; "
                        "ty-types would resolve it ambiently, masking the bug")
        except ImportError:
            pass

    def _dependency_venv(self):
        from rewrite.python.template.dependency_workspace import DependencyWorkspace
        workspace = DependencyWorkspace.get_or_create_from_pyproject(self._PYPROJECT)
        return os.path.join(workspace, ".venv")

    def _self_types(self, cu):
        from rewrite.java.tree import Identifier
        collected = []

        class _Collector(PythonVisitor):
            def visit_identifier(self, ident: Identifier, p):
                if ident.simple_name == 'self':
                    collected.append(ident.type)
                return super().visit_identifier(ident, p)

        _Collector().visit(cu, None)
        return collected

    def test_supertype_resolves_in_every_file_of_a_project(self, tmp_path):
        from rewrite.rpc import server
        from rewrite.python.type_utils import is_assignable_to

        proj = tmp_path / "proj"
        proj.mkdir()
        (proj / "pyproject.toml").write_text(self._PYPROJECT)
        (proj / "a.py").write_text(self._A)
        (proj / "b.py").write_text(self._B)

        items = server.handle_parse_project({
            "projectPath": str(proj),
            "relativeTo": str(proj),
            "dependencyPath": self._dependency_venv(),
        })
        assert items, "handle_parse_project returned no results"

        by_name = {}
        for it in items:
            obj = server.local_objects[it["id"]]
            sp = getattr(obj, "source_path", None)
            if sp:
                by_name[os.path.basename(str(sp))] = obj

        for fname in ("a.py", "b.py"):
            cu = by_name.get(fname)
            assert cu is not None, f"{fname} missing from parse results"
            self_types = self._self_types(cu)
            assert self_types, f"no `self` identifiers found in {fname}"
            assert all(
                is_assignable_to("pydantic.main.BaseModel", t) for t in self_types
            ), (
                f"`self` in {fname} did not resolve as a pydantic.main.BaseModel "
                "subclass; the shared ty-types session dropped the supertype "
                "descriptor for a file parsed after the first"
            )

@requires_ty_types_cli
class TestClassMembers:
    """``JavaType.Class.members`` must be populated with a class's attributes
    (annotated class/instance variables), not just its methods.

    Before this, the ``classLiteral`` branch of the type mapping only emitted
    ``JavaType.Method`` for function-kind members and silently dropped every
    attribute, so attribute-rich classes (dataclasses, pydantic/ORM models)
    came back with ``getMembers()`` empty and recipes that reason about fields
    had nothing to work with.

    The class type is obtained via a method invocation's ``declaring_type``,
    which the ``classLiteral`` branch builds (and enriches with members).
    """

    def _class_type(self, source: str):
        """Parse ``source`` (whose last statement is a method call on an
        instance) and return the callee's ``declaring_type`` — the populated
        ``JavaType.Class`` — along with cleanup handles."""
        mapping, tree, tmpdir, client = _make_mapping(source)
        call = tree.body[-1].value
        result = mapping.method_invocation_type(call)
        assert result is not None and isinstance(result._declaring_type, JavaType.Class)
        return result._declaring_type, mapping, tmpdir, client

    def test_dataclass_members_populated(self):
        src = (
            "from dataclasses import dataclass\n"
            "\n"
            "\n"
            "@dataclass\n"
            "class Point:\n"
            "    x: int\n"
            "    y: int = 0\n"
            "    label: str = \"p\"\n"
            "\n"
            "    def describe(self) -> str:\n"
            "        return self.label\n"
            "\n"
            "Point(1).describe()\n"
        )
        cls, mapping, tmpdir, client = self._class_type(src)
        try:
            members = cls._members
            assert members is not None, "dataclass should expose its fields as members"
            by_name = {v._name: v for v in members}

            assert by_name['x']._type == JavaType.Primitive.Int
            assert by_name['y']._type == JavaType.Primitive.Int
            assert by_name['label']._type == JavaType.Primitive.String

            # Each member is a JavaType.Variable owned by the class.
            for v in members:
                assert isinstance(v, JavaType.Variable)
                assert v._owner is cls

            # ty emits both the declared type and the default-value literal for a
            # field; members must be de-duplicated by name.
            assert len(members) == len(by_name), \
                f"members contain duplicate names: {[v._name for v in members]}"

            # Methods must still populate (no regression).
            assert cls._methods is not None
            assert 'describe' in [m._name for m in cls._methods]
        finally:
            _cleanup_mapping(mapping, tmpdir, client)

    def test_plain_class_members_populated(self):
        src = (
            "class Config:\n"
            "    count: int = 0\n"
            "    name: str\n"
            "    ratio: float = 1.5\n"
            "\n"
            "    def label(self) -> str:\n"
            "        return self.name\n"
            "\n"
            "Config().label()\n"
        )
        cls, mapping, tmpdir, client = self._class_type(src)
        try:
            by_name = {v._name: v for v in (cls._members or [])}
            assert by_name['count']._type == JavaType.Primitive.Int
            assert by_name['name']._type == JavaType.Primitive.String
            # ty treats a `float` annotation as accepting `int` too, so the
            # declared type can come back as a union that includes Double.
            ratio_type = by_name['ratio']._type
            ratio_bounds = getattr(ratio_type, '_bounds', [ratio_type])
            assert JavaType.Primitive.Double in ratio_bounds

            # A function-kind member must not leak into the variables.
            assert 'label' not in by_name
            assert cls._methods is not None
            assert 'label' in [m._name for m in cls._methods]
        finally:
            _cleanup_mapping(mapping, tmpdir, client)

    def test_self_typed_member_does_not_hang(self):
        # A member whose declared type is the owning class must resolve without
        # infinitely recursing (the cycle guard already covers methods; members
        # must rely on the same mechanism).
        src = (
            "class Node:\n"
            "    value: int\n"
            "    next: \"Node\" = None\n"
            "\n"
            "    def get(self) -> int:\n"
            "        return self.value\n"
            "\n"
            "Node().get()\n"
        )
        cls, mapping, tmpdir, client = self._class_type(src)
        try:
            by_name = {v._name: v for v in (cls._members or [])}
            assert by_name['value']._type == JavaType.Primitive.Int

            next_type = by_name['next']._type
            assert isinstance(next_type, JavaType.Class)
            assert next_type.fully_qualified_name.endswith('Node')
        finally:
            _cleanup_mapping(mapping, tmpdir, client)


@requires_ty_types_cli
class TestTypedDictMembers:
    """A ``TypedDict`` carries its declared keys in the descriptor's ``fields``;
    those must surface as ``JavaType.Class.members`` rather than being dropped.

    ty emits a ``typedDict`` descriptor (distinct from the class's
    ``classLiteral``) as the *type of a value* annotated with the TypedDict, so
    a recipe reasoning over such a value previously saw an empty shell class.
    Pydantic-core schemas (``AnySchema``, ``ListSchema``, …) are TypedDicts and
    were the prime offenders.
    """

    def _value_type(self, source: str):
        """Parse ``source`` (whose last statement is an expression whose type is
        a TypedDict) and return ``mapping.type`` for that expression."""
        mapping, tree, tmpdir, client = _make_mapping(source)
        node = tree.body[-1].value
        result = mapping.type(node)
        return result, mapping, tmpdir, client

    def test_typed_dict_fields_are_members(self):
        src = (
            "from typing import TypedDict\n"
            "\n"
            "\n"
            "class Movie(TypedDict):\n"
            "    name: str\n"
            "    year: int\n"
            "\n"
            "movie: Movie = {\"name\": \"x\", \"year\": 2020}\n"
            "movie\n"
        )
        cls, mapping, tmpdir, client = self._value_type(src)
        try:
            assert isinstance(cls, JavaType.Class)
            assert cls.fully_qualified_name.endswith('Movie')
            by_name = {v._name: v for v in (cls._members or [])}
            assert by_name['name']._type == JavaType.Primitive.String
            assert by_name['year']._type == JavaType.Primitive.Int

            # Each member is a JavaType.Variable owned by the class.
            for v in cls._members:
                assert isinstance(v, JavaType.Variable)
                assert v._owner is cls
        finally:
            _cleanup_mapping(mapping, tmpdir, client)

    def test_self_referential_typed_dict_does_not_hang(self):
        # ty emits a TypedDict field whose type is the same TypedDict (the
        # field's typeId points back at the owning descriptor). Resolving the
        # member must lean on the shared `_resolve_type` cycle guard rather than
        # recursing forever.
        src = (
            "from typing import TypedDict\n"
            "\n"
            "\n"
            "class Tree(TypedDict):\n"
            "    label: str\n"
            "    child: \"Tree\"\n"
            "\n"
            "t: Tree = {\"label\": \"root\", \"child\": {}}\n"
            "t\n"
        )
        cls, mapping, tmpdir, client = self._value_type(src)
        try:
            assert isinstance(cls, JavaType.Class)
            by_name = {v._name: v for v in (cls._members or [])}
            assert by_name['label']._type == JavaType.Primitive.String

            child_type = by_name['child']._type
            assert isinstance(child_type, JavaType.Class)
            assert child_type.fully_qualified_name.endswith('Tree')
        finally:
            _cleanup_mapping(mapping, tmpdir, client)


@requires_ty_types_cli
@requires_uv
class TestPydanticModelMembers:
    """A ``pydantic.BaseModel`` subclass must expose its annotated fields as
    ``JavaType.Class.members``, resolved through a forwarded dependency
    environment (the CLI ``mod build`` scenario). Mirrors the supertype-
    resolution setup in ``TestExternalSupertypeResolutionInParsePath``.
    """

    _SOURCE = (
        "from pydantic import BaseModel\n"
        "\n"
        "\n"
        "class User(BaseModel):\n"
        "    name: str\n"
        "    age: int = 0\n"
        "\n"
        "    def greeting(self) -> str:\n"
        "        return self.name\n"
    )
    _PYPROJECT = (
        "[project]\n"
        'name = "tyrepro"\n'
        'version = "0.0.0"\n'
        'requires-python = ">=3.10"\n'
        'dependencies = ["pydantic>=2.11"]\n'
    )

    @pytest.fixture(autouse=True)
    def _skip_if_pydantic_ambient(self):
        try:
            import pydantic  # noqa: F401
            pytest.skip("pydantic is importable in the test interpreter; "
                        "ty-types would resolve it ambiently")
        except ImportError:
            pass

    def _dependency_venv(self):
        from rewrite.python.template.dependency_workspace import DependencyWorkspace
        workspace = DependencyWorkspace.get_or_create_from_pyproject(self._PYPROJECT)
        return os.path.join(workspace, ".venv")

    def _user_class_type(self, tmp_path):
        from rewrite.rpc import server
        proj = tmp_path / "proj"
        proj.mkdir()
        (proj / "pyproject.toml").write_text(self._PYPROJECT)
        (proj / "models.py").write_text(self._SOURCE)
        ids = server.handle_parse({
            "inputs": [str(proj / "models.py")],
            "relativeTo": str(proj),
            "dependencyPath": self._dependency_venv(),
        })
        assert ids, "handle_parse returned no results"
        cu = server.local_objects[ids[0]]

        cls = _collect_class_declarations(cu)['User'].type
        assert isinstance(cls, JavaType.Class), \
            f"User class type not resolved: {cls!r}"
        return cls

    def test_pydantic_fields_are_members(self, tmp_path):
        cls = self._user_class_type(tmp_path)
        by_name = {v._name: v for v in (cls._members or [])}
        assert 'name' in by_name, \
            f"pydantic field `name` missing from members: {sorted(by_name)}"
        assert by_name['name']._type == JavaType.Primitive.String
        assert by_name['age']._type == JavaType.Primitive.Int
        for v in cls._members:
            assert isinstance(v, JavaType.Variable)
            assert v._owner is cls

        # Methods still populate.
        assert cls._methods is not None
        assert 'greeting' in [m._name for m in cls._methods]
