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
// Runs the tests of an npm/hosted-git-info checkout (the tag npm bundles: v9.0.3 for npm 11.17.0) against
// recording stand-ins for node:test and node:assert, and writes the "valid urls parse properly" and
// "invalid urls return undefined" tables as rows: {file, url, expected | null}.
// Usage: node hosted-git-tables.js <hosted-git-info checkout> <out>
const Module = require('module')
const fs = require('fs')
const path = require('path')
process.env.NODE_PATH = require("./npm-modules.js").dir
Module._initPaths()
const dir = path.join(path.resolve(process.argv[2]), "test")

const rows = []
let file
let current
const assert = {
  strictEqual: (actual, expected, msg = '') => {
    if (current === 'valid urls parse properly') {
      const m = /^(.*) parses \((\w+)\)$/s.exec(msg)
      if (m) {
        let row = rows.find(r => r.file === file && r.url === m[1])
        if (!row) {
          row = { file, url: m[1], expected: {} }
          rows.push(row)
        }
        row.expected[m[2]] = expected === undefined ? null : expected
      }
    } else if (current === 'invalid urls return undefined') {
      const m = /^(.*) returns undefined$/s.exec(msg)
      if (m) {
        rows.push({ file, url: m[1], expected: null })
      }
    }
  },
  ok: () => {},
  deepStrictEqual: () => {},
  throws: () => {},
  equal: () => {},
}
const nodeTest = {
  test: (name, fn) => {
    current = name
    try {
      fn()
    } catch (e) {
      // only the two tables are dumped
    }
    current = null
  },
}
nodeTest.test.test = nodeTest.test

const origLoad = Module._load
Module._load = function (request) {
  if (request === 'node:test') {
    return nodeTest
  }
  if (request === 'node:assert') {
    return assert
  }
  return origLoad.apply(this, arguments)
}
for (file of ['github.js', 'gitlab.js', 'gist.js', 'bitbucket.js', 'sourcehut.js']) {
  require(path.join(dir, file))
}
fs.writeFileSync(process.argv[3], JSON.stringify(rows, null, 1) + '\n')
console.log(`${rows.length} rows (${rows.filter(r => r.expected).length} valid)`)
