# Auto-generated stub file for IDE autocomplete support.
# Do not edit manually - regenerate with: python scripts/generate_stubs.py

from dataclasses import dataclass
from typing import Any, ClassVar, List, Optional
from typing_extensions import Self
from uuid import UUID
import weakref

from rewrite.markers import Marker
from rewrite.utils import replace_if_changed

@dataclass(frozen=True)
class ArrayTable(Marker):
    _id: UUID

    def replace(self, **kwargs: Any) -> ArrayTable: ...

    @property
    def id(self) -> UUID: ...

@dataclass(frozen=True)
class InlineTable(Marker):
    _id: UUID

    def replace(self, **kwargs: Any) -> InlineTable: ...

    @property
    def id(self) -> UUID: ...
