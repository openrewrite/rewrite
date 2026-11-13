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
// Expectations for VirtualTreeLoaderTest and the load/commit/toString cases of ShrinkwrapTest, from the
// arborist npm bundles over the fixture directories of test/arborist/load-virtual.js and test/shrinkwrap.js
// in an npm/cli checkout. Each fixture is copied under a temporary root so that its paths read
// /fixtures/<name>; the files a lock-only install reads (the lock, yarn.lock, every package.json outside
// node_modules) are recorded as the inputs, with the tree arborist loads (in NpmTreeSupport.describe's
// format), the lock data it commits and the text it writes.
// The inputs are stored once per fixture and the after-flags tree and text only where they differ; the
// file is gzipped, as the lock texts of the larger fixtures repeat through it.
// Usage: node load-virtual-dumps.js <npm/cli checkout> > ../load-virtual-dumps.gz
process.chdir('/')
const fs = require('fs')
const os = require('os')
const path = require('path')
const zlib = require('zlib')
const npm = require('./npm-modules.js')
const Arborist = npm('@npmcli/arborist')
const Shrinkwrap = npm('@npmcli/arborist/lib/shrinkwrap.js')
const Node = npm('@npmcli/arborist/lib/node.js')
const calcDepFlags = npm('@npmcli/arborist/lib/calc-dep-flags.js')
const resetDepFlags = npm('@npmcli/arborist/lib/reset-dep-flags.js')

const fixtures = path.resolve(process.argv[2], 'workspaces/arborist/test/fixtures')
const tmp = fs.mkdtempSync(path.join(os.tmpdir(), 'lv-'))
const ROOT = '/fixtures'
const clean = s => typeof s === 'string' ? s.split(tmp).join(ROOT) : s
const json = v => JSON.stringify(npm.sortKeys(v))

const loc = n => n ? (n.location === undefined ? '?' : JSON.stringify(n.location)) : '-'
const describe = root => {
  const out = []
  for (const [l, n] of root.inventory) {
    out.push(`${JSON.stringify(l)} ${n.constructor.name} name=${n.name} pkg=${n.packageName}@${n.version} ` +
      `path=${n.path} realpath=${n.realpath} parent=${loc(n.parent)} fsParent=${loc(n.fsParent)} ` +
      `target=${loc(n.target)} root=${loc(n.root)} ` +
      `flags=${[n.dev, n.optional, n.devOptional, n.peer, n.extraneous].map(Number).join('')} ` +
      `resolved=${n.resolved} integrity=${n.integrity}`)
    for (const [k, c] of n.children) {
      out.push(`  child ${k} ${loc(c)}`)
    }
    for (const c of n.fsChildren) {
      out.push(`  fsChild ${loc(c)}`)
    }
    for (const c of n.linksIn) {
      out.push(`  linkIn ${loc(c)}`)
    }
    for (const [k, e] of n.edgesOut) {
      out.push(`  out ${k} ${e.type} ${e.spec} to=${loc(e.to)} error=${e.error}`)
    }
    for (const e of n.edgesIn) {
      out.push(`  in ${loc(e.from)} ${e.type} ${e.name}`)
    }
  }
  for (const t of root.tops) {
    out.push(`top ${loc(t)}`)
  }
  return clean(out.join('\n'))
}

const read = f => fs.existsSync(f) ? fs.readFileSync(f, 'utf8') : null
const manifestsOf = dir => {
  const out = {}
  const walk = (d, rel) => {
    for (const entry of fs.readdirSync(d, { withFileTypes: true })) {
      if (entry.name === 'node_modules') {
        continue
      }
      const full = path.join(d, entry.name)
      const r = rel ? rel + '/' + entry.name : entry.name
      if (entry.isDirectory()) {
        walk(full, r)
      } else if (entry.name === 'package.json') {
        out[rel] = fs.readFileSync(full, 'utf8')
      }
    }
  }
  walk(dir, '')
  return out
}
const inputs = dir => ({
  lock: read(path.join(dir, 'npm-shrinkwrap.json')) ?? read(path.join(dir, 'package-lock.json')),
  yarnLock: read(path.join(dir, 'yarn.lock')),
  manifests: manifestsOf(dir),
})
const copy = name => {
  const dir = path.join(tmp, name)
  if (!fs.existsSync(dir)) {
    fs.cpSync(path.join(fixtures, name), dir, { recursive: true })
    out.inputs[name] = inputs(dir)
  }
  return dir
}
const out = { inputs: {}, loadVirtual: [], shrinkwrap: [] }

