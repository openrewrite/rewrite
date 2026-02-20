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
            assert result._declaring_type._fully_qualified_name == 'Greeter'
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
            assert result._fully_qualified_name == 'Color'
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
            assert result._fully_qualified_name == 'Greeter'
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
            assert result._fully_qualified_name == 'MyClass'
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
            assert result._fully_qualified_name == 'Multi'
            # First supertype → _supertype
            assert getattr(result, '_supertype', None) is not None, \
                "Multi should have a supertype"
            assert result._supertype._fully_qualified_name == 'Base'
            # Remaining supertypes → _interfaces
            interfaces = getattr(result, '_interfaces', None)
            assert interfaces is not None, \
                "Multi should have interfaces for additional supertypes"
            assert len(interfaces) >= 1
            iface_names = [i._fully_qualified_name for i in interfaces]
            assert 'Mixin' in iface_names
        finally:
            _cleanup_mapping(mapping, tmpdir, client)
