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
// NpmLockRegeneratorTest's and NpmInputsTest's walk rows: `npm prefix` run in packages/a under each layout.
// Usage: node npm-prefix-cases.js (with npm 11.17.0 beside the running node)
const childProcess = require('child_process')
const fs = require('fs')
const os = require('os')
const path = require('path')
const npm = require('./npm-modules.js')
const mapWorkspaces = npm('@npmcli/map-workspaces')
const PackageJson = npm('@npmcli/package-json')

const sandbox = fs.realpathSync(fs.mkdtempSync(path.join(os.tmpdir(), 'npm-prefix-')))
const MEMBER = '{"name":"a","version":"1.0.0"}'

const layout = files => {
  const root = path.join(sandbox, 'r')
  fs.rmSync(root, { recursive: true, force: true })
  for (const [file, text] of Object.entries({ 'packages/a/package.json': MEMBER, ...files })) {
    fs.mkdirSync(path.dirname(path.join(root, file)), { recursive: true })
    fs.writeFileSync(path.join(root, file), text + '\n')
  }
  return root
}

const prefix = root => {
  const result = childProcess.spawnSync(path.join(path.dirname(process.execPath), 'npm'),
    ['prefix', '--userconfig', '/dev/null'], {
      cwd: path.join(root, 'packages/a'),
      env: { PATH: path.dirname(process.execPath) + ':/usr/bin:/bin', HOME: path.join(sandbox, 'home') },
      encoding: 'utf8',
    })
  return result.status === 0 ? path.relative(path.join(root, 'packages/a'), result.stdout.trim()) : null
}

// the error npm's walk stops on, from the modules it runs at the root
const walkError = async root => {
  try {
    const { content: pkg } = await PackageJson.normalize(root).catch(() => ({ content: {} }))
    if (pkg.workspaces) {
      await mapWorkspaces({ cwd: root, pkg })
    }
  } catch (e) {
    return e.code ? 'rejects ' + e.code : 'fails ' + e.message
  }
  return 'npm prefix failed, the walk did not'
}

const WALKS = [
  ['bin-dir-no-workspaces', '{"name":"root","directories":{"bin":"bin"}}'],
  ['bin-dir-lists', '{"name":"root","directories":{"bin":"bin"},"workspaces":["packages/*"]}'],
  ['bin-dir-not-listing', '{"name":"root","directories":{"bin":"bin"},"workspaces":["libs/*"]}'],
  ['bin-dir-with-files-lists', '{"name":"root","directories":{"bin":"bin"},"workspaces":["packages/*"]}',
    'bin/tool.js=#!/usr/bin/env node'],
  ['bin-dir-number-lists', '{"name":"root","directories":{"bin":1},"workspaces":["packages/*"]}'],
  ['bin-dir-array-lists', '{"name":"root","directories":{"bin":["bin"]},"workspaces":["packages/*"]}'],
  ['mid-bin-dir-root-lists', '{"name":"root","workspaces":["packages/*"]}',
    'packages/package.json={"name":"mid","directories":{"bin":"bin"}}'],
  ['unlisted-duplicate-names', '{"name":"root","workspaces":["libs/*"]}',
    'libs/x/package.json={"name":"dup"};libs/y/package.json={"name":"dup"}'],
  ['listed-duplicate-of-project', '{"name":"root","workspaces":["packages/*"]}',
    'packages/b/package.json={"name":"a"}'],
  ['unlisted-folder-names-collide', '{"name":"root","workspaces":["libs/*","other/*"]}',
    'libs/x/package.json={};other/x/package.json={}'],
  ['unlisted-member-unparsable', '{"name":"root","workspaces":["libs/*"]}', 'libs/x/package.json={'],
  ['unlisted-member-null', '{"name":"root","workspaces":["libs/*"]}', 'libs/x/package.json=null'],
  ['unlisted-member-bin-dir-number', '{"name":"root","workspaces":["libs/*"]}',
    'libs/x/package.json={"name":"x","directories":{"bin":1}}'],
  ['unlisted-member-bin-dir', '{"name":"root","workspaces":["libs/*"]}',
    'libs/x/package.json={"name":"x","directories":{"bin":"bin"}}'],
  ['listed-member-unparsable', '{"name":"root","workspaces":["packages/*"]}', 'packages/b/package.json={'],
  ['workspaces-not-an-array', '{"name":"root","workspaces":"packages/*"}'],
  ['unlisted-duplicate-under-node-modules', '{"name":"root","workspaces":["libs/**"]}',
    'libs/x/package.json={"name":"dup"};libs/x/node_modules/y/package.json={"name":"dup"}'],
  ['unlisted-member-name-number', '{"name":"root","workspaces":["libs/*"]}', 'libs/x/package.json={"name":5}'],
  ['listed-plus-member-name-number', '{"name":"root","workspaces":["packages/*","libs/*"]}',
    'libs/x/package.json={"name":5}'],
]

const PREFIXES = [
  ['{"private":true,"workspaces":["packages/*"]}'],
  ['{"private":true,"workspaces":{"packages":["packages/*"]}}'],
  ['{"workspaces":["packages/*"]}'],
  ['{"private":true,"workspaces":["packages/*"]}', '{"name":"mid"}'],
  ['{"private":true,"workspaces":["packages/*"]}', '{"name":"mid","workspaces":["x/*"]}'],
  ['{"private":true,"workspaces":["packages/*"]}', '{"name":"mid","workspaces":["a"]}'],
  ['{"private":true,"workspaces":["libs/*"]}'],
  ['{"private":true,"workspaces":["packages/*","!packages/a"]}'],
  ['{"workspaces":["packages/*"]'],
]

const run = async () => {
  console.log('NpmInputsTest: name|root|files|outcome')
  for (const [name, rootManifest, files = ''] of WALKS) {
    const declared = { 'package.json': rootManifest }
    for (const file of files ? files.split(';') : []) {
      declared[file.slice(0, file.indexOf('='))] = file.slice(file.indexOf('=') + 1)
    }
    const root = layout(declared)
    const at = prefix(root)
    const outcome = at === null ? await walkError(root) : at === '' ? 'installs here' : 'installs at ' + at
    console.log([name, rootManifest, files, outcome].join('|'))
  }
  console.log('NpmLockRegeneratorTest: root|mid|installsAt')
  for (const [rootManifest, mid = ''] of PREFIXES) {
    const root = layout(mid ? { 'package.json': rootManifest, 'packages/package.json': mid } :
      { 'package.json': rootManifest })
    console.log([rootManifest, mid, prefix(root)].join('|'))
  }
  fs.rmSync(sandbox, { recursive: true, force: true })
}
run()
