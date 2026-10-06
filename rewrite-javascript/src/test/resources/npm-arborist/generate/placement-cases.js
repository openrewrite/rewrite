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
// Runs arborist's own test/can-place-dep.js or test/place-dep.js, unchanged but for a hook that records each
// runTest call, on the arborist npm bundles, and writes for every case: the trees and peer sets as the test
// declares them, the order tap sets up and runs the cases in (an ERESOLVE case sets up its force and
// legacyPeerDeps variants on the same tree before either runs), and what arborist answered (CanPlaceDep's
// results; PlaceDep's tree before and after, placements, warnings, needEvaluation, or the ERESOLVE it threw).
// Every case is then replayed from its declarations alone, with the harness the Java tests port, and must give
// the same answers. The one case that marks a node vulnerable through an audit report is replayed with the
// edge's name in updateNames instead, which reaches the same placement. With a fourth argument, the directory
// of an installed tap 16 (16.3.10 runs arborist's tests), the file runs under that tap rather than the stand-in
// here, and writes the same output. The output is gzipped JSON.
// Usage: node placement-cases.js <npm/cli checkout>/workspaces/arborist <can-place-dep|place-dep> <out.gz> [<tap dir>]
'use strict'
process.env.ARBORIST_DEBUG = '1'
const fs = require('fs')
const path = require('path')
const zlib = require('zlib')
const Module = require('module')
const [checkout, testName, out, tapDir] = process.argv.slice(2).map((a, i) => i === 1 ? a : a && path.resolve(a))
process.chdir('/')
const npm = require('./npm-modules.js')
const lib = path.join(npm.dir, '@npmcli/arborist/lib')
const Node = require(path.join(lib, 'node.js'))
const Link = require(path.join(lib, 'link.js'))
const OverrideSet = require(path.join(lib, 'override-set.js'))
const PlaceDep = require(path.join(lib, 'place-dep.js'))
const CanPlaceDep = require(path.join(lib, 'can-place-dep.js'))

// the tree as NpmTreeSupport.describe prints it, then every edge marked peerConflicted
const loc = n => n ? (n.location === undefined ? '?' : JSON.stringify(n.location)) : '-'
const describe = root => {
  const lines = []
  const conflicted = []
  for (const [l, n] of root.inventory) {
    lines.push(`${JSON.stringify(l)} ${n.constructor.name} name=${n.name} pkg=${n.packageName}@${n.version} ` +
      `path=${n.path} realpath=${n.realpath} parent=${loc(n.parent)} fsParent=${loc(n.fsParent)} ` +
      `target=${loc(n.target)} root=${loc(n.root)} ` +
      `flags=${[n.dev, n.optional, n.devOptional, n.peer, n.extraneous].map(Number).join('')} ` +
      `resolved=${n.resolved} integrity=${n.integrity}`)
    for (const [k, c] of n.children) {
      lines.push(`  child ${k} ${loc(c)}`)
    }
    for (const c of n.fsChildren) {
      lines.push(`  fsChild ${loc(c)}`)
    }
    for (const c of n.linksIn) {
      lines.push(`  linkIn ${loc(c)}`)
    }
    for (const [k, e] of n.edgesOut) {
      lines.push(`  out ${k} ${e.type} ${e.spec} to=${loc(e.to)} error=${e.error}`)
      if (e.peerConflicted) {
        conflicted.push(`peerConflicted ${loc(n)} ${k}`)
      }
    }
    for (const e of n.edgesIn) {
      lines.push(`  in ${loc(e.from)} ${e.type} ${e.name}`)
    }
  }
  for (const t of root.tops) {
    lines.push(`top ${loc(t)}`)
  }
  return lines.concat(conflicted).join('\n')
}

// JSON for comparing with what the port answers: Symbols by description, Maps as entry lists, errors by message,
// object members that are null or undefined left out, keys sorted
const plain = v => {
  if (v === undefined || v === null) {
    return null
  }
  if (typeof v === 'symbol') {
    return v.description
  }
  if (typeof v !== 'object') {
    return v
  }
  if (v instanceof Error) {
    return { message: v.message }
  }
  if (v instanceof Map) {
    return [...v].map(([k, x]) => [plain(k), plain(x)])
  }
  if (v instanceof Set || Array.isArray(v)) {
    return [...v].map(plain)
  }
  const o = {}
  for (const k of Object.keys(v).sort()) {
    if (v[k] !== undefined && v[k] !== null) {
      o[k] = plain(v[k])
    }
  }
  return o
}

