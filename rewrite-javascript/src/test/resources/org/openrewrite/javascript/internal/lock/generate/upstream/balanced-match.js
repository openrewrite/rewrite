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
// balanced-match 1.0.0 and 4.0.4 for upstream-calls.js: balanced and range. 4.0.4's tests import its sources, whose
// build npm 11.17.0 bundles: BALANCED_MATCH_4 names that package's directory.
const fs = require('fs')
const path = require('path')

const v4 = release => release.startsWith('4.')

module.exports = {
  library: 'balanced-match',
  entry: (dir, release) => v4(release) ? process.env.BALANCED_MATCH_4 : 'index.js',
  self: 'balanced',
  functions: ['balanced', 'range'],
  esmEntry: /^src\/index\.js$/,
  tests: (dir, release) => v4(release) ? ['test/test.ts'] : ['test/test.js']
}
