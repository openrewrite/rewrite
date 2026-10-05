#
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
#
"""Development-time oracle for the Pipfile.lock format layer.

pipenv's hash and lock emission are plain calls into Python's json module
(Project.calculate_pipfile_hash / _LockFileEncoder in pipenv/project.py), so
Python itself is the reference implementation. This script was run during
development to record the expected values embedded in the Java tests; tests
never execute it.

Modes (input on stdin, output on stdout, no trailing newline added except emit):
  hash [--canonical] [--sources-json JSON]  Pipfile TOML -> sha256 hex of the
                                            plette-shaped hash input
  compact                                   JSON -> json.dumps(sort_keys=True,
                                            separators=(",", ":"))
  emit                                      JSON -> json.dumps(indent=4,
                                            separators=(",", ": "),
                                            sort_keys=True) + "\\n"
"""
import hashlib
import json
import re
import sys
import tomllib

DEFAULT_SOURCE = {"name": "pypi", "url": "https://pypi.org/simple", "verify_ssl": True}
NON_CATEGORY = {"source", "packages", "dev-packages", "requires", "scripts", "pipfile", "pipenv"}


def canonicalize(name):
    # PEP 503 name normalization, as pipenv >= 2026.4 applies before hashing
    return re.sub(r"[-_.]+", "-", name).lower()


def hash_input(pipfile, fallback_sources, canonical):
    sources = pipfile.get("source") or fallback_sources or [DEFAULT_SOURCE]
    data = {
        "_meta": {"sources": sources, "requires": pipfile.get("requires", {})},
        "default": pipfile.get("packages", {}),
        "develop": pipfile.get("dev-packages", {}),
    }
    for category, values in pipfile.items():
        if category not in NON_CATEGORY:
            data[category] = values
    if canonical:
        for category in list(data):
            if category != "_meta":
                data[category] = {canonicalize(k): v for k, v in data[category].items()}
    return data


def main():
    mode = sys.argv[1]
    if mode == "hash":
        canonical = "--canonical" in sys.argv
        fallback = None
        if "--sources-json" in sys.argv:
            fallback = json.loads(sys.argv[sys.argv.index("--sources-json") + 1])
        data = hash_input(tomllib.loads(sys.stdin.read()), fallback, canonical)
        content = json.dumps(data, sort_keys=True, separators=(",", ":"))
        sys.stdout.write(hashlib.sha256(content.encode("utf-8")).hexdigest())
    elif mode == "compact":
        sys.stdout.write(json.dumps(json.loads(sys.stdin.read()), sort_keys=True, separators=(",", ":")))
    elif mode == "emit":
        sys.stdout.write(json.dumps(json.loads(sys.stdin.read()), indent=4, separators=(",", ": "), sort_keys=True) + "\n")
    else:
        raise SystemExit("unknown mode: " + mode)


if __name__ == "__main__":
    main()
