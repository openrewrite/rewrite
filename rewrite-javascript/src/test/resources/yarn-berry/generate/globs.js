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
// Writes the workspace-glob rows BerryDifferentialTest checks the port against, each answered by the modules Yarn
// 4.10.3 bundles (its yarn.lock): fast-glob 3.3.2, micromatch 4.0.5, picomatch 2.3.1, braces 3.0.2, fill-range 7.0.1,
// glob-parent 5.1.2, is-glob 4.0.3, @nodelib/fs.walk 1.2.8, installed with those exact versions (npm overrides):
//   glob-picomatch.tsv.gz   pattern, dot       -> makeRe's source or !<error>, and {path: test} over sample paths
//   glob-braces.tsv.gz      pattern            -> micromatch.braces as fast-glob calls it, or !<error>
//   glob-parent.tsv.gz      pattern            -> glob-parent's base, micromatch.scan's parts, isDynamicPattern
//   glob-workspaces.tsv.gz  directories, patterns -> Workspace.setup's members (fast-glob, sorted, resolved, with a
//                                                 package.json) or !<error>, and 1 when two tasks report one
//                                                 directory under two spellings
// Patterns are drawn from a seeded generator and taken from every workspaces field under the scenario roots. Each
// tree is built a few directories below a directory of its own, so a pattern that climbs out of the project finds
// only what the generator made. With --rows, the rows of an existing output directory are answered again instead.
//
// Usage: NODE_PATH=<dir with those modules> node globs.js <out-dir> <seed> <count> <scenario-root>...
//        NODE_PATH=<dir with those modules> node globs.js --rows <rows-dir> <out-dir>
'use strict';

const fs = require('fs');
const os = require('os');
const path = require('path');
const zlib = require('zlib');
// from NODE_PATH alone: a node_modules above this file would otherwise come first
const load = name => require(require.resolve(name, {paths: [process.env.NODE_PATH]}));
const fg = load('fast-glob');
const micromatch = load('micromatch');
const picomatch = load('picomatch');
const globParent = load('glob-parent');
for (const [name, version] of [['fast-glob', '3.3.2'], ['micromatch', '4.0.5'], ['picomatch', '2.3.1'],
  ['braces', '3.0.2'], ['fill-range', '7.0.1'], ['glob-parent', '5.1.2'], ['is-glob', '4.0.3']]) {
  const actual = load(`${name}/package.json`).version;
  if (actual !== version)
    throw new Error(`${name} ${actual} is not the ${version} Yarn 4.10.3 bundles`);
}

const args = process.argv.slice(2);
const rowsDir = args[0] === '--rows' ? args[1] : null;
const [outDir, seedArg, countArg, ...roots] = rowsDir ? [args[2]] : args;
if (!outDir)
  throw new Error('usage: node globs.js <out-dir> <seed> <count> <scenario-root>... | --rows <rows-dir> <out-dir>');

const escape = s => s.replace(/\\/g, '\\\\').replace(/\t/g, '\\t').replace(/\n/g, '\\n').replace(/\r/g, '\\r');
const unescape = s => s.replace(/\\([\\tnr])/g, (m, c) => ({'\\': '\\', t: '\t', n: '\n', r: '\r'})[c]);

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
const pick = a => a[Math.floor(random() * a.length)];

const DIRECTORIES = ['packages/a', 'packages/b', 'packages/c-d', 'packages/a/nested', 'packages/.hidden',
  'packages/.hidden/y', 'apps/x', 'apps/y', 'apps/y/z', 'libs/one', 'libs/two', 'node_modules/foo',
  'packages/node_modules/q', '1/z', 'a/b', 'a/b/c', 'tools', 'packages-extra/p', 'x-y', '?/x', '[a]/b', 'a.b/c',
  '(a)/b', '{a}/b', 'a,b/c', '!x/y', '@s/p', '+p/q', '\u00E9/f', ' sp/x', 'v1', 'v2', 'v10', '.yarn/x', '.git/y',
  'packages/a/__fixtures__/proj', 'packages/legacy/sub'];
