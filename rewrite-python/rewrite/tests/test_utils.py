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


from rewrite.utils import list_flat_map


def test_list_flat_map_keeps_items_before_the_first_change():
    lst = ["a", "b", "c"]
    assert list_flat_map(lambda x: [x, x] if x == "b" else [x], lst) == ["a", "b", "b", "c"]

    assert list_flat_map(lambda x: None if x == "b" else x, lst) == ["a", "c"]

    assert list_flat_map(lambda x: [x], lst) is lst
