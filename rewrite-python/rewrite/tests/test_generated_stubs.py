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
import subprocess
import sys
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


def test_stubs_type_check():
    stubs = sorted(str(p.relative_to(PROJECT_ROOT)) for p in (PROJECT_ROOT / "src").rglob("*.pyi"))
    result = subprocess.run(
        [sys.executable, "-m", "ty", "check", "--python", sys.prefix, "--output-format", "concise",
         # The project ignores these two for its own sources. A stub must pass them.
         "--error", "unresolved-reference", "--error", "invalid-argument-type", *stubs],
        cwd=PROJECT_ROOT, capture_output=True, text=True,
    )
    assert result.returncode == 0, result.stdout + result.stderr


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

    assert "    class Kind(Enum):\n        A = ...\n" in stub
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


def test_stub_imports_follow_runtime_bindings(tmp_path: Path):
    source = tmp_path / "tree.py"
    source.write_text('''\
from abc import ABC
from typing import TYPE_CHECKING

from .markers import Markers

if TYPE_CHECKING:
    from .visitor import Visitor
else:
    from .fallback import Fallback

if not TYPE_CHECKING:
    from .runtime import Runtime


class Tree(ABC):
    def accept(self, v: Visitor) -> Markers:
        from .printer import Printer
        return Printer().print(self)
''')
    stub = generate_stubs.generate_stub_content(source)

    assert "from .markers import Markers as Markers\n" in stub
    assert "from .fallback import Fallback as Fallback\n" in stub
    assert "from .runtime import Runtime as Runtime\n" in stub

    assert "from .visitor import Visitor\n" in stub

    assert "Printer" not in stub


def test_root_tree_stub_declares_replace(tmp_path: Path):
    source = tmp_path / "tree.py"
    source.write_text('''\
from abc import ABC
from typing import Self


class Tree(ABC):
    def replace(self, **kwargs) -> Self:
        return self
''')
    stub = generate_stubs.generate_stub_content(source)

    assert "class Tree(ABC):\n    def replace(self, **kwargs: Any) -> Self: ..." in stub


def test_dataclass_stub_mirrors_its_declaration(tmp_path: Path):
    source = tmp_path / "tree.py"
    source.write_text('''\
from dataclasses import dataclass
from enum import Enum


@dataclass
class Config:
    x: int


class Color(Enum):
    RED = 0


@dataclass(frozen=True)
class Node:
    _id: int
''')
    stub = generate_stubs.generate_stub_content(source)

    assert "@dataclass\nclass Config:\n    x: int\n" in stub

    assert "class Color(Enum):\n    RED = ...\n" in stub

    assert "@dataclass(frozen=True)\nclass Node:" in stub

    assert "def replace" not in stub


def test_member_stubs_keep_their_decorators(tmp_path: Path):
    source = tmp_path / "tree.py"
    source.write_text('''\
from abc import ABC, abstractmethod
from functools import cached_property


class Base(ABC):
    @property
    @abstractmethod
    def name(self) -> str: ...

    @name.setter
    def name(self, value: str) -> None: ...

    @cached_property
    def size(self) -> int:
        return 0

    @abstractmethod
    def visit(self, p: int) -> int: ...

    @classmethod
    def _from_wire(cls, d): ...
''')
    stub = generate_stubs.generate_stub_content(source)

    assert "    @property\n    @abstractmethod\n    def name(self) -> str: ...\n" in stub
    assert "    @name.setter\n    def name(self, value: str) -> None: ...\n" in stub

    assert "    @property\n    def size(self) -> int: ...\n" in stub

    assert "    @abstractmethod\n    def visit(self, p: int) -> int: ...\n" in stub

    assert "_from_wire" not in stub


def test_every_id_field_accepts_int_and_uuid():
    declared = {line.strip() for source in SOURCES
                for line in generate_stubs.generate_stub_content(source).splitlines()
                if line.strip().startswith("_id:")}
    assert declared == {"_id: int | UUID"}


def test_node_constructors_accept_generated_and_existing_ids(tmp_path: Path):
    recipe = tmp_path / "recipe.py"
    recipe.write_text('''\
from rewrite import Markers, random_id
from rewrite.java import Space
from rewrite.java.tree import Identifier

fresh = Identifier(random_id(), Space.EMPTY, Markers.EMPTY, [], "x", None, None)
Identifier(fresh.id, Space.EMPTY, Markers.build(random_id(), []), [], "y", None, None)
''')
    result = subprocess.run(
        [sys.executable, "-m", "ty", "check", "--python", sys.prefix, "--output-format", "concise",
         "--error", "invalid-argument-type", str(recipe)],
        cwd=PROJECT_ROOT, capture_output=True, text=True,
    )
    assert result.returncode == 0, result.stdout + result.stderr
