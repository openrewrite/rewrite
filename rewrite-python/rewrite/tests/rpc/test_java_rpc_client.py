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

"""Transport-level tests for JavaRpcClient using a fake stdio peer (no JVM)."""
import sys
from pathlib import Path

from rewrite.rpc.java_rpc_client import JavaRpcClient

FAKE_PEER = Path(__file__).parent / "fake_peer.py"


def test_dispatches_peer_requests_while_response_pending():
    """A peer-initiated request arriving before our response (the Java
    ``Visit`` → ``GetObject`` callback pattern) must be dispatched to the
    server's request handlers and answered."""
    client = JavaRpcClient(command=[sys.executable, str(FAKE_PEER)])
    client.start()
    try:
        # The fake peer answers our request with the result of the
        # GetLanguages callback it made against us mid-request.
        result = client.send_request("RoundTrip", {})
        assert result == ["org.openrewrite.python.tree.Py$CompilationUnit"]
    finally:
        client.shutdown()
