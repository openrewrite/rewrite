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
// Writes the workspace-glob rows TinyglobbyDifferentialTest checks the port against, each answered by the modules
// pnpm 11.2.2 bundles: tinyglobby 0.2.16, picomatch 4.0.4 and fdir 6.5.0 (pnpm's dist/node_modules holds the same):
//   globs-picomatch.tsv.gz   pattern            -> makeRe's source under tinyglobby's options or !<error>, and
//                                                 {path: match} over sample paths
//   globs-scan.tsv.gz        pattern            -> scan's parts and tinyglobby's isDynamicPattern
//   globs-workspaces.tsv.gz  directories, patterns -> @pnpm/workspace.projects-reader findWorkspaceProjectsNoCheck's
//                                                 project directories (relative, sorted) or !<error>
// Patterns are drawn from a seeded generator. Each tree is built a few directories below a directory of its own, so
// a pattern that climbs out of the project finds only what the generator made. With --rows, the rows of an existing
// output directory are answered again instead.
//
// Usage: NODE_PATH=<pnpm>/dist/node_modules node globs.js <out-dir> <seed> <count>
//        NODE_PATH=<pnpm>/dist/node_modules node globs.js --rows <rows-dir> <out-dir>
'use strict';

const fs = require('fs');
const os = require('os');
const path = require('path');
const zlib = require('zlib');
const load = name => require(require.resolve(name, {paths: [process.env.NODE_PATH]}));
const picomatch = load('picomatch');
const scan = (input, options) => picomatch.scan(input, options);
for (const [name, version] of [['tinyglobby', '0.2.16'], ['picomatch', '4.0.4'], ['fdir', '6.5.0']]) {
  const actual = load(`${name}/package.json`).version;
  if (actual !== version)
    throw new Error(`${name} ${actual} is not the ${version} pnpm 11.2.2 bundles`);
}

const args = process.argv.slice(2);
const rowsDir = args[0] === '--rows' ? args[1] : null;
const [outDir, seedArg, countArg] = rowsDir ? [args[2]] : args;
if (!outDir)
  throw new Error('usage: node globs.js <out-dir> <seed> <count> | --rows <rows-dir> <out-dir>');

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

const DIRECTORIES = ['packages/a', 'packages/b', 'packages/aa', 'packages/aaa', 'packages/c-d', 'packages/a/nested',
  'packages/.hidden', 'apps/x', 'apps/y', 'apps/y/z', 'libs/one', 'libs/two', 'node_modules/foo',
  'packages/node_modules/q', 'packages/bower_components/r', 'test/t', 'packages/tests/u', 'a/b', 'a/b/c', 'tools',
  'x-y', '[a]/b', 'a.b/c', '(a)/b', '{a}/b', 'a,b/c', '@s/p', '+p/q', 'ab', 'abab', 'v1', 'v2', 'v10', 'a*b/c'];
const SEGMENTS = ['packages', 'apps', 'libs', 'a', 'b', 'aa', '*', '*', '**', '**', '?', '??', '[ab]', '[!a]',
  '[^a]', '[a-c]', '{packages,apps}', '{a,b}', '{a,}', '{a..c}', 'v{1..2}', 'pack*', '*-d', '@(apps|libs)',
  '!(packages)', '+(a|b)', '?(a)', '*(a)', '+(a)', '+(a|aa)', '*(a|aa)', '+(*)', '*(?)', '+(a|)', '*(a|b|)',
  '+(+(a))', '*(*(a))', '*(a)*(b)', '+(@(a))', '@(+(a))', '*(ab)', '+(ab|abab)', '!(a|b)', '@(a|b)*', '.',
  '.hidden', '.*', 'node_modules', '\\*', '\\(a\\)', 'a\\*b', '\\[a\\]', '\\{a\\}', 'a\\,b', '\\@s', '\\+p',
  '[[:alpha:]]', '(a)', '{a', 'b}', '[a', 'a]', '"a"', 'constructor', '__proto__', '$', '^a', 'a|b', '[ab]*',
  '**/**', '***', '?*', 'tools', 'one', 'y', 'x-y'];

const draw = () => {
  const segments = [];
  const n = 1 + Math.floor(random() * 3);
  for (let i = 0; i < n; i++)
    segments.push(pick(SEGMENTS));
  let pattern = segments.join('/');
  if (random() < 0.2)
    pattern = './' + pattern;
  if (random() < 0.03)
    pattern = '../' + pattern;
  if (random() < 0.08)
    pattern = pattern + '/';
  if (random() < 0.12)
    pattern = '!' + pattern;
  return pattern;
};

const SAMPLE_PATHS = [...DIRECTORIES, 'packages', 'packages/a/package.json', 'a', 'b', 'aa', 'aaa', 'ab', 'abab',
  '.hidden', 'a/.b', 'x/node_modules', '(a)', 'a*b', '[a]', '{a}', 'a,b', '@s', '+p', 'a|b', '$', '^a',
  'constructor', 'package.json'];
