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
// Expectations for YarnLockTest, from the arborist npm bundles: test/yarn-lock.js's two fixtures (the input,
// and the lines of the regenerated output that differ from it) and the yarn.lock arborist writes for the
// trees that test declares inline.
// Usage: node yarn-lock-dumps.js <npm/cli checkout>/workspaces/arborist > ../yarn-lock-dumps
const fs = require('fs')
const path = require('path')
const npm = require('./npm-modules.js')
const Node = npm('@npmcli/arborist/lib/node.js')
const YarnLock = npm('@npmcli/arborist/lib/yarn-lock.js')

const fixtures = ['tap-with-yarn-lock', 'yarn-stuff'].map(name => {
  const input = fs.readFileSync(path.join(process.argv[2], 'test/fixtures', name, 'yarn.lock'), 'utf8')
  const parsed = new YarnLock().parse(input)
  const before = input.split('\n')
  const after = parsed.toString().split('\n')
  if (before.length !== after.length) {
    throw new Error(`${name}: ${before.length} lines in, ${after.length} out`)
  }
  const changedLines = []
  after.forEach((line, i) => {
    if (line !== before[i]) {
      changedLines.push([i, line])
    }
  })
  return { name, input, changedLines, specs: [...parsed.entries.keys()] }
})
process.chdir('/')

const trees = [
  {
    name: 'yarn lock with dedupes yarn wouldnt do',
    tree: {
      path: '/my/project',
      pkg: { dependencies: { x: '1.x', y: '1.x', z: '1.x' } },
      children: [
        { pkg: { name: 'x', version: '1.2.0' } },
        { pkg: { name: 'y', version: '1.0.0', dependencies: { x: '1.1', z: '2.x' } },
          children: [
            { pkg: { name: 'x', version: '1.1.0' } },
            { pkg: { name: 'z', version: '2.0.0', dependencies: { x: '1.x' } } },
          ],
        },
        { pkg: { name: 'z', version: '1.0.0' } },
      ],
    },
  },
  {
    name: 'deduped prior entries that do not match one another',
    tree: {
      path: '/my/project',
      pkg: { dependencies: { a: '', b: '' } },
      children: [
        { pkg: { name: 'a', dependencies: { i: '', x: '1.x', y: '1.x', z: '1.x' } },
          children: [
            { pkg: { name: 'i', version: '1.0.0', dependencies: { x: '1.2.0' } } },
            { pkg: { name: 'x', version: '1.2.0' }, integrity: 'x120' },
            { pkg: { name: 'y', version: '1.0.0', dependencies: { x: '1.1', z: '2.x' } },
              children: [
                { pkg: { name: 'x', version: '1.1.0' } },
                { pkg: { name: 'z', version: '2.0.0', dependencies: { x: '1.x' } } },
              ],
            },
            { pkg: { name: 'z', version: '1.0.0' } },
          ],
        },
        { pkg: { name: 'b', dependencies: { j: '', x: '1.x', y: '1.x', z: '1.x' } },
          children: [
            { pkg: { name: 'j', version: '1.0.0', dependencies: { x: '1.3.0' } } },
            { pkg: { name: 'x', version: '1.3.0' }, integrity: 'x130' },
            { pkg: { name: 'y', version: '1.0.0', dependencies: { x: '1.1', z: '2.x' } },
              children: [
                { pkg: { name: 'x', version: '1.1.0' } },
                { pkg: { name: 'z', version: '2.0.0', dependencies: { x: '1.x' } } },
              ],
            },
            { pkg: { name: 'z', version: '1.0.0' } },
          ],
        },
      ],
    },
  },
  {
    name: 'more nesting tree complications',
    tree: {
      path: '/my/project',
      pkg: { dependencies: { a: '', b: '', c: '', c2: '', d3: '' } },
      children: [
        { pkg: { name: 'a', dependencies: { x: '', g: '', h: '', i: '', j: '', k: '' } },
          children: [
            { pkg: { name: 'x', version: '1.0.0' }, resolved: 'https://x100.xyz' },
            { pkg: { name: 'g', version: '1.0.0', dependencies: { x: '^1.0.0' } } },
            { pkg: { name: 'h', version: '1.0.0', dependencies: { x: '1.0.0' } } },
            { pkg: { name: 'i', version: '1.0.0', dependencies: { x: '1.0' } } },
            { pkg: { name: 'j', version: '1.0.0', dependencies: { x: '1' } } },
            { pkg: { name: 'k', version: '1.0.0', dependencies: { x: '' } } },
          ],
        },
        { pkg: { name: 'b', dependencies: { x: '', l: '', m: '', n: '', n2: '', o: '', p: '' } },
          children: [
            { pkg: { name: 'x', version: '1.0.1' } },
            { pkg: { name: 'l', version: '1.0.0', dependencies: { x: '^1.0.1' } } },
            { pkg: { name: 'm', version: '1.0.0', dependencies: { x: '1.0.1' } } },
            { pkg: { name: 'n', version: '1.0.0', dependencies: { x: '1.0' } } },
            { pkg: { name: 'n2', version: '1.0.0', dependencies: { x: '>=1.0' } } },
            { pkg: { name: 'o', version: '1.0.0', dependencies: { x: '1' } } },
            { pkg: { name: 'p', version: '1.0.0', dependencies: { x: '' } } },
          ],
        },
        { pkg: { name: 'c', dependencies: { d: '', e: '', f: '' } },
          children: [
            { pkg: { name: 'x', version: '1.0.1' }, integrity: 'x101', resolved: 'https://x101.xyz' },
            { pkg: { name: 'd', version: '1.0.0', dependencies: { x: '^1.0.1' } } },
            { pkg: { name: 'e', version: '1.0.0', dependencies: { x: '>=1.0.1 <2' } } },
            { pkg: { name: 'f', version: '1.0.0', dependencies: { x: '>=1.0' } } },
          ],
        },
        { pkg: { name: 'c2', dependencies: { d2: '', e2: '', f2: '' } },
          children: [
            { pkg: { name: 'x', version: '1.0.1' }, resolved: 'https://x101.xyz' },
            { pkg: { name: 'd2', version: '1.0.0', dependencies: { x: '^1.0.0-x' } } },
            { pkg: { name: 'e2', version: '1.0.0', dependencies: { x: '>=1.0.0' } } },
            { pkg: { name: 'f2', version: '1.0.0', dependencies: { x: '1.0.1' } } },
          ],
        },
        { pkg: { name: 'd3', dependencies: { x: '' } },
          children: [{ pkg: { name: 'x' } }],
        },
      ],
    },
  },
]
for (const t of trees) {
  t.expected = new YarnLock().fromTree(new Node(JSON.parse(JSON.stringify(t.tree)))).toString()
}

console.log(JSON.stringify({ fixtures, trees }, null, 1))
