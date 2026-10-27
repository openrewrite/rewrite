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
// Replays the arborist test sequences (test/node.js, test/link.js, test/calc-dep-flags.js) whose snapshots print whole trees, on
// the arborist npm bundles, and prints a structural dump at each snapshot point. NpmTreeSupport.describe
// renders the Java tree in the same format. Where a sequence attaches lock metadata, the raw lock data
// (" / data") and what the metadata answers (" / get") are dumped too.
// Usage: node tree-dumps.js > ../tree-dumps
process.chdir('/')
const npm = require('./npm-modules.js')
const Node = npm('@npmcli/arborist/lib/node.js')
const Link = npm('@npmcli/arborist/lib/link.js')
const OverrideSet = npm('@npmcli/arborist/lib/override-set.js')
const Shrinkwrap = npm('@npmcli/arborist/lib/shrinkwrap.js')
const calcDepFlags = npm('@npmcli/arborist/lib/calc-dep-flags.js')

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
  return out.join('\n')
}
const dumps = {}
const dump = (name, root) => {
  dumps[name] = describe(root)
  if (root.meta) {
    dumps[name + ' / data'] = JSON.stringify(npm.sortKeys(root.meta.data))
  }
}
const get = (name, meta, location) => {
  dumps[name + ' / get'] = JSON.stringify(npm.sortKeys(meta.get(location)))
}
const emptyLock = path => {
  const meta = new Shrinkwrap({ path })
  meta.data = { lockfileVersion: 2, packages: {}, dependencies: {} }
  return meta
}

// test/node.js: testing with dep tree
const depTree = (prefix, rootMetadata) => {
  const rootPkg = () => ({
    name: 'root',
    bundleDependencies: ['bundled'],
    dependencies: { prod: '1.x', bundled: '', missing: '' },
    devDependencies: { dev: '', overlap: '' },
    optionalDependencies: { optional: '', overlap: '', optMissing: '' },
  })
  const root = new Node({
    pkg: rootPkg(),
    realpath: '/srv/user/projects/root',
    path: '/srv/user/projects/root',
    meta: rootMetadata,
    children: [{
      pkg: { name: 'prod', version: '1.2.3', dependencies: { meta: '' }, peerDependencies: { peer: '' } },
      fsChildren: [{
        realpath: '/srv/user/projects/root/node_modules/prod/foo',
        path: '/srv/user/projects/root/node_modules/prod/foo',
        name: 'foo',
        pkg: { name: 'foo', version: '1.2.3', dependencies: { meta: '' } },
      }],
      resolved: 'prod',
      integrity: 'prod',
    }],
  })
  const prod = root.children.get('prod')
  const meta = new Node({
    pkg: {
      name: 'meta', version: '1.2.3', devDependencies: { missing: '' }, dependencies: { bundled: '' },
      _resolved: 'meta', _integrity: 'meta',
    },
    path: '/srv/user/projects/root/node_modules/prod/node_modules/meta',
    realpath: '/srv/user/projects/root/node_modules/prod/node_modules/meta',
    parent: prod,
  })
  for (const [name, resolved] of [['bundled', 'bundled'], ['dev', 'dev'], ['optional', 'opt'], ['peer', 'peer'], ['extraneous', 'extraneous']]) {
    new Node({
      pkg: { name, version: '1.2.3', ...(name === 'bundled' ? { dependencies: { meta: '' } } : {}) },
      resolved,
      integrity: resolved,
      path: '/srv/user/projects/root/node_modules/' + name,
      realpath: '/srv/user/projects/root/node_modules/' + name,
      parent: root,
    })
  }
  dump(prefix + 'initial load with some deps', root)
  meta.parent = root
  dump(prefix + 'move meta to top level, update stuff', root)
  const newMeta = new Node({
    pkg: {
      name: 'meta', version: '2.3.4', peerDependencies: { asdf: '' }, peerDependenciesMeta: { asdf: { optional: true } },
    },
    resolved: 'newMeta',
    integrity: 'newMeta',
    name: 'meta',
    parent: prod,
  })
  newMeta.root = prod
  const metaMeta = new Link({
    pkg: { name: 'metameta', version: '1.2.3', _resolved: 'metameta', _integrity: 'metameta' },
    path: newMeta.path + '/node_modules/metameta',
    realpath: meta.realpath,
    target: meta,
  })
  metaMeta.parent = newMeta
  dump(prefix + 'add new meta under prod', root)
  newMeta.parent = root
  dump(prefix + 'move new meta to top level', root)
  newMeta.parent = root
  dump(prefix + 'move new meta to top level second time (no-op)', root)
  if (!rootMetadata) {
    return
  }

  // the replacement tests, which have no snapshots upstream
  const newProd = new Node({
    name: 'prod',
    pkg: { name: 'prod', version: '1.2.3', dependencies: { meta: '' }, peerDependencies: { peer: '' } },
    path: '/some/path',
    resolved: 'prod',
    integrity: 'prod',
  })
  newProd.canReplace(prod)
  newProd.replace(prod)
  const notProd = new Node({ pkg: { name: 'notprod', version: '1.2.3' }, path: '/some/path' })
  notProd.canReplace(newProd)
  const prodV2 = new Node({ name: 'prod', path: '/path/to/prod', pkg: { name: 'prod', version: '2.3.4' } })
  newProd.canReplaceWith(prodV2)
  const root2 = new Node({
    pkg: rootPkg(),
    realpath: '/srv/user/projects/root',
    path: '/srv/user/projects/root',
    meta: rootMetadata,
  })
  root.replaceWith(root2)
  root.replace(root2)
  dump(prefix + 'roots replaced', root)
  const prodLink = new Link({
    pkg: prod.package,
    realpath: '/some/other/path/entirely',
    path: '/not/where/it/really/is',
    name: 'prod',
  })
  prodLink.canReplace(newProd)
  prodLink.replace(newProd)
  dump(prefix + 'link replaced node', root)
  newProd.canReplace(prodLink)
  newProd.replace(prodLink)
  dump(prefix + 'node replaced link', root)
  for (const location of root.inventory.keys()) {
    get(prefix + location, rootMetadata, location)
  }
  dumps[prefix + 'final / data'] = JSON.stringify(npm.sortKeys(rootMetadata.data))
}
depTree('', undefined)
depTree('with meta: ', emptyLock('/srv/user/projects/root'))