const OPTIONS = {dot: false, nobrace: false, nocase: false, noextglob: false, noglobstar: false, posix: true,
  windows: false};

const count = Number(countArg || 0);
const patterns = [];
for (let i = 0; i < count; i++)
  patterns.push(draw());
for (const s of SEGMENTS)
  patterns.push(s);

const scanRow = pattern => [escape(pattern), escape(JSON.stringify(scan(pattern, {parts: true}).parts)),
  load('tinyglobby').isDynamicPattern(pattern) ? '1' : '0'];
const picomatchRow = pattern => {
  let source;
  const tests = {};
  try {
    const regex = picomatch.makeRe(pattern, OPTIONS, false, true);
    source = `^(?:${regex.state.output})$`;
    if (regex.state.negated === true)
      source = `^(?!${source}).*$`;
    const isMatch = picomatch(pattern, OPTIONS);
    for (const sample of SAMPLE_PATHS)
      tests[sample] = isMatch(sample);
  } catch (error) {
    source = `!${error.name}: ${error.message}`;
  }
  return [escape(pattern), escape(source), escape(JSON.stringify(tests))];
};

// @pnpm/workspace.projects-reader findPackages + findWorkspaceProjectsNoCheck as pnpm 11.2.2 runs them
const lexCompare = (a, b) => a < b ? -1 : a > b ? 1 : 0;
const normalizePatterns = ps => ps.map(p => p.replace(/\/?$/, '/package.{json,yaml,json5}'));
async function findWorkspaceProjects(root, patterns) {
  const {glob} = load('tinyglobby');
  const globOpts = {cwd: root, expandDirectories: false, ignore: ['**/node_modules/**', '**/bower_components/**']};
  const paths = await glob(normalizePatterns(patterns), globOpts);
  paths.push(...(await glob(normalizePatterns(['.']), globOpts)));
  const dirs = [...new Set(paths.map(p => path.join(root, p))
    .sort((a, b) => lexCompare(path.dirname(a), path.dirname(b))))]
    .map(p => path.relative(root, path.dirname(p)));
  return dirs.sort(lexCompare);
}

// deeper than any pattern climbs, so `..` only ever reaches directories the generator made
const sandbox = fs.realpathSync(fs.mkdtempSync(path.join(os.tmpdir(), 'pnpm-globs-')));
const tree = path.join(sandbox, 'u', 'v', 'w', 'x', 'y', 'z', 'root');
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
  try {
    answer = JSON.stringify(await findWorkspaceProjects(tree, list));
  } catch (error) {
    answer = `!${error.name}: ${error.message.split(tree).join('<root>').split(sandbox).join('<sandbox>')}`;
  }
  return [escape(JSON.stringify(dirs)), escape(JSON.stringify(list)), escape(answer)];
}

const picomatchRows = [];
const scanRows = [];
const workspacesRows = [];
async function draws() {
  for (const p of new Set(patterns)) {
    const pattern = p.startsWith('!') && p[1] !== '(' ? p.slice(1) : p;
    if (pattern === '')
      continue;
    scanRows.push(scanRow(pattern));
    picomatchRows.push(picomatchRow(pattern));
  }
  const rows = Math.floor(count / 4);
  for (let i = 0; i < rows; i++) {
    const dirs = DIRECTORIES.filter(() => random() < 0.5);
    if (random() < 0.7)
      dirs.push('');
    const list = [];
    const n = 1 + Math.floor(random() * 3);
    for (let j = 0; j < n; j++)
      list.push(draw());
    workspacesRows.push(await membersRow(dirs, list));
  }
}

async function answerAgain() {
  const read = name => zlib.gunzipSync(fs.readFileSync(path.join(rowsDir, name))).toString('utf8')
    .split('\n').filter(Boolean).map(line => line.split('\t').map(unescape));
  for (const row of read('globs-scan.tsv.gz'))
    scanRows.push(scanRow(row[0]));
  for (const row of read('globs-picomatch.tsv.gz'))
    picomatchRows.push(picomatchRow(row[0]));
  for (const row of read('globs-workspaces.tsv.gz'))
    workspacesRows.push(await membersRow(JSON.parse(row[0]), JSON.parse(row[1])));
}

const write = (name, rows) => {
  fs.mkdirSync(outDir, {recursive: true});
  fs.writeFileSync(path.join(outDir, name),
    zlib.gzipSync(rows.map(r => r.join('\t')).join('\n') + '\n', {level: 9}));
  console.log(name, rows.length);
};

(rowsDir ? answerAgain() : draws()).then(() => {
  fs.rmSync(sandbox, {recursive: true, force: true});
  write('globs-picomatch.tsv.gz', picomatchRows);
  write('globs-scan.tsv.gz', scanRows);
  write('globs-workspaces.tsv.gz', workspacesRows);
});
