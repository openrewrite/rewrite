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
// Writes the collation data LocaleCompare reads, root-collation.gz beside it in the main resources: the
// CLDR root collation, which is what Intl.Collator('en') sorts by. The weights come from CLDR's
// allkeys_CLDR.txt and the decompositions and combining classes from UnicodeData.txt, both at the versions
// the running node reports (process.versions.cldr and .unicode; 48 and 17.0 for Node v24.19.0):
//   https://raw.githubusercontent.com/unicode-org/cldr/release-48/common/uca/allkeys_CLDR.txt
//   https://www.unicode.org/Public/17.0.0/ucd/UnicodeData.txt
// allkeys_CLDR.txt leaves Han, Tangut, Nushu and Khitan to an order ICU builds from radical-stroke data;
// that order is read from the running node's collator.
// Usage: node root-collation.js <allkeys_CLDR.txt> <UnicodeData.txt> <root-collation.gz>
const fs = require('fs')
const zlib = require('zlib')

const [allkeysFile, unicodeDataFile, outFile] = process.argv.slice(2)
const compare = new Intl.Collator('en').compare
const str = cp => String.fromCodePoint(cp)

const RANKED_BASE = 0x10000
const UNASSIGNED_BASE = 0x1000000
const SPECIAL_BASE = 0x7fff0000

const decompositions = new Map()
const combiningClasses = new Map()
for (const line of fs.readFileSync(unicodeDataFile, 'utf8').split('\n')) {
  const fields = line.split(';')
  if (fields.length < 6) {
    continue
  }
  const cp = parseInt(fields[0], 16)
  if (fields[3] !== '0') {
    combiningClasses.set(cp, Number(fields[3]))
  }
  if (fields[5] && !fields[5].startsWith('<')) {
    decompositions.set(cp, fields[5].split(' ').map(h => parseInt(h, 16)))
  }
}
const decompose = cp => decompositions.has(cp) ? decompositions.get(cp).flatMap(decompose) : [cp]

const singles = new Map()
const contractions = []
for (const line of fs.readFileSync(allkeysFile, 'utf8').split('\n')) {
  const m = /^([0-9A-F ]+?)\s*;\s*((?:\[[.*][0-9A-F.]+\])+)/.exec(line)
  if (!m) {
    continue
  }
  const cps = m[1].trim().split(/\s+/).map(h => parseInt(h, 16))
  const ces = [...m[2].matchAll(/\[[.*]([0-9A-F]+)\.([0-9A-F]+)\.([0-9A-F]+)\]/g)]
    .map(ce => [parseInt(ce[1], 16), parseInt(ce[2], 16), parseInt(ce[3], 16)])
  if (cps.length === 1) {
    singles.set(cps[0], ces)
  } else {
    contractions.push([cps, ces])
  }
}

// the table writes an ideograph's weight as the UCA's two implicit elements, which name its code point
const implicitCodePoint = (lead, trail) => {
  const low = trail & 0x7fff
  if (lead === 0xfb00) {
    return 0x17000 + low
  }
  if (lead === 0xfb01) {
    return 0x1b170 + low
  }
  if (lead === 0xfb02) {
    return 0x18b00 + low
  }
  if (lead >= 0xfb40 && lead <= 0xfbff) {
    return ((lead & 0x3f) << 15) | low
  }
  throw new Error(`implicit lead ${lead.toString(16)}`)
}
const isImplicit = primary => primary >= 0xfb00 && primary < 0xfffd
const implicit = new Set()
for (const ces of [...singles.values(), ...contractions.map(c => c[1])]) {
  for (let i = 0; i < ces.length; i++) {
    if (isImplicit(ces[i][0])) {
      implicit.add(implicitCodePoint(ces[i][0], ces[++i][0]))
    }
  }
}

