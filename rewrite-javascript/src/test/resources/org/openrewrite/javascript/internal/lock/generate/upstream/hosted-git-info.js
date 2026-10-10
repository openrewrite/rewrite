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
// hosted-git-info 4.1.0 and 9.0.3 for upstream-calls.js: fromUrl and the GitHost methods, a GitHost written with its
// fields and the fromUrl arguments that made it ("from").
const fs = require('fs')
const path = require('path')

const METHODS = ['hash', 'ssh', 'sshurl', 'browse', 'browseFile', 'docs', 'bugs', 'https', 'git', 'shortcut', 'path',
  'tarball', 'file', 'edit', 'getDefaultRepresentation', 'toString']
const made = new WeakMap()

// where 4.1.0 and 9.0.3 differ (correctProtocol, correctUrl, sourcehut's https) and neither's tests ask
const DIFFERENCES = ['foo://user@github.com:a/b', 'git+http://github.com/a/b', 'git+http:github.com/a/b',
  'git@github.com:a/b#semver:^1:2', 'a@b@github.com:c/d', 'github.com:a/b#x@y', 'user@github.com/a/b',
  '//github.com/a/b', 'git@github.com:', 'https://git.sr.ht/~a/b#c', 'git+ssh://git.sr.ht/~a/b', 'x@y:z#a:b',
  'github.com/a/b', 'git@gitlab.com:a/b/c.git#d', 'ssh://git@bitbucket.org:a/b']

function isGitHost(v) {
  return v && typeof v === 'object' && v.constructor && v.constructor.name === 'GitHost'
}

module.exports = {
  library: 'hosted-git-info',
  entry: (dir, release) => release.startsWith('4.') ? 'index.js' : 'lib/index.js',
  tests: dir => fs.readdirSync(path.join(dir, 'test')).filter(f => f.endsWith('.js')).sort().map(f => 'test/' + f),
  differences: DIFFERENCES.map(url => hgi => {
    const host = hgi.fromUrl(url)
    return host && [host.https(), host.toString(), host.sshurl({noGitPlus: true})]
  }),
  encode(v, encode) {
    if (!isGitHost(v)) {
      return undefined
    }
    return {$: 'githost', from: encode(made.get(v)), type: v.type, user: encode(v.user), auth: encode(v.auth),
      project: encode(v.project), committish: encode(v.committish), default: encode(v.default)}
  },
  receiver(self) {
    return isGitHost(self) ? module.exports.encode(self, x => x === undefined ? {$: 'undefined'} : x) : undefined
  },
  wrap(real, {recorded}) {
    for (const m of METHODS) {
      if (Object.prototype.hasOwnProperty.call(real.prototype, m)) {
        real.prototype[m] = recorded('GitHost#' + m, real.prototype[m])
      }
    }
    const fromUrl = real.fromUrl
    real.fromUrl = recorded('fromUrl', function (giturl, opts) {
      const out = fromUrl.call(real, giturl, opts)
      if (isGitHost(out) && !made.has(out)) {
        made.set(out, [giturl, opts === undefined ? null : opts])
      }
      return out
    })
    return real
  }
}
