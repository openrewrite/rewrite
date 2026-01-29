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
LSP client for the ty type checker.

This module provides a long-lived LSP client that communicates with ty's
language server for Python type attribution. The client is lazily initialized
on first use and persists for the lifetime of the RPC session.
"""

from __future__ import annotations

import atexit
import json
import os
import subprocess
import threading
from pathlib import Path
from typing import Any, Dict, List, Optional


class TyLspClient:
    """Long-lived LSP client for ty type checker.

    This client starts `ty server` as a subprocess and communicates via the
    Language Server Protocol over stdin/stdout. The client is a lazy singleton
    that initializes on first access and shuts down when the process exits.

    Usage:
        client = TyLspClient.get()
        client.open_document("file:///path/to/file.py", source_code)
        hover_info = client.get_hover("file:///path/to/file.py", line=10, character=5)
    """

    _instance: Optional[TyLspClient] = None
    _lock: threading.Lock = threading.Lock()

    @classmethod
    def get(cls, venv_path: Optional[Path] = None) -> TyLspClient:
        """Get or create the singleton TyLspClient instance.

        Args:
            venv_path: Optional path to a virtual environment. If provided,
                      ty will be configured to use this environment for
                      type resolution.

        Returns:
            The singleton TyLspClient instance.

        Note:
            If the requested venv_path differs from the current instance's venv,
            the instance will be reset and recreated with the new venv.
        """
        with cls._lock:
            # Check if we need to reset due to venv change
            if cls._instance is not None and venv_path is not None:
                current_venv = cls._instance._venv_path
                if current_venv != venv_path:
                    # Venv changed, reset the instance
                    cls._instance.shutdown()
                    cls._instance = None

            if cls._instance is None:
                cls._instance = cls(venv_path)
                atexit.register(cls._instance.shutdown)
            return cls._instance

    @classmethod
    def reset(cls) -> None:
        """Reset the singleton instance, shutting down any existing client."""
        with cls._lock:
            if cls._instance is not None:
                cls._instance.shutdown()
                cls._instance = None

    def __init__(self, venv_path: Optional[Path] = None):
        """Initialize the ty LSP client.

        Args:
            venv_path: Optional path to a virtual environment.
        """
        self._process: Optional[subprocess.Popen] = None
        self._request_id: int = 0
        self._venv_path = venv_path
        self._initialized = False
        self._read_lock = threading.Lock()
        self._write_lock = threading.Lock()

        self._start_server()

    def _start_server(self) -> None:
        """Start the ty language server subprocess."""
        env = os.environ.copy()

        # Find the ty executable
        ty_path = self._find_ty_executable()
        if ty_path is None:
            self._process = None
            raise RuntimeError(
                "ty is not installed. Install it with: pip install ty"
            )

        cmd = [str(ty_path), 'server']

        if self._venv_path:
            # Configure environment to use the virtual environment
            env['VIRTUAL_ENV'] = str(self._venv_path)
            bin_path = self._venv_path / 'bin'
            env['PATH'] = f"{bin_path}:{env.get('PATH', '')}"

        try:
            self._process = subprocess.Popen(
                cmd,
                stdin=subprocess.PIPE,
                stdout=subprocess.PIPE,
                stderr=subprocess.PIPE,
                env=env,
            )
            self._initialize_lsp()
        except FileNotFoundError:
            # ty is not installed
            self._process = None
            raise RuntimeError(
                "ty is not installed. Install it with: pip install ty"
            )

    def _find_ty_executable(self) -> Optional[Path]:
        """Find the ty executable.

        Searches in order:
        1. The specified venv_path's bin directory
        2. The current Python interpreter's venv bin directory
        3. The system PATH
        """
        import shutil
        import sys

        # Check the specified venv first
        if self._venv_path:
            ty_in_venv = self._venv_path / 'bin' / 'ty'
            if ty_in_venv.exists():
                return ty_in_venv

        # Check the current Python's venv
        venv = os.environ.get('VIRTUAL_ENV')
        if venv:
            ty_in_venv = Path(venv) / 'bin' / 'ty'
            if ty_in_venv.exists():
                return ty_in_venv

        # Check adjacent to the Python interpreter
        python_dir = Path(sys.executable).parent
        ty_next_to_python = python_dir / 'ty'
        if ty_next_to_python.exists():
            return ty_next_to_python

        # Fall back to PATH lookup
        ty_in_path = shutil.which('ty')
        if ty_in_path:
            return Path(ty_in_path)

        return None

    def _initialize_lsp(self) -> None:
        """Send LSP initialize request and wait for response."""
        if self._process is None:
            return

        response = self._send_request("initialize", {
            "processId": os.getpid(),
            "capabilities": {
                "textDocument": {
                    "hover": {
                        "contentFormat": ["plaintext", "markdown"]
                    }
                }
            },
            "rootUri": None,
            "initializationOptions": {}
        })

        if response is not None:
            self._initialized = True
            # Send initialized notification
            self._send_notification("initialized", {})

    def _send_request(self, method: str, params: Dict[str, Any]) -> Optional[Any]:
        """Send an LSP request and wait for the response.

        Args:
            method: The LSP method name (e.g., "textDocument/hover").
            params: The request parameters.

        Returns:
            The result from the response, or None if the request failed.
        """
        if self._process is None or self._process.stdin is None:
            return None

        with self._write_lock:
            self._request_id += 1
            request = {
                "jsonrpc": "2.0",
                "id": self._request_id,
                "method": method,
                "params": params
            }

            content = json.dumps(request)
            message = f"Content-Length: {len(content)}\r\n\r\n{content}"

            try:
                self._process.stdin.write(message.encode('utf-8'))
                self._process.stdin.flush()
            except (BrokenPipeError, OSError):
                return None

        return self._read_response(self._request_id)

    def _send_notification(self, method: str, params: Dict[str, Any]) -> None:
        """Send an LSP notification (no response expected).

        Args:
            method: The LSP method name.
            params: The notification parameters.
        """
        if self._process is None or self._process.stdin is None:
            return

        with self._write_lock:
            notification = {
                "jsonrpc": "2.0",
                "method": method,
                "params": params
            }

            content = json.dumps(notification)
            message = f"Content-Length: {len(content)}\r\n\r\n{content}"

            try:
                self._process.stdin.write(message.encode('utf-8'))
                self._process.stdin.flush()
            except (BrokenPipeError, OSError):
                pass

    def _read_response(self, request_id: int) -> Optional[Any]:
        """Read and parse an LSP response.

        Args:
            request_id: The ID of the request we're waiting for.

        Returns:
            The result from the response, or None if reading failed.
        """
        if self._process is None or self._process.stdout is None:
            return None

        with self._read_lock:
            try:
                # Read headers until we get an empty line
                headers: Dict[str, str] = {}
                while True:
                    line = self._process.stdout.readline().decode('utf-8')
                    if line == '\r\n' or line == '\n' or line == '':
                        break
                    if ':' in line:
                        key, value = line.split(':', 1)
                        headers[key.strip().lower()] = value.strip()

                # Read content based on Content-Length
                content_length = int(headers.get('content-length', 0))
                if content_length == 0:
                    return None

                content = self._process.stdout.read(content_length).decode('utf-8')
                response = json.loads(content)

                # Handle notifications and other messages
                while response.get('id') != request_id:
                    if 'id' not in response:
                        # This is a notification, skip it
                        # Read next message
                        headers = {}
                        while True:
                            line = self._process.stdout.readline().decode('utf-8')
                            if line == '\r\n' or line == '\n' or line == '':
                                break
                            if ':' in line:
                                key, value = line.split(':', 1)
                                headers[key.strip().lower()] = value.strip()

                        content_length = int(headers.get('content-length', 0))
                        if content_length == 0:
                            return None
                        content = self._process.stdout.read(content_length).decode('utf-8')
                        response = json.loads(content)
                    else:
                        break

                if 'error' in response:
                    return None

                return response.get('result')

            except (json.JSONDecodeError, OSError, ValueError):
                return None

    def open_document(self, uri: str, content: str) -> None:
        """Notify ty that a document has been opened.

        This allows ty to analyze the document content without requiring
        the file to exist on disk.

        Args:
            uri: The document URI (e.g., "file:///path/to/file.py").
            content: The document content.
        """
        self._send_notification("textDocument/didOpen", {
            "textDocument": {
                "uri": uri,
                "languageId": "python",
                "version": 1,
                "text": content
            }
        })

    def close_document(self, uri: str) -> None:
        """Notify ty that a document has been closed.

        Args:
            uri: The document URI.
        """
        self._send_notification("textDocument/didClose", {
            "textDocument": {"uri": uri}
        })

    def get_hover(self, uri: str, line: int, character: int) -> Optional[str]:
        """Get type information at a specific position.

        Args:
            uri: The document URI.
            line: Zero-based line number.
            character: Zero-based character offset.

        Returns:
            The hover content (type information) as a string, or None if
            no type information is available at that position.
        """
        response = self._send_request("textDocument/hover", {
            "textDocument": {"uri": uri},
            "position": {"line": line, "character": character}
        })

        if response is None:
            return None

        contents = response.get("contents")
        if contents is None:
            return None

        # Handle different content formats
        if isinstance(contents, str):
            return contents
        elif isinstance(contents, dict):
            # MarkupContent format
            return contents.get("value", "")
        elif isinstance(contents, list):
            # MarkedString[] format
            values = []
            for item in contents:
                if isinstance(item, str):
                    values.append(item)
                elif isinstance(item, dict):
                    values.append(item.get("value", ""))
            return "\n".join(values)

        return None

    def get_diagnostics(self, uri: str) -> List[Dict[str, Any]]:
        """Get diagnostics (type errors) for a document.

        Args:
            uri: The document URI.

        Returns:
            A list of diagnostic objects.
        """
        # Diagnostics are typically pushed via notifications, not pulled.
        # This is a placeholder for future implementation if needed.
        return []

    @property
    def is_available(self) -> bool:
        """Check if the ty server is available and initialized."""
        return self._process is not None and self._initialized

    def shutdown(self) -> None:
        """Gracefully shut down the ty server."""
        if self._process is None:
            return

        try:
            # Send shutdown request
            self._send_request("shutdown", {})
            # Send exit notification
            self._send_notification("exit", {})
            # Wait for process to terminate
            self._process.wait(timeout=5)
        except (subprocess.TimeoutExpired, OSError):
            # Force kill if graceful shutdown fails
            if self._process is not None:
                self._process.kill()
        finally:
            self._process = None
            self._initialized = False
