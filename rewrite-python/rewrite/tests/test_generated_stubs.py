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

"""Fail when a committed ``.pyi`` stub differs from what ``scripts/generate_stubs.py`` renders
for its ``.py``. Type checkers read the stub in preference to the source, so drift ships wrong
types while the code runs fine."""

import importlib.util
from pathlib import Path

import pytest

PROJECT_ROOT = Path(__file__).parents[1]
_spec = importlib.util.spec_from_file_location("generate_stubs", PROJECT_ROOT / "scripts" / "generate_stubs.py")
generate_stubs = importlib.util.module_from_spec(_spec)
_spec.loader.exec_module(generate_stubs)

SOURCES = sorted(generate_stubs.find_lst_files(PROJECT_ROOT / "src" / "rewrite"))


@pytest.mark.parametrize("source", SOURCES, ids=lambda p: str(p.relative_to(PROJECT_ROOT / "src")))
def test_committed_stub_matches_generator(source: Path):
    expected = generate_stubs.generate_stub_content(source)
    if "class " not in expected:
        pytest.skip("the generator writes no stub for this file")
    stub = source.with_suffix(".pyi")
    assert stub.exists() and stub.read_text() == expected, (
        f"{stub.relative_to(PROJECT_ROOT)} is stale; regenerate with: python scripts/generate_stubs.py"
    )
