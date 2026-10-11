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
// Evaluates Node's path.posix (with cwd fixed to /) on a fixed list of calls and writes
// {fn, args, result} rows for PosixPathTest.
// Usage: node path-cases.js <out>
const fs = require('fs')
const path = require('path').posix
process.chdir('/')
const calls = [
  ['resolve', ['/a/b', 'c']], ['resolve', ['/a/b', '../c']], ['resolve', ['/a/b', '/c', 'd']],
  ['resolve', ['a/b']], ['resolve', ['']], ['resolve', ['/']], ['resolve', ['/a/b/./c/..//d/']],
  ['resolve', ['/srv/user/projects/root', 'node_modules', 'foo']], ['resolve', ['/x', '..', '..']],
  ['relative', ['/a/b/c', '/a/b/c/d/e']], ['relative', ['/a/b/c', '/a/x']], ['relative', ['/', '/foo']],
  ['relative', ['/foo/bar', '/']], ['relative', ['/foo', '/foo']], ['relative', ['/foo/bar', '/foo/barbaz']],
  ['relative', ['/srv/user/projects/some/kind/of/path', '/srv/user/some/other/path']],
  ['relative', ['/x/y', '/x/z/y/a/b/c']], ['relative', ['/foo/bar/baz', '/foo/bar']],
  ['dirname', ['/a/b/c']], ['dirname', ['/a/b/c/']], ['dirname', ['/a']], ['dirname', ['/']], ['dirname', ['a']],
  ['dirname', ['//a']], ['dirname', ['']], ['dirname', ['a/b']],
  ['basename', ['/a/b/c']], ['basename', ['/a/b/c/']], ['basename', ['/']], ['basename', ['']], ['basename', ['a']],
  ['normalize', ['/a/b/../c']], ['normalize', ['a/./b/']], ['normalize', ['']], ['normalize', ['../../a']],
  ['normalize', ['/..']], ['normalize', ['./']],
  ['join', ['/a', 'b', '../c']], ['join', ['', '']], ['join', ['/', '/path/to/foo']], ['join', ['a', '', 'b']],
  ['isAbsolute', ['/a']], ['isAbsolute', ['a']], ['isAbsolute', ['']],
]
const out = calls.map(([fn, args]) => ({ fn, args, result: path[fn](...args) }))
fs.writeFileSync(process.argv[2], JSON.stringify(out, null, 1) + '\n')
console.log(`${out.length} calls`)
