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
// TarballFetcherTest's archives and rows: what yarn 1.22.22's TarballFetcher reads from each one served over http.
// Usage: node tarball-rows.js <yarn>/lib/cli.js <out dir> [<yarn's __tests__/fixtures/fetchers/tarball dir>]
const fs = require('fs')
const http = require('http')
const os = require('os')
const path = require('path')
const zlib = require('zlib')
const Module = require('module')

const [cli, out, upstream] = process.argv.slice(2)
const scratch = fs.mkdtempSync(path.join(os.tmpdir(), 'tarball-rows-'))
process.env.HOME = path.join(scratch, 'home')
fs.mkdirSync(process.env.HOME)

let src = fs.readFileSync(cli, 'utf8').replace(/^#!.*\n/, '')
src = src.replace(/return __webpack_require__\(__webpack_require__\.s = \d+\);/,
  'global.__yarnRequire = __webpack_require__; return {};')
const m = new Module(cli, null)
m.filename = cli
m.paths = Module._nodeModulePaths(path.dirname(cli))
m._compile(src, cli)
const req = global.__yarnRequire
const find = re => {
  const ids = []
  for (let i = 0; i < req.m.length; i++) if (req.m[i] && re.test(req.m[i].toString())) ids.push(i)
  return ids
}
const fetchers = req(find(/fetchBadHashWithPath/)[0])
const Config = req(find(/workspacesFocusRootCheck/).find(i => req(i).default && req(i).default.create)).default
const reporters = req(find(/BufferReporter/).find(i => req(i).Reporter && req(i).NoopReporter))

const header = (name, size, type, linkname = '', prefix = '') => {
  const b = Buffer.alloc(512)
  b.write(name, 0)
  b.write('0000644\0', 100)
  b.write('0000000\0', 108)
  b.write('0000000\0', 116)
  b.write(size.toString(8).padStart(11, '0') + '\0', 124)
  b.write('00000000000\0', 136)
  b.write('        ', 148)
  b.write(type, 156)
  b.write(linkname, 157)
  b.write('ustar\0' + '00', 257)
  b.write(prefix, 345)
  let sum = 0
  for (const x of b) sum += x
  b.write(sum.toString(8).padStart(6, '0') + '\0 ', 148)
  return b
}
const pad = data => Buffer.concat([data, Buffer.alloc((512 - data.length % 512) % 512)])
const entry = (name, content = '', type = '0', linkname = '', prefix = '') => {
  const data = Buffer.from(content)
  return Buffer.concat([header(name, data.length, type, linkname, prefix), pad(data)])
}
const end = Buffer.alloc(1024)
const tar = (...entries) => Buffer.concat([...entries, end])
const pkg = JSON.stringify({name: 'a', version: '1.0.0', dependencies: {b: '^1.0.0'}})
const pax = record => {
  const line = ' ' + record + '\n'
  let total = line.length + String(line.length).length
  if (String(total).length > String(line.length).length) total++
  return total + line
}

const cases = {
  'plain': tar(entry('package/package.json', pkg)),
  'other-top-directory': tar(entry('node/package.json', pkg)),
  'dot-slash-prefix': tar(entry('./package/package.json', pkg)),
  'no-manifest': tar(entry('package/index.js', 'x')),
  'empty': end,
  'yarn-json': tar(entry('package/yarn.json', pkg)),
  'package-and-yarn-json': tar(entry('package/yarn.json', JSON.stringify({name: 'y', version: '2.0.0'})),
    entry('package/package.json', pkg)),
  'metadata-manifest': tar(entry('package/.yarn-metadata.json',
    JSON.stringify({manifest: {name: 'm', version: '3.0.0', dependencies: {c: '1'}}})),
    entry('package/package.json', pkg)),
  'metadata-without-manifest': tar(entry('package/.yarn-metadata.json', '{}'), entry('package/package.json', pkg)),
  'metadata-manifest-null': tar(entry('package/.yarn-metadata.json', '{"manifest":null}')),
  'metadata-bin-number': tar(entry('package/.yarn-metadata.json', '{"manifest":{"name":"m","bin":{"x":5}}}')),
  'later-entry-wins': tar(entry('package/package.json', JSON.stringify({name: 'first', version: '1.0.0'})),
    entry('package/package.json', pkg)),
  'pax-path': tar(entry('PaxHeader', pax('path=package/package.json'), 'x'), entry('ignored', pkg)),
  'gnu-long-name': tar(entry('././@LongLink', 'package/package.json\0', 'L'), entry('ignored', pkg)),
  'ustar-prefix': tar(entry('package.json', pkg, '0', '', 'package')),
  'symlink': tar(entry('package/real.json', pkg), entry('package/package.json', '', '2', 'real.json')),
  'symlink-absolute': tar(entry('package/real.json', pkg),
    entry('package/package.json', '', '2', '/package/real.json')),
  'symlink-dangling': tar(entry('package/package.json', '', '2', 'missing.json')),
  'hard-link': tar(entry('package/real.json', pkg), entry('package/package.json', '', '1', 'package/real.json')),
  'hard-link-below': tar(entry('package/lib/real.json', pkg),
    entry('package/lib/x.json', '', '1', 'package/lib/real.json')),
  'manifest-directory': tar(entry('package/package.json/', '', '5')),
  'path-through-file': tar(entry('package/lib', 'x'), entry('package/lib/a.js', 'y')),
  'unsupported-type': tar(entry('package/fifo', '', '6')),
  'bad-checksum': Buffer.concat([Buffer.from(entry('package/package.json', pkg)).fill(1, 148, 150), end]),
  'truncated': entry('package/package.json', pkg).subarray(0, 600),
  'invalid-json': tar(entry('package/package.json', '{"name": ')),
  'number-json': tar(entry('package/package.json', '5')),
  'version-cleaned': tar(entry('package/package.json', JSON.stringify({name: 'a', version: 'v1.0.0'}))),
  'bom': tar(entry('package/package.json', '﻿' + pkg)),
  'optional-dependencies': tar(entry('package/package.json',
    JSON.stringify({name: 'a', version: '1.0.0', optionalDependencies: {c: '1'}, dependencies: {c: '2'}}))),
}
const gzip = buf => zlib.gzipSync(buf)
const archives = {}
for (const [name, data] of Object.entries(cases)) archives[name] = gzip(data)
archives['ungzipped'] = cases['plain']
archives['gzip-twice'] = gzip(gzip(cases['plain']))
archives['deflate'] = zlib.deflateSync(cases['plain'])
archives['gzip-five-times'] = gzip(gzip(gzip(gzip(gzip(cases['plain'])))))
archives['not-gzip'] = Buffer.from([0x1f, 0x8b, 0x08, 0, 1, 2, 3])
if (upstream) {
  for (const f of ['broken-tar-data.tgz', 'broken-tar-header.tgz', 'ungzipped.tar']) {
    archives['upstream-' + f.replace(/\.(tgz|tar)$/, '')] = fs.readFileSync(path.join(upstream, f))
  }
}

const main = async () => {
  fs.mkdirSync(out, {recursive: true})
  const project = path.join(scratch, 'project')
  fs.mkdirSync(project)
  fs.writeFileSync(path.join(project, 'package.json'), '{}')
  const config = await Config.create({cwd: project, cacheFolder: path.join(scratch, 'cache')},
    new reporters.Reporter())
  const server = http.createServer((q, s) => s.end(archives[decodeURIComponent(q.url.slice(1))]))
  await new Promise(resolve => server.listen(0, '127.0.0.1', resolve))
  for (const [name, data] of Object.entries(archives)) {
    const file = path.join(out, name + '.tgz')
    fs.writeFileSync(file, data)
    const dest = path.join(scratch, 'dest', name)
    fs.mkdirSync(dest, {recursive: true})
    const url = `http://127.0.0.1:${server.address().port}/${name}`
    const fetcher = new fetchers.default(dest, {type: 'tarball', reference: url, registry: 'npm'}, config)
    let row
    try {
      const {package: p} = await fetcher.fetch({name: 'fallback', version: '9.9.9'})
      row = 'ok ' + JSON.stringify(p == null ? null :
        {name: p.name, version: p.version, dependencies: p.dependencies, optionalDependencies: p.optionalDependencies})
    } catch (e) {
      const message = String(e.message).split(dest + '/').join('').split(url).join('<url>')
      row = (e.constructor && e.constructor.name === 'MessageError' ? 'MessageError' : e.name) + ' ' + message
    }
    console.log(name + '\t' + row.replace(/\s+/g, ' '))
  }
  server.close()
  fs.rmSync(scratch, {recursive: true, force: true})
}
main().catch(e => {
  console.error(e)
  process.exit(1)
})
