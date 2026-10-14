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
// sort-cases.tsv: Array.prototype.sort of the running node (v24.19.0) over generated arrays, one per row.
// Usage: node sort-cases.js | gzip -9n > sort-cases.tsv.gz
const xorshift = seed => {
  let x = seed | 0 || 1
  return () => {
    x ^= x << 13
    x ^= x >>> 17
    x ^= x << 5
    return x >>> 0
  }
}

const COMPARATORS = {
  random: next => () => next() % 3 - 1,
  low: next => () => next() % 10 < 8 ? -1 : 1,
  high: next => () => next() % 10 < 8 ? 1 : -1,
  values: () => (a, b) => a.v - b.v,
  noisy: next => (a, b) => next() % 50 === 0 ? b.v - a.v || 1 : a.v - b.v,
}

const PATTERNS = {
  dups: (n, next) => i => next() % Math.max(1, n >> 2),
  ascending: () => i => Math.floor(i / 3),
  descending: n => i => n - i,
  sawtooth: () => i => i % 17,
  runs: (n, next) => {
    let left = 0
    let value = 0
    return () => {
      if (left-- === 0) {
        left = next() % 40
        value = next() % 100
      }
      return value++
    }
  },
  organ: n => i => i < n >> 1 ? i : n - i,
  few: (n, next) => i => next() % 3,
}

const rows = []
const emit = (comparator, pattern, n, gaps) => {
  const seed = Math.imul(rows.length + 1, 0x9e3779b1) >>> 0 || 1
  const next = xorshift(seed ^ 0x5bd1e995)
  const value = PATTERNS[pattern](n, next)
  const input = []
  const a = new Array(n)
  for (let i = 0; i < n; i++) {
    const gap = gaps ? next() % 100 : 100
    if (gap < 8) {
      input.push('u')
      a[i] = undefined
    } else if (gap < 16) {
      input.push('h')
    } else {
      const v = value(i)
      input.push(String(v))
      a[i] = {i, v}
    }
  }
  let calls = 0
  const compare = COMPARATORS[comparator](xorshift(seed))
  a.sort((x, y) => {
    calls++
    return compare(x, y)
  })
  const output = []
  for (let i = 0; i < n; i++) {
    output.push(!(i in a) ? 'h' : a[i] === undefined ? 'u' : String(a[i].i))
  }
  rows.push([comparator, pattern + (gaps ? '+gaps' : ''), seed, input.join(','), output.join(','), calls].join('\t'))
}

const comparators = Object.keys(COMPARATORS)
const patterns = Object.keys(PATTERNS)
for (let n = 0; n <= 140; n++) {
  emit('random', patterns[n % patterns.length], n, n % 5 === 0)
  emit('values', patterns[(n + 3) % patterns.length], n, n % 7 === 0)
  if (n % 4 === 0) {
    emit(comparators[2 + (n >> 2) % 3], patterns[(n >> 2) % patterns.length], n, n % 8 === 0)
  }
}
const long = [192, 255, 256, 257, 511, 512, 513, 1000, 1024, 1025, 2048, 2049, 4100]
long.forEach((n, k) => {
  comparators.forEach((comparator, c) => emit(comparator, patterns[(k + c) % patterns.length], n, (k + c) % 4 === 0))
})
process.stdout.write(rows.join('\n') + '\n')
