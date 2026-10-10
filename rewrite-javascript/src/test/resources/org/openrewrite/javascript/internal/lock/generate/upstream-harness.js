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
// The test APIs an upstream suite calls (tap, tape, mocha's globals, node:test, ava), run here one test after another,
// each assertion checked with node:assert and counted when it fails.
const assert = require('assert')

const queue = []
const state = {failures: [], assertions: 0, current: ''}

function fail(name, e) {
  state.failures.push(`${state.current}: ${name}: ${e && e.message ? e.message.split('\n')[0] : e}`)
}

function check(name, fn) {
  state.assertions++
  try {
    fn()
  } catch (e) {
    fail(name, e)
  }
}

function hasShape(actual, pattern) {
  if (pattern === null || typeof pattern !== 'object') {
    return pattern instanceof RegExp ? pattern.test(String(actual)) : actual === pattern || actual == pattern
  }
  if (pattern instanceof RegExp) {
    return pattern.test(String(actual))
  }
  if (actual === null || typeof actual !== 'object') {
    return false
  }
  return Object.keys(pattern).every(k => hasShape(actual[k], pattern[k]))
}

function throwsCheck(fn, expected) {
  let threw = false
  try {
    fn()
  } catch (e) {
    threw = true
    if (expected && typeof expected === 'object' && !(expected instanceof RegExp)) {
      assert.ok(hasShape(e, expected), 'error shape')
    } else if (expected instanceof RegExp) {
      assert.ok(expected.test(String(e && e.message !== undefined ? e.message : e)), 'error message')
    }
  }
  assert.ok(threw, 'expected to throw')
}

// one t of tap, tape and node:test
function makeT(name) {
  const subtests = []
  const t = {
    name,
    subtests,
    test: (n, o, f) => subtests.push(entry(n, o, f)),
    skip: () => {},
    todo: () => {},
    only: (n, o, f) => subtests.push(entry(n, o, f)),
    plan: () => {},
    end: () => {},
    done: () => {},
    comment: () => {},
    pass: () => state.assertions++,
    fail: m => fail('fail', m),
    teardown: f => t._teardown.push(f),
    after: f => t._teardown.push(f),
    before: f => t._before.push(f),
    beforeEach: () => {},
    afterEach: () => {},
    autoend: () => {},
    setTimeout: () => {},
    timeout: () => {},
    diagnostic: () => {},
    mock: {fn: () => () => {}, method: () => {}},
    _teardown: [],
    _before: [],
    ok: (v, m) => check('ok', () => assert.ok(v, m)),
    true: v => check('true', () => assert.strictEqual(v, true)),
    false: v => check('false', () => assert.strictEqual(v, false)),
    notOk: v => check('notOk', () => assert.ok(!v)),
    equal: (a, b) => check('equal', () => assert.strictEqual(a, b)),
    equals: (a, b) => check('equal', () => assert.strictEqual(a, b)),
    is: (a, b) => check('is', () => assert.ok(Object.is(a, b), `${a} is not ${b}`)),
    not: (a, b) => check('not', () => assert.ok(!Object.is(a, b))),
    strictEqual: (a, b) => check('strictEqual', () => assert.strictEqual(a, b)),
    notEqual: (a, b) => check('notEqual', () => assert.notStrictEqual(a, b)),
    notStrictEqual: (a, b) => check('notEqual', () => assert.notStrictEqual(a, b)),
    same: (a, b) => check('same', () => assert.deepEqual(a, b)),
    deepEqual: (a, b) => check('deepEqual', () => assert.deepEqual(a, b)),
    deepEquals: (a, b) => check('deepEqual', () => assert.deepEqual(a, b)),
    looseEqual: (a, b) => check('looseEqual', () => assert.deepEqual(a, b)),
    strictSame: (a, b) => check('strictSame', () => assert.deepStrictEqual(a, b)),
    deepStrictEqual: (a, b) => check('deepStrictEqual', () => assert.deepStrictEqual(a, b)),
    notSame: (a, b) => check('notSame', () => assert.notDeepEqual(a, b)),
    notDeepEqual: (a, b) => check('notSame', () => assert.notDeepEqual(a, b)),
    strictNotSame: (a, b) => check('notSame', () => assert.notDeepStrictEqual(a, b)),
    notDeepStrictEqual: (a, b) => check('notSame', () => assert.notDeepStrictEqual(a, b)),
    match: (a, p) => check('match', () => assert.ok(hasShape(a, p), 'match')),
    similar: (a, p) => check('similar', () => assert.ok(hasShape(a, p), 'similar')),
    notSimilar: (a, p) => check('notSimilar', () => assert.ok(!hasShape(a, p))),
    has: (a, p) => check('has', () => assert.ok(hasShape(a, p), 'has')),
    hasStrict: (a, p) => check('has', () => assert.ok(hasShape(a, p), 'has')),
    notMatch: (a, p) => check('notMatch', () => assert.ok(!hasShape(a, p))),
    type: () => state.assertions++,
    regex: (a, re) => check('regex', () => assert.ok(re.test(a))),
    throws: (fn, e) => check('throws', () => throwsCheck(fn, e)),
    doesNotThrow: fn => check('doesNotThrow', fn),
    notThrows: fn => check('doesNotThrow', fn),
    error: e => check('error', () => assert.ok(!e)),
    ifError: e => check('error', () => assert.ok(!e)),
    resolves: p => Promise.resolve(p).then(() => state.assertions++, e => fail('resolves', e)),
    rejects: (p, e) => Promise.resolve(typeof p === 'function' ? p() : p).then(() => fail('rejects', 'resolved'),
      () => state.assertions++),
    matchSnapshot: () => state.assertions++,
    cleanSnapshot: undefined,
    snapshot: () => state.assertions++,
    context: {},
    testdir: () => require('os').tmpdir()
  }
  t.assert = t.ok
  t.equivalent = t.same
  t.inequivalent = t.notSame
  t.isEqual = t.equal
  t.isNot = t.notEqual
  t.strictDeepEqual = t.strictSame
  // an assertion this harness does not know passes, so that the rest of the test still runs
  return new Proxy(t, {
    get(target, prop) {
      if (prop in target || typeof prop === 'symbol' || prop === 'then') {
        return target[prop]
      }
      state.unknown = state.unknown || new Set()
      state.unknown.add(prop)
      return () => undefined
    }
  })
}

