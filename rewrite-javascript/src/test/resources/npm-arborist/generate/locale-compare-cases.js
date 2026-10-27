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

// beyond ASCII: "unicodeOrder" is a sample of all code points in collation order, grouped where they
// compare equal, and "unicodePairs" is [a, b, sign] for strings in normalization form C or D
const str = cp => String.fromCodePoint(cp)
const sample = []
for (let cp = 0x80; cp <= 0x10ffff; cp++) {
  if (cp < 0x530 || cp % 257 === 0) {
    sample.push(cp)
  }
}
const unicodeOrder = []
for (const cp of sample.sort((a, b) => compare(str(a), str(b)) || a - b)) {
  const last = unicodeOrder[unicodeOrder.length - 1]
  if (last && compare(str(last[0]), str(cp)) === 0) {
    last.push(cp)
  } else {
    unicodeOrder.push([cp])
  }
}

const range = (a, b) => Array.from({ length: b - a + 1 }, (_, i) => a + i)
const pools = [
  [...'abcdefghijklmnopqrstuvwxyzABCXYZ0189-_./@ '].map(c => c.codePointAt(0)),
  range(0xa0, 0x24f),
  range(0x300, 0x36f),
  [0x300, 0x301, 0x302, 0x306, 0x308, 0x30a, 0x323, 0x327, 0x31b, 0x653, 0x654, 0x655, 0x93c, 0x94d, 0x3099, 0x309a,
    0xf71, 0xf72, 0xf74, 0xf80, 0x9be, 0x9d7, 0xdca, 0xdcf, 0xddf, 0x1b35, 0x102e],
  range(0x370, 0x4ff),
  [...range(0x5d0, 0x5ea), ...range(0x5b0, 0x5c7), ...range(0x620, 0x65f)],
  [...range(0x900, 0x97f), ...range(0x980, 0x9ff), ...range(0xcc0, 0xcd6), ...range(0xdc0, 0xddf)],
  [...range(0xe00, 0xe4e), ...range(0xe80, 0xecd), ...range(0xf40, 0xfbc), ...range(0x1000, 0x103f),
    ...range(0x1b00, 0x1b44)],
  [...range(0x1100, 0x1112), ...range(0x1161, 0x1175), ...range(0x11a8, 0x11c2), 0xac00, 0xac01, 0xd55c, 0xd7a3],
  [...range(0x3041, 0x30ff), 0x4e00, 0x4e8c, 0x5de5, 0x5177, 0x6587, 0x4ef6, 0x9fff, 0x3400, 0x20000, 0x323b0, 0xfa0e,
    0xf900, 0x2f00, 0x3007, 0x3220, 0x17000, 0x18800, 0x1b170, 0x18b00],
  [0xfffe, 0xffff, 0xfffd, 0xd800, 0xdc00, 0x378, 0xe000, 0xfdd0, 0x10ffff, 0, 0xad, 0x200b, 0x200d, 0xfeff, 0xfe0f,
    0x1f600, 0x1f3fb, 0x6c, 0x4c, 0xb7, 0x387, 0x16122, 0x1611e, 0x16129, 0x1d15e, 0x1d157, 0x1d165, 0x10378, 0x1e947],
]
const text = () => {
  const main = pools[rnd(pools.length)]
  let s = ''
  for (let i = 1 + rnd(6); i > 0; i--) {
    const pool = rnd(10) < 6 ? main : pools[rnd(pools.length)]
    s += str(pool[rnd(pool.length)])
  }
  return s.normalize(rnd(2) ? 'NFC' : 'NFD')
}
const unicodePairs = []
const names = ['packages/café', 'packages/cafe', 'packages/cafeteria', 'packages/Café', 'packages/cafe\u0301',
  'packages/工具', 'packages/文件', 'packages/一', 'packages/日本語', 'packages/にほんご', 'packages/ニホンゴ', 'packages/한국어',
  'packages/мой-пакет', 'packages/Мой-пакет', 'packages/мои-пакет', 'packages/и\u0306од', 'packages/straße', 'packages/strasse',
  'packages/ångström', 'packages/zebra', 'packages/🙂', 'packages/ไทย', 'packages/เก', 'packages/กเ', 'packages/ﬁle', 'packages/file',
  '../мой', 'packages/l·l', 'packages/ll', 'packages/\u0000a', 'packages/a', 'node_modules/@scope/a']
for (const a of names) {
  for (const b of names) {
    unicodePairs.push([a, b, sign(a, b)])
  }
}
for (let i = 0; i < 1200; i++) {
  const a = text()
  const b = rnd(4) === 0 ? a.normalize('NFD') + str(pools[3][rnd(pools[3].length)]) : rnd(4) === 0 ? a + text() : text()
  unicodePairs.push([a, b.normalize(rnd(2) ? 'NFC' : 'NFD'), 0])
}
for (const pair of unicodePairs) {
  pair[2] = sign(pair[0], pair[1])
  if (pair[2] !== sign(pair[0].normalize('NFD'), pair[1].normalize('NFD'))) {
    throw new Error(`the collator tells ${JSON.stringify(pair)} from its normalized form`)
  }
}
console.log(JSON.stringify({ order, pairs: cases, unicodeOrder, unicodePairs }))
