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
// compat-patches.json.gz: the builtin patch texts of @yarnpkg/plugin-compat at each release yarn-releases.json.gz
// lists, read at that release's commit. A text is stored as its per-file sections, each section once, since the
// releases share most of them. Usage: node compat-patches.js <resource directory of yarn-releases.json.gz>
'use strict';

const crypto = require('crypto');
const fs = require('fs');
const path = require('path');
const zlib = require('zlib');

const RAW = 'https://raw.githubusercontent.com/yarnpkg/berry';

async function fetchText(url) {
  const res = await fetch(url);
  if (!res.ok)
    throw new Error(`${url}: ${res.status}`);
  return await res.text();
}

// patches/<name>.patch.ts holds the patch brotli-compressed and base64-encoded
async function patchAt(sha, name) {
  const source = await fetchText(`${RAW}/${sha}/packages/plugin-compat/sources/patches/${name}.patch.ts`);
  const match = source.match(/Buffer\.from\(`([^`]*)`, `base64`\)/);
  if (!match)
    throw new Error(`no patch data in ${name}.patch.ts at ${sha}`);
  return zlib.brotliDecompressSync(Buffer.from(match[1], 'base64')).toString('utf8');
}

async function main() {
  const dir = process.argv[2];
  if (!dir)
    throw new Error('usage: node compat-patches.js <resource directory>');
  const data = JSON.parse(zlib.gunzipSync(fs.readFileSync(path.join(dir, 'yarn-releases.json.gz'))).toString('utf8'));
  const sections = {};
  const releases = {};
  for (const [version, release] of Object.entries(data.releases)) {
    process.stderr.write(`${version} ${release.commit}\n`);
    releases[version] = {};
    for (const name of release.patches) {
      const text = await patchAt(release.commit, name);
      const parts = text.split(/(?=^diff --git )/m);
      if (parts.join('') !== text)
        throw new Error(`${name} at ${version} does not split into sections`);
      releases[version][name] = parts.map(part => {
        const id = crypto.createHash('sha256').update(part).digest('hex').slice(0, 12);
        sections[id] = part;
        return id;
      });
    }
  }
  const json = JSON.stringify({releases, sections}) + '\n';
  fs.writeFileSync(path.join(dir, 'compat-patches.json.gz'), zlib.gzipSync(json, {level: 9}));
}

main().catch(error => {
  console.error(error);
  process.exit(1);
});
