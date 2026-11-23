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
// Writes suite-5.5.0.tsv: every call node-semver 5.5.0's own tests make to a function yarn 1.22.22 calls, with 5.5.0's
// answer, each checked against the copy yarn's bundle holds.
//
// Usage: node suite-5.5.0.js <node-semver v5.5.0 checkout> <yarn 1.22.22>/lib/cli.js > suite-5.5.0.tsv
const path = require('path')
const fs = require('fs')
const Module = require('module')

const [checkout, cli] = process.argv.slice(2).map(p => path.resolve(p))
const real = require(path.join(checkout, 'semver.js'))
const bundled = (() => {
  let src = fs.readFileSync(cli, 'utf8').replace(/^#!.*\n/, '')
  src = src.replace(/return __webpack_require__\(__webpack_require__\.s = \d+\);/,
    'global.__yarnRequire = __webpack_require__; return {};')
  const m = new Module(cli, null)
  m.filename = cli
  m.paths = Module._nodeModulePaths(path.dirname(cli))
  m._compile(src, cli)
  const req = global.__yarnRequire
  const ids = req.m.map((f, i) => f && /exports\.SEMVER_SPEC_VERSION = '2\.0\.0'/.test(f.toString()) ? i : -1)
    .filter(i => i >= 0)
  if (ids.length !== 1) throw new Error('expected one semver module in the bundle, found ' + JSON.stringify(ids))
  return req(ids[0])
})()

const rows = []
const seen = new Set()
const proxy = Object.assign({}, real)
const plain = v => v === undefined ? {undefined: true} :
  (v && typeof v === 'object' && !Array.isArray(v)) ? {object: String(v)} : v
const recordable = args => args.every(a => a === undefined || a === null || typeof a !== 'object' || Array.isArray(a))
function check(name, args, result) {
  let other
  try {
    other = bundled[name](...args)
  } catch (e) {
    other = 'threw ' + e.message
  }
  if (JSON.stringify(plain(result)) !== JSON.stringify(plain(other))) {
    throw new Error(`yarn's bundled semver disagrees on ${name}(${JSON.stringify(args)})`)
  }
}
for (const name of Object.keys(real)) {
  const fn = real[name]
  if (typeof fn !== 'function' || /^[A-Z]/.test(name)) continue
  proxy[name] = function (...args) {
    let row
    try {
      const result = fn.apply(this, args)
      row = {fn: name, args: args.map(plain), result: plain(result)}
      if (recordable(args)) check(name, args, result)
      return result
    } catch (e) {
      row = {fn: name, args: args.map(plain), threw: e.message}
      throw e
    } finally {
      const key = JSON.stringify(row)
      if (recordable(args) && !seen.has(key)) {
        seen.add(key)
        rows.push(row)
      }
    }
  }
}

const tests = []
const origResolve = Module._resolveFilename
Module._resolveFilename = function (request, parent, ...rest) {
  if (request === 'tap') return 'tap-stub'
  if ((request === '../' || request === '../semver.js' || request === '../semver') &&
      parent && parent.filename.startsWith(path.join(checkout, 'test'))) {
    return 'semver-proxy'
  }
  return origResolve.call(this, request, parent, ...rest)
}
require.cache['tap-stub'] = {id: 'tap-stub', filename: 'tap-stub', loaded: true, exports: {test: (name, fn) => tests.push(fn)}}
require.cache['semver-proxy'] = {id: 'semver-proxy', filename: 'semver-proxy', loaded: true, exports: proxy}
const t = {
  ok() {}, notOk() {}, equal() {}, equivalent() {}, done() {}, equals() {}, strictEqual() {}, notEqual() {}, same() {},
  deepEqual() {}, end() {}, plan() {}, pass() {}, comment() {},
  throws(fn) { try { fn() } catch (e) {} }, doesNotThrow(fn) { fn() }, test(name, fn) { fn(t) }
}
for (const file of ['index.js', 'clean.js', 'gtr.js', 'ltr.js', 'major-minor-patch.js', 'prerelease.js',
  'big-numbers.js', 'coerce.js']) {
  tests.length = 0
  require(path.join(checkout, 'test', file))
  for (const fn of tests) fn(t)
}

const KEEP = new Set(['valid', 'validRange', 'satisfies', 'maxSatisfying', 'clean', 'gt', 'lt', 'eq', 'neq', 'gte',
  'lte', 'cmp'])
const lines = ['# node-semver 5.5.0 test/*.js, the calls of the functions yarn 1.22.22 makes: fn, args..., =>, result ' +
  '(JSON; {"undefined":true} is undefined, {"threw":msg} a throw)']
for (const row of rows.map(r => JSON.parse(JSON.stringify(r)))) {
  if (!KEEP.has(row.fn)) continue
  const result = 'threw' in row ? {threw: row.threw} : row.result
  lines.push([row.fn, ...row.args.map(a => JSON.stringify(a)), '=>', JSON.stringify(result)].join('\t'))
}
process.stdout.write(lines.join('\n') + '\n')
