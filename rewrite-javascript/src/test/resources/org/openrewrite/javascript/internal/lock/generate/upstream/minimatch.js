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
// minimatch 3.0.4 and 10.2.5 (MINIMATCH_10: npm 11.17.0's copy) for upstream-calls.js: minimatch(), match, makeRe,
// filter, braceExpand, a Minimatch's match and makeRe (its receiver as its pattern and options).
const fs = require('fs')
const path = require('path')

const v10 = release => release.startsWith('10.')
const FUNCTIONS = ['match', 'makeRe', 'filter', 'braceExpand', 'escape', 'unescape']

module.exports = {
  library: 'minimatch',
  entry: (dir, release) => v10(release) ? process.env.MINIMATCH_10 : 'minimatch.js',
  esmEntry: /^(dist\/esm|src)\/index\.js$/,
  esm: v10,
  tests: (dir, release) => fs.readdirSync(path.join(dir, 'test'))
    .filter(f => /\.(js|ts)$/.test(f) && f !== 'patterns.js' && !(v10(release) && f === 'bash-results.js')).sort()
    .map(f => 'test/' + f),
  receiver(self) {
    return self && self.__upstreamArgs ? {pattern: self.__upstreamArgs[0], options: self.__upstreamArgs[1]} :
      undefined
  },
  wrap(real, {recorded}) {
    const mm = typeof real === 'function' ? real : real.minimatch
    const M = mm.Minimatch
    let Recorded
    if (/^class /.test(Function.prototype.toString.call(M))) {
      Recorded = class extends M {
        constructor(pattern, options) {
          super(pattern, options)
          this.__upstreamArgs = [pattern, options]
        }
      }
    } else {
      Recorded = function (pattern, options) {
        const instance = new M(pattern, options)
        instance.__upstreamArgs = [pattern, options]
        return instance
      }
      Recorded.prototype = M.prototype
      Object.setPrototypeOf(Recorded, M)
    }
    for (const m of ['match', 'makeRe']) {
      M.prototype[m] = recorded('Minimatch#' + m, M.prototype[m])
    }
    const w = recorded('minimatch', mm)
    for (const k of Object.getOwnPropertyNames(mm)) {
      if (!['length', 'name', 'prototype', 'caller', 'arguments'].includes(k)) {
        w[k] = FUNCTIONS.includes(k) && typeof mm[k] === 'function' ? recorded(k, mm[k]) : mm[k]
      }
    }
    w.Minimatch = Recorded
    if (typeof real === 'function') {
      return w
    }
    const out = {}
    for (const k of Object.keys(real)) {
      out[k] = k === 'minimatch' || k === 'default' ? w : k === 'Minimatch' ? Recorded :
        FUNCTIONS.includes(k) ? w[k] : real[k]
    }
    return out
  }
}