const declarations = { nodes: [], overrides: [] }
const nodeIds = new Map()
const overrideIds = new Map()
const clones = new Map()
const declare = v => {
  if (v === null || typeof v !== 'object') {
    if (typeof v === 'function' || typeof v === 'symbol') {
      throw new Error(`cannot declare ${String(v)}`)
    }
    return v
  }
  if (nodeIds.has(v)) {
    return { $node: nodeIds.get(v) }
  }
  if (overrideIds.has(v)) {
    return { $overrides: overrideIds.get(v) }
  }
  if (clones.has(v)) {
    return clones.get(v)
  }
  let c
  if (v instanceof Error) {
    c = { $error: v.message }
  } else if (Array.isArray(v)) {
    c = v.map(declare)
  } else if (Object.getPrototypeOf(v) === Object.prototype) {
    c = {}
    for (const [k, x] of Object.entries(v)) {
      if (x !== undefined) {
        c[k] = declare(x)
      }
    }
  } else {
    throw new Error(`cannot declare a ${v.constructor.name}`)
  }
  clones.set(v, c)
  return c
}

const declaring = (Real, kind) => new Proxy(Real, {
  construct (target, args) {
    const options = args[0] || {}
    // the virtual root runTest builds is rebuilt by the replay
    const decl = options.sourceReference ? null : { class: kind, options: declare(options) }
    const node = Reflect.construct(target, args)
    if (decl) {
      nodeIds.set(node, declarations.nodes.length)
      declarations.nodes.push(decl)
    }
    return node
  },
})

let running = null
const answering = (Real, key) => new Proxy(Real, {
  construct (target, args) {
    try {
      const answer = Reflect.construct(target, args)
      if (running && !running[key]) {
        running[key] = answer
      }
      return answer
    } catch (er) {
      if (running) {
        running.thrown = plain(Object.assign({ message: er.message }, er))
      }
      throw er
    }
  },
})

const cases = []
const events = []
const answers = []
const trees = []
const seen = new Map()
let pending

const caseHook = (desc, options) => {
  if (running && running.after === undefined) {
    running.after = describe(running.tree)
  }
  const count = (seen.get(desc) || 0) + 1
  seen.set(desc, count)
  const c = { name: count > 1 ? `${desc} (${count})` : desc }
  for (const [k, v] of Object.entries(options)) {
    if (k === 'tree' || k === 'dep') {
      if (!nodeIds.has(v)) {
        throw new Error(`${desc}: ${k} was not declared in the test`)
      }
      c[k] = nodeIds.get(v)
    } else if (k === 'test' || k === 'auditReport') {
      c[k] = !!v
    } else if (typeof v === 'symbol') {
      c[k] = v.description
    } else {
      c[k] = declare(v)
    }
  }
  trees[cases.length] = options.tree
  pending = cases.length
  events.push({ setup: cases.length })
  cases.push(c)
}

const placements = pd => plain([pd, ...pd.allChildren].map(c => ({
  ...(c.parent ? { parent: c.parent.name } : {}),
  edge: `{ ${c.edge.from.location || 'ROOT'} ${c.edge.type} ${c.edge.name}@${c.edge.spec} }`,
  dep: `${c.dep.name}@${c.dep.version}`,
  canPlace: c.canPlace && c.canPlace.canPlace,
  canPlaceSelf: c.canPlaceSelf && c.canPlaceSelf.canPlaceSelf,
  placed: c.placed && c.placed.location,
  checks: new Map([...pd.checks].map(([target, cpd]) => [target.location, [cpd.canPlace, cpd.canPlaceSelf]])),
})))

const conflictChildren = cpd => plain([...cpd.conflictChildren].map(c => ({
  dep: [c.dep.name, c.dep.version],
  edge: [c.edge.from.location, c.edge.type, c.edge.name, c.edge.spec],
  canPlace: c.canPlace,
  canPlaceSelf: c.canPlaceSelf,
})))

