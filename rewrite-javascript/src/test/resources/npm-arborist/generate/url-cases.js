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
// Parses a fixed list of URLs (optionally against a base) with Node's URL and writes the result of
// each as {input, base, href, protocol, username, password, host, hostname, port, pathname, search,
// hash} or {input, base, invalid: true}.
// Usage: node url-cases.js <out>
const fs = require('fs')
const cases = [
  ['https://github.com/foo/bar'],
  ['HTTPS://GitHub.COM/Foo/Bar'],
  ['https://user:pass@github.com/a/b.git#0000'],
  ['http://insecure.com/foo.tgz'],
  ['https://registry.npmjs.org/foo/-/foo-1.2.3.tgz'],
  ['https://example.com:443/x'],
  ['http://example.com:80/x'],
  ['http://example.com:8080/x/../y/./z'],
  ['https://example.com/a%2fb/%7Ec?q=1#frag ment'],
  ['https://example.com/a b'],
  ['http://[::1]:3000/x'],
  ['http://0x7f.1/'],
  ['http://127.0.0.1/'],
  ['http://example.com\\a\\b'],
  ['git://github.com/a/b'],
  ['git://github.com/a/b.git#semver:^1.0.0'],
  ['git+https://github.com/a/b'],
  ['git+ssh://git@github.com/a/b.git'],
  ['git+ssh://git@github.com:a/b.git'],
  ['git+ssh://git@github.com:12345'],
  ['git+ssh://git@github.com:12345/foo'],
  ['git+ssh://git@notgithub.com/user/foo#1.2.3'],
  ['git+file:///foo/bar'],
  ['git+file://C:/foo/bar'],
  ['git+http://foo.com/bar'],
  ['github:foo/bar'],
  ['github:foo/bar#branch'],
  ['github://foo/bar'],
  ['gist:feedbeef'],
  ['foobar://user:host@path'],
  ['::'],
  [''],
  ['git+ssh://git@abc:frontend/utils.git#6d45'],
  ['ssh://git@github.com/a/b'],
  ['file:/x/y/z/blorg.tgz'],
  ['file:///path/to/foo'],
  ['file://localhost/path'],
  ['file:./foo', 'file:/test/a/b/'],
  ['file:../foo/bar', 'file:/test/a/b/'],
  ['file:foo', 'file:/test/a/b/'],
  ['file:/abs/path', 'file:/test/a/b/'],
  ['file:path/to/foo.tar.gz', 'file:/test/a/b/'],
  ['file:/test%25dir', 'file:/'],
  ['file:%7E/path', 'file:/test/a/b/'],
  ['file:', 'file:/test/a/b/'],
  ['file:../../../global-prefix/lib/node_modules/@myscope/bar'],
  ['https://gitlab.com/group/sub/project.git#v1'],
  ['https://www.github.com/foo/bar'],
  ['https://github.com/foo/bar/tree/main'],
  ['git+ssh://git@git.sr.ht/~foo/bar'],
]
const out = cases.map(([input, base]) => {
  try {
    const u = base === undefined ? new URL(input) : new URL(input, base)
    return { input, base: base ?? null, href: u.href, protocol: u.protocol, username: u.username,
      password: u.password, host: u.host, hostname: u.hostname, port: u.port, pathname: u.pathname,
      search: u.search, hash: u.hash }
  } catch (e) {
    return { input, base: base ?? null, invalid: true }
  }
})
fs.writeFileSync(process.argv[2], JSON.stringify(out, null, 1) + '\n')
console.log(`${out.length} cases, ${out.filter(o => o.invalid).length} invalid`)
