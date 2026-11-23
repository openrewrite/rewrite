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

"""Stand-in for a spawned RPC peer (normally a JVM) in JavaRpcClient tests.

Speaks Content-Length-framed JSON-RPC on stdio. On receiving a request it
issues a ``GetLanguages`` request of its own back to the host — mimicking how
the Java peer calls back ``GetObject`` while a ``Visit`` response is pending —
and only then answers the original request with whatever the host returned.
"""
import json
import sys


def read_message():
    content_length = None
    while True:
        line = sys.stdin.buffer.readline()
        if not line:
            return None
        s = line.decode("ascii").strip()
        if s == "":
            break
        if s.lower().startswith("content-length:"):
            content_length = int(s.split(":", 1)[1])
    if content_length is None:
        return None
    return json.loads(sys.stdin.buffer.read(content_length).decode("utf-8"))


def write_message(msg):
    data = json.dumps(msg).encode("utf-8")
    sys.stdout.buffer.write(b"Content-Length: %d\r\n\r\n" % len(data))
    sys.stdout.buffer.write(data)
    sys.stdout.buffer.flush()


def main():
    request = read_message()
    if request is None:
        return

    write_message({"jsonrpc": "2.0", "id": 1000, "method": "GetLanguages", "params": {}})
    callback_response = read_message()
    if callback_response is None:
        return

    write_message({
        "jsonrpc": "2.0",
        "id": request["id"],
        "result": callback_response.get("result"),
    })


if __name__ == "__main__":
    main()
