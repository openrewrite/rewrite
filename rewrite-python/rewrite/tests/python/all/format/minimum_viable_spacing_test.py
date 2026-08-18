from rewrite import Markers, random_id
from rewrite.java import (Assert, Binary, Block, Expression, P, J, JLeftPadded, MethodInvocation, Return, Space,
                          JRightPadded, Ternary, Unary)
from rewrite.python import MinimumViableSpacingVisitor, PythonVisitor
from rewrite.test import rewrite_run, python, RecipeSpec, from_visitor


def test_semicolon():
    rewrite_run(
        # language=python
        python(
            """
            def foo():
                print('a'); print('b')
            """
        ),
        spec=RecipeSpec()
        .with_recipe(from_visitor(MinimumViableSpacingVisitor()))
    )


def test_statement_without_prefix():
    rewrite_run(
        # language=python
        python(
            """
            def foo():
                print('a')
            """,
            """
            def foo():
                print('a')
            print('a')
            """
        ),
        spec=RecipeSpec()
        .with_recipes(
            from_visitor(DuplicateMethod()),
            from_visitor(MinimumViableSpacingVisitor())
        )
    )


def test_statement_with_semicolon():
    rewrite_run(
        # language=python
        python(
            """
            def foo():
                print('a');
            """,
            """
            def foo():
                print('a');print('a');
            """
        ),
        spec=RecipeSpec()
        .with_recipes(
            from_visitor(DuplicateMethod()),
            from_visitor(MinimumViableSpacingVisitor())
        )
    )


def test_not_leaves_existing_spacing_untouched():
    rewrite_run(
        # language=python
        python("assert not  x"),
        spec=RecipeSpec()
        .with_recipe(from_visitor(MinimumViableSpacingVisitor()))
    )


def test_arithmetic_unary_stays_tight():
    rewrite_run(
        # language=python
        python("assert -x"),
        spec=RecipeSpec()
        .with_recipe(from_visitor(MinimumViableSpacingVisitor()))
    )


def negated(expression: Expression) -> Unary:
    """Wraps `expression` in `not`, leaving every prefix empty."""
    return Unary(
        random_id(),
        Space.EMPTY,
        Markers.EMPTY,
        JLeftPadded(Space.EMPTY, Unary.Type.Not, Markers.EMPTY),
        expression.replace(prefix=Space.EMPTY),
        None,
    )


class NegateAssertCondition(PythonVisitor):
    def visit_assert(self, assert_: Assert, p: P) -> J:
        a = super().visit_assert(assert_, p)
        return a if isinstance(a.condition, Unary) else a.replace(condition=negated(a.condition))


class NegateReturnValue(PythonVisitor):
    def visit_return(self, return_: Return, p: P) -> J:
        r = super().visit_return(return_, p)
        return r if isinstance(r.expression, Unary) else r.replace(expression=negated(r.expression))


class NegateBinaryRight(PythonVisitor):
    def visit_binary(self, binary: Binary, p: P) -> J:
        b = super().visit_binary(binary, p)
        return b if isinstance(b.right, Unary) else b.replace(right=negated(b.right))


class NegateTernaryCondition(PythonVisitor):
    def visit_ternary(self, ternary: Ternary, p: P) -> J:
        t = super().visit_ternary(ternary, p)
        return t if isinstance(t.condition, Unary) else t.replace(condition=negated(t.condition))


class NegateTernaryFalsePart(PythonVisitor):
    def visit_ternary(self, ternary: Ternary, p: P) -> J:
        t = super().visit_ternary(ternary, p)
        if isinstance(t.false_part, Unary):
            return t
        return t.padding.replace(false_part=t.padding.false_part.replace(element=negated(t.false_part)))


def test_not_operand_starting_with_delimiter_stays_tight():
    rewrite_run(
        # language=python
        python("assert not(x)"),
        spec=RecipeSpec()
        .with_recipe(from_visitor(MinimumViableSpacingVisitor()))
    )


def test_not_operand_starting_with_bracket_stays_tight():
    rewrite_run(
        # language=python
        python("assert not[1]"),
        spec=RecipeSpec()
        .with_recipe(from_visitor(MinimumViableSpacingVisitor()))
    )


def test_generated_not_before_string_literal_stays_tight():
    rewrite_run(
        # language=python
        python("assert 'x'", "assert not'x'"),
        spec=RecipeSpec()
        .with_recipes(from_visitor(NegateAssertCondition()), from_visitor(MinimumViableSpacingVisitor()))
    )


def test_generated_not_before_f_string_gets_space():
    rewrite_run(
        # language=python
        python("assert f'{x}'", "assert not f'{x}'"),
        spec=RecipeSpec()
        .with_recipes(from_visitor(NegateAssertCondition()), from_visitor(MinimumViableSpacingVisitor()))
    )


def test_space_added_around_generated_not():
    rewrite_run(
        # language=python
        python("assert x", "assert not x"),
        spec=RecipeSpec()
        .with_recipes(from_visitor(NegateAssertCondition()), from_visitor(MinimumViableSpacingVisitor()))
    )


def test_space_before_generated_not_only():
    rewrite_run(
        # language=python
        python("assert (x)", "assert not(x)"),
        spec=RecipeSpec()
        .with_recipes(from_visitor(NegateAssertCondition()), from_visitor(MinimumViableSpacingVisitor()))
    )


def test_space_before_generated_not_after_return():
    rewrite_run(
        # language=python
        python(
            """
            def f():
                return x
            """,
            """
            def f():
                return not x
            """
        ),
        spec=RecipeSpec()
        .with_recipes(from_visitor(NegateReturnValue()), from_visitor(MinimumViableSpacingVisitor()))
    )


def test_space_before_generated_not_after_and():
    rewrite_run(
        # language=python
        python("assert x and y", "assert x and not y"),
        spec=RecipeSpec()
        .with_recipes(from_visitor(NegateBinaryRight()), from_visitor(MinimumViableSpacingVisitor()))
    )


def test_space_before_generated_not_after_ternary_else():
    rewrite_run(
        # language=python
        python("z = a if c else b", "z = a if c else not b"),
        spec=RecipeSpec()
        .with_recipes(from_visitor(NegateTernaryFalsePart()), from_visitor(MinimumViableSpacingVisitor()))
    )


def test_space_before_generated_not_after_ternary_if():
    rewrite_run(
        # language=python
        python("z = a if c else b", "z = a if not c else b"),
        spec=RecipeSpec()
        .with_recipes(from_visitor(NegateTernaryCondition()), from_visitor(MinimumViableSpacingVisitor()))
    )


class DuplicateMethod(PythonVisitor):
    def visit_block(self, block: Block, p: P) -> J:
        if block.statements and isinstance(block.statements[0], MethodInvocation):
            existing_rp = block.padding.statements[0]
            stmt = existing_rp.element.replace(prefix=Space.EMPTY)
            new_rp = JRightPadded(stmt, Space.EMPTY, existing_rp.markers)
            new_statements = list(block.padding.statements) + [new_rp]
            return block.padding.replace(statements=new_statements)
