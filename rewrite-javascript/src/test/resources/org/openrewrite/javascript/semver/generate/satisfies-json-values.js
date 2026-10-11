/*
 * Copyright 2026 the original author or authors.
 * <p>
 * Licensed under the Moderne Source Available License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://docs.moderne.io/licensing/moderne-source-available-license
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
// Writes satisfies-json-values.tsv: satisfies(version, range, true) in node-semver 5.5.0 and 7.8.4 for versions that
// are not strings, as JSON package data holds them, and for one longer than node-semver's MAX_LENGTH.
//
// Usage: node satisfies-json-values.js <node-semver v5.5.0 checkout> > satisfies-json-values.tsv
// NPM_NODE_MODULES names the node_modules npm bundles (7.8.4 for npm 11.17.0) when it is not beside the running node.
const path = require('path')

const s5 = require(path.join(path.resolve(process.argv[2]), 'semver.js'))
const s7 = require(path.join(process.env.NPM_NODE_MODULES ||
  path.resolve(path.dirname(process.execPath), '../lib/node_modules/npm/node_modules'), 'semver'))
const versions = [5, 1.5, 0.1, 1e21, true, [1], ['1', '2'], [], {}, {prerelease: []}, {prerelease: ['a']},
  {prerelease: 'x'}, {prerelease: ''}, {prerelease: 5}, {prerelease: null}, {prerelease: {length: 2}}, [null],
  [[1, 2]], [{}], '1.0.0-' + 'a'.repeat(260)]
const ranges = ['^1.0.0', '*', '', 'x', '* || ^1.0.0', '^1.0.0 || *', '>=0.0.0', 'not a range', '* || *']
const run = (semver, v, r) => {
  try {
    return JSON.stringify(semver.satisfies(v, r, true))
  } catch (e) {
    return JSON.stringify({threw: e.message})
  }
}
const lines = ['# satisfies(version, range, true) in node-semver 5.5.0 and 7.8.4 with a version as JSON package data ' +
  'holds it: version, range, 5.5.0, 7.8.4 (JSON; {"threw":msg} a throw)']
for (const v of versions) {
  for (const r of ranges) {
    lines.push([JSON.stringify(v), JSON.stringify(r), run(s5, v, r), run(s7, v, r)].join('\t'))
  }
}
process.stdout.write(lines.join('\n') + '\n')
