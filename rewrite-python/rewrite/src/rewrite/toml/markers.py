from __future__ import annotations

from dataclasses import dataclass
from uuid import UUID

from rewrite.markers import Marker
from rewrite.utils import replace_if_changed


@dataclass(frozen=True, eq=False)
class ArrayTable(Marker):
    _id: UUID

    @property
    def id(self) -> UUID:
        return self._id

    def replace(self, **kwargs) -> ArrayTable:
        return replace_if_changed(self, **kwargs)


@dataclass(frozen=True, eq=False)
class InlineTable(Marker):
    _id: UUID

    @property
    def id(self) -> UUID:
        return self._id

    def replace(self, **kwargs) -> InlineTable:
        return replace_if_changed(self, **kwargs)
