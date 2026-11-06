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
// Every call a release's own tests make into it, with its answer, as JSON lines (spec: upstream/<library>.js).
// Usage: node upstream-calls.js <spec> <release checkout> <release> [<test file>...] > <rows.jsonl>
const Module = require('module')
const fs = require('fs')
const path = require('path')
const {pathToFileURL, fileURLToPath} = require('url')
const harness = require('./upstream-harness.js')

const [specPath, checkout, release, ...only] = process.argv.slice(2)
const spec = require(path.resolve(specPath))
// by its real path, which the ES module hooks see
const dir = fs.realpathSync(path.resolve(checkout))
const rows = []
let testFile = ''

// a value as JSON: undefined, RegExp, functions, non-finite numbers and errors (with js-yaml's reason) as {"$": kind}
// a value of more than LARGE nodes (aliases shared many times over) is written as {"$": "large"}
const LARGE = 200000
let nodes = 0
function encode(v, depth = 0) {
  nodes = depth === 0 ? 0 : nodes + 1
  if (nodes > LARGE) {
    return {$: 'large'}
  }
  if (depth > 40) {
    return {$: 'deep'}
  }
  if (v === undefined) {
    return {$: 'undefined'}
  }
  if (v === null || typeof v === 'string' || typeof v === 'boolean') {
    return v
  }
  if (typeof v === 'number') {
    return Number.isFinite(v) && !Object.is(v, -0) ? v : {$: 'number', value: String(Object.is(v, -0) ? '-0' : v)}
  }
  if (typeof v === 'bigint') {
    return {$: 'bigint', value: String(v)}
  }
  if (typeof v === 'function') {
    return {$: 'function', name: v.name}
  }
  if (typeof v === 'symbol') {
    return {$: 'symbol', value: String(v)}
  }
  if (v instanceof RegExp) {
    return {$: 'regexp', source: v.source, flags: v.flags}
  }
  if (v instanceof Error) {
    return {$: 'error', name: v.name, message: v.message, code: v.code, reason: v.reason}
  }
  if (Buffer.isBuffer(v)) {
    return {$: 'buffer', hex: v.toString('hex')}
  }
  if (Array.isArray(v)) {
    // an array of more than 1000 strings as its length and the SHA-256 of its JSON
    if (v.length > 1000 && v.every(x => typeof x === 'string')) {
      return {$: 'strings', length: v.length,
        sha256: require('crypto').createHash('sha256').update(JSON.stringify(v)).digest('hex')}
    }
    return v.map(x => encode(x, depth + 1))
  }
  if (spec.encode) {
    const special = spec.encode(v, x => encode(x, depth + 1))
    if (special !== undefined) {
      return special
    }
  }
  const out = {}
  for (const k of Object.keys(v)) {
    // an own __proto__ key stays a key
    Object.defineProperty(out, k,
      {value: encode(v[k], depth + 1), enumerable: true, writable: true, configurable: true})
  }
  if (Object.getPrototypeOf(v) === null) {
    return {$: 'nullproto', value: out}
  }
  return out
}

// the checkout's directory, wherever a value names it, as <checkout>, and the spec's own redactions
function redact(line) {
  line = line.split(JSON.stringify(dir).slice(1, -1)).join('<checkout>')
  return spec.redact ? spec.redact(line) : line
}

function record(row) {
  const r = Object.assign({test: testFile + (harness.state.current ? ' > ' + harness.state.current : '')}, row)
  const line = redact(JSON.stringify(r))
  if (!spec.keep || spec.keep(r)) {
    rows.push(line)
  }
}

// a recording function in the place of fn; a call made inside another recorded call is the library's own
let depth = 0
function quiet(fn) {
  depth++
  try {
    return fn()
  } finally {
    depth--
  }
}

function recorded(name, fn, self) {
  const wrapper = function (...args) {
    const outer = depth === 0
    depth++
    let row
    try {
      const encodedArgs = outer ? encode(args) : undefined
      const receiver = outer && spec.receiver && this && this !== globalThis ? spec.receiver(this) : undefined
      row = Object.assign({fn: name}, receiver !== undefined ? {self: receiver} : {}, {args: encodedArgs})
      const out = new.target ? Reflect.construct(fn, args, new.target) : fn.apply(self === undefined ? this : self,
        args)
      if (outer && out && typeof out.then === 'function') {
        return out.then(v => {
          record(Object.assign(row, {out: quiet(() => encode(v))}))
          return v
        }, e => {
          record(Object.assign(row, {threw: quiet(() => encode(e))}))
          throw e
        })
      }
      if (outer) {
        record(Object.assign(row, {out: encode(spec.result ? spec.result(name, out) : out)},
          receiver !== undefined && (spec.mutates || []).includes(name) ? {selfAfter: spec.receiver(this)} : {}))
      }
      return out
    } catch (e) {
      if (outer) {
        record(Object.assign(row, {threw: encode(e)}))
      }
      throw e
    } finally {
      depth--
    }
  }
  Object.defineProperty(wrapper, 'name', {value: fn.name})
  return wrapper
}

function wrapModule(real) {
  return spec.wrap ? spec.wrap(real, {recorded, record, encode, wrapFunctions}) : wrapFunctions(real)
}

