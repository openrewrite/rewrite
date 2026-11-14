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
// VersionFromTgzTest's rows: what @npmcli/arborist's version-from-tgz.js answers for names and tarball URLs, as npm
// 10.4.0 (url.parse) and npm 11.17.0 (WHATWG URL) bundle it.
// Usage: node version-from-tgz.js <npm 10.4.0 package dir> <npm 11.17.0 package dir> | gzip -9n \
//          > ../upstream/version-from-tgz-10.4.0-11.17.0.jsonl.gz
const path = require('path')
process.removeAllListeners('warning')
const [, , oldDir, newDir] = process.argv
const lib = d => require(path.join(d, 'node_modules/@npmcli/arborist/lib/version-from-tgz.js'))
const releases = { '10.4.0': lib(oldDir), '11.17.0': lib(newDir) }
const names = ['foo', '@s/foo', 'bar', '@s/bar', '']
const hosts = ['https://registry.npmjs.org', 'http://registry.npmjs.org', 'https://registry.yarnpkg.com',
  'HTTPS://registry.npmjs.org', 'Https://registry.npmjs.org', 'https:/registry.npmjs.org', 'https:registry.npmjs.org',
  'https://user:pw@registry.npmjs.org', 'https://registry.npmjs.org:443', 'https://registry.npmjs.org/sub/path',
  'https://example.com', 'git+https://registry.npmjs.org', 'file://registry.npmjs.org', 'https://REGISTRY.npmjs.org',
  'https://registry.npmjs.org\\', 'https://[::1]:8080', 'https://registry.npmjs.org.']
const paths = ['/foo/-/foo-1.0.0.tgz', '/@s/foo/-/foo-1.0.0.tgz', '/@s%2ffoo/-/foo-1.0.0.tgz', '/@s%2Ffoo/-/foo-1.0.0.tgz',
  '/bar/-/bar-1.0.0.tgz', '/@s/bar/-/bar-1.0.0.tgz', '/foo/-/foo-1.0.0-beta.1.tgz', '/foo/-/foo-1.0.0+build.tgz',
  '/foo/-/foo-v1.0.0.tgz', '/foo-1.0.0.tgz', '/bar-1.0.0.tgz', '/x/../bar/-/bar-1.0.0.tgz', '/bar/-/x/../bar-1.0.0.tgz',
  '/bar/./-/bar-1.0.0.tgz', '/bar/-/./bar-1.0.0.tgz', '/b%61r/-/bar-1.0.0.tgz', '/bar/-/b%61r-1.0.0.tgz',
  "/b'ar/-/b'ar-1.0.0.tgz", '/b ar/-/b ar-1.0.0.tgz', '/b"ar/-/b"ar-1.0.0.tgz', '/b^ar/-/b^ar-1.0.0.tgz',
  '/b|ar/-/b|ar-1.0.0.tgz', '/b{ar}/-/b{ar}-1.0.0.tgz', '/b`ar/-/b`ar-1.0.0.tgz', '/bär/-/bär-1.0.0.tgz',
  '/b<ar>/-/b<ar>-1.0.0.tgz', '/bar\\-\\bar-1.0.0.tgz', '/bar/-/bar-1.0.0.tgz?x=bar-2.0.0.tgz',
  '/bar/-/bar-1.0.0.tgz#bar-2.0.0.tgz', '/bar/-/bar-1.0.0.tgz?bar-1.0.0.tgz', '//bar/-/bar-1.0.0.tgz',
  '/a/-/b/-/bar-1.0.0.tgz', '/bar/-/bar-1.0.0.tgz/', '/BAR/-/bar-1.0.0.tgz', '/bar/-/BAR-1.0.0.tgz',
  '/bar/-/bar-1.0.0.TGZ', '/%40s/bar/-/bar-1.0.0.tgz', '/bar/-%2Fbar-1.0.0.tgz', '/bar%2F-%2Fbar-1.0.0.tgz',
  '/bar/-/bar-1.0.0.tgz\t', '/bar/-/bar-01.0.0.tgz', '/bar/-/bar-1.0.tgz']
const urls = []
for (const h of hosts) for (const p of paths) urls.push(h + p)
urls.push('registry.npmjs.org/bar/-/bar-1.0.0.tgz', './bar/-/bar-1.0.0.tgz', 'bar-1.0.0.tgz', '/abs/bar/-/bar-1.0.0.tgz',
  'https://foo.tgz', 'https:', 'HTTP:x.tgz', 'https://a%zz@registry.npmjs.org/bar/-/bar-1.0.0.tgz')
for (const u of urls) {
  for (const name of names) {
    const row = { test: 'version-from-tgz', fn: 'versionFromTgz', args: [name, u], out: {} }
    for (const [r, fn] of Object.entries(releases)) {
      try {
        const v = fn(name, u)
        row.out[r] = v ? { name: v.name, version: v.version } : null
      } catch (e) {
        row.out[r] = { $: 'error', name: e.name, message: e.message }
      }
    }
    console.log(JSON.stringify(row))
  }
}
