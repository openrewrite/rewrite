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

from rewrite.java.support_types import JavaType as JT

from .test_cyclic_java_type_roundtrip import _round_trip_type

CLASS_FLAGS = 1 | 1 << 4  # Public, Final
# Bit 20 is not a Java flag. C# sets it on extension methods.
METHOD_FLAGS = 1 | 1 << 43 | 1 << 20  # Public, Default
VARIABLE_FLAGS = 1 << 1 | 1 << 3 | 1 << 4  # Private, Static, Final


def test_flags_of_class_method_and_variable_round_trip():
    cls = JT.Class(_flags_bit_map=CLASS_FLAGS, _kind=JT.FullyQualified.Kind.Class,
                   _fully_qualified_name='my.Example')
    cls._methods = [JT.Method(_flags_bit_map=METHOD_FLAGS, _declaring_type=cls, _name='run',
                              _return_type=JT.Primitive.Void)]
    cls._members = [JT.Variable(_flags_bit_map=VARIABLE_FLAGS, _name='count', _owner=cls,
                                _type=JT.Primitive.Int)]

    received = _round_trip_type(cls)

    assert received._flags_bit_map == CLASS_FLAGS
    assert received._methods[0]._flags_bit_map == METHOD_FLAGS
    assert received._members[0]._flags_bit_map == VARIABLE_FLAGS
