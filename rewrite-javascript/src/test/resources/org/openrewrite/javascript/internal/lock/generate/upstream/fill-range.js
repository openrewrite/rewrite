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
// fill-range 2.2.4 and 7.0.1 for upstream-calls.js: the module's function, with the dependencies yarn 1.22.22 and Yarn
// 4.10.3 bundle installed beside each checkout.
const fs = require('fs')
const path = require('path')

const v7 = process.argv[4].startsWith('7.')

module.exports = {
  library: 'fill-range',
  entry: () => 'index.js',
  self: 'fill',
  tests: dir => fs.readdirSync(path.join(dir, 'test')).filter(f => f.endsWith('.js')).sort().map(f => 'test/' + f),
  shims: () => ({mocha: {}}),
  // the arguments braces passes: 1.8.5's expand-range asks 2.2.4 with makeRe, 3.0.2 asks 7.0.1 with its options
  differences: [['1', '3'], ['a', 'c'], ['01', '03'], ['1', '10', '3'], ['-1', '2'], ['a', 'A'], ['3', '1'],
    ['a', 'e', 'x'], ['1', '5', '+'], ['1', '5', '|'], ['1', '5', '>'], ['0', '-0'], ['1', '20'], ['1.1', '2'],
    ['a', '1'], ['', '2'], ['1', '9', '2~'], ['10', '1', '-3'], ['z', 'a', '5'], ['001', '10']]
    .map(args => fill => fill(...args, v7 ? {expand: true, nodupes: true, keepEscaping: true} : {makeRe: true}))
}
