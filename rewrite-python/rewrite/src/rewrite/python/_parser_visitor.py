import ast
import sys
import token
from argparse import ArgumentError
from io import BytesIO
from pathlib import Path
from tokenize import tokenize, TokenInfo
from typing import Optional, TypeVar, cast, Callable, List, Tuple, Dict, Sequence, Union, Iterable, NamedTuple

from rewrite import random_id, Markers
from rewrite.java import Space, JRightPadded, JContainer, JLeftPadded, JavaType, J, Statement, Semicolon, TrailingComma, \
    NameTree, OmitParentheses, Expression, TypeTree, TypedTree, Comment
from rewrite.java import tree as j
from rewrite.java.support_types import TextComment
from . import tree as py
from .markers import KeywordArguments, KeywordOnlyArguments, Quoted
from .type_mapping import PythonTypeMapping

T = TypeVar('T')
J2 = TypeVar('J2', bound=J)

# Custom token type for whitespace gaps between tokens
WHITESPACE_TOKEN = -1

# F-string token types (Python 3.12+) - define fallbacks for older versions
FSTRING_START = getattr(token, 'FSTRING_START', -2)
FSTRING_MIDDLE = getattr(token, 'FSTRING_MIDDLE', -3)
FSTRING_END = getattr(token, 'FSTRING_END', -4)

# Token types to skip when looking for significant (non-whitespace) tokens
_SKIP_TOKEN_TYPES = (token.NL, token.NEWLINE, token.INDENT, token.DEDENT,
                     token.COMMENT, token.ENCODING, token.ENDMARKER, WHITESPACE_TOKEN)


class _ParenStackEntry(NamedTuple):
    """Entry in the parentheses stack for tracking nested parenthesized expressions."""
    transformer: Callable[[T, Space], T]  # Function to wrap expression in Parentheses
    save_token_idx: int                    # Token index at push time (before whitespace)
    token_idx_after_open: int              # Token index after consuming '('
    node: ast.AST                          # The AST node being parenthesized
    prefix: Space                          # Whitespace before '('
    open_paren_idx: int                    # Token index of the '(' itself


