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
// request-authorization.jsonl: the Authorization header yarn 1.22.22's NpmRegistry.request sends, one JSON line per
// .npmrc, package and url. Usage: node request-authorization.js <yarn 1.22.22>/lib/cli.js > request-authorization.jsonl
'use strict'
const yarn = require('./yarn-bundle.js')(process.argv[2])
const NpmRegistry = yarn.load(/isRequestToRegistry\(requestUrl, registryUrl\) \{/).default
const base = {
  registry: 'https://registry.example.com/npm/',
  '//registry.example.com/npm/:_authToken': 'default-token',
  '@corp:registry': 'https://corp.example.com/',
  '//corp.example.com/:_authToken': 'corp-token',
}
const configs = [
  base,
  Object.assign({'always-auth': true}, base),
  Object.assign({'//registry.example.com/npm/:always-auth': true}, base),
  {'//registry.npmjs.org/:_authToken': 'npm-token', 'always-auth': true},
  {registry: 'https://b.example/', '//b.example/:username': 'u', '//b.example/:_password': 'cA==', 'always-auth': true},
  {registry: 'https://a.example/', '//a.example/:_auth': 'dTpw', 'always-auth': true},
  {registry: 'https://h.example/r/', 'https://h.example/r/:_authToken': 't', 'always-auth': true},
  {registry: 'https://s.example/registry/', '//s.example/:_authToken': 's', 'always-auth': true},
  {registry: 'https://n.example/', '//o.example/x/:_authToken': 'o'},
]
const rows = [
  ['lodash', 'https://registry.example.com/npm/lodash'],
  ['lodash', 'https://registry.example.com/npm/mirror/lodash'],
  ['lodash', 'https://registry.example.com/npm-other/lodash'],
  ['lodash', 'https://registry.example.com/lodash'],
  ['lodash', 'https://registry.yarnpkg.com/lodash'],
  ['lodash', 'https://registry.npmjs.org/lodash'],
  ['@corp/a', 'https://corp.example.com/@corp%2fa'],
  ['@corp/a', 'https://registry.example.com/npm/@corp%2fa'],
  ['lodash', 'https://corp.example.com/lodash'],
  ['lodash', 'https://corp.example.com/lodash/-/lodash-4.17.21.tgz'],
  ['lodash', 'https://WWW.Registry.Example.com:443/npm//lodash'],
  ['lodash', 'https://b.example/lodash'],
  ['lodash', 'https://a.example/lodash'],
  ['lodash', 'https://h.example/r/lodash'],
  ['lodash', 'https://s.example/registry/lodash'],
  ['lodash', 'https://o.example/x/lodash'],
]
// unscoped packages under configs without always-auth, at urls normalize-url rewrites in ways the port does not
const unauthenticatedRows = [
  ['lodash', 'https://registry.example.com/npm/lodash?b=2&a=1'],
  ['lodash', 'https://registry.example.com/npm/lodash#fragment'],
  ['lodash', 'https://registry.example.com/npm/./lodash'],
  ['lodash', 'https://registry.example.com/npm/%E2%82%AC/lodash'],
  ['lodash', 'https://régistry.example.com/npm/lodash'],
]
// the yarn registry's own config is its defaults, whose registry getRegistry falls back to
const registries = {
  yarn: {getScopedOption: (scope, option) => scope === '' && option === 'registry' ? 'https://registry.yarnpkg.com' :
    undefined},
}
;(async () => {
  for (const config of configs) {
    const npmrc = Object.entries(config).map(([key, value]) => `${key}=${value}\n`).join('')
    const seen = []
    const requestManager = {request: params => { seen.push(params); return Promise.resolve({}) }}
    const registry = new NpmRegistry('/tmp', registries, requestManager, {})
    registry.config = Object.assign({}, config)
    const unauthenticated = !config['always-auth'] && !config['//registry.example.com/npm/:always-auth']
    for (const [name, url] of unauthenticated ? rows.concat(unauthenticatedRows) : rows) {
      seen.length = 0
      await registry.request(url, {}, name)
      const headers = seen.length ? seen[0].headers || {} : {}
      console.log(JSON.stringify({npmrc, name, url, authorization: headers.authorization || null}))
    }
  }
})()
