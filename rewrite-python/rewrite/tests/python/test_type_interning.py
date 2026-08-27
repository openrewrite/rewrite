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
"""How many distinct JavaType objects a whole-project parse materializes."""
import dataclasses
import shutil
from enum import Enum

import pytest

from rewrite.java import JavaType
from rewrite.rpc import server

requires_ty_types_cli = pytest.mark.skipif(
    shutil.which('ty-types') is None,
    reason="ty-types CLI is not installed (ensure ty-types binary is on PATH)",
)

# Every concrete JavaType shape except Primitive, whose members are enum
# singletons and so are shared no matter how the caches are scoped.
_JAVA_TYPES = (
    JavaType.FullyQualified,
    JavaType.Method,
    JavaType.Variable,
    JavaType.GenericTypeVariable,
    JavaType.Union,
    JavaType.Intersection,
)

_LEAVES = (str, bytes, int, float, bool, complex, Enum, type)

# A first-party module every generated file imports, so the corpus has a type
# whose definition and references are spread across files.
_COMMON = '''\
from typing import List


class Shared:
    name: str
    values: List[int]

    def total(self) -> int:
        return sum(self.values)
'''

_SOURCE = '''\
import json
import os.path
from typing import Dict, List

from common import Shared


class Record{i}:
    name: str
    values: List[int]

    def to_json(self) -> str:
        return json.dumps({{"name": self.name, "values": self.values}})


def load{i}(path: str) -> Dict[str, object]:
    with open(os.path.join(path, "data.json")) as fh:
        return json.load(fh)


def totals{i}(shared: List[Shared]) -> List[int]:
    return [s.total() for s in shared]
'''


def _fields_of(obj):
    """The referenced values of an arbitrary object, whatever it uses to store them."""
    if dataclasses.is_dataclass(obj):
        return [getattr(obj, f.name, None) for f in dataclasses.fields(obj)]
    instance_dict = getattr(obj, '__dict__', None)
    if instance_dict is not None:
        return list(instance_dict.values())
    return [getattr(obj, slot, None)
            for klass in type(obj).__mro__
            for slot in getattr(klass, '__slots__', ()) or ()]


def _java_types(root):
    """Every JavaType object reachable from ``root``, keyed by identity."""
    found = {}
    seen = set()
    stack = [root]
    while stack:
        obj = stack.pop()
        if obj is None or isinstance(obj, _LEAVES):
            continue
        if id(obj) in seen:
            continue
        seen.add(id(obj))
        if isinstance(obj, _JAVA_TYPES):
            found[id(obj)] = obj
        if isinstance(obj, (list, tuple, set, frozenset)):
            stack.extend(obj)
        elif isinstance(obj, dict):
            stack.extend(obj.values())
        else:
            stack.extend(_fields_of(obj))
    return found


def _parse_project(tmp_path, module_count):
    """Parse a generated project, returning each source path's JavaType set."""
    (tmp_path / "common.py").write_text(_COMMON)
    for i in range(module_count):
        (tmp_path / f"mod{i}.py").write_text(_SOURCE.format(i=i))

    results = server.handle_parse_project(
        {'projectPath': str(tmp_path), 'relativeTo': str(tmp_path)})
    assert len(results) == module_count + 1
    return {result['sourcePath']: _java_types(server.local_objects[result['id']])
            for result in results}


@requires_ty_types_cli
def test_java_types_are_shared_across_the_files_of_one_parse_project(tmp_path):
    per_file = _parse_project(tmp_path, 10)

    largest_file = max(len(types) for types in per_file.values())
    assert largest_file > 100, "type attribution did not run; the count is meaningless"

    corpus = {}
    for types in per_file.values():
        corpus.update(types)

    # One object per type puts the corpus within reach of the richest single
    # file; one object per type per file puts it near the sum.
    assert len(corpus) < largest_file * 2


@requires_ty_types_cli
def test_same_named_typed_dicts_in_two_modules_keep_their_own_fields(tmp_path):
    (tmp_path / "a_mod.py").write_text(
        'from typing import TypedDict\n\n\n'
        'class Movie(TypedDict):\n    name: str\n\n\n'
        'def pick() -> Movie:\n    return Movie(name="x")\n')
    (tmp_path / "b_mod.py").write_text(
        'from typing import TypedDict\n\n\n'
        'class Movie(TypedDict):\n    year: int\n    director: str\n\n\n'
        'def pick() -> Movie:\n    return Movie(year=1, director="y")\n')

    results = server.handle_parse_project(
        {'projectPath': str(tmp_path), 'relativeTo': str(tmp_path)})
    fields = {}
    for result in results:
        for java_type in _java_types(server.local_objects[result['id']]).values():
            if getattr(java_type, 'fully_qualified_name', None) == 'Movie':
                fields[result['sourcePath']] = sorted(
                    m.name for m in (getattr(java_type, '_members', None) or []))

    assert fields == {'a_mod.py': ['name'], 'b_mod.py': ['director', 'year']}


@requires_ty_types_cli
def test_a_class_defined_in_one_file_and_used_in_another_is_one_object(tmp_path):
    per_file = _parse_project(tmp_path, 2)

    def shared_class(source_path):
        return {oid for oid, t in per_file[source_path].items()
                if getattr(t, 'fully_qualified_name', None) == 'common.Shared'}

    in_definition = shared_class('common.py')
    assert in_definition, "common.Shared was not attributed in its own module"
    assert in_definition == shared_class('mod0.py') == shared_class('mod1.py')
