#!/bin/bash
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

# Regenerates the fixtures beside this directory from the npm that sits beside `node` (11.17.0 when they
# were last written; NPM_NODE_MODULES names another) and from checkouts of the upstream repositories at
# the tags that npm bundles: npm/cli, npm/npm-package-arg and npm/hosted-git-info.
# The fixtures are JSON with no file extension, so the license plugin does not prepend a header to them.
# root-collation.js, which writes the collation data in the main resources, runs apart: see its header.
# So does npm-releases.js, which writes the list of npm's releases there from the registry.
# Usage: bash regenerate.sh <npm/cli checkout> <npm-package-arg checkout> <hosted-git-info checkout>
set -e
G=$(cd "$(dirname "$0")" && pwd)
R=$(dirname "$G")
node "$G/tree-dumps.js" > "$R/tree-dumps"
node "$G/shrinkwrap-dumps.js" > "$R/shrinkwrap-dumps"
node "$G/yarn-lock-dumps.js" "$1/workspaces/arborist" > "$R/yarn-lock-dumps"
node "$G/placement-cases.js" "$1/workspaces/arborist" can-place-dep "$R/can-place-dep-cases.gz"
node "$G/placement-cases.js" "$1/workspaces/arborist" place-dep "$R/place-dep-cases.gz"
node "$G/spec-from-lock-cases.js" > "$R/spec-from-lock-cases"
node "$G/package-shapes.js" > "$R/package-shapes"
node "$G/version-shapes.js" > "$R/version-shapes"
node "$G/locale-compare-cases.js" > "$R/locale-compare-cases"
node "$G/minimatch-cases.js" > "$R/minimatch-cases"
node "$G/npa-tables.js" "$2" "$R/npa-tables" 2> /dev/null
node "$G/hosted-git-tables.js" "$3" "$R/hosted-git-tables"
node "$G/url-cases.js" "$R/url-cases"
node "$G/path-cases.js" "$R/path-cases"
