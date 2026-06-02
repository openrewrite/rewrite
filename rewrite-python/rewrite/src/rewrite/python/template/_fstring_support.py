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

"""Registry for auto-registering Captures during f-string formatting.

Uses ``contextvars.ContextVar`` so each thread / asyncio Task gets its own
isolated registry, preventing interference between concurrent f-string
evaluations.
"""

from __future__ import annotations

import contextvars
from typing import Any, TYPE_CHECKING, Dict, Tuple

if TYPE_CHECKING:
    from .capture import Capture

_pending: contextvars.ContextVar[Dict[str, 'Capture']] = contextvars.ContextVar('_pending')


def register_capture(cap: 'Capture') -> None:
    """Register a capture from ``Capture.__format__``."""
    registry = _pending.get(None)
    if registry is None:
        registry = {}
        _pending.set(registry)
    registry[cap.name] = cap


def collect_captures(code: str) -> Dict[str, 'Capture']:
    """Collect registered captures whose placeholder identifier appears in *code*, then clear the registry.

    All entries are cleared afterward, including non-matching ones, to prevent
    stale captures from leaking into subsequent ``template()``/``pattern()`` calls.
    """
    from .placeholder import to_placeholder
    registry = _pending.get(None)
    if not registry:
        return {}
    captures = {name: cap for name, cap in registry.items() if to_placeholder(name) in code}
    registry.clear()
    return captures


def clear_registry() -> None:
    """Clear all pending captures (used when explicit kwargs take priority)."""
    registry = _pending.get(None)
    if registry:
        registry.clear()


def resolve_captures(code: Any, captures: Dict[str, 'Capture']) -> Tuple[str, Dict[str, 'Capture']]:
    """Resolve captures from t-string, f-string auto-registration, or explicit kwargs.

    Shared by ``template()`` and ``pattern()`` to avoid duplicating the
    dispatch logic.

    Returns:
        A ``(code, captures)`` tuple ready to pass to Template/Pattern.
    """
    from ._tstring_support import is_tstring, convert_tstring

    if is_tstring(code):
        if captures:
            raise TypeError(
                "Cannot pass keyword captures when using a t-string; "
                "interpolate Capture objects directly in the t-string instead"
            )
        code, captures = convert_tstring(code)
        clear_registry()
    elif captures:
        clear_registry()
    else:
        auto = collect_captures(code)
        if auto:
            captures = auto

    return code, captures
