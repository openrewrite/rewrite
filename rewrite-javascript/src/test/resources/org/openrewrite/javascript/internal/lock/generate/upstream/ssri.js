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
// ssri 5.3.0 and 13.0.1 for upstream-calls.js: the module's functions and the Integrity and Hash methods, an Integrity
// or Hash written as {"$": "integrity" | "hash", "string": its toString(), ...}.
const fs = require('fs')
const path = require('path')

const METHODS = ['match', 'merge', 'concat', 'hexDigest', 'pickAlgorithm', 'isEmpty', 'toString', 'toJSON']

module.exports = {
  library: 'ssri',
  entry: dir => JSON.parse(fs.readFileSync(path.join(dir, 'package.json'), 'utf8')).main || 'index.js',
  functions: ['parse', 'stringify', 'fromHex', 'fromData', 'fromStream', 'checkData', 'checkStream',
    'integrityStream', 'create'],
  mutates: ['Integrity#merge'],
  tests: dir => fs.readdirSync(path.join(dir, 'test')).filter(f => f.endsWith('.js')).sort().map(f => 'test/' + f),
  encode(v, encode) {
    if (v.isIntegrity) {
      return {$: 'integrity', string: String(v), algorithms: Object.keys(v)}
    }
    if (v.isHash) {
      return {$: 'hash', string: String(v), algorithm: encode(v.algorithm), digest: encode(v.digest),
        options: encode(v.options), source: encode(v.source)}
    }
    if (typeof v.pipe === 'function') {
      return {$: 'stream'}
    }
    return undefined
  },
  receiver(self) {
    return self && (self.isIntegrity || self.isHash) ? module.exports.encode(self, x => x) : undefined
  },
  wrap(real, {recorded, wrapFunctions}) {
    const integrity = real.parse('sha512-' + 'A'.repeat(86) + '==')
    for (const cls of [integrity.constructor, integrity.sha512[0].constructor]) {
      const name = cls === integrity.constructor ? 'Integrity' : 'Hash'
      for (const m of METHODS) {
        if (Object.prototype.hasOwnProperty.call(cls.prototype, m)) {
          cls.prototype[m] = recorded(name + '#' + m, cls.prototype[m])
        }
      }
    }
    return wrapFunctions(real)
  }
}
