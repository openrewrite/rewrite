# Copyright 2026 the original author or authors.
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

"""Fail when a committed ``.pyi`` stub differs from what ``scripts/generate_stubs.py`` renders
for its ``.py``. Type checkers read the stub in preference to the source, so drift ships wrong
types while the code runs fine."""

import importlib.util
from pathlib import Path

import pytest

PROJECT_ROOT = Path(__file__).parents[1]
_spec = importlib.util.spec_from_file_location("generate_stubs", PROJECT_ROOT / "scripts" / "generate_stubs.py")
generate_stubs = importlib.util.module_from_spec(_spec)
_spec.loader.exec_module(generate_stubs)

SOURCES = sorted(generate_stubs.find_lst_files(PROJECT_ROOT / "src" / "rewrite"))


@pytest.mark.parametrize("source", SOURCES, ids=lambda p: str(p.relative_to(PROJECT_ROOT / "src")))
def test_committed_stub_matches_generator(source: Path):
    expected = generate_stubs.generate_stub_content(source)
    if "class " not in expected:
        pytest.skip("the generator writes no stub for this file")
    stub = source.with_suffix(".pyi")
    assert stub.exists() and stub.read_text() == expected, (
        f"{stub.relative_to(PROJECT_ROOT)} is stale; regenerate with: python scripts/generate_stubs.py"
    )


def test_enum_stub_keeps_public_properties_and_methods(tmp_path: Path):
    source = tmp_path / "tree.py"
    source.write_text('''\
from abc import ABC
from dataclasses import dataclass
from enum import Enum


class Holder(ABC):
    class Kind(Enum):
        A = 0

        @property
        def label(self) -> str:
            return self.name

        def _missing_(cls, value):
            return None


@dataclass(frozen=True)
class Node:
    _style: Style

    class Style(Enum):
        SINGLE = 0

        def quote(self, doubled: bool) -> str:
            return "'"
''')
    stub = generate_stubs.generate_stub_content(source)

    assert "        @property\n        def label(self) -> str: ..." in stub
    assert "_missing_" not in stub

    assert "        def quote(self, doubled: bool) -> str: ..." in stub


def test_static_method_stub_keeps_its_decorator_and_defaults(tmp_path: Path):
    source = tmp_path / "tree.py"
    source.write_text('''\
from abc import ABC
from dataclasses import dataclass
from typing import Optional


class Markup(ABC):
    @staticmethod
    def warn(message: str, detail: Optional[str] = None) -> str:
        return message

    @staticmethod
    def _hidden() -> None:
        pass


@dataclass(frozen=True)
class SearchResult:
    _description: Optional[str]

    @staticmethod
    def found(tree: object, *, description: Optional[str] = None) -> object:
        return tree
''')
    stub = generate_stubs.generate_stub_content(source)

    assert "    @staticmethod\n    def warn(message: str, detail: Optional[str]=...) -> str: ..." in stub
    assert "_hidden" not in stub

    assert "    @staticmethod\n    def found(tree: object, *, description: Optional[str]=...) -> object: ..." in stub


def test_method_stub_keeps_defaults_and_unannotated_methods(tmp_path: Path):
    source = tmp_path / "tree.py"
    source.write_text('''\
from abc import ABC, abstractmethod
from dataclasses import dataclass
from typing import Optional


class Parser(ABC):
    def reset(self):
        pass

    @abstractmethod
    def parse(self, *sources: str, strict: bool = False) -> list:
        ...

    @classmethod
    def build(cls, erroneous: Optional[str] = None) -> 'Parser':
        ...


@dataclass(frozen=True)
class PrintOutputCapture:
    _out: str

    def append(self, text: Optional[str] = None) -> 'PrintOutputCapture':
        return self


class ParseErrorVisitor(TreeVisitor[Tree, P]):
    def is_acceptable(self, source_file, p: P) -> bool:
        return True
''')
    stub = generate_stubs.generate_stub_content(source)

    assert "    def reset(self) -> Any: ..." in stub

    assert "    def parse(self, *sources: str, strict: bool=...) -> list: ..." in stub

    assert "    @classmethod\n    def build(cls, erroneous: Optional[str]=...) -> 'Parser': ..." in stub

    assert "    def append(self, text: Optional[str]=...) -> 'PrintOutputCapture': ..." in stub

    assert "class ParseErrorVisitor(TreeVisitor[Tree, P]):\n    def is_acceptable(self, source_file: Any, p: P) -> bool: ..." in stub
