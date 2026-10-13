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
// Writes compat-db.json.gz, the compatibility database pnpm 11.2.2 applies to every manifest it reads (unless
// ignoreCompatibilityDb): the packageExtensions array of @yarnpkg/extensions 2.0.6, as [selector, extension] pairs in
// the package's order, which pnpm turns into an object with Object.fromEntries.
//
// Usage: NODE_PATH=<dir holding @yarnpkg/extensions@2.0.6> node compat-db.js <out-dir>
'use strict';

const fs = require('fs');
const path = require('path');
const zlib = require('zlib');
// from NODE_PATH alone: a node_modules above this file would otherwise come first
const dir = path.dirname(require.resolve('@yarnpkg/extensions/package.json', {paths: [process.env.NODE_PATH]}));
const version = JSON.parse(fs.readFileSync(path.join(dir, 'package.json'), 'utf8')).version;
if (version !== '2.0.6') {
  throw new Error(`@yarnpkg/extensions ${version}, not 2.0.6`);
}
const {packageExtensions} = require(path.join(dir, 'lib', 'index.js'));
const json = JSON.stringify(packageExtensions);
fs.writeFileSync(path.join(process.argv[2], 'compat-db.json.gz'), zlib.gzipSync(Buffer.from(json, 'utf8'), {level: 9}));
console.log(`${packageExtensions.length} entries, ${json.length} bytes`);
