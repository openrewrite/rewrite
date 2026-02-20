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

This module provides a client that communicates with the ty-types binary via
line-delimited JSON-RPC over stdin/stdout. The ty-types CLI returns all node
types in a single batch call per file, with structured type descriptors and
cross-file deduplication.
"""

from __future__ import annotations

import json
import select
import subprocess
from pathlib import Path
from typing import Any, Dict, Optional


class TyTypesClient:
    """Client for the ty-types CLI.

    This client starts `ty-types --serve` as a subprocess and communicates via
    line-delimited JSON-RPC over stdin/stdout. Create an instance per parse
    batch and close it when done.

    Note: This client is NOT thread-safe. Use one instance per thread.

    Usage:
        with TyTypesClient() as client:
            client.initialize("/path/to/project")
            result = client.get_types("/path/to/file.py")
    """

    def __init__(self):
        self._process: Optional[subprocess.Popen] = None
        self._request_id: int = 0
        self._initialized = False
        self._project_root: Optional[str] = None

        self._start_process()

    def __enter__(self) -> TyTypesClient:
        return self

    def __exit__(self, exc_type, exc_val, exc_tb) -> None:
        self.shutdown()

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

    def _send_request(self, method: str, params: Optional[Dict[str, Any]] = None,
                      timeout: float = 0) -> Optional[Any]:
        """Send a JSON-RPC request and read the response.

        Args:
            method: The JSON-RPC method name.
            params: Optional parameters for the request.
            timeout: Maximum seconds to wait for a response (0 = no limit).
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
            self._process.stdin.write(line.encode('utf-8'))
            self._process.stdin.flush()

            if self._process.stdout is None:
                return None

            if timeout > 0:
                ready, _, _ = select.select([self._process.stdout], [], [], timeout)
                if not ready:
                    return None

            response_line = self._process.stdout.readline().decode('utf-8')
            if not response_line:
                return None
            response = json.loads(response_line)

            if response.get('id') != self._request_id:
                return None

            if response.get('error') is not None:
                return None

            return response.get('result')
        except (BrokenPipeError, OSError, json.JSONDecodeError):
            return None

    def initialize(self, project_root: str) -> bool:
        """Initialize the ty-types session with a project root.

        If already initialized with the same project root, this is a no-op.
        If initialized with a different root, shuts down and reinitializes.
        """
        if self._initialized and self._project_root == project_root:
            return True

        if self._initialized:
            self._send_request("shutdown")
            self._initialized = False
            self._project_root = None
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

    def get_types(self, file_path: str, timeout: float = 30,
                  include_display: bool = False) -> Optional[Dict[str, Any]]:
        """Get all node types for a Python file.

        Args:
            file_path: Absolute path to the Python file.
            timeout: Maximum seconds to wait for a response (default 30s).
                     Some files with recursive types can cause ty to hang.
            include_display: Whether to include display strings in type
                           descriptors (default False — uses structured data).
        """
        if not self._initialized:
            return None
        return self._send_request(
            "getTypes",
            {"file": file_path, "includeDisplay": include_display},
            timeout=timeout,
        )

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
