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
// long-map-inherited-name.txt: yarn 1.22.22's own stringify of a 64-key map holding `constructor`.
// Usage: node long-map-inherited-name.js <yarn 1.22.22>/lib/cli.js > long-map-inherited-name.txt
'use strict'
const yarn = require('./yarn-bundle.js')(process.argv[2])
const stringify = yarn.load(/lines\.push\(`# yarn lockfile v/).default
const deps = {}
for (let i = 0; i < 63; i++) deps['dep-' + i] = '1.0.0'
deps.constructor = '1.0.0'
process.stdout.write(stringify({'a@1.0.0': {dependencies: deps}}, false, false))
