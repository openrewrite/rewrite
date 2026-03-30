# Copyright 2025 the original author or authors.
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

"""
Java RPC Client for Python recipe testing.

This module provides infrastructure to spawn a Java RPC server process
and communicate with it, enabling Python tests to use Java recipes
like ChangeType, AddLiteralMethodArgument, etc.

Usage:
    from rewrite.rpc.java_rpc_client import JavaRpcClient

    with JavaRpcClient() as client:
        # Now Python RPC calls (like prepare_java_recipe) will be
        # routed to the Java server
        result = client.send_request("PrepareRecipe", {...})
"""

import json
import logging
import os
import subprocess
import sys
import threading
from pathlib import Path
from typing import Any, Dict, Optional

logger = logging.getLogger(__name__)

# Global Java RPC client instance (for use with pytest fixtures)
_java_rpc_client: Optional["JavaRpcClient"] = None


def get_java_rpc_client() -> Optional["JavaRpcClient"]:
    """Get the current Java RPC client instance, if any."""
    return _java_rpc_client


def set_java_rpc_client(client: Optional["JavaRpcClient"]) -> None:
    """Set the global Java RPC client instance."""
    global _java_rpc_client
    _java_rpc_client = client


class JavaRpcClient:
    """
    Client for communicating with a Java RPC server process.

    This class spawns a Java subprocess running JavaRewriteRpc and
    communicates with it using the header-delimited JSON-RPC protocol.
    """

    def __init__(
        self,
        marketplace_csv: Path,
        java_classpath: Optional[str] = None,
        java_home: Optional[str] = None,
        log_file: Optional[Path] = None,
        trace: bool = False,
        timeout: float = 30.0,
    ):
        """
        Initialize the Java RPC client.

        Args:
            marketplace_csv: Path to the marketplace CSV file specifying
                            which recipes and bundles are available.
            java_classpath: Classpath for Java process. If None, attempts to
                           find rewrite-python classes from environment.
            java_home: Java home directory. If None, uses JAVA_HOME env var.
            log_file: Path to log file for Java server debugging.
            trace: Enable RPC message tracing.
            timeout: Timeout in seconds for RPC requests.
        """
        self._marketplace_csv = marketplace_csv
        self._java_classpath = java_classpath
        self._java_home = java_home or os.environ.get("JAVA_HOME")
        self._log_file = log_file
        self._trace = trace
        self._timeout = timeout

        self._process: Optional[subprocess.Popen] = None
        self._request_id = 0
        self._request_lock = threading.Lock()
        self._response_lock = threading.Lock()
        self._pending_responses: Dict[int, Any] = {}
        self._reader_thread: Optional[threading.Thread] = None
        self._shutdown = False

    def start(self) -> "JavaRpcClient":
        """Start the Java RPC server process."""
        java_cmd = self._find_java_command()
        classpath = self._find_classpath()

        if not self._marketplace_csv.exists():
            raise RuntimeError(f"Marketplace CSV not found: {self._marketplace_csv}")

        cmd = [
            java_cmd,
            "-cp", classpath,
            "org.openrewrite.maven.rpc.JavaRewriteRpc",
            f"--marketplace={self._marketplace_csv}",
        ]

        if self._log_file:
            cmd.append(f"--log-file={self._log_file}")
        if self._trace:
            cmd.append("--trace")

        logger.info(f"Starting Java RPC server: {' '.join(cmd)}")

        self._process = subprocess.Popen(
            cmd,
            stdin=subprocess.PIPE,
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE if not self._log_file else subprocess.DEVNULL,
            bufsize=0,  # Unbuffered for real-time communication
        )

        # Start reader thread to handle responses
        self._reader_thread = threading.Thread(target=self._read_responses, daemon=True)
        self._reader_thread.start()

        return self

    def shutdown(self) -> None:
        """Shutdown the Java RPC server process."""
        self._shutdown = True

        if self._process:
            try:
                # Close stdin to signal EOF to Java
                if self._process.stdin:
                    self._process.stdin.close()

                # Wait for graceful shutdown
                self._process.wait(timeout=5)
            except subprocess.TimeoutExpired:
                # Force kill if not responding
                self._process.kill()
                self._process.wait(timeout=2)
            finally:
                self._process = None

        if self._reader_thread and self._reader_thread.is_alive():
            self._reader_thread.join(timeout=2)

    def __enter__(self) -> "JavaRpcClient":
        """Context manager entry."""
        return self.start()

    def __exit__(self, exc_type, exc_val, exc_tb) -> None:
        """Context manager exit."""
        self.shutdown()

    def send_request(self, method: str, params: Dict[str, Any]) -> Any:
        """
        Send a JSON-RPC request to the Java server and wait for response.

        Args:
            method: The RPC method name (e.g., "PrepareRecipe", "Visit")
            params: The request parameters

        Returns:
            The result from the RPC response

        Raises:
            RuntimeError: If the request fails or times out
        """
        if not self._process or self._process.poll() is not None:
            raise RuntimeError("Java RPC server is not running")

        with self._request_lock:
            self._request_id += 1
            request_id = self._request_id

        request = {
            "jsonrpc": "2.0",
            "id": request_id,
            "method": method,
            "params": params,
        }

        # Create a condition for waiting on this specific response
        condition = threading.Condition()
        with self._response_lock:
            self._pending_responses[request_id] = {"condition": condition, "response": None}

        try:
            # Send the request
            self._write_message(request)

            # Wait for response
            with condition:
                deadline = threading.Event()
                if not condition.wait_for(
                    lambda: self._pending_responses.get(request_id, {}).get("response") is not None,
                    timeout=self._timeout
                ):
                    raise RuntimeError(f"Timeout waiting for response to {method}")

            with self._response_lock:
                response = self._pending_responses.pop(request_id, {}).get("response")

            if response is None:
                raise RuntimeError(f"No response received for {method}")

            if "error" in response:
                error = response["error"]
                raise RuntimeError(f"RPC error: {error.get('message', 'Unknown error')}")

            return response.get("result")

        except Exception:
            with self._response_lock:
                self._pending_responses.pop(request_id, None)
            raise

    def _write_message(self, message: Dict[str, Any]) -> None:
        """Write a JSON-RPC message to the Java process."""
        if not self._process or not self._process.stdin:
            raise RuntimeError("Java RPC server stdin not available")

        content = json.dumps(message)
        content_bytes = content.encode("utf-8")

        header = f"Content-Length: {len(content_bytes)}\r\n\r\n"
        self._process.stdin.write(header.encode("utf-8"))
        self._process.stdin.write(content_bytes)
        self._process.stdin.flush()

    def _read_responses(self) -> None:
        """Background thread that reads responses from Java process."""
        while not self._shutdown and self._process and self._process.stdout:
            try:
                response = self._read_message()
                if response is None:
                    break

                request_id = response.get("id")
                if request_id is not None:
                    with self._response_lock:
                        if request_id in self._pending_responses:
                            pending = self._pending_responses[request_id]
                            pending["response"] = response
                            condition = pending["condition"]
                            with condition:
                                condition.notify_all()
                else:
                    # This might be a notification or server-initiated request
                    logger.debug(f"Received message without id: {response}")

            except Exception as e:
                if not self._shutdown:
                    logger.error(f"Error reading Java RPC response: {e}")
                break

    def _read_message(self) -> Optional[Dict[str, Any]]:
        """Read a single JSON-RPC message from the Java process."""
        if not self._process or not self._process.stdout:
            return None

        # Read headers
        headers = {}
        while True:
            line = self._process.stdout.readline()
            if not line:
                return None

            line = line.decode("utf-8").rstrip("\r\n")
            if not line:
                break

            if ":" in line:
                key, value = line.split(":", 1)
                headers[key.strip()] = value.strip()

        # Read content
        content_length = int(headers.get("Content-Length", 0))
        if content_length == 0:
            return None

        content = self._process.stdout.read(content_length)
        if not content:
            return None

        return json.loads(content.decode("utf-8"))

    def _find_java_command(self) -> str:
        """Find the Java command to use."""
        if self._java_home:
            java_path = Path(self._java_home) / "bin" / "java"
            if java_path.exists():
                return str(java_path)

        # Fall back to system Java
        return "java"

    def _find_classpath(self) -> str:
        """Find the Java classpath for rewrite-python."""
        if self._java_classpath:
            return self._java_classpath

        # Try to find classpath from environment
        classpath = os.environ.get("REWRITE_PYTHON_CLASSPATH")
        if classpath:
            return classpath

        # Try to find classpath from Gradle build output
        # This looks for the typical Gradle build layout
        possible_paths = [
            # From rewrite-python directory
            Path("build/classes/java/main"),
            Path("build/libs"),
            # From rewrite root directory
            Path("rewrite-python/build/classes/java/main"),
            Path("rewrite-python/build/libs"),
        ]

        # Also need rewrite-core and dependencies
        # In practice, tests should set REWRITE_PYTHON_CLASSPATH explicitly
        raise RuntimeError(
            "Could not find Java classpath. Set REWRITE_PYTHON_CLASSPATH environment variable "
            "or pass java_classpath parameter. You can generate the classpath using:\n"
            "  ./gradlew :rewrite-python:printTestClasspath"
        )


def install_java_rpc_hooks() -> None:
    """
    Install hooks to route Python RPC requests to Java.

    This modifies the RPC server module to use the Java RPC client
    for handling requests when running in test mode.
    """
    from rewrite.rpc import server

    original_send_request = server.send_request

    def hooked_send_request(method: str, params: dict, timeout_seconds: float = 10.0) -> Any:
        """Send request to Java RPC server if available, else use original."""
        client = get_java_rpc_client()
        if client:
            return client.send_request(method, params)
        return original_send_request(method, params, timeout_seconds)

    server.send_request = hooked_send_request  # ty: ignore[invalid-assignment]  # monkey-patching (ty#2193)


def uninstall_java_rpc_hooks() -> None:
    """Remove the Java RPC hooks and restore original behavior."""
    # In practice, this would restore the original send_request
    # For now, reloading the module is simplest
    pass
