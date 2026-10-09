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

from __future__ import annotations

from uuid import UUID

from rewrite import Marker
from rewrite.utils import lst_dataclass


@lst_dataclass
class ArrayTable(Marker):
    """Marks a ``Table`` printed as an array of tables, ``[[name]]``."""
    _id: UUID


@lst_dataclass
class InlineTable(Marker):
    """Marks a ``Table`` printed inline, ``{ k = v }``."""
    _id: UUID
