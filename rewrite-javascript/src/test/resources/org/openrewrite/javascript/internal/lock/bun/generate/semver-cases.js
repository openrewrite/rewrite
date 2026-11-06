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
// Writes semver-cases.tsv: bun's answers to every Bun.semver call of test/cli/install/semver.test.ts, and to a sample
// of the ranges and versions the given Bun scenarios' locks and packuments name (`kind version range answer` rows).
// With --rows, bun answers the rows of an existing semver-cases.tsv again instead.
//
// Usage: node semver-cases.js <bun checkout>/test/cli/install <Bun scenarios directory> > semver-cases.tsv
//        node semver-cases.js --rows semver-cases.tsv > semver-cases.tsv.new
const fs = require('fs')
const os = require('os')
const path = require('path')
const {spawnSync} = require('child_process')

const [install, scenarios] = process.argv.slice(2)
const bun = spawnSync('sh', ['-c', 'command -v bun'], {encoding: 'utf8'}).stdout.trim()
const tmp = fs.mkdtempSync(path.join(os.tmpdir(), 'semver-cases-'))
const enc = s => s.replace(/\\/g, '\\\\').replace(/\t/g, '\\t').replace(/\n/g, '\\n').replace(/\r/g, '\\r')

if (install === '--rows') {
  fs.writeFileSync(path.join(tmp, 'rows.ts'), `
const enc = ${enc.toString()};
const dec = (s: string) => s.replace(/\\\\([\\\\tnr])/g, (m: string, c: string) =>
  ({"\\\\": "\\\\", t: "\\t", n: "\\n", r: "\\r"} as any)[c]);
const out: string[] = [];
for (const line of require("fs").readFileSync(${JSON.stringify(path.resolve(scenarios))}, "utf8").split("\\n")) {
  if (!line) continue;
  const [kind, a, b] = line.split("\\t").map(dec);
  const r = kind === "satisfies" ? Bun.semver.satisfies(a, b) : Bun.semver.order(a, b);
  out.push([kind, enc(a), enc(b), String(r)].join("\\t"));
}
process.stdout.write(out.join("\\n") + "\\n");
`)
  spawnSync(bun, ['run', 'rows.ts'], {cwd: tmp, stdio: 'inherit'})
  fs.rmSync(tmp, {recursive: true, force: true})
  process.exit(0)
}

fs.copyFileSync(path.join(install, 'semver-fixture.js'), path.join(tmp, 'semver-fixture.js'))
fs.mkdirSync(path.join(tmp, '__snapshots__'))
fs.copyFileSync(path.join(install, '__snapshots__', 'semver.test.ts.snap'),
  path.join(tmp, '__snapshots__', 'semver.test.ts.snap'))
fs.writeFileSync(path.join(tmp, 'semver.test.ts'), 'import { afterAll, describe, test, expect } from "bun:test";\n' +
  fs.readFileSync(path.join(install, 'semver.test.ts'), 'utf8')
    .replace(/^const \{ satisfies, order \} = Bun\.semver;$/m,
    'import { satisfies, order, flush } from "./recorder.ts";') +
  `afterAll(() => flush(${JSON.stringify(path.join(tmp, 'test-rows.tsv'))}));\n`)
fs.writeFileSync(path.join(tmp, 'recorder.ts'), `
const rows: string[] = [];
const ok = (s: unknown) => typeof s === "string";
const enc = ${enc.toString()};
export function satisfies(a: any, b: any) {
  const r = Bun.semver.satisfies(a, b);
  if (ok(a) && ok(b)) rows.push(["satisfies", enc(a), enc(b), String(r)].join("\\t"));
  return r;
}
export function order(a: any, b: any) {
  const r = Bun.semver.order(a, b);
  if (ok(a) && ok(b)) rows.push(["order", enc(a), enc(b), String(r)].join("\\t"));
  return r;
}
export function flush(file: string) {
  require("fs").writeFileSync(file, rows.join("\\n") + "\\n");
}
`)
// bun's own "expected errors" test fails under the recorder: satisfies() then reaches Bun.semver with two undefineds
spawnSync(bun, ['test', 'semver.test.ts'], {cwd: tmp, env: {PATH: '/usr/bin:/bin', HOME: tmp}, stdio: 'ignore'})

