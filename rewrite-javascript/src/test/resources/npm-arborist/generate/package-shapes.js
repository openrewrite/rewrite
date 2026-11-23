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
// What the arborist npm bundles makes of package.json content whose values have unexpected JSON types, whose
// keys are integers or whose keys name members of Object.prototype: {json, legacyPeerDeps, throws} when
// building the tree throws, else {json, legacyPeerDeps, expected}. The tree is a root at /r with the
// package, and children d (which depends on a and b) and a (which depends on b).
// A spec that is not a string, which an override's "$name" can refer to, is written THROW, and so is the
// error of its edge when the edge has a target: arborist carries such a spec to whatever reads it next,
// and npm fails there.
// Usage: node package-shapes.js > ../package-shapes
process.chdir('/')
const npm = require('./npm-modules.js')
const Node = npm('@npmcli/arborist/lib/node.js')

const deps = '"dependencies":{"a":"^1.0.0","b":"1"}'
const overrides = [
  '{"a":null}', '{"a":5}', '{"a":true}', '{"a":["x"]}', '{"a":[null]}', '{"a":{".":5}}', '{"a":{".":{}}}',
  '{"a":{".":["2"]}}', '{"a":{".":true}}', '{"a":{".":null}}', '{"a":{".":0}}', '{"a":{".":false,"b":"1"}}',
  '{"a@1":{".":""}}', '{"a":""}', '{"a":{"b":null}}', '{"b":"1","10":"2.0.0","a":{"2":"x","b":"3","1":"y"},"0":"3"}',
  '"abc"', '["a"]', '5', 'null', '{}', 'true', '""', '{".":"1"}', '{"a":"$constructor"}', '{"b":"$constructor"}',
  '{"a":"$b"}', '{"b":"$a"}', '{"a":"$zz"}',
  '{"a":{".":"$a","a":"1"}}', '{"a":"2.0.0"}', '{"a@^1":"1.0.0"}', '{"b":{".":5}}', '{"d":{"a":{".":5}}}',
]
const packages = [
  ...overrides.map(o => `{"name":"root","version":"1.0.0",${deps},"overrides":${o}}`),
  '{"dependencies":{"0":"1","a":"1"},"overrides":["2",{".":"3","a":"4"}]}',
  '{"dependencies":{"a":null}}',
  '{"dependencies":{"a":5}}',
  '{"dependencies":{"a":"1"},"acceptDependencies":{"a":null}}',
  '{"dependencies":{"a":"1"},"acceptDependencies":{"a":5}}',
  '{"dependencies":{"a":"1"},"acceptDependencies":{"a":""}}',
  '{"dependencies":{"a":"1"},"acceptDependencies":{"a":"2"}}',
  '{"dependencies":{"a":"1"},"acceptDependencies":"x"}',
  '{"dependencies":{"a":"1"},"acceptDependencies":5}',
  '{"dependencies":{"a":"1","0":"2"},"acceptDependencies":"x"}',
  '{"dependencies":{"a":"1","length":"2"},"acceptDependencies":"x"}',
  '{"dependencies":{"a":"1","trim":"2"},"acceptDependencies":"x"}',
  '{"dependencies":{"a":"1","0":"2"},"acceptDependencies":["9"]}',
  '{"dependencies":{"a":"1","map":"2"},"acceptDependencies":["9"]}',
  '{"dependencies":{"a":"1","toFixed":"2"},"acceptDependencies":5}',
  '{"dependencies":{"constructor":"1"}}',
  '{"dependencies":{"constructor":"1"},"acceptDependencies":{"constructor":"2"}}',
  '{"dependencies":{"toString":"1"}}',
  '{"dependencies":{"hasOwnProperty":"1"}}',
  '{"dependencies":{"__proto__":"1"}}',
  '{"dependencies":{"__proto__":"1"},"acceptDependencies":{"__proto__":"2"}}',
  '{"optionalDependencies":{"valueOf":"1"}}',
  '{"devDependencies":{"isPrototypeOf":"1"}}',
  '{"peerDependencies":{"constructor":"1","a":"1"},"peerDependenciesMeta":{"a":{"optional":true}}}',
  '{"peerDependencies":{"constructor":"1","a":"1"},"peerDependenciesMeta":{"a":{"optional":true}},' +
    '"acceptDependencies":{"constructor":""}}',
  '{"peerDependencies":{"__proto__":"1","a":"1"}}',
  '{"peerDependencies":{"a":"1","0":"2"},"peerDependenciesMeta":"xy"}',
  '{"peerDependencies":{"a":"1","0":"2"},"peerDependenciesMeta":[{"optional":true}]}',
  '{"peerDependencies":{"a":"1"},"peerDependenciesMeta":{"a":null}}',
  '{"peerDependencies":{"a":"1"},"peerDependenciesMeta":{"a":{"optional":1}}}',
  '{"peerDependencies":{"a":"1"},"peerDependenciesMeta":{"a":"optional"}}',
  '{"peerDependencies":["a","b"]}',
  '{"peerDependencies":"ab"}',
  '{"peerDependencies":{"a":"1","b":5},"legacyPeerDeps":true}',
  '{"dependencies":"a","optionalDependencies":["a"]}',
  '{"dependencies":["a","b"]}',
  '{"dependencies":5,"optionalDependencies":true}',
  '{"devDependencies":[null]}',
  '{"dependencies":{"a":"1"},"optionalDependencies":{"a":null}}',
  '{"dependencies":{"b":"1","10":"1","a":"1","2":"1"},"devDependencies":{"z":"1","1":"1"},' +
    '"peerDependencies":{"q":"1","3":"1"},"optionalDependencies":{"y":"1","4":"1"}}',
  '{"dependencies":{"b":"1","4294967295":"1","01":"1","4294967294":"1","1":"1","-1":"1","1.5":"1","0":"1"}}',
  '{"dependencies":{"b":"1","10":"1","a":"1","2":"1"},"bundleDependencies":true}',
  '{"bundleDependencies":true,"dependencies":"ab"}',
  '{"bundleDependencies":{"x":1,"10":2,"y":3,"9":4}}',
  '{"name":"a","version":"1.0.0","bundledDependencies":["x"],"bundleDependencies":null}',
  '{"name":"a","version":"1.0.0","bundledDependencies":["x"]}',
  '{"bundleDependencies":"x"}',
  '{"bundleDependencies":false}',
  '{"bundleDependencies":0}',
  '{"name":"a","version":2,"dependencies":{"a":"1","b":"2"},"optionalDependencies":{"a":"3"}}',
  '{"dependencies":{"a":"1"},"optionalDependencies":{"a":"3"}}',
  '{"dependencies":{},"optionalDependencies":{}}',
  '{"dependencies":{},"optionalDependencies":[]}',
  '{"dependencies":{"a":"1"},"optionalDependencies":"a"}',
  '{"dependencies":5,"optionalDependencies":{"a":"1"}}',
  '{"dependencies":{"0":"1","a":"1"},"optionalDependencies":["x"]}',
  '{"scripts":{"a":"x","b":1,"c":null,"install":"y"}}',
  '{"scripts":"x"}',
  '{"scripts":null}',
  '{"scripts":["install"]}',
  '{"scripts":5}',
  '{"funding":"https://x"}',
  '{"funding":""}',
  '{"funding":["https://x"]}',
  '{"name":"","version":"1.0.0"}',
  '{"name":5,"version":"1.0.0","dependencies":{"a":"1"}}',
]

