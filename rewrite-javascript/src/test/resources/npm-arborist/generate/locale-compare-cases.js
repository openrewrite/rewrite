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
// Intl.Collator('en').compare, the comparator npm sorts lock keys and yarn.lock entries with, over ASCII:
// "order" is the 128 characters in collation order, grouped where they compare equal, and "pairs" is
// [a, b, sign] for lock-style keys and seeded random strings.
// Usage: node locale-compare-cases.js > ../locale-compare-cases
const compare = new Intl.Collator('en').compare
const sign = (a, b) => Math.sign(compare(a, b))

let seed = 20261005
const rnd = k => {
  seed = (seed + 0x6D2B79F5) | 0
  let t = Math.imul(seed ^ (seed >>> 15), 1 | seed)
  t = (t + Math.imul(t ^ (t >>> 7), 61 | t)) ^ t
  return ((t ^ (t >>> 14)) >>> 0) % k
}

const cases = []
const chars = []
for (let c = 0; c < 128; c++) {
  chars.push(String.fromCharCode(c))
}
const order = []
for (const c of chars.sort(compare)) {
  const last = order[order.length - 1]
  if (last && compare(last[0], c) === 0) {
    last.push(c)
  } else {
    order.push([c])
  }
}

const keys = ['node_modules/string-width-cjs', 'node_modules/string-width/node_modules/ansi-regex',
  'node_modules/string-width', 'node_modules/string_width', 'node_modules/string.width', 'node_modules/@scope/a',
  'node_modules/a', 'node_modules/a/node_modules/@scope/b', 'node_modules/a-b', 'node_modules/a.b', 'node_modules/A',
  'node_modules/a@b', 'x@1.x', 'x@>=1.0.1 <2', 'x@^1.0.0-x', 'X@1.x', 'x@1', '', ' ', 'packages/a', '../a']
for (const a of keys) {
  for (const b of keys) {
    cases.push([a, b, sign(a, b)])
  }
}

const alphabet = 'aAbBzZ019 _-,;:!?.\'"()[]{}@*/\\&#%`^+<=>|~$\t\n\u0001\u007f'
const word = () => {
  let s = ''
  for (let i = rnd(7); i > 0; i--) {
    s += alphabet[rnd(alphabet.length)]
  }
  return s
}
for (let i = 0; i < 1500; i++) {
  const a = word()
  const b = rnd(4) === 0 ? a.toUpperCase() : rnd(4) === 0 ? a + word() : word()
  cases.push([a, b, sign(a, b)])
}
console.log(JSON.stringify({ order, pairs: cases }))