const SEGMENTS = ['packages', 'apps', 'libs', 'a', 'b', '*', '*', '**', '**', '?', '??', '[ab]', '[!a]', '[^a]',
  '[a-c]', '{packages,apps}', '{a,b}', '{a,}', '{,a}', '{a}', 'pack*', '*-d', '*.b', 'x-y', 'tools', 'y', 'one',
  '1', '{1..3}', 'v{1..2}', 'v{01..2}', '{a..c}', '@(apps|libs)', '!(packages)', '+(a|b)', '?(a)', '*(a)', '.', '..',
  '__fixtures__', '.hidden', '.*', 'node_modules', '\\*', '[[:alpha:]]', '[[:digit:]]', '(a)', '{a', 'b}', '[a',
  'a]', '!', '"a"', 'constructor', '$', '^a', 'a|b', '\u00E9', '[ab]*', '*?', 'legacy', '{x}{(a)', '[]', '[-a]',
  '\\.', 'a\\', '{{a,b}}', '{,}', '**/**', '***', '?*', '[a-]'];

// segments the trees hold, for patterns that match some of them
const MATCHING = ['packages', 'apps', 'libs', 'a', 'b', 'x', 'y', 'z', 'one', 'nested', 'legacy', '__fixtures__',
  '.hidden', 'node_modules', '*', '*', '*', '**', '**', '?', '[ab]', '[!a]', '{a,b}', '{packages,apps}', 'pack*',
  '*-d', 'v{1..2}', '@(apps|libs)', '!(packages)', '.*', '?(a)'];
const draw = (pool = SEGMENTS) => {
  const segments = [];
  const n = 1 + Math.floor(random() * 3);
  for (let i = 0; i < n; i++)
    segments.push(pick(pool));
  let pattern = segments.join(random() < 0.03 ? '//' : '/');
  if (random() < 0.25)
    pattern = './' + pattern;
  if (random() < 0.04)
    pattern = '../' + pattern;
  if (random() < 0.1)
    pattern = pattern + '/';
  if (random() < 0.12)
    pattern = '!' + pattern;
  return pattern;
};

const walk = (dir, out = []) => {
  if (!fs.existsSync(dir))
    return out;
  for (const e of fs.readdirSync(dir, {withFileTypes: true})) {
    const p = path.join(dir, e.name);
    if (e.isDirectory())
      walk(p, out);
    else if (/^pkg|package\.json$/.test(e.name))
      out.push(p);
  }
  return out;
};

const corpus = new Set();
for (const root of roots) {
  for (const file of walk(root)) {
    try {
      const manifest = JSON.parse(fs.readFileSync(file, 'utf8'));
      const workspaces = Array.isArray(manifest.workspaces) ? manifest.workspaces :
        manifest.workspaces && Array.isArray(manifest.workspaces.packages) ? manifest.workspaces.packages : [];
      for (const pattern of workspaces)
        if (typeof pattern === 'string')
          corpus.add(pattern);
    } catch {
      // not JSON
    }
  }
}

const SAMPLE_PATHS = [...DIRECTORIES, 'packages', 'packages/', 'packages/a/', '.hidden', 'a/.b', '/', 'a//b',
  'node_modules', 'x/node_modules', '.git', 'packages/node_modules', 'apps/x/', 'a', 'b', 'ab', '.', '..', './a'];
const count = Number(countArg || 0);
const patterns = [...corpus];
for (let i = 0; i < count; i++)
  patterns.push(draw());

const bracesRow = positive => {
  try {
    const expanded = micromatch.braces(positive, {expand: true, nodupes: true, keepEscaping: true});
    return [[escape(positive), escape(JSON.stringify(expanded))], expanded];
  } catch (error) {
    return [[escape(positive), escape(`!${error.name}: ${error.message}`)], []];
  }
};
const parentRow = p => [escape(p), escape(globParent(p, {flipBackslashes: false})),
  escape(JSON.stringify(micromatch.scan(p, {posix: true, strictSlashes: false, parts: true}).parts)),
  fg.isDynamicPattern(p) ? '1' : '0'];
const picomatchRow = (p, dot) => {
  let source;
  const tests = {};
  try {
    const regex = picomatch.makeRe(p, {dot, matchBase: false, nobrace: false, nocase: false, noext: false,
      noglobstar: false, posix: true, strictSlashes: false}, false, true);
    // the string compileRe gives new RegExp; RegExp.prototype.source would escape its slashes
    source = `^(?:${regex.state.output})$`;
    if (regex.state.negated === true)
      source = `^(?!${source}).*$`;
    for (const sample of SAMPLE_PATHS)
      tests[sample] = regex.test(sample);
  } catch (error) {
    source = `!${error.name}: ${error.message}`;
  }
  return [escape(p), dot ? '1' : '0', escape(source), escape(JSON.stringify(tests))];
};

