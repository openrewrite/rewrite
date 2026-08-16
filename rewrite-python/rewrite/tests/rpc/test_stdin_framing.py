"""Framing and liveness of the stdin JSON-RPC loop, held to the semantics the JS peer
gets from ``vscode-jsonrpc``. The protocol has no shutdown message, so exiting 0 is
itself an assertion: the host closed the stream.
"""
import json
import os
import subprocess
import sys
import threading

import pytest

import rewrite.rpc.server as server


def _framed(msg: dict) -> bytes:
    body = json.dumps(msg).encode('utf-8')
    return b"Content-Length: %d\r\n\r\n" % len(body) + body


def _header_and_body(msg: dict):
    """The two halves a slow peer can deliver in separate writes."""
    framed = _framed(msg)
    split = framed.index(b"\r\n\r\n") + 4
    return framed[:split], framed[split:]


def _parse_frames(data: bytes) -> list:
    messages = []
    while data:
        separator = data.index(b"\r\n\r\n")
        length = int(data[:separator].split(b":")[1])
        start = separator + 4
        messages.append(json.loads(data[start:start + length]))
        data = data[start + length:]
    return messages


def _write_later(pipe, chunks, delay=0.15):
    """Deliver the rest of a message after the reader's deadline has passed."""
    def run():
        for chunk in chunks:
            try:
                pipe.write(chunk)
            except (OSError, ValueError):
                return  # the test finished early and its pipe is gone

    timer = threading.Timer(delay, run)
    timer.daemon = True
    timer.start()


@pytest.fixture
def stdin_pipe(monkeypatch):
    """Bind the module-level stdin buffer to a pipe this test writes into."""
    read_fd, write_fd = os.pipe()
    buffer = server._StdinBuffer()
    buffer._fd = read_fd
    monkeypatch.setattr(server, '_stdin_buffer', buffer)
    writer = os.fdopen(write_fd, 'wb', buffering=0)
    try:
        yield writer
    finally:
        if not writer.closed:
            writer.close()
        os.close(read_fd)


class _Stdout:
    """A real descriptor for ``write_message``, so responses are asserted through the
    production encoder and framing rather than around them."""

    def __init__(self, path):
        self._path = path
        self._handle = open(path, 'wb', buffering=0)

    def install(self, monkeypatch):
        # Applied from the test body: pytest reinstalls sys.stdout when the call phase
        # begins, so a patch made during fixture setup is discarded before main() runs.
        monkeypatch.setattr(sys, 'stdout', self._handle)

    def frames(self):
        return _parse_frames(self._path.read_bytes())

    def close(self):
        self._handle.close()


@pytest.fixture
def stdout(tmp_path):
    out = _Stdout(tmp_path / 'stdout.bin')
    try:
        yield out
    finally:
        out.close()


@pytest.fixture(autouse=True)
def isolated_server(monkeypatch):
    """The server keeps its request ids and in-flight bookkeeping in module state, and
    main() reads argv, so both are per-test rather than per-session."""
    monkeypatch.setattr(server, '_request_id_counter', 0)
    monkeypatch.setattr(server, '_awaiting_ids', set())
    monkeypatch.setattr(server, '_pending_responses', {})
    monkeypatch.setattr(sys, 'argv', ['server'])


@pytest.fixture
def sent(monkeypatch):
    """Messages the server sent, for tests that care about content, not encoding."""
    messages = []
    monkeypatch.setattr(server, 'write_message', messages.append)
    return messages


LATE = {'jsonrpc': '2.0', 'id': 1, 'result': 'late'}
NEXT_REQUEST = {'jsonrpc': '2.0', 'id': 2, 'method': 'GetLanguages', 'params': {}}


def test_read_with_timeout_waits_for_a_body_that_arrives_after_the_deadline(stdin_pipe):
    header, body = _header_and_body(LATE)
    stdin_pipe.write(header)
    _write_later(stdin_pipe, [body])

    assert server.read_message_with_timeout(0.05) == LATE


def test_stream_stays_aligned_after_a_body_arrives_late(stdin_pipe):
    header, body = _header_and_body(LATE)
    stdin_pipe.write(header)
    _write_later(stdin_pipe, [body, _framed(NEXT_REQUEST)])

    server.read_message_with_timeout(0.05)

    assert server.read_message() == NEXT_REQUEST


def test_main_survives_a_handler_result_that_is_not_json_serializable(stdin_pipe, stdout,
                                                                     monkeypatch):
    stdout.install(monkeypatch)
    monkeypatch.setattr(server, 'handle_request', lambda method, params: {'path': {'a', 'b'}})

    stdin_pipe.write(_framed({'jsonrpc': '2.0', 'id': 1, 'method': 'GetObject', 'params': {}}))
    stdin_pipe.write(_framed(NEXT_REQUEST))
    stdin_pipe.close()

    server.main()

    written = stdout.frames()
    assert [r['id'] for r in written] == [1, 2]
    assert 'error' in written[0]


