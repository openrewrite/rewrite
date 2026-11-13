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
// Writes the releases PnpmReleases reads, pnpm-releases.txt beside it in the main resources: every stable release of
// pnpm 9 to 12 the registry publishes as the package `pnpm`, one a line in ascending order. pnpm 9.0.0 is the first
// that writes lockfileVersion 9.0, the format the port reads. They come from the registry's packument of pnpm, read
// from the file given or fetched.
// Usage: node pnpm-releases.js [packument.json] \
//          > <main resources>/org/openrewrite/javascript/internal/lock/pnpm/pnpm-releases.txt
const fs = require('fs')

const parse = version => version.split('.').map(Number)
const compare = (a, b) => a[0] - b[0] || a[1] - b[1] || a[2] - b[2]

const packument = async file => file
  ? JSON.parse(fs.readFileSync(file, 'utf8'))
  : (await fetch('https://registry.npmjs.org/pnpm', { headers: { accept: 'application/json' } })).json()

packument(process.argv[2]).then(doc => {
  const releases = Object.keys(doc.versions)
    .filter(version => /^(9|1[0-2])\.\d+\.\d+$/.test(version))
    .map(parse)
    .sort(compare)
  process.stdout.write(releases.map(release => release.join('.')).join('\n') + '\n')
}).catch(e => {
  console.error(e)
  process.exit(1)
})
