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

"""Cached workspaces with installed PyPI dependencies for ty type attribution."""

from __future__ import annotations

import fcntl
import hashlib
import logging
import os
import shutil
import subprocess
import tempfile
from typing import Dict, Tuple

logger = logging.getLogger(__name__)

WORKSPACE_BASE = os.path.join(tempfile.gettempdir(), "openrewrite-python-template-workspaces")
WORKSPACE_VERSION = "1"


class DependencyWorkspace:
    """Cached workspaces with installed PyPI dependencies for ty type attribution.

    Creates and caches virtual environments with specified PyPI packages installed,
    so that ``TyTypesClient`` can resolve types during template parsing.

    The caching strategy mirrors the Java ``DependencyWorkspace``:

    * Hash dependencies to produce a stable directory name.
    * Check an in-memory cache, then look for a valid workspace on disk.
    * If neither exists, write a ``pyproject.toml``, run ``uv sync``, and
      atomically rename the temp directory into place.
    """

    _cache: Dict[str, str] = {}

    @classmethod
    def get_or_create(cls, dependencies: Tuple[Tuple[str, str], ...]) -> str:
        """Return the path to a workspace with *dependencies* installed.

        Parameters
        ----------
        dependencies:
            ``(package, version)`` pairs, e.g. ``(("requests", ">=2.31.0"),)``.
            Bare versions are normalized to ``>=``.

        Returns
        -------
        str
            Absolute path to the workspace directory (contains ``.venv/``).
        """
        cache_key = cls._hash_dependencies(dependencies)

        # 1. In-memory cache
        if cache_key in cls._cache:
            workspace = cls._cache[cache_key]
            if cls._is_valid(workspace):
                return workspace

        # 2. Disk cache
        workspace = os.path.join(WORKSPACE_BASE, cache_key)
        if cls._is_valid(workspace):
            cls._cache[cache_key] = workspace
            return workspace

        # 3. Create new workspace
        workspace = cls._create_workspace(dependencies, cache_key)
        cls._cache[cache_key] = workspace
        return workspace

    @classmethod
    def _hash_dependencies(cls, dependencies: Tuple[Tuple[str, str], ...]) -> str:
        """Produce a stable hash string from sorted dependencies."""
        normalized = sorted(dependencies)
        content = "\n".join(f"{pkg}=={ver}" for pkg, ver in normalized)
        digest = hashlib.sha256(content.encode()).hexdigest()
        return digest[:16]

    @classmethod
    def _is_valid(cls, workspace: str) -> bool:
        """Check whether *workspace* looks like a valid, ready workspace."""
        if not os.path.isdir(workspace):
            return False
        venv = os.path.join(workspace, ".venv")
        if not os.path.isdir(venv):
            return False
        version_file = os.path.join(workspace, "version.txt")
        if not os.path.isfile(version_file):
            return False
        try:
            with open(version_file) as f:
                return f.read().strip() == WORKSPACE_VERSION
        except OSError:
            return False

    @classmethod
    def get_or_create_from_pyproject(cls, pyproject_content: str) -> str:
        """Return the path to a workspace created from raw ``pyproject.toml`` content.

        This mirrors the Java ``DependencyWorkspace.getOrCreateWorkspace(pyprojectContent)``
        API.  The content is hashed directly (rather than normalizing dependencies)
        so any ``pyproject.toml`` — including those with extras, optional groups,
        or tool sections — is supported.

        Parameters
        ----------
        pyproject_content:
            Complete ``pyproject.toml`` file content.

        Returns
        -------
        str
            Absolute path to the workspace directory (contains ``.venv/``).
        """
        cache_key = cls._hash_content(pyproject_content)

        # 1. In-memory cache
        if cache_key in cls._cache:
            workspace = cls._cache[cache_key]
            if cls._is_valid(workspace):
                return workspace

        # 2. Disk cache
        workspace = os.path.join(WORKSPACE_BASE, cache_key)
        if cls._is_valid(workspace):
            cls._cache[cache_key] = workspace
            return workspace

        # 3. Create new workspace
        workspace = cls._create_workspace_with_content(pyproject_content, cache_key)
        cls._cache[cache_key] = workspace
        return workspace

    @classmethod
    def _hash_content(cls, content: str) -> str:
        """Produce a stable hash string from arbitrary content."""
        digest = hashlib.sha256(content.encode()).hexdigest()
        return digest[:16]

    @classmethod
    def _create_workspace(
        cls,
        dependencies: Tuple[Tuple[str, str], ...],
        cache_key: str,
    ) -> str:
        """Create a new workspace from dependency tuples."""
        pyproject = cls._generate_pyproject(dependencies)
        return cls._create_workspace_with_content(pyproject, cache_key)

    @classmethod
    def _create_workspace_with_content(
        cls,
        pyproject_content: str,
        cache_key: str,
    ) -> str:
        """Create a new workspace, install dependencies, and move it into place."""
        os.makedirs(WORKSPACE_BASE, exist_ok=True)

        final_dir = os.path.join(WORKSPACE_BASE, cache_key)
        lock_path = os.path.join(WORKSPACE_BASE, f".{cache_key}.lock")

        # Serialize concurrent creators for the same cache_key. Holding the lock
        # across uv sync is intentional — only one process should build the same
        # workspace at a time.
        with open(lock_path, "w") as lock_file:
            fcntl.flock(lock_file.fileno(), fcntl.LOCK_EX)

            # Another holder may have built it while we waited for the lock.
            if cls._is_valid(final_dir):
                return final_dir

            # Remove any half-built leftover at the target. macOS rename(2)
            # refuses to replace a non-empty directory (ENOTEMPTY), so we must
            # clear the target before renaming.
            if os.path.exists(final_dir):
                shutil.rmtree(final_dir)

            tmp_dir = os.path.join(
                WORKSPACE_BASE,
                f".tmp-{os.getpid()}-{cache_key}",
            )
            try:
                if os.path.exists(tmp_dir):
                    shutil.rmtree(tmp_dir)
                os.makedirs(tmp_dir)

                with open(os.path.join(tmp_dir, "pyproject.toml"), "w") as f:
                    f.write(pyproject_content)

                cls._run_uv_sync(tmp_dir)

                with open(os.path.join(tmp_dir, "version.txt"), "w") as f:
                    f.write(WORKSPACE_VERSION)

                os.rename(tmp_dir, final_dir)
                return final_dir

            except Exception:
                shutil.rmtree(tmp_dir, ignore_errors=True)
                raise

    _PEP508_COMPARATORS = ('>=', '<=', '==', '!=', '~=', '>', '<')

    @classmethod
    def _normalize_version(cls, version: str) -> str:
        """Ensure *version* has a PEP 508 comparator prefix, defaulting to ``>=``."""
        if any(version.startswith(op) for op in cls._PEP508_COMPARATORS):
            return version
        return f">={version}"

    @classmethod
    def _generate_pyproject(cls, dependencies: Tuple[Tuple[str, str], ...]) -> str:
        """Generate a minimal ``pyproject.toml`` for the given dependencies."""
        lines = [
            '[project]',
            'name = "openrewrite-template-workspace"',
            'version = "0.0.0"',
            'requires-python = ">=3.10"',
            'dependencies = [',
        ]
        for pkg, ver in sorted(dependencies):
            lines.append(f'    "{pkg}{cls._normalize_version(ver)}",')
        lines.append(']')
        return "\n".join(lines) + "\n"

    @classmethod
    def _run_uv_sync(cls, workspace: str) -> None:
        """Run ``uv sync`` in *workspace* to install dependencies."""
        try:
            result = subprocess.run(
                ["uv", "sync"],
                cwd=workspace,
                capture_output=True,
                text=True,
                timeout=300,
            )
            if result.returncode != 0:
                raise RuntimeError(
                    f"uv sync failed (exit {result.returncode}):\n{result.stderr}"
                )
        except FileNotFoundError:
            raise RuntimeError(
                "uv is not installed or not on PATH. "
                "Install it with: pip install uv"
            )
        except subprocess.TimeoutExpired:
            raise RuntimeError("uv sync timed out after 300 seconds")

    @classmethod
    def clear_cache(cls) -> None:
        """Clear the in-memory workspace cache."""
        cls._cache.clear()
