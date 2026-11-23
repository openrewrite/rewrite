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
// The hash= each builtin compat patch of a Yarn release gives a package version: patchUtils.makePatchHash hashes the
// patch's effects whose semver exclusivity the version satisfies (and those without one), so a patch is its ranges,
// each with the hash its effects give, and the hash of the effects without a range. The patch text is the one
// plugin-compat ships (sources/patches/<name>.patch.ts), parsed by the release's own tools/parse.ts.
//   compatPatchHashes(fetchText, ref, names) -> {<name>: {ranges: [[range, hash], ...], none: hash}}
// With --augment <yarn-releases.json.gz> <dir of release sources> it adds each release's table to the data from sources
// laid out as <dir>/<version>/packages/... (the sweep's inventory), as releases.js adds it from the tags.
'use strict';

const crypto = require('crypto');
const fs = require('fs');
const path = require('path');
const {stripTypeScriptTypes} = require('module');
const vm = require('vm');
const zlib = require('zlib');

// plugin-patch's constants.ts
const CACHE_VERSION = 3;

function makeHash(...parts) {
  return crypto.createHash('sha512').update(parts.join('')).digest('hex');
}

// tools/parse.ts as JavaScript, with the few @yarnpkg/core and @yarnpkg/fslib functions it calls
function evaluateParser(source) {
  const js = stripTypeScriptTypes(source.replace(/^import .*$/gm, ''), {mode: 'transform'})
    .replace(/^export (function|const|enum|class|let|var) /gm, '$1 ');
  const sandbox = {
    module: {exports: null},
    ppath: path.posix,
    npath: {toPortablePath: p => p},
    PortablePath: {root: '/'},
    miscUtils: {assertNever: value => { throw new Error(`Assertion failed: Unexpected object '${value}'`); }},
  };
  vm.runInNewContext(`${js}\nmodule.exports = {parsePatchFile};`, sandbox);
  return sandbox.module.exports.parsePatchFile;
}

function patchText(source) {
  const m = source.match(/brotliDecompressSync\(Buffer\.from\(`([^`]*)`,\s*`base64`\)\)/);
  if (!m)
    throw new Error('no brotli payload in the patch module');
  return zlib.brotliDecompressSync(Buffer.from(m[1], 'base64')).toString('utf8');
}

function table(parsePatchFile, text) {
  const effects = parsePatchFile(text);
  const ranges = [];
  for (const effect of effects)
    if (effect.semverExclusivity !== null && !ranges.includes(effect.semverExclusivity))
      ranges.push(effect.semverExclusivity);
  const hashOf = range => {
    const parts = [];
    for (const effect of effects) {
      const {semverExclusivity, ...effectWithoutRange} = effect;
      if (semverExclusivity !== null && semverExclusivity !== range)
        continue;
      parts.push(JSON.stringify(effectWithoutRange));
    }
    return makeHash(`${CACHE_VERSION}`, ...parts).slice(0, 6);
  };
  return {ranges: ranges.map(range => [range, hashOf(range)]), none: hashOf(null)};
}

async function compatPatchHashes(fetchText, ref, names) {
  const parsePatchFile = evaluateParser(await fetchText(`${ref}/packages/plugin-patch/sources/tools/parse.ts`));
  const out = {};
  for (const name of names)
    out[name] = table(parsePatchFile, patchText(await fetchText(`${ref}/packages/plugin-compat/sources/patches/${name}.patch.ts`)));
  return out;
}

module.exports = {compatPatchHashes};

async function augment(resource, sources) {
  const data = JSON.parse(zlib.gunzipSync(fs.readFileSync(resource)).toString('utf8'));
  const read = async file => fs.readFileSync(path.join(sources, file), 'utf8');
  for (const [version, release] of Object.entries(data.releases)) {
    const hashes = await compatPatchHashes(read, version, release.patches);
    const json = JSON.stringify(hashes);
    const id = crypto.createHash('sha256').update(json).digest('hex').slice(0, 12);
    data.tables[id] = hashes;
    release.compatPatchHashes = id;
  }
  fs.writeFileSync(resource, zlib.gzipSync(JSON.stringify(data, null, 1) + '\n', {level: 9}));
}

if (require.main === module) {
  const at = process.argv.indexOf('--augment');
  if (at < 0)
    throw new Error('usage: node compat-patch-hashes.js --augment <yarn-releases.json.gz> <dir of release sources>');
  augment(process.argv[at + 1], process.argv[at + 2]).catch(error => {
    console.error(error);
    process.exit(1);
  });
}
