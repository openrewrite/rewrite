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

"""Parenthesization of a value spliced into a slot that would otherwise reparse it."""

from rewrite import ExecutionContext, Recipe
from rewrite.python.template import capture, pattern, template
from rewrite.python.visitor import PythonVisitor
from rewrite.test import RecipeSpec, python


def _rule(before: str, after: str) -> Recipe:
    """A recipe rewriting `before` to `after`, so one test drives one slot from source to source."""
    x = capture('x')
    pat = pattern(before, x=x)
    tmpl = template(after, x=x)

    class Rule(Recipe):
        @property
        def name(self) -> str:
            return "test.Splice"

        @property
        def display_name(self) -> str:
            return "Splice"

        @property
        def description(self) -> str:
            return "Splices a captured expression into a template slot."

        def editor(self):
            class Visitor(PythonVisitor[ExecutionContext]):
                def visit_method_invocation(self, method, p):
                    method = super().visit_method_invocation(method, p)
                    match = pat.match(method, self.cursor)
                    return tmpl.apply(self.cursor, values=match) if match else method

                def visit_array_access(self, array_access, p):
                    array_access = super().visit_array_access(array_access, p)
                    match = pat.match(array_access, self.cursor)
                    return tmpl.apply(self.cursor, values=match) if match else array_access

            return Visitor()

    return Rule()


def test_a_binary_operator_binds_its_right_operand_more_tightly():
    RecipeSpec(recipe=_rule("below({x})", "10 - {x}")).rewrite_run(python(
        "a = below(n - 1)\n"
        "b = below(n * 2)\n",
        "a = 10 - (n - 1)\n"
        "b = 10 - n * 2\n",
    ))


def test_power_is_right_associative_and_takes_no_unary_on_its_left():
    RecipeSpec(recipe=_rule("squared({x})", "{x} ** 2")).rewrite_run(python(
        "a = squared(n ** 3)\n"
        "b = squared(-n)\n"
        "c = squared(await n)\n",
        "a = (n ** 3) ** 2\n"
        "b = (-n) ** 2\n"
        "c = await n ** 2\n",
    ))
    RecipeSpec(recipe=_rule("exp({x})", "2 ** {x}")).rewrite_run(python(
        "a = exp(n ** 3)\n"
        "b = exp(-n)\n",
        "a = 2 ** n ** 3\n"
        "b = 2 ** -n\n",
    ))


def test_comparisons_chain_rather_than_associate():
    RecipeSpec(recipe=_rule("under({x})", "{x} < 10")).rewrite_run(python(
        "a = under(n < m)\n"
        "b = under(n | m)\n",
        "a = (n < m) < 10\n"
        "b = n | m < 10\n",
    ))


def test_an_expression_that_is_not_an_operator_still_has_a_precedence():
    RecipeSpec(recipe=_rule("twice({x})", "2 * {x}")).rewrite_run(python(
        "a = twice(p if q else r)\n"
        "b = twice(lambda: q)\n"
        "c = twice(n := q)\n",
        "a = 2 * (p if q else r)\n"
        "b = 2 * (lambda: q)\n"
        "c = 2 * (n := q)\n",
    ))


def test_an_attribute_target_takes_a_call_or_tighter():
    RecipeSpec(recipe=_rule("length({x})", "{x}.bit_length()")).rewrite_run(python(
        "a = length(n + 1)\n"
        "b = length(n.d)\n",
        "a = (n + 1).bit_length()\n"
        "b = n.d.bit_length()\n",
    ))


def test_an_integer_literal_before_a_dot_is_parenthesized():
    RecipeSpec(recipe=_rule("length({x})", "{x}.bit_length()")).rewrite_run(python(
        "a = length(1)\n"
        "b = length(1.5)\n",
        "a = (1).bit_length()\n"
        "b = 1.5.bit_length()\n",
    ))


def test_a_bare_tuple_survives_a_subscript_but_not_a_call():
    RecipeSpec(recipe=_rule("d[{x}]", "f({x})")).rewrite_run(python(
        "a = d[p, q]\n",
        "a = f((p, q))\n",
    ))
    RecipeSpec(recipe=_rule("d[{x}]", "e[{x}]")).rewrite_run(python(
        "a = d[p, q]\n",
        "a = e[p, q]\n",
    ))


def test_a_bare_generator_survives_only_as_a_call_s_sole_argument():
    RecipeSpec(recipe=_rule("collect({x})", "list({x})")).rewrite_run(python(
        "a = collect(v for v in vs)\n",
        "a = list(v for v in vs)\n",
    ))
    RecipeSpec(recipe=_rule("collect({x})", "sorted({x}, reverse=True)")).rewrite_run(python(
        "a = collect(v for v in vs)\n",
        "a = sorted((v for v in vs), reverse=True)\n",
    ))


def test_a_slot_that_takes_a_plain_expression_rejects_a_walrus():
    RecipeSpec(recipe=_rule("kw({x})", "f(k={x})")).rewrite_run(python(
        "a = kw(n := 1)\n",
        "a = f(k=(n := 1))\n",
    ))
    RecipeSpec(recipe=_rule("entry({x})", "{1: {x}}")).rewrite_run(python(
        "a = entry(n := 1)\n",
        "a = {1: (n := 1)}\n",
    ))


def test_a_comprehension_clause_takes_an_or_test():
    RecipeSpec(recipe=_rule("over({x})", "[v for v in {x}]")).rewrite_run(python(
        "a = over(p if c else q)\n",
        "a = [v for v in (p if c else q)]\n",
    ))
    RecipeSpec(recipe=_rule("keep({x})", "[v for v in vs if {x}]")).rewrite_run(python(
        "a = keep(p if c else q)\n",
        "a = [v for v in vs if (p if c else q)]\n",
    ))


def test_a_comprehension_result_takes_an_expression():
    RecipeSpec(recipe=_rule("d[{x}]", "[{x} for v in vs]")).rewrite_run(python(
        "a = d[p, q]\n",
        "a = [(p, q) for v in vs]\n",
    ))


def test_an_associative_operator_lets_its_right_operand_regroup():
    RecipeSpec(recipe=_rule("both({x})", "x and {x}")).rewrite_run(python(
        "a = both(not p and not q)\n",
        "a = x and not p and not q\n",
    ))
    RecipeSpec(recipe=_rule("either({x})", "x or {x}")).rewrite_run(python(
        "a = either(p or q)\n",
        "a = x or p or q\n",
    ))


def test_an_overloadable_operator_keeps_its_right_operand_parenthesized():
    RecipeSpec(recipe=_rule("sum2({x})", "n + {x}")).rewrite_run(python(
        "a = sum2(p + q)\n",
        "a = n + (p + q)\n",
    ))
    RecipeSpec(recipe=_rule("union({x})", "s | {x}")).rewrite_run(python(
        "a = union(p | q)\n",
        "a = s | (p | q)\n",
    ))
