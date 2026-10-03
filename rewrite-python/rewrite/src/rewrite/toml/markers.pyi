# Auto-generated stub file for IDE autocomplete support.
# Do not edit manually - regenerate with: python scripts/generate_stubs.py

from dataclasses import dataclass
from typing import Any, ClassVar, List, Optional
from typing_extensions import Self
from uuid import UUID
import weakref

from rewrite import Marker

@dataclass(frozen=True)
class ArrayTable(Marker):
    _id: UUID

    def replace(self, **kwargs: Any) -> Self: ...

@dataclass(frozen=True)
class InlineTable(Marker):
    _id: UUID

    def replace(self, **kwargs: Any) -> Self: ...