def test_main_exits_zero_when_the_host_closes_the_stream(stdin_pipe, stdout, monkeypatch):
    stdout.install(monkeypatch)
    stdin_pipe.write(_framed(NEXT_REQUEST))
    stdin_pipe.close()

    server.main()

    assert [r['id'] for r in stdout.frames()] == [2]


def test_main_exits_nonzero_on_a_corrupt_frame(stdin_pipe, stdout, monkeypatch):
    stdout.install(monkeypatch)
    stdin_pipe.write(b"Content-Length: not-a-number\r\n\r\n")
    stdin_pipe.close()

    with pytest.raises(SystemExit) as exit_info:
        server.main()

    assert exit_info.value.code == 8


def test_send_request_returns_the_response_matching_its_id(stdin_pipe, sent):
    stdin_pipe.write(_framed({'jsonrpc': '2.0', 'id': 999, 'result': 'stale'}))
    stdin_pipe.write(_framed({'jsonrpc': '2.0', 'id': 1, 'result': 'mine'}))

    assert server.send_request('GetObject', {'id': 'x'}, timeout_seconds=1.0) == 'mine'


def test_send_request_serves_a_request_that_arrives_while_it_waits(stdin_pipe, sent):
    stdin_pipe.write(_framed({'jsonrpc': '2.0', 'id': 'reentrant', 'method': 'GetLanguages',
                              'params': {}}))
    stdin_pipe.write(_framed({'jsonrpc': '2.0', 'id': 1, 'result': 'mine'}))

    assert server.send_request('GetObject', {'id': 'x'}, timeout_seconds=1.0) == 'mine'
    assert 'reentrant' in [m.get('id') for m in sent]


def _nested_call(method, params):
    return server.send_request('Inner', {}, timeout_seconds=1.0)


def test_a_nested_call_does_not_discard_the_outer_calls_response(stdin_pipe, sent, monkeypatch):
    monkeypatch.setattr(server, 'handle_request', _nested_call)
    stdin_pipe.write(_framed({'jsonrpc': '2.0', 'id': 'reentrant', 'method': 'Visit', 'params': {}}))
    stdin_pipe.write(_framed({'jsonrpc': '2.0', 'id': 1, 'result': 'outer'}))
    stdin_pipe.write(_framed({'jsonrpc': '2.0', 'id': 2, 'result': 'inner'}))

    assert server.send_request('Outer', {}, timeout_seconds=1.0) == 'outer'


def test_a_protocol_error_inside_a_handler_still_exits_nonzero(stdin_pipe, stdout, monkeypatch):
    stdout.install(monkeypatch)
    monkeypatch.setattr(server, 'handle_request', _nested_call)
    stdin_pipe.write(_framed({'jsonrpc': '2.0', 'id': 1, 'method': 'Visit', 'params': {}}))
    # A frame the reader consumes whole, so main() reaches EOF rather than tripping
    # over leftovers and reporting the fault for the wrong reason.
    stdin_pipe.write(b"garbage\r\n")
    stdin_pipe.close()

    with pytest.raises(SystemExit) as exit_info:
        server.main()

    assert exit_info.value.code == 8


def test_main_exits_nonzero_on_a_truncated_message(stdin_pipe, stdout, monkeypatch):
    stdout.install(monkeypatch)
    stdin_pipe.write(_header_and_body(NEXT_REQUEST)[0])
    stdin_pipe.close()

    with pytest.raises(SystemExit) as exit_info:
        server.main()

    assert exit_info.value.code == 8


def test_send_request_reports_a_closed_stream_rather_than_a_timeout(stdin_pipe, sent):
    stdin_pipe.close()

    with pytest.raises(RuntimeError, match='closed'):
        server.send_request('GetObject', {'id': 'x'}, timeout_seconds=1.0)


def test_a_response_no_call_is_awaiting_is_not_retained(stdin_pipe, sent):
    stdin_pipe.write(_framed({'jsonrpc': '2.0', 'id': 999, 'result': 'stale'}))
    stdin_pipe.write(_framed({'jsonrpc': '2.0', 'id': 1, 'result': 'mine'}))

    assert server.send_request('GetObject', {'id': 'x'}, timeout_seconds=1.0) == 'mine'
    assert not server._pending_responses


def _spawn_server(stdin_bytes, tmp_path):
    """Exercise the exit status the Java host actually observes."""
    proc = subprocess.run(
        [sys.executable, '-m', 'rewrite.rpc.server', '--log-file', str(tmp_path / 'rpc.log')],
        input=stdin_bytes, stdout=subprocess.PIPE, timeout=60)
    return proc.returncode


def test_the_process_exits_zero_when_the_host_closes_the_stream(tmp_path):
    assert _spawn_server(_framed(NEXT_REQUEST), tmp_path) == 0


def test_the_process_exits_eight_on_a_corrupt_frame(tmp_path):
    assert _spawn_server(b"garbage\r\n", tmp_path) == 8
