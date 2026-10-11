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
// Expectations for ShrinkwrapTest, from the arborist npm bundles:
// - "construct": test/shrinkwrap.js "construct metadata from node and package data", with the flags
//   calc-dep-flags gave each node and every meta.get the test snapshots, in order.
// - "yarn": test/shrinkwrap.js's "a yarn.lock entry with ..." cases, whose trees upstream reads from disk,
//   built here as nodes: what each node was decorated with, the lock entries, and the yarn.lock rewritten
//   from the tree.
// Usage: node shrinkwrap-dumps.js > ../shrinkwrap-dumps
process.chdir('/')
const npm = require('./npm-modules.js')
const Node = npm('@npmcli/arborist/lib/node.js')
const Link = npm('@npmcli/arborist/lib/link.js')
const Shrinkwrap = npm('@npmcli/arborist/lib/shrinkwrap.js')
const YarnLock = npm('@npmcli/arborist/lib/yarn-lock.js')
const calcDepFlags = npm('@npmcli/arborist/lib/calc-dep-flags.js')

const json = v => JSON.stringify(npm.sortKeys(v))
const out = {}

{
  const meta = new Shrinkwrap({ path: '/srv/user/projects/root' })
  meta.data = { lockfileVersion: 2, requires: true, dependencies: {}, packages: {} }
  const root = new Node({
    pkg: {
      name: 'root',
      dependencies: { a: '', link: '', link2: '', link3: '' },
      devDependencies: { d: '', e: 'https://foo.com/e.tgz', devit: '' },
      optionalDependencies: { optin: '' },
      peerDependencies: { peer: '' },
      scripts: { install: 'true' },
    },
    path: '/srv/user/projects/root',
    realpath: '/srv/user/projects/root',
    meta,
  })
  const peer = new Node({
    pkg: { name: 'peer', version: '1.2.3', dependencies: { peerdep: '' } },
    resolved: 'https://peer.com/peer.tgz',
    integrity: 'sha512-peerpeerpeer',
    parent: root,
  })
  const peerdep = new Node({
    pkg: { name: 'peerdep', version: '1.2.3' },
    resolved: 'https://peer.com/peerdep.tgz',
    integrity: 'sha512-peerdeppeerdep',
    parent: peer,
  })
  const e = new Node({
    pkg: {
      name: 'e',
      version: '1.2.3',
      dependencies: { tgz: '', 'tgz-pkg-resolved': '' },
      license: { type: 'blurb', url: 'http://example.com/' },
    },
    resolved: 'https://foo.com/e.tgz',
    parent: root,
  })
  const tgz = new Node({
    pkg: {
      name: 'tgz',
      version: '1.2.3',
      funding: { url: 'https://example.com/' },
      engines: { node: '>=10' },
      os: ['any', '!win32'],
      cpu: ['x64'],
      libc: ['glibc'],
    },
    resolved: '/srv/user/projects/root/archives/tarball.tgz',
    parent: root,
  })
  const tgzPkgResolved = new Node({
    pkg: {
      name: 'tgz-pkg-resolved',
      version: '1.2.3',
      funding: { url: 'https://example.com/' },
      _resolved: '/srv/user/projects/root/archives/tarball-pkg-resolved.tgz',
      _integrity: 'sha512-tarball/package/resolved/integrity',
    },
    parent: root,
  })
  const linkPkg = name => ({
    name,
    version: '1.2.3',
    funding: 'https://example.com/payme',
    _resolved: 'github:isaacs/foobarbaz#aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa',
  })
  const link = new Link({
    path: '/srv/user/projects/root/node_modules/link',
    realpath: '/srv/user/projects/root/target',
    parent: root,
    pkg: linkPkg('link'),
    target: new Node({
      realpath: '/srv/user/projects/root/target',
      path: '/srv/user/projects/root/target',
      root,
      pkg: linkPkg('link'),
    }),
  })
  const link3 = new Link({
    name: 'link3',
    path: '/srv/user/projects/root/node_modules/link3',
    realpath: '/srv/user/projects/root/realPkg',
    parent: root,
    pkg: linkPkg('link3'),
    target: new Node({
      name: 'link3',
      realpath: '/srv/user/projects/root/realPkg',
      path: '/srv/user/projects/root/realPkg',
      root,
      pkg: linkPkg('link3'),
    }),
  })
  const a = new Node({
    resolved: 'https://example.com/a.tgz',
    integrity: 'sha512-helloyesthisisdog',
    pkg: { name: 'a', version: '1.2.3' },
    parent: root,
  })
  const optin = new Node({ pkg: { name: 'optin', version: '1.2.3', dependencies: { devo: '' } }, parent: root })
  const devit = new Node({ pkg: { name: 'devit', version: '1.2.3', dependencies: { devo: '' } }, parent: root })
  const devo = new Node({ pkg: { name: 'devo', version: '1.2.3' }, parent: root })
  const d = new Node({
    pkg: {
      name: 'd',
      version: '1.2.3',
      _shasum: 'aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa',
      bundleDependencies: ['bundled'],
      dependencies: { e: '1.2.3', bundled: 'npm:not-bundled-tho@*', 'git-dep': 'github:foo/bar' },
    },
    parent: root,
  })
  const bundled = new Node({
    pkg: { name: 'not-bundled-tho', version: '1.2.3' },
    path: d.path + '/node_modules/bundled',
    name: 'bundled',
    parent: d,
  })
  const git = new Node({
    pkg: { name: 'git-dep', version: '1.2.3' },
    parent: d,
    resolved: 'git+ssh://git@github.com/foo/bar.git#0000000000000000000000000000000000000000',
  })
  const nopkg = new Node({ path: root.path + '/node_modules/nopkg', parent: root })

  calcDepFlags(root)
  const flags = {}
  for (const n of root.inventory.values()) {
    flags[n.path] = [n.dev, n.optional, n.devOptional, n.peer, n.extraneous].map(Number).join('')
  }

  const gets = []
  const get = (label, key) => gets.push([label, key, json(meta.get(key))])
  get('root metadata, no package version', '')
  root.package.version = '1.2.3'
  meta.add(root)
  get('root metadata, with package version', '')
  get('meta for optional dep', optin.location)
  get('meta for dev dep', devit.location)
  get('meta for devOptional dep', devo.location)
  get('metadata for tarball file pkg', tgz.location)
  get('metadata for tarball file pkg with _resolved value', tgzPkgResolved.location)
  get('link metadata', link.path)
  get('link target metadata', link.target.path)
  get('link metadata with same pkg name as link target', link3.path)
  get('link target metadata with same pkg name as link target', link3.target.path)
  get('dep a metadata', a.location)
  get('dep d metadata', d.location)
  get('dep e metadata', e.location)
  get('bundled pkg metadata', bundled.location)
  get('git dep metadata', git.location)
  get('node without a package', nopkg.location)
  get('a peer dep', peer.location)
  get('a peer meta-dep', peerdep.location)
  out.construct = { flags, gets }
}

