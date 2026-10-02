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

"""PEP 696 — a type parameter default is part of the annotation scope (Python 3.13+)."""

from tests.python.test_scope import _scope_at_anchor


def test_a_type_parameter_reaches_the_default_of_a_later_one():
    assert _scope_at_anchor("""
        def f[List, T = anchor()]():
            pass
        """).declares('List') is True
