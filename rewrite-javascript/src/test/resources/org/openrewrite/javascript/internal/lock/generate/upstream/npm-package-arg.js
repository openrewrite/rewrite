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
// npm-package-arg 13.0.2 and the releases npm bundled before it, and @pnpm/npm-package-arg 2.0.0 (release pnpm-2.0.0)
// for upstream-calls.js: npa, resolve, toPurl and Result#toString, a Result with its fields and its hosted's; the home
// directory as <home>. Run each release with NODE_PATH naming the node_modules of an npm that bundles it (8.1.1 with
// npm 7.15.1's, which bundles 8.1.2, whose npa.js is 8.1.1's, beside the hosted-git-info 4.0.2 the port gives it).
const fs = require('fs')
const os = require('os')
const path = require('path')

// where the fork and 13.0.2 differ and neither's tests ask
const DIFFERENCES = ['foo@', 'foo', 'foo.tarxgz', 'foo@npm:bar@^1', 'foo@NPM:bar@^1', 'git+ssh://git@host:12abc/repo',
  'foo@ 1.2.3 ', 'foo@>=^4', '@a/.b@1', '-a@1', 'git@github.com:foo/bar', 'foo@file:///../x', 'foo@~/x',
  'foo@C:\\x', 'foo@user@example.com/repo', 'foo@a/b', 'foo@github:a/b#semver:^1', 'foo@git+http://x.com/a.git']

const FIELDS = ['type', 'registry', 'where', 'raw', 'name', 'escapedName', 'scope', 'rawSpec', 'saveSpec', 'fetchSpec',
  'gitRange', 'gitCommittish', 'gitSubdir', 'alias']

function isResult(v) {
  return v && typeof v === 'object' && 'rawSpec' in v && 'raw' in v && typeof v.toString === 'function' &&
    v.toString !== Object.prototype.toString
}

module.exports = {
  library: 'npm-package-arg',
  // npm-package-arg 8 and the fork keep npa.js at the package root
  entry: (dir, release) => release.startsWith('pnpm') || release.startsWith('8.') ? 'npa.js' : 'lib/npa.js',
  self: 'npa',
  functions: ['resolve', 'toPurl'],
  tests: dir => fs.readdirSync(path.join(dir, 'test')).filter(f => f.endsWith('.js')).sort().map(f => 'test/' + f),
  // tap's mock of the module the tests load (13.0.2 mocks node:path with itself)
  shims(dir, release, wrapped) {
    const tap = require('../upstream-harness.js').tap
    return {tap: new Proxy(tap, {get: (target, prop) => prop === 'mock' ? () => wrapped : target[prop]})}
  },
  differences: DIFFERENCES.map(arg => npa => npa(arg, '/w')),
  encode(v, encode) {
    if (!isResult(v)) {
      return undefined
    }
    const out = {$: 'result'}
    for (const k of FIELDS) {
      if (k in v) {
        out[k] = encode(v[k])
      }
    }
    out.hosted = v.hosted ? {type: v.hosted.type, user: encode(v.hosted.user), project: encode(v.hosted.project),
      committish: encode(v.hosted.committish), default: encode(v.hosted.default)} : encode(v.hosted)
    return out
  },
  receiver(self) {
    return isResult(self) ? module.exports.encode(self, x => x === undefined ? {$: 'undefined'} : x) : undefined
  },
  wrap(real, {recorded, wrapFunctions}) {
    real.Result.prototype.toString = recorded('Result#toString', real.Result.prototype.toString)
    return wrapFunctions(real)
  },
  redact: line => line.split(JSON.stringify(os.homedir()).slice(1, -1)).join('<home>')
}
