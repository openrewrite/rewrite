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
// js-yaml 3.13.1, 3.14.1 and @zkochan/js-yaml 0.0.11 (js-yaml 4.1.0's tests, JS_YAML_ENTRY its index.js) for
// upstream-calls.js: load, safeLoad, loadAll, dump; a schema by name, a Date by its time, bytes as hex.
const SCHEMAS = ['FAILSAFE_SCHEMA', 'JSON_SCHEMA', 'CORE_SCHEMA', 'DEFAULT_SAFE_SCHEMA', 'DEFAULT_FULL_SCHEMA',
  'DEFAULT_SCHEMA']
let real
const v3 = process.argv[4].startsWith('3.')

// where 3.13.1, 3.14.1 and 4.1.0's loader differ: 3.x as yarn and Berry ask it (FAILSAFE_SCHEMA, with and without
// json), the fork as pnpm does
const DIFFERENCES = ['a: 1\0b', '!<?> [a]', 'a: &hasOwnProperty x\nb: *hasOwnProperty', 'a:\n\t- b', '[a, , b]',
  '__proto__: x\nb: y', 'a: &__proto__ x\nb: *__proto__', 'a: {x: 1} b: 2', '!<tag:yaml.org,2002:%73tr> a',
  '%TAG !e! tag:yaml.org,2002:%73\n---\n!e!tr a', 'a: 1\na: 2', '{a: 1, a: 2}', 'key: [a, b]', '', '# comment',
  'a\n---\nb', 'a: !!str 1', 'a: !!int 1', 'a: 1\nb: true\nc: null\nd: 2001-12-14', '<<: {a: 1}\nb: 2',
  '? a\n: b\n? c', 'a: |\n  x\n\n  y\n', '"\\x41\\u00e9"', "'it''s'", '[a, {b: c}]: d']

module.exports = {
  library: 'js-yaml',
  entry: () => process.env.JS_YAML_ENTRY || 'index.js',
  alsoEntries: () => process.env.JS_YAML_ENTRY ? ['index.js'] : [],
  functions: ['load', 'loadAll', 'safeLoad', 'safeLoadAll', 'dump', 'safeDump'],
  tests: () => ['test/00-units.js', 'test/10-loader.js', 'test/11-load-errors.js', 'test/20-dumper.js',
    'test/30-issues.js'],
  wrap(module, {wrapFunctions}) {
    real = module
    return wrapFunctions(module)
  },
  differences: DIFFERENCES.flatMap(input => v3 ?
    [yaml => yaml.safeLoad(input, {schema: yaml.FAILSAFE_SCHEMA}),
      yaml => yaml.safeLoad(input, {schema: yaml.FAILSAFE_SCHEMA, json: true})] : [yaml => yaml.load(input)]),
  encode(v) {
    if (v instanceof Date) {
      return {$: 'date', time: v.getTime()}
    }
    if (v instanceof Uint8Array) {
      return {$: 'bytes', hex: Buffer.from(v).toString('hex')}
    }
    if (real && real.Schema && v instanceof real.Schema) {
      const name = SCHEMAS.find(n => real[n] === v)
      return {$: 'schema', name: name || 'custom'}
    }
    return undefined
  }
}
