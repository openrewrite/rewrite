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
pytest configuration and fixtures for OpenRewrite Python tests.

This module provides fixtures for testing Python recipes, including
support for recipes that delegate to Java via RPC.
"""

import os

import pytest

# Try to import Java RPC client (may not be available in minimal installs)
try:
    from rewrite.rpc.java_rpc_client import (
        JavaRpcClient,
        find_test_classpath,
        install_java_rpc_hooks,
        set_java_rpc_client,
        uninstall_java_rpc_hooks,
    )
    JAVA_RPC_AVAILABLE = True
except ImportError:
    JAVA_RPC_AVAILABLE = False


@pytest.fixture(scope="module")
def java_rpc_client(tmp_path_factory):
    """
    Module-scoped fixture owning the Java RPC peer process.

    Starts a JVM running JavaRewriteRpc and routes Python RPC calls to it for
    the duration of the test module. Tests should normally depend on the
    per-test ``java_rpc`` fixture instead, which adds a state reset between
    tests.

    Skips when the Java classpath is not configured.
    """
    if not JAVA_RPC_AVAILABLE:
        pytest.skip("Java RPC client module not available")

    if not find_test_classpath():
        pytest.skip(
            "Java RPC classpath not available. "
            "Run './gradlew :rewrite-python:generateTestClasspath' first, "
            "or set REWRITE_PYTHON_CLASSPATH environment variable."
        )

    # Create log directory
    log_dir = tmp_path_factory.mktemp("java_rpc_logs")
    log_file = log_dir / "java-rpc-server.log"

    client = JavaRpcClient(
        log_file=log_file,
        trace=os.environ.get("REWRITE_RPC_TRACE", "").lower() in ("1", "true", "yes"),
    )

    try:
        client.start()

        # Install hooks so Python RPC calls go to Java
        set_java_rpc_client(client)
        install_java_rpc_hooks()

        yield client

    finally:
        # Cleanup
        set_java_rpc_client(None)
        uninstall_java_rpc_hooks()
        client.shutdown()

        # Print log file on failure (helpful for debugging)
        if log_file.exists():
            log_content = log_file.read_text()
            if log_content:
                print(f"\n=== Java RPC Server Log ===\n{log_content}")


@pytest.fixture
def java_rpc(java_rpc_client):
    """
    Per-test fixture ensuring Java RPC is available with a clean slate.

    Resets accumulated state on both the Java peer and the Python server
    module before the test, so tests stay independent even though the JVM
    persists for the whole module.

    Usage:
        def test_something(java_rpc):
            # Java RPC is available
            pass
    """
    from rewrite.rpc import server

    java_rpc_client.send_request("Reset", {})
    server.handle_request("Reset", {})
    return java_rpc_client


# Marker for tests that require Java RPC
def pytest_configure(config):
    """Register custom markers."""
    config.addinivalue_line(
        "markers",
        "requires_java_rpc: mark test as requiring Java RPC server"
    )


def pytest_collection_modifyitems(config, items):
    """Skip tests marked with requires_java_rpc if Java RPC is not available."""
    if not JAVA_RPC_AVAILABLE:
        skip_java_rpc = pytest.mark.skip(reason="Java RPC not available")
        for item in items:
            if "requires_java_rpc" in item.keywords:
                item.add_marker(skip_java_rpc)
    elif not find_test_classpath():
        skip_classpath = pytest.mark.skip(
            reason="Java RPC classpath not configured (run ./gradlew :rewrite-python:generateTestClasspath)"
        )
        for item in items:
            if "requires_java_rpc" in item.keywords:
                item.add_marker(skip_classpath)
