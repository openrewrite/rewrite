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
Java RPC client for Python-hosted recipe runs and tests.

Spawns a ``org.openrewrite.maven.rpc.JavaRewriteRpc`` JVM as a child process
and exchanges Content-Length-framed JSON-RPC over its stdin/stdout — the
Python-as-host mirror of the usual JVM-as-host deployment, and the same
transport the JavaScript module's ``JavaRpcTestServer`` uses.

The connection is fully bidirectional: while a request to Java is pending,
Java issues requests of its own back to Python (e.g. ``Visit`` makes Java call
``GetObject`` to fetch the tree and execution context being visited). The
transport is :class:`~rewrite.rpc.child_connection.ChildConnection`, whose
read loop dispatches such inbound requests — here to the regular server
handlers (``rewrite.rpc.server.handle_request``). Reads block without a
deadline; run RPC tests under pytest ``--timeout`` (see the module CLAUDE.md)
so a wedged peer fails the test instead of hanging the session.

Usage:
    from rewrite.rpc.java_rpc_client import JavaRpcClient

    with JavaRpcClient() as client:
        result = client.send_request("PrepareRecipe", {...})
"""

import logging
import os
import subprocess
from pathlib import Path
from typing import Any, Optional

from rewrite.rpc.child_connection import ChildConnection

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


def find_test_classpath() -> str | None:
    """Find the Java classpath for spawning the RPC peer.

    Resolution order:
      1. ``REWRITE_PYTHON_CLASSPATH`` environment variable.
      2. ``test-classpath.txt`` written by ``./gradlew
         :rewrite-python:generateTestClasspath``, looked up relative to the
         working directory and to this package's root.

    Returns None if no source is configured (callers typically skip).
    """
    classpath = os.environ.get("REWRITE_PYTHON_CLASSPATH")
    if classpath:
        return classpath

    package_root = Path(__file__).resolve().parents[3]
    for candidate in (Path("test-classpath.txt"),
                      Path("rewrite/test-classpath.txt"),
                      package_root / "test-classpath.txt"):
        if candidate.exists():
            return candidate.read_text().strip()
    return None


class JavaRpcClient:
    """
    Client for communicating with a Java RPC peer process.

    This class spawns a Java subprocess running JavaRewriteRpc and
    communicates with it using the header-delimited JSON-RPC protocol.
    """

    def __init__(
        self,
        marketplace_csv: Path | None = None,
        java_classpath: str | None = None,
        java_home: str | None = None,
        log_file: Path | None = None,
        trace: bool = False,
        command: list[str] | None = None,
    ):
        """
        Initialize the Java RPC client.

        Args:
            marketplace_csv: Optional path to a marketplace CSV specifying
                            which recipes and bundles are available. Without
                            it, the Java peer resolves recipes by class name
                            from its own classpath — the right default when
                            the wanted recipes (e.g. rewrite-java's) are
                            already on ``java_classpath``.
            java_classpath: Classpath for the Java process. If None, resolved
                           via :func:`find_test_classpath`.
            java_home: Java home directory. If None, uses JAVA_HOME env var.
            log_file: Path receiving the Java peer's stderr output. This
                     captures both JVM startup failures (bad classpath, etc.)
                     and everything the server logs once running.
            trace: Enable RPC message tracing on the Java side.
            command: Full command line to spawn instead of a JVM — a test
                    seam for exercising the transport against a fake peer.
        """
        self._marketplace_csv = marketplace_csv
        self._java_classpath = java_classpath
        self._java_home = java_home or os.environ.get("JAVA_HOME")
        self._log_file = log_file
        self._trace = trace
        self._command = command

        self._connection: ChildConnection | None = None
        self._stderr_file = None

    def start(self) -> "JavaRpcClient":
        """Start the Java RPC peer process."""
        from rewrite.rpc.server import handle_request

        cmd = self._command or self._java_command_line()

        logger.info(f"Starting Java RPC server: {' '.join(cmd)}")

        # Give the child's stderr a real sink: an unread PIPE fills up and
        # blocks the JVM mid-write, wedging the whole connection.
        stderr: Any = subprocess.DEVNULL
        if self._log_file:
            self._stderr_file = open(self._log_file, "ab")
            stderr = self._stderr_file

        process = subprocess.Popen(
            cmd,
            stdin=subprocess.PIPE,
            stdout=subprocess.PIPE,
            stderr=stderr,
        )
        self._connection = ChildConnection(process, upstream=handle_request)
        return self

    def _java_command_line(self) -> list[str]:
        cmd = [
            self._find_java_command(),
            "-cp", self._find_classpath(),
            "org.openrewrite.maven.rpc.JavaRewriteRpc",
        ]
        if self._marketplace_csv is not None:
            if not self._marketplace_csv.exists():
                raise RuntimeError(f"Marketplace CSV not found: {self._marketplace_csv}")
            cmd.append(f"--marketplace={self._marketplace_csv}")
        if self._trace:
            cmd.append("--trace")
        return cmd

    def shutdown(self) -> None:
        """Shutdown the Java RPC peer process."""
        if self._connection:
            self._connection.close()
            self._connection = None
        if self._stderr_file:
            self._stderr_file.close()
            self._stderr_file = None

    def __enter__(self) -> "JavaRpcClient":
        """Context manager entry."""
        return self.start()

    def __exit__(self, exc_type, exc_val, exc_tb) -> None:
        """Context manager exit."""
        self.shutdown()

    def send_request(self, method: str, params: dict[str, Any]) -> Any:
        """
        Send a JSON-RPC request to the Java peer and wait for its response,
        answering any requests the peer makes back at us in the meantime.

        Args:
            method: The RPC method name (e.g., "PrepareRecipe", "Visit")
            params: The request parameters

        Returns:
            The result from the RPC response

        Raises:
            RuntimeError: If the request fails or the peer exits
        """
        if self._connection is None:
            raise RuntimeError("Java RPC server is not running")
        return self._connection.request(method, params)

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

        classpath = find_test_classpath()
        if classpath:
            return classpath

        raise RuntimeError(
            "Could not find Java classpath. Set REWRITE_PYTHON_CLASSPATH environment variable "
            "or pass java_classpath parameter. You can generate the classpath using:\n"
            "  ./gradlew :rewrite-python:generateTestClasspath"
        )


def install_java_rpc_hooks() -> None:
    """
    Route ``rewrite.rpc.server.send_request`` to the active Java RPC client.

    Idempotent: re-installing over an already-hooked function is a no-op, so
    module-scoped fixtures can call it freely.
    """
    from rewrite.rpc import server

    if getattr(server.send_request, "_java_rpc_original", None) is not None:
        return

    original_send_request = server.send_request

    def hooked_send_request(method: str, params: dict, timeout_seconds: float = 30.0) -> Any:
        """Send request to the Java RPC client if one is active, else use the stdio path."""
        client = get_java_rpc_client()
        if client:
            return client.send_request(method, params)
        return original_send_request(method, params, timeout_seconds)

    hooked_send_request._java_rpc_original = original_send_request  # ty: ignore[unresolved-attribute]
    server.send_request = hooked_send_request  # ty: ignore[invalid-assignment]  # monkey-patching (ty#2193)


def uninstall_java_rpc_hooks() -> None:
    """Restore the original ``send_request`` installed over by
    :func:`install_java_rpc_hooks`."""
    from rewrite.rpc import server

    original = getattr(server.send_request, "_java_rpc_original", None)
    if original is not None:
        server.send_request = original
