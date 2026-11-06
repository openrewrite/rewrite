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
// MinimatchTest's and MapWorkspacesTest's rows from the minimatch and @npmcli/map-workspaces npm 11.17.0 bundles.
// Usage: node minimatch-cases.js > ../minimatch-cases
const fs = require('fs')
const os = require('os')
const path = require('path')
const npm = require('./npm-modules.js')
const mapWorkspaces = npm('@npmcli/map-workspaces')
const { minimatch } = npm('minimatch')

const pairs = []
const mm = (f, p, o) => pairs.push({ path: f, pattern: p, options: o || {}, matches: minimatch(f, p, o) })
const partial = { partial: true }
for (const [f, p] of [
  ['node_modules', '**/node_modules/**'], ['node_modules/a', '**/node_modules/**'],
  ['a/node_modules', '**/node_modules/**'],
  ['a/node_modules/b', '**/node_modules/**'], ['node_modules/', '**/node_modules/**'],
  ['x/node_modules/', '**/node_modules/**'],
  ['packages/a', 'packages/*/'], ['packages/a/', 'packages/*'], ['packages/a/', 'packages/*/'],
  ['packages/.a', 'packages/*'], ['packages/.a', 'packages/.*'], ['packages/a', '**'], ['.git/a', '**'],
  ['', '*'], ['', '**'], ['a', ''], ['', ''], ['a', '#a'], ['x', '{x,y}'], ['x3', 'x{1..5}'], ['x03', 'x{01..05}'],
  ['a/b', 'a/\\*'], ['a/*', 'a/\\*'], ['a/b', 'a/[!b]'], ['a/c', 'a/[!b]'], ['a/b', 'a/[^b]'], ['a/b', 'a/[[:alpha:]]'],
  ['a/1', 'a/[[:alpha:]]'], ['a/1', 'a/[[:digit:]]'], ['a/b', 'A/B'], ['a//b', 'a/b'], ['a/b', 'a//b'], ['./a', 'a'],
  ['a', './a'], ['a/b/c', 'a/**/c'], ['a/c', 'a/**/c'], ['a/.b/c', 'a/**/c'], ['a/b/.c', 'a/**/.c'],
  ['a/b/c', 'a/**/**/c'], ['a/b', 'a/**/b/**'], ['a/b/', 'a/**/b/**'], ['a/b/c', 'a/**/b/**'], ['a', 'a/**'],
  ['a/', 'a/**'], ['ab', 'a*'], ['a', 'a*'], ['a.b', 'a*'], ['.b', '*b'], ['.b', '.*'], ['.', '.'], ['..', '..'],
  ['.', '*'], ['..', '.*'], ['a/..', 'a/*'], ['a/..', 'a/**'], ['a/../b', 'a/**/b'], ['x', 'x/'],
  ['a\\b', 'a\\\\b'], ['aXb', 'a[X-Z]b'], ['a-b', 'a[-]b'], ['a]b', 'a[]]b'], ['a', '[a'], ['[a', '[a'],
  ['ab', 'a{b'], ['a{b', 'a{b'], ['a', 'a{}'], ['a{}', 'a{}'], ['ab', 'a{,b}'], ['a', 'a{,b}'], ['a$b', 'a$b'],
  ['a/b/c/d', 'a/**/b/**/d'], ['a/b/x/d', 'a/**/b/**/d'], ['a/x/b/d', 'a/**/b/**/d'], ['a/b/c/d', '**/c/**'],
  ['a/b/c/d', '**/d'], ['a/b/c/d', 'a/**'], ['a/b/c/d', '**/b/c/d'], ['a/b/c/d', 'a/*/*/*'], ['a/b/c/d', 'a/*/*'],
  ['a.js', '*.js'], ['a.js', '*.JS'], ['.a.js', '*.js'], ['a/b.js', '**/*.js'], ['packages/a-b', 'packages/*-*'],
  ['packages/a b', 'packages/* *'], ['@scope/x', '@*/*'], ['pkg', 'p?g'], ['pkg', 'p??g'], ['péché', 'p?ch?'],
  ['a/b', 'a/b/..'], ['a', 'a/b/..'], ['a/b', 'a/b/.'], ['a/b/.', 'a/b/.'], ['packages/a', '!packages/a'],
  ['packages/b', '!packages/a'], ['packages/a', '!!packages/a'], ['apps/x', '{packages,apps}/*'],
  ['packages/x/y', '{packages,apps}/**'], ['a1', 'a{1,2}{,x}'], ['a2x', 'a{1,2}{,x}'], ['ab', 'a{b,{c,d}}'],
  ['ad', 'a{b,{c,d}}'], ['a{b', 'a\\{b'], ['a', 'a{,}'], ['abc', 'a{b}c'], ['a{b}c', 'a{b}c'], ['a-1', 'a-{1..1}'],
]) {
  mm(f, p)
}
for (const [f, p] of [
  ['packages/a', 'packages/a/b'], ['packages', 'packages/*'], ['packages/a/b', 'packages/*'], ['/', 'anything'],
  ['packages/a', 'packages/*/'], ['packages/a/', 'packages/a/'], ['a', 'a/**/b'], ['a/x', 'a/**/b'], ['a/x', 'a/**'],
  ['a', '**/b'], ['', 'a/b'], ['packages/a', 'packages/*'], ['x', 'packages/*'],
]) {
  mm(f, p, partial)
  mm(f, p, { partial: true, windowsPathsNoEscape: true })
}
mm('a/b', 'a\\b', { windowsPathsNoEscape: true })
mm('a', 'a\\b', { windowsPathsNoEscape: true })
mm('.a', '*', { dot: true })
mm('.', '*', { dot: true })
mm('..', '*', { dot: true })
mm('a/.b/c', 'a/**/c', { dot: true })
mm('#a', '#a', { nocomment: true })
mm('a', '!a', { nonegate: true })
mm('!a', '!a', { nonegate: true })

