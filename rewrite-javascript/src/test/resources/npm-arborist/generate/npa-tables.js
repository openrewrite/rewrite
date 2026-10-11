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
// Runs the table-driven tests of an npm/npm-package-arg checkout (the tag npm bundles: v13.0.2 for npm 11.17.0)
// against a recording stand-in for `tap` and writes each table row as {arg, where, expected}.
// os.homedir() in an expectation becomes "{HOME}"; a RegExp becomes {"regex": source}.
// Usage: node npa-tables.js <npm-package-arg checkout> <out>
const Module = require('module')
const fs = require('fs')
const os = require('os')
const path = require('path')
process.env.NODE_PATH = require("./npm-modules.js").dir
Module._initPaths()
const dir = path.join(path.resolve(process.argv[2]), "test")

const clean = v => {
  if (v === undefined) {
    return null
  }
  if (v instanceof RegExp) {
    return { regex: v.source }
  }
  if (typeof v === 'string') {
    return v.split(os.homedir()).join('{HOME}')
  }
  if (v && typeof v === 'object') {
    const o = {}
    for (const k of Object.keys(v)) {
      o[k] = clean(v[k])
    }
    return o
  }
  return v
}

const rows = []
let file
const makeT = () => {
  const t = {
    test: (name, ...rest) => {
      const fn = rest.find(r => typeof r === 'function')
      try {
        fn && fn(makeT())
      } catch (e) {
        // tests that throw outside a table row are ported by hand
      }
    },
    end: () => {},
    plan: () => {},
    teardown: () => {},
    setMaxListeners: () => {},
    comment: () => {},
    mock: (p, mocks) => require(path.resolve(dir, p)),
    ok: () => {},
    notOk: () => {},
    equal: () => {},
    not: () => {},
    same: () => {},
    throws: () => {},
    match: (actual, expected, msg) => {
      const m = /^(.*) \[(\w+)\]$/s.exec(msg || '')
      if (m) {
        let row = rows.find(r => r.file === file && r.arg === m[1])
        if (!row) {
          row = { file, arg: m[1], where: file === 'basic.js' ? '/test/a/b' : null, expected: {} }
          rows.push(row)
        }
        row.expected[m[2]] = clean(expected)
      }
    },
    has: (actual, expected, msg) => {
      const m = /^(.*) matches expectations$/s.exec(msg || '')
      if (m) {
        rows.push({ file, arg: m[1], where: null, expected: clean(expected) })
      }
    },
  }
  return t
}

const origLoad = Module._load
Module._load = function (request) {
  if (request === 'tap') {
    return makeT()
  }
  return origLoad.apply(this, arguments)
}
for (file of ['basic.js', 'github.js', 'gitlab.js', 'bitbucket.js']) {
  require(path.join(dir, file))
}
fs.writeFileSync(process.argv[3], JSON.stringify(rows, null, 1) + '\n')
const keys = new Set(rows.flatMap(r => Object.keys(r.expected)))
console.log(`${rows.length} rows; keys: ${[...keys].join(', ')}`)