class ParserVisitor(ast.NodeVisitor):
    _source: str
    _parentheses_stack: List[_ParenStackEntry]
    _tokens: List[TokenInfo]
    _token_idx: int
    _paren_pairs: Dict[int, int]
    _bom_marked: bool

    # UTF-8 BOM character
    _BOM = '\ufeff'

    def __init__(self, source: str, file_path: Optional[str] = None):
        super().__init__()
        # Detect and strip UTF-8 BOM if present
        if source.startswith(self._BOM):
            self._bom_marked = True
            source = source[1:]
        else:
            self._bom_marked = False

        self._source = source
        self._parentheses_stack = []
        self._type_mapping = PythonTypeMapping(source, file_path)

        # Pre-compute byte-to-char mappings for lines with multi-byte characters
        self._byte_to_char = self._build_byte_to_char_mapping(source)

        # Token infrastructure - _token_idx is the primary position tracker
        self._tokens, self._paren_pairs = self._build_tokens(
            tokenize(BytesIO(source.encode('utf-8')).readline)
        )
        self._token_idx = 1  # Skip ENCODING token

    @staticmethod
    def _build_byte_to_char_mapping(source: str) -> Optional[Dict[int, List[int]]]:
        """Build byte-to-char offset mappings for lines with multi-byte characters.

        Python 3.8+ AST uses byte offsets for col_offset/end_col_offset,
        but the tokenizer uses character offsets. This mapping enables
        conversion for correct position comparisons.

        Returns None for pure ASCII files (no multi-byte characters).
        Only stores mappings for lines that actually have multi-byte characters,
        since ASCII-only lines have identical byte and character offsets.
        """
        result: Dict[int, List[int]] = {}
        for lineno, line in enumerate(source.splitlines(keepends=True), start=1):
            line_bytes = line.encode('utf-8')
            if len(line_bytes) != len(line):  # Line has multi-byte characters
                mapping = []
                for char_idx, char in enumerate(line):
                    for _ in range(len(char.encode('utf-8'))):
                        mapping.append(char_idx)
                result[lineno] = mapping
        return result if result else None  # Return None for pure ASCII files

    def _byte_offset_to_char_offset(self, lineno: int, byte_offset: int) -> int:
        """Convert a byte offset to a character offset for a given line."""
        if self._byte_to_char is None or lineno not in self._byte_to_char:
            return byte_offset  # ASCII file or ASCII-only line, offsets are identical
        mapping = self._byte_to_char[lineno]
        if byte_offset >= len(mapping):
            return mapping[-1] + 1 if mapping else byte_offset
        return mapping[byte_offset]

    def _build_tokens(self, raw_tokens: Iterable[TokenInfo]) -> Tuple[List[TokenInfo], Dict[int, int]]:
        """Build token list with whitespace tokens and compute paren pairs in one pass."""
        result: List[TokenInfo] = []
        paren_pairs: Dict[int, int] = {}
        paren_stack: List[int] = []
        prev_end = 0  # character offset where previous token ended
        row = 1       # current row (1-based like tokenize)
        col = 0       # current column (0-based like tokenize)
        in_from_import = False  # track if we're between 'from' and 'import' keywords
        fstring_depth = 0  # track nested f-string depth

        for tok in raw_tokens:
            # ENCODING token is virtual (doesn't consume source text)
            if tok.type == token.ENCODING:
                result.append(tok)
                continue

            target_row, target_col = tok.start

            # Scan from prev_end to find tok_start by tracking row/col
            scan = prev_end
            while scan < len(self._source) and (row < target_row or (row == target_row and col < target_col)):
                if self._source[scan] == '\n':
                    row += 1
                    col = 0
                else:
                    col += 1
                scan += 1

            tok_start = scan

            # Insert whitespace token if there's a gap.
            # Inside f-strings, skip gaps that are just escaped braces ({{ or }}) because the tokenizer
            # already decoded them. These gaps contain exactly one '{' or '}' character.
            if tok_start > prev_end:
                ws_text = self._source[prev_end:tok_start]
                # Skip single-character brace gaps inside f-strings (escaped {{ or }})
                is_escaped_brace = fstring_depth > 0 and ws_text in ('{', '}')
                if not is_escaped_brace:
                    ws_tok = TokenInfo(
                        WHITESPACE_TOKEN,
                        ws_text,
                        (0, prev_end),
                        (0, tok_start),
                        ''
                    )
                    result.append(ws_tok)

            # Track f-string depth for whitespace injection.
            if tok.type == FSTRING_START:
                fstring_depth += 1
            elif tok.type == FSTRING_END:
                fstring_depth -= 1

            # Track paren pairs
            if tok.type == token.OP:
                if tok.string == '(':
                    paren_stack.append(len(result))
                elif tok.string == ')' and paren_stack:
                    paren_pairs[paren_stack.pop()] = len(result)

            # Normalize ellipsis '...' into three '.' tokens only in relative imports
            # (between 'from' and 'import' keywords). Elsewhere '...' is the Ellipsis literal.
            if tok.string == '...' and in_from_import:
                for i in range(3):
                    dot_tok = TokenInfo(
                        token.OP,
                        '.',
                        (tok.start[0], tok.start[1] + i),
                        (tok.start[0], tok.start[1] + i + 1),
                        tok.line
                    )
                    result.append(dot_tok)
            else:
                result.append(tok)

            # Track from/import context for ellipsis normalization
            if tok.type == token.NAME:
                if tok.string == 'from':
                    in_from_import = True
                else:
                    # Any other identifier (module name or 'import') ends the context
                    in_from_import = False

            # Update row/col for token content
            for c in tok.string:
                if c == '\n':
                    row += 1
                    col = 0
                else:
                    col += 1
            prev_end = tok_start + len(tok.string)

        return result, paren_pairs

    def _advance_token(self) -> TokenInfo:
        """Advance _token_idx and return the new current token."""
        self._token_idx += 1
        return self._tokens[self._token_idx]

    def _next_token(self) -> TokenInfo:
        """Get the current token and advance _token_idx."""
        tok = self._tokens[self._token_idx]
        self._token_idx += 1
        return tok

    def _skip_whitespace_tokens(self) -> TokenInfo:
        """Skip whitespace tokens and return the next non-whitespace token."""
        while self._token_idx < len(self._tokens):
            tok = self._tokens[self._token_idx]
            if tok.type not in _SKIP_TOKEN_TYPES:
                return tok
            self._token_idx += 1
        return self._tokens[-1]

    def _peek_significant_token(self) -> Tuple[TokenInfo, int]:
        """Peek at next non-whitespace token without advancing _token_idx.

        Returns (token, index) tuple. Useful for lookahead without consuming whitespace.
        """
        idx = self._token_idx
        while idx < len(self._tokens):
            tok = self._tokens[idx]
            if tok.type not in _SKIP_TOKEN_TYPES:
                return tok, idx
            idx += 1
        return self._tokens[-1], len(self._tokens) - 1

    def generic_visit(self, node):
        return super().generic_visit(node)

    def visit_arguments(self, node, with_close_paren: bool = True) -> List[JRightPadded[Statement]]:
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
                            [self.__pad_right(empty_name, self.__source_before(','))]
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

        # Handle type parameters (Python 3.12+ PEP 695)
        type_params = getattr(node, 'type_params', None)
        if type_params:
            type_parameters = JContainer(
                self.__source_before('['),
                [self.__pad_list_element(self.__convert(tp), i == len(type_params) - 1, end_delim=']')
                 for i, tp in enumerate(type_params)],
                Markers.EMPTY
            )
        else:
            type_parameters = None

        save_token_idx = self._token_idx
        interfaces_prefix = self.__whitespace()
        if (node.bases or node.keywords) and self.__skip('('):
            # Sort bases and keywords by source position to preserve original order
            all_items = list(node.bases) + list(node.keywords)
            all_items.sort(key=lambda n: (n.lineno, n.col_offset))
            interfaces = JContainer(
                interfaces_prefix,
                [
                    self.__pad_list_element(self.__convert_type(n), i == len(all_items) - 1, end_delim=')') for i, n in
                    enumerate(all_items)],
                Markers.EMPTY
            )
        elif self.__skip('('):
            interfaces = JContainer(
                interfaces_prefix,
                [self.__pad_right(j.Empty(random_id(), self.__source_before(')'), Markers.EMPTY), Space.EMPTY)],
                Markers.EMPTY
            )
        else:
            interfaces = None
            self._token_idx = save_token_idx
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
            type_parameters,
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
        else:
            # No value - type annotation only (e.g., `x: int`)
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
        # Handle both 'if' and 'elif' keywords - elif is used when this If is nested in an else clause
        if self.__at_token('elif'):
            prefix = self.__source_before('elif')
        else:
            prefix = self.__source_before('if')
        condition = j.ControlParentheses(random_id(), self.__whitespace(), Markers.EMPTY,
                                         self.__pad_right(self.__convert(node.test), Space.EMPTY))
        then = self.__pad_right(self.__convert_block(node.body), Space.EMPTY)
        elze = None
        if len(node.orelse) > 0:
            else_prefix = self.__whitespace()
            # Check if this is an elif (token will be 'elif') vs else (token will be 'else')
            if len(node.orelse) == 1 and isinstance(node.orelse[0], ast.If) and self.__at_token('elif'):
                is_elif = True
                # Don't skip 'elif' here - the recursive visit_If will handle it
            else:
                is_elif = False
                self.__skip('else')

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

        parenthesized = self.__at_token('(')
        parens_handler = self.__push_parentheses(node, items_prefix, self._token_idx) if parenthesized else None

        resources = [self.__pad_list_element(self.__convert(r), i == len(node.items) - 1) for i, r in
                     enumerate(node.items)]

        if parenthesized and self._parentheses_stack and self._parentheses_stack[-1] is parens_handler:
            self._token_idx += 1  # consume ')'
            resources_container = self._parentheses_stack.pop().transformer(
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
        if parenthesized := self.__at_token('('):
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
        if self.__skip('**'):
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
        return self.__visit_variable_scope(node, 'global', py.VariableScope.Kind.GLOBAL)

    def visit_Nonlocal(self, node):
        return self.__visit_variable_scope(node, 'nonlocal', py.VariableScope.Kind.NONLOCAL)

    def __visit_variable_scope(self, node, keyword: str, kind: py.VariableScope.Kind):
        return py.VariableScope(
            random_id(),
            self.__source_before(keyword),
            Markers.EMPTY,
            kind,
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
        parenthesized = False

        # Collect any extra parentheses that wrap the generator expression.
        # Similar to tuples, generators handle all surrounding parentheses here because
        # generator parentheses can be shared with a function call or be explicit.
        # These are '(' tokens that appear before the generator's AST position.
        extra_parens: List[Tuple[Space, Space]] = []  # List of (prefix, inner_prefix) for each extra paren
        # Convert AST byte offset to char offset for comparisons with token positions
        node_char_col = self._byte_offset_to_char_offset(node.lineno, node.col_offset)
        while self.__at_token('('):
            curr_tok = self._tokens[self._token_idx]
            curr_line, curr_col = curr_tok.start
            # If token position is before generator's AST position, it's an extra paren
            if curr_line < node.lineno or (curr_line == node.lineno and curr_col < node_char_col):
                extra_parens.append((prefix, Space.EMPTY))
                self._token_idx += 1  # consume '('
                prefix = self.__whitespace()  # whitespace after the extra '(' becomes the next prefix
                extra_parens[-1] = (extra_parens[-1][0], prefix)
                prefix = Space.EMPTY
            else:
                break

        if self.__at_token('('):
            # Use paren map to determine if this '(' is for a parenthesized generator
            # or for a sub-expression within the generator element.
            # If the '(' closes BEFORE the 'for' keyword, it's a sub-expression.
            open_paren_idx = self._token_idx
            close_paren_idx = self._paren_pairs.get(open_paren_idx)

            # Find token index of 'for' keyword
            for_token_idx = None
            for idx in range(self._token_idx, len(self._tokens)):
                if self._tokens[idx].type == token.NAME and self._tokens[idx].string == 'for':
                    for_token_idx = idx
                    break

            if close_paren_idx is not None and for_token_idx is not None and close_paren_idx < for_token_idx:
                # The '(' closes before 'for', so it's a sub-expression parenthesis.
                # Don't consume it here - let normal parentheses handling deal with it.
                result = self.__convert(node.elt)
                parenthesized = False
            else:
                # Speculatively consume '(' and check if it was the generator's parentheses.
                # After converting the element, if the next significant token is ')', then
                # the '(' we consumed was part of a sub-expression (e.g., inner parenthesized
                # generator), not the outer generator's parentheses.
                save_token_idx = self._token_idx
                self._token_idx += 1  # tentatively consume '('
                result = self.__convert(node.elt)
                next_tok, _ = self._peek_significant_token()
                if next_tok.string == ')':
                    # The '(' was part of the element, not the generator's parens.
                    # Backtrack and re-convert without consuming the '('.
                    self._token_idx = save_token_idx
                    result = self.__convert(node.elt)
                    parenthesized = False
                else:
                    parenthesized = True
        else:
            result = self.__convert(node.elt)
            parenthesized = False

        gen_result: Expression = py.ComprehensionExpression(
            random_id(),
            extra_parens[0][1] if extra_parens else prefix,
            Markers.EMPTY if parenthesized else Markers.EMPTY.replace(markers=[OmitParentheses(random_id())]),
            py.ComprehensionExpression.Kind.GENERATOR,
            result,
            cast(List[py.ComprehensionExpression.Clause], [self.__convert(g) for g in node.generators]),
            self.__source_before(')') if parenthesized else Space.EMPTY,
            self._type_mapping.type(node)
        )

        # Wrap in extra parentheses (in reverse order, innermost first)
        for i in range(len(extra_parens) - 1, -1, -1):
            paren_prefix, inner_prefix = extra_parens[i]
            suffix = self.__whitespace()
            self.__skip(')')  # consume the extra ')'
            # For the outermost paren, use the original prefix
            if i == 0:
                gen_result = j.Parentheses(
                    random_id(),
                    prefix if not extra_parens else paren_prefix,
                    Markers.EMPTY,
                    self.__pad_right(gen_result, suffix)
                )
            else:
                gen_result = j.Parentheses(
                    random_id(),
                    paren_prefix,
                    Markers.EMPTY,
                    self.__pad_right(gen_result, suffix)
                )

        return gen_result

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

    def visit_ExceptHandler(self, node, is_exception_group: bool = False):
        prefix = self.__source_before('except')
        # For except*, consume the '*' after 'except' and let __convert_type capture the space after '*'
        if is_exception_group:
            self.__source_before('*')
            type_prefix = Space.EMPTY  # Space goes on inner type via __convert_type
        else:
            type_prefix = self.__whitespace()
        except_type = self.__convert_type(node.type) if node.type else j.Empty(random_id(), Space.EMPTY,
                                                                               Markers.EMPTY)
        # Wrap in ExceptionType to track exception_group flag
        except_type = py.ExceptionType(
            random_id(),
            Space.EMPTY,
            Markers.EMPTY,
            None,
            is_exception_group,
            except_type
        )
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

        # Use __convert_match_pattern to handle parentheses (GROUP patterns)
        pattern = self.__convert_match_pattern(node.pattern)
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

    def __convert_match_pattern(self, node):
        """Convert a match pattern node, handling parentheses (GROUP patterns).

        Python's AST doesn't have MatchGroup - parentheses are only in tokens.
        We need to detect them and create GROUP patterns to preserve them.
        """
        # Check if we're at an opening parenthesis
        save_idx = self._token_idx
        prefix = self.__whitespace()

        if self.__at_token('('):
            # Check if this is a parenthesized pattern (GROUP)
            # by seeing if the paren closes before the node ends
            close_paren_idx = self._paren_pairs.get(self._token_idx)
            if close_paren_idx is not None and hasattr(node, 'end_lineno') and hasattr(node, 'end_col_offset'):
                close_tok = self._tokens[close_paren_idx]
                close_line, close_col = close_tok.start
                # Convert AST byte offset to character offset for comparison with token position
                end_char_col = self._byte_offset_to_char_offset(node.end_lineno, node.end_col_offset)
                # If closing paren is at or after node's end position, it wraps the whole pattern
                # Note: Use >= because the pattern ends where its content ends, and ) comes right after
                if close_line > node.end_lineno or (close_line == node.end_lineno and close_col >= end_char_col - 1):
                    # This is a GROUP pattern - consume '(' and convert inner pattern
                    self._token_idx += 1  # consume '('
                    inner_prefix = self.__whitespace()
                    inner = self.__convert(node)
                    # Consume ')' and get trailing whitespace
                    self.__source_before(')')

                    # Wrap in GROUP pattern
                    return py.MatchCase(
                        random_id(),
                        Space.EMPTY,
                        Markers.EMPTY,
                        py.MatchCase.Pattern(
                            random_id(),
                            Space.EMPTY,
                            Markers.EMPTY,
                            py.MatchCase.Pattern.Kind.GROUP,
                            JContainer(
                                prefix,
                                [JRightPadded(inner.replace(prefix=inner_prefix) if hasattr(inner, 'replace') else inner, Space.EMPTY, Markers.EMPTY)],  # ty: ignore[call-non-callable]
                                Markers.EMPTY
                            ),
                            None
                        ),
                        None,
                        None
                    )

        # Not a GROUP, restore position and do normal conversion
        self._token_idx = save_idx
        return self.__convert(node)

    def visit_MatchValue(self, node):
        return self.__convert(node.value)

    def visit_MatchSequence(self, node):
        prefix = self.__whitespace()
        end_delim = None
        if self.__skip('['):
            kind = py.MatchCase.Pattern.Kind.SEQUENCE_LIST
            end_delim = ']'
        elif self.__skip('('):
            kind = py.MatchCase.Pattern.Kind.SEQUENCE_TUPLE
            end_delim = ')'
        else:
            kind = py.MatchCase.Pattern.Kind.SEQUENCE

        # Handle elements
        if node.patterns:
            elements = [self.__pad_list_element(self.__convert_match_pattern(e), last=i == len(node.patterns) - 1,
                                                end_delim=end_delim) for i, e in enumerate(node.patterns)]
        else:
            # Empty sequence - need to consume the closing delimiter
            if end_delim:
                self.__source_before(end_delim)
            elements = []

        return py.MatchCase(
            random_id(),
            Space.EMPTY,
            Markers.EMPTY,
            py.MatchCase.Pattern(
                random_id(),
                Space.EMPTY,
                Markers.EMPTY,
                kind,
                JContainer(prefix, elements, Markers.EMPTY),
                None
            ),
            None,
            None
        )

    def visit_MatchSingleton(self, node):
        # MatchSingleton is for None, True, False in match patterns
        # node.value is the actual Python value (not an AST node)
        prefix = self.__whitespace()
        if node.value is None:
            self.__source_before('None')
            name = 'None'
        elif node.value is True:
            self.__source_before('True')
            name = 'True'
        elif node.value is False:
            self.__source_before('False')
            name = 'False'
        else:
            raise ValueError(f"Unexpected MatchSingleton value: {node.value}")
        return j.Identifier(random_id(), prefix, Markers.EMPTY, [], name, None, None)

    def visit_MatchStar(self, node):
        prefix = self.__source_before('*')
        if node.name:
            expression = self.__convert_name(node.name)
        else:
            # For *_ wildcard patterns, AST has name=None but we still need an identifier
            # Check if there's a NAME token '_' next
            if (self._token_idx < len(self._tokens) and
                self._tokens[self._token_idx].type == token.NAME and
                self._tokens[self._token_idx].string == '_'):
                space = self.__whitespace()
                self._token_idx += 1  # consume '_'
                expression = j.Identifier(random_id(), space, Markers.EMPTY, [], '_', None, None)
            else:
                expression = None
        return py.Star(
            random_id(),
            prefix,
            Markers.EMPTY,
            py.Star.Kind.LIST,
            expression,
            None
        )

    def visit_MatchMapping(self, node):
        prefix = self.__whitespace()
        has_rest = node.rest is not None

        # Consume '{' FIRST before iterating over patterns
        brace_prefix = self.__source_before('{')

        # Handle key-value patterns
        elements = []
        for i in range(len(node.patterns)):
            is_last = (i == len(node.patterns) - 1) and not has_rest
            kv_pattern = py.MatchCase.Pattern(
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
            )
            elements.append(self.__pad_list_element(kv_pattern, last=is_last, end_delim='}'))

        # Handle **rest pattern
        if has_rest:
            rest_pattern = py.MatchCase.Pattern(
                random_id(),
                self.__source_before('**'),
                Markers.EMPTY,
                py.MatchCase.Pattern.Kind.DOUBLE_STAR,
                JContainer(
                    Space.EMPTY,
                    [self.__pad_right(self.__convert_name(node.rest), Space.EMPTY)],
                    Markers.EMPTY
                ),
                None
            )
            elements.append(self.__pad_list_element(rest_pattern, last=True, end_delim='}'))

        return py.MatchCase(
            random_id(),
            prefix,
            Markers.EMPTY,
            py.MatchCase.Pattern(
                random_id(),
                Space.EMPTY,
                Markers.EMPTY,
                py.MatchCase.Pattern.Kind.MAPPING,
                JContainer(
                    brace_prefix,
                    elements,
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
        has_positional = len(node.patterns) > 0
        has_keyword = len(node.kwd_attrs) > 0
        if has_positional or has_keyword:
            # Process positional patterns
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
                is_last_positional = i == len(node.patterns) - 1
                converted = self.__pad_list_element(arg_name, last=is_last_positional and not has_keyword,
                                                    end_delim=')' if (is_last_positional and not has_keyword) else ',')
                children.append(converted)
            # Process keyword patterns
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
                            self.__pad_right(self.__convert_match_pattern(node.pattern), self.__source_before('as')),
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
                    # Use '|' as delimiter for OR patterns, not ','
                    [self.__pad_list_element(self.__convert_match_pattern(e), last=i == len(node.patterns) - 1, delim='|') for i, e in
                     enumerate(node.patterns)] if node.patterns else [],
                    Markers.EMPTY
                ),
                None
            ),
            None,
            None
        )

    def visit_TryStar(self, node):
        prefix = self.__source_before('try')
        body = self.__convert_block(node.body)
        # For TryStar, handlers use except* syntax
        handlers = [cast(j.Try.Catch, self.visit_ExceptHandler(handler, is_exception_group=True)) for handler in node.handlers]
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

    def visit_TypeVar(self, node) -> j.TypeParameter:
        """Visit a TypeVar (e.g., T or T: int)."""
        prefix = self.__whitespace()
        name = self.__convert_name(node.name)
        if node.bound:
            bounds = JContainer(
                self.__source_before(':'),
                [self.__pad_right(self.__convert(node.bound), Space.EMPTY)],
                Markers.EMPTY
            )
        else:
            bounds = None
        return j.TypeParameter(
            random_id(),
            prefix,
            Markers.EMPTY,
            [],  # annotations
            [],  # modifiers
            name,
            bounds
        )

    def visit_ParamSpec(self, node) -> j.TypeParameter:
        """Visit a ParamSpec (e.g., **P)."""
        prefix = self.__whitespace()
        modifier = j.Modifier(
            random_id(),
            self.__source_before('**'),
            Markers.EMPTY,
            '**',
            j.Modifier.Type.LanguageExtension,
            []
        )
        name = self.__convert_name(node.name)
        return j.TypeParameter(
            random_id(),
            prefix,
            Markers.EMPTY,
            [],  # annotations
            [modifier],
            name,
            None  # no bounds
        )

    def visit_TypeVarTuple(self, node) -> j.TypeParameter:
        """Visit a TypeVarTuple (e.g., *Ts)."""
        prefix = self.__whitespace()
        modifier = j.Modifier(
            random_id(),
            self.__source_before('*'),
            Markers.EMPTY,
            '*',
            j.Modifier.Type.LanguageExtension,
            []
        )
        name = self.__convert_name(node.name)
        return j.TypeParameter(
            random_id(),
            prefix,
            Markers.EMPTY,
            [],  # annotations
            [modifier],
            name,
            None  # no bounds
        )

    def visit_TypeAlias(self, node):
        prefix = self.__source_before("type")
        name = cast(j.Identifier, self.__convert_name(node.name.id))

        # Handle type parameters (Python 3.12+ PEP 695)
        type_params = getattr(node, 'type_params', None)
        if type_params:
            type_parameters = JContainer(
                self.__source_before('['),
                [self.__pad_list_element(self.__convert(tp), i == len(type_params) - 1, end_delim=']')
                 for i, tp in enumerate(type_params)],
                Markers.EMPTY
            )
        else:
            type_parameters = None

        return py.TypeAlias(
            random_id(),
            prefix,
            Markers.EMPTY,
            name,
            type_parameters,
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
        left = self.__convert(node.values[0])
        for right_expr in node.values[1:]:
            left = j.Binary(
                random_id(),
                Space.EMPTY,
                Markers.EMPTY,
                left,
                self.__convert_binary_operator(node.op),
                self.__convert(right_expr),
                self._type_mapping.type(node)
            )
        return left

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
            save_token_idx = self._token_idx
            parens_right_padding = self.__whitespace()
            if self.__at_token(')'):
                name = j.FieldAccess(
                    random_id(),
                    Space.EMPTY,
                    Markers.EMPTY,
                    select.element,
                    JLeftPadded(select.after, name, Markers.EMPTY),
                    None
                )
                while len(self._parentheses_stack) > 0 and self.__skip(')'):
                    name = self._parentheses_stack.pop().transformer(name, parens_right_padding)
                    save_token_idx = self._token_idx
                    parens_right_padding = self.__whitespace()
            self._token_idx = save_token_idx
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

        method_type = self._type_mapping.method_invocation_type(node)

        return j.MethodInvocation(
            random_id(),
            prefix,
            Markers.EMPTY,
            select if isinstance(name, j.Identifier) else self.__pad_right(name, Space.EMPTY),
            None,
            name if isinstance(name, j.Identifier) else j.Identifier(random_id(), Space.EMPTY, Markers.EMPTY, [], "",
                                                                     None, None),
            args,
            method_type,
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

        return left.replace(prefix=prefix)  # ty: ignore[unresolved-attribute]  # complex union type

    def __convert_binary_operator(self, op) -> Union[JLeftPadded[j.Binary.Type], JLeftPadded[py.Binary.Type]]:
        operation_map: Dict[type, Tuple[j.Binary.Type, str]] = {
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

    @staticmethod
    def _is_byte_string(tok_string: str) -> bool:
        """Check if a string token represents a byte string (has b/B in prefix)."""
        # Find where the quote starts
        for i, c in enumerate(tok_string):
            if c in ('"', "'"):
                # Check if 'b' or 'B' appears in the prefix
                prefix = tok_string[:i].lower()
                return 'b' in prefix
        return False

    def visit_Constant(self, node):
        # For non-string constants, use simple token-based literal mapping
        if not isinstance(node.value, (str, bytes)):
            return self.__map_literal_simple(node)

        # For strings/fstrings, find the next STRING/FSTRING_START token without advancing _token_idx
        tok, _ = self._peek_significant_token()

        is_byte_string = self._is_byte_string(tok.string)
        res = None
        is_first = True

        while tok.type in (token.STRING, FSTRING_START) and is_byte_string == self._is_byte_string(tok.string):
            if not is_first:
                # Check for statement boundary (NEWLINE) before continuing concatenation
                # String concatenation only applies within the same statement
                save_idx = self._token_idx
                saw_statement_end = False
                while self._token_idx < len(self._tokens):
                    peek_tok = self._tokens[self._token_idx]
                    if peek_tok.type == token.NEWLINE:
                        saw_statement_end = True
                        self._token_idx += 1
                    elif peek_tok.type in (token.NL, token.INDENT, token.DEDENT, token.COMMENT,
                                           token.ENCODING, token.ENDMARKER, WHITESPACE_TOKEN):
                        self._token_idx += 1
                    else:
                        break
                self._token_idx = save_idx
                if saw_statement_end:
                    # NEWLINE means end of statement, not concatenation
                    break

            if tok.type == FSTRING_START:
                prefix = self.__whitespace()
                current, tok, _ = self.__map_fstring(node, prefix, tok)
            else:
                current, tok = self.__map_literal(node, tok)

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

            is_first = False

            # Peek at next non-whitespace token for loop condition
            tok, idx = self._peek_significant_token()
            if idx >= len(self._tokens) - 1 and tok.type == token.ENDMARKER:
                break

        return res

    def __map_literal_simple(self, node):
        """Map a non-string constant (numbers, None, True, False, Ellipsis)."""
        prefix = self.__whitespace()

        # Determine literal value and source representation
        if node.value is None:
            source_repr = 'None'
            value = None
        elif node.value is True:
            source_repr = 'True'
            value = True
        elif node.value is False:
            source_repr = 'False'
            value = False
        elif node.value is Ellipsis:
            source_repr = '...'
            value = None
        else:
            # For numbers, extract from source using the current token
            tok = self._tokens[self._token_idx]
            source_repr = tok.string
            value = node.value
            self._token_idx += 1  # consume the number token
            return j.Literal(
                random_id(),
                prefix,
                Markers.EMPTY,
                value,
                source_repr,
                None,
                self._type_mapping.type(node),
            )

        # Handle None, True, False, Ellipsis
        self._token_idx += 1  # consume the keyword token
        return j.Literal(
            random_id(),
            prefix,
            Markers.EMPTY,
            value,
            source_repr,
            None,
            self._type_mapping.type(node),
        )

    def __map_literal(self, node, tok):
        prefix = self.__whitespace()
        next_tok = self._advance_token()

        return (j.Literal(
            random_id(),
            prefix,
            Markers.EMPTY,
            self.__map_literal_value(node, tok),
            tok.string,  # Use token string directly instead of extracting from source
            None,
            self._type_mapping.type(node),
        ), next_tok)

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
        name_identifier = j.Identifier(
            random_id(),
            self.__source_before(node.name),
            Markers.EMPTY,
            [],
            node.name,
            None,
            None
        )

        # Handle type parameters (Python 3.12+ PEP 695)
        type_params = getattr(node, 'type_params', None)
        if type_params:
            type_parameters = j.TypeParameters(
                random_id(),
                self.__source_before('['),
                Markers.EMPTY,
                [],  # annotations
                [self.__pad_list_element(self.__convert(tp), i == len(type_params) - 1, end_delim=']')
                 for i, tp in enumerate(type_params)]
            )
        else:
            type_parameters = None

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
            type_parameters,
            return_type,
            [],  # name_annotations
            name_identifier,
            params,
            None,
            body,
            None,
            self._type_mapping.type(node),
        )

    def __map_decorator(self, decorator) -> j.Annotation:
        prefix = self.__source_before('@')

        # Collect any extra parentheses that wrap the decorator expression.
        # In Python, decorators can be wrapped in parentheses like @(expr).
        # These are '(' tokens that appear before the decorator's AST position.
        extra_parens: List[Tuple[Space, Space]] = []  # List of (prefix, inner_prefix) for each extra paren
        name_prefix = self.__whitespace()
        while self.__at_token('('):
            curr_tok = self._tokens[self._token_idx]
            curr_line, curr_col = curr_tok.start
            # If token position is before decorator's AST position, it's an extra paren
            # Convert byte offset to char offset for comparison (AST uses byte offsets, tokenizer uses char offsets)
            decorator_char_col = self._byte_offset_to_char_offset(decorator.lineno, decorator.col_offset)
            if curr_line < decorator.lineno or (curr_line == decorator.lineno and curr_col < decorator_char_col):
                extra_parens.append((name_prefix, Space.EMPTY))
                self._token_idx += 1  # consume '('
                inner_prefix = self.__whitespace()
                extra_parens[-1] = (extra_parens[-1][0], inner_prefix)
                name_prefix = Space.EMPTY
            else:
                break

        if isinstance(decorator, (ast.Attribute, ast.Name, ast.Subscript)):
            name = self.__convert(decorator)
            args = None
        elif isinstance(decorator, ast.Call):
            # If there are extra parentheses around the call, convert the entire call
            # and wrap it, setting args=None since args are part of the wrapped call
            if extra_parens:
                name = self.__convert(decorator)  # Convert entire call expression
                args = None
            else:
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

        # Apply the whitespace after @ to the name when there are no extra parentheses.
        # When extra_parens is non-empty, this is handled differently (prefix is set on the wrapped paren).
        if not extra_parens:
            name = name.replace(prefix=name_prefix)  # ty: ignore[unresolved-attribute]  # recursive call returns unknown

        # Wrap name in extra parentheses if present
        if extra_parens:
            # Set the inner prefix on the name
            name = name.replace(prefix=extra_parens[-1][1])  # ty: ignore[possibly-missing-attribute]  # recursive call returns unknown

            # Wrap in extra parentheses (innermost to outermost)
            wrapped: Expression = name
            for i in range(len(extra_parens) - 1, -1, -1):
                paren_prefix, _ = extra_parens[i]
                suffix = self.__whitespace()
                self.__skip(')')  # consume the extra ')'
                wrapped = j.Parentheses(
                    random_id(),
                    paren_prefix,
                    Markers.EMPTY,
                    self.__pad_right(wrapped, suffix)
                )

            # Wrap in ExpressionTypeTree to satisfy NameTree type requirement
            name = py.ExpressionTypeTree(
                random_id(),
                Space.EMPTY,
                Markers.EMPTY,
                wrapped
            )

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
        leading_prefix = self.__whitespace()

        tok = self._skip_whitespace_tokens()
        while tok.type not in (FSTRING_START, token.STRING):
            tok = self._advance_token()

        value_idx = 0
        res = None
        is_first = True
        # Loop while we have STRING or FSTRING_START tokens to process
        # Note: AST merges adjacent strings, so we may have more tokens than AST values
        while True:
            # For first element, use leading_prefix. For subsequent elements, peek ahead
            # to check if there's another string/fstring before consuming whitespace.
            if is_first:
                prefix = leading_prefix
                tok = self._skip_whitespace_tokens()
            else:
                # Peek at next token to check for string concatenation
                # String concatenation only applies within the same statement
                # NEWLINE (type 4) terminates a statement - break on NEWLINE
                # NL (type 65) is a non-terminating newline inside parens - allow concatenation
                save_idx = self._token_idx
                saw_statement_end = False
                while self._token_idx < len(self._tokens):
                    peek_tok = self._tokens[self._token_idx]
                    if peek_tok.type == token.NEWLINE:
                        saw_statement_end = True
                        self._token_idx += 1
                    elif peek_tok.type in (token.NL, token.INDENT, token.DEDENT, token.COMMENT,
                                           token.ENCODING, token.ENDMARKER, WHITESPACE_TOKEN):
                        self._token_idx += 1
                    else:
                        break
                if saw_statement_end or peek_tok.type not in (token.STRING, FSTRING_START):
                    # NEWLINE means end of statement, not concatenation
                    self._token_idx = save_idx
                    break
                # There's another string (concatenated) - now consume whitespace properly
                self._token_idx = save_idx
                prefix = self.__whitespace()
                tok = self._skip_whitespace_tokens()

            if tok.type == token.STRING:
                # Use AST value if available, otherwise create a synthetic node for the token
                ast_value = node.values[value_idx] if value_idx < len(node.values) else ast.Constant(value=ast.literal_eval(tok.string))
                current, tok = self.__map_literal(ast_value, tok)
                # Always apply our captured prefix (overrides __map_literal's internal whitespace call)
                current = current.replace(prefix=prefix)
                if value_idx < len(node.values) and isinstance(node.values[value_idx], ast.Constant):
                    # Check if we've consumed all content for this AST value
                    expected_value = cast(ast.Constant, node.values[value_idx]).value
                    if isinstance(expected_value, str) and current.value == expected_value:
                        value_idx += 1
            elif tok.type == FSTRING_START:
                current, tok, value_idx = self.__map_fstring(node, prefix, tok, value_idx)
            else:
                # No more f-strings or strings to process (should only happen for first element)
                break

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

            is_first = False

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
        return self.__visit_collection_literal(node, '[', ']', py.CollectionLiteral.Kind.LIST)

    def __visit_collection_literal(self, node, start_delim: str, end_delim: str, kind: py.CollectionLiteral.Kind):
        prefix = self.__source_before(start_delim)
        elements = JContainer(
            Space.EMPTY,
            [self.__pad_list_element(self.__convert(e), last=i == len(node.elts) - 1, end_delim=end_delim) for i, e in
             enumerate(node.elts)] if node.elts else
            [self.__pad_right(j.Empty(random_id(), self.__source_before(end_delim), Markers.EMPTY), Space.EMPTY)],
            Markers.EMPTY
        )
        return py.CollectionLiteral(
            random_id(),
            prefix,
            Markers.EMPTY,
            kind,
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
        cu = py.CompilationUnit(
            random_id(),
            Space.EMPTY,
            Markers.EMPTY,
            Path("TODO"),
            None,
            None,
            self._bom_marked,
            None,
            [],
            [self.__pad_statement(stmt) for stmt in node.body] if node.body else [
                self.__pad_right(j.Empty(random_id(), Space.EMPTY, Markers.EMPTY), Space.EMPTY)],
            self.__whitespace()
        )
        # Parsing complete - all tokens should be consumed
        return cu

    def visit_Name(self, node):
        space, actual_name = self.__consume_identifier(node.id)
        return j.Identifier(
            random_id(),
            space,
            Markers.EMPTY,
            [],
            actual_name,
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
        return self.__visit_collection_literal(node, '{', '}', py.CollectionLiteral.Kind.SET)

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
        self._token_idx += 1  # consume first ':'

        upper_expr = self.__convert(node.upper) if node.upper else None
        right_padding = self.__whitespace()
        upper = self.__pad_right(upper_expr if node.upper else j.Empty(random_id(), Space.EMPTY, Markers.EMPTY), right_padding)
        has_step = self.__skip(':')
        if has_step:
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
        save_token_idx = self._token_idx

        # Collect any extra parentheses that wrap the tuple.
        # Tuples handle all surrounding parentheses here (not in __parse_expr) because
        # tuple parentheses are optional depending on context, making it cleaner to
        # handle both the tuple's own parens and any extra wrapping parens in one place.
        # These are '(' tokens that appear before the tuple's AST position.
        extra_parens: List[Tuple[Space, Space]] = []  # List of (prefix, inner_prefix) for each extra paren
        # Convert AST byte offset to char offset for comparisons with token positions
        node_char_col = self._byte_offset_to_char_offset(node.lineno, node.col_offset)

        while self.__at_token('('):
            curr_tok = self._tokens[self._token_idx]
            curr_line, curr_col = curr_tok.start
            # If token position is before tuple's AST position, it's an extra paren
            if curr_line < node.lineno or (curr_line == node.lineno and curr_col < node_char_col):
                extra_parens.append((prefix, Space.EMPTY))
                self._token_idx += 1  # consume '('
                prefix = self.__whitespace()  # whitespace after the extra '(' becomes the next prefix
                extra_parens[-1] = (extra_parens[-1][0], prefix)
                prefix = Space.EMPTY
            else:
                break

        # Check if '(' at current position actually belongs to the tuple or to the first element
        # If tuple starts at same position (same line AND same column) as first element,
        # the '(' belongs to the first element, not the tuple.
        # For multi-line tuples, even if col_offset matches, different lineno means the '(' belongs to tuple.
        maybe_parens = self.__at_token('(')
        if maybe_parens and node.elts:
            same_position = (node.lineno == node.elts[0].lineno and
                             node.col_offset == node.elts[0].col_offset)
            if same_position:
                maybe_parens = False
            else:
                # Check if the matching ')' covers the whole tuple or just the first element.
                # For `x = (a), (b)`, the '(' at col 4 has ')' at col 6, but the second element
                # starts at col 9. Since ')' is before the second element, this '(' is for the
                # first element, not the tuple.
                # For `t = (1 , )`, '(' has ')' after all elements, so it's the tuple's paren.
                close_paren_idx = self._paren_pairs.get(self._token_idx)
                if close_paren_idx is not None and len(node.elts) > 1:
                    close_tok = self._tokens[close_paren_idx]
                    close_line, close_col = close_tok.start
                    # Check if ')' comes before the second element starts
                    # If so, the '(' is for the first element, not the tuple
                    second_elt = node.elts[1]
                    second_elt_char_col = self._byte_offset_to_char_offset(second_elt.lineno, second_elt.col_offset)
                    if close_line < second_elt.lineno or (
                        close_line == second_elt.lineno and close_col < second_elt_char_col
                    ):
                        maybe_parens = False

        if maybe_parens:
            self._token_idx += 1  # consume '('
            omit_parens = False
        else:
            omit_parens = True

        # For implicit tuples (no parens), don't include trailing whitespace on last element
        elements = JContainer(
            Space.EMPTY,
            [self.__pad_list_element(self.__convert(e), last=i == len(node.elts) - 1, pad_last=maybe_parens) for i, e in enumerate(node.elts)],
            Markers.EMPTY
        ) if node.elts else JContainer(
            Space.EMPTY,
            [self.__pad_right(j.Empty(random_id(), self.__whitespace(), Markers.EMPTY), Space.EMPTY)],
            Markers.EMPTY
        )

        # Use _is_closing_paren to verify the ')' matches a '(' pushed at our scope level
        if self._is_closing_paren(save_token_idx):
            self._token_idx += 1  # consume ')'
            elements = self._parentheses_stack.pop().transformer(elements, Space.EMPTY)
            omit_parens = False
        elif maybe_parens and len(self._parentheses_stack) > 0 and self._parentheses_stack[-1].node == node:
            elements = self._parentheses_stack.pop()
        elif maybe_parens and self.__skip(')'):
            pass
        else:
            omit_parens = True

        result: Expression = py.CollectionLiteral(
            random_id(),
            extra_parens[0][1] if extra_parens else prefix,
            Markers.EMPTY,
            py.CollectionLiteral.Kind.TUPLE,
            elements.replace(markers=  # ty: ignore[possibly-missing-attribute]  # complex union type
                Markers.build(random_id(), [OmitParentheses(random_id())])) if omit_parens else elements,
            self._type_mapping.type(node)
        )

        # Wrap in extra parentheses (in reverse order, innermost first)
        for i in range(len(extra_parens) - 1, -1, -1):
            paren_prefix, inner_prefix = extra_parens[i]
            suffix = self.__whitespace()
            self.__skip(')')  # consume the extra ')'
            # For the outermost paren, use the original prefix
            if i == 0:
                result = j.Parentheses(
                    random_id(),
                    prefix if not extra_parens else paren_prefix,
                    Markers.EMPTY,
                    self.__pad_right(result, suffix)
                )
            else:
                result = j.Parentheses(
                    random_id(),
                    paren_prefix,
                    Markers.EMPTY,
                    self.__pad_right(result, suffix)
                )

        return result

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
            return converted_type.replace(prefix=prefix)  # ty: ignore[unresolved-attribute]  # TypeTree base class doesn't have replace
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
                # Call visit directly to avoid __convert_internal's parenthesis handling.
                # When we're inside __convert_type which handles parentheses, calling __convert
                # would trigger the closing paren handler and wrap in Parentheses.
                converted = self.visit(node)
                # For implicit string concatenation (Binary), wrap in ExpressionTypeTree
                # to preserve the full concatenation as the type expression.
                if isinstance(converted, py.Binary):
                    return py.ExpressionTypeTree(
                        random_id(),
                        converted.prefix,
                        Markers.EMPTY,
                        converted.replace(prefix=Space.EMPTY)
                    )
                # Unwrap parenthesized literals to get to the Literal inside
                while isinstance(converted, j.Parentheses):
                    converted = converted.tree
                literal = cast(j.Literal, converted)
                source = literal.value_source
                if source is None:
                    source = str(literal.value) if literal.value is not None else ""

                # Determine quote style and extract inner content from value_source.
                # For strings, we need to preserve escape sequences from the source, not use
                # str(literal.value) which gives the interpreted value (losing escapes like \n, \').
                # Find where quotes start (after any prefix like r, b, u, f, etc.)
                quote_start = 0
                while quote_start < len(source) and source[quote_start] not in ('"', "'"):
                    quote_start += 1

                if quote_start < len(source):
                    quote_char = source[quote_start]
                    if source[quote_start:quote_start+3] == quote_char * 3:
                        # Triple quotes
                        quote_style = Quoted.Style.TRIPLE_SINGLE if quote_char == "'" else Quoted.Style.TRIPLE_DOUBLE
                        quote_len = 3
                    else:
                        quote_style = Quoted.Style.SINGLE if quote_char == "'" else Quoted.Style.DOUBLE
                        quote_len = 1
                    # Extract inner content (between quotes), preserving escape sequences
                    name = source[quote_start + quote_len : -quote_len]
                else:
                    quote_style = None
                    # Non-string literal: use value_source to preserve case (e.g., 1J vs 1j)
                    name = source

                return j.Identifier(
                    random_id(),
                    literal.prefix,
                    Markers.build(random_id(), [Quoted(random_id(), quote_style)]) if quote_style else Markers.EMPTY,
                    [],
                    name,
                    self._type_mapping.type(node),
                    None
                )
        elif isinstance(node, ast.Subscript):
            prefix = self.__whitespace()
            converted_value = self.__convert(node.value)
            bracket_prefix = self.__source_before('[')

            # Determine slice elements. For tuples:
            # - Union[str, int] - slice is Tuple, no parens -> unpack to [str, int]
            # - Union[(str, int)] - slice is Tuple WITH parens -> keep as [(str, int)]
            # Check if next token after '[' is '(' to detect parenthesized tuples.
            is_parenthesized_tuple = (isinstance(node.slice, ast.Tuple) and
                                      node.slice.elts and
                                      self.__at_token('('))
            if isinstance(node.slice, ast.Tuple) and node.slice.elts and not is_parenthesized_tuple:
                slices = node.slice.elts
            else:
                slices = [node.slice]

            return j.ParameterizedType(
                random_id(),
                prefix,
                Markers.EMPTY,
                converted_value,
                JContainer(
                    bracket_prefix,
                    [self.__pad_list_element(self.__convert_type(s), last=i == len(slices) - 1, end_delim=']') for
                     i, s in
                     enumerate(slices)],
                    Markers.EMPTY
                ),
                None
            )
        elif isinstance(node, ast.BinOp):
            # Type unions using `|` was added in Python 3.10
            # Only treat as UnionType if the operator is actually BitOr
            if isinstance(node.op, ast.BitOr):
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
            # Other binary operations in types (like `int + int`) should be handled as regular BinOp
            return self.visit_BinOp(node)

        return self.__convert_internal(node, self.__convert_type)  # ty: ignore[invalid-return-type]  # __convert_internal returns J, TypeTree is subtype

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

        save_token_idx = self._token_idx
        prefix = self.__whitespace()

        # Handle normal expression or parenthesized expression
        result = self.__parse_expr(node, mapping or self.visit, recursion, save_token_idx, prefix)

        save_token_idx_2 = self._token_idx
        suffix = self.__whitespace()

        # Process closing parentheses if any
        while self._is_closing_paren(save_token_idx):
            self._token_idx += 1  # consume ')'
            entry = self._parentheses_stack.pop()
            result = entry.transformer(result, suffix)
            save_token_idx = entry.save_token_idx
            save_token_idx_2 = self._token_idx
            suffix = self.__whitespace()

        # Clean up unmatched parentheses for this node
        while len(self._parentheses_stack) > 1 and self._parentheses_stack[-1].node is node and \
                self._parentheses_stack[-2].node is not node:
            self._parentheses_stack.pop()

        self._token_idx = save_token_idx_2
        return result  # ty: ignore[invalid-return-type]  # result type from transformer

    def __parse_expr(self, node, mapping, recursion, save_token_idx: int, prefix: Space) -> J:
        """Parse either a normal expression or a parenthesized expression."""
        if not self.__at_token('('):
            self._token_idx = save_token_idx
            return mapping(cast(ast.AST, node))

        # Tuples handle their own parentheses in visit_Tuple
        if isinstance(node, ast.Tuple):
            self._token_idx = save_token_idx
            return mapping(cast(ast.AST, node))

        # Check if closing paren comes before node ends (paren wraps inner expression, not this node)
        close_paren_idx = self._paren_pairs.get(self._token_idx)
        if close_paren_idx is not None and hasattr(node, 'end_lineno') and hasattr(node, 'end_col_offset'):
            close_tok = self._tokens[close_paren_idx]
            close_line, close_col = close_tok.start
            # Convert AST byte offset to character offset for comparison with token position
            end_char_col = self._byte_offset_to_char_offset(node.end_lineno, node.end_col_offset)
            # If closing paren is before node's end, it wraps an inner expression
            if close_line < node.end_lineno or (close_line == node.end_lineno and close_col < end_char_col):
                self._token_idx = save_token_idx
                return mapping(cast(ast.AST, node))

        # For non-Tuples: push to stack and process
        self.__push_parentheses(node, prefix, save_token_idx)
        return recursion(node)

    def __push_parentheses(self, node, prefix: Space, save_token_idx: int) -> _ParenStackEntry:
        open_paren_idx = self._token_idx  # Token index of '(' before incrementing
        self._token_idx += 1  # consume '('
        expr_prefix = self.__whitespace()
        entry = _ParenStackEntry(
            transformer=lambda e, r, expr_prefix=expr_prefix, prefix=prefix: (
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
            save_token_idx=save_token_idx,
            token_idx_after_open=self._token_idx,
            node=node,
            prefix=prefix,
            open_paren_idx=open_paren_idx
        )
        self._parentheses_stack.append(entry)
        return entry

    def _is_closing_paren(self, save_token_idx: int) -> bool:
        """Check if current position has a valid closing parenthesis.

        Uses both scope checking and paren map for definitive matching.
        A child expression should not pop parentheses pushed by an ancestor.
        """
        if not self._parentheses_stack or not self.__at_token(')'):
            return False

        # First check scope - we can only close parens pushed at our scope level
        # This prevents child expressions from popping parentheses belonging to ancestors
        # stack_token_idx is where we were at push time (at the '(')
        # save_token_idx is where we were at the start of __convert_internal (before whitespace)
        # Allow stack_token_idx to be slightly ahead of save_token_idx (due to whitespace consumption)
        stack_token_idx = self._parentheses_stack[-1].token_idx_after_open
        if stack_token_idx < save_token_idx or stack_token_idx > save_token_idx + 2:
            return False

        # Then verify position using paren map (token indices)
        open_paren_idx = self._parentheses_stack[-1].open_paren_idx
        expected_close_idx = self._paren_pairs.get(open_paren_idx)
        if expected_close_idx is not None:
            return self._token_idx == expected_close_idx

        # If not in paren map, scope check already passed
        return True

    def __convert_name(self, name: str, name_type: Optional[JavaType] = None) -> NameTree:
        def ident_or_field(parts: List[str]) -> NameTree:
            if len(parts) == 1:
                space, actual_name = self.__consume_identifier(parts[-1])
                return j.Identifier(random_id(), space, Markers.EMPTY, [], actual_name,
                                    name_type, None)
            else:
                return j.FieldAccess(
                    random_id(),
                    self.__whitespace(),
                    Markers.EMPTY,
                    ident_or_field(parts[:-1]),
                    self.__pad_left(
                        self.__source_before('.'),
                        (lambda s, n: j.Identifier(random_id(), s, Markers.EMPTY, [], n,
                                     name_type,
                                     None))(*self.__consume_identifier(parts[-1])),
                    ),
                    name_type
                )

        return ident_or_field(name.split('.'))

    def __convert_all(self, trees: Sequence) -> List[J2]:
        return [c for tree in trees if (c := self.__convert(tree)) is not None]  # ty: ignore[invalid-return-type]

    def __convert_block(self, statements: Sequence[Statement], delim: str = ':') -> j.Block:
        prefix = self.__source_before(delim)
        if statements:
            statements = [self.__pad_statement(cast(ast.stmt, s)) for s in statements]  # ty: ignore[invalid-assignment]
        else:
            statements = [self.__pad_right(j.Empty(random_id(), Space.EMPTY, Markers.EMPTY), Space.EMPTY)]  # ty: ignore[invalid-assignment]
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
        # Only capture trailing whitespace as padding if followed by semicolon
        # Otherwise, whitespace belongs to the next statement's prefix
        save_idx = self._token_idx
        padding = self.__whitespace('\n')
        if self.__skip(';'):
            markers = Markers.build(random_id(), [Semicolon(random_id())])
        else:
            # No semicolon - restore position so whitespace becomes next statement's prefix
            self._token_idx = save_idx
            padding = Space.EMPTY
            markers = Markers.EMPTY
        return JRightPadded(statement, padding, markers)  # ty: ignore[invalid-return-type]  # statement is J|None from __convert_statement

    def __pad_list_element(self, element: J2, last: bool = False, pad_last: bool = True, delim: str = ',',
                           end_delim: Optional[str] = None) -> JRightPadded[J2]:
        save_token_idx = self._token_idx
        markers = Markers.EMPTY

        if last and self._token_idx < len(self._tokens):
            # For the last element, always consume whitespace first to check for trailing comma
            padding = self.__whitespace()
            if end_delim != delim and self.__skip(delim):
                # Found trailing comma - keep the whitespace as padding
                markers = markers.replace(markers=[TrailingComma(random_id(), self.__whitespace())])
            elif not self.__at_token(end_delim) if end_delim else True:
                # No trailing comma - decide whether to keep padding based on pad_last
                if not pad_last:
                    padding = Space.EMPTY
                    self._token_idx = save_token_idx
            if end_delim and self.__skip(end_delim):
                pass
        elif last:
            padding = Space.EMPTY
            self._token_idx = save_token_idx
        elif not last:
            # For non-last elements, always consume whitespace (space before comma belongs to element)
            padding = self.__whitespace()
            self._token_idx += 1  # consume delimiter
            markers = Markers.EMPTY
        else:
            padding = Space.EMPTY

        return JRightPadded(element, padding, markers)

    def __pad_right(self, tree, space: Space) -> JRightPadded[J2]:
        return JRightPadded(tree, space, Markers.EMPTY)

    def __pad_left(self, space: Space, tree) -> JLeftPadded[J2]:
        if isinstance(tree, ast.AST):
            raise ArgumentError(tree, "must be a Tree but is a {}".format(type(tree)))
        return JLeftPadded(space, tree, Markers.EMPTY)

    def __source_before(self, until_delim: Optional[str], stop: Optional[str] = None) -> Space:
        """Extract whitespace before a delimiter, then consume the delimiter token."""
        if not until_delim:
            return Space.EMPTY

        # Get whitespace (this also advances past whitespace tokens)
        space = self.__whitespace(stop)

        # Consume the delimiter token
        if self._token_idx < len(self._tokens):
            tok = self._tokens[self._token_idx]
            if tok.string == until_delim or (tok.type == token.NAME and tok.string == until_delim):
                self._token_idx += 1

        return space

    def __consume_identifier(self, expected_name: str) -> Tuple[Space, str]:
        """Consume an identifier token and return (prefix_space, actual_source_text).

        Python normalizes identifiers to NFKC form in the AST (per PEP 3131),
        but we need to preserve the original source text for idempotent printing.
        This method matches the expected AST name against the token using NFKC
        normalization, but returns the original token string.
        """
        import unicodedata
        save_idx = self._token_idx
        space = self.__whitespace()
        if self._token_idx < len(self._tokens):
            tok = self._tokens[self._token_idx]
            if tok.type == token.NAME:
                # NFKC-normalize token string to compare with AST name
                normalized = unicodedata.normalize('NFKC', tok.string)
                if normalized == expected_name:
                    self._token_idx += 1
                    return space, tok.string
                # Token is a NAME but doesn't match - try direct match as fallback
                if tok.string == expected_name:
                    self._token_idx += 1
                    return space, tok.string
        # No matching identifier found - restore position and use __source_before
        self._token_idx = save_idx
        return self.__source_before(expected_name), expected_name

    def __skip(self, tok: Optional[str]) -> Optional[str]:
        """Skip the current token if it matches the given string."""
        if tok is None:
            return None
        if self._token_idx < len(self._tokens):
            curr = self._tokens[self._token_idx]
            if curr.string == tok:
                self._token_idx += 1
                return tok
        return None

    def __whitespace(self, stop: Optional[str] = None) -> Space:
        """Consume whitespace tokens and return the whitespace as a Space."""
        prefix: Optional[str] = None
        whitespace: List[str] = []
        comments: List[Comment] = []

        while self._token_idx < len(self._tokens):
            tok = self._tokens[self._token_idx]

            # Skip virtual tokens with no source representation
            if tok.type in (token.DEDENT, token.ENCODING, token.ENDMARKER):
                self._token_idx += 1
                continue

            # Handle whitespace tokens
            if tok.type in (WHITESPACE_TOKEN, token.NEWLINE, token.NL, token.INDENT):
                text = tok.string
                # Check for stop character
                if stop is not None and stop in text:
                    # For NEWLINE/NL tokens containing stop, don't split (treat \r\n as unit)
                    if tok.type in (token.NEWLINE, token.NL):
                        break
                    stop_idx = text.index(stop)
                    # Check if this newline is escaped (line continuation: backslash before newline)
                    # If so, don't stop here - the logical line continues
                    if stop == '\n' and stop_idx > 0 and text[stop_idx - 1] == '\\':
                        # Line continuation - consume entire token and continue
                        whitespace.append(text)
                        self._token_idx += 1
                        continue
                    whitespace.append(text[:stop_idx])
                    break
                whitespace.append(text)
                self._token_idx += 1

            # Handle comments
            elif tok.type == token.COMMENT:
                # Save current whitespace as prefix or previous comment's suffix
                if comments:
                    comments[-1] = comments[-1].replace(suffix=''.join(whitespace))
                else:
                    prefix = ''.join(whitespace)
                whitespace = []
                # Create comment (strip leading '#')
                comment_text = tok.string[1:] if tok.string.startswith('#') else tok.string
                comments.append(TextComment(False, comment_text, '', Markers.EMPTY))
                self._token_idx += 1

            else:
                # Non-whitespace token, stop
                break

        if not whitespace and not comments:
            return Space.EMPTY

        if not comments:
            prefix = ''.join(whitespace)
        elif whitespace:
            comments[-1] = comments[-1].replace(suffix=''.join(whitespace))

        return Space(comments, prefix if prefix is not None else '')

    def _map_unary_operator(self, op) -> Tuple[j.Unary.Type, str]:
        operation_map: Dict[type, Tuple[j.Unary.Type, str]] = {
            ast.Invert: (j.Unary.Type.Complement, '~'),
            ast.Not: (j.Unary.Type.Not, 'not'),
            ast.UAdd: (j.Unary.Type.Positive, '+'),
            ast.USub: (j.Unary.Type.Negative, '-'),
        }
        return operation_map[type(op)]

    def _map_assignment_operator(self, op):
        operation_map: Dict[type, Tuple[j.AssignmentOperation.Type, str]] = {
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

    def __map_fstring(self, node: ast.JoinedStr, prefix: Space, tok: TokenInfo, value_idx: int = 0) -> \
            Tuple[J, TokenInfo, int]:
        """Map an f-string to a FormattedString AST node.

        Uses _token_idx directly to iterate through tokens.
        """
        if tok.type != FSTRING_START:
            if len(node.values) == 1 and isinstance(node.values[0], ast.Constant):
                # format specifiers are stored as f-strings in the AST; e.g. `f'{1:n}'`
                format_val = node.values[0].value
                format_str = str(format_val) if format_val is not None else None
                self._token_idx += 1  # consume the format token
                return (j.Literal(
                    random_id(),
                    self.__whitespace(),
                    Markers.EMPTY,
                    format_val,
                    format_str,
                    None,
                    JavaType.Primitive.String
                ), self._tokens[self._token_idx], 0)
            else:
                delimiter = ''
            consume_end_delim = False
        else:
            delimiter = tok.string
            tok = self._advance_token()  # consume FSTRING_START, get next
            consume_end_delim = True

        # tokenizer tokens: FSTRING_START, FSTRING_MIDDLE, OP, ..., OP, FSTRING_MIDDLE, FSTRING_END
        parts = []
        prev_token_idx = -1
        while tok.type != FSTRING_END and value_idx < len(node.values):
            # Safety check: ensure loop is making progress
            if self._token_idx == prev_token_idx:
                raise RuntimeError(
                    f"F-string parsing stuck at token_idx={self._token_idx}, "
                    f"tok.type={tok.type}, tok.string={repr(tok.string)}, "
                    f"value_idx={value_idx}, node.values={[type(v).__name__ for v in node.values]}"
                )
            prev_token_idx = self._token_idx

            # Skip synthetic whitespace tokens injected between f-string tokens
            if tok.type == WHITESPACE_TOKEN:
                tok = self._advance_token()
                continue

            # Handle nested FSTRING_START - this indicates nested f-string concatenation
            # which is not fully supported. Skip to prevent infinite loop.
            if tok.type == FSTRING_START:
                # Skip until we find the matching FSTRING_END
                depth = 1
                while depth > 0 and self._token_idx < len(self._tokens):
                    tok = self._advance_token()
                    if tok.type == FSTRING_START:
                        depth += 1
                    elif tok.type == FSTRING_END:
                        depth -= 1
                tok = self._advance_token() if self._token_idx < len(self._tokens) - 1 else tok
                continue

            value = node.values[value_idx]
            if tok.type == FSTRING_MIDDLE:
                # Accumulate text from consecutive FSTRING_MIDDLE tokens
                s = tok.string
                tok = self._advance_token()  # consume first FSTRING_MIDDLE, get next
                while tok.type == FSTRING_MIDDLE:
                    s += tok.string
                    tok = self._advance_token()  # consume and get next
                # For value_source, escape braces so the printer outputs them correctly
                # In f-strings, {{ becomes { and }} becomes }, so we reverse that
                value_source = s.replace('{', '{{').replace('}', '}}')
                parts.append(j.Literal(
                    random_id(),
                    Space.EMPTY,
                    Markers.EMPTY,
                    s,
                    value_source,
                    None,
                    JavaType.Primitive.String
                ))
                if cast(ast.Constant, value).value == s:
                    value_idx += 1
            elif tok.type == token.OP and tok.string == '{':
                tok = self._advance_token()  # consume '{', get next
                if not isinstance(value, ast.FormattedValue):
                    # this is the case when using the `=` "debug specifier"
                    value_idx += 1
                    value = node.values[value_idx]

                if isinstance(cast(ast.FormattedValue, value).value, ast.JoinedStr):
                    nested, tok, _ = self.__map_fstring(cast(ast.JoinedStr, cast(ast.FormattedValue, value).value),
                                                        Space.EMPTY, tok)
                    expr = self.__pad_right(
                        nested,
                        Space.EMPTY
                    )
                else:
                    expr = self.__pad_right(
                        self.__convert(cast(ast.FormattedValue, value).value),
                        self.__whitespace()
                    )

                # Scan for specifiers (debug, conversion, format) - applies to both nested f-string and regular expressions
                while self._token_idx < len(self._tokens) and self._tokens[self._token_idx].type not in (FSTRING_END, FSTRING_MIDDLE):
                    tok = self._next_token()  # get current and advance (we need to examine current token)
                    if tok.type == token.OP and tok.string in ('!'):
                        break
                    la_tok = self._tokens[self._token_idx]
                    if tok.type == token.OP and tok.string == '}' and (
                            la_tok.type in (FSTRING_END, FSTRING_MIDDLE) or (
                            la_tok.type == token.OP and la_tok.string == '{')):
                        break
                    # Debug specifier '=' - break regardless of what follows (whitespace is valid after '=')
                    if tok.type == token.OP and tok.string == '=':
                        break
                    if tok.type == token.OP and tok.string == ':' and la_tok.string in ('{'):
                        break

                # debug specifier
                if tok.type == token.OP and tok.string == '=':
                    debug = self.__pad_right(True, self.__whitespace('\n'))
                    tok = self._tokens[self._token_idx]  # get token after whitespace
                else:
                    debug = None

                # conversion specifier
                if tok.type == token.OP and tok.string == '!':
                    # If we came from debug specifier handling, _token_idx points to '!'
                    # If we came from the while loop directly (no debug), _token_idx already points to conversion char
                    if debug is not None:
                        self._token_idx += 1  # advance past '!' (only needed after debug specifier)
                    tok = self._tokens[self._token_idx]  # get conversion char
                    conv = py.FormattedString.Value.Conversion.ASCII if tok.string == 'a' else py.FormattedString.Value.Conversion.STR if tok.string == 's' else py.FormattedString.Value.Conversion.REPR
                    tok = self._advance_token()  # consume conversion char, get next
                else:
                    conv = None

                # format specifier
                if tok.type == token.OP and tok.string == ':':
                    # After conversion handling: _token_idx points to ':' (need to advance)
                    # After scanning loop only: _token_idx already points past ':' (don't advance)
                    if conv is not None:
                        self._token_idx += 1  # advance past ':' (only needed after conversion)
                    format_spec, tok, _ = self.__map_fstring(
                        cast(ast.JoinedStr, cast(ast.FormattedValue, value).format_spec), Space.EMPTY, self._tokens[self._token_idx])
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
                # After format spec, conversion, or debug: _token_idx points to '}', need to advance
                # After scanning loop without any of these: _token_idx already past '}'
                if (format_spec is not None or conv is not None or debug is not None) and self._tokens[self._token_idx].string == '}':
                    self._token_idx += 1
                tok = self._tokens[self._token_idx]
            elif tok.type == FSTRING_END:
                raise NotImplementedError("Unsupported: String concatenation with f-strings")

        if consume_end_delim:
            tok = self._advance_token()  # consume FSTRING_END, get next
        elif tok.type == FSTRING_MIDDLE and len(tok.string) == 0:
            tok = self._advance_token()  # consume empty FSTRING_MIDDLE, get next

        return (py.FormattedString(
            random_id(),
            prefix,
            Markers.EMPTY,
            delimiter,
            parts
        ), tok, value_idx)

    def __at_token(self, s: str) -> bool:
        """Check if the current token matches the given string."""
        if self._token_idx >= len(self._tokens):
            return False
        return self._tokens[self._token_idx].string == s
