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
// Writes micromatch-generated.tsv: seeded patterns over micromatch 2.3.11's syntax with yarn 1.22.22's micromatch's
// answers; a makeRe source past `${` keeps braces' Math.random id, which differs from run to run.
//
// Usage: SHORT=1 node micromatch-generated.js <yarn 1.22.22>/lib/cli.js 500 1 > micromatch-generated.tsv
const [cli, countArg, seedArg] = process.argv.slice(2)
const count = Number(countArg || 2000)
const mm = require('./micromatch-recorder.js')(cli, `${count} patterns generated over micromatch 2.3.11's syntax, ` +
  "with yarn 1.22.22's bundled micromatch's answer: makeRe() (the source toRegex gives new RegExp) and micromatch() " +
  'over a fixed list of paths')

let seed = Number(seedArg || 1)
function rand(n) {
  seed = (seed + 0x6D2B79F5) | 0
  let t = Math.imul(seed ^ (seed >>> 15), 1 | seed)
  t = (t + Math.imul(t ^ (t >>> 7), 61 | t)) ^ t
  return (((t ^ (t >>> 14)) >>> 0) % n)
}
const pick = list => list[rand(list.length)]

const words = ['a', 'b', 'ab', 'app', 'mid', 'x.y', '.x', 'x.', '1', '2', 'A', 'c', 'packages', 'a-b', 'a_b', '@s']
const tokens = ['*', '**', '?', '??', '[ab]', '[!a]', '[^a]', '[a-c]', '[[:alpha:]]', '[[:digit:]]', '[[:space:]]',
  '[[:punct:]]', '[[:alnum:][:digit:]]', '[[=a=]]', '{a,b}', '{a,}', '{,a}', '{1..3}', '{a..c}', '{1..10}',
  '{01..3}', '{3..1}', '{a..c..2}', '{1..5..2}', '{1..3..+}', '{a..c..>}', '{a..c..|}', '{1..3..~}', '{-1..2}',
  '@(a|b)', '!(a)', '+(a)', '*(a)', '?(a)', '@(a|*.y)', '!(*.y)', '*.!(y)', '\\*', '\\?', '\\[', '\\{a,b}',
  '(', ')', '[', ']', '{', '}', '$', '+', '.', '..', '{}', '{,}', '${x}', '${a}{b,c}', ',', '|', '^', '=', '"',
  "'", '%~', '%%', ' ', 'a b', '{a, b}', '[]', '[!]', '\\', '\\\\', '(?:a)', '(?=a)', '__UNESC_STAR__',
  '__ESC_X__', '{x.y,z}', '{.x,y}', '{a,{b,c}}', '{a,b}{c,d}', '{a..c}{1..2}', '[a-]', '[-a]', '[z-a]', 'a{2}',
  '\\.', '\\,', '/.', '{a\\,b}', '{1\\.2}', '[[:alpha:]', '[:alpha:]]', 'C:', '!', '!!', '*(a|b)', '+(*.y)']
const paths = ['a', 'b', 'ab', 'app', 'mid/app', 'mid/b', 'a/b', 'a/b/c', '.x', 'a/.x', 'x.y', 'a.b', '1', '2',
  '10', 'A', 'a/x.y', 'mid/a.b', 'packages/a', 'packages/a/b', 'a b', '(a)', '[a]', '$', 'a+b', 'c', 'x', 'y',
  '.x/a', 'a/..', '..', '.', 'abc', 'aa', 'a/b/c/d', 'mid', 'a-b', 'a_b', '@s/a', '03', '3', 'b.y', 'a.y']

const shortPaths = ['a', 'ab', 'mid/app', 'a/b', '.x', 'a/.x', 'x.y', '1', '10', 'a b', '(a)', '[a]', '..', '@s/a']
const matched = process.env.SHORT ? shortPaths : paths

function segment() {
  const n = 1 + rand(3)
  let s = ''
  for (let i = 0; i < n; i++) s += rand(3) === 0 ? pick(words) : pick(tokens)
  return s
}

function pattern() {
  const parts = []
  const n = 1 + rand(3)
  for (let i = 0; i < n; i++) parts.push(segment())
  let p = parts.join('/')
  const r = rand(12)
  if (r === 0) p = './' + p
  else if (r === 1) p = '!' + p
  else if (r === 2) p = p + '/'
  else if (r === 3) p = '/' + p
  return p
}

function call(fn) {
  try {
    fn()
  } catch (e) {
    // recorded by the wrapper
  }
}

for (let i = 0; i < count; i++) {
  const p = pattern()
  call(() => mm.makeRe(p))
  call(() => mm(matched, p))
  if (i % 4 === 0) {
    const q = pattern()
    call(() => mm(matched, [p, '!' + q]))
  }
}
