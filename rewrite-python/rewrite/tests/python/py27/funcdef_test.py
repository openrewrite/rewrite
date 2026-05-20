# Copyright 2026 the original author or authors.
#
# Licensed under the Moderne Source Available License (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     https://docs.moderne.io/licensing/moderne-source-available-license
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

"""Structural tests for Py2 function definitions, including decorators."""

import pytest

from rewrite.java import tree as j
from rewrite.python._py2_parser_visitor import Py2ParserVisitor
from rewrite.python.markers import KeywordArguments


def _stmt(source: str):
    cu = Py2ParserVisitor(source, "<test>", "2.7").parse()
    return cu.statements[0]


def _param_names(method: j.MethodDeclaration):
    names = []
    for param in method.parameters:
        if isinstance(param, j.VariableDeclarations):
            for v in param.variables:
                names.append(v.name.simple_name)
    return names


class TestFuncdefBasics:
    def test_no_arg(self):
        m = _stmt("def f():\n    pass\n")
        assert isinstance(m, j.MethodDeclaration)
        assert m.name.simple_name == "f"
        # Empty parameter list still produces a JContainer with an Empty
        # sentinel — matches the Py3 visitor convention.
        assert _param_names(m) == []

    def test_def_modifier_present(self):
        m = _stmt("def f():\n    pass\n")
        kinds = [mod.type for mod in m.modifiers]
        assert j.Modifier.Type.Default in kinds
        keywords = [mod.keyword for mod in m.modifiers]
        assert 'def' in keywords

    def test_single_positional(self):
        m = _stmt("def f(a):\n    pass\n")
        assert _param_names(m) == ["a"]

    def test_multi_positional(self):
        m = _stmt("def f(a, b, c):\n    pass\n")
        assert _param_names(m) == ["a", "b", "c"]

    def test_positional_with_default(self):
        m = _stmt("def f(a, b=1):\n    pass\n")
        # The second parameter holds a NamedVariable with an initializer.
        params = m.parameters
        b_param = params[1]
        assert isinstance(b_param, j.VariableDeclarations)
        b_var = b_param.variables[0]
        assert b_var.initializer is not None
        assert isinstance(b_var.initializer, j.Literal)
        assert b_var.initializer.value_source == "1"

    def test_vararg(self):
        m = _stmt("def f(*args):\n    pass\n")
        assert _param_names(m) == ["args"]
        vararg_param = m.parameters[0]
        assert vararg_param.varargs is not None  # populated for *args

    def test_kwarg(self):
        m = _stmt("def f(**kw):\n    pass\n")
        assert _param_names(m) == ["kw"]
        kwarg_param = m.parameters[0]
        # KeywordArguments marker should be present so the printer emits `**`.
        kwarg_markers = [type(mk).__name__ for mk in kwarg_param.markers.markers]
        assert 'KeywordArguments' in kwarg_markers

    def test_full_signature(self):
        m = _stmt("def f(a, b=1, *args, **kw):\n    pass\n")
        assert _param_names(m) == ["a", "b", "args", "kw"]

    def test_body_contains_real_statements(self):
        m = _stmt("def f(x):\n    y = x + 1\n    return y\n")
        body_stmts = m.body.statements
        assert isinstance(body_stmts[0], j.Assignment)
        assert isinstance(body_stmts[1], j.Return)


class TestDecorators:
    def test_simple_decorator(self):
        m = _stmt("@d\ndef f():\n    pass\n")
        assert isinstance(m, j.MethodDeclaration)
        assert len(m.leading_annotations) == 1

    def test_call_decorator(self):
        m = _stmt("@d(x)\ndef f():\n    pass\n")
        assert len(m.leading_annotations) == 1
        ann = m.leading_annotations[0]
        # Call decorators have arguments populated.
        assert ann.arguments is not None
        assert len(ann.arguments) == 1

    def test_attribute_decorator(self):
        m = _stmt("@a.b.c\ndef f():\n    pass\n")
        assert len(m.leading_annotations) == 1
        ann_type = m.leading_annotations[0].annotation_type
        # a.b.c → FieldAccess(FieldAccess(FieldAccess(Empty, a), b), c)
        assert isinstance(ann_type, j.FieldAccess)
        assert ann_type.name.simple_name == "c"

    def test_multiple_decorators(self):
        m = _stmt("@d1\n@d2\ndef f():\n    pass\n")
        assert len(m.leading_annotations) == 2


class TestFuncdefRegressions:
    """Whole-tree placeholder sweep."""

    @pytest.mark.parametrize("src", [
        "def f():\n    pass\n",
        "def f(a):\n    pass\n",
        "def f(a, b=1):\n    pass\n",
        "def f(*args, **kw):\n    pass\n",
        "def f(x):\n    return x + 1\n",
        "@d\ndef f():\n    pass\n",
        "@d(x)\ndef f():\n    pass\n",
        "@a.b\ndef f(x):\n    return x\n",
        "def outer(a):\n    def inner(b):\n        return a + b\n    return inner\n",
    ])
    def test_no_placeholders(self, src):
        from tests.python.py27.expressions_test import _collect_placeholders
        cu = Py2ParserVisitor(src, "<test>", "2.7").parse()
        stmt = cu.statements[0]
        placeholders = list(_collect_placeholders(stmt))
        assert placeholders == [], f"unexpected placeholders in {src!r}: {placeholders}"
