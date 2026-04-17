from __future__ import annotations

from dataclasses import dataclass, replace

from .. import random_id
from ..style import Style, NamedStyles


class PythonStyle(Style):
    pass


@dataclass(frozen=True)
class SpacesStyle(PythonStyle):
    @dataclass(frozen=True)
    class BeforeParentheses:
        _method_call: bool

        @property
        def method_call(self) -> bool:
            return self._method_call

        def with_method_call(self, method_call: bool) -> SpacesStyle.BeforeParentheses:
            return self if method_call is self._method_call else replace(self, _method_call=method_call)

        _method_declaration: bool

        @property
        def method_declaration(self) -> bool:
            return self._method_declaration

        def with_method_declaration(self, method_declaration: bool) -> SpacesStyle.BeforeParentheses:
            return self if method_declaration is self._method_declaration else replace(self, _method_declaration=method_declaration)

        _left_bracket: bool

        @property
        def left_bracket(self) -> bool:
            return self._left_bracket

        def with_left_bracket(self, left_bracket: bool) -> SpacesStyle.BeforeParentheses:
            return self if left_bracket is self._left_bracket else replace(self, _left_bracket=left_bracket)

    @dataclass(frozen=True)
    class AroundOperators:
        _assignment: bool

        @property
        def assignment(self) -> bool:
            return self._assignment

        def with_assignment(self, assignment: bool) -> SpacesStyle.AroundOperators:
            return self if assignment is self._assignment else replace(self, _assignment=assignment)

        _equality: bool

        @property
        def equality(self) -> bool:
            return self._equality

        def with_equality(self, equality: bool) -> SpacesStyle.AroundOperators:
            return self if equality is self._equality else replace(self, _equality=equality)

        _relational: bool

        @property
        def relational(self) -> bool:
            return self._relational

        def with_relational(self, relational: bool) -> SpacesStyle.AroundOperators:
            return self if relational is self._relational else replace(self, _relational=relational)

        _bitwise: bool

        @property
        def bitwise(self) -> bool:
            return self._bitwise

        def with_bitwise(self, bitwise: bool) -> SpacesStyle.AroundOperators:
            return self if bitwise is self._bitwise else replace(self, _bitwise=bitwise)

        _additive: bool

        @property
        def additive(self) -> bool:
            return self._additive

        def with_additive(self, additive: bool) -> SpacesStyle.AroundOperators:
            return self if additive is self._additive else replace(self, _additive=additive)

        _multiplicative: bool

        @property
        def multiplicative(self) -> bool:
            return self._multiplicative

        def with_multiplicative(self, multiplicative: bool) -> SpacesStyle.AroundOperators:
            return self if multiplicative is self._multiplicative else replace(self, _multiplicative=multiplicative)

        _shift: bool

        @property
        def shift(self) -> bool:
            return self._shift

        def with_shift(self, shift: bool) -> SpacesStyle.AroundOperators:
            return self if shift is self._shift else replace(self, _shift=shift)

        _power: bool

        @property
        def power(self) -> bool:
            return self._power

        def with_power(self, power: bool) -> SpacesStyle.AroundOperators:
            return self if power is self._power else replace(self, _power=power)

        _eq_in_named_parameter: bool

        @property
        def eq_in_named_parameter(self) -> bool:
            return self._eq_in_named_parameter

        def with_eq_in_named_parameter(self, eq_in_named_parameter: bool) -> SpacesStyle.AroundOperators:
            return self if eq_in_named_parameter is self._eq_in_named_parameter else replace(self, _eq_in_named_parameter=eq_in_named_parameter)

        _eq_in_keyword_argument: bool

        @property
        def eq_in_keyword_argument(self) -> bool:
            return self._eq_in_keyword_argument

        def with_eq_in_keyword_argument(self, eq_in_keyword_argument: bool) -> SpacesStyle.AroundOperators:
            return self if eq_in_keyword_argument is self._eq_in_keyword_argument else replace(self, _eq_in_keyword_argument=eq_in_keyword_argument)

    @dataclass(frozen=True)
    class Within:
        _brackets: bool

        @property
        def brackets(self) -> bool:
            return self._brackets

        def with_brackets(self, brackets: bool) -> SpacesStyle.Within:
            return self if brackets is self._brackets else replace(self, _brackets=brackets)

        _method_declaration_parentheses: bool

        @property
        def method_declaration_parentheses(self) -> bool:
            return self._method_declaration_parentheses

        def with_method_declaration_parentheses(self, method_declaration_parentheses: bool) -> SpacesStyle.Within:
            return self if method_declaration_parentheses is self._method_declaration_parentheses else replace(self, _method_declaration_parentheses=method_declaration_parentheses)

        _empty_method_declaration_parentheses: bool

        @property
        def empty_method_declaration_parentheses(self) -> bool:
            return self._empty_method_declaration_parentheses

        def with_empty_method_declaration_parentheses(self, empty_method_declaration_parentheses: bool) -> SpacesStyle.Within:
            return self if empty_method_declaration_parentheses is self._empty_method_declaration_parentheses else replace(self, _empty_method_declaration_parentheses=empty_method_declaration_parentheses)

        _method_call_parentheses: bool

        @property
        def method_call_parentheses(self) -> bool:
            return self._method_call_parentheses

        def with_method_call_parentheses(self, method_call_parentheses: bool) -> SpacesStyle.Within:
            return self if method_call_parentheses is self._method_call_parentheses else replace(self, _method_call_parentheses=method_call_parentheses)

        _empty_method_call_parentheses: bool

        @property
        def empty_method_call_parentheses(self) -> bool:
            return self._empty_method_call_parentheses

        def with_empty_method_call_parentheses(self, empty_method_call_parentheses: bool) -> SpacesStyle.Within:
            return self if empty_method_call_parentheses is self._empty_method_call_parentheses else replace(self, _empty_method_call_parentheses=empty_method_call_parentheses)

        _braces: bool

        @property
        def braces(self) -> bool:
            return self._braces

        def with_braces(self, braces: bool) -> SpacesStyle.Within:
            return self if braces is self._braces else replace(self, _braces=braces)

    @dataclass(frozen=True)
    class Other:
        _before_comma: bool

        @property
        def before_comma(self) -> bool:
            return self._before_comma

        def with_before_comma(self, before_comma: bool) -> SpacesStyle.Other:
            return self if before_comma is self._before_comma else replace(self, _before_comma=before_comma)

        _after_comma: bool

        @property
        def after_comma(self) -> bool:
            return self._after_comma

        def with_after_comma(self, after_comma: bool) -> SpacesStyle.Other:
            return self if after_comma is self._after_comma else replace(self, _after_comma=after_comma)

        _before_for_semicolon: bool

        @property
        def before_for_semicolon(self) -> bool:
            return self._before_for_semicolon

        def with_before_for_semicolon(self, before_for_semicolon: bool) -> SpacesStyle.Other:
            return self if before_for_semicolon is self._before_for_semicolon else replace(self, _before_for_semicolon=before_for_semicolon)

        _before_colon: bool

        @property
        def before_colon(self) -> bool:
            return self._before_colon

        def with_before_colon(self, before_colon: bool) -> SpacesStyle.Other:
            return self if before_colon is self._before_colon else replace(self, _before_colon=before_colon)

        _after_colon: bool

        @property
        def after_colon(self) -> bool:
            return self._after_colon

        def with_after_colon(self, after_colon: bool) -> SpacesStyle.Other:
            return self if after_colon is self._after_colon else replace(self, _after_colon=after_colon)

        _before_backslash: bool

        @property
        def before_backslash(self) -> bool:
            return self._before_backslash

        def with_before_backslash(self, before_backslash: bool) -> SpacesStyle.Other:
            return self if before_backslash is self._before_backslash else replace(self, _before_backslash=before_backslash)

        _before_hash: bool

        @property
        def before_hash(self) -> bool:
            return self._before_hash

        def with_before_hash(self, before_hash: bool) -> SpacesStyle.Other:
            return self if before_hash is self._before_hash else replace(self, _before_hash=before_hash)

        _after_hash: bool

        @property
        def after_hash(self) -> bool:
            return self._after_hash

        def with_after_hash(self, after_hash: bool) -> SpacesStyle.Other:
            return self if after_hash is self._after_hash else replace(self, _after_hash=after_hash)

    _before_parentheses: BeforeParentheses

    @property
    def before_parentheses(self) -> BeforeParentheses:
        return self._before_parentheses

    def with_before_parentheses(self, before_parentheses: BeforeParentheses) -> SpacesStyle:
        return self if before_parentheses is self._before_parentheses else replace(self, _before_parentheses=before_parentheses)

    _around_operators: AroundOperators

    @property
    def around_operators(self) -> AroundOperators:
        return self._around_operators

    def with_around_operators(self, around_operators: AroundOperators) -> SpacesStyle:
        return self if around_operators is self._around_operators else replace(self, _around_operators=around_operators)

    _within: Within

    @property
    def within(self) -> Within:
        return self._within

    def with_within(self, within: Within) -> SpacesStyle:
        return self if within is self._within else replace(self, _within=within)

    _other: Other

    @property
    def other(self) -> Other:
        return self._other

    def with_other(self, other: Other) -> SpacesStyle:
        return self if other is self._other else replace(self, _other=other)


