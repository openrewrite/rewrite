// Copyright 2026 the original author or authors.
// Licensed under the Moderne Source Available License.
// https://docs.moderne.io/licensing/moderne-source-available-license
// Development-only generator. Reads the supported release inventory and fetches its exact tagged patch modules.
// node compat-patches.js <rewrite-javascript module directory>
'use strict';
const fs = require('fs');
const path = require('path');
const zlib = require('zlib');
const crypto = require('crypto');
const root = path.join(process.argv[2], 'src/main/resources/org/openrewrite/javascript/internal/lock/berry');
const existingIndex = JSON.parse(zlib.gunzipSync(fs.readFileSync(path.join(root, 'compat-patches.json.gz'))));
const releases = JSON.parse(zlib.gunzipSync(fs.readFileSync(path.join(root, 'yarn-releases.json.gz')))).releases;
async function main() {
  const index = {};
  const texts = new Map();
  for (const [version, release] of Object.entries(releases)) {
    index[version] = {};
    for (const name of release.patches) {
      const url = `https://raw.githubusercontent.com/yarnpkg/berry/@yarnpkg/cli/${version}/packages/plugin-compat/sources/patches/${name}.patch.ts`;
      const response = await fetch(url);
      if (!response.ok) throw new Error(`${url}: ${response.status}`);
      const module = await response.text();
      const match = module.match(/brotliDecompressSync\(Buffer\.from\(`([^`]*)`,\s*`base64`\)\)/);
      if (!match) throw new Error(`No patch payload: ${url}`);
      const text = zlib.brotliDecompressSync(Buffer.from(match[1], 'base64'));
      const sha = crypto.createHash('sha256').update(text).digest('hex');
      if (existingIndex[version] && existingIndex[version][name] !== sha)
        throw new Error(`Upstream patch changed for ${version}/${name}; review before updating the inventory`);
      index[version][name] = sha;
      texts.set(sha, text);
    }
  }
  const dir = path.join(root, 'compat');
  fs.mkdirSync(dir, {recursive:true});
  for (const [sha, text] of texts) fs.writeFileSync(path.join(dir, sha + '.patch.gz'), zlib.gzipSync(text, {level:9}));
  fs.writeFileSync(path.join(root, 'compat-patches.json.gz'), zlib.gzipSync(JSON.stringify(index), {level:9}));
  console.log(`${Object.keys(index).length} releases; ${texts.size} unique patch texts`);
}
main().catch(error => { console.error(error); process.exit(1); });
