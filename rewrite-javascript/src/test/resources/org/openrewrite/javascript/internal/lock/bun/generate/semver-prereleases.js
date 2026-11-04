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
// Writes semver-prereleases.txt and semver-prereleases-sorted.txt: the unsortedPrereleases of bun's semver fixture and
// the order its semver test's snapshot expects after Array.prototype.sort(Bun.semver.order).
//
// Usage: node semver-prereleases.js <bun checkout>/test/cli/install <out-dir>
const fs = require('fs')
const path = require('path')

const [install, out] = process.argv.slice(2)
const lenient = text => JSON.parse(text.replace(/,(\s*])/g, '$1'))
const fixture = fs.readFileSync(path.join(install, 'semver-fixture.js'), 'utf8')
const unsorted = lenient(fixture.slice(fixture.indexOf('['), fixture.lastIndexOf(']') + 1))
const snap = fs.readFileSync(path.join(install, '__snapshots__', 'semver.test.ts.snap'), 'utf8')
const sorted = lenient(snap.slice(snap.indexOf('`\n[') + 2, snap.lastIndexOf(']') + 1))
fs.writeFileSync(path.join(out, 'semver-prereleases.txt'), unsorted.join('\n') + '\n')
fs.writeFileSync(path.join(out, 'semver-prereleases-sorted.txt'), sorted.join('\n') + '\n')
