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
// Writes the rows BerryDifferentialTest checks the port against, each answered by Yarn 4.10.3's own code
// (@yarnpkg/core 4.4.4, @yarnpkg/parsers 3.0.3 and @yarnpkg/extensions 2.0.6, the versions the
// @yarnpkg/cli/4.10.3 tag holds):
//   syml-parse.tsv.gz   yaml text           -> parseSyml as ordered JSON, or !<error message>
//   syml-stringify.tsv.gz ordered JSON value  -> stringifySyml text
//   struct.tsv.gz       function, string    -> structUtils' answer as JSON, or !<error message>
//   manifest.tsv.gz     registry document   -> the fields a lock entry takes from Manifest.load
//   normalize.tsv.gz    registry document   -> the package NpmSemverResolver makes, after normalizePackage with
//                                              the builtin extensions (@yarnpkg/extensions 2.0.6)
// The inputs are every lock, key, resolution, range and registry version document under the scenario roots, and
// strings drawn from a seeded generator. Fields are tab-separated with \t \n \r \\ escaped;
// the files are gzipped. With --rows, the rows of an existing output directory are answered again instead.
//
// Usage: NODE_PATH=<dir with @yarnpkg/core 4.4.4> node differential.js <out-dir> <seed> <count> <scenario-root>...
//        NODE_PATH=<dir with @yarnpkg/core 4.4.4> node differential.js --rows <rows-dir> <out-dir>
'use strict';

const fs = require('fs');
const zlib = require('zlib');
const path = require('path');
const {structUtils, Manifest} = require('@yarnpkg/core');
const {parseSyml, stringifySyml} = require('@yarnpkg/parsers');

const args = process.argv.slice(2);
const rowsDir = args[0] === '--rows' ? args[1] : null;
const [outDir, seedArg, countArg, ...roots] = rowsDir ? [args[2]] : args;
if (!outDir || roots.length === 0 && !rowsDir)
  throw new Error('usage: node differential.js <out-dir> <seed> <count> <scenario-root>... | ' +
    '--rows <rows-dir> <out-dir>');

const escape = s => s.replace(/\\/g, '\\\\').replace(/\t/g, '\\t').replace(/\n/g, '\\n').replace(/\r/g, '\\r');
const unescape = s => s.replace(/\\([\\tnr])/g, (m, c) => ({'\\': '\\', t: '\t', n: '\n', r: '\r'})[c]);

// JSON with every object as [[key, value], ...] so the key order survives
const ordered = value => {
  if (value === null || typeof value !== 'object')
    return value;
  if (Array.isArray(value))
    return {array: value.map(ordered)};
  return {object: Object.keys(value).map(k => [k, ordered(value[k])])};
};
const unordered = value => value === null || typeof value !== 'object' ? value :
  value.array ? value.array.map(unordered) : Object.fromEntries(value.object.map(([k, v]) => [k, unordered(v)]));
const parseAnswer = text => {
  try {
    return JSON.stringify(ordered(parseSyml(text)));
  } catch (error) {
    return `!${error.message}`;
  }
};

function mulberry32(seed) {
  return () => {
    seed |= 0;
    seed = seed + 0x6D2B79F5 | 0;
    let t = Math.imul(seed ^ seed >>> 15, 1 | seed);
    t = t + Math.imul(t ^ t >>> 7, 61 | t) ^ t;
    return ((t ^ t >>> 14) >>> 0) / 4294967296;
  };
}

const random = mulberry32(Number(seedArg || 1));
const ALPHABET = ['a', 'b', 'z', '0', '9', '-', '?', ':', ',', '[', ']', '{', '}', '#', '&', '*', '!', '|', '>',
  '\'', '"', '%', '@', '`', ' ', '\t', '\n', '\r', '.', '/', '^', '~', '=', '<', '\\', 'é', ' ', '\u0085',
  'npm:', '::', '%3A', '%2', 'workspace:', '@scope/', '#commit=', 'virtual:', '0.0.0', '1.2.3-rc.1', ' '];
const draw = () => {
  let s = '';
  const n = 1 + Math.floor(random() * 8);
  for (let i = 0; i < n; i++)
    s += ALPHABET[Math.floor(random() * ALPHABET.length)];
  return s;
};

const walk = (dir, out = []) => {
  if (!fs.existsSync(dir))
    return out;
  for (const e of fs.readdirSync(dir, {withFileTypes: true})) {
    const p = path.join(dir, e.name);
    if (e.isDirectory())
      walk(p, out);
    else
      out.push(p);
  }
  return out;
};

