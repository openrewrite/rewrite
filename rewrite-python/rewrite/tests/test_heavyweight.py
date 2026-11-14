# Copyright 2026 the original author or authors.
#
# Licensed under the Moderne Source Available License (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     https://docs.moderne.io/licensing/moderne-source-available-license
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

from rewrite import random_id
from rewrite.java import Space
from rewrite.java.tree import Identifier
from rewrite.markers import Markers
from rewrite.python.markers import PythonResolutionResult


def test_heavyweight_keeps_its_subclasses_slotted():
    assert not hasattr(Identifier(random_id(), Space.EMPTY, Markers.EMPTY, [], "a", None, None), '__dict__')
    assert not hasattr(PythonResolutionResult.ResolvedDependency("requests", "2.32.3", None, None), '__dict__')