// what one run answered; the tree is described before PlaceDep runs, as the test's tree.toJSON() reads every
// edge's error there, and after CanPlaceDep runs, as that test reads none
const answer = rec => {
  if (testName === 'can-place-dep') {
    const cpd = rec.canPlaceDep
    return {
      tree: describe(rec.tree),
      canPlace: cpd.canPlace.description,
      canPlaceSelf: cpd.canPlaceSelf && cpd.canPlaceSelf.description,
      description: cpd.description,
      conflictChildren: conflictChildren(cpd),
    }
  }
  const a = { before: rec.before, after: rec.after, warnings: rec.warnings }
  if (rec.thrown) {
    a.thrown = rec.thrown
  }
  const pd = rec.placeDep
  if (pd) {
    a.placements = placements(pd)
    a.needEvaluation = [...pd.needEvaluation].map(n => `${n.location} ${n.version}`)
  }
  return a
}

const recording = fn => {
  const index = pending
  pending = undefined
  if (index === undefined) {
    return fn
  }
  return function (t) {
    events.push({ run: index })
    const rec = running = { tree: trees[index], warnings: [] }
    if (testName === 'place-dep') {
      rec.before = describe(rec.tree)
    }
    const onlog = (level, ...msg) => {
      if (level === 'warn') {
        rec.warnings.push(plain(msg))
      }
    }
    process.on('log', onlog)
    try {
      return fn.call(this, t)
    } finally {
      process.removeListener('log', onlog)
      running = null
      if (rec.after === undefined) {
        rec.after = describe(rec.tree)
      }
      answers[index] = answer(rec)
    }
  }
}

// tap 16 runs a subtest at once unless one is running, and then after it, in the order they were queued
const standIn = () => {
  const make = () => {
    const t = { occupied: false, queue: [] }
    const run = (fn, sub) => {
      t.occupied = true
      fn(sub)
      t.occupied = false
    }
    t.test = (name, fn) => {
      t.queue.push(recording(fn))
      if (!t.occupied) {
        while (t.queue.length) {
          run(t.queue.shift(), make())
        }
      }
    }
    t.throws = fn => {
      try {
        fn()
      } catch (er) {
        return er
      }
      throw new Error('expected a throw')
    }
    for (const m of ['end', 'equal', 'ok', 'strictSame', 'matchSnapshot', 'comment']) {
      t[m] = () => true
    }
    return t
  }
  return make()
}

let tap
if (tapDir) {
  tap = require(path.join(tapDir, 'node_modules', 'tap'))
  const test = tap.Test.prototype.test
  tap.Test.prototype.test = function (name, fn, ...rest) {
    return test.call(this, name, recording(fn), ...rest)
  }
  // there is no snapshot file beside this script, and a missing one throws out of the test function
  tap.Test.prototype.matchSnapshot = () => true
} else {
  tap = standIn()
}

const file = path.join(checkout, 'test', `${testName}.js`)
const source = fs.readFileSync(file, 'utf8')
const hooked = testName === 'can-place-dep'
  ? source.replace('const runTest = (desc, {', 'const runTest = (desc, __options) => { __case(desc, __options); const {')
    .replace('}) => {\n    const target', '} = __options\n    const target')
  : source.replace('const runTest = (desc, options) => {', 'const runTest = (desc, options) => { __case(desc, options)')
if (!hooked.includes('__case(desc')) {
  throw new Error('runTest not found')
}
const mod = new Module(file, null)
mod.filename = file
mod.paths = Module._nodeModulePaths(path.dirname(file))
const realRequire = mod.require.bind(mod)
const libs = {
  '../lib/node.js': declaring(Node, 'Node'),
  '../lib/link.js': declaring(Link, 'Link'),
  '../lib/override-set.js': new Proxy(OverrideSet, {
    construct (target, args) {
      const decl = declare(args[0])
      const os = Reflect.construct(target, args)
      overrideIds.set(os, declarations.overrides.length)
      declarations.overrides.push(decl)
      return os
    },
  }),
  '../lib/place-dep.js': answering(PlaceDep, 'placeDep'),
  '../lib/can-place-dep.js': answering(CanPlaceDep, 'canPlaceDep'),
}
mod.require = id => {
  if (id === 'tap') {
    return tap
  }
  if (id === 'tcompare') {
    return { strict: () => ({ diff: '' }) }
  }
  if (libs[id]) {
    return libs[id]
  }
  if (id.startsWith('../lib/') || id.startsWith('@isaacs/')) {
    throw new Error(`unexpected module ${id}`)
  }
  return realRequire(id)
}
global.__case = caseHook

