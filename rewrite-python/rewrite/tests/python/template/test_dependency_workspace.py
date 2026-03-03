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

"""Tests for DependencyWorkspace."""

import os
import shutil

import pytest

from rewrite.python.template.dependency_workspace import (
    DependencyWorkspace,
    WORKSPACE_BASE,
)


@pytest.fixture(autouse=True)
def _clear_cache():
    """Clear the in-memory cache before each test."""
    DependencyWorkspace.clear_cache()
    yield
    DependencyWorkspace.clear_cache()


class TestDependencyWorkspaceUnit:
    """Unit tests that don't require uv or network access."""

    def test_hash_content_stability(self):
        """Same content always produces the same hash."""
        content = "[project]\nname = \"test\"\n"
        h1 = DependencyWorkspace._hash_content(content)
        h2 = DependencyWorkspace._hash_content(content)
        assert h1 == h2

    def test_hash_content_varies(self):
        """Different content produces different hashes."""
        h1 = DependencyWorkspace._hash_content("[project]\nname = \"a\"\n")
        h2 = DependencyWorkspace._hash_content("[project]\nname = \"b\"\n")
        assert h1 != h2

    def test_hash_content_length(self):
        """Content hash is 16 characters long."""
        h = DependencyWorkspace._hash_content("test")
        assert len(h) == 16

    def test_hash_stability(self):
        """Same dependencies always produce the same hash."""
        deps = (("requests", "2.31.0"), ("flask", "3.0.0"))
        h1 = DependencyWorkspace._hash_dependencies(deps)
        h2 = DependencyWorkspace._hash_dependencies(deps)
        assert h1 == h2

    def test_hash_order_independent(self):
        """Dependency order doesn't affect the hash."""
        h1 = DependencyWorkspace._hash_dependencies(
            (("requests", "2.31.0"), ("flask", "3.0.0"))
        )
        h2 = DependencyWorkspace._hash_dependencies(
            (("flask", "3.0.0"), ("requests", "2.31.0"))
        )
        assert h1 == h2

    def test_hash_varies_by_package(self):
        """Different packages produce different hashes."""
        h1 = DependencyWorkspace._hash_dependencies((("requests", "2.31.0"),))
        h2 = DependencyWorkspace._hash_dependencies((("flask", "3.0.0"),))
        assert h1 != h2

    def test_hash_varies_by_version(self):
        """Different versions produce different hashes."""
        h1 = DependencyWorkspace._hash_dependencies((("requests", "2.31.0"),))
        h2 = DependencyWorkspace._hash_dependencies((("requests", "2.32.0"),))
        assert h1 != h2

    def test_hash_length(self):
        """Hash is 16 characters long."""
        h = DependencyWorkspace._hash_dependencies((("requests", "2.31.0"),))
        assert len(h) == 16

    def test_generate_pyproject(self):
        """Generated pyproject.toml contains correct dependencies."""
        deps = (("requests", "2.31.0"), ("flask", "3.0.0"))
        content = DependencyWorkspace._generate_pyproject(deps)

        assert '[project]' in content
        assert '"requests==2.31.0"' in content
        assert '"flask==3.0.0"' in content
        assert 'requires-python' in content

    def test_is_valid_nonexistent(self):
        """Non-existent directory is not valid."""
        assert DependencyWorkspace._is_valid("/nonexistent/path") is False

    def test_is_valid_empty_dir(self, tmp_path):
        """Empty directory is not valid."""
        assert DependencyWorkspace._is_valid(str(tmp_path)) is False

    def test_is_valid_missing_version(self, tmp_path):
        """Directory with .venv but no version.txt is not valid."""
        os.makedirs(tmp_path / ".venv")
        assert DependencyWorkspace._is_valid(str(tmp_path)) is False

    def test_is_valid_wrong_version(self, tmp_path):
        """Directory with wrong version is not valid."""
        os.makedirs(tmp_path / ".venv")
        (tmp_path / "version.txt").write_text("0")
        assert DependencyWorkspace._is_valid(str(tmp_path)) is False


def _uv_available() -> bool:
    """Check if uv is installed."""
    return shutil.which("uv") is not None


@pytest.mark.skipif(not _uv_available(), reason="uv not installed")
class TestDependencyWorkspaceIntegration:
    """Integration tests that require uv and network access."""

    @pytest.fixture(autouse=True)
    def _cleanup_workspace(self):
        """Clean up any workspace created during the test."""
        yield
        # Best-effort cleanup of test workspaces
        if os.path.isdir(WORKSPACE_BASE):
            for entry in os.listdir(WORKSPACE_BASE):
                if entry.startswith(".tmp-"):
                    shutil.rmtree(
                        os.path.join(WORKSPACE_BASE, entry),
                        ignore_errors=True,
                    )

    def test_get_or_create_returns_valid_workspace(self):
        """get_or_create returns a valid workspace path."""
        # Use a tiny, fast-installing package
        deps = (("six", "1.17.0"),)
        workspace = DependencyWorkspace.get_or_create(deps)

        assert os.path.isdir(workspace)
        assert os.path.isdir(os.path.join(workspace, ".venv"))
        assert os.path.isfile(os.path.join(workspace, "pyproject.toml"))
        assert DependencyWorkspace._is_valid(workspace)

    def test_get_or_create_caches(self):
        """Second call returns the same workspace (from cache)."""
        deps = (("six", "1.17.0"),)
        ws1 = DependencyWorkspace.get_or_create(deps)
        ws2 = DependencyWorkspace.get_or_create(deps)
        assert ws1 == ws2

    def test_get_or_create_from_pyproject(self):
        """get_or_create_from_pyproject creates a valid workspace."""
        content = (
            '[project]\n'
            'name = "test"\n'
            'version = "0.0.0"\n'
            'requires-python = ">=3.10"\n'
            'dependencies = ["six==1.17.0"]\n'
        )
        workspace = DependencyWorkspace.get_or_create_from_pyproject(content)

        assert os.path.isdir(workspace)
        assert os.path.isdir(os.path.join(workspace, ".venv"))
        assert DependencyWorkspace._is_valid(workspace)

    def test_get_or_create_from_pyproject_caches(self):
        """Second call with same content returns the same workspace."""
        content = (
            '[project]\n'
            'name = "test"\n'
            'version = "0.0.0"\n'
            'requires-python = ">=3.10"\n'
            'dependencies = ["six==1.17.0"]\n'
        )
        ws1 = DependencyWorkspace.get_or_create_from_pyproject(content)
        ws2 = DependencyWorkspace.get_or_create_from_pyproject(content)
        assert ws1 == ws2