// test/node.js: basic instantiation
{
  const root = new Node({
    pkg: { name: 'root' },
    path: '/srv/user/projects/root',
    realpath: '/srv/user/projects/root',
    overrides: new OverrideSet({ overrides: { foo: '1' } }),
  })
  dump('just a lone root node', root)
}

// test/node.js: changing root
{
  const root = new Node({
    pkg: { name: 'root', dependencies: { a: '', link: '', link2: '' } },
    path: '/srv/user/projects/root',
    realpath: '/srv/user/projects/root',
    meta: emptyLock('/srv/user/projects/root'),
  })
  const a = new Node({
    pkg: { name: 'a', version: '1.2.3' },
    parent: root,
    name: 'a',
    resolved: 'https://example.com/a-1.2.3.tgz',
    integrity: 'sha512-asdfasdfasdf',
  })
  new Node({ pkg: { name: 'b', version: '1.2.3' }, parent: a, name: 'b' })
  const root2 = new Node({
    pkg: { name: 'root2', dependencies: { a: '', link: '', link2: '' } },
    path: '/srv/user/projects/root2',
    realpath: '/srv/user/projects/root2',
    meta: emptyLock('/srv/user/projects/root2'),
  })
  dump('changing root: before, root', root)
  a.parent = root2
  dump('changing root: after, root', root)
  dump('changing root: after, root2', root2)
  for (const location of root2.inventory.keys()) {
    get('changing root: root2 ' + location, root2.meta, location)
  }
  get('changing root: root node_modules/a', root.meta, 'node_modules/a')
}

// test/node.js: update metadata when moving between linked top-of-tree parents
{
  const root = new Node({
    pkg: { name: 'root' },
    path: '/srv/user/projects/root',
    realpath: '/srv/user/projects/root',
    meta: emptyLock('/srv/user/projects/root'),
  })
  const top1 = new Node({
    pkg: { name: 'top', version: '1.1.1' },
    path: '/path/to/top1',
    realpath: '/path/to/top1',
    meta: emptyLock('/path/to/top1'),
  })
  new Link({ name: 'link1', parent: root, realpath: top1.path, target: top1 })
  const top2 = new Node({
    pkg: { name: 'top', version: '1.1.1' },
    path: '/path/to/top2',
    realpath: '/path/to/top2',
    meta: emptyLock('/path/to/top2'),
  })
  const link2 = new Link({ name: 'link2', parent: root, realpath: top2.path, target: top2 })
  const child = new Node({
    parent: top1,
    pkg: { name: 'child', version: '1.2.3', dependencies: { child2: '2' } },
    resolved: 'https://child.com/-/child-1.2.3.tgz',
    integrity: 'sha512-blortzeyblartzeyfartz',
  })
  const child2 = new Node({
    parent: child,
    pkg: { name: 'child2', version: '2.3.4' },
    resolved: 'https://child.com/-/child-2.3.4.tgz',
    integrity: 'sha512-a childs child is a kidkid',
  })
  get('moving: metadata from root', root.meta, child.location)
  get('moving: metadata from top1', top1.meta, child.location)
  const oldLocation = child.location
  const oldLocation2 = child2.location
  child.parent = link2
  get('moving: root metadata updated 1', root.meta, child.location)
  get('moving: root metadata updated 2', root.meta, child2.location)
  get('moving: old location deleted from root', root.meta, oldLocation)
  get('moving: old location deleted from top1', top1.meta, oldLocation)
  get('moving: old location2 deleted from root', root.meta, oldLocation2)
  get('moving: old location2 deleted from top1', top1.meta, oldLocation2)
  get('moving: new top metadata updated 1', top2.meta, child.location)
  get('moving: new top metadata updated 2', top2.meta, child2.location)
  dump('moving: final', root)
}

