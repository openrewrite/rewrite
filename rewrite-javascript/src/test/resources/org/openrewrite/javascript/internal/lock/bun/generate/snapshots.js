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
// Writes snapshots/<test name>.lock: every snapshot of bun's bun-lock.test.ts and catalogs.test.ts that is a whole
// bun.lock.
//
// Usage: node snapshots.js <bun checkout>/test/cli/install/__snapshots__ <out-dir>
const fs = require('fs')
const path = require('path')

const [snapshots, out] = process.argv.slice(2)
fs.mkdirSync(out, {recursive: true})
for (const file of ['bun-lock.test.ts.snap', 'catalogs.test.ts.snap']) {
  const exports = {}
  new Function('exports', fs.readFileSync(path.join(snapshots, file), 'utf8'))(exports)
  for (const [name, raw] of Object.entries(exports)) {
    const value = typeof raw === 'string' ? raw.replace(/^\n/, '') : raw
    if (typeof value !== 'string' || !value.startsWith('"{\n  "lockfileVersion"')) continue
    const slug = (file.startsWith('catalogs') ? 'catalogs-' : 'bun-lock-') +
      name.toLowerCase().replace(/[^a-z0-9]+/g, '-').replace(/^-|-$/g, '')
    fs.writeFileSync(path.join(out, slug + '.lock'), value.slice(1, value.lastIndexOf('"')))
  }
}
