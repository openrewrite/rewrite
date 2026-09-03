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

"""Type attribution resolves against the typeshed of the version the project
declares, not that of whichever interpreter runs the parse.

These projects declare via classifiers, the signal ty cannot read for itself;
``requires-python`` it honors on its own, so a fixture using that would pass
whether or not the version reached ty.
"""

import shutil

import pytest

from rewrite.python.visitor import PythonVisitor
from rewrite.rpc import server


def _ty_types_cli_available() -> bool:
    try:
        from ty_types.__main__ import find_ty_types_bin  # noqa: F401
        return True
    except Exception:
        return shutil.which('ty-types') is not None


requires_ty_types_cli = pytest.mark.skipif(
    not _ty_types_cli_available(),
    reason="ty-types CLI is not installed (ensure ty-types binary is on PATH)",
)

SOURCE = (
    "from gettext import lgettext\n"
    "from locale import getdefaultlocale\n"
    "msg = lgettext('Hi')\n"
    "loc = getdefaultlocale()\n"
)


def _project(root, classifier_version):
    root.mkdir(parents=True, exist_ok=True)
    (root / "pyproject.toml").write_text(
        '[project]\nname = "demo"\nversion = "0.1.0"\n'
        'classifiers = ["Programming Language :: Python :: %s"]\n' % classifier_version
    )
    (root / "app.py").write_text(SOURCE)
    return root


def _declaring_types(root):
    """Map each call in the project to its method type's declaring-type FQN.

    ``JavaType.Unknown`` is truthy and carries no name, so an unresolved
    declaring type reports as None rather than being mistaken for a hit.
    """
    found = {}

    class Collect(PythonVisitor):
        def visit_method_invocation(self, mi, p):
            method_type = mi.method_type
            declaring = getattr(method_type, 'declaring_type', None) if method_type else None
            found[mi.name.simple_name] = (
                declaring.fully_qualified_name
                if declaring is not None and hasattr(declaring, '_fully_qualified_name')
                else None
            )
            return super().visit_method_invocation(mi, p)

    for item in server.handle_parse_project({'projectPath': str(root), 'relativeTo': str(root)}):
        obj = server.local_objects.get(item['id'] if isinstance(item, dict) else item)
        if obj is not None and obj.__class__.__name__ == 'CompilationUnit':
            Collect().visit(obj, None)
    return found


@requires_ty_types_cli
def test_declared_version_selects_typeshed(tmp_path):
    on_310 = _declaring_types(_project(tmp_path / "p310", "3.10"))
    on_313 = _declaring_types(_project(tmp_path / "p313", "3.13"))

    # gettext.lgettext is in 3.10's typeshed and gone from 3.11's.
    assert on_310["lgettext"] == "gettext"
    assert on_313["lgettext"] is None

    # In both, so the pair varies only by typeshed content, not by whether
    # attribution ran at all.
    assert on_310["getdefaultlocale"] == on_313["getdefaultlocale"] == "locale"


@requires_ty_types_cli
def test_version_outside_ty_support_keeps_the_rest_of_attribution(tmp_path):
    found = _declaring_types(_project(tmp_path / "p399", "3.99"))

    # ty declines a version it ships no stubs for by failing initialization,
    # which takes every type down with it unless the version is given up.
    assert found["getdefaultlocale"] == "locale"
