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

"""Telling a type from a value inside an annotation, from the source alone.

The parser and the import use census both need the rule and have to agree on it,
so it lives here once.
"""

import ast
from typing import Iterator, Optional, Tuple

# The subscripts whose arguments from this index on are values rather than types, so a
# string there names nothing. ``Annotated``'s first argument is still the annotated type.
VALUE_SUBSCRIPTS = {'Literal': 0, 'Annotated': 1}


def subscript_head(node) -> Optional[str]:
    """The trailing name of a subscript's head, as spelled in the source."""
    if isinstance(node, ast.Name):
        return node.id
    if isinstance(node, ast.Attribute):
        return node.attr
    return None


def annotation_names(source: str) -> Tuple[str, ...]:
    """The names the annotation ``source`` looks up, or () when it is not an expression.

    A string in a type position is itself a forward reference and contributes the names
    it spells; one in a value position spells a value and contributes none.
    """
    try:
        expression = ast.parse(source.strip(), mode='eval')
    except (SyntaxError, ValueError):
        return ()
    return tuple(_names(expression.body, True))


def _names(node, in_type: bool) -> Iterator[str]:
    if isinstance(node, ast.Name):
        yield node.id
    elif isinstance(node, ast.Constant):
        if in_type and isinstance(node.value, str):
            yield from annotation_names(node.value)
    elif isinstance(node, ast.Subscript):
        yield from _names(node.value, in_type)
        values_from = VALUE_SUBSCRIPTS.get(subscript_head(node.value))
        arguments = node.slice.elts if isinstance(node.slice, ast.Tuple) else [node.slice]
        for index, argument in enumerate(arguments):
            yield from _names(argument, in_type and
                              (values_from is None or index < values_from))
    else:
        # The table is the only thing that narrows a type position: a union or a
        # ``Callable`` argument list still holds types.
        for child in ast.iter_child_nodes(node):
            yield from _names(child, in_type)