// test/node.js: set workspaces
{
  const root = new Node({
    pkg: { name: 'workspaces_root' },
    path: '/srv/user/projects/workspaces_root',
    realpath: '/srv/user/projects/workspaces_root',
  })
  new Link({
    parent: root,
    target: new Node({ path: '/srv/user/projects/workspaces_root/foo', pkg: { name: 'foo', version: '1.2.3' } }),
  })
  new Link({
    parent: root,
    target: new Node({ path: '/srv/user/projects/workspaces_root/unknown', pkg: { name: 'unknown', version: '1.2.3' } }),
  })
  root.workspaces = new Map([
    ['foo', '/srv/user/projects/workspaces_root/foo'],
    ['bar', '/srv/user/projects/workspaces_root/bar'],
  ])
  dump('should setup edges out for each workspace', root)
}

// test/link.js
{
  const meta = emptyLock('/srv/user/projects/some/kind/of/path')
  const root = new Node({ pkg: { name: 'root' }, path: '/srv/user/projects/some/kind/of/path', meta })
  const l1 = new Link({
    pkg: { name: 'root' },
    path: '/srv/user/some/other/path',
    realpath: '/srv/user/projects/some/kind/of/path',
    meta,
  })
  dump('instantiate without providing target', l1)
  dumps['instantiate without providing target / tree'] = String(meta.tree === l1.target)
  const l2 = new Link({ path: '/srv/user/some/other/path', target: root })
  dump('instantiate with target specified', l2.root)
}

// test/calc-dep-flags.js
{
  const root = new Node({
    path: '/x',
    realpath: '/x',
    pkg: {
      dependencies: { prod: '' },
      devDependencies: { dev: '' },
      optionalDependencies: { optional: '' },
      peerDependencies: { peer: '', peeroptional: '' },
      peerDependenciesMeta: { peeroptional: { optional: true } },
    },
  })
  new Node({ pkg: { name: 'optional', version: '1.2.3', dependencies: { devoptional: '', missing: '' } }, parent: root })
  new Node({ pkg: { name: 'devoptional', version: '1.2.3' }, parent: root })
  new Node({ pkg: { name: 'extraneous' }, parent: root })
  new Node({ pkg: { name: 'peer', version: '1.2.3', dependencies: { peerdep: '' } }, parent: root })
  new Node({ pkg: { name: 'peerdep', version: '1.2.3' }, parent: root })
  new Node({ pkg: { name: 'prod', version: '1.2.3', dependencies: { proddep: '' }, peerDependencies: { metapeer: '' } }, parent: root })
  new Node({ pkg: { name: 'metapeer', version: '1.2.3', dependencies: { metapeerdep: '' } }, parent: root })
  new Node({ pkg: { name: 'metapeerdep', version: '1.2.3' }, parent: root })
  new Node({ pkg: { name: 'proddep', version: '1.2.3', dependencies: { proddep: '' } }, parent: root })
  new Node({ pkg: { name: 'dev', version: '1.2.3', dependencies: { devdep: '' } }, parent: root })
  const devdep = new Node({
    pkg: {
      name: 'devdep', version: '1.2.3',
      dependencies: { proddep: '', linky: '', devoptional: '' },
      optionalDependencies: { devandoptional: '' },
    },
    parent: root,
  })
  new Node({ pkg: { name: 'devandoptional', version: '1.2.3' }, parent: root })
  const linky = new Link({ pkg: { name: 'linky', version: '1.2.3', dependencies: { linklink: '' } }, realpath: '/x/y/z', parent: devdep })
  new Link({ pkg: { name: 'linklink', version: '1.2.3' }, realpath: '/l/i/n/k/link', parent: linky.target })
  new Node({ pkg: { name: 'peeroptional', version: '1.2.3', dependencies: { optional: '' } }, parent: root })
  calcDepFlags(root)
  dump('flag stuff', root)
}
{
  const root = new Node({ path: '/some/path', realpath: '/some/path', pkg: { dependencies: { foo: '' } } })
  new Node({ parent: root, pkg: { name: 'foo', version: '1.2.3' } })
  root.optional = false
  root.dev = true
  root.extraneous = false
  calcDepFlags(root, false)
  dump('no reset', root)
}
{
  const { resolve } = require('path')
  const root = new Node({
    path: '/some/path',
    realpath: '/some/path',
    pkg: { dependencies: { baz: 'file:node_modules/asdf/node_modules/baz', foo: 'file:bar/foo' } },
  })
  const bar = new Node({ root, path: resolve(root.path, 'bar') })
  const foo = new Node({ root, path: resolve(bar.path, 'foo'), pkg: { name: 'foo', version: '1.2.3' } })
  const asdf = new Node({ parent: root, pkg: { name: 'asdf', version: '1.2.3' } })
  const baz = new Node({ parent: asdf, pkg: { name: 'baz', version: '1.2.3' } })
  new Link({ name: 'foo', target: foo, parent: root, realpath: foo.path })
  new Link({ name: 'baz', target: baz, parent: root, realpath: baz.path })
  calcDepFlags(root, true)
  dump('set parents to not extraneous when visiting', root)
}

console.log(JSON.stringify(dumps, null, 1))
