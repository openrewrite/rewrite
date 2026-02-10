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

"""
Python language support for OpenRewrite.

This module provides the Python-specific LST types, visitor, parser,
and printer for transforming Python source code.
"""

# Template system
from rewrite.python.template import (
    template,
    pattern,
    capture,
    raw,
    Template,
    Pattern,
    MatchResult,
    Capture,
    RawCode,
    PythonCoordinates,
)

from rewrite.python.tree import (
    Py,
    Async,
    Await,
    Binary,
    ChainedAssignment,
    CollectionLiteral,
    CompilationUnit,
    ComprehensionExpression,
    Del,
    DictLiteral,
    ErrorFrom,
    ExceptionType,
    ExpressionStatement,
    ExpressionTypeTree,
    FormattedString,
    KeyValue,
    LiteralType,
    MatchCase,
    MultiImport,
    NamedArgument,
    Pass,
    Slice,
    SpecialParameter,
    Star,
    StatementExpression,
    TrailingElseWrapper,
    TypeAlias,
    TypeHint,
    TypeHintedExpression,
    UnionType,
    VariableScope,
    YieldFrom,
)
from rewrite.python.support_types import PyComment
from rewrite.python.visitor import PythonVisitor
from rewrite.python.style import (
    SpacesStyle,
    TabsAndIndentsStyle,
    BlankLinesStyle,
    WrappingAndBracesStyle,
    OtherStyle,
    IntelliJ,
)
from rewrite.python.format import (
    AutoFormat,
    BlankLinesVisitor,
    MinimumViableSpacingVisitor,
    NormalizeFormatVisitor,
    NormalizeTabsOrSpacesVisitor,
    SpacesVisitor,
    TabsAndIndentsVisitor,
)
from rewrite.python.add_import import AddImport, AddImportOptions, maybe_add_import
from rewrite.python.remove_import import RemoveImport, RemoveImportOptions, maybe_remove_import
from rewrite.python.method_matcher import MethodMatcher

# Precondition helpers (delegate to Java via RPC)
from rewrite.python.preconditions import (
    has_source_path,
    uses_method,
    uses_type,
    find_methods,
    find_types,
)

# Recipe wrappers (delegate to Java via RPC)
from rewrite.python.recipes import (
    ChangeType,
    ChangeMethodName,
    ChangePackage,
    DeleteMethodArgument,
    ReorderMethodArguments,
    AddLiteralMethodArgument,
)

__all__ = [
    # Template system
    "template",
    "pattern",
    "capture",
    "raw",
    "Template",
    "Pattern",
    "MatchResult",
    "Capture",
    "RawCode",
    "PythonCoordinates",
    # Marker class
    "Py",
    # Python-specific types
    "Async",
    "Await",
    "Binary",
    "ChainedAssignment",
    "CollectionLiteral",
    "CompilationUnit",
    "ComprehensionExpression",
    "Del",
    "DictLiteral",
    "ErrorFrom",
    "ExceptionType",
    "ExpressionStatement",
    "ExpressionTypeTree",
    "FormattedString",
    "KeyValue",
    "LiteralType",
    "MatchCase",
    "MultiImport",
    "NamedArgument",
    "Pass",
    "Slice",
    "SpecialParameter",
    "Star",
    "StatementExpression",
    "TrailingElseWrapper",
    "TypeAlias",
    "TypeHint",
    "TypeHintedExpression",
    "UnionType",
    "VariableScope",
    "YieldFrom",
    # Support types
    "PyComment",
    # Visitor
    "PythonVisitor",
    # Style
    "SpacesStyle",
    "TabsAndIndentsStyle",
    "BlankLinesStyle",
    "WrappingAndBracesStyle",
    "OtherStyle",
    "IntelliJ",
    # Format
    "AutoFormat",
    "BlankLinesVisitor",
    "MinimumViableSpacingVisitor",
    "NormalizeFormatVisitor",
    "NormalizeTabsOrSpacesVisitor",
    "SpacesVisitor",
    "TabsAndIndentsVisitor",
    # Import handling
    "AddImport",
    "AddImportOptions",
    "maybe_add_import",
    "RemoveImport",
    "RemoveImportOptions",
    "maybe_remove_import",
    # Method matching
    "MethodMatcher",
    # Precondition helpers
    "has_source_path",
    "uses_method",
    "uses_type",
    "find_methods",
    "find_types",
    # Recipe wrappers (Java delegation)
    "ChangeType",
    "ChangeMethodName",
    "ChangePackage",
    "DeleteMethodArgument",
    "ReorderMethodArguments",
    "AddLiteralMethodArgument",
]
