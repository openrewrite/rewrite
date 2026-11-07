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
// SemverRangeIntersectTest's rows: semver-range-intersect 0.3.1 over the range lists pnpm 11.2.2 passes it.
// Usage: NODE_PATH=<dir holding semver-range-intersect@0.3.1> node semver-range-intersect.js
'use strict';

const fs = require('fs');
const path = require('path');
// from NODE_PATH alone: a node_modules above this file would otherwise come first
const dir = path.dirname(require.resolve('semver-range-intersect/package.json', {paths: [process.env.NODE_PATH]}));
const version = JSON.parse(fs.readFileSync(path.join(dir, 'package.json'), 'utf8')).version;
if (version !== '0.3.1') {
  throw new Error(`semver-range-intersect ${version}, not 0.3.1`);
}
const {intersect} = require(dir);
const cases = [['^1.0.0 || >=3.0.0 <2.0.0', '1'], ['>=3.0.0 <2.0.0', '*'], ['^1.0.0', '^1.2.0'], ['^1.0.0', '^2.0.0']];
for (const ranges of cases) {
  try {
    console.log(JSON.stringify(ranges), '->', JSON.stringify(intersect(...ranges)));
  } catch (e) {
    console.log(JSON.stringify(ranges), 'throws', e.constructor.name, e.message);
  }
}
