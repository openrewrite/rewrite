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
// Dumps the test fixtures of an npm/node-semver checkout (the tag npm bundles: v7.8.4 for npm 11.17.0) as JSON,
// with each row's options normalized the way node-semver's parseOptions reads them, and checks
// each expectation against the node-semver npm bundles.
// NPM_NODE_MODULES names the node_modules npm bundles when it is not beside the running node.
// Usage: node dump-fixtures.js <node-semver checkout> fixtures-7.8.4
const fs = require('fs')
const path = require('path')
const checkout = path.resolve(process.argv[2])
const fx = name => require(path.join(checkout, "test/fixtures", name))
const semver = require(path.join(process.env.NPM_NODE_MODULES ||
  path.resolve(path.dirname(process.execPath), "../lib/node_modules/npm/node_modules"), "semver"))

const opts = o => {
  if (!o) {
    return { loose: false, includePrerelease: false }
  }
  if (typeof o !== 'object') {
    return { loose: true, includePrerelease: false }
  }
  return { loose: !!o.loose, includePrerelease: !!o.includePrerelease }
}
const check = (cond, msg) => {
  if (!cond) {
    throw new Error('fixture disagrees with installed semver: ' + msg)
  }
}

const out = {
  version: require(path.join(checkout, "package.json")).version,
  rangeInclude: fx('range-include.js').map(([r, v, o]) => {
    check(semver.satisfies(v, r, o), `${v} in ${r}`)
    return [r, v, opts(o)]
  }),
  rangeExclude: fx('range-exclude.js').filter(([, v]) => typeof v === 'string').map(([r, v, o]) => {
    check(!semver.satisfies(v, r, o), `${v} not in ${r}`)
    return [r, v, opts(o)]
  }),
  rangeIntersection: fx('range-intersection.js').map(([a, b, e]) => {
    check(semver.intersects(a, b) === e, `${a} ~ ${b}`)
    return [a, b, e]
  }),
  comparatorIntersection: fx('comparator-intersection.js').map(([a, b, e, ip]) => {
    check(semver.intersects(a, b, { includePrerelease: ip }) === e, `${a} ~ ${b}`)
    return [a, b, e, !!ip]
  }),
  rangeParse: fx('range-parse.js').map(([r, e, o]) => {
    check(semver.validRange(r, o) === e, `validRange(${r})`)
    return [r, e, opts(o)]
  }),
  validVersions: fx('valid-versions.js').map(([v]) => [v, semver.valid(v)]),
  invalidVersions: fx('invalid-versions.js')
    .filter(([v]) => typeof v === 'string')
    .map(([v, , o]) => {
      check(semver.valid(v, o) === null, `invalid ${v}`)
      return [v, opts(o)]
    }),
  comparisons: fx('comparisons.js').map(([a, b, o]) => {
    check(semver.gt(a, b, o), `${a} > ${b}`)
    return [a, b, opts(o)]
  }),
  equality: fx('equality.js').map(([a, b, o]) => {
    check(semver.eq(a, b, o), `${a} == ${b}`)
    return [a, b, opts(o)]
  }),
}
fs.writeFileSync(process.argv[3], JSON.stringify(out, null, 1) + '\n')
console.log(Object.entries(out).map(([k, v]) => `${k}=${Array.isArray(v) ? v.length : v}`).join(' '))
