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
Python template system for generating and matching AST patterns.

This module provides a Python-idiomatic templating mechanism for OpenRewrite,
similar to JavaTemplate but leveraging Python's f-string-like syntax.

Examples:
    # Import the templating API
    from rewrite.python.template import template, pattern, capture

    # Create captures for matching/substitution
    expr = capture('expr')

    # Create a pattern to match print() calls
    pat = pattern("print({expr})", expr=expr)

    # Create a template to generate logging calls
    tmpl = template("logging.info({expr})", expr=expr)

    # In a visitor
    class MyVisitor(PythonVisitor):
        def visit_method_invocation(self, method, ctx):
            match = pat.match(method, self.cursor)
            if match:
                return tmpl.apply(self.cursor, values=match)
            return super().visit_method_invocation(method, ctx)
"""

from .capture import Capture, capture, RawCode, raw
from .coordinates import PythonCoordinates, CoordinateMode, CoordinateLocation
from .pattern import Pattern, MatchResult, pattern
from .template import Template, TemplateBuilder, template
from .engine import TemplateEngine, TemplateOptions

__all__ = [
    # Capture
    "Capture",
    "capture",
    "RawCode",
    "raw",

    # Coordinates
    "PythonCoordinates",
    "CoordinateMode",
    "CoordinateLocation",

    # Pattern
    "Pattern",
    "MatchResult",
    "pattern",

    # Template
    "Template",
    "TemplateBuilder",
    "template",

    # Engine
    "TemplateEngine",
    "TemplateOptions",
]
