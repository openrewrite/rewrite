"""JSON-RPC over a child process's stdin/stdout.

Uses the same Content-Length framing as the Java<->Python link. stdio is point-to-point, so the
owner of the process speaks to it directly here; the child's *outbound* callbacks are relayed by
handling inbound requests inside ``request``'s read loop. :mod:`rewrite.rpc.java_rpc_client` drives
a Java peer over this.
"""
import json
import subprocess
import traceback


class ChildConnection:
    def __init__(self, proc: subprocess.Popen, upstream=None):
        self._proc = proc
        self._upstream = upstream
        self._id = 0

    def request(self, method: str, params: dict):
        self._id += 1
        rid = self._id
        self._write({"jsonrpc": "2.0", "id": rid, "method": method, "params": params})
        while True:
            msg = self._read()
            if msg is None:
                raise RuntimeError(f"child closed the connection during {method}")

            if msg.get("id") == rid and ("result" in msg or "error" in msg):
                if "error" in msg:
                    err = msg["error"]
                    tb = err.get("data")  # the child sends its full traceback here
                    detail = f"\nchild traceback:\n{tb}" if tb else ""
                    raise RuntimeError(f"child error for {method}: {err.get('message')}{detail}")
                return msg.get("result")

            callback_method = msg.get("method")
            if callback_method is not None:
                child_id = msg.get("id")
                if self._upstream is None:
                    raise RuntimeError(
                        f"child issued a '{callback_method}' callback but no upstream is configured")
                try:
                    result = self._upstream(callback_method, msg.get("params", {}))
                    response = {"jsonrpc": "2.0", "id": child_id, "result": result}
                except Exception as e:
                    # Carry the traceback in `data` — the same field the peer's own
                    # error responses use — so the failing side is diagnosable from
                    # the other end of the wire.
                    response = {"jsonrpc": "2.0", "id": child_id,
                                "error": {"code": -32603, "message": str(e),
                                          "data": traceback.format_exc()}}
                # Notifications (no id, e.g. Evict) get no reply — a null-id response
                # would fail every in-flight request on the peer's reader.
                if child_id is not None:
                    self._write(response)

    def close(self) -> None:
        try:
            if self._proc.stdin:
                self._proc.stdin.close()
        except Exception:
            pass
        try:
            self._proc.terminate()
            self._proc.wait(timeout=5)
        except Exception:
            self._proc.kill()

    def _write(self, msg: dict) -> None:
        data = json.dumps(msg).encode("utf-8")
        header = ("Content-Length: %d\r\n\r\n" % len(data)).encode("ascii")
        self._proc.stdin.write(header + data)
        self._proc.stdin.flush()

    def _read(self):
        content_length = None
        while True:
            line = self._proc.stdout.readline()
            if not line:
                return None
            s = line.decode("ascii").strip()
            if s == "":
                break
            if s.lower().startswith("content-length:"):
                content_length = int(s.split(":", 1)[1])
        if content_length is None:
            return None
        body = b""
        while len(body) < content_length:
            chunk = self._proc.stdout.read(content_length - len(body))
            if not chunk:
                return None
            body += chunk
        return json.loads(body.decode("utf-8"))
