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
// NpmResolverTest's registry-environment rows: yarn 1.22.22 run live with each variable set, and the lock's url.
// Usage: node registry-env-rows.js <yarn>/lib/cli.js
const fs = require('fs')
const os = require('os')
const path = require('path')
const {execFileSync} = require('child_process')

const cli = process.argv[2]
const rows = [['none', ''], ['YARN_REGISTRY', 'https://registry.npmjs.org'],
  ['yarn_registry', 'https://registry.npmjs.org'],
  ['npm_config_registry', 'https://registry.npmjs.org'], ['NPM_CONFIG_REGISTRY', 'https://registry.npmjs.org']]
for (const [variable, value] of rows) {
  const scratch = fs.mkdtempSync(path.join(os.tmpdir(), 'registry-env-'))
  const project = path.join(scratch, 'project')
  fs.mkdirSync(project)
  fs.mkdirSync(path.join(scratch, 'home'))
  fs.writeFileSync(path.join(project, 'package.json'),
    '{"name":"p","version":"1.0.0","dependencies":{"left-pad":"1.3.0"}}')
  const env = {PATH: path.dirname(process.execPath) + ':/usr/bin:/bin', HOME: path.join(scratch, 'home'),
    YARN_CACHE_FOLDER: path.join(scratch, 'cache'), TMPDIR: scratch}
  if (value) env[variable] = value
  execFileSync(process.execPath, [cli, 'install', '--ignore-scripts', '--non-interactive', '--no-progress'],
    {cwd: project, env, stdio: 'ignore'})
  const resolved = fs.readFileSync(path.join(project, 'yarn.lock'), 'utf8').match(/resolved "([^"]+)\/left-pad\/-\//)[1]
  console.log(`"${variable}, ${value}, ${resolved}",`)
  fs.rmSync(scratch, {recursive: true, force: true})
}
