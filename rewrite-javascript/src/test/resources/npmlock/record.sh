#!/usr/bin/env bash
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

# Regenerates the npmlock engine fixtures from the live npm registry.
#
# Each scenario directory receives:
#   package.json.before      manifest before the recipe's edit
#   package.json             manifest after the edit
#   package-lock.before.json lock produced by real npm for the before manifest
#   package-lock.after.json  lock produced by real npm after the edit (the golden)
#   http/<name>.json         full packuments for every package the edit moves,
#                            recorded at the same time as the goldens so the
#                            offline engine replay sees the registry state npm saw
#
# Byte-identity of the engine's output against package-lock.after.json is only
# meaningful when before-lock, golden, and packuments were recorded together
# with the same npm version. Record the npm/node versions in README.md when
# regenerating.
set -euo pipefail

cd "$(dirname "$0")"
REGISTRY="https://registry.npmjs.org"
NPM_FLAGS=(--package-lock-only --ignore-scripts --no-audit --no-fund)
WORK=$(mktemp -d)
trap 'rm -rf "$WORK"' EXIT

record() {
  local dir=$1 before=$2 after=$3
  shift 3
  local movers=("$@")
  echo "=== $dir"
  mkdir -p "$dir/http"
  printf '%s\n' "$before" >"$dir/package.json.before"
  printf '%s\n' "$after" >"$dir/package.json"

  rm -rf "$WORK/p" && mkdir "$WORK/p"
  cp "$dir/package.json.before" "$WORK/p/package.json"
  (cd "$WORK/p" && npm install "${NPM_FLAGS[@]}" >/dev/null 2>&1)
  cp "$WORK/p/package-lock.json" "$dir/package-lock.before.json"

  cp "$dir/package.json" "$WORK/p/package.json"
  (cd "$WORK/p" && npm install "${NPM_FLAGS[@]}" >/dev/null 2>&1)
  cp "$WORK/p/package-lock.json" "$dir/package-lock.after.json"

  for name in "${movers[@]+"${movers[@]}"}"; do
    local encoded=${name//\//%2f}
    curl -sf -H "Accept: application/json" "$REGISTRY/$encoded" -o "$dir/http/$encoded.json"
  done
}

record upgrade-leaf \
  '{
  "name": "fixture",
  "version": "1.0.0",
  "dependencies": {
    "is-number": "^4.0.0"
  }
}' \
  '{
  "name": "fixture",
  "version": "1.0.0",
  "dependencies": {
    "is-number": "^6.0.0"
  }
}' \
  is-number

record range-satisfied \
  '{
  "name": "fixture",
  "version": "1.0.0",
  "dependencies": {
    "is-number": "^6.0.0"
  }
}' \
  '{
  "name": "fixture",
  "version": "1.0.0",
  "dependencies": {
    "is-number": ">=4.0.0"
  }
}'

record cascade-fails \
  '{
  "name": "fixture",
  "version": "1.0.0",
  "dependencies": {
    "is-odd": "^2.0.0"
  }
}' \
  '{
  "name": "fixture",
  "version": "1.0.0",
  "dependencies": {
    "is-odd": "^3.0.1"
  }
}' \
  is-odd

record remove-orphans \
  '{
  "name": "fixture",
  "version": "1.0.0",
  "dependencies": {
    "is-odd": "^3.0.1"
  },
  "devDependencies": {
    "is-even": "^1.0.0"
  }
}' \
  '{
  "name": "fixture",
  "version": "1.0.0",
  "dependencies": {
    "is-odd": "^3.0.1"
  }
}'

record upgrade-orphans \
  '{
  "name": "fixture",
  "version": "1.0.0",
  "dependencies": {
    "chalk": "^4.1.2"
  }
}' \
  '{
  "name": "fixture",
  "version": "1.0.0",
  "dependencies": {
    "chalk": "^5.0.0"
  }
}' \
  chalk

record add-leaf \
  '{
  "name": "fixture",
  "version": "1.0.0",
  "dependencies": {
    "is-odd": "^3.0.1"
  }
}' \
  '{
  "name": "fixture",
  "version": "1.0.0",
  "dependencies": {
    "is-buffer": "^2.0.5",
    "is-odd": "^3.0.1"
  }
}' \
  is-buffer

record override \
  '{
  "name": "fixture",
  "version": "1.0.0",
  "dependencies": {
    "is-even": "^1.0.0"
  }
}' \
  '{
  "name": "fixture",
  "version": "1.0.0",
  "dependencies": {
    "is-even": "^1.0.0"
  },
  "overrides": {
    "is-buffer": "1.1.5"
  }
}' \
  is-buffer

record dev-recolor \
  '{
  "name": "fixture",
  "version": "1.0.0",
  "dependencies": {
    "kind-of": "^3.2.2"
  },
  "devDependencies": {
    "is-number": "^3.0.0"
  }
}' \
  '{
  "name": "fixture",
  "version": "1.0.0",
  "devDependencies": {
    "is-number": "^3.0.0"
  }
}'

record scoped \
  '{
  "name": "fixture",
  "version": "1.0.0",
  "dependencies": {
    "@isaacs/string-locale-compare": "1.0.1"
  }
}' \
  '{
  "name": "fixture",
  "version": "1.0.0",
  "dependencies": {
    "@isaacs/string-locale-compare": "^1.1.0"
  }
}' \
  @isaacs/string-locale-compare

echo "done"