@dataclass(frozen=True)
class TabsAndIndentsStyle(PythonStyle):
    _use_tab_character: bool

    @property
    def use_tab_character(self) -> bool:
        return self._use_tab_character

    def with_use_tab_character(self, use_tab_character: bool) -> TabsAndIndentsStyle:
        return self if use_tab_character is self._use_tab_character else replace(self, _use_tab_character=use_tab_character)

    _tab_size: int

    @property
    def tab_size(self) -> int:
        return self._tab_size

    def with_tab_size(self, tab_size: int) -> TabsAndIndentsStyle:
        return self if tab_size is self._tab_size else replace(self, _tab_size=tab_size)

    _indent_size: int

    @property
    def indent_size(self) -> int:
        return self._indent_size

    def with_indent_size(self, indent_size: int) -> TabsAndIndentsStyle:
        return self if indent_size is self._indent_size else replace(self, _indent_size=indent_size)

    _continuation_indent: int

    @property
    def continuation_indent(self) -> int:
        return self._continuation_indent

    def with_continuation_indent(self, continuation_indent: int) -> TabsAndIndentsStyle:
        return self if continuation_indent is self._continuation_indent else replace(self, _continuation_indent=continuation_indent)

    _keep_indents_on_empty_lines: bool

    @property
    def keep_indents_on_empty_lines(self) -> bool:
        return self._keep_indents_on_empty_lines

    def with_keep_indents_on_empty_lines(self, keep_indents_on_empty_lines: bool) -> TabsAndIndentsStyle:
        return self if keep_indents_on_empty_lines is self._keep_indents_on_empty_lines else replace(self, _keep_indents_on_empty_lines=keep_indents_on_empty_lines)

    @dataclass(frozen=True)
    class MethodDeclarationParameters:
        _align_multiline_parameters: bool

        @property
        def align_multiline_parameters(self) -> bool:
            return self._align_multiline_parameters

        def with_align_multiline_parameters(self,
                                            align_multiline_parameters: bool) -> TabsAndIndentsStyle.MethodDeclarationParameters:
            return self if align_multiline_parameters is self._align_multiline_parameters else replace(self,
                                                                                                       _align_multiline_parameters=align_multiline_parameters)

    _method_declaration_parameters: MethodDeclarationParameters

    @property
    def method_declaration_parameters(self) -> MethodDeclarationParameters:
        return self._method_declaration_parameters

    def with_method_declaration_parameters(self,
                                           method_declaration_parameters: MethodDeclarationParameters) -> TabsAndIndentsStyle:
        return self if method_declaration_parameters is self._method_declaration_parameters else replace(self,
                                                                                                         _method_declaration_parameters=method_declaration_parameters)


