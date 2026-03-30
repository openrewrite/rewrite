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

"""
Python dependency workspace management for type attribution.

This module provides caching of Python virtual environments for type
attribution with ty LSP. Similar to the JavaScript DependencyWorkspace,
it caches environments by hashing the pyproject.toml content.
"""

from __future__ import annotations

import hashlib
import logging
import os
import shutil
import subprocess
import tempfile
import threading
from pathlib import Path
from typing import Dict, Optional

logger = logging.getLogger(__name__)


class PythonDependencyWorkspace:
    """Caches Python virtual environments by pyproject.toml content hash.

    This class manages virtual environments for Python projects during parsing.
    Environments are cached based on a hash of the pyproject.toml content,
    allowing reuse across multiple parse operations with the same dependencies.

    Usage:
        workspace = PythonDependencyWorkspace.get_or_create(pyproject_content)
        venv_path = workspace / '.venv'
        # Configure ty with venv_path for type resolution
    """

    WORKSPACE_BASE = Path(tempfile.gettempdir()) / 'openrewrite-python-workspaces'

    # In-memory cache: hash -> workspace path
    _cache: Dict[str, Path] = {}
    _lock = threading.Lock()

    @classmethod
    def get_or_create(cls, pyproject_content: str) -> Path:
        """Get or create a workspace with dependencies installed.

        Args:
            pyproject_content: The content of pyproject.toml

        Returns:
            Path to the workspace directory containing .venv
        """
        content_hash = cls._hash_content(pyproject_content)[:16]

        with cls._lock:
            # Check in-memory cache
            if content_hash in cls._cache:
                workspace = cls._cache[content_hash]
                if cls._is_valid(workspace):
                    logger.debug(f"Using cached workspace: {workspace}")
                    return workspace

            # Check disk cache
            workspace = cls.WORKSPACE_BASE / content_hash
            if cls._is_valid(workspace):
                cls._cache[content_hash] = workspace
                logger.debug(f"Using disk-cached workspace: {workspace}")
                return workspace

            # Create new workspace
            logger.info(f"Creating new workspace: {workspace}")
            workspace = cls._create_workspace(content_hash, pyproject_content)
            cls._cache[content_hash] = workspace
            return workspace

    @classmethod
    def _hash_content(cls, content: str) -> str:
        """Generate a SHA-256 hash of the content."""
        return hashlib.sha256(content.encode('utf-8')).hexdigest()

    @classmethod
    def _is_valid(cls, workspace: Path) -> bool:
        """Check if a workspace is valid (has .venv with site-packages)."""
        if not workspace.exists():
            return False

        venv = workspace / '.venv'
        if not venv.exists():
            return False

        # Check for site-packages
        site_packages = venv / 'lib'
        if not site_packages.exists():
            return False

        return True

    @classmethod
    def _create_workspace(cls, content_hash: str, pyproject_content: str) -> Path:
        """Create a new workspace with dependencies installed.

        Uses atomic directory creation to handle concurrent access.
        """
        # Create temp workspace first (atomic creation pattern)
        temp_workspace = cls.WORKSPACE_BASE / f'.tmp-{content_hash}-{os.getpid()}'
        final_workspace = cls.WORKSPACE_BASE / content_hash

        try:
            # Ensure base directory exists
            cls.WORKSPACE_BASE.mkdir(parents=True, exist_ok=True)

            # Create temp workspace
            temp_workspace.mkdir(parents=True, exist_ok=True)

            # Write pyproject.toml
            pyproject_path = temp_workspace / 'pyproject.toml'
            pyproject_path.write_text(pyproject_content)

            # Create virtual environment with uv
            venv_path = temp_workspace / '.venv'
            cls._run_command(['uv', 'venv', str(venv_path)], cwd=temp_workspace)

            # Install dependencies
            cls._run_command(
                ['uv', 'pip', 'install', '-e', '.'],
                cwd=temp_workspace,
                env={**os.environ, 'VIRTUAL_ENV': str(venv_path)}
            )

            # Also install ty in the workspace for type stubs
            cls._run_command(
                ['uv', 'pip', 'install', 'ty'],
                cwd=temp_workspace,
                env={**os.environ, 'VIRTUAL_ENV': str(venv_path)}
            )

            # Atomic move to final location
            if final_workspace.exists():
                # Another process beat us to it, use that one
                shutil.rmtree(temp_workspace)
                return final_workspace

            temp_workspace.rename(final_workspace)
            return final_workspace

        except Exception as e:
            # Clean up on failure
            if temp_workspace.exists():
                shutil.rmtree(temp_workspace, ignore_errors=True)
            raise RuntimeError(f"Failed to create workspace: {e}") from e

    @classmethod
    def _run_command(
        cls,
        cmd: list,
        cwd: Path,
        env: Optional[Dict[str, str]] = None
    ) -> None:
        """Run a command and raise on failure."""
        try:
            result = subprocess.run(
                cmd,
                cwd=cwd,
                env=env,
                capture_output=True,
                text=True,
                timeout=300  # 5 minute timeout
            )
            if result.returncode != 0:
                logger.error(f"Command failed: {' '.join(cmd)}")
                logger.error(f"stdout: {result.stdout}")
                logger.error(f"stderr: {result.stderr}")
                raise RuntimeError(f"Command failed: {result.stderr}")
        except subprocess.TimeoutExpired:
            raise RuntimeError(f"Command timed out: {' '.join(cmd)}")
        except FileNotFoundError as e:
            if 'uv' in str(e):
                raise RuntimeError(
                    "uv is not installed. Install it with: pip install uv"
                ) from e
            raise

    @classmethod
    def clear_cache(cls) -> None:
        """Clear the in-memory cache. Useful for testing."""
        with cls._lock:
            cls._cache.clear()

    @classmethod
    def cleanup_old_workspaces(cls, max_age_days: int = 7) -> None:
        """Remove workspaces older than max_age_days."""
        import time

        if not cls.WORKSPACE_BASE.exists():
            return

        cutoff = time.time() - (max_age_days * 24 * 60 * 60)

        for workspace in cls.WORKSPACE_BASE.iterdir():
            if workspace.name.startswith('.tmp-'):
                # Clean up orphaned temp directories
                shutil.rmtree(workspace, ignore_errors=True)
                continue

            try:
                mtime = workspace.stat().st_mtime
                if mtime < cutoff:
                    logger.info(f"Removing old workspace: {workspace}")
                    shutil.rmtree(workspace)
            except OSError:
                pass

    @classmethod
    def symlink_venv(cls, source_workspace: Path, target_dir: Path) -> None:
        """Create a symlink from target_dir/.venv to source_workspace/.venv.

        Args:
            source_workspace: The cached workspace containing .venv
            target_dir: The test directory where .venv should be symlinked
        """
        source_venv = source_workspace / '.venv'
        target_venv = target_dir / '.venv'

        if target_venv.exists() or target_venv.is_symlink():
            if target_venv.is_symlink():
                target_venv.unlink()
            else:
                shutil.rmtree(target_venv)

        target_venv.symlink_to(source_venv)