const main = async () => {

  const virtual = [
    'install-types', 'install-types-sw-only', 'cli-750', 'pnpm', 'dev-deps', 'two-bundled-deps',
    'empty-with-shrinkwrap', 'empty-with-shrinkwrap-no-pj', 'old-package-lock', 'tap-and-flow',
    'edit-package-json/ok', 'edit-package-json/removed', 'edit-package-json/changed',
    'edit-package-json/workspaces-changed', 'workspaces-simple-virtual', 'workspaces-shared-deps-virtual',
    'workspaces-conflicting-versions-virtual', 'workspaces-prefer-linking-virtual',
    'workspaces-version-unsatisfied-virtual', 'workspaces-top-level-link-virtual',
    'workspaces-transitive-deps-virtual', 'workspaces-ignore-nm-virtual', 'workspaces-with-overrides',
    'test-package-with-shrinkwrap', 'tap-old-lockfile', 'tap-with-yarn-lock', 'root',
  ]
  const loadCase = async (name, dir) => {
    const c = { name, inputs: name }
    const arb = new Arborist({ path: dir })
    try {
      const tree = await arb.loadVirtual()
      c.tree = describe(tree)
      c.flagsSuspect = !!arb[Symbol.for('flagsSuspect')]
      c.lockfileVersion = tree.meta.lockfileVersion
      c.originalLockfileVersion = tree.meta.originalLockfileVersion ?? null
      c.loadedFromDisk = tree.meta.loadedFromDisk
      c.ancientLockfile = tree.meta.ancientLockfile
      c.string = clean(tree.meta.toString())
      resetDepFlags(tree)
      calcDepFlags(tree)
      const treeAfterFlags = describe(tree)
      const stringAfterFlags = clean(tree.meta.toString())
      c.treeAfterFlags = treeAfterFlags === c.tree ? null : treeAfterFlags
      c.stringAfterFlags = stringAfterFlags === c.string ? null : stringAfterFlags
      if (tree.meta.yarnLock) {
        c.yarnLockAfter = tree.meta.yarnLock.toString()
      }
    } catch (e) {
      c.error = { code: e.code, message: clean(e.message) }
    }
    out.loadVirtual.push(c)
  }
  for (const name of virtual) {
    await loadCase(name, copy(name))
  }

  // packages entries that are not objects: #loadFromShrinkwrap reads meta.link and #loadNode assigns sw.name on the raw value
  for (const [kind, entry] of [
    ['null', 'null'], ['string', '"str"'], ['true', 'true'], ['number', '5'], ['array', '["x"]'],
    ['link-without-resolved', '{"link":true}'], ['link-number-resolved', '{"link":true,"resolved":5}'],
  ]) {
    const name = 'packages-entry-' + kind
    const dir = path.join(tmp, name)
    fs.mkdirSync(dir)
    fs.writeFileSync(path.join(dir, 'package.json'), '{"name":"p"}')
    fs.writeFileSync(path.join(dir, 'package-lock.json'),
      `{"name":"p","lockfileVersion":3,"packages":{"":{"name":"p"},"node_modules/a":${entry}}}`)
    out.inputs[name] = inputs(dir)
    await loadCase(name, dir)
  }

  // a workspace member's package.json with a non-string spec: #resolveLinks swallows what the package setter
  // throws after assigning it, and the raw value is written back (npm exits 0; the next install cannot load it)
  {
    const name = 'member-spec-not-string'
    const dir = path.join(tmp, name)
    fs.mkdirSync(path.join(dir, 'packages', 'm'), { recursive: true })
    const root = { name: 'root', version: '1.0.0', workspaces: ['packages/*'], dependencies: { 'is-number': '^7.0.0' } }
    fs.writeFileSync(path.join(dir, 'package.json'), JSON.stringify(root, null, 2) + '\n')
    fs.writeFileSync(path.join(dir, 'packages', 'm', 'package.json'),
      JSON.stringify({ name: 'm', version: '1.0.0', dependencies: { 'is-odd': 5 } }, null, 2) + '\n')
    fs.writeFileSync(path.join(dir, 'package-lock.json'), JSON.stringify({
      name: 'root', version: '1.0.0', lockfileVersion: 3, requires: true,
      packages: {
        '': root,
        'node_modules/is-number': { version: '7.0.0', resolved: 'https://registry.npmjs.org/is-number/-/is-number-7.0.0.tgz',
          integrity: 'sha512-41Cifkg6e8TylSpdtTpeLVMqvSBEVzTttHvERD741+pnZ8ANv0004MRL43QKPDlK9cGvNp6NZWZUBlbGXYxxng==',
          license: 'MIT', engines: { node: '>=0.12.0' } },
        'node_modules/m': { resolved: 'packages/m', link: true },
        'packages/m': { version: '1.0.0' },
      },
    }, null, 2) + '\n')
    out.inputs[name] = inputs(dir)
    await loadCase(name, dir)
  }

  // values of another JSON type where a lock-only install reads them raw: resolved, integrity and hasShrinkwrap
  // go to the Node as they are; a link target's manifest that is not an object fails package-json's normalize
  // (#resolveLinks swallows that, mapWorkspaces does not); map-workspaces calls pattern.match on each workspaces
  // entry and the workspace Edge refuses a non-string name; the lock's top level is spread into the data; a
  // lockfileVersion array compares through its string
  const custom = async (name, lock, manifests) => {
    const dir = path.join(tmp, name)
    for (const [rel, text] of Object.entries(manifests)) {
      fs.mkdirSync(path.join(dir, rel), { recursive: true })
      fs.writeFileSync(path.join(dir, rel, 'package.json'), text)
    }
    fs.writeFileSync(path.join(dir, 'package-lock.json'), lock)
    out.inputs[name] = inputs(dir)
    await loadCase(name, dir)
  }
  const lock3 = (packages, extra = {}) => JSON.stringify({ name: 'p', version: '1.0.0', lockfileVersion: 3, requires: true, packages, ...extra }, null, 2) + '\n'
  const rootEntry = (deps, extra = {}) => ({ name: 'p', version: '1.0.0', dependencies: deps, ...extra })
  const pkg = (deps, extra = {}) => JSON.stringify({ name: 'p', version: '1.0.0', dependencies: deps, ...extra }, null, 2) + '\n'
  const a100 = { version: '1.0.0', resolved: 'https://registry.npmjs.org/a/-/a-1.0.0.tgz', integrity: 'sha512-abc' }
  for (const [kind, entry] of [
    ['resolved-number', { version: '1.0.0', resolved: 5, integrity: 'sha512-abc' }],
    ['resolved-array', { version: '1.0.0', resolved: ['x'], integrity: 'sha512-abc' }],
    ['resolved-object', { version: '1.0.0', resolved: { x: 1 }, integrity: 'sha512-abc' }],
    ['resolved-true', { version: '1.0.0', resolved: true, integrity: 'sha512-abc' }],
    ['resolved-file-array', { version: '1.0.0', resolved: ['file:x'] }],
    ['integrity-number', { ...a100, integrity: 7 }],
    ['integrity-object', { ...a100, integrity: { x: 1 } }],
    ['hasShrinkwrap-string', { ...a100, hasShrinkwrap: 'yes' }],
    ['hasShrinkwrap-number', { ...a100, hasShrinkwrap: 1 }],
  ]) {
    await custom('lock-value-' + kind, lock3({ '': rootEntry({ a: '^1.0.0' }), 'node_modules/a': entry }), { '': pkg({ a: '^1.0.0' }) })
  }
  const fileLock = lock3({ '': rootEntry({ a: 'file:packages/a' }), 'node_modules/a': { resolved: 'packages/a', link: true },
    'packages/a': { name: 'a', version: '1.0.0', dependencies: { b: '^1.0.0' } }, 'node_modules/b': a100 })
  const wsLock = lock3({ '': rootEntry({}, { workspaces: ['packages/*'] }), 'node_modules/a': { resolved: 'packages/a', link: true },
    'packages/a': { name: 'a', version: '1.0.0', dependencies: { b: '^1.0.0' } }, 'node_modules/b': a100 })
  const wsPkg = pkg({}, { workspaces: ['packages/*'] })
  for (const [kind, text] of [['string', '"abc"'], ['null', 'null'], ['number', '7'], ['false', 'false'], ['array', '[]'], ['empty', '{}']]) {
    await custom('file-dep-manifest-' + kind, fileLock, { '': pkg({ a: 'file:packages/a' }), 'packages/a': text })
    await custom('ws-member-manifest-' + kind, wsLock, { '': wsPkg, 'packages/a': text })
  }
  await custom('ws-pattern-number', lock3({ '': rootEntry({}, { workspaces: [5] }) }), { '': pkg({}, { workspaces: [5] }) })
  await custom('ws-pattern-number-lock-only', lock3({ '': rootEntry({}, { workspaces: [5] }) }), { '': pkg({}) })
  await custom('ws-pattern-null', lock3({ '': rootEntry({}, { workspaces: [null] }) }), { '': pkg({}, { workspaces: [null] }) })
  await custom('ws-pattern-object', lock3({ '': rootEntry({}, { workspaces: [{}] }) }), { '': pkg({}, { workspaces: [{}] }) })
  await custom('ws-null', lock3({ '': rootEntry({}) }), { '': pkg({}, { workspaces: null }) })
  await custom('ws-null-lock-only', lock3({ '': rootEntry({}, { workspaces: null }) }), { '': pkg({}) })
  await custom('ws-member-name-number', wsLock, { '': wsPkg, 'packages/a': '{"name":5,"version":"1.0.0"}' })
  await custom('ws-member-name-number-twice', lock3({ '': rootEntry({}, { workspaces: ['packages/*'] }),
    'node_modules/a': { resolved: 'packages/a', link: true }, 'packages/a': { name: 5, version: '1.0.0' }, 'packages/b': { name: 5, version: '1.0.0' } }),
  { '': wsPkg, 'packages/a': '{"name":5,"version":"1.0.0"}', 'packages/b': '{"name":5,"version":"1.0.0"}' })
  await custom('ws-member-name-number-lock-only', lock3({ '': rootEntry({}, { workspaces: ['packages/*'] }),
    'node_modules/a': { resolved: 'packages/a', link: true }, 'packages/a': { name: 5, version: '1.0.0' } }), { '': wsPkg })
  await custom('ws-member-name-array-lock-only', lock3({ '': rootEntry({}, { workspaces: ['packages/*'] }),
    'node_modules/a': { resolved: 'packages/a', link: true }, 'packages/a': { name: ['a'], version: '1.0.0' } }), { '': wsPkg, 'packages/a': '{"name":"a","version":"1.0.0"}' })
  for (const [kind, text] of [['string', '"abc"\n'], ['array', '[1,2]\n'], ['number', '5\n'], ['true', 'true\n'], ['null', 'null\n']]) {
    await custom('lock-text-' + kind, text, { '': pkg({}) })
  }
  for (const [kind, packages] of [['string', 'ab'], ['number', 5], ['true', true], ['array', [{ name: 'x' }]], ['array-empty', []], ['null', null]]) {
    await custom('lock-packages-' + kind, lock3(packages), { '': pkg({}) })
  }
  for (const [kind, v] of [['array-3', [3]], ['array-1', [1]], ['array-empty', []], ['object', { a: 1 }], ['string-abc', 'abc']]) {
    await custom('lock-lfv-' + kind, lock3({ '': rootEntry({ a: '^1.0.0' }), 'node_modules/a': a100 }, { lockfileVersion: v }), { '': pkg({ a: '^1.0.0' }) })
  }
  await custom('v1-deps-string', JSON.stringify({ name: 'p', version: '1.0.0', lockfileVersion: 1, requires: true, dependencies: 'abc' }, null, 2) + '\n', { '': pkg({}) })

  // test/arborist/load-virtual.js 'do not reset flags on supplied root option'
  {
    const name = 'test-package-with-shrinkwrap'
    const dir = copy(name)
    const root = new Node({ path: dir, pkg: JSON.parse(read(path.join(dir, 'package.json'))), dev: true, optional: false, peer: false, devOptional: true })
    const tree = await new Arborist({ path: dir }).loadVirtual({ root })
    out.loadVirtual.push({ name: name + ' (root option)', inputs: name, tree: describe(tree), flagsSuspect: true,
      string: clean(tree.meta.toString()) })
  }
  // 'load from root that already has shrinkwrap'
  {
    const name = 'tap-and-flow'
    const dir = copy(name)
    const meta = await Shrinkwrap.load({ path: dir })
    const root = new Node({ path: dir, pkg: JSON.parse(read(path.join(dir, 'package.json'))), meta })
    const arb = new Arborist({ path: dir })
    const tree = await arb.loadVirtual({ root })
    out.loadVirtual.push({ name: name + ' (root with meta)', inputs: name, tree: describe(tree),
      flagsSuspect: !!arb[Symbol.for('flagsSuspect')], string: clean(tree.meta.toString()) })
  }
  // 'load a tree where package.json edited': changed, then re-used as a root with meta whose loadedFromDisk is false
  {
    const name = 'edit-package-json/changed'
    const dir = copy(name)
    const changeArb = new Arborist({ path: dir })
    const root = await changeArb.loadVirtual()
    root.meta.loadedFromDisk = false
    const newArb = new Arborist({ path: dir })
    const tree = await newArb.loadVirtual({ root })
    out.loadVirtual.push({ name: name + ' (re-used root)', inputs: name, tree: describe(tree),
      flagsSuspect: !!newArb[Symbol.for('flagsSuspect')], string: clean(tree.meta.toString()) })
  }

  const shrinkwrap = [
    ['install-types'], ['install-types', 1], ['install-types', 2], ['install-types', 3],
    ['install-types-sw-only'], ['dev-deps'], ['empty'], ['testing-peer-deps-bad-sw'], ['sax'],
    ['yarn-stuff'], ['tap-with-yarn-lock'], ['old-package-lock'], ['tap-old-lockfile'], ['cli-750'],
    ['two-bundled-deps'], ['pnpm'], ['workspaces-simple-virtual'], ['edit-package-json/workspaces-changed'],
  ]
  for (const [name, lockfileVersion] of shrinkwrap) {
    const dir = copy(name)
    const c = { name, lockfileVersion: lockfileVersion ?? null, inputs: name }
    const s = await Shrinkwrap.load({ path: dir, lockfileVersion })
    c.loaded = {
      loadedFromDisk: s.loadedFromDisk,
      ancientLockfile: s.ancientLockfile ?? null,
      lockfileVersion: s.lockfileVersion,
      originalLockfileVersion: s.originalLockfileVersion ?? null,
      loadingError: s.loadingError ? s.loadingError.message : null,
      indent: s.indent,
      newline: s.newline,
      yarnEntries: s.yarnLock ? s.yarnLock.entries.size : null,
    }
    c.string = clean(s.toString())
    s.reset()
    c.reset = { data: json(s.data), lockfileVersion: s.lockfileVersion, originalLockfileVersion: s.originalLockfileVersion }
    out.shrinkwrap.push(c)
  }
  // lockfileVersion from the file contents
  for (const v of [1, 2, 3]) {
    for (const opt of [undefined, 1, 2, 3]) {
      const dir = path.join(tmp, `v${v}-opt${opt}`)
      fs.mkdirSync(dir)
      fs.writeFileSync(path.join(dir, 'package-lock.json'), JSON.stringify({ lockfileVersion: v }))
      const s = await Shrinkwrap.load({ path: dir, lockfileVersion: opt })
      const r = new Shrinkwrap({ path: dir, lockfileVersion: opt })
      r.reset()
      out.inputs[`v${v}`] = inputs(dir)
      out.shrinkwrap.push({ name: `lockfileVersion ${v} with option ${opt}`, lockfileVersion: opt ?? null, inputs: `v${v}`,
        loaded: { lockfileVersion: s.lockfileVersion, originalLockfileVersion: s.originalLockfileVersion, data: json(s.data) },
        commit: json(s.commit()), string: s.toString(),
        reset: { data: json(r.data), lockfileVersion: r.lockfileVersion, originalLockfileVersion: r.originalLockfileVersion } })
    }
  }
  // formats
  for (const [name, text] of [
    ['minified', JSON.stringify({ name: 'badsave', version: '1.0.0', lockfileVersion: 3, packages: { '': { name: 'badsave', version: '1.0.0' } } })],
    ['tabs', JSON.stringify({ name: 'p', lockfileVersion: 3, packages: { '': { name: 'p' } } }, null, '\t')],
    ['crlf', JSON.stringify({ name: 'p', lockfileVersion: 3, packages: { '': { name: 'p' } } }, null, 4).replace(/\n/g, '\r\n')],
    ['null', 'null'], ['empty-object', '{}'], ['empty-object-newline', '{}\n'], ['garbage', 'not json'], ['empty-file', ''],
    ['string-version', JSON.stringify({ lockfileVersion: '3', packages: {} })],
    ['number-version', JSON.stringify({ lockfileVersion: 2.0, packages: {} })],
    ['extra-keys', JSON.stringify({ name: 'x', lockfileVersion: 3, custom: { a: 1 }, packages: { '': { name: 'x' } } })],
  ]) {
    const dir = path.join(tmp, 'format-' + name)
    fs.mkdirSync(dir)
    fs.writeFileSync(path.join(dir, 'package-lock.json'), text)
    const s = await Shrinkwrap.load({ path: dir })
    out.inputs['format-' + name] = inputs(dir)
    const c = { name: 'format ' + name, lockfileVersion: null, inputs: 'format-' + name, loaded: {
      loadedFromDisk: s.loadedFromDisk, ancientLockfile: s.ancientLockfile ?? null, lockfileVersion: s.lockfileVersion,
      originalLockfileVersion: s.originalLockfileVersion ?? null, loadingError: s.loadingError ? s.loadingError.message : null,
      indent: s.indent, newline: s.newline, data: json(s.data),
    } }
    c.commit = json(s.commit())
    c.string = s.toString()
    c.stringUnformatted = s.toString({ format: false })
    c.stringTab = s.toString({ format: '\t' })
    out.shrinkwrap.push(c)
  }
  // lockfileVersion 1 dependency entries that are not objects: #metaFromLock skips a falsy one, #loadAll reads
  // lock.dependencies on the rest
  for (const [kind, deps] of [
    ['falsy', '{"a":"","b":false,"c":0,"d":{"version":"1.0.0"}}'],
    ['null', '{"a":null}'],
    ['nested-null', '{"a":{"version":"1.0.0","dependencies":{"b":"","c":null}}}'],
    ['string', '{"a":"abc"}'], ['number', '{"a":5}'], ['true', '{"a":true}'], ['array', '{"a":["x"]}'],
    ['nested-falsy', '{"a":{"version":"1.0.0","requires":{"b":"1.0.0"},"dependencies":{"b":0,"e":{"version":"2.0.0"}}}}'],
  ]) {
    const name = 'v1-entry-' + kind
    const dir = path.join(tmp, name)
    fs.mkdirSync(dir)
    fs.writeFileSync(path.join(dir, 'package.json'), '{"name":"p","dependencies":{"a":"","d":""}}')
    fs.writeFileSync(path.join(dir, 'package-lock.json'),
      `{"name":"p","lockfileVersion":1,"requires":true,"dependencies":${deps}}`)
    out.inputs[name] = inputs(dir)
    const c = { name: 'v1 entry ' + kind, lockfileVersion: null, inputs: name }
    try {
      const s = await Shrinkwrap.load({ path: dir })
      c.loaded = { lockfileVersion: s.lockfileVersion, originalLockfileVersion: s.originalLockfileVersion ?? null, data: json(s.data) }
      c.string = s.toString()
    } catch (e) {
      c.error = { code: e.code, message: e.message }
    }
    out.shrinkwrap.push(c)
  }
  // a v1 lock whose dependencies is a string loads its characters as entries; deleting one is a strict-mode TypeError
  {
    const name = 'v1-deps-string'
    const dir = path.join(tmp, name)
    const s = await Shrinkwrap.load({ path: dir })
    const c = { name: 'v1 dependencies string, delete', lockfileVersion: null, inputs: name, delete: 'node_modules/0',
      loaded: { lockfileVersion: s.lockfileVersion, originalLockfileVersion: s.originalLockfileVersion ?? null, data: json(s.data) } }
    try {
      s.delete(path.join(dir, 'node_modules/0'))
      c.string = s.toString()
    } catch (e) {
      c.error = { code: e.code, message: e.message }
    }
    out.shrinkwrap.push(c)
  }

  fs.rmSync(tmp, { recursive: true })
  process.stdout.write(zlib.gzipSync(JSON.stringify(out, null, 1), { level: 9 }))
}
main()
