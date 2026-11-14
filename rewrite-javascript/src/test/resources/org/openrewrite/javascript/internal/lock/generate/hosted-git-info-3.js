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
// GitHostTest's and ConsistentResolveTest's rows for hosted-git-info 3: how npm 7.4.3 (hosted-git-info 3.0.7) and npm
// 7.6.3 (3.0.8) read git specs of every form: npm-package-arg's resolve (type, saveSpec, fetchSpec, gitCommittish,
// gitRange), the GitHost it is given ({noGitPlus: true, noCommittish: true}: its fields, then sshurl, https and ssh
// with the committish, ssh without, shortcut, git, path, and toString as the saveSpec and as the fetchSpec), and
// arborist's consistent-resolve. --wide writes a larger corpus, to check the port with and not to keep.
// Usage: node hosted-git-info-3.js <pm/npm dir> [--wide] | gzip -9n > ../upstream/hosted-git-info-3.jsonl.gz
const path = require('path')
process.removeAllListeners('warning')
const [, , pmArg, wide] = process.argv
const pm = path.resolve(pmArg)
// a drive-letter spec stays relative, and path.relative reads it from the working directory, which the port's is /
process.chdir('/')
const RELEASES = ['7.4.3', '7.6.3']
const load = (r, m) => require(path.join(pm, r, 'package/node_modules', m))
const sides = RELEASES.map(r => ({
  release: r, npa: load(r, 'npm-package-arg'), hgi: load(r, 'hosted-git-info'),
  cr: load(r, '@npmcli/arborist/lib/consistent-resolve.js')
}))

const sha = '0123456789abcdef0123456789abcdef01234567'
const hosts = wide ? ['github.com', 'gitlab.com', 'bitbucket.org', 'gist.github.com', 'www.github.com',
  'www.gitlab.com', 'GitHub.com', 'github.com:22', 'example.com', 'gitlab.example.com'] :
  ['github.com', 'gitlab.com', 'bitbucket.org', 'gist.github.com', 'www.github.com', 'example.com']
const paths = ['u/r', 'u/r.git', 'g/s/r', 'g/s/r.git', 'g/s/t/r.git', 'u', 'r.git', 'u/r/', 'u/r.git/', '12345/r.git',
  'u/re%20po.git', 'u/r?x=1', 'u/-/r', 'abcdef1234567', 'u/abcdef1234567', 'u.x/r_y.git']
const hashes = wide ? ['', '#' + sha, '#semver:^1.0.0', '#semver:1.x', '#v1.0.0', '#feature/x', '#a:b', '#', '#a%20b',
  '#a%zz'] : ['', '#' + sha, '#semver:^1.0.0', '#feature/x', '#a:b', '#', '#a%zz']