// the module with spec.functions recorded (and the module itself, when it is a function and spec.self names it)
function wrapFunctions(real) {
  const names = spec.functions || []
  let out
  if (typeof real === 'function' && spec.self) {
    out = recorded(spec.self, real, undefined)
  } else {
    out = Object.create(Object.getPrototypeOf(real))
  }
  for (const k of Object.getOwnPropertyNames(real)) {
    if (k === 'length' || k === 'name' || k === 'prototype' || k === 'caller' || k === 'arguments') {
      continue
    }
    const desc = Object.getOwnPropertyDescriptor(real, k)
    if (names.includes(k) && typeof real[k] === 'function') {
      out[k] = recorded(spec.aliases && spec.aliases[k] || k, real[k], real)
    } else if (desc) {
      Object.defineProperty(out, k, desc)
    }
  }
  return out
}

const entryPath = require.resolve(path.resolve(dir, spec.entry(dir, release)))
const real = spec.implementation ? spec.implementation(dir, release) : require(entryPath)
const wrapped = wrapModule(real)
const entries = new Set([entryPath, ...(spec.alsoEntries ? spec.alsoEntries(dir, release) : [])
  .map(p => require.resolve(path.join(dir, p)))])
const shims = {
  tap: harness.tap, tape: harness.tape, 'node:test': harness.nodeTest, ava: harness.ava,
  should: maybe('should'), chai: maybe('chai')
}
if (spec.shims) {
  Object.assign(shims, spec.shims(dir, release, wrapped))
}

function maybe(name) {
  try {
    return require(require.resolve(name, {paths: [dir, __dirname]}))
  } catch (e) {
    return undefined
  }
}

const load = Module._load
Module._load = function (request, parent, isMain) {
  if (Object.prototype.hasOwnProperty.call(shims, request) && shims[request] !== undefined) {
    return shims[request]
  }
  let filename
  try {
    filename = Module._resolveFilename(request, parent, isMain)
  } catch (e) {
    return load.apply(this, arguments)
  }
  if (entries.has(filename)) {
    return wrapped
  }
  return load.apply(this, arguments)
}

// ES module tests: the shims and the entry through a resolve hook, the entry's sources by a regex of the spec's
globalThis.__upstreamWrapped = wrapped
globalThis.__upstreamShims = shims
if (Module.registerHooks) {
  const tmp = fs.mkdtempSync(path.join(require('os').tmpdir(), 'upstream-calls-'))
  const esm = (file, globalName, value) => {
    const names = value && (typeof value === 'object' || typeof value === 'function') ?
      Object.keys(value).filter(k => /^[A-Za-z_$][\w$]*$/.test(k) && k !== 'default') : []
    const p = path.join(tmp, file)
    fs.writeFileSync(p, `const v = ${globalName}\nexport default v\n` +
      names.map((n, i) => `const _${i} = v[${JSON.stringify(n)}]\nexport { _${i} as ${JSON.stringify(n)} }`)
        .join('\n') + '\n')
    return pathToFileURL(p).href
  }
  const shimUrls = {}
  for (const k of Object.keys(shims)) {
    if (shims[k] !== undefined) {
      shimUrls[k] = esm('shim-' + k.replace(/\W/g, '_') + '.mjs', `globalThis.__upstreamShims[${JSON.stringify(k)}]`,
        shims[k])
    }
  }
  const entryUrl = esm('entry.mjs', 'globalThis.__upstreamWrapped', wrapped)
  Module.registerHooks({
    resolve(specifier, context, next) {
      if (shimUrls[specifier]) {
        return {url: shimUrls[specifier], shortCircuit: true, format: 'module'}
      }
      if (spec.esmEntry && context.parentURL && context.parentURL.startsWith('file:') &&
        (specifier.startsWith('.') || specifier.startsWith('/'))) {
        const resolved = path.resolve(path.dirname(fileURLToPath(context.parentURL)), specifier)
        if (spec.esmEntry.test(path.relative(dir, resolved))) {
          return {url: entryUrl, shortCircuit: true, format: 'module'}
        }
      }
      return next(specifier, context)
    }
  })
}

async function main() {
  harness.installMocha()
  const tests = only.length ? only : spec.tests(dir, release)
  for (const t of tests) {
    testFile = t
    const file = path.join(dir, t)
    const cwd = process.cwd()
    process.chdir(path.dirname(file))
    try {
      if (/\.(ts|mjs)$/.test(file) || (typeof spec.esm === 'function' ? spec.esm(release) : spec.esm)) {
        await import(pathToFileURL(file).href)
      } else {
        require(file)
      }
      await harness.drain()
    } catch (e) {
      harness.state.failures.push(`${t}: load: ${e && e.stack ? e.stack.split('\n').slice(0, 3).join(' | ') : e}`)
    } finally {
      process.chdir(cwd)
    }
  }
  if (spec.differences) {
    // inputs where the releases' sources differ and no upstream test reaches
    testFile = 'differences'
    for (const call of spec.differences) {
      try {
        await call(wrapped)
      } catch (e) {
        // recorded with its error
      }
    }
  }
  const header = {header: `${spec.library} ${release}: every call its own tests (${tests.join(', ')}) make, ` +
    `with its answer; generate/upstream-calls.js generate/upstream/${path.basename(specPath)} <checkout> ${release}`}
  process.stdout.write([JSON.stringify(header), ...rows].join('\n') + '\n')
  process.stderr.write(`${rows.length} rows, ${harness.state.assertions} assertions, ` +
    `${harness.state.failures.length} failed; unknown assertions: ${[...(harness.state.unknown || [])].join(' ')}\n` +
    harness.state.failures.slice(0, 40).join('\n') + '\n')
}

main().then(() => process.exit(0), e => {
  process.stderr.write(String(e && e.stack) + '\n')
  process.exit(1)
})