const rule = o => o ? [o.key ?? null, o.name ?? null, o.keySpec ?? null, o.value ?? null] : null
// keeps a key named __proto__, which an assignment would drop
const sorted = v => Array.isArray(v) ? v.map(sorted) :
  v && typeof v === 'object' ? Object.fromEntries(Object.keys(v).sort().map(k => [k, sorted(v[k])])) : v
const attempt = fn => {
  try {
    return fn()
  } catch {
    return 'THROW'
  }
}
const describe = (json, legacyPeerDeps) => {
  const root = new Node({ path: '/r', pkg: JSON.parse(json), loadOverrides: true, legacyPeerDeps })
  const dPkg = { name: 'd', version: '1.0.0', dependencies: { a: '1.x', b: '^1' } }
  const d = new Node({ parent: root, name: 'd', pkg: dPkg })
  const a = new Node({ parent: root, name: 'a', pkg: { name: 'a', version: '1.0.0', dependencies: { b: '1' } } })
  const edges = []
  for (const node of [root, d, a]) {
    for (const edge of node.edgesOut.values()) {
      const spec = attempt(() => edge.spec)
      const stringSpec = typeof spec === 'string'
      const error = stringSpec || !edge.to ? attempt(() => edge.error) : 'THROW'
      edges.push([node.name, edge.name, edge.type, stringSpec ? spec : 'THROW', edge.rawSpec, edge.accept ?? null,
        rule(edge.overrides), error, edge.to ? edge.to.location : null])
    }
  }
  return {
    package: sorted(root.package),
    rules: root.overrides ? [...root.overrides.ruleset.values()].map(rule) : null,
    nested: a.overrides ? [...a.overrides.ruleset.values()].map(rule) : null,
    nodes: [root, d, a].map(node => rule(node.overrides)),
    edges,
    rootOverrides: attempt(() => {
      root.assertRootOverrides()
      return 'ok'
    }),
  }
}

console.log(JSON.stringify(packages.map(text => {
  const legacyPeerDeps = text.includes('"legacyPeerDeps":true')
  const json = text.replace(',"legacyPeerDeps":true', '')
  try {
    return { json, legacyPeerDeps, expected: describe(json, legacyPeerDeps) }
  } catch (e) {
    return { json, legacyPeerDeps, throws: e.message }
  }
})))
