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
// Writes ini-cases: the envVarTest cases of bun's test/js/bun/ini/ini.test.ts (bun-v1.3.10), each with its
// name, the .ini text, the environment and the object bun's parser makes of it.
//
// Usage: node ini-cases.js <path to ini.test.ts> > ini-cases
const fs = require('fs');

const source = fs.readFileSync(process.argv[2], 'utf8')
  .replace(/^import .*$/gm, '')
  .replace(/^const \{ iniInternals \} = .*$/m, '')
  .replace(/^const \{ parse \} = .*$/m, '')
  .replace(/function envVarTest\(args: \{[^}]*\}\)/, 'function envVarTest_(args)');
const cases = [];
const AsyncFunction = Object.getPrototypeOf(async function () {}).constructor;
const sandbox = new AsyncFunction('describe', 'test', 'it', 'envVarTest', 'expect', 'parse', 'Bun', 'iniInternals',
  source);
const noop = () => {};
sandbox((name, fn) => fn(), noop, noop, args => cases.push(args), () => ({}), noop, {}, {});
process.stdout.write(JSON.stringify(cases.map(c => ({name: c.name, ini: c.ini, env: c.env, expected: c.expected})),
  null, 1) + '\n');
