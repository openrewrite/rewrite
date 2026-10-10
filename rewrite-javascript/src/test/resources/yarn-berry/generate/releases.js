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
// yarn-releases.json.gz: the data each Yarn 4.x release ships that reaches a lock, read at its berry tag.
// Usage: node releases.js <rewrite-javascript/src/main/resources/org/openrewrite/javascript/internal/lock/berry>
'use strict';

const {execFileSync} = require('child_process');
const crypto = require('crypto');
const fs = require('fs');
const {stripTypeScriptTypes} = require('module');
const os = require('os');
const path = require('path');
const vm = require('vm');
const zlib = require('zlib');

const REPO = 'https://github.com/yarnpkg/berry';
const RAW = 'https://raw.githubusercontent.com/yarnpkg/berry';

async function fetchText(url) {
  const res = await fetch(url);
  if (res.status === 404)
    return null;
  if (!res.ok)
    throw new Error(`${url}: ${res.status}`);
  return await res.text();
}

async function fetchBuffer(url) {
  const res = await fetch(url);
  if (!res.ok)
    throw new Error(`${url}: ${res.status}`);
  return Buffer.from(await res.arrayBuffer());
}

function releases() {
  const out = execFileSync('git', ['ls-remote', '--tags', REPO, 'refs/tags/@yarnpkg/cli/4.*'], {encoding: 'utf8'});
  const commits = new Map();
  for (const line of out.trim().split('\n')) {
    const [sha, ref] = line.split('\t');
    const match = ref.match(/^refs\/tags\/@yarnpkg\/cli\/(4\.\d+\.\d+)(\^\{\})?$/);
    if (!match)
      continue;
    if (match[2] || !commits.has(match[1]))
      commits.set(match[1], sha);
  }
  const compare = (a, b) => {
    const x = a.split('.').map(Number);
    const y = b.split('.').map(Number);
    return x[0] - y[0] || x[1] - y[1] || x[2] - y[2];
  };
  return [...commits.entries()].sort(([a], [b]) => compare(a, b));
}

// Node 24's type stripping turns the TypeScript module into JavaScript
function evaluateExtensions(source) {
  const js = stripTypeScriptTypes(source.replace(/^import .*$/gm, ''))
    .replace(/export const packageExtensions\s*=/, 'module.exports =');
  const sandbox = {module: {exports: null}};
  vm.runInNewContext(js, sandbox);
  return JSON.parse(JSON.stringify(sandbox.module.exports));
}

async function extensionsAt(sha) {
  const moved = await fetchText(`${RAW}/${sha}/packages/yarnpkg-extensions/sources/index.ts`);
  if (moved !== null)
    return evaluateExtensions(moved);
  return evaluateExtensions(await fetchText(`${RAW}/${sha}/packages/plugin-compat/sources/extensions.ts`));
}

