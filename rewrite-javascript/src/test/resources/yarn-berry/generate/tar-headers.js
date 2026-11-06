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
// node-tar-headers.tgz (a NUL before a line end, long pax numbers), node-tar-oversize.tgz (an entry past the end)
// and each one's entries in node-tar 6.1.11 as .jsonl. Usage: NODE_PATH=<tar 6.1.11> node tar-headers.js <out-dir>
const fs = require('fs')
const path = require('path')
const zlib = require('zlib')
const tar = require('tar')

function header(name, type, size, linkname) {
  const b = Buffer.alloc(512)
  b.write(name, 0, 100, 'utf8')
  b.write('0000644\0', 100)
  b.write('0000000\0', 108)
  b.write('0000000\0', 116)
  b.write(size, 124, 12, 'utf8')
  b.write('00000000000\0', 136)
  b.write('        ', 148)
  b.write(type, 156)
  if (linkname) {
    b.write(linkname, 157, 100, 'utf8')
  }
  b.write('ustar\u000000', 257, 8, 'latin1')
  let sum = 0
  for (let i = 0; i < 512; i++) {
    sum += b[i]
  }
  b.write(sum.toString(8).padStart(6, '0') + '\0 ', 148, 8, 'latin1')
  return b
}

function body(text) {
  const data = Buffer.from(text, 'utf8')
  return Buffer.concat([data, Buffer.alloc((512 - data.length % 512) % 512)])
}

function pax(records) {
  const text = records.map(([k, v]) => {
    const rest = ' ' + k + '=' + v + '\n'
    let n = Buffer.byteLength(rest) + 1
    while (String(n).length + Buffer.byteLength(rest) !== n) {
      n++
    }
    return n + rest
  }).join('')
  return [header('PaxHeader', 'x', Buffer.byteLength(text).toString(8).padStart(11, '0') + '\0'), body(text)]
}

const archive = zlib.gzipSync(Buffer.concat([
  header('package/a\0x\ny', '0', '00000000003\0'), body('abc'),
  header('package/L', 'L', '00000000033\0'), body('package/long\0junk\nrest'),
  header('ignored', '0', '00000000001\0'), body('z'),
  header('package/c', '2', '00000000000\0', 'target\0junk\nmore'),
  ...pax([['size', '000000000000000000000000004'], ['path', 'package/pax']]), header('x', '0', '00000000001\0'),
  body('wxyz'),
  ...pax([['size', '99999999999999999999']]), header('package/big', '0', '00000000005\0'), body('hello'),
  Buffer.alloc(1024),
]))

// a pax size past the archive, then a header node-tar reads as part of that entry's body
const oversize = zlib.gzipSync(Buffer.concat([
  ...pax([['size', '99999999999999999999']]), header('package/big', '0', '00000000005\0'), body('hello'),
  header('package/after', '0', '00000000001\0'), body('x'),
  Buffer.alloc(1024),
]))

function record(name, tgz) {
  const entries = []
  const parser = new tar.Parse()
  parser.on('entry', entry => {
    const chunks = []
    entry.on('data', c => chunks.push(c))
    entry.on('end', () => {
      const data = Buffer.concat(chunks)
      entries.push({type: entry.type, path: entry.path, linkpath: entry.linkpath || null, length: data.length,
        text: data.toString('utf8').replace(/\0+$/, '')})
    })
  })
  parser.on('end', () => {
    const out = process.argv[2]
    fs.writeFileSync(path.join(out, name + '.tgz'), tgz)
    fs.writeFileSync(path.join(out, name + '.jsonl'), entries.map(e => JSON.stringify(e)).join('\n') + '\n')
  })
  parser.end(tgz)
}

record('node-tar-headers', archive)
record('node-tar-oversize', oversize)
