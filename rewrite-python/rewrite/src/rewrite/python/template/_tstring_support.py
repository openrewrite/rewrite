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

"""Support for Python 3.14+ t-string templates."""

from __future__ import annotations

from typing import Any, Dict, Tuple

from .capture import Capture, RawCode

_TemplateType = None


def _get_template_type():
    global _TemplateType
    if _TemplateType is None:
        try:
            from string.templatelib import Template
            _TemplateType = Template
        except (ImportError, ModuleNotFoundError):
            _TemplateType = type('_NoMatch', (), {})  # sentinel: never matches
    return _TemplateType


def is_tstring(obj: Any) -> bool:
    """Check if obj is a string.templatelib.Template (Python 3.14+ t-string).

    Returns False on Python < 3.14 or if templatelib is unavailable.
    """
    return isinstance(obj, _get_template_type())


def convert_tstring(tpl: Any) -> Tuple[str, Dict[str, Capture]]:
    """Convert a t-string Template into a (code, captures) tuple.

    Iterates the t-string's args: static strings are concatenated as-is,
    Capture interpolations become ``{name}`` placeholders, and RawCode
    interpolations are spliced directly into the code string.

    Raises:
        TypeError: If an interpolation is not a Capture or RawCode.
    """
    parts: list[str] = []
    captures: Dict[str, Capture] = {}

    for arg in tpl:
        if isinstance(arg, str):
            parts.append(arg)
        elif hasattr(arg, 'value'):
            # Interpolation object: has .value, .expression, .conversion, .format_spec
            value = arg.value
            if isinstance(value, Capture):
                parts.append('{' + value.name + '}')
                captures[value.name] = value
            elif isinstance(value, RawCode):
                parts.append(value.code)
            else:
                raise TypeError(
                    f"t-string interpolations must be Capture or RawCode instances, "
                    f"got {type(value).__name__}: {value!r}"
                )
        else:
            raise TypeError(
                f"Unexpected t-string component: {type(arg).__name__}"
            )

    return ''.join(parts), captures