const picomatchRows = [];
const bracesRows = [];
const parentRows = [];
const workspacesRows = [];
// deeper than any pattern climbs, so `..` only ever reaches directories the generator made
const sandbox = fs.mkdtempSync(path.join(os.tmpdir(), 'globs-'));
const tree = path.join(sandbox, 'u', 'v', 'w', 'x', 'y', 'z', 'root');
const IGNORE = ['**/node_modules', '**/.git', '**/.yarn'];
async function membersRow(dirs, list) {
  for (const pattern of list) {
    if (pattern.split('/').filter(s => s === '..').length >= 7)
      throw new Error(`${pattern} climbs above the sandbox`);
  }
  fs.rmSync(sandbox, {recursive: true, force: true});
  fs.mkdirSync(tree, {recursive: true});
  for (const d of dirs) {
    fs.mkdirSync(path.join(tree, d), {recursive: true});
    fs.writeFileSync(path.join(tree, d, 'package.json'), '{}');
  }
  let answer;
  let racy = false;
  try {
    const options = {cwd: tree, onlyDirectories: true, ignore: IGNORE};
    const raw = await fg(list, options);
    const spellings = new Map();
    for (const p of await fg(list, {...options, unique: false})) {
      const key = p.startsWith('./') ? p.slice(2) : p;
      spellings.set(key, (spellings.get(key) || new Set()).add(p));
    }
    racy = [...spellings.values()].some(s => s.size > 1);
    raw.sort();
    const result = [];
    for (const rel of raw) {
      const abs = path.resolve(tree, rel);
      if (fs.existsSync(path.join(abs, 'package.json'))) {
        const r = path.relative(tree, abs);
        if (!result.includes(r))
          result.push(r);
      }
    }
    answer = JSON.stringify(result);
  } catch (error) {
    answer = `!${error.name}: ${error.message.split(tree).join('<root>').split(sandbox).join('<sandbox>')}`;
  }
  return [escape(JSON.stringify(dirs)), escape(JSON.stringify(list)), escape(answer), racy ? '1' : '0'];
}

async function draws() {
  for (const pattern of new Set(patterns)) {
    const positive = pattern.startsWith('!') && pattern[1] !== '(' ? pattern.slice(1) : pattern;
    const [row, expanded] = bracesRow(positive);
    bracesRows.push(row);
    for (const p of new Set([positive, ...expanded.filter(Boolean)])) {
      if (p !== '')
        parentRows.push(parentRow(p));
      for (const dot of [false, true])
        picomatchRows.push(picomatchRow(p, dot));
    }
  }
  const rows = Math.max(Math.floor(count / 4), corpus.size);
  const corpusPatterns = [...corpus];
  for (let i = 0; i < rows; i++) {
    const dirs = DIRECTORIES.filter(() => random() < 0.5);
    const list = [];
    const n = 1 + Math.floor(random() * 3);
    for (let j = 0; j < n; j++)
      list.push(i < corpusPatterns.length && j === 0 ? corpusPatterns[i] : draw(i % 2 === 0 ? MATCHING : SEGMENTS));
    workspacesRows.push(await membersRow(dirs, list));
  }
}

async function answerAgain() {
  const read = name => zlib.gunzipSync(fs.readFileSync(path.join(rowsDir, name + '.gz'))).toString('utf8')
    .split('\n').filter(Boolean).map(line => line.split('\t').map(unescape));
  for (const row of read('glob-braces.tsv'))
    bracesRows.push(bracesRow(row[0])[0]);
  for (const row of read('glob-parent.tsv'))
    parentRows.push(parentRow(row[0]));
  for (const row of read('glob-picomatch.tsv'))
    picomatchRows.push(picomatchRow(row[0], row[1] === '1'));
  for (const row of read('glob-workspaces.tsv'))
    workspacesRows.push(await membersRow(JSON.parse(row[0]), JSON.parse(row[1])));
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
(rowsDir ? answerAgain() : draws()).then(() => {
  fs.rmSync(sandbox, {recursive: true, force: true});
  write('glob-picomatch.tsv', picomatchRows);
  write('glob-braces.tsv', bracesRows);
  write('glob-parent.tsv', parentRows);
  write('glob-workspaces.tsv', workspacesRows);
});
