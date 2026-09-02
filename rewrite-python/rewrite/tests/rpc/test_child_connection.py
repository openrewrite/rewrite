import subprocess
import sys

from rewrite.rpc.child_connection import ChildConnection

# A minimal stub child: read one Content-Length framed JSON-RPC request, reply with a canned result
# echoing the request id, so the framing is exercised without a real peer.
def _stub_process(stub):
    return subprocess.Popen([sys.executable, str(stub)],
                            stdin=subprocess.PIPE, stdout=subprocess.PIPE)


STUB = r'''
import sys, json
buf = sys.stdin.buffer
cl = None
while True:
    line = buf.readline()
    if not line:
        sys.exit(0)
    s = line.decode("ascii").strip()
    if s == "":
        break
    if s.lower().startswith("content-length:"):
        cl = int(s.split(":", 1)[1])
body = b""
while len(body) < cl:
    body += buf.read(cl - len(body))
req = json.loads(body.decode("utf-8"))
resp = {"jsonrpc": "2.0", "id": req["id"], "result": [{"descriptor": {"name": "stub.recipe"}}]}
data = json.dumps(resp).encode("utf-8")
out = sys.stdout.buffer
out.write(("Content-Length: %d\r\n\r\n" % len(data)).encode("ascii"))
out.write(data)
out.flush()
'''



def test_child_connection_round_trips_a_request(tmp_path):
    stub = tmp_path / "stub.py"
    stub.write_text(STUB)
    conn = ChildConnection(_stub_process(stub))
    try:
        result = conn.request("GetMarketplace", {})
    finally:
        conn.close()
    assert result == [{"descriptor": {"name": "stub.recipe"}}]



STUB_WITH_CALLBACK = r'''
import sys, json
buf = sys.stdin.buffer
out = sys.stdout.buffer

def read():
    cl = None
    while True:
        line = buf.readline()
        if not line:
            return None
        s = line.decode("ascii").strip()
        if s == "":
            break
        if s.lower().startswith("content-length:"):
            cl = int(s.split(":", 1)[1])
    if cl is None:
        return None
    body = b""
    while len(body) < cl:
        body += buf.read(cl - len(body))
    return json.loads(body.decode("utf-8"))

def write(msg):
    data = json.dumps(msg).encode("utf-8")
    out.write(("Content-Length: %d\r\n\r\n" % len(data)).encode("ascii"))
    out.write(data)
    out.flush()

req = read()                                                        # routed request from facade
write({"jsonrpc": "2.0", "id": 99, "method": "GetObject", "params": {"id": "tree-1"}})
cb = read()                                                         # callback response from facade
write({"jsonrpc": "2.0", "id": req["id"], "result": {"got": cb["result"]}})
'''


def test_child_connection_relays_callbacks_to_upstream(tmp_path):
    stub = tmp_path / "stub.py"
    stub.write_text(STUB_WITH_CALLBACK)
    seen = []

    def upstream(method, params):
        seen.append((method, params))
        return {"data": "from-java"}

    conn = ChildConnection(_stub_process(stub), upstream=upstream)
    try:
        result = conn.request("Visit", {})
    finally:
        conn.close()

    assert seen == [("GetObject", {"id": "tree-1"})]        # child's callback relayed up
    assert result == {"got": {"data": "from-java"}}          # Java's response fed back to the child
