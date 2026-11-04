# Copyright 2025 the original author or authors.
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
"""Which directories a ParseProject walk prunes."""
from pathlib import Path

from rewrite.rpc.server import DEFAULT_PARSE_EXCLUSIONS, _walk_python_files


def _sources(tmp_path, *relative_paths, exclusions=None):
    """Build a project from the given paths and return what the walk finds."""
    for relative in relative_paths:
        source = tmp_path / relative
        source.parent.mkdir(parents=True, exist_ok=True)
        source.write_text("x = 1\n")
    found = _walk_python_files(
        str(tmp_path), DEFAULT_PARSE_EXCLUSIONS if exclusions is None else exclusions
    )
    return sorted(Path(p).relative_to(tmp_path).as_posix() for p in found)


def test_build_output_is_pruned_at_the_project_root(tmp_path):
    assert _sources(tmp_path, 'build/out.py', 'dist/out.py', 'app.py') == ['app.py']


def test_build_and_dist_below_the_root_are_ordinary_packages(tmp_path):
    assert _sources(tmp_path, 'src/build/mod.py', 'src/dist/mod.py') == [
        'src/build/mod.py',
        'src/dist/mod.py',
    ]


def test_target_is_not_a_python_convention(tmp_path):
    assert _sources(tmp_path, 'target/mod.py') == ['target/mod.py']


def test_caches_and_dependency_trees_are_pruned_at_any_depth(tmp_path):
    assert _sources(
        tmp_path,
        'src/pkg/__pycache__/mod.py',
        'src/node_modules/dep/mod.py',
        'src/pkg/.venv/lib/mod.py',
        'src/pkg/mod.py',
    ) == ['src/pkg/mod.py']


def test_caller_exclusions_extend_the_defaults(tmp_path):
    assert _sources(
        tmp_path,
        'vendor/mod.py',
        'src/__pycache__/mod.py',
        'src/app.py',
        exclusions=DEFAULT_PARSE_EXCLUSIONS + ['vendor'],
    ) == ['src/app.py']