// every code point whose weight is implicit, in collation order: first the ideographs, then the
// unassigned code points, which sort by code point
const unlisted = [...implicit].filter(cp => singles.has(cp))
for (let cp = 0; cp <= 0x10ffff; cp++) {
  const hangulSyllable = cp >= 0xac00 && cp <= 0xd7a3
  if (!singles.has(cp) && !decompositions.has(cp) && !hangulSyllable) {
    unlisted.push(cp)
  }
}
unlisted.sort((a, b) => compare(str(a), str(b)))
let tail = unlisted.length - 1
while (tail > 0 && unlisted[tail - 1] < unlisted[tail]) {
  tail--
}
const ranked = unlisted.slice(0, tail)
const rank = new Map(ranked.map((cp, i) => [cp, i]))
for (let i = 1; i < unlisted.length; i++) {
  if (compare(str(unlisted[i - 1]), str(unlisted[i])) >= 0) {
    throw new Error(`implicit code points tie: ${unlisted[i - 1].toString(16)} ${unlisted[i].toString(16)}`)
  }
}

const unplaced = new Set()
const widen = ces => {
  const out = []
  for (let i = 0; i < ces.length; i++) {
    const [primary, secondary, tertiary] = ces[i]
    if (isImplicit(primary)) {
      const cp = implicitCodePoint(primary, ces[++i][0])
      if (!rank.has(cp)) {
        unplaced.add(cp)
      }
      out.push([RANKED_BASE + rank.get(cp), secondary, tertiary])
    } else {
      out.push([primary >= 0xfffd ? SPECIAL_BASE + primary : primary, secondary, tertiary])
    }
  }
  return out
}

const chunks = []
const int = n => {
  const b = Buffer.alloc(4)
  b.writeInt32BE(n)
  chunks.push(b)
}
const byte = n => chunks.push(Buffer.from([n]))
const elements = ces => {
  const wide = widen(ces)
  byte(wide.length)
  for (const [primary, secondary, tertiary] of wide) {
    int(primary)
    const b = Buffer.alloc(2)
    b.writeUInt16BE((secondary << 5) | tertiary)
    chunks.push(b)
  }
}

for (let cp = 0; cp < 128; cp++) {
  if (singles.get(cp).length !== 1) {
    throw new Error(`ASCII U+${cp.toString(16)} does not have one element`)
  }
}
if (contractions.some(([cps]) => cps.every(cp => cp < 128))) {
  throw new Error('a contraction of ASCII alone')
}

// a character normalization takes apart is never looked up
const reachable = [...singles.keys()].filter(cp => !decompositions.has(cp)).sort((a, b) => a - b)
int(reachable.length)
for (const cp of reachable) {
  int(cp)
  elements(singles.get(cp))
}
int(contractions.length)
for (const [cps, ces] of contractions) {
  byte(cps.length)
  cps.forEach(int)
  elements(ces)
}
const decomposable = [...decompositions.keys()].sort((a, b) => a - b)
int(decomposable.length)
for (const cp of decomposable) {
  const full = decompose(cp)
  int(cp)
  byte(full.length)
  full.forEach(int)
}
const combining = [...combiningClasses.keys()].sort((a, b) => a - b)
int(combining.length)
for (const cp of combining) {
  int(cp)
  byte(combiningClasses.get(cp))
}
// each as its distance from the one before, zigzag in a variable-length integer
int(ranked.length)
let previous = 0
for (const cp of ranked) {
  const delta = cp - previous
  previous = cp
  let zigzag = delta < 0 ? -2 * delta - 1 : 2 * delta
  while (zigzag >= 0x80) {
    byte((zigzag & 0x7f) | 0x80)
    zigzag >>>= 7
  }
  byte(zigzag)
}

if (unplaced.size) {
  const hex = [...unplaced].map(c => c.toString(16))
  throw new Error(`implicit weights for code points the collator does not rank: ${hex}`)
}
fs.writeFileSync(outFile, zlib.gzipSync(Buffer.concat(chunks), { level: 9 }))
console.error(`${reachable.length} characters, ${contractions.length} contractions, ` +
  `${decomposable.length} decompositions, ${combining.length} combining classes, ${ranked.length} ranked ideographs ` +
  `(first unassigned U+${unlisted[tail].toString(16)}), ${fs.statSync(outFile).size} bytes`)
