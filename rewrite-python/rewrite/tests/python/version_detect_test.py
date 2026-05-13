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

"""Unit tests for the auto-detection heuristics used by the Python RPC server
to pick between the Py2 (parso) and Py3 (ast) parser per file/project."""

from pathlib import Path

import pytest

from rewrite.python._version_detect import detect_from_source, detect_from_project


class TestSourceDetection:
    def test_returns_none_for_empty(self):
        assert detect_from_source("") is None

    def test_returns_none_when_no_signal(self):
        assert detect_from_source("x = 1\nprint(x)\n") is None

    @pytest.mark.parametrize(
        "shebang,expected",
        [
            ("#!/usr/bin/env python2", "2"),
            ("#!/usr/bin/env python2.7", "2.7"),
            ("#!/usr/bin/python2", "2"),
            ("#!/usr/bin/python2.7", "2.7"),
            ("#!python2", "2"),
            ("#!python3", "3"),
            ("#!/usr/bin/env python3.11", "3.11"),
        ],
    )
    def test_shebang(self, shebang, expected):
        assert detect_from_source(f"{shebang}\nx = 1\n") == expected

    def test_unversioned_shebang_yields_none(self):
        # `python` with no version digit is ambiguous — must not be a Py2 signal.
        assert detect_from_source("#!/usr/bin/env python\nx = 1\n") is None

    def test_shebang_only_on_line_one(self):
        # A `#!` on line 3 is not a real shebang and must not be honored.
        assert detect_from_source("# foo\n# bar\n#!/usr/bin/env python2\n") is None

    @pytest.mark.parametrize(
        "comment,expected",
        [
            ("# -*- python: 2 -*-", "2"),
            ("# -*- python: 2.7 -*-", "2.7"),
            ("# -*- python: 3 -*-", "3"),
            ("#  python:2.7", "2.7"),
        ],
    )
    def test_magic_comment(self, comment, expected):
        assert detect_from_source(f"{comment}\nx = 1\n") == expected

    def test_magic_comment_line_two(self):
        # PEP-263 allows the magic comment on line 2 (after a shebang).
        src = "#!/usr/bin/env python\n# -*- python: 2.7 -*-\nx = 1\n"
        assert detect_from_source(src) == "2.7"

    def test_magic_comment_not_after_line_two(self):
        src = "# foo\n# bar\n# -*- python: 2 -*-\n"
        assert detect_from_source(src) is None

    def test_magic_comment_beats_shebang(self):
        # If both are present, magic comment is the authoritative signal.
        src = "#!/usr/bin/env python3\n# -*- python: 2.7 -*-\nx = 1\n"
        assert detect_from_source(src) == "2.7"

    def test_bom_is_stripped(self):
        src = "﻿#!/usr/bin/env python2\nx = 1\n"
        assert detect_from_source(src) == "2"


class TestProjectDetection:
    def test_returns_none_for_none_path(self):
        assert detect_from_project(None) is None

    def test_returns_none_for_missing_dir(self, tmp_path):
        assert detect_from_project(tmp_path / "does-not-exist") is None

    def test_returns_none_when_no_metadata(self, tmp_path):
        assert detect_from_project(tmp_path) is None

    def test_pyproject_classifier_py2(self, tmp_path):
        (tmp_path / "pyproject.toml").write_text(
            '[project]\nclassifiers = ["Programming Language :: Python :: 2.7"]\n'
        )
        assert detect_from_project(tmp_path) == "2.7"

    def test_pyproject_classifier_py3(self, tmp_path):
        (tmp_path / "pyproject.toml").write_text(
            '[project]\nclassifiers = ["Programming Language :: Python :: 3.10"]\n'
        )
        assert detect_from_project(tmp_path) == "3.10"

    def test_pyproject_mixed_major_versions_yields_none(self, tmp_path):
        # A project that declares support for both 2.7 and 3.x cannot be
        # routed at the project level; per-file detection must decide.
        (tmp_path / "pyproject.toml").write_text(
            '[project]\nclassifiers = ['
            '"Programming Language :: Python :: 2.7", '
            '"Programming Language :: Python :: 3.10"]\n'
        )
        assert detect_from_project(tmp_path) is None

    def test_requires_python_pinned_to_py2(self, tmp_path):
        (tmp_path / "pyproject.toml").write_text(
            '[project]\nrequires-python = ">=2.7,<3"\n'
        )
        assert detect_from_project(tmp_path) == "2.7"

    def test_requires_python_pinned_to_py3(self, tmp_path):
        (tmp_path / "pyproject.toml").write_text(
            '[project]\nrequires-python = ">=3.10"\n'
        )
        assert detect_from_project(tmp_path) == "3"

    def test_setup_cfg_classifier(self, tmp_path):
        (tmp_path / "setup.cfg").write_text(
            "[metadata]\nclassifiers =\n"
            "    Programming Language :: Python :: 2.7\n"
        )
        assert detect_from_project(tmp_path) == "2.7"

    def test_accepts_string_path(self, tmp_path):
        (tmp_path / "pyproject.toml").write_text(
            '[project]\nclassifiers = ["Programming Language :: Python :: 2.7"]\n'
        )
        assert detect_from_project(str(tmp_path)) == "2.7"