const replay = () => {
  const built = new Map()
  const overrideSets = new Map()
  const revive = v => {
    if (v === null || typeof v !== 'object') {
      return v
    }
    if (Array.isArray(v)) {
      return v.map(revive)
    }
    if (v.$node !== undefined) {
      return build(v.$node)
    }
    if (v.$overrides !== undefined) {
      if (!overrideSets.has(v.$overrides)) {
        overrideSets.set(v.$overrides, new OverrideSet(revive(declarations.overrides[v.$overrides])))
      }
      return overrideSets.get(v.$overrides)
    }
    if (v.$error !== undefined) {
      return new Error(v.$error)
    }
    const o = {}
    for (const [k, x] of Object.entries(v)) {
      o[k] = revive(x)
    }
    return o
  }
  const build = id => {
    if (!built.has(id)) {
      const { class: kind, options } = declarations.nodes[id]
      built.set(id, new (kind === 'Link' ? Link : Node)(revive(options)))
    }
    return built.get(id)
  }

  const state = []
  const setup = i => {
    const c = cases[i]
    const tree = build(c.tree)
    const dep = build(c.dep)
    const node = tree.inventory.get(c.nodeLoc)
    const edge = node.edgesOut.get(dep.name)
    if (!dep.satisfies(edge)) {
      edge.peerConflicted = true
    }
    const vr = new Node({
      sourceReference: node,
      path: node.path,
      pkg: { ...node.package },
      children: c.peerSet && revive(c.peerSet),
    })
    dep.parent = vr
    for (const child of vr.children.values()) {
      for (const edgeIn of child.edgesIn) {
        if (edgeIn.invalid) {
          edgeIn.peerConflicted = true
        }
      }
    }
    state[i] = { tree, dep, edge, target: tree.inventory.get(c.targetLoc) }
  }
  const run = i => {
    const c = cases[i]
    const { tree, dep, edge, target } = state[i]
    const rec = { tree, warnings: [] }
    if (testName === 'can-place-dep') {
      rec.canPlaceDep = new CanPlaceDep({
        target,
        edge,
        dep,
        preferDedupe: c.preferDedupe,
        explicitRequest: c.explicitRequest,
      })
      return answer(rec)
    }
    rec.before = describe(tree)
    const onlog = (level, ...msg) => {
      if (level === 'warn') {
        rec.warnings.push(plain(msg))
      }
    }
    process.on('log', onlog)
    try {
      rec.placeDep = new PlaceDep({
        edge,
        dep,
        preferDedupe: c.preferDedupe || false,
        force: c.force || false,
        explicitRequest: c.explicitRequest,
        updateNames: c.auditReport ? [edge.name] : c.updateNames || [],
        strictPeerDeps: c.strictPeerDeps || false,
        legacyPeerDeps: c.legacyPeerDeps || false,
        installStrategy: c.installStrategy || 'hoisted',
      })
    } catch (er) {
      rec.thrown = plain(Object.assign({ message: er.message }, er))
    } finally {
      process.removeListener('log', onlog)
    }
    rec.after = describe(tree)
    return answer(rec)
  }
  for (const e of events) {
    if (e.setup !== undefined) {
      setup(e.setup)
    } else {
      const replayed = JSON.stringify(run(e.run))
      if (replayed !== JSON.stringify(answers[e.run])) {
        throw new Error(`${cases[e.run].name}: the replay answered\n${replayed}\nwhere the test run answered\n` +
          JSON.stringify(answers[e.run]))
      }
    }
  }
}

const write = () => {
  if (events.filter(e => e.run !== undefined).length !== cases.length || answers.length !== cases.length) {
    throw new Error('a case was set up but never run')
  }
  replay()
  fs.writeFileSync(out, zlib.gzipSync(JSON.stringify({ declarations, cases, events, answers }, null, 1), { level: 9 }))
}

mod._compile(hooked, file)
if (tapDir) {
  process.on('exit', write)
} else {
  write()
}
