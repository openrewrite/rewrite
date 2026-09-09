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
"""Whether refs sent for one source file are reusable by the next."""
import shutil

import pytest

from rewrite.rpc import server

requires_ty_types_cli = pytest.mark.skipif(
    shutil.which('ty-types') is None,
    reason="ty-types CLI is not installed (ensure ty-types binary is on PATH)",
)

_SOURCE = '''\
import json
from typing import Dict


def load{i}(text: str) -> Dict[str, object]:
    return json.loads(text)


def dump{i}(value: Dict[str, object]) -> str:
    return json.dumps(value)
'''

_CU_TYPE = 'org.openrewrite.python.tree.Py$CompilationUnit'


def defined(response):
    """Ref ids this response assigns, i.e. sends the full object for."""
    return {d['ref'] for d in response if 'ref' in d and 'valueType' in d}


def used(response):
    """Ref ids this response cites without resending the object."""
    return {d['ref'] for d in response if 'ref' in d and 'valueType' not in d}


@pytest.fixture(autouse=True)
def clean_connection_state():
    server.handle_reset({})
    yield
    server.handle_reset({})


def parse_two_files(tmp_path):
    for i in range(2):
        (tmp_path / f"mod{i}.py").write_text(_SOURCE.format(i=i))
    results = server.handle_parse_project(
        {'projectPath': str(tmp_path), 'relativeTo': str(tmp_path)})
    assert len(results) == 2
    return sorted(r['id'] for r in results)


def get_object(obj_id):
    return server.handle_get_object({'id': obj_id, 'sourceFileType': _CU_TYPE})


@requires_ty_types_cli
def test_the_second_file_cites_refs_the_first_file_sent(tmp_path):
    first_id, second_id = parse_two_files(tmp_path)

    first = get_object(first_id)
    second = get_object(second_id)

    assert defined(first), "the first file sent no refs; the test proves nothing"
    # Ref ids are one ascending sequence across the connection, so the second file's
    # ids continue past the first's rather than restarting and renaming them.
    assert not (defined(first) & defined(second)), "a ref id was assigned twice"

    inherited = used(second) - defined(second)
    assert inherited, "the second file resent everything"
    assert inherited <= defined(first)
    assert len(defined(second)) < len(defined(first)) / 2


@requires_ty_types_cli
def test_evicting_a_file_releases_the_refs_it_introduced(tmp_path):
    first_id, second_id = parse_two_files(tmp_path)

    # The sequence Java drives per file: checkpoint on first visit, pull the tree, evict.
    server._local_ref_checkpoints.setdefault(first_id, server.local_refs.snapshot())
    get_object(first_id)
    server.handle_evict({'id': first_id})

    second = get_object(second_id)
    assert used(second) <= defined(second), "cited a ref Java rolled back"
    assert len(server.local_refs) == len(defined(second))


@requires_ty_types_cli
def test_a_failed_transfer_releases_the_refs_it_assigned(tmp_path, monkeypatch):
    from rewrite.rpc import send_queue

    first_id, second_id = parse_two_files(tmp_path)
    get_object(first_id)
    before_failure = server.local_refs.snapshot()

    def fail_midway(self, after, before=None):
        self.refs.create(object())
        raise RuntimeError("transfer failed")

    monkeypatch.setattr(send_queue.RpcSendQueue, 'generate', fail_midway)

    assert get_object(second_id) == [{'state': 'END_OF_OBJECT'}]
    assert server.local_refs.snapshot() == before_failure


@requires_ty_types_cli
def test_reset_clears_the_sent_refs(tmp_path):
    first_id, _ = parse_two_files(tmp_path)
    get_object(first_id)
    assert len(server.local_refs) > 0

    server.handle_reset({})
    assert len(server.local_refs) == 0
