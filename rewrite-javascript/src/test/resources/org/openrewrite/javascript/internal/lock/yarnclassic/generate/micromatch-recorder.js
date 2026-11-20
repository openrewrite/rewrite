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
// yarn 1.22.22's bundled micromatch 2.3.11, recording each call the port answers (micromatch(), match(), isMatch(),
// makeRe() without options; makeRe as the source toRegex gives new RegExp) and printing the rows on exit, to
// $MICROMATCH_ROWS when set. Usage: require('./micromatch-recorder.js')(<yarn lib/cli.js>, <header>) -> micromatch
const yarnBundle = require('./yarn-bundle.js')

module.exports = (cli, header) => {
  const yarn = yarnBundle(cli)
  const real = yarn.req(109)
  const regexCache = yarn.load(/function regexCache\(fn, str, opts\)/)
  const calls = []
  const RealRegExp = RegExp

  const plainOptions = opts => opts === undefined || (opts !== null && typeof opts === 'object' &&
    !Array.isArray(opts) && Object.keys(opts).every(k => k === 'cache'))
  const jsonable = v => typeof v === 'string' ||
    (Array.isArray(v) ? v.every(x => typeof x === 'string' || typeof x === 'number' || x === null) :
      typeof v === 'number' || typeof v === 'boolean' || v === null)

  function record(fn, args, run) {
    const ok = args.every((a, i) => i === args.length - 1 || jsonable(a)) && plainOptions(args[args.length - 1])
    const recorded = args.slice(0, args.length - 1)
    try {
      const out = run()
      if (ok) calls.push({fn, args: recorded, out})
      return out
    } catch (e) {
      if (ok) calls.push({fn, args: recorded, threw: e.name + ': ' + e.message})
      throw e
    }
  }

  // the string toRegex hands to new RegExp, with the regex cache emptied so it is built again
  function source(glob) {
    for (const k of Object.keys(regexCache.cache)) delete regexCache.cache[k]
    for (const k of Object.keys(regexCache.basic)) delete regexCache.basic[k]
    const sources = []
    global.RegExp = function (src, flags) {
      sources.push(src)
      return new RealRegExp(src, flags)
    }
    global.RegExp.prototype = RealRegExp.prototype
    try {
      const re = real.makeRe(glob)
      return {re, src: sources.length ? sources[sources.length - 1] : null}
    } finally {
      global.RegExp = RealRegExp
    }
  }

  const mm = (files, patterns, opts) => record('micromatch', [files, patterns, opts], () => real(files, patterns, opts))
  mm.match = (files, pattern, opts) => record('match', [files, pattern, opts], () => real.match(files, pattern, opts))
  mm.isMatch = (fp, pattern, opts) => record('isMatch', [fp, pattern, opts], () => real.isMatch(fp, pattern, opts))
  mm.makeRe = (glob, opts) => {
    if (!plainOptions(opts) || typeof glob !== 'string') return real.makeRe(glob, opts)
    let made
    try {
      made = source(glob)
    } catch (e) {
      calls.push({fn: 'makeRe', args: [glob], threw: e.name + ': ' + e.message})
      throw e
    }
    calls.push({fn: 'makeRe', args: [glob], out: made.src === null ? '$^' : made.src})
    return made.re
  }
  for (const k of ['filter', 'any', 'contains', 'matcher', 'matchKeys', 'expand', 'braces', 'braceExpand']) {
    mm[k] = real[k]
  }

  process.on('exit', () => {
    const seen = new Set()
    const rows = ['# ' + header]
    for (const c of calls) {
      const row = c.fn + '\t' + JSON.stringify(c.args) + '\t' +
        JSON.stringify('threw' in c ? {threw: c.threw} : {out: c.out})
      if (!seen.has(row)) {
        seen.add(row)
        rows.push(row)
      }
    }
    const fs = require('fs')
    if (process.env.MICROMATCH_ROWS) {
      fs.writeFileSync(process.env.MICROMATCH_ROWS, rows.join('\n') + '\n')
    } else {
      fs.writeSync(1, rows.join('\n') + '\n')
    }
  })
  return mm
}
