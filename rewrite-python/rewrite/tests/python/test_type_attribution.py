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


class TestIsVariableHover:
    """Tests for _is_variable_hover detecting variable vs function/class/module."""

    @pytest.fixture(autouse=True)
    def setup_mapping(self):
        self.mapping = PythonTypeMapping("")
        yield
        self.mapping.close()

    def test_bare_type_is_variable(self):
        """ty returns just the type for variables, e.g. 'int'."""
        assert self.mapping._is_variable_hover("int") is True

    def test_literal_type_is_variable(self):
        """ty returns Literal[5] for inferred constants."""
        assert self.mapping._is_variable_hover("Literal[5]") is True

    def test_class_type_is_variable(self):
        """A class-typed variable shows just the class name."""
        assert self.mapping._is_variable_hover("Response") is True

    def test_function_hover(self):
        assert self.mapping._is_variable_hover("def foo(x: int) -> str") is False

    def test_class_hover(self):
        assert self.mapping._is_variable_hover("class MyClass") is False

    def test_module_hover(self):
        assert self.mapping._is_variable_hover("<module 'os'>") is False

    def test_empty_hover(self):
        assert self.mapping._is_variable_hover("") is False

    def test_markdown_wrapped_variable(self):
        assert self.mapping._is_variable_hover("```python\nint\n```") is True

    def test_markdown_wrapped_function(self):
        assert self.mapping._is_variable_hover("```python\ndef foo() -> int\n```") is False

    def test_unknown_is_not_variable(self):
        """Unknown hover means ty couldn't resolve the reference — not a variable."""
        assert self.mapping._is_variable_hover("Unknown") is False


class TestMakeVariable:
    """Tests for _make_variable creating JavaType.Variable instances."""

    @pytest.fixture(autouse=True)
    def setup_mapping(self):
        self.mapping = PythonTypeMapping("")
        yield
        self.mapping.close()

    def test_creates_variable_with_name_and_type(self):
        var = self.mapping._make_variable("x", JavaType.Primitive.Int)
        assert isinstance(var, JavaType.Variable)
        assert var.name == "x"
        assert var.type == JavaType.Primitive.Int
        assert var.owner is None

    def test_creates_variable_with_owner(self):
        owner = JavaType.Class()
        owner._fully_qualified_name = "MyClass"
        var = self.mapping._make_variable("field", JavaType.Primitive.String, owner=owner)
        assert var.name == "field"
        assert var.type == JavaType.Primitive.String
        assert var.owner is owner

    def test_creates_variable_with_none_type(self):
        var = self.mapping._make_variable("unknown", None)
        assert var.name == "unknown"
        assert var.type is None


@requires_ty_cli
class TestNameTypeInfo:
    """Tests for name_type_info returning (expr_type, field_type) for name references."""

    @pytest.fixture(autouse=True)
    def reset_client(self):
        yield
        TyLspClient.reset()

    def test_local_variable_has_field_type(self):
        """A local variable reference should have a Variable field_type."""
        source = 'x: int = 5\ny = x\n'
        with tempfile.NamedTemporaryFile(suffix='.py', delete=False, mode='w') as f:
            f.write(source)
            file_path = f.name

        try:
            tree = ast.parse(source)
            mapping = PythonTypeMapping(source, file_path)

            # 'x' on line 2 (the reference)
            name_node = tree.body[1].value  # the Name('x') on RHS
            expr_type, field_type = mapping.name_type_info(name_node)

            mapping.close()

            assert expr_type is not None
            assert field_type is not None
            assert isinstance(field_type, JavaType.Variable)
            assert field_type.name == "x"
        finally:
            os.unlink(file_path)

    def test_function_name_has_no_field_type(self):
        """A function name should not get a Variable field_type."""
        source = 'def foo():\n    pass\nfoo()\n'
        with tempfile.NamedTemporaryFile(suffix='.py', delete=False, mode='w') as f:
            f.write(source)
            file_path = f.name

        try:
            tree = ast.parse(source)
            mapping = PythonTypeMapping(source, file_path)

            # 'foo' in foo() call — the func is ast.Name
            func_node = tree.body[1].value.func
            _, field_type = mapping.name_type_info(func_node)

            mapping.close()

            assert field_type is None
        finally:
            os.unlink(file_path)

    def test_annotated_module_variable(self):
        """A module-level annotated variable should have a Variable field_type."""
        source = 'MY_CONST: str = "hello"\nprint(MY_CONST)\n'
        with tempfile.NamedTemporaryFile(suffix='.py', delete=False, mode='w') as f:
            f.write(source)
            file_path = f.name

        try:
            tree = ast.parse(source)
            mapping = PythonTypeMapping(source, file_path)

            # MY_CONST on line 2 (the reference inside print())
            name_node = tree.body[1].value.args[0]
            expr_type, field_type = mapping.name_type_info(name_node)

            mapping.close()

            assert field_type is not None
            assert isinstance(field_type, JavaType.Variable)
            assert field_type.name == "MY_CONST"
        finally:
            os.unlink(file_path)


@requires_ty_cli
class TestAttributeTypeInfo:
    """Tests for attribute_type_info returning (expr_type, field_type) for attribute access."""

    @pytest.fixture(autouse=True)
    def reset_client(self):
        yield
        TyLspClient.reset()

    def test_class_field_has_field_type(self):
        """self.x should produce a Variable with the class as owner."""
        source = 'class Foo:\n    def __init__(self):\n        self.x: int = 5\n    def get(self):\n        return self.x\n'
        with tempfile.NamedTemporaryFile(suffix='.py', delete=False, mode='w') as f:
            f.write(source)
            file_path = f.name

        try:
            tree = ast.parse(source)
            mapping = PythonTypeMapping(source, file_path)

            # self.x on line 5 (the reference in get())
            attr_node = tree.body[0].body[1].body[0].value  # Return's value = Attribute
            expr_type, field_type = mapping.attribute_type_info(attr_node)

            mapping.close()

            assert expr_type is not None
            assert field_type is not None
            assert isinstance(field_type, JavaType.Variable)
            assert field_type.name == "x"
        finally:
            os.unlink(file_path)

    def test_method_attribute_has_no_field_type(self):
        """obj.method (not a call, but a method reference) should not have Variable field_type."""
        source = '"hello".upper()\n'
        with tempfile.NamedTemporaryFile(suffix='.py', delete=False, mode='w') as f:
            f.write(source)
            file_path = f.name

        try:
            tree = ast.parse(source)
            mapping = PythonTypeMapping(source, file_path)

            # The Attribute node for .upper
            attr_node = tree.body[0].value.func  # Call.func = Attribute
            _, field_type = mapping.attribute_type_info(attr_node)

            mapping.close()

            assert field_type is None
        finally:
            os.unlink(file_path)