@dataclass(frozen=True)
class WrappingAndBracesStyle(PythonStyle):
    pass


@dataclass(frozen=True)
class BlankLinesStyle(PythonStyle):
    @dataclass(frozen=True)
    class KeepMaximum:
        _in_declarations: int

        @property
        def in_declarations(self) -> int:
            return self._in_declarations

        def with_in_declarations(self, in_declarations: int) -> BlankLinesStyle.KeepMaximum:
            return self if in_declarations is self._in_declarations else replace(self, _in_declarations=in_declarations)

        _in_code: int

        @property
        def in_code(self) -> int:
            return self._in_code

        def with_in_code(self, in_code: int) -> BlankLinesStyle.KeepMaximum:
            return self if in_code is self._in_code else replace(self, _in_code=in_code)

    @dataclass(frozen=True)
    class Minimum:
        _after_top_level_imports: int

        @property
        def after_top_level_imports(self) -> int:
            return self._after_top_level_imports

        def with_after_top_level_imports(self, after_top_level_imports: int) -> BlankLinesStyle.Minimum:
            return self if after_top_level_imports is self._after_top_level_imports else replace(self, _after_top_level_imports=after_top_level_imports)

        _around_class: int

        @property
        def around_class(self) -> int:
            return self._around_class

        def with_around_class(self, around_class: int) -> BlankLinesStyle.Minimum:
            return self if around_class is self._around_class else replace(self, _around_class=around_class)

        _around_method: int

        @property
        def around_method(self) -> int:
            return self._around_method

        def with_around_method(self, around_method: int) -> BlankLinesStyle.Minimum:
            return self if around_method is self._around_method else replace(self, _around_method=around_method)

        _around_top_level_classes_functions: int

        @property
        def around_top_level_classes_functions(self) -> int:
            return self._around_top_level_classes_functions

        def with_around_top_level_classes_functions(self, around_top_level_classes_functions: int) -> BlankLinesStyle.Minimum:
            return self if around_top_level_classes_functions is self._around_top_level_classes_functions else replace(self, _around_top_level_classes_functions=around_top_level_classes_functions)

        _after_local_imports: int

        @property
        def after_local_imports(self) -> int:
            return self._after_local_imports

        def with_after_local_imports(self, after_local_imports: int) -> BlankLinesStyle.Minimum:
            return self if after_local_imports is self._after_local_imports else replace(self, _after_local_imports=after_local_imports)

        _before_first_method: int

        @property
        def before_first_method(self) -> int:
            return self._before_first_method

        def with_before_first_method(self, before_first_method: int) -> BlankLinesStyle.Minimum:
            return self if before_first_method is self._before_first_method else replace(self, _before_first_method=before_first_method)

    _keep_maximum: KeepMaximum

    @property
    def keep_maximum(self) -> KeepMaximum:
        return self._keep_maximum

    def with_keep_maximum(self, keep_maximum: KeepMaximum) -> BlankLinesStyle:
        return self if keep_maximum is self._keep_maximum else replace(self, _keep_maximum=keep_maximum)

    _minimum: Minimum

    @property
    def minimum(self) -> Minimum:
        return self._minimum

    def with_minimum(self, minimum: Minimum) -> BlankLinesStyle:
        return self if minimum is self._minimum else replace(self, _minimum=minimum)


