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
// Writes hosted-git-info-valid: the validGitUrls table of bun's test/cli/install/hosted-git-info/cases.ts as JSON.
//
// Usage: node hosted-git-info-valid.js <bun checkout>/test/cli/install/hosted-git-info/cases.ts > hosted-git-info-valid
const fs = require('fs')
const os = require('os')
const path = require('path')
const {spawnSync} = require('child_process')

const bun = spawnSync('sh', ['-c', 'command -v bun'], {encoding: 'utf8'}).stdout.trim()
const tmp = fs.mkdtempSync(path.join(os.tmpdir(), 'hosted-git-info-'))
fs.copyFileSync(process.argv[2], path.join(tmp, 'cases.ts'))
fs.writeFileSync(path.join(tmp, 'dump.ts'), 'import { validGitUrls } from "./cases.ts";\n' +
  'process.stdout.write(JSON.stringify(validGitUrls, null, 1) + "\\n");\n')
process.stdout.write(spawnSync(bun, ['run', 'dump.ts'], {cwd: tmp, encoding: 'utf8'}).stdout)
fs.rmSync(tmp, {recursive: true, force: true})
