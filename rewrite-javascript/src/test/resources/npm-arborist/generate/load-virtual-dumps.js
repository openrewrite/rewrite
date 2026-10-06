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
  for (const name of virtual) {
    const dir = copy(name)
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

  fs.rmSync(tmp, { recursive: true })
  process.stdout.write(zlib.gzipSync(JSON.stringify(out, null, 1), { level: 9 }))
}
main()