async function patchesAt(sha) {
  const source = await fetchText(`${RAW}/${sha}/packages/plugin-compat/sources/index.ts`);
  const names = [];
  for (const match of source.matchAll(/\[structUtils\.makeIdent\(null, `([^`]+)`\)\.identHash/g))
    names.push(match[1]);
  return names;
}

async function nodeGypAt(sha) {
  const source = await fetchText(`${RAW}/${sha}/packages/plugin-npm/sources/NpmSemverResolver.ts`);
  const pattern = source.match(/const NODE_GYP_MATCH = \/(.+)\/;/);
  const range = source.match(/makeDescriptor\(NODE_GYP_IDENT, `([^`]+)`\)/);
  if (!pattern || !range)
    throw new Error(`no node-gyp rule at ${sha}`);
  return {pattern: pattern[1], range: range[1]};
}

async function pluginsAt(sha) {
  const manifest = JSON.parse(await fetchText(`${RAW}/${sha}/packages/yarnpkg-cli/package.json`));
  return manifest[`@yarnpkg/builder`].bundles.standard;
}

async function jsrScopeAt(sha) {
  const source = await fetchText(`${RAW}/${sha}/packages/plugin-npm/sources/npmConfigUtils.ts`);
  return /if \(scope === `jsr`\)/.test(source);
}

// generateLockfile writes a checksum without its cache key when the key is the lockfile's
async function checksumWithoutCacheKeyAt(sha) {
  const source = await fetchText(`${RAW}/${sha}/packages/yarnpkg-core/sources/Project.ts`);
  return /if \(entryCacheKey === cacheKey\) \{\s*entryChecksum = hash;/.test(source);
}

// the command the usage errors for an unknown setting and an unknown key of a shape tell the user to run
async function unknownSettingHintAt(sha) {
  const source = await fetchText(`${RAW}/${sha}/packages/yarnpkg-core/sources/Configuration.ts`);
  const hint = source.match(/Unrecognized or legacy configuration settings found: \$\{key\} - run "([^"]+)"/);
  const shape = source.match(/Unrecognized configuration settings found: \$\{path\}\.\$\{propKey\} - run "([^"]+)"/);
  if (!hint || !shape)
    throw new Error(`no unknown-setting message at ${sha}`);
  if (hint[1] !== shape[1])
    throw new Error(`the unknown-setting messages at ${sha} name different commands`);
  return hint[1];
}

function untar(tgz, wanted) {
  const tar = zlib.gunzipSync(tgz);
  for (let offset = 0; offset + 512 <= tar.length;) {
    const name = tar.toString('utf8', offset, offset + 100).replace(/\0.*$/s, '');
    const size = parseInt(tar.toString('utf8', offset + 124, offset + 136).replace(/\0.*$/s, '').trim() || '0', 8);
    if (name === wanted)
      return tar.subarray(offset + 512, offset + 512 + size);
    offset += 512 + Math.ceil(size / 512) * 512;
  }
  throw new Error(`${wanted} not in the tarball`);
}

// the release bundle where corepack downloads it from, else the @yarnpkg/cli-dist package
async function bundleOf(version) {
  const res = await fetch(`https://repo.yarnpkg.com/${version}/packages/yarnpkg-cli/bin/yarn.js`);
  if (res.ok)
    return Buffer.from(await res.arrayBuffer());
  const tgz = await fetchBuffer(`https://registry.npmjs.org/@yarnpkg/cli-dist/-/cli-dist-${version}.tgz`);
  return untar(tgz, 'package/bin/yarn.js');
}

async function settingsOf(version) {
  const bundle = await bundleOf(version);
  const dir = fs.mkdtempSync(path.join(os.tmpdir(), 'yarn-settings-'));
  try {
    fs.writeFileSync(path.join(dir, 'yarn.js'), bundle);
    fs.mkdirSync(path.join(dir, 'project'));
    fs.writeFileSync(path.join(dir, 'project', 'package.json'), '{}\n');
    const env = {PATH: process.env.PATH, HOME: dir, YARN_ENABLE_TELEMETRY: '0'};
    const out = execFileSync(process.execPath, [path.join(dir, 'yarn.js'), 'config', '--json'],
      {cwd: path.join(dir, 'project'), env, encoding: 'utf8', maxBuffer: 64 * 1024 * 1024});
    return out.trim().split('\n').map(line => JSON.parse(line).key).sort();
  } finally {
    fs.rmSync(dir, {recursive: true, force: true});
  }
}

function store(tables, value) {
  const json = JSON.stringify(value);
  const id = crypto.createHash('sha256').update(json).digest('hex').slice(0, 12);
  tables[id] = value;
  return id;
}

async function main() {
  const outDir = process.argv[2];
  if (!outDir)
    throw new Error('usage: node releases.js <resource directory>');
  const tables = {};
  const out = {};
  for (const [version, sha] of releases()) {
    process.stderr.write(`${version} ${sha}\n`);
    out[version] = {
      commit: sha,
      packageExtensions: store(tables, await extensionsAt(sha)),
      patches: await patchesAt(sha),
      nodeGyp: await nodeGypAt(sha),
      plugins: store(tables, await pluginsAt(sha)),
      jsrScope: await jsrScopeAt(sha),
      checksumWithoutCacheKey: await checksumWithoutCacheKeyAt(sha),
      settings: store(tables, await settingsOf(version)),
      unknownSettingHint: await unknownSettingHintAt(sha),
    };
  }
  const json = JSON.stringify({releases: out, tables}, null, 1) + '\n';
  fs.writeFileSync(path.join(outDir, 'yarn-releases.json.gz'), zlib.gzipSync(json, {level: 9}));
}

main().catch(error => {
  console.error(error);
  process.exit(1);
});
