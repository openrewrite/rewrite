# Copyright 2026 the original author or authors.
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

"""Tests for the scope model: scope_of, Scope, binding_names, LocalBindings."""

from typing import List

from rewrite import Cursor
from rewrite.java.tree import ClassDeclaration, Identifier, MethodDeclaration, MethodInvocation
from rewrite.python.scope_utils import LocalBindings, Scope, scope_of
from rewrite.python.tree import CompilationUnit
from rewrite.python.visitor import PythonVisitor
from rewrite.test import RecipeSpec, python


def _cursor_at_anchor(source: str) -> Cursor:
    """The cursor at the source's single ``anchor()`` call."""
    found: List[Cursor] = []

    class Finder(PythonVisitor):
        def visit_method_invocation(self, method: MethodInvocation, p):
            if isinstance(method.name, Identifier) and method.name.simple_name == 'anchor':
                found.append(self.cursor)
            return super().visit_method_invocation(method, p)

    RecipeSpec(type_attribution=False).rewrite_run(
        python(source, after_recipe=lambda sf: Finder().visit(sf, None)))
    assert len(found) == 1
    return found[0]


def _scope_at_anchor(source: str) -> Scope:
    return scope_of(_cursor_at_anchor(source))


def _names_at_anchor(source: str) -> List[List[str]]:
    """What each scope out from the anchor binds, innermost first."""
    bound: List[List[str]] = []
    _scope_at_anchor(source).walk(lambda scope: (bound.append(sorted(scope.names())), True)[1])
    return bound


def test_declaring_scope_names_the_innermost_scope_binding_a_name():
    shadowed = _scope_at_anchor("""
        import json
        def f():
            json = 1
            anchor()
        """)
    assert isinstance(shadowed.declaring_scope('json'), MethodDeclaration)
    assert shadowed.declaring_scope('absent') is None
    assert shadowed.declares('absent') is False

    reachable = _scope_at_anchor("""
        import json
        def f():
            anchor()
        """)
    assert isinstance(reachable.declaring_scope('json'), CompilationUnit)
    assert reachable.declares('json') is True


def test_is_bound_answers_for_a_local_alone_so_a_module_import_stays_attributable():
    bindings = LocalBindings()
    shadowed = _cursor_at_anchor("""
        import json
        def f():
            json = 1
            anchor()
        """)
    # RemoveImport reads this to keep a local shadowing an import from counting as a use of it.
    assert bindings.is_bound(shadowed, 'json') is True

    reachable = _cursor_at_anchor("""
        import json
        def f():
            anchor()
        """)
    assert bindings.is_bound(reachable, 'json') is False


def test_a_defs_decorators_and_parameter_defaults_belong_to_the_enclosing_scope():
    default = _scope_at_anchor("""
        def outer():
            def f(p=anchor()):
                pass
        """)
    assert default.declares('f') is True
    assert default.declares('p') is False

    decorator = _scope_at_anchor("""
        def outer():
            @deco(anchor())
            def f(p):
                pass
        """)
    assert decorator.declares('p') is False


def test_a_class_body_is_reachable_from_directly_within_it_and_nowhere_else():
    assert _scope_at_anchor("""
        class A:
            a = 1
            anchor()
        """).declares('a') is True

    # A method's own scope chains past the class body straight to the module.
    assert _scope_at_anchor("""
        class A:
            a = 1
            def m(self):
                anchor()
        """).declares('a') is False

    # Class bodies do not nest either: `class B: y = a` inside `A` raises NameError.
    assert _scope_at_anchor("""
        class A:
            a = 1
            class B:
                anchor()
        """).declares('a') is False


def test_a_match_case_binds_the_names_its_pattern_captures():
    # The class matched, a keyword attribute and a dotted value pattern name something else.
    assert _names_at_anchor("""
        def f(p):
            match p:
                case Point(a, x=b):
                    pass
                case [c, *d]:
                    pass
                case {'k': e, **rest}:
                    pass
                case Other() as g:
                    pass
                case Color.RED:
                    pass
                case None | _:
                    pass
                case h:
                    anchor()
        """)[0] == ['a', 'b', 'c', 'd', 'e', 'g', 'h', 'p', 'rest']


