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
// Writes pnpm/differential/yaml.tsv.gz (seed 20261008, count 3000), the rows JsYamlDifferentialTest checks the port
// against, each answered by the YAML module pnpm 11.2.2 bundles (@zkochan/js-yaml 0.0.11):
//   dump <tagged value> <text>    yaml.dump with lockfileYamlDump's options
//   load <text> <tagged value>    yaml.load, or `!` and the error's message
// Values are drawn from a seeded generator over strings that sit at the edges of the scalar rules (types the
// default schema resolves, indicators, quotes, line breaks, non-printables) and keys the dumper treats specially; load
// inputs are the dumped texts, mutations of them, and hand-shaped fragments. A tagged value is JSON: {"s":string},
// {"n":String(number) or "-0"}, {"b":bool}, {"z":1} null, {"a":[...]}, {"o":[[key,value],...]} in key order,
// {"d":time} a Date, {"y":[bytes]} a Uint8Array, {"u":1} undefined.
//
// Usage: NODE_PATH=<dir holding @zkochan/js-yaml@0.0.11> node yaml-differential.js <out-file> <seed> <count>
'use strict';

const fs = require('fs');
const path = require('path');
const zlib = require('zlib');
const dir = path.dirname(require.resolve('@zkochan/js-yaml/package.json', {paths: [process.env.NODE_PATH]}));
const version = JSON.parse(fs.readFileSync(path.join(dir, 'package.json'), 'utf8')).version;
if (version !== '0.0.11') {
  throw new Error(`@zkochan/js-yaml ${version}, not 0.0.11`);
}
const yaml = require(dir);
const OPTIONS = {blankLines: true, lineWidth: -1, noCompatMode: true, noRefs: true, sortKeys: false};

let seed = Number(process.argv[3]);
const random = () => {
  seed = (seed + 0x6D2B79F5) | 0;
  let t = Math.imul(seed ^ (seed >>> 15), 1 | seed);
  t = (t + Math.imul(t ^ (t >>> 7), 61 | t)) ^ t;
  return ((t ^ (t >>> 14)) >>> 0) / 4294967296;
};
const pick = list => list[Math.floor(random() * list.length)];

const ATOMS = ['', ' ', 'a', 'a b', ' a', 'a ', 'true', 'True', 'TRUE', 'false', 'yes', 'no', 'on', 'off', 'y', 'n',
  'null', 'Null', '~', '0', '1', '-1', '+1', '01', '1.0', '1.', '.5', '1e3', '1E-3', '0x1F', '0o17', '0b101', '1_000',
  '_1', '1_', '.inf', '-.inf', '.NaN', '2020-01-01', '2020-1-1 1:02:03', '2020-01-01T00:00:00Z', '2001-12-14t21:59:43.10-05:00',
  '<<', '-', '- a', '-a', '?', '? a', ':', ':a', 'a:', 'a:b', 'a: b', 'a #b', 'a#b', '#a', ',', 'a,b', '[a]', ']', '{a}',
  '}', '&a', '*a', '!a', '|', '>', "'", "a'b", '"', 'a"b', '%', '@', '@scope/pkg', '`', '\\', 'a\nb', '\n', 'a\n', '\na',
  'a\n\nb', ' a\nb', 'a\tb', '\t', '\r', '\u0085', '\u00a0', '\u2028', '\ufeff', '\u0007', '\u001b', '\ud83d\ude00', '\ud800',
  'é', '^1.0.0', '>=1 <2', '*', '1.0.0 || 2.0.0', 'link:../a', 'file:a.tgz', 'sha512-abc+/=', 'https://x/-/a-1.0.0.tgz',
  'a(b@1.0.0)', '9.0', "'9.0'", 'engines', 'resolution', 'packages', 'constructor', 'toString', 'variations', 'binary'];
const KEYS = ['a', 'b', 'name', 'version', 'engines', 'cpu', 'os', 'libc', 'resolution', 'packages', 'importers',
  'snapshots', 'dependencies', 'type', 'constructor', 'toString', '__proto__', 'valueOf', 'specifier', 'integrity'];

function str() {
  if (random() < 0.6) return pick(ATOMS);
  let s = '';
  const n = 1 + Math.floor(random() * 3);
  for (let i = 0; i < n; i++) s += pick(ATOMS);
  return s;
}
function num() {
  return pick([0, -0, 1, -1, 1.5, 0.1, 1e21, 1e-7, 5e-324, 123456789012345680000, NaN, Infinity, -Infinity,
    Math.floor(random() * 1e6), random() * 1000]);
}
function value(depth) {
  const r = random();
  if (depth > 3 || r < 0.45) {
    const s = random();
    if (s < 0.75) return str();
    if (s < 0.85) return num();
    if (s < 0.92) return random() < 0.5;
    if (s < 0.96) return null;
    if (s < 0.98) return new Date(Date.UTC(2001, 1, 2, 3, 4, 5, Math.floor(random() * 1000)));
    return new Uint8Array([1, 2, 255].slice(0, 1 + Math.floor(random() * 3)));
  }
  if (r < 0.65) {
    const out = [];
    const n = Math.floor(random() * 4);
    for (let i = 0; i < n; i++) out.push(random() < 0.05 ? undefined : value(depth + 1));
    return out;
  }
  const out = {};
  const n = Math.floor(random() * 5);
  for (let i = 0; i < n; i++) {
    const key = random() < 0.6 ? pick(KEYS) : str();
    // an own property, as the loader stores __proto__, rather than a prototype
    Object.defineProperty(out, key, {
      value: random() < 0.05 ? undefined : value(depth + 1), enumerable: true, writable: true, configurable: true
    });
  }
  return out;
}

