import ast
import sys
import token
from argparse import ArgumentError
from functools import lru_cache
from io import BytesIO
from pathlib import Path
from tokenize import tokenize, TokenInfo
from typing import Optional, TypeVar, cast, Callable, List, Tuple, Dict, Type, Sequence, Union, Iterator

from more_itertools import peekable

from rewrite import random_id, Markers, list_map_last
from rewrite.java import Space, JRightPadded, JContainer, JLeftPadded, JavaType, J, Statement, Semicolon, TrailingComma, \
    NameTree, OmitParentheses, Expression, TypeTree, TypedTree, Comment
from rewrite.java import tree as j
from . import tree as py
from .markers import KeywordArguments, KeywordOnlyArguments, Quoted
from .support_types import PyComment
from .type_mapping import PythonTypeMapping

T = TypeVar('T')
J2 = TypeVar('J2', bound=J)


class ParserVisitor(ast.NodeVisitor):
    _source: str
    _cursor: int
    _parentheses_stack: List[Tuple[Callable[[T, Space], T], int, int, ast.AST, Space]]

    @property
    def _source_after_cursor(self) -> str:
        return self._slow_source_after_cursor(self._source, self._cursor)

    @staticmethod
    @lru_cache
    def _slow_source_after_cursor(source: str, cursor: int) -> str:
        return source[cursor:]

    def __init__(self, source: str):
        super().__init__()
        self._source = source
        self._cursor = 0
        self._parentheses_stack = []
        self._type_mapping = PythonTypeMapping(source)

    def generic_visit(self, node):
        return super().generic_visit(node)

    def visit_arguments(self, node, with_close_paren: bool = True) -> List[JRightPadded[j.VariableDeclarations]]:
        first_with_default = len(node.posonlyargs) + len(node.args) - len(
            node.defaults) if node.defaults else sys.maxsize
        if not node.posonlyargs and not node.args and not node.vararg and not node.kwarg and not node.kwonlyargs:
            return [
                JRightPadded(
                    j.Empty(random_id(), self.__source_before(')') if with_close_paren else Space.EMPTY, Markers.EMPTY),
                    Space.EMPTY, Markers.EMPTY)
            ]

        mapped = []
        if node.posonlyargs:
            mapped += [self.__pad_list_element(
                self.map_arg(a, node.defaults[i - first_with_default] if i >= first_with_default else None)) for
                i, a in enumerate(node.posonlyargs)]
            mapped.append(self.__pad_list_element(
                j.VariableDeclarations(
                    random_id(),
                    self.__whitespace(),
                    Markers.EMPTY,
                    [],
                    [],
                    None,
                    None,
                    [],
                    [self.__pad_right(
                        j.VariableDeclarations.NamedVariable(
                            random_id(),
                            Space.EMPTY,
                            Markers.EMPTY,
                            cast(j.Identifier, self.__convert_name('/', None)),
                            [],
                            None,
                            None
                        ),
                        Space.EMPTY
                    )]
                ),
                last=not node.args and not node.vararg and not node.kwarg and not node.kwonlyargs,
                end_delim=',' if node.args or node.vararg or node.kwarg or node.kwonlyargs else ')' if with_close_paren else None
            ))

        mapped += [self.__pad_list_element(
            self.map_arg(a, node.defaults[i - first_with_default] if i >= first_with_default else None),
            i == len(node.args) + len(
                node.posonlyargs) - 1 and not node.vararg and not node.kwarg and not node.kwonlyargs,
            end_delim=',' if node.vararg or node.kwarg or node.kwonlyargs else ')' if with_close_paren else None) for
            (i, a) in [(i + len(node.posonlyargs), a) for i, a in enumerate(node.args)]]
        if node.vararg:
            mapped.append(self.__pad_list_element(
                self.map_arg(node.vararg, None, vararg=True),
                not node.kwarg and not node.kwonlyargs,
                end_delim=')' if with_close_paren else None
            ))
        if node.kwonlyargs:
            if not node.vararg:
                empty_name = j.VariableDeclarations.NamedVariable(random_id(), Space.EMPTY, Markers.EMPTY,
                                                                  cast(j.Identifier, self.__convert_name('', None)), [],
                                                                  None, None)
                kwonly_prefix = self.__source_before('*')
                mapped.append(
                    JRightPadded(
                        j.VariableDeclarations(
                            random_id(),
                            kwonly_prefix,
                            Markers(random_id(), [KeywordOnlyArguments(random_id())]),
                            [], [], None, None, [],
                            [self.__pad_right(empty_name, self.__source_before(','))],
                            None
                        ),
                        Space.EMPTY,
                        Markers.EMPTY
                    ))

            for i, kwonlyarg in enumerate(node.kwonlyargs):
                mapped.append(self.__pad_list_element(
                    self.map_arg(kwonlyarg, node.kw_defaults[i], kwarg=False),
                    not node.kwarg and i == len(node.kwonlyargs) - 1,
                    end_delim=')' if with_close_paren else None
                ))

        if node.kwarg:
            mapped.append(self.__pad_list_element(
                self.map_arg(node.kwarg, None, kwarg=True),
                True,
                end_delim=')' if with_close_paren else None
            ))
        return mapped

    def map_arg(self, node, default=None, vararg=False, kwarg=False):
        prefix = self.__source_before('**') if kwarg else self.__whitespace()
        vararg_prefix = self.__source_before('*') if vararg else None
        name = self.__convert_name(node.arg, self._type_mapping.type(node))
        after_name = self.__source_before(':') if node.annotation else Space.EMPTY
        type_expression = self.__convert_type(node.annotation) if node.annotation else None
        initializer = self.__pad_left(self.__source_before('='), self.__convert(default)) if default else None

        return j.VariableDeclarations(
            random_id(),
            prefix,
            Markers(random_id(), [KeywordArguments(random_id())]) if kwarg else Markers.EMPTY,
            [],
            [],
            type_expression,
            vararg_prefix if vararg else None,
            [],
            [self.__pad_right(j.VariableDeclarations.NamedVariable(
                random_id(),
                Space.EMPTY,
                Markers.EMPTY,
                cast(j.Identifier, name),
                [],
                initializer,
                self._type_mapping.type(node)
            ), after_name)],
        )

    def visit_Assert(self, node):
        return j.Assert(
            random_id(),
            self.__source_before('assert'),
            Markers.EMPTY,
            self.__convert(node.test),
            self.__pad_left(self.__source_before(','), self.__convert(node.msg)) if node.msg else None,
        )

    def visit_Assign(self, node):
        if len(node.targets) == 1:
            return j.Assignment(
                random_id(),
                self.__whitespace(),
                Markers.EMPTY,
                self.__convert(node.targets[0]),
                self.__pad_left(self.__source_before('='), self.__convert(node.value)),
                self._type_mapping.type(node.value)
            )
        else:
            return py.ChainedAssignment(
                random_id(),
                self.__whitespace(),
                Markers.EMPTY,
                [self.__pad_list_element(
                    self.__convert(t),
                    i == len(node.targets) - 1,
                    delim='=',
                    end_delim='=',
                    pad_last=True) for i, t in enumerate(node.targets)],
                self.__convert(node.value),
                self._type_mapping.type(node.value)
            )

    def visit_AugAssign(self, node):
        return j.AssignmentOperation(
            random_id(),
            self.__whitespace(),
            Markers.EMPTY,
            self.__convert(node.target),
            self._map_assignment_operator(node.op),
            self.__convert(node.value),
            self._type_mapping.type(node)
        )

    def visit_Await(self, node):
        return py.Await(
            random_id(),
            self.__source_before('await'),
            Markers.EMPTY,
            self.__convert(node.value),
            self._type_mapping.type(node)
        )

    def visit_Interactive(self, node):
        raise NotImplementedError("Implement visit_Interactive!")

    def visit_AsyncFunctionDef(self, node):
        return self.visit_FunctionDef(node)

    def visit_ClassDef(self, node):
        prefix = self.__whitespace()
        decorators = [self.__map_decorator(d) for d in node.decorator_list]
        kind_prefix = self.__source_before('class')
        name = self.__convert_name(node.name)
        save_cursor = self._cursor
        interfaces_prefix = self.__whitespace()
        if (node.bases or node.keywords) and self.__cursor_at('('):
            all = node.bases + node.keywords
            self.__skip('(')
            interfaces = JContainer(
                interfaces_prefix,
                [
                    self.__pad_list_element(self.__convert_type(n), i == len(all) - 1, end_delim=')') for i, n in
                    enumerate(all)],
                Markers.EMPTY
            )
        elif self.__cursor_at('('):
            self.__skip('(')
            interfaces = JContainer(
                interfaces_prefix,
                [self.__pad_right(j.Empty(random_id(), self.__source_before(')'), Markers.EMPTY), Space.EMPTY)],
                Markers.EMPTY
            )
        else:
            interfaces = None
            self._cursor = save_cursor
        return j.ClassDeclaration(
            random_id(),
            prefix,
            Markers.EMPTY,
            decorators,
            [],  # TODO modifiers
            j.ClassDeclaration.Kind(
                random_id(),
                kind_prefix,
                Markers.EMPTY,
                [],
                j.ClassDeclaration.Kind.Type.Class
            ),
            name,
            None,
            None,
            None,  # no `extends`, all in `implements`
            interfaces,
            None,
            self.__convert_block(node.body),
            self._type_mapping.type(node)
        )

    def visit_Delete(self, node):
        return py.Del(
            random_id(),
            self.__source_before('del'),
            Markers.EMPTY,
            [self.__pad_list_element(self.__convert(e), last=i == len(node.targets) - 1) for i, e in
             enumerate(node.targets)]
        )

    def visit_AnnAssign(self, node):
        prefix = self.__whitespace()

        if node.value:
            return j.Assignment(
                random_id(),
                prefix,
                Markers.EMPTY,
                py.TypeHintedExpression(
                    random_id(),
                    self.__whitespace(),
                    Markers.EMPTY,
                    self.__convert(node.target),
                    py.TypeHint(
                        random_id(),
                        self.__source_before(':'),
                        Markers.EMPTY,
                        self.__convert_type(node.annotation),
                        self._type_mapping.type(node.annotation)
                    ),
                    self._type_mapping.type(node)
                ),
                self.__pad_left(
                    self.__source_before('='),
                    self.__convert(node.value)
                ) if node.value else None,
                self._type_mapping.type(node)
            )
        elif not node.value:
            return py.ExpressionStatement(
                random_id(),
                py.TypeHintedExpression(
                    random_id(),
                    prefix,
                    Markers.EMPTY,
                    self.__convert(node.target),
                    py.TypeHint(
                        random_id(),
                        self.__source_before(':'),
                        Markers.EMPTY,
                        self.__convert_type(node.annotation),
                        self._type_mapping.type(node.annotation)
                    ),
                    self._type_mapping.type(node)
                )
            )
        else:
            name = cast(j.Identifier, self.__convert(node.target))
            if node.annotation:
                after = self.__source_before(':')
                type = self.__convert_type(node.annotation)
            else:
                after = Space.EMPTY
                type = None

            initializer = self.__pad_left(
                self.__source_before('='),
                self.__convert(node.value)
            ) if node.value else None

            return j.VariableDeclarations(
                random_id(),
                prefix,
                Markers.EMPTY,
                [],
                [],
                type,
                None,
                [],
                [self.__pad_right(
                    j.VariableDeclarations.NamedVariable(
                        random_id(),
                        Space.EMPTY,
                        Markers.EMPTY,
                        name,
                        [],
                        initializer,
                        self._type_mapping.type(node.target)),
                    after
                )]
            )

    def visit_For(self, node):
        prefix = self.__source_before('for')
        target = self.__convert(node.target)
        # Wrap target in ExpressionStatement so it can be used as a Statement
        wrapped_target = py.ExpressionStatement(random_id(), target)
        in_prefix = self.__source_before('in')
        iterable = self.__convert(node.iter)
        body = self.__convert_block(node.body)

        control = j.ForEachLoop.Control(
            random_id(),
            Space.EMPTY,  # No parentheses in Python, so no prefix space for control
            Markers.EMPTY,
            self.__pad_right(wrapped_target, in_prefix),  # Right padding has space before 'in'
            self.__pad_right(iterable, Space.EMPTY)  # ':' comes from body's Block prefix
        )

        loop = j.ForEachLoop(
            random_id(),
            prefix,
            Markers.EMPTY,
            control,
            self.__pad_right(body, Space.EMPTY)
        )

        return loop if not node.orelse else py.TrailingElseWrapper(
            random_id(),
            loop.prefix,
            Markers.EMPTY,
            loop.replace(prefix=Space.EMPTY),
            self.__pad_left(
                self.__source_before('else'),
                self.__convert_block(node.orelse)
            )
        )

    def visit_AsyncFor(self, node):
        return py.Async(
            random_id(),
            self.__source_before('async'),
            Markers.EMPTY,
            self.visit_For(node)
        )

    def visit_While(self, node):
        while_ = j.WhileLoop(
            random_id(),
            self.__source_before('while'),
            Markers.EMPTY,
            j.ControlParentheses(
                random_id(),
                self.__whitespace(),
                Markers.EMPTY,
                self.__pad_right(self.__convert(node.test), Space.EMPTY)
            ),
            self.__pad_right(self.__convert_block(node.body), Space.EMPTY)
        )

        return while_ if not node.orelse else py.TrailingElseWrapper(
            random_id(),
            while_.prefix,
            Markers.EMPTY,
            while_.replace(prefix=Space.EMPTY),
            self.__pad_left(
                self.__source_before('else'),
                self.__convert_block(node.orelse)
            )
        )

    def visit_If(self, node):
        prefix = self.__source_before('if')
        condition = j.ControlParentheses(random_id(), self.__whitespace(), Markers.EMPTY,
                                         self.__pad_right(self.__convert(node.test), Space.EMPTY))
        then = self.__pad_right(self.__convert_block(node.body), Space.EMPTY)
        elze = None
        if len(node.orelse) > 0:
            else_prefix = self.__whitespace()
            if len(node.orelse) == 1 and isinstance(node.orelse[0], ast.If) and self._source.startswith('elif',
                                                                                                        self._cursor):
                is_elif = True
                self._cursor += 2
            else:
                is_elif = False
                self._cursor += 4

            elze = j.If.Else(
                random_id(),
                else_prefix,
                Markers.EMPTY,
                self.__pad_statement(node.orelse[0]) if is_elif else self.__pad_right(
                    self.__convert_block(node.orelse), Space.EMPTY
                )
            )
        return j.If(
            random_id(),
            prefix,
            Markers.EMPTY,
            condition,
            then,
            elze
        )

    def visit_With(self, node):
        prefix = self.__source_before('with')
        items_prefix = self.__whitespace()

        parenthesized = self.__cursor_at('(')
        parens_handler = self.__push_parentheses(node, items_prefix, self._cursor) if parenthesized else None

        resources = [self.__pad_list_element(self.__convert(r), i == len(node.items) - 1) for i, r in
                     enumerate(node.items)]

        if parenthesized and self._parentheses_stack and self._parentheses_stack[-1] is parens_handler:
            self._cursor += 1
            resources_container = self._parentheses_stack.pop()[0](
                JContainer(items_prefix, resources, Markers.EMPTY),
                Space.EMPTY
            )
        else:
            resources_container = JContainer(
                items_prefix if not parenthesized else Space.EMPTY,
                resources,
                Markers.build(random_id(), [OmitParentheses(random_id())])
            )

        return j.Try(
            random_id(),
            prefix,
            Markers.EMPTY,
            resources_container,
            self.__convert_block(node.body),
            [],
            None
        )

    def visit_withitem(self, node):
        prefix = self.__whitespace()
        expr = self.__convert(node.context_expr)
        if node.optional_vars:
            value = self.__pad_left(self.__source_before('as'), expr)
            name = self.__convert(node.optional_vars)
            var = j.Assignment(
                random_id(),
                Space.EMPTY,
                Markers.EMPTY,
                name,
                value,
                self._type_mapping.type(node.context_expr)
            )
        else:
            var = expr

        if not isinstance(var, TypedTree):
            var = py.ExpressionTypeTree(
                random_id(),
                Space.EMPTY,
                Markers.EMPTY,
                var
            )

        return j.Try.Resource(
            random_id(),
            prefix,
            Markers.EMPTY,
            var,
            False
        )

    def visit_AsyncWith(self, node):
        return py.Async(
            random_id(),
            self.__source_before('async'),
            Markers.EMPTY,
            self.visit_With(node)
        )

    def visit_Raise(self, node):
        prefix = self.__source_before('raise')
        if node.cause:
            exc = py.ErrorFrom(
                random_id(),
                self.__whitespace(),
                Markers.EMPTY,
                self.__convert(node.exc),
                self.__pad_left(self.__source_before('from'), self.__convert(node.cause)),
                self._type_mapping.type(node)
            )
        elif node.exc:
            exc = self.__convert(node.exc)
        else:
            exc = j.Empty(random_id(), Space.EMPTY, Markers.EMPTY)

        return j.Throw(
            random_id(),
            prefix,
            Markers.EMPTY,
            exc,
        )

    def visit_Try(self, node):
        prefix = self.__source_before('try')
        body = self.__convert_block(node.body)
        handlers = [cast(j.Try.Catch, self.__convert(handler)) for handler in node.handlers]
        if node.orelse:
            else_block = self.__pad_left(
                self.__source_before('else'),
                self.__convert_block(node.orelse)
            )

        finally_ = self.__pad_left(self.__source_before('finally'),
                                   self.__convert_block(node.finalbody)) if node.finalbody else None
        try_ = j.Try(random_id(), prefix, Markers.EMPTY, JContainer.empty(), body, handlers, finally_)

        return try_ if not node.orelse else py.TrailingElseWrapper(
            random_id(),
            try_.prefix,
            Markers.EMPTY,
            try_.replace(prefix=Space.EMPTY),
            else_block
        )

    def visit_Import(self, node):
        # TODO only use `MultiImport` when necessary (requires corresponding changes to printer)
        return py.MultiImport(
            random_id(),
            self.__source_before('import'),
            Markers.EMPTY,
            None,
            False,
            JContainer(
                Space.EMPTY,
                [self.__pad_list_element(self.__convert(n), i == len(node.names) - 1, pad_last=False) for i, n in
                 enumerate(node.names)],
                Markers.EMPTY
            )
        )

    def visit_ImportFrom(self, node):
        prefix = self.__source_before('from')
        from_ = self.__pad_right(self.__convert_name(('.' * node.level) + (node.module if node.module else '')),
                                 self.__source_before('import'))
        names_prefix = self.__whitespace()
        if parenthesized := self._source[self._cursor] == '(':
            self.__skip('(')
        multi_import = py.MultiImport(
            random_id(),
            prefix,
            Markers.EMPTY,
            from_,
            parenthesized,
            JContainer(
                names_prefix,
                [self.__pad_list_element(self.__convert(n), i == len(node.names) - 1) for i, n in
                 enumerate(node.names)],
                Markers.EMPTY
            )
        )
        if parenthesized:
            self.__skip(')')
        return multi_import

    def visit_alias(self, node):
        return j.Import(
            random_id(),
            self.__whitespace(),
            Markers.EMPTY,
            self.__pad_left(Space.EMPTY, False),
            self.__convert_qualified_name(node.name),
            None if not node.asname else
            self.__pad_left(self.__source_before('as'), self.__convert_name(node.asname))
        )

    def visit_keyword(self, node):
        if node.arg:
            return py.NamedArgument(
                random_id(),
                self.__whitespace(),
                Markers.EMPTY,
                self.__convert_name(node.arg),
                self.__pad_left(self.__source_before('='), self.__convert(node.value)),
                self._type_mapping.type(node)
            )
        prefix = self.__whitespace()
        if self._source.startswith('**', self._cursor):
            self.__skip('**')
            return py.Star(
                random_id(),
                prefix,
                Markers.EMPTY,
                py.Star.Kind.DICT,
                self.__convert(node.value),
                self._type_mapping.type(node.value),
            )

    def __convert_qualified_name(self, name: str) -> j.FieldAccess:
        if '.' not in name:
            return j.FieldAccess(
                random_id(),
                self.__whitespace(),
                Markers.EMPTY,
                j.Empty(random_id(), Space.EMPTY, Markers.EMPTY),
                self.__pad_left(
                    Space.EMPTY,
                    self.__convert_name(name)
                ),
                None
            )
        return cast(j.FieldAccess, self.__convert_name(name))

    def visit_Global(self, node):
        return py.VariableScope(
            random_id(),
            self.__source_before('global'),
            Markers.EMPTY,
            py.VariableScope.Kind.GLOBAL,
            [self.__pad_list_element(
                cast(j.Identifier, self.__convert_name(n)),
                i == len(node.names) - 1,
                pad_last=False) for i, n in enumerate(node.names)
            ]
        )

    def visit_Nonlocal(self, node):
        return py.VariableScope(
            random_id(),
            self.__source_before('nonlocal'),
            Markers.EMPTY,
            py.VariableScope.Kind.NONLOCAL,
            [self.__pad_list_element(
                cast(j.Identifier, self.__convert_name(n)),
                i == len(node.names) - 1,
                pad_last=False) for i, n in enumerate(node.names)
            ]
        )

    def visit_Pass(self, node):
        return py.Pass(
            random_id(),
            self.__source_before('pass'),
            Markers.EMPTY,
        )

    def visit_Break(self, node):
        return j.Break(random_id(), self.__source_before('break'), Markers.EMPTY, None)

    def visit_Continue(self, node):
        return j.Continue(random_id(), self.__source_before('continue'), Markers.EMPTY, None)

    def visit_GeneratorExp(self, node):
        # this weird logic is here to deal with the case of generator expressions appearing as the argument to a call
        prefix = self.__whitespace()
        if self._source[self._cursor] == '(':
            save_cursor = self._cursor
            self._cursor += 1
            try:
                result = self.__convert(node.elt)
                save_cursor_2 = self._cursor
                self.__whitespace()
                assert self._source[self._cursor] != ')'
                self._cursor = save_cursor_2
                parenthesized = True
            except:
                self._cursor = save_cursor
                result = self.__convert(node.elt)
                parenthesized = False
        else:
            result = self.__convert(node.elt)
            parenthesized = False

        return py.ComprehensionExpression(
            random_id(),
            prefix,
            Markers.EMPTY if parenthesized else Markers.EMPTY.replace(markers=[OmitParentheses(random_id())]),
            py.ComprehensionExpression.Kind.GENERATOR,
            result,
            cast(List[py.ComprehensionExpression.Clause], [self.__convert(g) for g in node.generators]),
            self.__source_before(')') if parenthesized else Space.EMPTY,
            self._type_mapping.type(node)
        )

    def visit_Expr(self, node):
        return self.__convert(node.value)

    def visit_Yield(self, node):
        return py.StatementExpression(
            random_id(),
            j.Yield(
                random_id(),
                self.__source_before('yield'),
                Markers.EMPTY,
                False,
                self.__convert(node.value) if node.value else j.Empty(random_id(), Space.EMPTY, Markers.EMPTY),
            )
        )

    def visit_YieldFrom(self, node):
        return py.StatementExpression(
            random_id(),
            j.Yield(
                random_id(),
                self.__source_before('yield'),
                Markers.EMPTY,
                False,
                py.YieldFrom(
                    random_id(),
                    self.__source_before('from'),
                    Markers.EMPTY,
                    self.__convert(node.value),
                    self._type_mapping.type(node)
                )
            )
        )

    def visit_TypeIgnore(self, node):
        raise NotImplementedError("Implement visit_TypeIgnore!")

    def visit_Attribute(self, node):
        return j.FieldAccess(
            random_id(),
            self.__whitespace(),
            Markers.EMPTY,
            self.__convert(node.value),
            self.__pad_left(self.__source_before('.'), self.__convert_name(node.attr)),
            self._type_mapping.type(node),
        )

    def visit_Del(self, node):
        raise NotImplementedError("Implement visit_Del!")

    def visit_Load(self, node):
        raise NotImplementedError("Implement visit_Load!")

    def visit_Store(self, node):
        raise NotImplementedError("Implement visit_Store!")

    def visit_ExceptHandler(self, node):
        prefix = self.__source_before('except')
        type_prefix = self.__whitespace()
        except_type = self.__convert_type(node.type) if node.type else j.Empty(random_id(), Space.EMPTY,
                                                                               Markers.EMPTY)
        if node.name:
            before_as = self.__source_before('as')
            except_type_name = self.__convert_name(node.name)
        else:
            before_as = Space.EMPTY
            except_type_name = j.Identifier(random_id(), Space.EMPTY, Markers.EMPTY, [], '', None, None)
        except_type_name = self.__pad_right(
            j.VariableDeclarations.NamedVariable(
                random_id(),
                Space.EMPTY,
                Markers.EMPTY,
                except_type_name,
                [], None, None
            ),
            before_as
        )

        return j.Try.Catch(
            random_id(),
            prefix,
            Markers.EMPTY,
            j.ControlParentheses(
                random_id(),
                Space.EMPTY,
                Markers.EMPTY,
                self.__pad_right(j.VariableDeclarations(
                    random_id(),
                    type_prefix,
                    Markers.EMPTY,
                    [], [],
                    except_type,
                    None, [],
                    [except_type_name]
                ), Space.EMPTY)
            ),
            self.__convert_block(node.body)
        )

    def visit_Match(self, node):
        return j.Switch(
            random_id(),
            self.__source_before('match'),
            Markers.EMPTY,
            j.ControlParentheses(
                random_id(),
                self.__whitespace(),
                Markers.EMPTY,
                self.__pad_right(self.__convert(node.subject), Space.EMPTY)
            ),
            self.__convert_block(node.cases)
        )

    def visit_match_case(self, node):
        prefix = self.__source_before('case')
        pattern_prefix = self.__whitespace()

        pattern = self.__convert(node.pattern)
        if isinstance(pattern, py.MatchCase) and node.guard:
            guard = self.__pad_left(self.__source_before('if'), self.__convert(node.guard))
            pattern = pattern.padding.replace(guard=guard)

        return j.Case(
            random_id(),
            prefix,
            Markers.EMPTY,
            j.Case.Type.Rule,
            JContainer(
                pattern_prefix,
                [self.__pad_right(pattern, Space.EMPTY)],
                Markers.EMPTY
            ),
            JContainer.empty(),
            self.__pad_right(self.__convert_block(node.body), Space.EMPTY),
            None
        )

    def visit_MatchValue(self, node):
        return self.__convert(node.value)

    def visit_MatchSequence(self, node):
        prefix = self.__whitespace()
        end_delim = None
        if self._source[self._cursor] == '[':
            kind = py.MatchCase.Pattern.Kind.SEQUENCE_LIST
            self._cursor += 1
            end_delim = ']'
        elif self._source[self._cursor] == '(':
            kind = py.MatchCase.Pattern.Kind.SEQUENCE_TUPLE
            self._cursor += 1
            end_delim = ')'
        else:
            kind = py.MatchCase.Pattern.Kind.SEQUENCE
        return py.MatchCase(
            random_id(),
            Space.EMPTY,
            Markers.EMPTY,
            py.MatchCase.Pattern(
                random_id(),
                Space.EMPTY,
                Markers.EMPTY,
                kind,
                JContainer(
                    prefix,
                    [self.__pad_list_element(self.__convert(e), last=i == len(node.patterns) - 1,
                                             end_delim=end_delim) for i, e in
                     enumerate(node.patterns)] if node.patterns else [],
                    Markers.EMPTY
                ),
                None
            ),
            None,
            None
        )

    def visit_MatchSingleton(self, node):
        raise NotImplementedError("Implement visit_MatchSingleton!")

    def visit_MatchStar(self, node):
        return py.Star(
            random_id(),
            self.__source_before('*'),
            Markers.EMPTY,
            py.Star.Kind.LIST,
            self.__convert_name(node.name),
            None
        )

    def visit_MatchMapping(self, node):
        return py.MatchCase(
            random_id(),
            self.__whitespace(),
            Markers.EMPTY,
            py.MatchCase.Pattern(
                random_id(),
                Space.EMPTY,
                Markers.EMPTY,
                py.MatchCase.Pattern.Kind.MAPPING,
                JContainer(
                    self.__source_before('{'),
                    [self.__pad_list_element(py.MatchCase.Pattern(
                        random_id(),
                        Space.EMPTY,
                        Markers.EMPTY,
                        py.MatchCase.Pattern.Kind.KEY_VALUE,
                        JContainer(
                            Space.EMPTY,
                            [
                                self.__pad_right(self.__convert(node.keys[i]), self.__source_before(':')),
                                self.__pad_right(self.__convert(node.patterns[i]), Space.EMPTY),
                            ],
                            Markers.EMPTY
                        ),
                        None
                    ), last=i == len(node.patterns) - 1, end_delim='}') for i, e in
                        enumerate(node.patterns)] if node.patterns else [],
                    Markers.EMPTY
                ),
                None
            ),
            None,
            None
        )

    def visit_MatchClass(self, node):
        prefix = self.__whitespace()
        children = [self.__pad_right(self.__convert(node.cls), self.__source_before('('))]
        if len(node.patterns) > 0:
            for i, arg in enumerate(node.patterns):
                arg_name = j.VariableDeclarations(
                    random_id(),
                    self.__whitespace(),
                    Markers.EMPTY,
                    [], [], None, None, [],
                    [
                        self.__pad_right(j.VariableDeclarations.NamedVariable(
                            random_id(),
                            Space.EMPTY,
                            Markers.EMPTY,
                            cast(j.Identifier, self.__convert(arg)),
                            [],
                            None,
                            None
                        ), Space.EMPTY)
                    ]
                )
                converted = self.__pad_list_element(arg_name, last=i == len(node.patterns) - 1,
                                                    end_delim=')' if len(node.kwd_attrs) == 0 else ',')
                children.append(converted)
            for i, kwd in enumerate(node.kwd_attrs):
                kwd_var = j.VariableDeclarations(
                    random_id(),
                    self.__whitespace(),
                    Markers.EMPTY,
                    [], [], None, None, [],
                    [
                        self.__pad_right(j.VariableDeclarations.NamedVariable(
                            random_id(),
                            Space.EMPTY,
                            Markers.EMPTY,
                            cast(j.Identifier, self.__convert_name(kwd)),
                            [],
                            self.__pad_left(self.__source_before('='), self.__convert(node.kwd_patterns[i])),
                            None
                        ), Space.EMPTY)
                    ]
                )
                converted = self.__pad_list_element(kwd_var, last=i == len(node.kwd_attrs) - 1,
                                                    end_delim=')')
                children.append(converted)
        else:
            children.append(
                self.__pad_right(j.Empty(random_id(), self.__source_before(')'), Markers.EMPTY), Space.EMPTY))
        return py.MatchCase(
            random_id(),
            prefix,
            Markers.EMPTY,
            py.MatchCase.Pattern(
                random_id(),
                Space.EMPTY,
                Markers.EMPTY,
                py.MatchCase.Pattern.Kind.CLASS,
                JContainer(Space.EMPTY, children, Markers.EMPTY),
                None
            ),
            None,
            None
        )

    def visit_MatchAs(self, node):
        if node.name is None and node.pattern is None:
            return py.MatchCase(
                random_id(),
                self.__source_before('_'),
                Markers.EMPTY,
                py.MatchCase.Pattern(
                    random_id(),
                    Space.EMPTY,
                    Markers.EMPTY,
                    py.MatchCase.Pattern.Kind.WILDCARD,
                    JContainer.empty(),
                    None
                ),
                None,
                None
            )
        elif node.pattern:
            return py.MatchCase(
                random_id(),
                self.__whitespace(),
                Markers.EMPTY,
                py.MatchCase.Pattern(
                    random_id(),
                    Space.EMPTY,
                    Markers.EMPTY,
                    py.MatchCase.Pattern.Kind.AS,
                    JContainer(
                        Space.EMPTY,
                        [
                            self.__pad_right(self.__convert(node.pattern), self.__source_before('as')),
                            self.__pad_right(self.__convert_name(node.name), Space.EMPTY),
                        ],
                        Markers.EMPTY
                    ),
                    None
                ),
                None,
                None
            )
        return self.__convert_name(node.name)

    def visit_MatchOr(self, node):
        return py.MatchCase(
            random_id(),
            self.__whitespace(),
            Markers.EMPTY,
            py.MatchCase.Pattern(
                random_id(),
                Space.EMPTY,
                Markers.EMPTY,
                py.MatchCase.Pattern.Kind.OR,
                JContainer(
                    Space.EMPTY,
                    [self.__pad_list_element(self.__convert(e), last=i == len(node.patterns) - 1) for i, e in
                     enumerate(node.patterns)] if node.patterns else [],
                    Markers.EMPTY
                ),
                None
            ),
            None,
            None
        )

    def visit_TryStar(self, node):
        raise NotImplementedError("Implement visit_TryStar!")

    def visit_TypeVar(self, node):
        raise NotImplementedError("Implement visit_TypeVar!")

    def visit_ParamSpec(self, node):
        raise NotImplementedError("Implement visit_ParamSpec!")

    def visit_TypeVarTuple(self, node):
        raise NotImplementedError("Implement visit_TypeVarTuple!")

    def visit_TypeAlias(self, node):
        return py.TypeAlias(
            random_id(),
            self.__source_before("type"),
            Markers.EMPTY,
            self.__convert(node.name),
            self.__pad_left(self.__source_before('='), self.__convert(node.value)),
            self._type_mapping.type(node)
        )

    def visit_ExtSlice(self, node):
        raise NotImplementedError("Implement visit_ExtSlice!")

    def visit_Index(self, node):
        raise NotImplementedError("Implement visit_Index!")

    def visit_Suite(self, node):
        raise NotImplementedError("Implement visit_Suite!")

    def visit_AugLoad(self, node):
        raise NotImplementedError("Implement visit_AugLoad!")

    def visit_AugStore(self, node):
        raise NotImplementedError("Implement visit_AugStore!")

    def visit_Param(self, node):
        raise NotImplementedError("Implement visit_Param!")

    def visit_Num(self, node):
        raise NotImplementedError("Implement visit_Num!")

    def visit_Str(self, node):
        raise NotImplementedError("Implement visit_Str!")

    def visit_Bytes(self, node):
        raise NotImplementedError("Implement visit_Bytes!")

    def visit_NameConstant(self, node):
        raise NotImplementedError("Implement visit_NameConstant!")

    def visit_Ellipsis(self, node):
        raise NotImplementedError("Implement visit_Ellipsis!")

    def visit_BinOp(self, node):
        prefix = self.__whitespace()
        left = self.__convert(node.left)
        op = self.__convert_binary_operator(node.op)

        if isinstance(op.element, py.Binary.Type):
            return py.Binary(
                random_id(),
                prefix,
                Markers.EMPTY,
                left,
                op,
                None,
                self.__convert(node.right),
                self._type_mapping.type(node)
            )
        else:
            return j.Binary(
                random_id(),
                prefix,
                Markers.EMPTY,
                left,
                op,
                self.__convert(node.right),
                self._type_mapping.type(node)
            )

    def visit_BoolOp(self, node):
        binaries = []
        prefix = Space.EMPTY
        left = self.__convert(node.values[0])
        for right_expr in node.values[1:]:
            left = j.Binary(
                random_id(),
                prefix,
                Markers.EMPTY,
                left,
                self.__convert_binary_operator(node.op),
                self.__convert(right_expr),
                self._type_mapping.type(node)
            )
            binaries.append(left)
            prefix = Space.EMPTY

        return binaries[-1]

    def visit_Call(self, node):
        prefix = self.__whitespace()
        if isinstance(node.func, ast.Name):
            select = None
            name = cast(j.Identifier, self.__convert(node.func))
        elif isinstance(node.func, ast.Attribute):
            select = self.__pad_right(self.__convert(node.func.value), self.__source_before('.'))
            name = j.Identifier(
                random_id(),
                self.__source_before(node.func.attr),
                Markers.EMPTY,
                [],
                node.func.attr,
                self._type_mapping.type(node),
                None
            )
            save_cursor = self._cursor
            parens_right_padding = self.__whitespace()
            if self.__cursor_at(')'):
                name = j.FieldAccess(
                    random_id(),
                    Space.EMPTY,
                    Markers.EMPTY,
                    select.element,
                    JLeftPadded(select.after, name, Markers.EMPTY),
                    None
                )
                while len(self._parentheses_stack) > 0 and self.__cursor_at(')'):
                    self._cursor += 1
                    name = self._parentheses_stack.pop()[0](name, parens_right_padding)
                    save_cursor = self._cursor
                    parens_right_padding = self.__whitespace()
            self._cursor = save_cursor
        else:
            select = self.__pad_right(cast(Expression, self.__convert(node.func)), self.__whitespace())
            # printer handles empty name by not printing `.` before it
            name = self.__convert_name('')

        all_args = self.__sort_call_arguments(node)
        args = JContainer(
            self.__source_before('('),
            [self.__pad_list_element(self.__convert(a), last=i == len(all_args) - 1, end_delim=')') for i, a in
             enumerate(all_args)] if all_args else [
                self.__pad_right(j.Empty(random_id(), self.__source_before(')'), Markers.EMPTY),
                                 Space.EMPTY)],
            Markers.EMPTY
        )

        return j.MethodInvocation(
            random_id(),
            prefix,
            Markers.EMPTY,
            select if isinstance(name, j.Identifier) else self.__pad_right(name, Space.EMPTY),
            None,
            name if isinstance(name, j.Identifier) else j.Identifier(random_id(), Space.EMPTY, Markers.EMPTY, [], "",
                                                                     None, None),
            args,
            name.type if isinstance(name.type, JavaType.Method) else None,
        )

    def __sort_call_arguments(self, call: ast.Call) -> List[Union[ast.expr, ast.keyword]]:
        all_args = []

        for arg in call.args:
            all_args.append((arg.lineno, arg.col_offset, arg))

        for kw in call.keywords:
            all_args.append((kw.value.lineno, kw.value.col_offset, kw))

        all_args.sort(key=lambda x: (x[0], x[1]))
        return [arg[2] for arg in all_args]

    def visit_Compare(self, node):
        prefix = self.__whitespace()
        left = self.__convert(node.left)

        for i in range(len(node.ops)):
            op = self.__convert_binary_operator(node.ops[i])

            if isinstance(op.element, j.Binary.Type):
                left = j.Binary(
                    random_id(),
                    Space.EMPTY,
                    Markers.EMPTY,
                    left,
                    op,
                    self.__convert(node.comparators[i]),
                    self._type_mapping.type(node)
                )
            else:
                if op.element == py.Binary.Type.IsNot:
                    negation = self.__source_before('not')
                elif op.element == py.Binary.Type.NotIn:
                    negation = self.__source_before('in')
                else:
                    negation = None

                left = py.Binary(
                    random_id(),
                    Space.EMPTY,
                    Markers.EMPTY,
                    left,
                    op,
                    negation,
                    self.__convert(node.comparators[i]),
                    self._type_mapping.type(node)
                )

        return left.replace(prefix=prefix)

    def __convert_binary_operator(self, op) -> Union[JLeftPadded[j.Binary.Type], JLeftPadded[py.Binary.Type]]:
        operation_map: Dict[Type[ast], Tuple[j.Binary.Type, str]] = {
            ast.Add: (j.Binary.Type.Addition, '+'),
            ast.And: (j.Binary.Type.And, 'and'),
            ast.BitAnd: (j.Binary.Type.BitAnd, '&'),
            ast.BitOr: (j.Binary.Type.BitOr, '|'),
            ast.BitXor: (j.Binary.Type.BitXor, '^'),
            ast.Div: (j.Binary.Type.Division, '/'),
            ast.Eq: (j.Binary.Type.Equal, '=='),
            ast.FloorDiv: (py.Binary.Type.FloorDivision, '//'),
            ast.Gt: (j.Binary.Type.GreaterThan, '>'),
            ast.GtE: (j.Binary.Type.GreaterThanOrEqual, '>='),
            ast.In: (py.Binary.Type.In, 'in'),
            ast.Is: (py.Binary.Type.Is, 'is'),
            ast.IsNot: (py.Binary.Type.IsNot, 'is'),
            ast.LShift: (j.Binary.Type.LeftShift, '<<'),
            ast.Lt: (j.Binary.Type.LessThan, '<'),
            ast.LtE: (j.Binary.Type.LessThanOrEqual, '<='),
            ast.MatMult: (py.Binary.Type.MatrixMultiplication, '@'),
            ast.Mod: (j.Binary.Type.Modulo, '%'),
            ast.Mult: (j.Binary.Type.Multiplication, '*'),
            ast.NotEq: (j.Binary.Type.NotEqual, '!='),
            ast.NotIn: (py.Binary.Type.NotIn, 'not'),
            ast.Or: (j.Binary.Type.Or, 'or'),
            ast.Pow: (py.Binary.Type.Power, '**'),
            ast.RShift: (j.Binary.Type.RightShift, '>>'),
            ast.Sub: (j.Binary.Type.Subtraction, '-'),
        }
        try:
            op, op_str = operation_map[type(op)]
        except KeyError:
            raise ValueError(f"Unsupported operator: {op}")
        return self.__pad_left(self.__source_before(op_str), op)

    def visit_Constant(self, node):
        tokens = peekable(tokenize(BytesIO(self._source[self._cursor:].encode('utf-8')).readline))
        tok = next(tokens)  # skip ENCODING token
        tok = next(tokens)  # skip ENCODING token
        while tok.type in (token.ENCODING, token.NL, token.NEWLINE, token.INDENT, token.DEDENT, token.COMMENT):
            tok = next(tokens)

        if not isinstance(node.value, (str, bytes)):
            return self.__map_literal(node, tok, tokens)[0]

        is_byte_string = tok.string.startswith(('b', "B"))
        res = None
        end_seen = False

        while tok.type in (token.STRING, token.FSTRING_START) and is_byte_string == tok.string.startswith(('b', "B")):
            if tok.type == token.FSTRING_START:
                prefix = self.__whitespace()
                current, tok, _ = self.__map_fstring(node, prefix, tok, tokens)
            else:
                current, tok = self.__map_literal(node, tok, tokens)
                end_seen = current.value.endswith(node.value)

            if res is None:
                res = current
            else:
                res = py.Binary(
                    random_id(),
                    Space.EMPTY,
                    Markers.EMPTY,
                    res,
                    self.__pad_left(Space.EMPTY, py.Binary.Type.StringConcatenation),
                    None,
                    current,
                    self._type_mapping.type(node)
                )

            if end_seen:
                break

            while tok.type in (token.NL, token.NEWLINE, token.INDENT, token.DEDENT, token.COMMENT):
                try:
                    tok = next(tokens)
                except IndentationError:
                    break

        return res

    def __map_literal(self, node, tok, tokens):
        prefix = self.__whitespace()
        start = self._cursor
        self._cursor += len(tok.string)

        return (j.Literal(
            random_id(),
            prefix,
            Markers.EMPTY,
            self.__map_literal_value(node, tok),
            self._source[start:self._cursor],
            None,
            self._type_mapping.type(node),
        ), next(tokens))

    def __map_literal_value(self, node, tok):
        if node.value is Ellipsis:
            return None
        elif isinstance(node.value, (str, bytes)):
            return ast.literal_eval(ast.parse(tok.string, mode='eval').body)
        return node.value

    def visit_Dict(self, node):
        return py.DictLiteral(
            random_id(),
            self.__source_before('{'),
            Markers.EMPTY,
            JContainer(
                Space.EMPTY,
                [self.__pad_right(j.Empty(random_id(), self.__source_before('}'), Markers.EMPTY),
                                  Space.EMPTY)] if not node.keys else
                [self.__map_dict_entry(k, v, i == len(node.keys) - 1) for i, (k, v) in
                 enumerate(zip(node.keys, node.values))],
                Markers.EMPTY
            ),
            self._type_mapping.type(node)
        )

    def visit_DictComp(self, node):
        return py.ComprehensionExpression(
            random_id(),
            self.__source_before('{'),
            Markers.EMPTY,
            py.ComprehensionExpression.Kind.DICT,
            py.KeyValue(
                random_id(),
                self.__whitespace(),
                Markers.EMPTY,
                self.__pad_right(self.__convert(node.key), self.__source_before(':')),
                self.__convert(node.value),
                self._type_mapping.type(node.value)
            ),
            cast(List[py.ComprehensionExpression.Clause], [self.__convert(g) for g in node.generators]),
            self.__source_before('}'),
            self._type_mapping.type(node)
        )

    def __map_dict_entry(self, key: Optional[ast.expr], value: ast.expr, last: bool) -> JRightPadded[J]:
        if key is None:
            element = py.Star(
                random_id(),
                self.__source_before('**'),
                Markers.EMPTY,
                py.Star.Kind.DICT,
                self.__convert(value),
                self._type_mapping.type(value),
            )
        else:
            element = py.KeyValue(random_id(), self.__whitespace(), Markers.EMPTY,
                                  self.__pad_right(self.__convert(key), self.__source_before(':')),
                                  self.__convert(value),
                                  self._type_mapping.type(value))
        return self.__pad_list_element(element, last, end_delim='}')

    def visit_FunctionDef(self, node: ast.FunctionDef) -> j.MethodDeclaration:
        prefix = self.__whitespace()
        decorators = [self.__map_decorator(d) for d in node.decorator_list]

        modifiers = []
        if isinstance(node, ast.AsyncFunctionDef):
            modifiers.append(j.Modifier(
                random_id(),
                self.__source_before('async'),
                Markers.EMPTY,
                'async',
                j.Modifier.Type.Async,
                []
            ))

        def_prefix = self.__source_before('def')
        modifiers.append(j.Modifier(
            random_id(),
            def_prefix,
            Markers.EMPTY,
            'def',
            j.Modifier.Type.Default,
            []
        ))
        name = j.MethodDeclaration.IdentifierWithAnnotations(j.Identifier(
            random_id(),
            self.__source_before(node.name),
            Markers.EMPTY,
            [],
            node.name,
            None,
            None
        ), [])

        params = JContainer(self.__source_before('('), self.visit_arguments(node.args), Markers.EMPTY)
        if node.returns is None:
            return_type = None
        else:
            return_type = py.TypeHint(
                random_id(),
                self.__source_before('->'),
                Markers.EMPTY,
                self.__convert(node.returns),
                self._type_mapping.type(node.returns)
            )
        body = self.__convert_block(node.body)

        return j.MethodDeclaration(
            random_id(),
            prefix,
            Markers.EMPTY,
            decorators,
            modifiers,
            None,
            return_type,
            name,
            params,
            None,
            body,
            None,
            self._type_mapping.type(node),
        )

    def __map_decorator(self, decorator) -> j.Annotation:
        prefix = self.__source_before('@')
        if isinstance(decorator, (ast.Attribute, ast.Name)):
            name = self.__convert(decorator)
            args = None
        elif isinstance(decorator, ast.Call):
            name = self.__convert(decorator.func)
            all_args = decorator.args + decorator.keywords
            args = JContainer(
                self.__source_before('('),
                [self.__pad_right(j.Empty(random_id(), self.__source_before(')'), Markers.EMPTY),
                                  Space.EMPTY)] if not all_args else
                [self.__pad_list_element(self.__convert(a), i == len(all_args) - 1, end_delim=')') for i, a in
                 enumerate(all_args)],
                Markers.EMPTY
            )
        else:
            raise NotImplementedError("Unsupported decorator type: " + str(type(decorator)))

        return j.Annotation(
            random_id(),
            prefix,
            Markers.EMPTY,
            name,
            args
        )

    def visit_IfExp(self, node):
        # TODO check if we actually want to use `J.Ternary` as it requires "reversing" some of the padding
        prefix = self.__whitespace()
        true_expr = self.__convert(node.body)
        true_part = self.__pad_left(self.__source_before('if'), true_expr)
        condition = self.__convert(node.test)
        false_part = self.__pad_left(self.__source_before('else'), self.__convert(node.orelse))
        return j.Ternary(
            random_id(),
            prefix,
            Markers.EMPTY,
            condition,
            true_part,
            false_part,
            self._type_mapping.type(node)
        )

    def visit_JoinedStr(self, node):
        tokens = peekable(tokenize(BytesIO(self._source[self._cursor:].encode('utf-8')).readline))
        next(tokens)  # skip ENCODING token
        while (tok := next(tokens)).type not in (token.FSTRING_START, token.STRING):
            pass

        value_idx = 0
        res = None
        while tok.type in (token.FSTRING_START, token.STRING):
            if tok.type == token.STRING:
                current, tok = self.__map_literal(node.values[value_idx], tok, tokens)
                if current.value.endswith(node.values[value_idx].value):
                    value_idx += 1
            else:
                prefix = self.__whitespace()
                current, tok, value_idx = self.__map_fstring(node, prefix, tok, tokens, value_idx)

            if res is None:
                res = current
            else:
                res = py.Binary(
                    random_id(),
                    Space.EMPTY,
                    Markers.EMPTY,
                    res,
                    self.__pad_left(Space.EMPTY, py.Binary.Type.StringConcatenation),
                    None,
                    current,
                    self._type_mapping.type(node)
                )

            if value_idx >= len(node.values):
                break

            while tok.type in (token.NL, token.NEWLINE, token.INDENT, token.DEDENT, token.COMMENT):
                try:
                    tok = next(tokens)
                except IndentationError:
                    break

        return res

    def visit_FormattedValue(self, node):
        raise ValueError("This method should not be called directly")

    def visit_Lambda(self, node):
        return j.Lambda(
            random_id(),
            self.__source_before('lambda'),
            Markers.EMPTY,
            j.Lambda.Parameters(
                random_id(),
                self.__whitespace(),
                Markers.EMPTY,
                False,
                self.visit_arguments(node.args, with_close_paren=False)
            ),
            self.__source_before(':'),
            self.__convert(node.body),
            self._type_mapping.type(node)
        )

    def visit_List(self, node):
        prefix = self.__source_before('[')
        elements = JContainer(
            Space.EMPTY,
            [self.__pad_list_element(self.__convert(e), last=i == len(node.elts) - 1, end_delim=']') for i, e in
             enumerate(node.elts)] if node.elts else
            [self.__pad_right(j.Empty(random_id(), self.__source_before(']'), Markers.EMPTY), Space.EMPTY)],
            Markers.EMPTY
        )
        return py.CollectionLiteral(
            random_id(),
            prefix,
            Markers.EMPTY,
            py.CollectionLiteral.Kind.LIST,
            elements,
            self._type_mapping.type(node)
        )

    def visit_ListComp(self, node):
        return py.ComprehensionExpression(
            random_id(),
            self.__source_before('['),
            Markers.EMPTY,
            py.ComprehensionExpression.Kind.LIST,
            self.__convert(node.elt),
            cast(List[py.ComprehensionExpression.Clause], [self.__convert(g) for g in node.generators]),
            self.__source_before(']'),
            self._type_mapping.type(node)
        )

    def visit_comprehension(self, node):
        if node.is_async:
            prefix = self.__source_before('async')
            async_ = JRightPadded(True, self.__source_before('for'), Markers.EMPTY)
        else:
            prefix = self.__source_before('for')
            async_ = None

        return py.ComprehensionExpression.Clause(
            random_id(),
            prefix,
            Markers.EMPTY,
            async_,
            self.__convert(node.target),
            self.__pad_left(self.__source_before('in'), self.__convert(node.iter)),
            [self._map_comprehension_condition(i) for i in node.ifs] if node.ifs else []
        )

    def _map_comprehension_condition(self, i):
        return py.ComprehensionExpression.Condition(
            random_id(),
            self.__source_before('if'),
            Markers.EMPTY,
            self.__convert(i)
        )

    def visit_Module(self, node: ast.Module) -> py.CompilationUnit:
        self._type_mapping.resolve_types(node)

        cu = py.CompilationUnit(
            random_id(),
            Space.EMPTY,
            Markers.EMPTY,
            Path("TODO"),
            None,
            None,
            False,
            None,
            [],
            [self.__pad_statement(stmt) for stmt in node.body] if node.body else [
                self.__pad_right(j.Empty(random_id(), Space.EMPTY, Markers.EMPTY), Space.EMPTY)],
            self.__whitespace()
        )
        # assert self._cursor == len(self._source)
        return cu

    def visit_Name(self, node):
        return j.Identifier(
            random_id(),
            self.__source_before(node.id),
            Markers.EMPTY,
            [],
            node.id,
            self._type_mapping.type(node),
            None
        )

    def visit_NamedExpr(self, node):
        return j.Assignment(
            random_id(),
            self.__whitespace(),
            Markers.EMPTY,
            self.__convert(node.target),
            self.__pad_left(self.__source_before(':='), self.__convert(node.value)),
            self._type_mapping.type(node.value)
        )

    def visit_Return(self, node):
        return j.Return(
            random_id(),
            self.__source_before('return'),
            Markers.EMPTY,
            self.__convert(node.value) if node.value else None
        )

    def visit_Set(self, node):
        prefix = self.__source_before('{')
        elements = JContainer(
            Space.EMPTY,
            [self.__pad_list_element(self.__convert(e), last=i == len(node.elts) - 1, end_delim='}') for i, e in
             enumerate(node.elts)] if node.elts else
            [self.__pad_right(j.Empty(random_id(), self.__source_before('}'), Markers.EMPTY), Space.EMPTY)],
            Markers.EMPTY
        )
        return py.CollectionLiteral(
            random_id(),
            prefix,
            Markers.EMPTY,
            py.CollectionLiteral.Kind.SET,
            elements,
            self._type_mapping.type(node)
        )

    def visit_SetComp(self, node):
        return py.ComprehensionExpression(
            random_id(),
            self.__source_before('{'),
            Markers.EMPTY,
            py.ComprehensionExpression.Kind.SET,
            self.__convert(node.elt),
            cast(List[py.ComprehensionExpression.Clause], [self.__convert(g) for g in node.generators]),
            self.__source_before('}'),
            self._type_mapping.type(node)
        )

    def visit_Slice(self, node):
        prefix = self.__whitespace()

        lower_expr = self.__convert(node.lower) if node.lower else None
        right_padding = self.__whitespace(':') if node.lower else Space.EMPTY
        lower = self.__pad_right(lower_expr, right_padding) if lower_expr else None
        self._cursor += 1

        upper_expr = self.__convert(node.upper) if node.upper else None
        right_padding = self.__whitespace()
        upper = self.__pad_right(upper_expr if node.upper else j.Empty(random_id(), Space.EMPTY, Markers.EMPTY), right_padding)
        has_step = self.__cursor_at(':')
        if has_step:
            self._cursor += 1
            step = self.__pad_right(
                self.__convert(node.step) if node.step else j.Empty(random_id(), Space.EMPTY, Markers.EMPTY),
                self.__whitespace(']')) if node.step or has_step else Space.EMPTY
        else:
            step = None

        return py.Slice(
            random_id(),
            prefix,
            Markers.EMPTY,
            lower,
            upper,
            step
        )

    def visit_Starred(self, node):
        return py.Star(
            random_id(),
            self.__source_before('*'),
            Markers.EMPTY,
            py.Star.Kind.LIST,
            self.__convert(node.value),
            self._type_mapping.type(node),
        )

    def visit_Subscript(self, node):
        return j.ArrayAccess(
            random_id(),
            self.__whitespace(),
            Markers.EMPTY,
            self.__convert(node.value),
            j.ArrayDimension(
                random_id(),
                self.__source_before('['),
                Markers.EMPTY,
                self.__pad_right(self.__convert(node.slice), self.__source_before(']'))
            ),
            self._type_mapping.type(node)
        )

    def visit_Tuple(self, node):
        prefix = self.__whitespace()
        save_cursor = self._cursor
        maybe_parens = self.__cursor_at('(')

        if maybe_parens:
            self._cursor += 1
            omit_parens = False
        else:
            self._cursor = save_cursor
            omit_parens = True

        elements = JContainer(
            Space.EMPTY,
            [self.__pad_list_element(self.__convert(e), last=i == len(node.elts) - 1) for i, e in enumerate(node.elts)],
            Markers.EMPTY
        ) if node.elts else JContainer(
            Space.EMPTY,
            [self.__pad_right(j.Empty(random_id(), self.__whitespace(), Markers.EMPTY), Space.EMPTY)],
            Markers.EMPTY
        )

        if len(self._parentheses_stack) > 0 and self.__cursor_at(')'):
            self._cursor += 1
            elements = self._parentheses_stack.pop()[0](elements, Space.EMPTY)
            omit_parens = False
        elif maybe_parens and len(self._parentheses_stack) > 0 and self._parentheses_stack[-1][3] == node:
            elements = self._parentheses_stack.pop()
        else:
            if elements.padding.elements:
                # remove the last element's right padding so that if becomes the prefix of the next LST element
                padding = elements.padding.elements[-1].after
                padding_len = len(padding.whitespace) if padding.whitespace else 0
                for c in padding.comments:
                    padding_len += len(c.text) + 1 + len(c.suffix)
                unpadded_last = elements.padding.elements[-1].replace(after=Space.EMPTY)
                elements = elements.padding.replace(elements=list_map_last(lambda last: unpadded_last, elements.padding.elements))
                self._cursor -= padding_len
            omit_parens = True

        return py.CollectionLiteral(
            random_id(),
            prefix,
            Markers.EMPTY,
            py.CollectionLiteral.Kind.TUPLE,
            elements.replace(markers=
                Markers.build(random_id(), [OmitParentheses(random_id())])) if omit_parens else elements,
            self._type_mapping.type(node)
        )

    def visit_UnaryOp(self, node):
        mapped = self._map_unary_operator(node.op)
        return j.Unary(
            random_id(),
            self.__source_before(mapped[1]),
            Markers.EMPTY,
            self.__pad_left(Space.EMPTY, mapped[0]),
            self.__convert(node.operand),
            self._type_mapping.type(node)
        )

    def __convert_type(self, node) -> Optional[TypeTree]:
        prefix = self.__whitespace()
        converted_type = self.__convert_internal(node, self.__convert_type, self.__convert_type_mapper)
        if isinstance(converted_type, TypeTree):
            return converted_type.replace(prefix=prefix)
        else:
            return py.ExpressionTypeTree(
                random_id(),
                prefix,
                Markers.EMPTY,
                converted_type
            )

    def __convert_type_mapper(self, node) -> Optional[TypeTree]:
        if isinstance(node, ast.Constant):
            if node.value is None or node.value is Ellipsis:
                return py.LiteralType(
                    random_id(),
                    self.__whitespace(),
                    Markers.EMPTY,
                    self.__convert(node),
                    self._type_mapping.type(node)
                )
            else:
                literal = cast(j.Literal, self.__convert(node))
                if literal.value_source.startswith("'''"):
                    quote_style = Quoted.Style.TRIPLE_SINGLE
                elif literal.value_source[0] == "'":
                    quote_style = Quoted.Style.SINGLE
                elif literal.value_source.startswith('"""'):
                    quote_style = Quoted.Style.TRIPLE_DOUBLE
                elif literal.value_source.startswith('"'):
                    quote_style = Quoted.Style.DOUBLE
                else:
                    quote_style = None

                return j.Identifier(
                    random_id(),
                    literal.prefix,
                    Markers.build(random_id(), [Quoted(random_id(), quote_style)]) if quote_style else Markers.EMPTY,
                    [],
                    str(literal.value),
                    self._type_mapping.type(node),
                    None
                )
        elif isinstance(node, ast.Subscript):
            slices = node.slice.elts if isinstance(node.slice, ast.Tuple) and node.slice.elts else [node.slice]
            return j.ParameterizedType(
                random_id(),
                self.__whitespace(),
                Markers.EMPTY,
                self.__convert(node.value),
                JContainer(
                    self.__source_before('['),
                    [self.__pad_list_element(self.__convert_type(s), last=i == len(slices) - 1, end_delim=']') for
                     i, s in
                     enumerate(slices)],
                    Markers.EMPTY
                ),
                None
            )
        elif isinstance(node, ast.BinOp):
            # NOTE: Type unions using `|` was added in Python 3.10
            prefix = self.__whitespace()
            # FIXME consider flattening nested unions
            left = self.__pad_right(self.__convert_internal(node.left, self.__convert_type),
                                    self.__source_before('|'))
            right = self.__pad_right(self.__convert_internal(node.right, self.__convert_type), Space.EMPTY)
            return py.UnionType(
                random_id(),
                prefix,
                Markers.EMPTY,
                [left, right],
                self._type_mapping.type(node)
            )

        return self.__convert_internal(node, self.__convert_type)

    def __convert(self, node) -> Optional[J]:
        return self.__convert_internal(node, self.__convert)

    def __convert_statement(self, node) -> Optional[J]:
        converted = self.__convert_internal(node, self.__convert_statement)
        if isinstance(converted, Statement):
            return converted
        return py.ExpressionStatement(
            random_id(),
            converted
        )

    def __convert_internal(self, node, recursion, mapping = None) -> Optional[J]:
        if not node or not isinstance(node, ast.expr) or isinstance(node, ast.GeneratorExp):
            return self.visit(cast(ast.AST, node)) if node else None

        save_cursor = self._cursor
        prefix = self.__whitespace()

        # Handle normal expression or parenthesized expression
        result = self.__parse_expr(node, mapping or self.visit, recursion, save_cursor, prefix)

        save_cursor_2 = self._cursor
        suffix = self.__whitespace()

        # Process closing parentheses if any
        while self._is_closing_paren(save_cursor):
            self._cursor += 1
            transformer, save_cursor, _, _, _ = self._parentheses_stack.pop()
            result = transformer(result, suffix)
            save_cursor_2 = self._cursor
            suffix = self.__whitespace()

        # Clean up unmatched parentheses for this node
        while len(self._parentheses_stack) > 1 and self._parentheses_stack[-1][3] is node and \
                self._parentheses_stack[-2][3] is not node:
            self._parentheses_stack.pop()

        self._cursor = save_cursor_2
        return result

    def __parse_expr(self, node, mapping, recursion, save_cursor: int, prefix: Space) -> J:
        """Parse either a normal expression or a parenthesized expression."""
        if not (self._cursor < len(self._source) and self._source[self._cursor] == '('):
            self._cursor = save_cursor
            return mapping(cast(ast.AST, node))

        self.__push_parentheses(node, prefix, save_cursor)

        return recursion(node)

    def __push_parentheses(self, node, prefix: Space, save_cursor):
        self._cursor += 1
        expr_prefix = self.__whitespace()
        handler = (
            lambda e, r: (
                (lambda c: c.padding.replace(elements=[
                    padded.replace(element=padded.element.replace(prefix=expr_prefix)) if i == 0 else padded
                    for i, padded in enumerate(c.padding.elements)
                ]))(cast(JContainer, e).replace(before=prefix))
                if isinstance(e, JContainer)
                else j.Parentheses(
                    random_id(),
                    prefix,
                    Markers.EMPTY,
                    self.__pad_right(e.replace(prefix=expr_prefix), r)
                )
            ),
            save_cursor,
            self._cursor,
            node,
            prefix
        )
        self._parentheses_stack.append(handler)
        return handler

    def _is_closing_paren(self, save_cursor: int) -> bool:
        """Check if current position has a valid closing parenthesis."""
        if (not self._parentheses_stack or
                self._cursor >= len(self._source) or
                self._source[self._cursor] != ')'):
            return False

        stack_cursor = self._parentheses_stack[-1][2]
        slice_content = self._source[stack_cursor:save_cursor]
        return stack_cursor == save_cursor or slice_content.isspace() or slice_content == '('

    def __convert_name(self, name: str, name_type: Optional[JavaType] = None) -> NameTree:
        def ident_or_field(parts: List[str]) -> NameTree:
            if len(parts) == 1:
                return j.Identifier(random_id(), self.__source_before(parts[-1]), Markers.EMPTY, [], parts[-1],
                                    name_type, None)
            else:
                return j.FieldAccess(
                    random_id(),
                    self.__whitespace(),
                    Markers.EMPTY,
                    ident_or_field(parts[:-1]),
                    self.__pad_left(
                        self.__source_before('.'),
                        j.Identifier(random_id(), self.__source_before(parts[-1]), Markers.EMPTY, [], parts[-1],
                                     name_type,
                                     None),
                    ),
                    name_type
                )

        return ident_or_field(name.split('.'))

    def __next_lexer_token(self, tokens: Iterator[TokenInfo]) -> TokenInfo:
        tok = next(tokens)
        value_source = tok.string
        self._cursor += len(value_source)
        return tok

    def __convert_all(self, trees: Sequence) -> List[J2]:
        return [self.__convert(tree) for tree in trees]

    def __convert_block(self, statements: Sequence[Statement], prefix: str = ':') -> j.Block:
        prefix = self.__source_before(prefix)
        if statements:
            statements = [self.__pad_statement(cast(ast.stmt, s)) for s in statements]
        else:
            statements = [self.__pad_right(j.Empty(random_id(), Space.EMPTY, Markers.EMPTY), Space.EMPTY)]
        return j.Block(
            random_id(),
            prefix,
            Markers.EMPTY,
            JRightPadded(False, Space.EMPTY, Markers.EMPTY),
            statements,
            Space.EMPTY
        )

    def __pad_statement(self, stmt: ast.stmt) -> JRightPadded[Statement]:
        statement = self.__convert_statement(stmt)
        # use whitespace until end of line as padding; what follows will be the prefix of next element
        # This includes trailing comments on the same line
        padding = self.__whitespace('\n')
        if self._cursor < len(self._source) and self._source[self._cursor] == ';':
            self._cursor += 1
            markers = Markers.build(random_id(), [Semicolon(random_id())])
        else:
            markers = Markers.EMPTY
        return JRightPadded(statement, padding, markers)

    def __pad_list_element(self, element: J2, last: bool = False, pad_last: bool = True, delim: str = ',',
                           end_delim: Optional[str] = None) -> JRightPadded[J2]:
        save_cursor = self._cursor
        padding = self.__whitespace() if pad_last or not last else Space.EMPTY
        markers = Markers.EMPTY
        if last and self._cursor < len(self._source):
            if self._source[self._cursor] == delim and end_delim != delim:
                self._cursor += len(delim)
                markers = markers.replace(markers=[TrailingComma(random_id(), self.__whitespace())])
            elif self._source[self._cursor] != end_delim:
                if not pad_last:
                    self._cursor = save_cursor
            if end_delim and self._source[self._cursor] == end_delim:
                self._cursor += len(end_delim)
        elif last:
            padding = Space.EMPTY
            self._cursor = save_cursor
        elif not last:
            self._cursor += len(delim)
            markers = Markers.EMPTY
        return JRightPadded(element, padding, markers)

    def __pad_right(self, tree, space: Space) -> JRightPadded[J2]:
        return JRightPadded(tree, space, Markers.EMPTY)

    def __pad_left(self, space: Space, tree) -> JLeftPadded[J2]:
        if isinstance(tree, ast.AST):
            raise ArgumentError(tree, "must be a Tree but is a {}".format(type(tree)))
        return JLeftPadded(space, tree, Markers.EMPTY)

    def __source_before(self, until_delim: Optional[str], stop: Optional[str] = None) -> Space:
        if not until_delim:
            return Space.EMPTY

        if self._source.startswith(until_delim, self._cursor):
            self._cursor += len(until_delim)
            return Space.EMPTY

        save_cursor = self._cursor
        space = self.__whitespace()
        if self._source.startswith(until_delim, self._cursor):
            self._cursor += len(until_delim)
            return space
        else:
            self._cursor = save_cursor

        delim_index = self.__position_of_next(until_delim, stop)
        if delim_index == -1:
            return Space.EMPTY

        if delim_index == self._cursor:
            self._cursor += len(until_delim)
            return Space.EMPTY

        space = self.__whitespace()
        self._cursor = delim_index + len(until_delim)
        return space

    def __skip(self, tok: Optional[str]) -> Optional[str]:
        if tok is None:
            return None
        if self._source.startswith(tok, self._cursor):
            self._cursor += len(tok)
        return tok

    def __whitespace(self, stop: Optional[str] = None) -> Space:
        space, self._cursor = self.__format(self._source, self._cursor, stop)
        return space

    def __format(self, source: str, offset: int, stop: Optional[str] = None) -> Tuple[Space, int]:
        prefix = None
        whitespace = []
        comments: List[Comment] = []
        source_len = len(source)
        while offset < source_len:
            char = source[offset]
            if stop is not None and char == stop:
                break
            if char.isspace() or char == '\\':
                whitespace.append(char)
            elif char == '#':
                if comments:
                    comments[-1] = comments[-1].replace(suffix=''.join(whitespace))
                else:
                    prefix = ''.join(whitespace)
                whitespace = []
                comment = []
                offset += 1
                while offset < source_len and source[offset] not in ['\r', '\n']:
                    comment.append(source[offset])
                    offset += 1
                comments.append(PyComment(''.join(comment), '',
                                          False, Markers.EMPTY))
                continue
            else:
                break
            if offset < source_len:
                offset += 1

        if not whitespace and not comments:
            return Space.EMPTY, offset

        if not comments:
            prefix = ''.join(whitespace)
        elif whitespace:
            comments[-1] = comments[-1].replace(suffix=''.join(whitespace))
        return Space(comments, prefix), offset

    def __position_of_next(self, until_delim: str, stop: Optional[str] = None) -> int:
        in_single_line_comment = False

        delim_index = self._cursor
        end_index = len(self._source) - len(until_delim) + 1
        while delim_index < end_index:
            if in_single_line_comment:
                if self._source[delim_index] == '\n':
                    in_single_line_comment = False
            else:
                if self._source[delim_index] == '#':
                    in_single_line_comment = True

            if not in_single_line_comment:
                if stop is not None and self._source.startswith(stop, delim_index):
                    return -1  # reached stop word before finding the delimiter

                if self._source.startswith(until_delim, delim_index):
                    break  # found it!

            delim_index += 1

        return -1 if delim_index > len(self._source) - len(until_delim) else delim_index

    def _map_unary_operator(self, op) -> Tuple[j.Unary.Type, str]:
        operation_map: Dict[Type[ast], Tuple[j.Unary.Type, str]] = {
            ast.Invert: (j.Unary.Type.Complement, '~'),
            ast.Not: (j.Unary.Type.Not, 'not'),
            ast.UAdd: (j.Unary.Type.Positive, '+'),
            ast.USub: (j.Unary.Type.Negative, '-'),
        }
        return operation_map[type(op)]

    def _map_assignment_operator(self, op):
        operation_map: Dict[Type[ast], Tuple[j.AssignmentOperation.Type, str]] = {
            ast.Add: (j.AssignmentOperation.Type.Addition, '+='),
            ast.BitAnd: (j.AssignmentOperation.Type.BitAnd, '&='),
            ast.BitOr: (j.AssignmentOperation.Type.BitOr, '|='),
            ast.BitXor: (j.AssignmentOperation.Type.BitXor, '^='),
            ast.Div: (j.AssignmentOperation.Type.Division, '/='),
            ast.Pow: (j.AssignmentOperation.Type.Exponentiation, '**='),
            ast.FloorDiv: (j.AssignmentOperation.Type.FloorDivision, '//='),
            ast.LShift: (j.AssignmentOperation.Type.LeftShift, '<<='),
            ast.MatMult: (j.AssignmentOperation.Type.MatrixMultiplication, '@='),
            ast.Mod: (j.AssignmentOperation.Type.Modulo, '%='),
            ast.Mult: (j.AssignmentOperation.Type.Multiplication, '*='),
            ast.RShift: (j.AssignmentOperation.Type.RightShift, '>>='),
            ast.Sub: (j.AssignmentOperation.Type.Subtraction, '-='),
        }
        try:
            op, op_str = operation_map[type(op)]
        except KeyError:
            raise ValueError(f"Unsupported operator: {op}")
        return self.__pad_left(self.__source_before(op_str), op)

    def __map_fstring(self, node: ast.JoinedStr, prefix: Space, tok: TokenInfo, tokens: peekable, value_idx: int = 0) -> \
            Tuple[J, TokenInfo, int]:
        if tok.type != token.FSTRING_START:
            if len(node.values) == 1 and isinstance(node.values[0], ast.Constant):
                # format specifiers are stored as f-strings in the AST; e.g. `f'{1:n}'`
                format = cast(ast.Constant, node.values[0]).value
                self._cursor += len(format)
                return (j.Literal(
                    random_id(),
                    self.__whitespace(),
                    Markers.EMPTY,
                    format,
                    format,
                    None,
                    JavaType.Primitive.String
                ), next(tokens), 0)
            else:
                delimiter = ''
            consume_end_delim = False
        else:
            delimiter = tok.string
            self._cursor += len(delimiter)
            tok = next(tokens)
            consume_end_delim = True

        # tokenizer tokens: FSTRING_START, FSTRING_MIDDLE, OP, ..., OP, FSTRING_MIDDLE, FSTRING_END
        parts = []
        while tok.type != token.FSTRING_END and value_idx < len(node.values):
            value = node.values[value_idx]
            if tok.type == token.FSTRING_MIDDLE:
                save_cursor = self._cursor
                while True:
                    self._cursor += len(tok.string) + (1 if tok.string.endswith('{') or tok.string.endswith('}') else 0)
                    if (tok := next(tokens)).type != token.FSTRING_MIDDLE:
                        break
                s = self._source[save_cursor:self._cursor]
                parts.append(j.Literal(
                    random_id(),
                    Space.EMPTY,
                    Markers.EMPTY,
                    s,
                    s,
                    None,
                    JavaType.Primitive.String
                ))
                if cast(ast.Constant, value).value == s:
                    value_idx += 1
            elif tok.type == token.OP and tok.string == '{':
                self._cursor += 1
                tok = next(tokens)
                if not isinstance(value, ast.FormattedValue):
                    # this is the case when using the `=` "debug specifier"
                    value_idx += 1
                    value = node.values[value_idx]

                if isinstance(cast(ast.FormattedValue, value).value, ast.JoinedStr):
                    nested, tok, _ = self.__map_fstring(cast(ast.JoinedStr, cast(ast.FormattedValue, value).value),
                                                        Space.EMPTY, tok, tokens)
                    expr = self.__pad_right(
                        nested,
                        Space.EMPTY
                    )
                else:
                    expr = self.__pad_right(
                        self.__convert(cast(ast.FormattedValue, value).value),
                        self.__whitespace()
                    )
                    try:
                        while (tokens.peek()).type not in (token.FSTRING_END, token.FSTRING_MIDDLE):
                            tok = next(tokens)
                            if tok.type == token.OP and tok.string in ('!'):
                                break
                            la_tok = tokens.peek()
                            if tok.type == token.OP and tok.string == '}' and (
                                    la_tok.type in (token.FSTRING_END, token.FSTRING_MIDDLE) or (
                                    la_tok.type == token.OP and la_tok.string == '{')):
                                break
                            if tok.type == token.OP and tok.string == '=' and la_tok.string in ('!', ':', '}'):
                                break
                            if tok.type == token.OP and tok.string == ':' and la_tok.string in ('{'):
                                break
                    except StopIteration:
                        pass

                # debug specifier
                if tok.type == token.OP and tok.string == '=':
                    self._cursor += len(tok.string)
                    tok = next(tokens)
                    debug = self.__pad_right(True, self.__whitespace('\n'))
                else:
                    debug = None

                # conversion specifier
                if tok.type == token.OP and tok.string == '!':
                    self._cursor += len(tok.string)
                    tok = next(tokens)
                    conv = py.FormattedString.Value.Conversion.ASCII if tok.string == 'a' else py.FormattedString.Value.Conversion.STR if tok.string == 's' else py.FormattedString.Value.Conversion.REPR
                    self._cursor += len(tok.string)
                    tok = next(tokens)
                else:
                    conv = None

                # format specifier
                if tok.type == token.OP and tok.string == ':':
                    self._cursor += len(tok.string)
                    format_spec, tok, _ = self.__map_fstring(
                        cast(ast.JoinedStr, cast(ast.FormattedValue, value).format_spec), Space.EMPTY, next(tokens),
                        tokens)
                else:
                    format_spec = None

                parts.append(py.FormattedString.Value(
                    random_id(),
                    Space.EMPTY,
                    Markers.EMPTY,
                    expr,
                    debug,
                    conv,
                    format_spec
                ))
                value_idx += 1
                self._cursor += len(tok.string)
                tok = next(tokens)
            elif tok.type == token.FSTRING_END:
                raise NotImplementedError("Unsupported: String concatenation with f-strings")

        if consume_end_delim:
            self._cursor += len(tok.string)  # FSTRING_END token
            tok = next(tokens)
        elif tok.type == token.FSTRING_MIDDLE and len(tok.string) == 0:
            tok = next(tokens)

        return (py.FormattedString(
            random_id(),
            prefix,
            Markers.EMPTY,
            delimiter,
            parts
        ), tok, value_idx)

    def __cursor_at(self, s: str):
        return self._cursor < len(self._source) and (
                len(s) == 1 and self._source[self._cursor] == s or self._source.startswith(s, self._cursor))
