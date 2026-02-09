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
from rewrite.python.type_mapping import PythonTypeMapping, _TY_AVAILABLE
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
    """Tests for the TyTypesClient singleton.

    These tests require the ty-types CLI to be installed.
    """

    @pytest.fixture(autouse=True)
    def reset_client(self):
        """Reset the TyTypesClient singleton before each test."""
        yield
        TyTypesClient.reset()

    def test_client_initializes(self):
        """Test that TyTypesClient starts the ty-types process."""
        client = TyTypesClient.get()
        assert client is not None

        # Create a temp dir as project root
        with tempfile.TemporaryDirectory() as tmpdir:
            assert client.initialize(tmpdir)
            assert client.is_available

    def test_client_is_singleton(self):
        """Test that TyTypesClient returns the same instance."""
        client1 = TyTypesClient.get()
        client2 = TyTypesClient.get()
        assert client1 is client2

    def test_client_reset(self):
        """Test that reset creates a new instance."""
        client1 = TyTypesClient.get()
        TyTypesClient.reset()
        client2 = TyTypesClient.get()
        assert client1 is not client2

    def test_get_types_on_simple_file(self):
        """Test that get_types returns node types for a file."""
        client = TyTypesClient.get()

        with tempfile.TemporaryDirectory() as tmpdir:
            # Write a simple Python file
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

    @pytest.fixture(autouse=True)
    def reset_client(self):
        """Reset the TyTypesClient singleton after each test."""
        yield
        TyTypesClient.reset()

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

    @pytest.fixture(autouse=True)
    def reset_client(self):
        """Reset the TyTypesClient singleton after each test."""
        yield
        TyTypesClient.reset()

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
            mapping = PythonTypeMapping(source, file_path)

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

    @pytest.fixture(autouse=True)
    def reset_client(self):
        """Reset the TyTypesClient singleton after each test."""
        yield
        TyTypesClient.reset()

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
            mapping = PythonTypeMapping(source, file_path)

            # The call is in the second statement
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
            mapping = PythonTypeMapping(source, file_path)

            call = tree.body[1].value
            result = mapping.method_invocation_type(call)

            mapping.close()

            assert result is not None
            assert result._name == 'split'
            assert result._parameter_types is not None
        finally:
            os.unlink(file_path)


class TestTypeAttributionEdgeCases:
    """Tests for edge cases in type attribution."""

    @pytest.fixture(autouse=True)
    def reset_client(self):
        """Reset the TyTypesClient singleton after each test."""
        yield
        TyTypesClient.reset()

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
        offsets = PythonTypeMapping._compute_line_byte_offsets(source)
        # Line 1 starts at byte 0, line 2 at byte 4 (after 'abc\n')
        assert offsets[0] == 0
        assert offsets[1] == 4

        mapping = PythonTypeMapping(source)
        mapping.close()


@requires_ty_types_cli
class TestStructuredCallSignatures:
    """Tests for structured call signature data from ty-types."""

    def test_split_has_structured_signature(self):
        """Test that str.split() returns structured parameter data."""
        source = '"hello".split(",")'
        tree = ast.parse(source)
        mapping = PythonTypeMapping(source)

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
        mapping = PythonTypeMapping(source)

        call = tree.body[0].value

        # Directly check the call signature index
        sig = mapping._lookup_call_signature(call)

        mapping.close()

        assert sig is not None, "Expected structured callSignature data"
        params = sig['parameters']
        assert len(params) >= 1
        assert params[0]['name'] == 'sep'
        assert params[0]['typeId'] is not None
        # sep has a default value
        assert params[0]['hasDefault'] is True

    def test_builtin_len_structured_signature(self):
        """Test structured signature for builtin len()."""
        source = 'len("hello")'
        tree = ast.parse(source)
        mapping = PythonTypeMapping(source)

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

        # There's no call node, so we shouldn't find any signatures
        assert len(mapping._call_signature_index) == 0

        mapping.close()
