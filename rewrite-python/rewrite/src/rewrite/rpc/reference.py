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

"""Ref-id bookkeeping for the send side of the RPC protocol."""

from __future__ import annotations

from typing import Any, Dict, Optional, Tuple


class ReferenceMap:
    """Ref ids assigned to objects sent to a peer, keyed by object identity.

    The peer builds the mirror of this map as it receives, so an id may only be
    cited once the object behind it has arrived. Its lifetime is therefore the
    peer connection's, and ``rollback_to`` undoes the ids of an exchange the peer
    never received. Mirrors ``RewriteRpc.localRefs`` and the JavaScript and Go
    ``ReferenceMap``.
    """

    def __init__(self) -> None:
        # The object is kept beside its ref so a recycled id() cannot alias a
        # freed object onto another object's ref.
        self._by_id: Dict[int, Tuple[Any, int]] = {}
        self._next: int = 0

    def get(self, obj: Any) -> Optional[int]:
        """The ref already assigned to ``obj``, or None when it has not been sent."""
        entry = self._by_id.get(id(obj))
        return entry[1] if entry is not None and entry[0] is obj else None

    def create(self, obj: Any) -> int:
        self._next += 1
        self._by_id[id(obj)] = (obj, self._next)
        return self._next

    def snapshot(self) -> int:
        """The high-water ref id, for a later ``rollback_to``."""
        return self._next

    def rollback_to(self, snapshot: int) -> None:
        """Drop every ref assigned since ``snapshot``."""
        for key in [k for k, (_, ref) in self._by_id.items() if ref > snapshot]:
            del self._by_id[key]
        self._next = snapshot

    def clear(self) -> None:
        self._by_id.clear()
        self._next = 0

    def __len__(self) -> int:
        return len(self._by_id)
