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

import pytest

from rewrite.java import JavaType


@pytest.mark.parametrize("instance", [
    JavaType.Class(),
    JavaType.GenericTypeVariable(),
    JavaType.Union(),
    JavaType.Intersection(),
    JavaType.Primitive.Int,
    JavaType.Method(),
    JavaType.Variable(),
    JavaType.Array(),
], ids=lambda t: type(t).__qualname__)
def test_every_java_type_is_a_java_type(instance):
    assert isinstance(instance, JavaType)


def test_java_type_base_keeps_its_subclasses_slotted():
    assert not hasattr(JavaType.Method(), '__dict__')