const layouts = [
  {
    name: 'mixed',
    dirs: {
      'packages/a': { name: 'a' }, 'packages/b': { name: '@s/b' }, 'packages/b/nested': { name: 'nested' },
      'packages/.hidden': { name: 'hidden' }, 'packages/nopkg': null, 'apps/x': { name: 'x' },
      'apps/x/node_modules/y': { name: 'y' }, 'node_modules/z': { name: 'z' }, 'lib/low': { name: 'low' },
      'packages/noname': {}, 'packages/c/d/e': { name: 'deep' },
    },
    workspaces: [
      ['packages/*'], ['packages/**'], ['**'], ['*/*'], ['packages/*', '!packages/b'],
      ['packages/*', '!packages/b', 'packages/b'], ['!packages/b', 'packages/*'], ['{packages,apps}/*'],
      ['packages/b/nested', 'packages/*'], ['./packages/a', '/packages/b'], ['packages/a/', 'apps/x/'],
      ['apps/x/node_modules/y'], ['packages/[ab]'], ['packages/?'], ['lib/*'], ['packages/nopkg'],
      ['packages/noname'], [], { packages: ['apps/*'] }, 'packages/*', ['packages/*', 'apps/*'],
      ['apps/*', 'packages/*'], ['pack*/*'], ['packages/*/'], ['packages'], ['.'], [''], ['packages/c/**'],
      ['packages/*/*'], ['packages/*', '!packages/**'], ['**/*', '!**/node_modules/**'], ['packages/*', 'packages/*'],
      ['!!packages/a'], ['!packages/*', 'packages/a'],
      ['!packages/b/**', '!packages/b/*', 'packages/b/nested'],
      ['!packages/b/*', '!packages/b/**', 'packages/b/nested'],
      ['!packages/b', '!packages/*', '!packages/**', 'packages/b'],
      ['!packages/*', '!packages/b', 'packages/b', 'packages/a'],
    ],
  },
  {
    name: 'duplicate-names',
    dirs: { 'a': { name: 'same' }, 'b': { name: 'same' }, 'c': { name: 'other' } },
    workspaces: [['a', 'b', 'c'], ['a', 'c'], ['*']],
  },
  {
    name: 'root-manifest',
    dirs: { '': { name: 'root', version: '1.0.0' }, 'a': { name: 'a' } },
    workspaces: [['.'], ['*'], ['**'], ['a', '.']],
  },
  {
    name: 'invalid-member',
    dirs: { 'a': 'not json', 'b': { name: 'b' } },
    workspaces: [['b'], ['*']],
  },
  {
    name: 'member-bin-dir',
    dirs: { 'a': { name: 'a', directories: { bin: 'bin' } }, 'b': { name: 'b', directories: { bin: 1 } }, 'c': {} },
    workspaces: [['a'], ['a', 'c'], ['b'], ['*']],
  },
  {
    name: 'member-without-content',
    dirs: { 'a': 'null', 'b': { name: 'b' } },
    workspaces: [['b'], ['*']],
  },
]

const run = async () => {
  const out = { pairs, layouts: [] }
  for (const layout of layouts) {
    const dir = fs.mkdtempSync(path.join(os.tmpdir(), 'mapws-'))
    const manifests = {}
    for (const [p, pkg] of Object.entries(layout.dirs)) {
      fs.mkdirSync(path.join(dir, p), { recursive: true })
      if (pkg !== null) {
        const text = typeof pkg === 'string' ? pkg : JSON.stringify(pkg)
        fs.writeFileSync(path.join(dir, p, 'package.json'), text)
        manifests[p] = text
      }
    }
    const cases = []
    for (const workspaces of layout.workspaces) {
      const c = { workspaces }
      try {
        const m = await mapWorkspaces({ cwd: dir, pkg: { workspaces } })
        c.members = [...m].map(([n, p]) => [n, path.relative(dir, p)])
      } catch (e) {
        c.error = { code: e.code, message: e.message.split(dir).join('{CWD}') }
      }
      const packages = { '': { workspaces } }
      for (const [p, pkg] of Object.entries(layout.dirs)) {
        if (p && pkg && typeof pkg === 'object') {
          packages[p] = pkg
          packages['node_modules/' + (pkg.name || path.basename(p))] = { resolved: p, link: true }
        }
      }
      try {
        const v = mapWorkspaces.virtual({ cwd: '/project', lockfile: { packages } })
        c.virtual = [...v].map(([n, p]) => [n, path.relative('/project', p)])
      } catch (e) {
        c.virtualError = { code: e.code, message: e.message }
      }
      cases.push(c)
    }
    fs.rmSync(dir, { recursive: true })
    out.layouts.push({ name: layout.name, manifests, cases })
  }
  console.log(JSON.stringify(out, null, 1))
}
run()
