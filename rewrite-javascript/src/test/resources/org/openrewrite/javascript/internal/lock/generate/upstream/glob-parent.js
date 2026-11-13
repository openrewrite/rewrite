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
// glob-parent 2.0.0 and 5.1.2 for upstream-calls.js: the module's function.
const v5 = release => release.startsWith('5.')

module.exports = {
  library: 'glob-parent',
  entry: () => 'index.js',
  self: 'globParent',
  tests: (dir, release) => v5(release) ? ['test/index.test.js'] : ['test.js'],
  differences: ['', 'path/{a,b}/c', 'path/[a]/b', 'path/\\*.js', 'path/(a|b', 'a/\\{b}/c', 'a/[b]', '{a,b}/',
    'a/(b|c)/d', 'a/@(b)/c', 'a\\(b/c', 'path/!x', '/', '//a/*'].map(input => gp => gp(input))
}
