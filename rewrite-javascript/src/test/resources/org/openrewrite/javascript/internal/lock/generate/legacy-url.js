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
// LegacyUrlTest's rows: Node's legacy url.parse, asked of the Node that runs this, over git specs and tarball URLs in
// the forms hosted-git-info 3 and version-from-tgz read, and the edges of its parser.
// Usage: node legacy-url.js | gzip -9n > ../upstream/node-24.21.0-url-parse.jsonl.gz
const url = require('url')
process.removeAllListeners('warning')
const inputs = new Set()
const sha = '0123456789abcdef0123456789abcdef01234567'
const hosts = ['github.com', 'gitlab.com', 'GitHub.com', 'github.com:22', 'github.com:', 'registry.npmjs.org',
  '[::1]:8080', 'bücher.example', 'a_b.example', 'host%41.example', 'h"st.example']
const paths = ['u/r', 'u/r.git', 'g/s/r.git', 'u', '', 'u/r/', 'u/r.git/', '12345/r.git', 'u/re%20po.git',
  'u/r?x=1', 'u/r?x=1#y', 'abcdef1234567', "b'ar/-/b'ar-1.0.0.tgz", 'b|ar/-/b|ar-1.0.0.tgz',
  'bär/-/bär-1.0.0.tgz', 'bar/./-/bar-1.0.0.tgz', 'bar/-/x/../bar-1.0.0.tgz', 'b ar/x', 'a\\b\\c', 'a\\b?c\\d',
  'a{b}', 'a^b', 'a`b', 'a<b>', 'x/y;z', '%2Fx', '@s%2fx/-/x-1.0.0.tgz']
const hashes = ['', '#' + sha, '#a:b', '#a b']
for (const p of paths) {
  for (const c of hashes) {
    inputs.add(`${p}${c}`)
    for (const sc of ['github:', 'gist:', 'GITHUB:']) inputs.add(`${sc}${p}${c}`)
    for (const h of hosts) {
      for (const u of ['git@', 'u:p%40ss@', '']) {
        inputs.add(`${u}${h}:${p}${c}`)
        inputs.add(`git+ssh://${u}${h}/${p}${c}`)
        inputs.add(`git+ssh://${u}${h}:${p}${c}`)
        inputs.add(`https://${u}${h}/${p}${c}`)
      }
      inputs.add(`ssh://${h}/${p}${c}`)
      inputs.add(`git://${h}/${p}${c}`)
      inputs.add(`HTTP://${h}/${p}${c}`)
      inputs.add(`https:${h}/${p}${c}`)
      inputs.add(`https:/${h}/${p}${c}`)
      inputs.add(`//${h}/${p}${c}`)
    }
  }
}
for (const s of ['', ' ', '  https://a.com/x  ', '\thttps://a.com/x\n', ' https://a.com/x﻿',
  'https://a.com/x y z ', 'https://a\tb.com/x', 'https://a\nb.com/x', 'https://a.com\\x\\y', 'https://a.com/x\\y?z\\w',
  'javascript:alert(1)', 'JavaScript:x', 'mailto:a@b.com', 'file:///a/b', 'file:/a/b', 'file:a/b', 'http://',
  'http:', 'https://@', 'https://a@', 'https://:@a.com', 'https://a.com:abc/x', 'https://a.com:12x/y',
  'https://[::1', 'https://[a.com]/x', 'https://' + 'a'.repeat(300) + '.com/x', '/a/b?c', '//a/b', '///a/b',
  '?a=1', '#h', 'a#b?c', 'a?b#c', 'git@host', 'git@host:', 'x:y@z:w', 'u:p@h.com:1/x', 'h.com@x/y', 'a%zz@b.com/x',
  'https://a%zz@b.com/x', 'https://a.com/%', 'https://a.com/%zz', 'https://bÜcher.de/x', 'https://ex%41mple.com/x',
  'http://a.com?x', 'http://a.com#y', 'http://a.com;x', 'git+ssh://git@github.com:u/r.git#feature/x',
  'git+ssh://git@github.com:/u/r.git', 'git+ssh://git:pw@github.com/u/r.git']) {
  inputs.add(s)
}
const fields = ['protocol', 'slashes', 'auth', 'host', 'port', 'hostname', 'hash', 'search', 'query', 'pathname',
  'path']
for (const s of inputs) {
  const row = { test: 'url.parse', fn: 'url.parse', args: [s] }
  try {
    const u = url.parse(s)
    row.out = {}
    for (const f of fields) row.out[f] = u[f]
  } catch (e) {
    row.threw = { $: 'error', name: e.name, message: e.message, code: e.code }
  }
  console.log(JSON.stringify(row))
}
