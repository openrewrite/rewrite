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
// Loads a module from the node_modules npm bundles (arborist, npm-package-arg, semver, ...): the npm beside
// the running node, unless NPM_NODE_MODULES names another.
const path = require('path')
const dir = process.env.NPM_NODE_MODULES ||
  path.resolve(path.dirname(process.execPath), '../lib/node_modules/npm/node_modules')
module.exports = name => require(path.join(dir, name))
module.exports.dir = dir
module.exports.sortKeys = v => {
  if (Array.isArray(v)) {
    return v.map(module.exports.sortKeys)
  }
  if (v && typeof v === 'object') {
    const o = {}
    for (const k of Object.keys(v).sort()) {
      o[k] = module.exports.sortKeys(v[k])
    }
    return o
  }
  return v
}
