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
Client for the ty-types CLI.

This module provides a long-lived client that communicates with the ty-types
binary via line-delimited JSON-RPC over stdin/stdout. The ty-types CLI returns
all node types in a single batch call per file, with structured type
descriptors and cross-file deduplication.
"""

from __future__ import annotations

import atexit
import json
import subprocess
import threading
from pathlib import Path
from typing import Any, Dict, Optional


class TyTypesClient:
    """Client for the ty-types CLI.

    This client starts `ty-types` as a subprocess and communicates via
    line-delimited JSON-RPC over stdin/stdout. It is a lazy singleton
    that initializes on first access and shuts down when the process exits.

    Usage:
        client = TyTypesClient.get()
        client.initialize("/path/to/project")
        result = client.get_types("/path/to/file.py")
        # result = {"nodes": [...], "types": {...}}
    """

    _instance: Optional[TyTypesClient] = None
    _lock: threading.Lock = threading.Lock()

    @classmethod
    def get(cls) -> TyTypesClient:
        """Get or create the singleton TyTypesClient instance.

        Returns:
            The singleton TyTypesClient instance.
        """
        with cls._lock:
            if cls._instance is None:
                cls._instance = cls()
                atexit.register(cls._instance.shutdown)
            return cls._instance

    @classmethod
    def reset(cls) -> None:
        """Reset the singleton instance, shutting down any existing client."""
        with cls._lock:
            if cls._instance is not None:
                cls._instance.shutdown()
                cls._instance = None

    def __init__(self):
        """Initialize the ty-types client."""
        self._process: Optional[subprocess.Popen] = None
        self._request_id: int = 0
        self._initialized = False
        self._project_root: Optional[str] = None
        self._read_lock = threading.Lock()
        self._write_lock = threading.Lock()

        self._start_process()

    def _start_process(self) -> None:
        """Start the ty-types subprocess."""
        binary = self._find_binary()
        if binary is None:
            self._process = None
            raise RuntimeError(
                "ty-types is not installed. Ensure the ty-types binary is on PATH."
            )

        try:
            self._process = subprocess.Popen(
                [str(binary), '--serve'],
                stdin=subprocess.PIPE,
                stdout=subprocess.PIPE,
                stderr=subprocess.PIPE,
            )
        except FileNotFoundError:
            self._process = None
            raise RuntimeError(
                "ty-types is not installed. Ensure the ty-types binary is on PATH."
            )

    @staticmethod
    def _find_binary() -> Optional[Path]:
        """Find the ty-types binary via the ty-types package or PATH."""
        try:
            from ty_types.__main__ import find_ty_types_bin
            return Path(find_ty_types_bin())
        except (ImportError, FileNotFoundError):
            pass

        import shutil
        ty_types = shutil.which('ty-types')
        if ty_types:
            return Path(ty_types)
        return None

    def _send_request(self, method: str, params: Optional[Dict[str, Any]] = None) -> Optional[Any]:
        """Send a JSON-RPC request and read the response.

        Args:
            method: The JSON-RPC method name.
            params: Optional request parameters.

        Returns:
            The result from the response, or None if the request failed.
        """
        if self._process is None or self._process.stdin is None:
            return None

        self._request_id += 1
        request: Dict[str, Any] = {
            "jsonrpc": "2.0",
            "method": method,
            "id": self._request_id,
        }
        if params is not None:
            request["params"] = params

        line = json.dumps(request) + "\n"

        try:
            with self._write_lock:
                self._process.stdin.write(line.encode('utf-8'))
                self._process.stdin.flush()

            with self._read_lock:
                if self._process.stdout is None:
                    return None
                response_line = self._process.stdout.readline().decode('utf-8')
                if not response_line:
                    return None
                response = json.loads(response_line)

                if response.get('error') is not None:
                    return None

                return response.get('result')
        except (BrokenPipeError, OSError, json.JSONDecodeError):
            return None

    def initialize(self, project_root: str) -> bool:
        """Initialize the ty-types session with a project root.

        If already initialized with the same project root, this is a no-op.
        If initialized with a different root, shuts down and reinitializes.

        Args:
            project_root: Absolute path to the project root directory.

        Returns:
            True if initialization succeeded.
        """
        if self._initialized and self._project_root == project_root:
            return True

        if self._initialized:
            # Need to shutdown and reinitialize with new project root
            self._send_request("shutdown")
            self._initialized = False
            self._project_root = None
            # Restart the process
            if self._process is not None:
                try:
                    self._process.wait(timeout=5)
                except subprocess.TimeoutExpired:
                    self._process.kill()
            self._start_process()

        result = self._send_request("initialize", {"projectRoot": project_root})
        if result and result.get("ok"):
            self._initialized = True
            self._project_root = project_root
            return True
        return False

    def get_types(self, file_path: str) -> Optional[Dict[str, Any]]:
        """Get all node types for a Python file.

        Args:
            file_path: Path to the Python file (absolute or relative to project root).

        Returns:
            A dict with 'nodes' (list of NodeAttribution) and 'types' (dict of
            TypeId -> TypeDescriptor), or None if the request failed.
        """
        if not self._initialized:
            return None
        return self._send_request("getTypes", {"file": file_path})

    @property
    def is_available(self) -> bool:
        """Check if the ty-types process is available and initialized."""
        return self._process is not None and self._initialized

    def shutdown(self) -> None:
        """Gracefully shut down the ty-types process."""
        if self._process is None:
            return

        try:
            self._send_request("shutdown")
            self._process.wait(timeout=5)
        except (subprocess.TimeoutExpired, OSError):
            if self._process is not None:
                self._process.kill()
        finally:
            self._process = None
            self._initialized = False
            self._project_root = None