const ranges = new Map()
const versions = new Map()
const add = (map, k, v) => (map.get(k) || map.set(k, new Set()).get(k)).add(v)
const lenient = text => JSON.parse(text.replace(/^\s*\/\/.*$/mg, '').replace(/,(\s*[}\]])/g, '$1'))
const plain = s => typeof s === 'string' && !/[\t\r\n]/.test(s) && /^[\x20-\x7e]*$/.test(s)
const groups = o => ['dependencies', 'devDependencies', 'optionalDependencies', 'peerDependencies'].forEach(g =>
  Object.entries((o || {})[g] || {}).forEach(([n, r]) => plain(r) && add(ranges, n, r)))
for (const dir of fs.readdirSync(scenarios)) {
  const d = path.join(scenarios, dir)
  for (const lock of ['before', 'after', 'fresh']) {
    const f = path.join(d, lock)
    if (!fs.existsSync(f)) continue
    let json
    try {
      json = lenient(fs.readFileSync(f, 'utf8'))
    } catch (e) {
      continue
    }
    Object.values(json.workspaces || {}).forEach(groups)
    Object.values(json.packages || {}).forEach(t => Array.isArray(t) &&
      t.forEach(x => x && typeof x === 'object' && groups(x)))
  }
  const http = path.join(d, 'http')
  if (!fs.existsSync(http)) continue
  for (const f of fs.readdirSync(http)) {
    let json
    try {
      json = JSON.parse(fs.readFileSync(path.join(http, f), 'utf8'))
    } catch (e) {
      continue
    }
    if (!json.versions || !json.name) continue
    Object.keys(json.versions).forEach(v => plain(v) && add(versions, json.name, v))
    Object.values(json.versions).forEach(m => ['dependencies', 'optionalDependencies', 'peerDependencies'].forEach(g =>
      Object.entries(m[g] || {}).forEach(([n, r]) => plain(r) && add(ranges, n, r))))
  }
}
const pairs = new Set()
for (const [name, rs] of ranges) {
  for (const r of rs) for (const v of versions.get(name) || []) pairs.add(`satisfies\t${v}\t${r}`)
}
for (const vs of versions.values()) {
  const list = [...vs]
  for (let i = 0; i + 1 < list.length; i++) pairs.add(`order\t${list[i]}\t${list[i + 1]}`)
}
fs.writeFileSync(path.join(tmp, 'pairs.tsv'), [...pairs].join('\n') + '\n')
fs.writeFileSync(path.join(tmp, 'pairs.ts'), `
const enc = ${enc.toString()};
const out: string[] = [];
for (const line of require("fs").readFileSync(${JSON.stringify(path.join(tmp, 'pairs.tsv'))}, "utf8").split("\\n")) {
  if (!line) continue;
  const [kind, a, b] = line.split("\\t");
  const r = kind === "satisfies" ? Bun.semver.satisfies(a, b) : Bun.semver.order(a, b);
  out.push([kind, enc(a), enc(b), String(r)].join("\\t"));
}
require("fs").writeFileSync(${JSON.stringify(path.join(tmp, 'pair-rows.tsv'))}, out.join("\\n") + "\\n");
`)
spawnSync(bun, ['run', 'pairs.ts'], {cwd: tmp, stdio: 'inherit'})

const unique = file => [...new Set(fs.readFileSync(path.join(tmp, file), 'utf8').split('\n').filter(Boolean))]
const test = unique('test-rows.tsv')
const seen = new Set(test)
const out = [...test]
let satisfied = 0
let unsatisfied = 0
let ordered = 0
// every row of bun's own test, then every 7th satisfied, every 13th unsatisfied and every 5th order pair
for (const row of unique('pair-rows.tsv')) {
  if (seen.has(row)) continue
  const [kind, , , answer] = row.split('\t')
  if (kind === 'satisfies' && answer === 'true' ? satisfied++ % 7 === 0 :
    kind === 'satisfies' ? unsatisfied++ % 13 === 0 : ordered++ % 5 === 0) {
    out.push(row)
  }
}
process.stdout.write(out.join('\n') + '\n')
fs.rmSync(tmp, {recursive: true, force: true})