const LOCKS = /^(before|after|fresh|again|lock-seed|before-[\w.-]+)$/;
const locks = [];
const documents = [];
for (const root of roots) {
  for (const file of walk(root)) {
    const name = path.basename(file);
    const dir = path.basename(path.dirname(file));
    if (LOCKS.test(name) && !name.endsWith('.log') && name !== 'before-commands')
      locks.push(fs.readFileSync(file, 'utf8'));
    else if (dir === 'http' && !name.includes('.') ||
      dir === 'http' && /^[^.]+-\d/.test(name) && !name.endsWith('.tgz'))
      documents.push(file);
  }
}

const strings = new Set();
const symlParse = [];
const symlStringify = [];
for (const text of new Set(locks)) {
  symlParse.push([escape(text), escape(parseAnswer(text))]);
  let parsed;
  try {
    parsed = parseSyml(text);
  } catch (error) {
    continue;
  }
  if (!text.includes('yarn lockfile v1'))
    symlStringify.push([escape(JSON.stringify(ordered(parsed))), escape(stringifySyml(parsed))]);
  for (const [key, entry] of Object.entries(parsed)) {
    for (const part of key.split(/ *, */))
      strings.add(part);
    if (entry && typeof entry === 'object') {
      if (typeof entry.resolution === 'string')
        strings.add(entry.resolution);
      for (const field of ['dependencies', 'peerDependencies'])
        for (const [name, range] of Object.entries(entry[field] || {}))
          strings.add(`${name}@${range}`);
    }
  }
}

const count = Number(countArg || 0);
for (let i = 0; i < count; i++) {
  const s = draw();
  strings.add(s);
  strings.add(`name@${s}`);
  strings.add(`@scope/name@npm:${s}`);
  const value = {[draw()]: {[draw()]: draw(), version: draw(), [draw()]: {[draw()]: draw()}}};
  symlStringify.push([escape(JSON.stringify(ordered(value))), escape(stringifySyml(value))]);
  const yaml = `${draw()}:\n  ${draw()}: ${draw()}\n${draw()}`;
  symlParse.push([escape(yaml), escape(parseAnswer(yaml))]);
}

const answer = fn => {
  try {
    return JSON.stringify(ordered(fn()) ?? null);
  } catch (error) {
    return `!${error.message}`;
  }
};
const range = r => r === null ? null : {protocol: r.protocol, source: r.source, selector: r.selector, params: r.params};
const STRUCT = {
  parseIdent: s => structUtils.stringifyIdent(structUtils.parseIdent(s)),
  parseDescriptor: s => {
    const d = structUtils.parseDescriptor(s);
    return [d.scope, d.name, d.range, structUtils.stringifyDescriptor(d)];
  },
  parseDescriptorStrict: s => {
    const d = structUtils.parseDescriptor(s, true);
    return [d.scope, d.name, d.range];
  },
  parseLocatorStrict: s => {
    const l = structUtils.parseLocator(s, true);
    return [l.scope, l.name, l.reference, structUtils.stringifyLocator(l)];
  },
  parseRange: s => range(structUtils.parseRange(s)),
  makeRange: s => structUtils.makeRange(structUtils.parseRange(s)),
  convertToManifestRange: s => structUtils.convertToManifestRange(s),
};
const struct = [];
for (const s of strings) {
  for (const [fn, compute] of Object.entries(STRUCT))
    struct.push([fn, escape(s), escape(answer(() => compute(s)))]);
}

const manifestRow = version => {
  const m = new Manifest();
  let result;
  try {
    m.load(version);
    const exported = m.exportTo({}, {compatibilityMode: false});
    result = {
      conditions: m.getConditions(),
      dependencies: exported.dependencies,
      peerDependencies: exported.peerDependencies,
      dependenciesMeta: exported.dependenciesMeta,
      peerDependenciesMeta: exported.peerDependenciesMeta,
      bin: exported.bin,
      scripts: exported.scripts,
    };
    result = JSON.stringify(ordered(JSON.parse(JSON.stringify(result))));
  } catch (error) {
    result = `!${error.message}`;
  }
  return [escape(JSON.stringify(version)), escape(result)];
};
const manifest = [];
const versionDocs = [];
for (const file of documents) {
  let doc;
  try {
    doc = JSON.parse(fs.readFileSync(file, 'utf8'));
  } catch {
    continue;
  }
  const versions = doc && doc.versions ? Object.values(doc.versions) : [doc];
  for (const version of versions) {
    if (!version || typeof version !== 'object')
      continue;
    manifest.push(manifestRow(version));
    versionDocs.push(version);
  }
}