@dataclass(frozen=True)
class OtherStyle(PythonStyle):
    @dataclass(frozen=True)
    class UseContinuationIndent:
        _method_call_arguments: bool

        @property
        def method_call_arguments(self) -> bool:
            return self._method_call_arguments

        def with_method_call_arguments(self, method_call_arguments: bool) -> OtherStyle.UseContinuationIndent:
            return self if method_call_arguments is self._method_call_arguments else replace(self, _method_call_arguments=method_call_arguments)

        _method_declaration_parameters: bool

        @property
        def method_declaration_parameters(self) -> bool:
            return self._method_declaration_parameters

        def with_method_declaration_parameters(self, method_declaration_parameters: bool) -> OtherStyle.UseContinuationIndent:
            return self if method_declaration_parameters is self._method_declaration_parameters else replace(self, _method_declaration_parameters=method_declaration_parameters)

        _collections_and_comprehensions: bool

        @property
        def collections_and_comprehensions(self) -> bool:
            return self._collections_and_comprehensions

        def with_collections_and_comprehensions(self, collections_and_comprehensions: bool) -> OtherStyle.UseContinuationIndent:
            return self if collections_and_comprehensions is self._collections_and_comprehensions else replace(self, _collections_and_comprehensions=collections_and_comprehensions)

    _use_continuation_indent: UseContinuationIndent

    @property
    def use_continuation_indent(self) -> UseContinuationIndent:
        return self._use_continuation_indent

    def with_use_continuation_indent(self, use_continuation_indent: UseContinuationIndent) -> OtherStyle:
        return self if use_continuation_indent is self._use_continuation_indent else replace(self, _use_continuation_indent=use_continuation_indent)


