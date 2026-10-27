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
// is-glob 2.0.1 and 4.0.3 for upstream-calls.js: the module's function.
module.exports = {
  library: 'is-glob',
  entry: () => 'index.js',
  self: 'isGlob',
  tests: () => ['test.js'],
  // their tests require mocha for its globals, which upstream-harness.js installs
  shims: () => ({mocha: {}}),
  differences: ['', '!', '\\*', '\\{a,b}', 'a{b', 'a(b', 'a|b', '[]', '{}', '()', '\\!a', 'a/(b|c)', '@(a', '\\@(a)',
    'a[b\\]c]', 'a.?', '(?:a)', '(?!a', 'a\u0085)', '\\[a]!'].map(input => isGlob => isGlob(input))
}
