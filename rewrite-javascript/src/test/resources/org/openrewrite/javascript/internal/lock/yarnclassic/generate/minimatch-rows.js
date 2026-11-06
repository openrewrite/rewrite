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
// Prints the rows of MinimatchTest: minimatch(path, pattern) as yarn 1.22.22's bundled minimatch 3.0.4 answers it, for
// the patterns a resolution or a workspace glob takes.
//
// Usage: node minimatch-rows.js <yarn 1.22.22>/lib/cli.js
const minimatch = require('./yarn-bundle.js')(process.argv[2]).load(/minimatch\.Minimatch = Minimatch/)
const cases = [
  ['**/ms', ['ms', 'debug/ms', 'a/b/ms', 'msx', 'debug/msx', '.x/ms']],
  ['debug/ms', ['debug/ms', 'morgan/debug/ms', 'ms']],
  ['**/debug/ms', ['debug/ms', 'morgan/debug/ms', 'debug/x/ms']],
  ['a/**/ms', ['a/ms', 'a/b/ms', 'a/b/c/ms', 'b/ms']],
  ['**/@scope/pkg', ['@scope/pkg', 'x/@scope/pkg', 'x/@scope/other']],
  ['@scope/pkg/**/ms', ['@scope/pkg/ms', '@scope/pkg/x/ms']],
  ['workspace-aggregator-x/a/left-pad', ['workspace-aggregator-x/a/left-pad', 'a/left-pad']],
  ['**/a/left-pad', ['workspace-aggregator-x/a/left-pad', 'a/left-pad', 'b/left-pad']],
  ['packages/*/+(package.json)', ['packages/a/package.json', 'packages/a/b/package.json', 'packages/.a/package.json',
    'packages/a/package.jsonpackage.json', 'other/a/package.json']],
  ['packages/*/node_modules/**/+(package.json)', ['packages/a/node_modules/x/package.json',
    'packages/a/package.json']],
  ['packages/**/+(package.json)', ['packages/a/package.json', 'packages/a/b/package.json',
    'packages/a/node_modules/x/package.json', 'packages/.a/b/package.json']],
  ['{a,b}/+(package.json)', ['a/package.json', 'b/package.json', 'c/package.json']],
  ['pkg-[ab]/+(package.json)', ['pkg-a/package.json', 'pkg-c/package.json']],
  ['pkg-[!a]/+(package.json)', ['pkg-a/package.json', 'pkg-c/package.json']],
  ['pkg-?/+(package.json)', ['pkg-a/package.json', 'pkg-ab/package.json']],
  ['!(x)/+(package.json)', ['a/package.json', 'x/package.json']],
  ['@(a|b)/+(package.json)', ['a/package.json', 'b/package.json', 'c/package.json']],
  ['packages/a/+(package.json)', ['packages/a/package.json', 'packages/b/package.json']],
  ['*', ['ms', '.ms', 'a/ms']],
  ['a\\*b', ['a*b', 'axb']],
  ['#comment', ['#comment']], // written quoted in MinimatchTest: CsvSource reads a leading # as a comment
  ['!ms', ['ms', 'debug']],
  ['{1..3}/x', ['1/x', '2/x', '4/x']],
  ['[z-a]/x', ['[z-a]/x', 'b/x']],
]
const rows = []
for (const [pattern, paths] of cases) {
  for (const p of paths) {
    rows.push(`"${pattern.replace(/\\/g, "\\\\")};${p};${minimatch(p, pattern)}",`)
  }
}
console.log(rows.join('\n'))
