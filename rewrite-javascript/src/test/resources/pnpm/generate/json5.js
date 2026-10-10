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
// json5 2.2.3, pnpm's package.json5 reader: `unicode <file>` writes its util's code point ranges, `rows <dir> <file>`
// its answers to v2.2.3's test/parse.js and errors.js inputs. Usage: NODE_PATH=<json5@2.2.3> node json5.js <mode>
const fs = require('fs')
const Module = require('module')
const JSON5 = require('json5')
const util = require('json5/lib/util')

const [mode, ...args] = process.argv.slice(2)

if (mode === 'unicode') {
  const lines = []
  for (const [name, test] of [['Space_Separator', util.isSpaceSeparator], ['ID_Start', util.isIdStartChar],
    ['ID_Continue', util.isIdContinueChar]]) {
    const ranges = []
    let start = -1
    for (let cp = 0; cp <= 0x110000; cp++) {
      const inSet = cp <= 0x10FFFF && test(String.fromCodePoint(cp))
      if (inSet && start < 0) start = cp
      if (!inSet && start >= 0) {
        ranges.push(start.toString(16) + (cp - 1 === start ? '' : '-' + (cp - 1).toString(16)))
        start = -1
      }
    }
    lines.push(name + ' ' + ranges.join(' '))
  }
  fs.writeFileSync(args[0], lines.join('\n') + '\n')
} else if (mode === 'rows') {
  const [dir, out] = args
  const inputs = []
  const recording = {
    parse (text, reviver) {
      if (reviver === undefined && !inputs.includes(String(text))) inputs.push(String(text))
      return JSON5.parse(text, reviver)
    },
    stringify: JSON5.stringify,
  }
  const tap = {
    test (name, fn) {
      if (typeof fn === 'function') fn(tap)
    },
  }
  for (const m of ['strictSame', 'equal', 'ok', 'end', 'throws', 'same', 'deepEqual', 'notOk']) {
    tap[m] = (fn) => {
      if (m === 'throws' && typeof fn === 'function') {
        try { fn() } catch (e) { /* recorded */ }
      }
    }
  }
  const assert = new Proxy(function () {}, {
    get: () => (fn) => {
      if (typeof fn === 'function') {
        try { fn() } catch (e) { /* recorded */ }
      }
    },
  })
  const chain = new Proxy(function () {}, { get: () => chain, apply: () => chain })
  const sinon = chain
  const load = Module._load
  Module._load = function (request, parent, isMain) {
    if (request === '../lib') return recording
    if (request === 'tap') return tap
    if (request === 'assert') return assert
    if (request === 'sinon') return sinon
    return load.apply(this, arguments)
  }
  require(require('path').resolve(dir, 'parse.js'))
  require(require('path').resolve(dir, 'errors.js'))
  Module._load = load
  // what a manifest read as an object adds: key order, repeated and prototype keys, signed zero, lone surrogates
  for (const text of ['{b:1,"2":2,a:3,"1":4,"10":5,"01":6}', '{a:1,b:2,a:3}', '{__proto__:1,constructor:2}',
    '[-0,+0,-0x0]', '{a:-Infinity,b:+NaN}', "'\\uD800'", "'\\uDC00\\uD83D'", '{"\\u0000":0}',
    "'\\x41\\u{41}'", '0x', '1e', '.e1',
    '{a:1,}', '[1,]', '{,}', '[,]', '{a b:1}', '{"a":1 "b":2}', '{a\\u0020:1}', '/**/0/**/', '0//']) {
    if (!inputs.includes(text)) inputs.push(text)
  }
  const encode = (k, v) => typeof v === 'number' && (!Number.isFinite(v) || Object.is(v, -0)) ?
    { $number: Object.is(v, -0) ? '-0' : String(v) } : v
  const rows = inputs.map(text => {
    try {
      return JSON.stringify({ text, value: JSON5.parse(text) }, encode)
    } catch (e) {
      return JSON.stringify({ text, error: e.message, line: e.lineNumber, column: e.columnNumber })
    }
  })
  fs.writeFileSync(out, rows.join('\n') + '\n')
} else {
  console.error('usage: json5.js unicode <file> | rows <dir> <file>')
  process.exit(1)
}
