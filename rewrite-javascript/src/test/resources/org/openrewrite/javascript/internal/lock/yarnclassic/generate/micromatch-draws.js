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
// What yarn 1.22.22's micromatch answers over Math.random draws for patterns whose regex keeps preserve's id.
// Usage: node micromatch-draws.js <yarn>/lib/cli.js [runs]
const fs = require('fs')
const path = require('path')
const Module = require('module')

const [cli, runs = '4000'] = process.argv.slice(2)
let src = fs.readFileSync(cli, 'utf8').replace(/^#!.*\n/, '')
src = src.replace(/return __webpack_require__\(__webpack_require__\.s = \d+\);/,
  'global.__yarnRequire = __webpack_require__; return {};')
const m = new Module(cli, null)
m.filename = cli
m.paths = Module._nodeModulePaths(path.dirname(cli))
m._compile(src, cli)
const req = global.__yarnRequire
let micromatch
let regexCache
for (let i = 0; i < req.m.length; i++) {
  if (!req.m[i]) continue
  const text = req.m[i].toString()
  try {
    if (!micromatch && /micromatch/.test(text)) {
      const x = req(i)
      if (typeof x === 'function' && typeof x.makeRe === 'function' && typeof x.isMatch === 'function') micromatch = x
    }
    if (!regexCache && /function regexCache\(fn, str, opts\)/.test(text)) regexCache = req(i)
  } catch (e) {
  }
}
// regex-cache keeps one regex per pattern for the life of the process: one yarn run sees one draw
const fresh = (list, pattern) => {
  for (const k of Object.keys(regexCache.basic)) delete regexCache.basic[k]
  for (const k of Object.keys(regexCache.cache)) delete regexCache.cache[k]
  return micromatch(list, pattern)
}

// V8's Math.random() is a multiple of 2^-52 in [0, 1); its id is shorter than five digits when it prints with four
// decimals or fewer, and none prints in exponent form with fewer than five mantissa decimals
const short = []
for (let j = 0; j < 10000; j++) {
  const x = j / 10000
  if (Number.isInteger(x * 2 ** 52) && !/^[0-9]{5}$/.test(x.toString().slice(2, 7))) short.push(x)
}
for (let n = 7; n <= 17; n++) {
  for (let m = 0; m <= 3; m++) {
    for (let d = 10 ** m; d < 10 ** (m + 1); d++) {
      const str = String(d)[0] + (m ? '.' + String(d).slice(1) : '') + 'e-' + n
      const x = Number(str)
      if (Number.isInteger(x * 2 ** 52) && x.toString() === str) throw new Error('an exponent-form id: ' + str)
    }
  }
}
console.log('#short\t' + short.length)

const patterns = ['1${a}{b,c}', '[${a}]{b,c}', 'x[1${a}2]{b,c}', '[!${a}]{b,c}', '${a}${b}{c,d}']
const paths = ['1b', 'b', 'xb', 'Ib', '5b', '0b', '12345b', '1__ID12345__b', 'x1b', 'x2b', 'x5b', 'x0b',
  '9876543210b', '__ID__b', '1__ID5__b', '/b', 'Db']
const random = Math.random
for (const pattern of patterns) {
  for (const p of paths) {
    const answers = new Set()
    for (let i = 0; i < Number(runs); i++) answers.add(fresh([p], pattern).length > 0)
    const spelled = [...p.matchAll(/__ID([0-9]{5})/g)]
      .map(m => Math.floor(Number('0.' + m[1] + '1') * 2 ** 52) / 2 ** 52)
    for (const x of [...short, ...spelled]) {
      Math.random = () => x
      answers.add(fresh([p], pattern).length > 0)
      Math.random = random
    }
    console.log(pattern + '\t' + p + '\t' + (answers.size > 1 ? 'random' : [...answers][0]))
  }
}