// a registry version as NpmSemverResolver.resolve makes it, then Configuration.normalizePackage with the extension
// table @yarnpkg/extensions gives (what the compat plugin registers)
async function normalizeRows(versionDocs) {
  const {Configuration, LinkType} = require('@yarnpkg/core');
  const {packageExtensions} = require('@yarnpkg/extensions');
  const compat = {hooks: {registerPackageExtensions: async (configuration, register) => {
    for (const [descriptor, data] of packageExtensions)
      register(structUtils.parseDescriptor(descriptor, true), data);
  }}};
  const configuration = Configuration.create(process.cwd(), process.cwd(), new Map([[`compat`, compat]]));
  const extensions = await configuration.getPackageExtensions();
  const NODE_GYP_IDENT = structUtils.makeIdent(null, `node-gyp`);
  const rows = [];
  for (const version of versionDocs) {
    if (typeof version.name !== 'string' || typeof version.version !== 'string')
      continue;
    let result;
    try {
      const m = new Manifest();
      m.load(version);
      if (!m.dependencies.has(NODE_GYP_IDENT.identHash) && !m.peerDependencies.has(NODE_GYP_IDENT.identHash)) {
        for (const value of m.scripts.values()) {
          if (value.match(/\b(node-gyp|prebuild-install)\b/)) {
            m.dependencies.set(NODE_GYP_IDENT.identHash, structUtils.makeDescriptor(NODE_GYP_IDENT, `latest`));
            break;
          }
        }
      }
      const locator = structUtils.makeLocator(structUtils.parseIdent(version.name), `npm:${version.version}`);
      const pkg = {...locator, version: version.version, languageName: `node`, linkType: LinkType.HARD,
        conditions: m.getConditions(), dependencies: configuration.normalizeDependencyMap(m.dependencies),
        peerDependencies: m.peerDependencies, dependenciesMeta: m.dependenciesMeta,
        peerDependenciesMeta: m.peerDependenciesMeta, bin: m.bin};
      const normalized = configuration.normalizePackage(pkg, {packageExtensions: extensions});
      result = JSON.stringify(ordered(JSON.parse(JSON.stringify({
        dependencies: [...normalized.dependencies.values()].map(d => structUtils.stringifyDescriptor(d)),
        peerDependencies: [...normalized.peerDependencies.values()].map(d => structUtils.stringifyDescriptor(d)),
        peerDependenciesMeta: Object.fromEntries(normalized.peerDependenciesMeta),
      }))));
    } catch (error) {
      result = `!${error.message}`;
    }
    rows.push([escape(JSON.stringify(version)), escape(result)]);
  }
  return rows;
}

const write = (name, rows) => {
  const seen = new Set();
  const lines = [];
  for (const row of rows) {
    const line = row.join('\t');
    if (!seen.has(line)) {
      seen.add(line);
      lines.push(line);
    }
  }
  fs.mkdirSync(outDir, {recursive: true});
  fs.writeFileSync(path.join(outDir, name + '.gz'), zlib.gzipSync(lines.join('\n') + '\n', {level: 9}));
  console.log(`${name}\t${lines.length}`);
};
let normalized = versionDocs;
if (rowsDir) {
  const read = name => zlib.gunzipSync(fs.readFileSync(path.join(rowsDir, name + '.gz'))).toString('utf8')
    .split('\n').filter(Boolean).map(line => line.split('\t').map(unescape));
  for (const [text] of read('syml-parse.tsv'))
    symlParse.push([escape(text), escape(parseAnswer(text))]);
  for (const [json] of read('syml-stringify.tsv'))
    symlStringify.push([escape(json), escape(stringifySyml(unordered(JSON.parse(json))))]);
  for (const [fn, s] of read('struct.tsv'))
    struct.push([fn, escape(s), escape(answer(() => STRUCT[fn](s)))]);
  for (const [version] of read('manifest.tsv'))
    manifest.push(manifestRow(JSON.parse(version)));
  normalized = read('normalize.tsv').map(([version]) => JSON.parse(version));
}
write('syml-parse.tsv', symlParse);
write('syml-stringify.tsv', symlStringify);
write('struct.tsv', struct);
write('manifest.tsv', manifest);
normalizeRows(normalized).then(rows => write('normalize.tsv', rows));
