# Copyright 2026 the original author or authors.
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

"""Heuristics for auto-detecting the Python language version of a source file
or project.

The parser's effective version resolves in this order:

1. Explicit ``language_level`` argument (from the RPC ``options`` payload).
2. In-source signals via :func:`detect_from_source` (magic comment, then shebang).
3. Project-level signals via :func:`detect_from_project` (pyproject.toml,
   setup.cfg).
4. Process-wide default (``REWRITE_PYTHON_VERSION``).

Both detectors return ``None`` when no signal is present, allowing the caller
to fall through to the next layer.
"""

import re
from pathlib import Path
from typing import Optional, Union

# `# -*- python: 2 -*-` / `# -*- python: 2.7 -*-` — mirrors PEP-263's encoding
# declaration style. Must appear on line 1 or 2 of the source.
_MAGIC_COMMENT_RE = re.compile(r"#.*?\bpython\s*:\s*(?P<ver>\d+(?:\.\d+)?)")

# Shebangs: #!/usr/bin/env python2.7, #!/usr/bin/python2, #!python3, etc.
# Anchored to the start of line 1 (a shebang anywhere else is not honored by
# the OS and is not a reliable signal).
_SHEBANG_RE = re.compile(r"^#!.*\bpython(?P<ver>\d+(?:\.\d+)?)\b")

# pyproject.toml [project] classifier: "Programming Language :: Python :: 2.7"
_CLASSIFIER_RE = re.compile(
    r"Programming Language\s*::\s*Python\s*::\s*(?P<ver>\d+(?:\.\d+)?)"
)


def detect_from_source(source: str) -> Optional[str]:
    """Return the Python version declared in the source file, or ``None``.

    Recognized signals (highest priority first):

    * ``# -*- python: 2 -*-`` on line 1 or 2 (PEP-263-style magic comment)
    * ``#!/usr/bin/env python2.7`` shebang on line 1

    The magic comment is checked first because it is an explicit author
    intent, whereas a shebang may carry stale tooling info on legacy files.
    """
    if not source:
        return None

    if source.startswith("﻿"):
        source = source[1:]

    # Inspect only the first two lines — these are the only places PEP-263
    # and shebang declarations are recognized by Python itself.
    lines = source.split("\n", 2)[:2]

    for line in lines:
        m = _MAGIC_COMMENT_RE.search(line)
        if m:
            return m.group("ver")

    if lines:
        m = _SHEBANG_RE.match(lines[0])
        if m:
            return m.group("ver")

    return None


def detect_from_project(project_path: Union[str, Path, None]) -> Optional[str]:
    """Return the Python version declared at the project level, or ``None``.

    Reads, in order:

    * ``pyproject.toml`` ``[project].classifiers`` for
      ``Programming Language :: Python :: <ver>``.
    * ``pyproject.toml`` ``[project].requires-python`` when it pins a major
      version (e.g. ``"<3"`` or ``">=2.7,<3"`` resolves to ``"2.7"``).
    * ``setup.cfg`` ``[metadata].classifiers`` (same shape as pyproject).

    The most specific version found wins; if multiple major versions are
    declared (e.g. both ``2.7`` and ``3.10``), returns ``None`` because the
    project supports both and per-file detection should decide.
    """
    if project_path is None:
        return None

    root = Path(project_path)
    if not root.exists():
        return None

    versions = set()

    pyproject = root / "pyproject.toml"
    if pyproject.is_file():
        try:
            text = pyproject.read_text(encoding="utf-8")
        except OSError:
            text = ""
        versions.update(_classifier_versions(text))
        requires = _requires_python(text)
        if requires:
            versions.add(requires)

    setup_cfg = root / "setup.cfg"
    if setup_cfg.is_file():
        try:
            text = setup_cfg.read_text(encoding="utf-8")
        except OSError:
            text = ""
        versions.update(_classifier_versions(text))

    return _resolve(versions)


def _classifier_versions(text: str):
    for m in _CLASSIFIER_RE.finditer(text):
        ver = m.group("ver")
        # Skip the bare "Programming Language :: Python :: 3" form when
        # accompanied by more specific entries — handled by _resolve.
        yield ver


def _requires_python(pyproject_text: str) -> Optional[str]:
    """Best-effort parse of ``requires-python`` from a pyproject.toml.

    Avoids a TOML dependency; only looks for the ``requires-python = "..."``
    line under any section. Returns the most restrictive major version
    implied by the constraint, or None if it spans both 2 and 3.
    """
    m = re.search(
        r'(?m)^\s*requires-python\s*=\s*"([^"]+)"', pyproject_text
    )
    if not m:
        return None
    spec = m.group(1)
    allows_two = bool(re.search(r"(^|,)\s*[<>=!~]*\s*2(\.|\b)", spec))
    allows_three = bool(re.search(r"(^|,)\s*[<>=!~]*\s*3(\.|\b)", spec))
    upper_excludes_three = bool(re.search(r"<\s*3(\.|\b)", spec))
    if allows_two and upper_excludes_three:
        return "2.7"
    if allows_three and not allows_two:
        return "3"
    return None


def _resolve(versions) -> Optional[str]:
    """Reduce a set of version strings to a single effective version.

    * Multiple major versions → ``None`` (project supports both; defer).
    * Any Py2 entry → most specific Py2 version found.
    * Otherwise → most specific Py3 version found.
    """
    if not versions:
        return None

    majors = {v.split(".", 1)[0] for v in versions}
    if "2" in majors and "3" in majors:
        return None

    py2 = sorted(v for v in versions if v.startswith("2"))
    if py2:
        return py2[-1]

    py3 = sorted(v for v in versions if v.startswith("3"))
    if py3:
        return py3[-1]

    return None