def test_a_class_bodys_bindings_reach_only_the_statements_after_them():
    # A class body compiles a read to `LOAD_NAME`, which falls back to the module until the
    # class-local name exists.
    rebinding = _scope_at_anchor("""
        import json
        class C:
            json = anchor()
        """)
    assert isinstance(rebinding.declaring_scope('json'), CompilationUnit)

    rebound = _scope_at_anchor("""
        import json
        class C:
            json = 1
            alias = anchor()
        """)
    assert isinstance(rebound.declaring_scope('json'), ClassDeclaration)


def test_a_comprehensions_leading_iterable_is_evaluated_before_its_targets_exist():
    assert _scope_at_anchor("[x for x in anchor()]").declares('x') is False
    assert _scope_at_anchor("[anchor() for x in ys]").declares('x') is True


def test_global_hands_a_name_back_to_the_module_scope():
    scope = _scope_at_anchor("""
        import json
        def f():
            global json
            json = 1
            anchor()
        """)
    assert isinstance(scope.declaring_scope('json'), CompilationUnit)


def test_an_except_alias_binds_for_the_whole_function():
    scope = _scope_at_anchor("""
        import json
        def f():
            try:
                pass
            except ValueError as json:
                pass
            anchor()
        """)
    # CPython puts the alias in `co_varnames`; the end-of-block `del` clears its value, so
    # the later reference raises UnboundLocalError instead of resolving to the import.
    assert isinstance(scope.declaring_scope('json'), MethodDeclaration)


def test_walk_visits_each_scope_outwards_and_stops_where_the_caller_says():
    source = """
        top = 1
        def fn(param):
            local = 2
            anchor()
        """
    assert _names_at_anchor(source) == [['local', 'param'], ['fn', 'top']]

    visited: List[List[str]] = []
    _scope_at_anchor(source).walk(
        lambda scope: (visited.append(sorted(scope.names())), len(visited) < 1)[1])
    assert visited == [['local', 'param']]


def test_a_target_that_assigns_through_an_object_binds_no_name():
    # `self.x` and `d[k]` mutate an object rather than binding a name in the scope.
    assert _names_at_anchor("""
        def f(self, d, k):
            a, (b, *rest) = pair
            self.x = 1
            d[k] = 2
            anchor()
        """)[0] == ['a', 'b', 'd', 'k', 'rest', 'self']


def test_a_cursor_outside_any_compilation_unit_has_no_scope():
    found: List[Cursor] = []

    class Finder(PythonVisitor):
        def visit_method_invocation(self, method: MethodInvocation, p):
            found.append(self.cursor)
            return method

    def visit_statement_alone(sf):
        Finder().visit(sf.statements[0], None)

    RecipeSpec(type_attribution=False).rewrite_run(
        python("anchor()", after_recipe=visit_statement_alone))
    scope = scope_of(found[0])
    assert scope.names() == frozenset()
    assert scope.declares('anchor') is False


def test_a_cursor_on_a_scope_node_sits_outside_it_except_the_module():
    answers = {}

    class Peek(PythonVisitor):
        def visit_comprehension_expression(self, comp, p):
            answers['comprehension'] = scope_of(self.cursor).declares('zz')
            return comp

        def visit_lambda(self, lambda_, p):
            answers['lambda'] = scope_of(self.cursor).declares('aa')
            return lambda_

        def visit_compilation_unit(self, cu, p):
            answers['module'] = scope_of(self.cursor).declares('ys')
            return super().visit_compilation_unit(cu, p)

    RecipeSpec(type_attribution=False).rewrite_run(python("""
        ys = [1]
        q = [zz for zz in ys]
        g = lambda aa: aa
        """, after_recipe=lambda sf: Peek().visit(sf, None)))

    assert answers == {'comprehension': False, 'lambda': False, 'module': True}
