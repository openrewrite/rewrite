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

"""Tests for Python type attribution using ty.

These tests verify that the PythonTypeMapping class correctly:
1. Starts and communicates with the ty LSP server
2. Maps Python types to JavaType objects
3. Returns proper JavaType.Method with declaring_type and parameter_types

These tests require ty to be installed: `uv tool install ty`
"""

import ast
import pytest
import tempfile
import os
from pathlib import Path

from rewrite.java import JavaType

# Import type mapping modules
from rewrite.python.type_mapping import PythonTypeMapping, _TY_AVAILABLE
from rewrite.python.ty_client import TyLspClient


def _ty_cli_available() -> bool:
    """Check if the ty CLI is available on PATH."""
    import shutil
    return shutil.which('ty') is not None


_TY_CLI_INSTALLED = _ty_cli_available()


# Mark for tests that require ty CLI to be installed
requires_ty_cli = pytest.mark.skipif(
    not _TY_CLI_INSTALLED,
    reason="ty CLI is not installed (install with: uv tool install ty)"
)


@requires_ty_cli
class TestTyLspClient:
    """Tests for the TyLspClient singleton.

    These tests require the ty CLI to be installed.
    """

    @pytest.fixture(autouse=True)
    def reset_client(self):
        """Reset the TyLspClient singleton before each test."""
        yield
        TyLspClient.reset()

    def test_client_initializes(self):
        """Test that TyLspClient starts the ty server."""
        client = TyLspClient.get()
        assert client is not None
        assert client.is_available

    def test_client_is_singleton(self):
        """Test that TyLspClient returns the same instance."""
        client1 = TyLspClient.get()
        client2 = TyLspClient.get()
        assert client1 is client2

    def test_client_reset(self):
        """Test that reset creates a new instance."""
        client1 = TyLspClient.get()
        TyLspClient.reset()
        client2 = TyLspClient.get()
        assert client1 is not client2

    def test_hover_on_simple_variable(self):
        """Test hover returns type for a simple variable."""
        client = TyLspClient.get()

        # Create a temp file to get a valid URI
        with tempfile.NamedTemporaryFile(suffix='.py', delete=False) as f:
            f.write(b'x: int = 42\n')
            uri = Path(f.name).as_uri()

        try:
            client.open_document(uri, 'x: int = 42\n')
            hover = client.get_hover(uri, 0, 0)  # Line 0, char 0 = start of 'x'
            client.close_document(uri)

            # ty should return type information for 'x'
            assert hover is not None
            # ty may return 'int' or 'Literal[42]' depending on version
            assert 'int' in hover or 'Literal[42]' in hover
        finally:
            os.unlink(f.name)


class TestPythonTypeMappingPrimitives:
    """Tests for mapping Python primitive types to JavaType."""

    @pytest.fixture(autouse=True)
    def reset_client(self):
        """Reset the TyLspClient singleton after each test."""
        yield
        TyLspClient.reset()

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
        """Reset the TyLspClient singleton after each test."""
        yield
        TyLspClient.reset()

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
        # ty returns all signature params (sep, maxsplit), not just the called args
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
        # ty returns all signature params (old, new, count), not just the called args
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
        """Reset the TyLspClient singleton after each test."""
        yield
        TyLspClient.reset()

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
        """Reset the TyLspClient singleton after each test."""
        yield
        TyLspClient.reset()

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
        # ty returns all signature params for print (values, sep, end, file, flush)
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