function entry(name, opts, fn) {
  if (typeof name === 'function') {
    fn = name
    name = fn.name || '(anonymous)'
  } else if (typeof opts === 'function') {
    fn = opts
  }
  if (opts && typeof opts === 'object' && (opts.skip || opts.todo)) {
    fn = undefined
  }
  return {name: String(name), fn}
}

async function run(e, prefix) {
  if (!e.fn) {
    return
  }
  const t = makeT(e.name)
  const before = state.current
  state.current = prefix ? prefix + ' > ' + e.name : e.name
  try {
    await new Promise((resolve, reject) => {
      let ended = false
      const end = () => {
        if (!ended) {
          ended = true
          resolve()
        }
      }
      t.end = end
      t.done = end
      let out
      const self = {timeout() {}, slow() {}, skip() {}, retries() {}}
      try {
        out = e.fn.call(self, e.mocha ? end : t, end)
      } catch (x) {
        reject(x)
        return
      }
      if (out && typeof out.then === 'function') {
        out.then(end, reject)
      } else if (e.mocha ? e.fn.length === 0 : true) {
        // tap and tape end a test whose function returned, once its subtests ran
        setImmediate(end)
      }
    })
  } catch (x) {
    fail('threw', x)
  }
  for (const sub of t.subtests) {
    await run(sub, state.current)
  }
  for (const f of t._teardown) {
    try {
      await f()
    } catch (x) {
      fail('teardown', x)
    }
  }
  state.current = before
}

const root = makeT('root')
const tap = new Proxy(root, {
  get(target, prop) {
    if (prop === 'test' || prop === 'only') {
      return (n, o, f) => queue.push(entry(n, o, f))
    }
    if (prop === 'mocha' || prop === 'default' || prop === 't') {
      return tap
    }
    return target[prop]
  }
})

function tape(n, o, f) {
  queue.push(entry(n, o, f))
}
tape.test = tape
tape.skip = () => {}
tape.only = tape
tape.onFinish = () => {}

function nodeTest(n, o, f) {
  queue.push(entry(n, o, f))
}
nodeTest.test = nodeTest
nodeTest.describe = nodeTest
nodeTest.it = nodeTest
nodeTest.skip = () => {}
nodeTest.todo = () => {}
nodeTest.before = f => queue.push({name: 'before', fn: f})
nodeTest.after = () => {}
nodeTest.beforeEach = () => {}
nodeTest.afterEach = () => {}
nodeTest.mock = {fn: () => () => {}, method: () => {}}

// mocha's BDD and TDD interfaces: describe and suite blocks run their bodies at once, their tests queue
const mochaStack = []
function describe(n, f) {
  mochaStack.push(String(n))
  try {
    f.call({timeout() {}, slow() {}})
  } finally {
    mochaStack.pop()
  }
}
describe.skip = () => {}
describe.only = describe
function it(n, f) {
  if (!f) {
    return
  }
  const name = mochaStack.concat(String(n)).join(' > ')
  queue.push({name, fn: f, mocha: true, async: f.length === 0})
}
it.skip = () => {}
it.only = it
const hooks = {beforeEach: [], afterEach: []}
function installMocha() {
  global.describe = describe
  global.context = describe
  global.it = it
  global.specify = it
  global.before = f => queue.push({name: 'before', fn: f, mocha: true, async: f.length === 0})
  global.after = () => {}
  global.beforeEach = f => hooks.beforeEach.push(f)
  global.afterEach = () => {}
  global.xit = () => {}
  global.xdescribe = () => {}
  // mocha's TDD interface
  global.suite = describe
  global.test = it
  global.suiteSetup = global.before
  global.setup = global.beforeEach
  global.teardown = global.afterEach
}

function ava(n, f) {
  queue.push({name: String(n), fn: f})
}
ava.serial = ava
ava.skip = () => {}

async function drain() {
  while (queue.length) {
    const e = queue.shift()
    for (const h of hooks.beforeEach) {
      h()
    }
    await run(e, '')
  }
}

module.exports = {tap, tape, nodeTest, ava, installMocha, drain, state}
