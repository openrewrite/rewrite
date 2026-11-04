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
// SpecTest's dependency-value rows as the npm-package-arg npm 11.17.0 bundles answers them (npa.resolve(name, value)).
// Usage: node dependency-values.js
const npa = require('./npm-modules.js')('npm-package-arg')

const KINDS = ['catalog', 'workspace', 'link', 'portal', 'patch', 'jsr', 'exec']
const VALUES = [
  'catalog:', 'catalog:react17', 'workspace:^', 'workspace:1.4.1', 'npm:@acme/logger@^1.4.1',
  'patch:acme-logger@1.4.1#fix.patch', 'portal:../acme-logger', 'file:../acme-logger', 'link:../acme-logger',
  'jsr:@std/path@^1', 'exec:./gen.js', 'github:acme/logger', 'https://example.com/acme.tgz',
  'http://example.com/a.tgz', 'git+ssh://example.com/acme.git', 'git+https://example.com/a.git', 'git+file:///tmp/a',
  'git:x', 'user/repo', 'mochajs/mocha#4727d357ea', 'user/repo#semver:^1.0.0', 'user/repo#semver:>=1 <2',
  'git@github.com:user/repo.git', 'gitlab:a/b', 'bitbucket:a/b', 'gist:abc', 'sourcehut:a/b', '../pkg', './pkg',
  '/abs/pkg', '~/pkg', '.local', 'C:\\pkgs\\foo', 'c:/pkgs/foo', 'foo-1.0.0.tgz', 'team/sub/repo', '@scope/pkg',
  'Catalog:', 'gitmoji:x', 'future:x', '^1.4.1', '~1.4.1', '1.4.1', '1.x', '*', '', 'latest', 'next',
  '>=1.0.0 <2.0.0', '1.2.3 - 2.3.4', '1.2.3-beta.1', '1.2.3+build.4',
]
const REJECTED = ['foo\\bar', 'git-lfs:x', 'x+y:abc', '-bad:']
const LOCATIONS = [
  'github:me/old-fork', 'git+https://example.com/a', 'https://example.com/a.tgz', 'file:../a', '../a',
  'foo-1.0.0.tgz', 'link:../a', 'portal:../a', 'sourcehut:a/b', 'catalog:', 'workspace:*', 'patch:a@1.0.0#./p.patch',
  'npm:other@^1', 'jsr:@std/path@^1', 'exec:./gen.js', 'future:x', '^1.0.0',
]

function classify(value) {
  try {
    const r = npa.resolve('acme-logger', value, '/srv/where')
    const scheme = /^[a-z][a-z0-9+.-]*:/i.exec(r.rawSpec)
    const protocol = ['range', 'version', 'tag'].includes(r.type) ? null :
      r.type === 'alias' ? 'npm:' : r.type === 'file' || r.type === 'directory' ? 'file:' :
        r.hosted ? r.hosted.type + ':' : scheme ? scheme[0].toLowerCase() : 'git+ssh:'
    return {type: r.type, protocol}
  } catch (e) {
    if (e.code !== 'EUNSUPPORTEDPROTOCOL') {
      return {code: e.code}
    }
    const kind = KINDS.find(k => value.startsWith(k + ':'))
    return {type: kind || 'unknown-protocol', protocol: /^[a-z]+:/i.exec(value)[0].toLowerCase()}
  }
}

const java = v => v === '' ? "''" : v.replace(/\\/g, '\\\\')
console.log('dependencyValue')
for (const value of VALUES) {
  const c = classify(value)
  console.log(`      ${java(value).padEnd(33)} | ${c.type.padEnd(16)} | ${c.protocol}`)
}
console.log('dependencyValueNpaRejects')
console.log(REJECTED.map(v => `${JSON.stringify(v)} ${classify(v).code}`).join('\n'))
console.log('location')
for (const value of LOCATIONS) {
  const c = classify(value)
  const location = ['git', 'file', 'directory', 'remote', 'link', 'portal'].includes(c.type)
  console.log(`      ${java(value).padEnd(27)} | ${location}`)
}
