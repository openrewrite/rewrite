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
// What the arborist npm bundles makes of a dependency whose package.json holds a version or a name of an
// unexpected JSON type. A node w under the root at /r has the version; p depends on w by the spec, x holds a
// nested w@2.0.0 (so canDedupe compares the two), g depends on w by a git spec with the spec as its range,
// and the root overrides w@<spec> to the spec; w is resolved to the git repository g names. Every value a
// reader could throw on is written as "THROW: <message>".
// "versions": the readers of node.version for each version and spec.
// "names": the readers of node.packageName, with a sibling of the same name and one of its string form.
// "locked": node.resolved after Shrinkwrap.add, for a lock entry version and a package version.
// "bundled": edge.bundled for each bundleDependencies value, normalized by the constructor and raw from the
// package setter.
// "folder": the name a path-less node takes from its package name.
// Usage: node version-shapes.js > ../version-shapes
process.chdir('/')
const npm = require('./npm-modules.js')
const Node = npm('@npmcli/arborist/lib/node.js')
const Shrinkwrap = npm('@npmcli/arborist/lib/shrinkwrap.js')

const attempt = fn => {
  try {
    return fn()
  } catch (e) {
    return 'THROW: ' + e.message
  }
}
const bool = fn => attempt(() => !!fn())

const values = [2, true, 10.5, [1], { a: 1 }, 0, false, null, '', 'abc', '1.0.0', 1e21, [], {}, '2.0.0']
const specs = ['^2.0.0', 'x', '>=0.0.0', '*', '', '1.x', '2.0.0', 'latest', '>=1 || x', '<0.0.0-0', 'x || 1',
  '2.0.0 - 3.0.0', 'a-b', ' ', '>=0.0.0-0', '^0.0.0', '=2.0.0']

const gitResolved = 'git+ssh://git@github.com/a/b.git#0123456789abcdef0123456789abcdef01234567'
const versions = []
for (const version of values) {
  for (const spec of specs) {
    const root = new Node({ path: '/r', pkg: { name: 'root', version: '1.0.0', overrides: { ['w@' + spec]: spec } }, loadOverrides: true })
    const w = new Node({ parent: root, name: 'w', pkg: { name: 'w', version }, resolved: gitResolved })
    const x = new Node({ parent: root, name: 'x', pkg: { name: 'x', version: '1.0.0', dependencies: { w: spec } } })
    const y = new Node({ parent: x, name: 'w', pkg: { name: 'w', version: '2.0.0' } })
    const p = new Node({ parent: root, name: 'p', pkg: { name: 'p', version: '1.0.0', dependencies: { w: spec } } })
    const g = new Node({ parent: root, name: 'g', pkg: { name: 'g', version: '1.0.0', dependencies: { w: 'github:a/b#semver:' + spec } } })
    const same = new Node({ path: '/elsewhere/node_modules/w', pkg: { name: 'w', version } })
    const string = new Node({ path: '/elsewhere/node_modules/w', pkg: { name: 'w', version: String(version) } })
    versions.push({
      version,
      spec,
      expected: {
        version: attempt(() => w.version),
        error: attempt(() => p.edgesOut.get('w').error),
        valid: attempt(() => p.edgesOut.get('w').valid),
        gitError: attempt(() => g.edgesOut.get('w').error),
        nodeRule: attempt(() => root.overrides.getNodeRule(w).key ?? null),
        matchingRule: attempt(() => root.overrides.getMatchingRule(w)?.key ?? null),
        canDedupe: attempt(() => y.canDedupe(false, false)),
        canDedupePreferred: attempt(() => y.canDedupe(true, false)),
        matches: bool(() => w.matches(y)),
        matchesSame: bool(() => w.matches(same)),
        matchesString: bool(() => w.matches(string)),
        overridden: bool(() => w.overridden),
        meta: attempt(() => npm.sortKeys(Shrinkwrap.metaFromNode(w, '/r'))),
      },
    })
  }
}

const names = []
for (const name of [5, true, 'w', null, '', 0, ['w'], { a: 1 }]) {
  const root = new Node({ path: '/r', pkg: { name: 'root', version: '1.0.0', dependencies: { w: '1' } } })
  const w = new Node({ parent: root, name: 'w', pkg: { name, version: '1.0.0' } })
  const same = new Node({ path: '/elsewhere/node_modules/w', pkg: { name, version: '1.0.0' } })
  const string = new Node({ path: '/elsewhere/node_modules/w', pkg: { name: String(name), version: '1.0.0' } })
  const other = new Node({ path: '/elsewhere/node_modules/w', pkg: { name: 'w', version: '1.0.0' } })
  names.push({
    name,
    expected: {
      packageName: attempt(() => w.packageName),
      matchesSame: bool(() => w.matches(same)),
      matchesString: bool(() => w.matches(string)),
      canReplaceWith: bool(() => w.canReplaceWith(other)),
      canReplaceWithSame: bool(() => w.canReplaceWith(same)),
      isWorkspace: bool(() => w.isWorkspace),
      edgesOutGet: bool(() => root.edgesOut.get(w.packageName)),
      meta: attempt(() => npm.sortKeys(Shrinkwrap.metaFromNode(w, '/r'))),
    },
  })
}

const locked = []
for (const lockVersion of [2, '2', '2.0.0', true, 0]) {
  for (const version of [2, '2', '2.0.0', true, 0]) {
    const meta = new Shrinkwrap({ path: '/r' })
    meta.data = {
      lockfileVersion: 3,
      packages: { 'node_modules/w': { version: lockVersion, resolved: 'https://r/w.tgz', integrity: 'sha512-w' } },
    }
    const root = new Node({ path: '/r', pkg: { name: 'root', version: '1.0.0', dependencies: { w: '*' } }, meta })
    const w = new Node({ parent: root, name: 'w', pkg: { name: 'w', version } })
    locked.push({ lockVersion, version, expected: { resolved: w.resolved, integrity: w.integrity } })
  }
}

const bundled = []
for (const bundleDependencies of [['w'], ['x'], 'w', 'x', 'vw', {}, { w: true }, 5, true, false, 0, null, [5], []]) {
  const pkg = { name: 'root', version: '1.0.0', dependencies: { w: '1' }, bundleDependencies }
  const root = new Node({ path: '/r', pkg: JSON.parse(JSON.stringify(pkg)) })
  const normalized = attempt(() => root.edgesOut.get('w').bundled)
  root.package = pkg
  bundled.push({ bundleDependencies, expected: { normalized, raw: attempt(() => root.edgesOut.get('w').bundled) } })
}

const folder = []
for (const name of [5, true, 10.5, [1], { a: 1 }, 'w', '@s/w', '', null, false]) {
  folder.push({ name, expected: attempt(() => new Node({ pkg: { name, version: '1.0.0' } }).name) })
}

console.log(JSON.stringify({ versions, names, locked, bundled, folder }))