const users = wide ? ['git', 'u', 'git:pw', 'tok', ''] : ['git', 'git:pw', '']
const specs = new Set()
for (const p of paths) {
  for (const c of hashes) {
    for (const sc of ['github:', 'gitlab:', 'bitbucket:', 'gist:']) {
      specs.add(`${sc}${p}${c}`)
    }
    specs.add(`${p}${c}`)
    for (const h of hosts) {
      for (const u of users) {
        const at = u === '' ? '@' : `${u}@`
        specs.add(`${at}${h}:${p}${c}`)
        specs.add(`${at}${h}:/${p}${c}`)
        specs.add(`git+ssh://${at}${h}:${p}${c}`)
        specs.add(`git+ssh://${at}${h}/${p}${c}`)
        specs.add(`ssh://${at}${h}/${p}${c}`)
        specs.add(`git+https://${at}${h}/${p}${c}`)
      }
      specs.add(`https://${h}/${p}${c}`)
      specs.add(`git://${h}/${p}${c}`)
      specs.add(`git+http://${h}/${p}${c}`)
      specs.add(`http://${h}/${p}${c}`)
    }
  }
}
// what the shapes above leave out: auth that url.parse decodes, a committish with $ patterns or braces, a GitLab
// group path with /-/, the gist fix-up, case, and inputs url.parse refuses
for (const s of ['https://user%3An%40me:p%40ss%3Aword@github.com/u/r.git', 'https://:p%40ss@github.com/u/r.git',
  'git+https://a%zz@github.com/u/r.git', 'github:u/r#a$&b', 'github:u/r#$`x', "github:u/r#$'x", 'github:u/r#{user}',
  'github:u/r#{/committish}', 'git@github.com:u/r#{/tree/committish}', 'github:u/{project}', 'github:{user}/r',
  'gitlab:g/s/r#x', 'https://gitlab.com/g/s/-/r', 'https://gitlab.com/g/r/repository/archive.tar.gz',
  'gist:abcdef1234567', 'gist:u/abcdef1234567#x', 'gist:abcdef1', 'gist:abcdef', 'GITHUB:u/r', 'Github:u/r',
  'github:u/r.git#x', 'github:u/r.git.git', 'github:foo', 'github:/u/r', 'github:u/r\n', 'github:u/r.git\n',
  'git@github.com:u/r.git\n', 'u/r#x y', 'u/r #x', 'u /r', '-u/r', '.u/r', 'u/r/', 'git+ssh://git@github.com:u/r',
  'git+https://git@github.com:u/r', 'https://github.com:443/u/r', 'https://github.com/u/r/tree/x', 'github:u/r/x',
  'git@github.com:u/r/x', 'git@github.com', 'git@github.com:', 'git@github.com:u', 'https://github.com/u',
  'https://github.com', 'git+ssh://github.com/u/r', 'git+ssh://git@[::1]:u/r', 'https://xn--n3h.com/u/r',
  'https://☃.com/u/r', 'git@☃.com:u/r', 'bitbucket:u/r#feature/x', 'gitlab:u/r#semver:^1',
  'user@github.com/u/r', 'foo://user@github.com:u/r', 'git+http:github.com/u/r', 'a@b@github.com:c/d',
  'github.com:u/r#x@y', '//github.com/u/r', 'x@y:z#a:b', 'github.com/u/r', 'git@gitlab.com:g/s/r.git#d',
  'ssh://git@bitbucket.org:u/r', 'git@github.com:u/r#\ud800', 'github:u/r#\ud800']) {
  specs.add(s)
}

const value = v => v === undefined ? null : v
const error = e => ({ $: 'error', name: e.name, message: e.message, code: e.code === undefined ? null : e.code })
const read = (side, s) => {
  const out = {}
  try {
    const r = side.npa.resolve('dep', s, '/p')
    out.npa = [r.type, r.saveSpec, r.fetchSpec, r.gitCommittish, r.gitRange].map(value)
  } catch (e) {
    out.npa = error(e)
  }
  try {
    const h = side.hgi.fromUrl(s, { noGitPlus: true, noCommittish: true })
    if (h) {
      const call = f => {
        try {
          return value(f())
        } catch (e) {
          return error(e)
        }
      }
      const withCommittish = { noCommittish: false }
      out.hosted = [h.type, h.user, h.auth, h.project, h.committish, h.default].map(value).concat([
        call(() => h.sshurl(withCommittish)), call(() => h.https(withCommittish)),
        call(() => h.ssh({ noCommittish: false })), call(() => h.ssh({ noCommittish: true })),
        call(() => h.shortcut(withCommittish)), call(() => h.git(withCommittish)), call(() => h.path(withCommittish)),
        call(() => h.toString({ noGitPlus: false, noCommittish: false })), call(() => h.toString())])
    } else {
      out.hosted = null
    }
  } catch (e) {
    out.hosted = error(e)
  }
  try {
    out.cr = value(side.cr(s, '/p', '/p', true))
  } catch (e) {
    out.cr = error(e)
  }
  return out
}
console.log(JSON.stringify({ header: `hosted-git-info 3.0.7 (npm 7.4.3) and 3.0.8 (npm 7.6.3) over ${specs.size} ` +
    `git specs; generate/hosted-git-info-3.js <pm/npm dir>${wide ? ' --wide' : ''}` }))
for (const s of specs) {
  const out = {}
  for (const side of sides) {
    out[side.release] = read(side, s)
  }
  console.log(JSON.stringify({ test: 'hosted-git-info-3', fn: 'resolve', args: ['dep', s, '/p'], out }))
}
