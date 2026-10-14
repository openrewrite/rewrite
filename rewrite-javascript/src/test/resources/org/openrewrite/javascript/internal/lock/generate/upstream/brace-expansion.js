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
// brace-expansion 1.1.11, 5.0.6 and 5.0.9 for upstream-calls.js: expand. A 5.x release's tests import its build, which
// is the copy npm bundles (5.0.6 in npm 11.17.0, 5.0.9 in npm 11.19.1): BRACE_EXPANSION_5 names that package's
// directory.
const fs = require('fs')
const path = require('path')

const v5 = release => release.startsWith('5.')

module.exports = {
  library: 'brace-expansion',
  entry: (dir, release) => v5(release) ? process.env.BRACE_EXPANSION_5 : 'index.js',
  self: 'expand',
  functions: ['expand'],
  esmEntry: /^dist\/esm\/index\.js$/,
  esm: v5,
  tests: dir => fs.readdirSync(path.join(dir, 'test')).filter(f => f.endsWith('.js')).sort().map(f => 'test/' + f),
  // 1.1.11 loops forever on a zero step ({1..2..0}), so that is not asked
  differences: [
    expand => (expand.expand || expand)('a$b{c,d}{e,f}'),
    expand => (expand.expand || expand)('x{a}y,{c,d}'),
    expand => (expand.expand || expand)('{a},{,b}'),
    expand => (expand.expand || expand)('{a}y,,z}'),
    expand => (expand.expand || expand)('{a}{b,c},,d}'),
    expand => (expand.expand || expand)('{1..3}{a,b}'),
    // 5.0.9 combines the sets left to right and drops an empty result by the first set alone
    expand => (expand.expand || expand)('{x,y}{a},}'),
    expand => (expand.expand || expand)('{,a}{b},}'),
    expand => (expand.expand || expand)('{,a}{b}c,}'),
    expand => (expand.expand || expand)('{a,b}{}c,}'),
    expand => (expand.expand || expand)('{,a}{},}'),
    expand => (expand.expand || expand)('{,}{a},}'),
    expand => (expand.expand || expand)('{,x}{{a,b}}'),
    expand => (expand.expand || expand)('{a,b}{c},d}{e,f}'),
    expand => (expand.expand || expand)('x${a,b}{c,d}{,e}')
  ]
}
