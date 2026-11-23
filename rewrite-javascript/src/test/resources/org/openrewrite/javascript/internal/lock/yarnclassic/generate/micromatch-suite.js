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
// Writes micromatch-suite.tsv: micromatch 2.3.11's own mocha suite run against yarn 1.22.22's bundled micromatch, less
// glob.js and utils.js (lib internals) and minimatch/defaults.js (a regex literal Node 24 cannot parse).
//
// Usage: node micromatch-suite.js <yarn 1.22.22>/lib/cli.js <micromatch 2.3.11 checkout, npm installed> > <tsv>
const fs = require('fs')
const os = require('os')
const path = require('path')
const {spawnSync} = require('child_process')

const [cli, checkout] = process.argv.slice(2).map(p => path.resolve(p))
const HEADER = "micromatch 2.3.11's own test suite run against yarn 1.22.22's bundled micromatch: every micromatch(), " +
  "match(), isMatch() and makeRe() call without options, with the bundle's answer (makeRe: the source toRegex gives " +
  'new RegExp)'
const tmp = fs.mkdtempSync(path.join(os.tmpdir(), 'micromatch-suite-'))
fs.writeFileSync(path.join(tmp, 'package.json'), '{"name":"micromatch","version":"2.3.11","main":"index.js"}\n')
fs.writeFileSync(path.join(tmp, 'index.js'), `module.exports = require(${JSON.stringify(
  path.join(__dirname, 'micromatch-recorder.js'))})(${JSON.stringify(cli)}, ${JSON.stringify(HEADER)})\n`)
fs.cpSync(path.join(checkout, 'test'), path.join(tmp, 'test'), {recursive: true})
fs.rmSync(path.join(tmp, 'test', 'glob.js'))
fs.rmSync(path.join(tmp, 'test', 'utils.js'))
fs.symlinkSync(path.join(checkout, 'node_modules'), path.join(tmp, 'node_modules'))
const tests = fs.readdirSync(path.join(tmp, 'test')).filter(f => f.endsWith('.js')).sort().map(f => 'test/' + f)
const rows = path.join(tmp, 'rows.tsv')
const mocha = spawnSync('node', [path.join(checkout, 'node_modules/mocha/bin/_mocha'), '--reporter', 'dot', ...tests,
  'test/minimatch/basic.js', 'test/minimatch/temp.js'], {cwd: tmp, env: {...process.env, MICROMATCH_ROWS: rows},
  encoding: 'utf8'})
console.error(`mocha exit ${mocha.status}: ` + (mocha.stdout + mocha.stderr).split('\n').filter(l =>
  /passing|failing/.test(l)).map(l => l.trim()).join(', '))
process.stdout.write(fs.readFileSync(rows, 'utf8'))
fs.rmSync(tmp, {recursive: true, force: true})
