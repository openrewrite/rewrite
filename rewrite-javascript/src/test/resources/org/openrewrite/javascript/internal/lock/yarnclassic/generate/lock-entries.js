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
// Writes lock-entries.tsv: yarn 1.22.22's own parse, Lockfile, PackageRequest.getLocked of every pattern,
// getLockfile and stringify over locks whose fields have odd types, each lock beside what yarn wrote or threw.
//
// Usage: node lock-entries.js <yarn 1.22.22>/lib/cli.js > lock-entries.tsv
'use strict'
const yarn = require('./yarn-bundle.js')(process.argv[2])
const parse = yarn.load(/var VERSION_REGEX = \/\^yarn lockfile v/).default
const stringify = yarn.load(/lines\.push\(`# yarn lockfile v/).default
const Lockfile = yarn.load(/Lockfile.prototype.getLockfile = function getLockfile/).default
const PackageRequest = yarn.load(/PackageRequest.prototype.getLocked = function getLocked/).default
const head = '# yarn lockfile v1\n\n\n'
const cases = [
  'a@1:\n  version "1.0.0"\n  resolved "https://r/a-1.0.0.tgz#abc"\n  registry true\n',
  'a@1:\n  version "1.0.0"\n  resolved "https://r/a-1.0.0.tgz#abc"\n  registry 5\n',
  'a@1:\n  version "1.0.0"\n  resolved "https://r/a-1.0.0.tgz#abc"\n  registry yarn\n',
  'a@1:\n  version "1.0.0"\n  resolved 5\n',
  'a@1:\n  version "1.0.0"\n  resolved true\n',
  'a@1:\n  version "1.0.0"\n  resolved:\n    x y\n',
  'a@1:\n  version 1\n  resolved "https://r/a-1.0.0.tgz#abc"\n',
  'a@1:\n  version "1.0.0"\n  uid 7\n  resolved "https://r/a-1.0.0.tgz#abc"\n',
  'a@1:\n  version "1.0.0"\n  resolved "https://r/a-1.0.0.tgz#abc"\n  integrity:\n    x y\n',
  'a@1:\n  version "1.0.0"\n  resolved "https://r/a-1.0.0.tgz#abc"\n  dependencies "x"\n',
  'a@1:\n  name 5\n  version "1.0.0"\n  resolved "https://r/a-1.0.0.tgz#abc"\n',
  'a@1:\n  name:\n    x y\n  version "1.0.0"\n  resolved "https://r/a-1.0.0.tgz#abc"\n',
  'a@1:\n  version "1.0.0"\n  resolved "git+ssh://git@h/a.git#abc"\n',
  'a@1:\n  version "1.0.0"\n  resolved "https://r/a.tgz#abc#def"\n',
  'a@1:\n  version "1.0.0"\n  resolved "https://r/a.tgz"\n',
  'a@1:\n  version "1.0.0"\n  resolved "https://r/a.tgz#abc"\n  integrity 5\n',
  'a@1:\n  version "1.0.0"\n  resolved "https://r/a.tgz#abc"\n  integrity 1.50\n',
  'a@1:\n  version "1.0.0"\n  resolved "https://r/a.tgz#abc"\n  integrity true\n',
  'a@1:\n  version "1.0.0"\n  resolved "https://r/a.tgz#abc"\n  integrity "sha512-b sha1-a"\n',
  'a@1:\n  version 1.5\n  resolved "https://r/a.tgz#abc"\n',
  'a@1:\n  version "1.0.0"\n  uid 1.50\n  resolved "https://r/a.tgz#abc"\n',
  "'a@1':\n  version: '1.0.0'\n  resolved: 'https://r/a.tgz#abc'\n  integrity: {x: y}\n",
  "'a@1':\n  version: '1.0.0'\n  resolved: 'https://r/a.tgz#abc'\n  integrity: [x, y]\n",
  "'a@1':\n  version: '1.0.0'\n  resolved: [x]\n",
  // explodeEntry's ssri.parse of an integrity with isIntegrity
  'a@1:\n  version "1.0.0"\n  resolved "https://r/a.tgz#abc"\n  integrity:\n    isIntegrity true\n    algorithm sha512\n    digest abc\n',
  'a@1:\n  version "1.0.0"\n  resolved "https://r/a.tgz#abc"\n  integrity:\n    isIntegrity true\n    sha512 "x"\n',
  'a@1:\n  version "1.0.0"\n  resolved "https://r/a.tgz#abc"\n  integrity:\n    isIntegrity true\n    algorithm sha1\n    digest abc\n    options "x"\n',
  'a@1:\n  version "1.0.0"\n  resolved "https://r/a.tgz#abc"\n  integrity:\n    isIntegrity true\n    algorithm:\n      x y\n    digest abc\n',
  'a@1:\n  version "1.0.0"\n  resolved "https://r/a.tgz#abc"\n  integrity:\n    isIntegrity false\n    x y\n',
  'a@1:\n  version "1.0.0"\n  resolved "https://r/a.tgz#abc"\n  integrity:\n    isIntegrity true\n    algorithm 5\n    digest 7\n',
  'a@1:\n  version "1.0.0"\n  resolved "https://r/a.tgz#abc"\n  integrity:\n    isIntegrity true\n    algorithm sha1\n    digest abc\n    options 5\n',
  'a@1:\n  version "1.0.0"\n  resolved "https://r/a.tgz#abc"\n  integrity:\n    isIntegrity true\n',
  'a@1:\n  version "1.0.0"\n  resolved "https://r/a.tgz#abc"\n  integrity:\n    isIntegrity "1"\n    algorithm "sha512"\n    digest "b a"\n',
  "'a@1':\n  version: '1.0.0'\n  resolved: 'https://r/a.tgz#abc'\n  integrity: {isIntegrity: x, algorithm: sha512, digest: abc}\n",
  "'a@1':\n  version: '1.0.0'\n  resolved: 'https://r/a.tgz#abc'\n  integrity: {isIntegrity: [a, {algorithm: sha1, digest: d}], sha512: [b]}\n",
  "'a@1':\n  version: '1.0.0'\n  resolved: 'https://r/a.tgz#abc'\n  integrity: {isIntegrity: x, algorithm: {a: b}, digest: d}\n",
  "'a@1':\n  version: '1.0.0'\n  resolved: 'https://r/a.tgz#abc'\n  integrity: {isIntegrity: x, algorithm: sha1, digest: d, options: [p, q]}\n",
  "'a@1':\n  version: '1.0.0'\n  resolved: 'https://r/a.tgz#abc'\n  integrity: {isIntegrity: x, algorithm: sha1, digest: d, options: ''}\n",
  "'a@1':\n  version: '1.0.0'\n  resolved: 'https://r/a.tgz#abc'\n  integrity: {isIntegrity: x, algorithm: sha1, digest: d, options: [p, [q, r]]}\n",
  "'a@1':\n  version: '1.0.0'\n  resolved: 'https://r/a.tgz#abc'\n  integrity: {isIntegrity: x, algorithm: sha1, digest: d, options: {length: 1}}\n",
  "'a@1':\n  version: '1.0.0'\n  resolved: 'https://r/a.tgz#abc'\n  integrity:\n    isIntegrity:\n      -\n",
  "'a@1':\n  version: '1.0.0'\n  resolved: 'https://r/a.tgz#abc'\n  integrity: {isIntegrity: [{digest: d, algorithm: }]}\n",
  "'a@1':\n  version: '1.0.0'\n  resolved: 'https://r/a.tgz#abc'\n  integrity: {isIntegrity: [{options: [x]}], 0: [{algorithm: z, digest: y}]}\n",
  "'a@1':\n  version: '1.0.0'\n  resolved: 'https://r/a.tgz#abc'\n  integrity: {isIntegrity: x, sha512: }\n",
  "'a@1':\n  version: '1.0.0'\n  resolved: 'https://r/a.tgz#abc'\n  integrity: [sha1-x, {a: b}]\n",
  'a@1:\n  version "1.0.0"\n  resolved "https://r/a.tgz#abc"\n  integrity false\n',
  'a@1:\n  version "1.0.0"\n  resolved "https://r/a.tgz#abc"\n  integrity 1e21\n',
  'a@1:\n  version "1.0.0"\n  resolved "https://r/a.tgz#abc"\n  registry false\n',
  'a@1:\n  version "1.0.0"\n  resolved "https://r/a.tgz#abc"\n  registry:\n    x y\n',
  "'a@1':\n  version: '1.0.0'\n  resolved: 'https://r/a.tgz#abc'\n  registry: [npm]\n",
]
const rows = ['# yarn 1.22.22\'s own parse, Lockfile.getLocked, PackageRequest.getLocked of every pattern, ' +
  'getLockfile and stringify over each lock: lock, then {"wrote": text} or {"threw": "<name>: <message>"} (JSON)']
for (const body of cases) {
  const text = head + body
  let outcome
  try {
    const lockfile = new Lockfile({cache: parse(text, 'yarn.lock').object, source: text})
    const patterns = {}
    for (const pattern of Object.keys(lockfile.cache).sort()) {
      const req = Object.create(PackageRequest.prototype)
      req.lockfile = lockfile
      req.pattern = pattern
      const info = req.getLocked('tarball')
      if (!info) continue
      info._reference = {permissions: {}}
      patterns[pattern] = info
    }
    outcome = {wrote: stringify(lockfile.getLockfile(patterns), false, false)}
  } catch (e) {
    outcome = {threw: e.name + ': ' + e.message}
  }
  rows.push(JSON.stringify(text) + '\t' + JSON.stringify(outcome))
}
process.stdout.write(rows.join('\n') + '\n')