class IntelliJ(NamedStyles):

    def __init__(self):
        super().__init__(
            _id=random_id(),
            _name='org.openrewrite.python.style.IntelliJ',
            _display_name='IntelliJ IDEA',
            _description='IntelliJ IDEA default Python style.',
            _tags={},
            _styles=(
                IntelliJ.spaces(),
                IntelliJ.wrapping_and_braces(),
                IntelliJ.tabs_and_indents(),
                IntelliJ.blank_lines(),
                IntelliJ.other(),
            )
        )

    @classmethod
    def defaults(cls) -> IntelliJ:
        return IntelliJ()

    @classmethod
    def spaces(cls) -> SpacesStyle:
        return SpacesStyle(
            SpacesStyle.BeforeParentheses(
                _method_call=False,
                _method_declaration=False,
                _left_bracket=False,
            ),
            SpacesStyle.AroundOperators(
                _assignment=True,
                _equality=True,
                _relational=True,
                _bitwise=True,
                _additive=True,
                _multiplicative=True,
                _shift=True,
                _power=True,
                _eq_in_named_parameter=False,
                _eq_in_keyword_argument=False,
            ),
            SpacesStyle.Within(
                _brackets=False,
                _method_declaration_parentheses=False,
                _empty_method_declaration_parentheses=False,
                _method_call_parentheses=False,
                _empty_method_call_parentheses=False,
                _braces=False,
            ),
            SpacesStyle.Other(
                _before_comma=False,
                _after_comma=True,
                _before_for_semicolon=False,
                _before_colon=False,
                _after_colon=True,
                _before_backslash=True,
                _before_hash=True,
                _after_hash=True,
            ),
        )

    @classmethod
    def tabs_and_indents(cls) -> TabsAndIndentsStyle:
        return TabsAndIndentsStyle(
            _use_tab_character=False,
            _tab_size=4,
            _indent_size=4,
            _continuation_indent=8,
            _keep_indents_on_empty_lines=False,
            _method_declaration_parameters=TabsAndIndentsStyle.MethodDeclarationParameters(
                _align_multiline_parameters=True,
            ),
        )

    @classmethod
    def wrapping_and_braces(cls) -> WrappingAndBracesStyle:
        return WrappingAndBracesStyle()

    @classmethod
    def blank_lines(cls) -> BlankLinesStyle:
        return BlankLinesStyle(
            BlankLinesStyle.KeepMaximum(
                _in_declarations=1,
                _in_code=1,
            ),
            BlankLinesStyle.Minimum(
                _after_top_level_imports=1,
                _around_class=1,
                _around_method=1,
                _around_top_level_classes_functions=2,
                _after_local_imports=0,
                _before_first_method=0,
            )
        )

    @classmethod
    def other(cls) -> OtherStyle:
        return OtherStyle(
            OtherStyle.UseContinuationIndent(
                _method_call_arguments=False,
                _method_declaration_parameters=True,
                _collections_and_comprehensions=False,
            )
        )