const header = '\n# THIS IS AN AUTOGENERATED FILE. DO NOT EDIT THIS FILE DIRECTLY.\n# yarn lockfile v1\n'
const mkdirp102 = 'https://registry.yarnpkg.com/mkdirp/-/mkdirp-1.0.2.tgz#5ccd93437619ca7050b538573fc918327eba98fb'
const sha = 'sha512-N2REVrJ/X/jGPfit2d7zea2J1pf7EAR5chIUcfHffAZ7gmlam5U65sAm76+o4ntQbSRdTjYf7qZz3chuHlwXEA=='
const scenarios = [
  {
    name: 'a yarn.lock entry with version mismatch',
    yarn: `${header}\nmkdirp@^1.0.2:\n  version "1.0.2"\n  resolved "${mkdirp102}"\n  integrity ${sha}\n`,
    spec: '^1.0.2',
    pkg: { name: 'mkdirp', version: '1.0.4', _resolved: 'https://registry.npmjs.org/mkdirp/-/mkdirp-1.0.4.tgz' },
  },
  {
    name: 'a yarn.lock entry with integrity mismatch',
    yarn: `${header}\nmkdirp@^1.0.2:\n  version "1.0.2"\n  resolved "${mkdirp102}"\n  integrity "${sha}"\n`,
    spec: '^1.0.2',
    pkg: {
      name: 'mkdirp',
      version: '1.0.2',
      _resolved: 'https://registry.npmjs.org/mkdirp/-/mkdirp-1.0.2.tgz',
      _integrity: 'um, nope, not even close buddy',
    },
  },
  {
    name: 'a yarn.lock entry with no resolved',
    yarn: `${header}\nmkdirp@^1.0.2:\n  version "1.0.2"\n  integrity "${sha}"\n`,
    spec: '^1.0.2',
    pkg: { name: 'mkdirp', version: '1.0.2' },
  },
  {
    name: 'a yarn.lock entry with file: resolved',
    yarn: `${header}\nmkdirp@file:mkdirp:\n  version "1.0.2"\n  integrity "${sha}"\n`,
    spec: 'file:mkdirp',
    pkg: { name: 'mkdirp', version: '1.0.2' },
    link: true,
  },
  {
    name: 'a yarn.lock entry with no integrity',
    yarn: `${header}\nmkdirp@^1.0.2:\n  version "1.0.2"\n  resolved "${mkdirp102}"\n`,
    spec: '^1.0.2',
    pkg: { name: 'mkdirp', version: '1.0.2' },
  },
  {
    name: 'a yarn.lock with no entries',
    yarn: header,
    spec: '^1.0.2',
    pkg: { name: 'mkdirp', version: '1.0.4', _resolved: 'https://registry.npmjs.org/mkdirp/-/mkdirp-1.0.4.tgz' },
  },
  {
    name: 'a yarn.lock entry that matches',
    yarn: `${header}\nmkdirp@^1.0.2:\n  version "1.0.2"\n  resolved "${mkdirp102}"\n  integrity ${sha}\n`,
    spec: '^1.0.2',
    pkg: { name: 'mkdirp', version: '1.0.2' },
  },
]
out.yarn = scenarios.map(s => {
  const path = '/project'
  const meta = new Shrinkwrap({ path })
  meta.data = { lockfileVersion: 3, requires: true, packages: {}, dependencies: {} }
  meta.yarnLock = new YarnLock()
  meta.yarnLock.parse(s.yarn)
  const root = new Node({
    path,
    realpath: path,
    meta,
    pkg: { name: 'root', version: '1.0.0', dependencies: { mkdirp: s.spec } },
  })
  if (s.link) {
    new Link({
      name: 'mkdirp',
      parent: root,
      realpath: path + '/mkdirp',
      target: new Node({ path: path + '/mkdirp', root, pkg: JSON.parse(JSON.stringify(s.pkg)) }),
    })
  } else {
    new Node({ name: 'mkdirp', parent: root, pkg: JSON.parse(JSON.stringify(s.pkg)) })
  }
  const nodes = [...root.inventory.values()].map(n => [n.location, n.resolved, n.integrity])
  const gets = [...root.inventory.keys()].map(location => [location, json(meta.get(location))])
  return { ...s, link: !!s.link, nodes, gets, rewritten: meta.yarnLock.fromTree(root).toString() }
})

console.log(JSON.stringify(out, null, 1))
