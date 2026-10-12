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
// Where an earlier node-semver answers another thing than the newest: every validRange and satisfies question the
// newest release's fixtures ask (and the ranges below), put to the semver each npm release bundles, keeping the rows
// whose answer is not the newest's. One JSON array a line: release, function, arguments, loose, includePrerelease,
// answer.
// Usage: node release-differences.js <newest fixtures file> <newest npm's node_modules> <release>=<npm's node_modules>...
const fs = require('fs')
const path = require('path')
const [fixturesFile, newestModules, ...others] = process.argv.slice(2)
const fixtures = JSON.parse(fs.readFileSync(fixturesFile, 'utf8'))
const newest = require(path.join(newestModules, 'semver'))

// ranges at the edges the releases moved: build metadata on partial versions, a number after a wildcard, a caret
// below 1.0.0 under includePrerelease
const RANGES = ['1.x+build', '^1+build', '~1.2+build', '1.2.3+build', '>=1.2.3+build <2', '1.2.3+a - 2.0.0+b',
  '1.2+a - 2', 'x.1.2', '*.1', '1.x.3', 'x.x.1', '1.*.0', '^x.1.2', '~x.1', '>=x.1.2', '^0.0.1', '^0.1.2', '^0.1',
  '^0', '^1.2.3', '~0.0.1', '1.2.3-pre+build', '>1.2.3+build', '<=1.2.3+b.1', '1.2.x+a', '1+a', '>=1+a',
  '1.2.3+a.b - 1.2.4', '1.2.3+a||2.0.0+b', '^1.2.3+a || ~2.0+b', '1.2+a.b', '*+a', 'x+a', '>=1.0.0+a <2.0.0+b',
  '1.x.5 || 2.x', '>1.2.3+a <1.2.5+b || 3.x+c', `1.2+${'a'.repeat(260)}`, `^1+${'b'.repeat(251)}.c`]
const VERSIONS = ['0.0.1-beta', '0.1.2-beta', '0.1.2', '0.0.1', '1.0.0', '1.5.0', '1.2.3', '1.2.4', '2.0.0', '0.1.0',
  '1.2.3-pre.2', '3.1.0']

const optionSets = [[false, false], [true, false], [false, true], [true, true]]
const questions = []
const ranges = new Set(RANGES)
for (const [r] of fixtures.rangeParse) ranges.add(r)
for (const [r, v] of [...fixtures.rangeInclude, ...fixtures.rangeExclude]) {
  ranges.add(r)
  for (const o of optionSets) questions.push(['satisfies', [v, r], ...o])
}
for (const r of ranges) {
  for (const o of optionSets) questions.push(['validRange', [r], ...o])
}
for (const r of RANGES) {
  for (const v of VERSIONS) {
    for (const o of optionSets) questions.push(['satisfies', [v, r], ...o])
  }
}
const ask = (semver, [fn, args, loose, includePrerelease]) => semver[fn](...args, { loose, includePrerelease })
const seen = new Set()
for (const other of others) {
  const [release, modules] = other.split('=')
  const semver = require(path.join(modules, 'semver'))
  for (const q of questions) {
    const key = release + JSON.stringify(q)
    if (seen.has(key)) continue
    seen.add(key)
    const answer = ask(semver, q)
    if (answer !== ask(newest, q)) {
      console.log(JSON.stringify([release, ...q, answer]))
    }
  }
}
