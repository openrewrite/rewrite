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
// inherited-name-maps.jsonl: yarn 1.22.22's stringify of maps holding several Object.prototype names, one per line.
// Usage: node inherited-name-maps.js <yarn 1.22.22>/lib/cli.js > inherited-name-maps.jsonl
'use strict'
const yarn = require('./yarn-bundle.js')(process.argv[2])
const stringify = yarn.load(/lines\.push\(`# yarn lockfile v/).default
const proto = ['constructor', 'toString', 'valueOf', 'hasOwnProperty', 'isPrototypeOf', 'propertyIsEnumerable',
  'toLocaleString', '__defineGetter__', '__lookupSetter__']
const prio = ['name', 'version', 'uid', 'resolved', 'integrity', 'registry', 'dependencies']
let seed = 7
const rnd = () => {
  seed = (seed * 1103515245 + 12345) % 2147483648
  return seed / 2147483648
}
for (let c = 0; c < 60; c++) {
  const n = c % 2 === 0 ? 2 + Math.floor(rnd() * 20) : 64 + Math.floor(rnd() * 90)
  const keys = []
  while (keys.filter(k => proto.includes(k)).length < 2) {
    const k = proto[Math.floor(rnd() * proto.length)]
    if (!keys.includes(k)) keys.push(k)
  }
  while (keys.length < n) {
    const r = rnd()
    const k = r < 0.1 ? proto[Math.floor(rnd() * proto.length)] : r < 0.2 ? prio[Math.floor(rnd() * prio.length)] :
      'pkg-' + Math.floor(rnd() * 1000).toString(36)
    if (!keys.includes(k)) keys.splice(Math.floor(rnd() * (keys.length + 1)), 0, k)
  }
  const deps = {}
  for (const k of keys) deps[k] = '1.0.' + Math.floor(rnd() * 5)
  const out = stringify({'a@1.0.0': {dependencies: deps}}, false, false)
  process.stdout.write(JSON.stringify({keys, values: keys.map(k => deps[k]), out}) + '\n')
}
