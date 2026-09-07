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
from typing import Any, Optional, Union

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


def requested_python_version(value: Any) -> Optional[str]:
    """The value if it looks like a Python minor version ("3.12"), else None."""
    return value if isinstance(value, str) and re.fullmatch(r"\d+\.\d+", value) else None


def ty_python_version(*levels: Optional[str]) -> Optional[str]:
    """The first language level precise enough to name a stdlib surface to ty.

    One ty session serves a whole batch, so callers pass only levels holding
    for all of it — never a per-file shebang or magic comment.
    """
    for level in levels:
        version = requested_python_version(level)
        if version:
            return version
    return None


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
      version (e.g. ``">=2.7,<3"`` resolves to ``"2.7"``).
    * ``setup.cfg`` ``[metadata].classifiers`` (same shape as pyproject).

    A Python 3 span resolves to its floor; if multiple major versions are
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


# A clause that sets a floor. ``<`` and ``<=`` cap the range and ``!=`` punches
# a hole in it; neither says what the lowest admitted version is.
_LOWER_BOUND = re.compile(r"(?:^|,)\s*(?:>=?|~=|==)?\s*(?P<ver>\d+(?:\.\d+)?)")

# An upper bound excluding every 3.x, which leaves only Python 2. ``<=3`` is
# not one: it admits 3.0.
_EXCLUDES_PY3 = re.compile(r"<\s*3(\.0)?\s*(,|$)")


def _requires_python(pyproject_text: str) -> Optional[str]:
    """Best-effort parse of ``requires-python`` from a pyproject.toml.

    Avoids a TOML dependency; only looks for the ``requires-python = "..."``
    line under any section. Returns the lowest version the constraint admits,
    keeping its minor (``">=3.10"`` -> ``"3.10"``, matching how ty reads the
    same field), or None where it names no floor or spans both 2 and 3.
    """
    m = re.search(
        r'(?m)^\s*requires-python\s*=\s*"([^"]+)"', pyproject_text
    )
    if not m:
        return None
    spec = m.group(1)
    floors = _LOWER_BOUND.findall(spec)
    py2 = [v for v in floors if v.split(".", 1)[0] == "2"]
    py3 = [v for v in floors if v.split(".", 1)[0] == "3"]
    if not py3 and _EXCLUDES_PY3.search(spec):
        return "2.7"
    if py3 and not py2:
        return _lowest(py3)
    return None


def _resolve(versions) -> Optional[str]:
    """Reduce a set of version strings to a single effective version.

    Multiple major versions yield ``None`` — the project supports both, so
    per-file detection has to decide. A Python 3 span resolves to its floor,
    the one version every line of the project has to work on.
    """
    if not versions:
        return None

    majors = {v.split(".", 1)[0] for v in versions}
    if "2" in majors and "3" in majors:
        return None
    if "2" in majors:
        # parso implements a single Python 2 grammar, so 2.7 is the only value
        # the Py2 parser can act on.
        return "2.7"

    return _lowest(versions)


def _lowest(versions) -> str:
    """The numerically lowest version, preferring those carrying a minor: a
    bare ``"3"`` names no stdlib surface and would otherwise mask ``"3.10"``."""
    with_minor = [v for v in versions if "." in v]
    return min(with_minor or list(versions),
               key=lambda v: tuple(int(part) for part in v.split(".")))
