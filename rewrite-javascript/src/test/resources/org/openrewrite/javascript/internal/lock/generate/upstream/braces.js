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
// braces 1.8.5 and 3.0.2 for upstream-calls.js: braces() and 3.0.2's expand, compile, parse, stringify ({"$": "ast"}),
// with the dependencies yarn 1.22.22 and Yarn 4.10.3 bundle installed beside each checkout.
const fs = require('fs')
const path = require('path')

const v3 = process.argv[4].startsWith('3.')

module.exports = {
  library: 'braces',
  entry: () => 'index.js',
  self: 'braces',
  functions: ['expand', 'compile', 'parse', 'stringify', 'create'],
  tests: dir => fs.readdirSync(path.join(dir, 'test')).filter(f => f.endsWith('.js')).sort().map(f => 'test/' + f),
  shims: () => ({mocha: {}, 'bash-path': () => '/bin/bash'}),
  // a home directory in 1.8.5's own test, spelled under /srv as other upstream paths are; braces reads it as text
  redact: line => line.replace(/\/Users\/(?=tobiasreich\/)/g, '/srv/'),
  encode(v) {
    return v && typeof v === 'object' && typeof v.type === 'string' && Array.isArray(v.nodes) ? {$: 'ast'} : undefined
  },
  // yarn's micromatch asks 1.8.5 with makeRe, fast-glob asks 3.0.2 with these three
  differences: ['a{b,c}d', '{a,b}{c,d}', 'a\\{b,c}', '{1..3}', '{a..c}', '{01..03}', '{1..10..3}', 'a/{b,c}/d',
    '{,a}', '{a,}', '${a,b}', '{a,b', '{"a,b"}', '{a..e..x}', '{a,b}/{c,d}/{e,f}', 'a{1..3}b', '{-1..2}',
    '{a..A}', '{1..2}{a,b}', 'a/**/{b,c}.js', '{[a],b}', '{(a|b),c}', '{a,b}c{d,e}', '{a,{b,c}}', '{3..1}']
    .map(input => braces => braces(input, v3 ? {expand: true, nodupes: true, keepEscaping: true} : {makeRe: true}))
}
