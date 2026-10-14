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
// Writes lock-entries-reused.tsv: as lock-entries.js, with a PackageReference and addPattern for every pattern, as an
// install that reuses the lock's entries does, over locks with odd permissions, prebuiltVariants and dependencies.
//
// Usage: node lock-entries-reused.js <yarn 1.22.22>/lib/cli.js > lock-entries-reused.tsv
'use strict'
const yarn = require('./yarn-bundle.js')(process.argv[2])
const parse = yarn.load(/var VERSION_REGEX = \/\^yarn lockfile v/).default
const stringify = yarn.load(/lines\.push\(`# yarn lockfile v/).default
const Lockfile = yarn.load(/Lockfile.prototype.getLockfile = function getLockfile/).default
const PackageRequest = yarn.load(/PackageRequest.prototype.getLocked = function getLocked/).default
const PackageReference = yarn.load(/PackageReference.prototype.addPattern = function addPattern/).default
const head = '# yarn lockfile v1\n\n\n'
const R = '  version "1.0.0"\n  resolved "https://r/a.tgz#abc"\n'
const cases = [
  'a@1:\n' + R + '  permissions "ab"\n',
  'a@1:\n' + R + '  permissions:\n    x true\n',
  'a@1:\n' + R + '  permissions:\n    x 5\n    y "s"\n',
  'a@1:\n' + R + '  permissions 5\n',
  'a@1:\n' + R + '  permissions true\n',
  "'a@1':\n  version: '1.0.0'\n  resolved: 'https://r/a.tgz#abc'\n  permissions: [p, q]\n",
  "'a@1':\n  version: '1.0.0'\n  resolved: 'https://r/a.tgz#abc'\n  permissions: {x: y}\n",
  'a@1:\n' + R + '  prebuiltVariants "ab"\n',
  'a@1:\n' + R + '  prebuiltVariants:\n    x y\n',
  'a@1:\n' + R + '  dependencies "ab"\n',
  'a@1:\n' + R + '  optionalDependencies "ab"\n',
  "'a@1':\n  version: '1.0.0'\n  resolved: 'https://r/a.tgz#abc'\n  dependencies: [p, q]\n",
  'a@1:\n' + R + '  dependencies:\n    toString "1"\n    b "2"\n',
  "'a@1':\n  version: '1.0.0'\n  resolved: 'https://r/a.tgz#abc'\n  integrity: {toString: x}\n",
  "'a@1':\n  version: '1.0.0'\n  resolved: 'https://r/a.tgz#abc'\n  integrity: [{toString: x}]\n",
  'a@1, a@2:\n' + R + '  permissions:\n    x true\n',
  'a@1:\n' + R + 'a@2:\n' + R + '  permissions:\n    x true\n',
  'a@1:\n' + R + '  version "2.0.0"\n',
  'a@1:\n  version "1.0.0"\n  resolved "https://r/a.tgz#abc"\n  uid ""\n',
  'a@1:\n  version "1.0.0"\n  resolved "https://r/a.tgz#abc"\n  name b\n',
  'a@1:\n  version "1.0.0"\n  resolved "https://r/a.tgz#abc"\n  name a\n',
  '"@s/a@1":\n  version "1.0.0"\n  resolved "https://r/a.tgz#abc"\n  name "@s/a"\n',
]
const rows = ['# yarn 1.22.22\'s own parse, Lockfile.getLocked, PackageRequest.getLocked and a PackageReference with ' +
  'addPattern for every pattern, getLockfile and stringify over each lock: lock, then {"wrote": text} or ' +
  '{"threw": "<name>: <message>"} (JSON)']
for (const body of cases) {
  const text = head + body
  let outcome
  try {
    const lockfile = new Lockfile({cache: parse(text, 'yarn.lock').object, source: text})
    const resolver = {patterns: {}, addPattern(p, m) { this.patterns[p] = m }, removePattern() {}}
    for (const pattern of Object.keys(lockfile.cache).sort()) {
      const req = Object.create(PackageRequest.prototype)
      req.lockfile = lockfile
      req.pattern = pattern
      req.resolver = resolver
      req.parentNames = []
      const info = req.getLocked('tarball')
      if (!info) continue
      const ref = new PackageReference(req, info, info._remote)
      ref.addPattern(pattern, info)
      info._reference = ref
    }
    outcome = {wrote: stringify(lockfile.getLockfile(resolver.patterns), false, false)}
  } catch (e) {
    outcome = {threw: e.name + ': ' + e.message}
  }
  rows.push(JSON.stringify(text) + '\t' + JSON.stringify(outcome))
}
process.stdout.write(rows.join('\n') + '\n')
