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
// The cases of arborist's test/spec-from-lock.js, evaluated by the arborist npm bundles from the working
// directory /: {about, name, lock, expected}, where expected is null for the empty object.
// Usage: node spec-from-lock-cases.js > ../spec-from-lock-cases
process.chdir('/')
const npm = require('./npm-modules.js')
const sfl = npm('@npmcli/arborist/lib/spec-from-lock.js')

const cases = [
  ['version and integrity', 'x', { version: '1.2.3', integrity: 'integral' }],
  ['git repo with resolved value', 'gitthing', {
    version: 'git+ssh://git@github.com/isaacs/abbrev-js#a9ee72ebc8fe3975f1b0c7aeb3a8f2a806a432eb',
    from: 'github:isaacs/abbrev-js#some-ref',
  }],
  ['legacy metadata with "from" and no integrity', 'legacy', { from: '1.2.3' }],
  ['version (file) and integrity set', 'x', { version: 'foo.tgz', integrity: 'integral' }],
  ['version and range, no integrity', 'x', {
    version: '1.2.3',
    from: '^1.2.0',
    shasum: 'deadbeef0cafebad',
    resolved: 'https://registry.npmjs.org/x/-/x-1.2.3.tgz',
  }],
  ['file with from, no integrity', 'x', { version: 'file:x-1.2.3.tgz', from: 'x-1.2.3.tgz' }],
  ['file with resolved, no integrity', 'x', { version: 'file:x-1.2.3.tgz', resolved: '/path/to/x-1.2.3.tgz' }],
  ['file with resolved and from', 'x', {
    version: 'file:x-1.2.3.tgz',
    from: 'file:x-1.2.3.tgz',
    resolved: '/path/to/x-1.2.3.tgz',
  }],
  ['directory symlink', 'x', { version: 'file:../some/path' }],
  ['completely invalid, return empty object', 'really bad and invalid', {
    version: 'url:// not even close to a ! valid @ npm @ specifier',
    resolved: 'this: is: also: not: valid!',
  }],
]
const fields = ['type', 'registry', 'where', 'raw', 'name', 'escapedName', 'scope', 'rawSpec', 'saveSpec', 'fetchSpec',
  'gitRange', 'gitCommittish', 'gitSubdir']
console.log(JSON.stringify(cases.map(([about, name, lock]) => {
  const spec = sfl(name, lock)
  const expected = spec.type === undefined ? null : Object.fromEntries(fields.map(f => [f, spec[f] ?? null]))
  if (expected) {
    expected.hosted = spec.hosted ? spec.hosted.toString() : null
  }
  return { about, name, lock, expected }
}), null, 1))
