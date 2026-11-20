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
// Loads yarn 1.22.22's bundle (lib/cli.js) without running its CLI and exposes its webpack modules, so the other
// scripts here can call yarn's own code: require('./yarn-bundle.js')(cli).load(/text unique to the module/).
const fs = require('fs')
const path = require('path')
const Module = require('module')

module.exports = cli => {
  let src = fs.readFileSync(cli, 'utf8').replace(/^#!.*\n/, '')
  const entry = /return __webpack_require__\(__webpack_require__\.s = \d+\);/
  if (!entry.test(src)) {
    throw new Error('webpack entry call not found')
  }
  src = src.replace(entry, 'global.__yarnRequire = __webpack_require__; return {};')
  const m = new Module(cli, null)
  m.filename = cli
  m.paths = Module._nodeModulePaths(path.dirname(cli))
  m._compile(src, cli)
  const req = global.__yarnRequire
  const find = pattern => {
    const ids = []
    for (let i = 0; i < req.m.length; i++) {
      if (req.m[i] && pattern.test(req.m[i].toString())) {
        ids.push(i)
      }
    }
    return ids
  }
  const load = pattern => {
    const ids = find(pattern)
    if (ids.length !== 1) {
      throw new Error('expected one module for ' + pattern + ', found ' + JSON.stringify(ids))
    }
    return req(ids[0])
  }
  return {req, find, load}
}
