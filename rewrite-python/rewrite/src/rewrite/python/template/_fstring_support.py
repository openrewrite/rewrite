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
from typing import TYPE_CHECKING, Dict

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
    """Collect registered captures whose ``{name}`` appears in *code*, then clear the registry."""
    registry = _pending.get(None)
    if not registry:
        return {}
    captures = {name: cap for name, cap in registry.items() if '{' + name + '}' in code}
    registry.clear()
    return captures


def clear_registry() -> None:
    """Clear all pending captures (used when explicit kwargs take priority)."""
    registry = _pending.get(None)
    if registry:
        registry.clear()