function tag(v) {
  if (v === undefined) return {u: 1};
  if (v === null) return {z: 1};
  if (typeof v === 'string') return {s: v};
  if (typeof v === 'number') return {n: Object.is(v, -0) ? '-0' : String(v)};
  if (typeof v === 'boolean') return {b: v};
  if (v instanceof Date) return {d: v.getTime()};
  if (v instanceof Uint8Array) return {y: Array.from(v)};
  if (Array.isArray(v)) return {a: v.map(tag)};
  return {o: Object.keys(v).map(k => [k, tag(v[k])])};
}

const INSERTS = [' ', '\n', ':', '-', '#', '\t', "'", '"', '{', '}', '[', ']', ',', '&', '*', '!', '|', '>', '?', '%'];
function mutate(text) {
  if (text.length === 0) return pick(['a', ':', '-', '{', '[', "'", '"']);
  const i = Math.floor(random() * text.length);
  const op = random();
  if (op < 0.33) return text.slice(0, i) + text.slice(i + 1);
  if (op < 0.66) return text.slice(0, i) + pick(INSERTS) + text.slice(i);
  return text.slice(0, i) + text.slice(i + 1, i + 2) + text.slice(i, i + 1) + text.slice(i + 2);
}

const FRAGMENTS = ['', '\n', '# c\n', '---\n', '---\na: 1\n...\n', 'a: 1\n---\nb: 2\n', '%YAML 1.2\n---\na: 1\n',
  '%TAG !e! tag:example.com,2000:\n---\na: !e!foo 1\n', 'a: &x {b: 1}\nc: *x\n', '<<: {a: 1}\nb: 2\n',
  'a: 1\n<<: [{a: 2, c: 3}, {d: 4}]\n', 'a: 1\na: 2\n', '? a\n: b\n', '? [a, b]\n: c\n', 'a: |\n  x\n   y\n',
  'a: >-\n  x\n\n  y\n', 'a: |+\n  x\n\n', 'a: !!str 1\n', 'a: !!int "1"\n', 'a: !!float 1\n', 'a: !!bool yes\n',
  'a: !!binary aGVsbG8=\n', 'a: !!set {x, y}\n', 'a: !!omap [{x: 1}, {y: 2}]\n', 'a: !!pairs [{x: 1}, {x: 2}]\n',
  'a: !!timestamp 2001-12-14\n', 'a: !unknown 1\n', 'a: !<tag:yaml.org,2002:str> 1\n', '\ta: 1\n', 'a:\n\t- 1\n',
  'a: "\\x41\\u00e9\\U0001F600\\n"\n', "a: 'it''s'\n", 'a: [1, , 2]\n', 'a: {b: 1 c: 2}\n', 'a: b: c\n',
  '- a\n- b: c\n  d: e\n', 'a:\n- 1\n- 2\n', 'a: 0005-01-01\n', 'a: 2001-02-30\n', 'a: 1_000\n', 'a: 0b1_0\n',
  'a: -0\n', 'a: -0.0\n', 'a: 1e400\n', 'key with spaces: v\n', '"quoted key": v\n', '[a]: b\n', '{a: 1}: b\n',
  'a: \u0085\n', 'a: b\u2028c\n'];

const out = [];
const count = Number(process.argv[4]);
const texts = [];
for (let i = 0; i < count; i++) {
  const v = value(0);
  let text;
  try {
    text = yaml.dump(v, OPTIONS);
  } catch (e) {
    out.push(['dump', JSON.stringify(tag(v)), '!' + JSON.stringify(e.message)].join('\t'));
    continue;
  }
  out.push(['dump', JSON.stringify(tag(v)), JSON.stringify(text)].join('\t'));
  texts.push(text);
}
const loads = texts.concat(FRAGMENTS);
for (let i = 0; i < count; i++) {
  loads.push(mutate(pick(texts)));
}
for (const text of loads) {
  let result;
  try {
    result = JSON.stringify(tag(yaml.load(text)));
  } catch (e) {
    result = '!' + JSON.stringify(e.message);
  }
  out.push(['load', JSON.stringify(text), result].join('\t'));
}
fs.writeFileSync(process.argv[2], zlib.gzipSync(Buffer.from(out.join('\n') + '\n', 'utf8'), {level: 9}));
console.log(`${out.length} rows`);
