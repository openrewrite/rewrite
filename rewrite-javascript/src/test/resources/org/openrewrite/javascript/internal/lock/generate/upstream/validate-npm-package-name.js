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
// validate-npm-package-name 4.0.0 and 7.0.2 for upstream-calls.js: the module's function. 4.0.0 requires builtins
// 5.1.0, the release pnpm 11.2.2 bundles, from wherever Node resolves it.
module.exports = {
  library: 'validate-npm-package-name',
  entry: () => 'lib/index.js',
  self: 'validate',
  tests: () => ['test/index.js'],
  differences: ['-a', '@a/.b', '@a/-b', 'node_modules', 'Favicon.ico', '@/a', '@a/', ' a', 'a\u00a0', 'a\ufeff',
    '\u2028a', '@a/b/c', '@a%2fb', 'a\nb'].map(input => validate => validate(input))
}
