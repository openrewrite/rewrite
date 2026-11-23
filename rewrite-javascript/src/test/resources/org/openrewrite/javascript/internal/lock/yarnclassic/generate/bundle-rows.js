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
// Prints the rows of the Yarn classic tests that hold yarn 1.22.22's own answers, one Java row per line under the name
// of the test method or row source it belongs to, each answered by yarn's bundle (lib/cli.js) with HOME, the global
// prefix and the project in an empty scratch directory: NpmResolverTest (findVersionInRegistryResponse), JsYamlTest
// (js-yaml safeLoad under FAILSAFE_SCHEMA), ConfigTest (the registries' init and getRegistry, rc.js
// getRcConfigForCwd) and NormalizeManifestTest (normalize-manifest, resolve-relative).
//
// Usage: node bundle-rows.js <yarn 1.22.22>/lib/cli.js
'use strict'
const fs = require('fs')
const os = require('os')
const path = require('path')

const scratch = fs.realpathSync(fs.mkdtempSync(path.join(os.tmpdir(), 'bundle-rows-')))
const HOME = path.join(scratch, 'home')
const PREFIX = path.join(scratch, 'prefix')
fs.mkdirSync(HOME)
fs.mkdirSync(PREFIX)
process.env.HOME = HOME
process.env.PREFIX = PREFIX
const yarn = require('./yarn-bundle.js')(process.argv[2])
const NpmResolver = yarn.load(/NpmResolver.findVersionInRegistryResponse = function/).default
const PackageConstraintResolver = yarn.load(/function PackageConstraintResolver\(config, reporter\)/).default
const BaseReporter = yarn.load(/function BaseReporter\(/).default
const jsyaml = yarn.load(/module\.exports\.safeLoad {4}= safeLoad;/)
const FAILSAFE = yarn.load(/module\.exports\.FAILSAFE_SCHEMA/).FAILSAFE_SCHEMA
const NpmRegistry = yarn.load(/NpmRegistry.prototype.getRegistry = function getRegistry/).default
const YarnRegistry = yarn.load(/YarnRegistry.prototype.saveHomeConfig = /).default
const rc = yarn.load(/exports\.getRcConfigForCwd = getRcConfigForCwd/)
const normalizeManifest = yarn.load(/if \(isRoot && info\._loc\) \{/).default
const NoopReporter = yarn.load(/NoopReporter = function \(_BaseReporter\)/).default
const nullify = yarn.load(/function nullify\(\) \{/).default
const resolveRelative = yarn.load(/hasPathPrefix = \/\^\\\.\(\\\/\|\$\)\/\.test\(unprefixed\)/).default

const q = s => s === null ? 'null' : JSON.stringify(s)
const env = e => 'env(' + Object.entries(e).map(([k, v]) => q(k) + ', ' + q(v)).join(', ') + ')'
const section = (name, rows) => console.log('== ' + name + '\n' + rows.join('\n'))
const error = e => e.name + ': ' + e.message

async function withEnv(e, cwd, rcFiles, run) {
  const saved = process.env
  process.env = Object.assign({HOME, PREFIX}, e)
  try {
    const registries = {}
    const reporter = {verbose() {}, warn() {}, lang: k => k}
    registries.npm = new NpmRegistry(cwd, registries, null, reporter, rcFiles, [])
    registries.yarn = new YarnRegistry(cwd, registries, null, reporter, rcFiles, [])
    await registries.npm.init({registry: undefined})
    await registries.yarn.init({registry: undefined})
    return run(registries.npm)
  } finally {
    process.env = saved
  }
}

async function npmResolver() {
  const reporter = new BaseReporter()
  const config = {reporter, looseSemver: true, nonInteractive: true}
  const constraint = new PackageConstraintResolver(config, reporter)
  config.resolveConstraints = (versions, range) => constraint.reduce(versions, range)
  const answer = async (name, range, body) => {
    try {
      return 'version ' + (await NpmResolver.findVersionInRegistryResponse(config, name, range, body, null)).version
    } catch (e) {
      return e.constructor.name === 'MessageError' ? e.message : error(e)
    }
  }

  const leftPad = {name: 'left-pad', 'dist-tags': {}, versions: {
    '1.1.3': {name: 'left-pad', version: '1.1.3'}, '1.3.0': {name: 'left-pad', version: '1.3.0'},
    '1.2.0': {name: 'left-pad', version: '1.2.0'}}}
  const picks = []
  for (const range of ['latest', '^1.1.0', '1.1.x']) {
    picks.push(q(range + ', ' + (await answer('left-pad', range, leftPad)).replace(/^version /, '')) + ',')
  }
  section('NpmResolverTest.picksAVersionWithoutALatestTag', picks)

  const VERSIONS = '{"1.0.0": {"name": "p", "version": "1.0.0"}, "1.2.0": {"name": "p", "version": "1.2.0"}}'
  const cases = [
    ['dist-tags a string', '"latest"', VERSIONS, '^1.0.0'],
    ['dist-tags a number', '5', VERSIONS, '^1.0.0'],
    ['dist-tags true', 'true', VERSIONS, '^1.0.0'],
    ['dist-tags an array', '["1.0.0"]', VERSIONS, '0'],
    ["an array's length", '["1.0.0"]', VERSIONS, 'length'],
    ["an array's map", '["1.0.0"]', VERSIONS, 'map'],
    ["an object's toString", '{"latest": "1.2.0"}', VERSIONS, 'toString'],
    ["an object's __proto__", '{"latest": "1.2.0"}', VERSIONS, '__proto__'],
    ['a tag that is a number', '{"next": 5}', VERSIONS, 'next'],
    ['versions a number', '{}', '5', '^1.0.0'],
    ['versions an empty array', '{}', '[]', '^1.0.0'],
    ['latest a number', '{"latest": 5}', VERSIONS, '^1.0.0'],
    ['latest a number, any range', '{"latest": 5}', VERSIONS, '*'],
    ['latest a number, two sets', '{"latest": 5}', VERSIONS, '1.x || 2.x'],
    ['latest true', '{"latest": true}', VERSIONS, '^1.0.0'],
    ['latest an object', '{"latest": {}}', VERSIONS, '>=1.0.0'],
    ['latest an array', '{"latest": [1]}', VERSIONS, '~1'],
    ['latest an array, x', '{"latest": [1]}', VERSIONS, 'x'],
    ['latest not a version', '{"latest": "garbage"}', VERSIONS, '^1.0.0'],
    ['latest a partial version', '{"latest": "1.2"}', VERSIONS, '*'],
    ['latest with =', '{"latest": "=1.2.0"}', VERSIONS, '1.2.0'],
    ['versions an array of one', '{}', '["1.0.0"]', '^1.0.0'],
    ['a version key not a version', '{}', '{"latest": {"version": "x"}}', '^1.0.0'],
  ]
  const rows = []
  for (const [label, tags, versions, range] of cases) {
    const body = JSON.parse('{"name": "p", "dist-tags": ' + tags + ', "versions": ' + versions + '}')
    rows.push('Arguments.of(' + [q(label), q(tags), versions === VERSIONS ? 'VERSIONS' : q(versions), q(range),
      q(await answer('p', range, body))].join(', ') + '),')
  }
  section('NpmResolverTest.malformed', rows)
}

function jsYaml() {
  const texts = ["'k': \n", "'k': !!int 5\n", "'k': [a, , b]\n", "'k': !!binary aGk=\n", "'k':\n\t- a\n",
    "'k': {a: }\n", "'k': !!null ''\n", "'k': !!bool true\n", "'k': !!str\n", "'k': !!seq\n", "'k': ! v\n",
    "'a': &x {version: '1'}\n'b': *x\n", "'__proto__': {version: '1'}\n'b': '2'\n",
    "'hasOwnProperty': &hasOwnProperty x\n'b': *hasOwnProperty\n", "'1': a\n'0': b\n'x': c\n",
    "'k': \"\\u0041\\x42\\U0001F600\"\n", "'k': |-\n  line1\n  line2\n", "'k': >+\n  a\n\n", '? [a, b]\n: c\n',
    '? {a: b}\n: c\n', "%YAML 2.0\n---\n'k': v\n", "---\n'k': v\n---\n'j': w\n", "'k': v\n'k': w\n", 'x: [a, b]\n']
  section('JsYamlTest.documents', texts.map(text => {
    let answer
    try {
      answer = JSON.stringify(jsyaml.safeLoad(text, {schema: FAILSAFE}))
    } catch (e) {
      answer = 'throws'
    }
    return 'Arguments.of(' + q(text) + ', ' + q(answer) + '),'
  }))
}

async function registries() {
  const none = path.join(scratch, 'none', 'project')
  const environments = [
    {YARN_REGISTRY: 'https://r.example/'}, {yarn_ignore_optional: 'true', YARN_PRODUCTION: 'false'},
    {YARN_A__B_C: 'x'}, {YARN___A: 'x'}, {YARN_A___B: 'x'}, {YARN_: 'x'}, {YARN_A__2: 'x'},
    {YARN_FOO: 'x', YARN_FOO__0: 'y'}, {YARN_REGISTRY: 'a', yarn_registry: 'b'},
    {npm_config__auth: 'x', 'npm_config_@scope:registry': 'https://s.example/', NPM_CONFIG_REGISTRY: 'r'},
  ]
  const rows = []
  for (const e of environments) {
    let answer
    try {
      answer = await withEnv(e, none, false, npm => JSON.stringify(npm.config))
    } catch (err) {
      answer = error(err)
    }
    rows.push('Arguments.of(' + env(e) + ', ' + q(answer) + '),')
  }
  section('ConfigTest.environments', rows)

  const n = {npm_config_registry: 'https://n.example/'}
  const ns = {npm_config_registry: 'https://n.example/', 'npm_config_@s:registry': 'https://s.example/'}
  const idents = [
    [{}, ['left-pad', '@scope/x']],
    [{npm_config_registry: 'http://127.0.0.1:9/'}, ['left-pad']],
    [{'npm_config_@scope:registry': 'https://s.example/'}, ['@scope/x', 'left-pad']],
    [{YARN_REGISTRY: 'https://r.example/'}, ['left-pad']],
    [{YARN_REGISTRY: 'https://r.example/', npm_config_registry: 'https://n.example/'}, ['left-pad']],
    [{npm_config_undefined: 'https://u.example/'}, ['left-pad', '@scope/x']],
    [{}, ['https://registry.npmjs.org/left-pad']],
    [n, ['https://n.example/left-pad', 'HTTPS://n.example/left-pad', '//n.example/left-pad', 'https://o.example/x']],
    [ns, ['https://s.example/@s%2fx', 'https://o.example/@s/x', 'https://o.example/x', 'https://n.example/@s/x']],
    [{'npm_config_a:registry:b': 'https://ab.example/'}, ['https://ab.example/x', 'x']],
    [{'npm_config_//h.example/:registry': 'https://h.example/'}, ['https://h.example/x']],
    [{npm_config_registry: '//p.example/'}, ['//p.example/x', 'http://p.example/x']],
    [{npm_config_registry: 'true'}, ['https://x', 'true']],
    [{YARN_REGISTRY: 'https://r.example/'}, ['https://r.example/x']],
    [{'npm_config_@s:registry': 'https://s.example/', npm_config_registry: 'https://s.example/'},
      ['https://s.example/x']],
  ]
  rows.length = 0
  for (const [e, list] of idents) {
    for (const ident of list) {
      rows.push('Arguments.of(' + env(e) + ', ' + q(ident) + ', ' +
        q(await withEnv(e, none, false, npm => npm.getRegistry(ident))) + '),')
    }
  }
  section('ConfigTest.registries', rows)

  const npmrcs = [
    ['registry=https://a/\n@s:registry=https://a/b/\n', {}, 'https://a/b/x https://a/x @s/x x'],
    ['@s:registry=https://a/b/\nregistry=https://a/\n', {}, 'https://a/b/x https://a/x @s/x x'],
    ['registry=\n', {}, 'https://x/y x //x/y'],
    ['registry=\n@s:registry=https://s/\n', {}, 'https://s/x https://q/@s/x'],
    ['@s:registry=https://e/s/\n', {npm_config_registry: 'https://e/'}, 'https://e/s/x https://e/x'],
    ['registry=https://a/\n', {npm_config_registry: 'https://b/'}, 'https://b/x https://a/x'],
    ['registry=false\n', {}, 'https://x/y x'],
    ['registry=true\n', {}, 'https://x/y x'],
    ['@a:registry=https://h/a/\n@b:registry=https://h/\n', {}, 'https://h/a/x https://h/x'],
    ['registry=https://n/\n', {YARN_REGISTRY: 'https://y/'}, 'https://y/x x'],
    [null, {npm_config_registry: ''}, 'https://x/y x'],
    ['registry = https://sp/ \n', {}, 'https://sp/x x'],
  ]
  rows.length = 0
  for (const [npmrc, e, list] of npmrcs) {
    const project = fs.mkdtempSync(path.join(scratch, 'npmrc-'))
    if (npmrc !== null) {
      fs.writeFileSync(path.join(project, '.npmrc'), npmrc)
    }
    const answers = await withEnv(e, project, true, npm => list.split(' ').map(i => npm.getRegistry(i)).join(' '))
    rows.push('Arguments.of(' + [q(npmrc), env(e), q(list), q(answers)].join(', ') + '),')
  }
  section('ConfigTest.npmrcRegistries', rows)
}

function rcDirectoryKeys() {
  const cases = [
    {'.yarnrc': 'yarn-path ".yarn/releases/yarn-1.22.22.cjs"'},
    {'.yarnrc': 'yarn-path "./.yarn/releases/yarn-1.22.22.cjs"'},
    {'../.yarnrc': 'yarn-path "./yarn-wrapper.js"'},
    {'../../.yarnrc': 'yarn-path "tools/yarn.js"'},
    {'../.yarnrc.yml': 'yarnPath: .yarn/releases/yarn-4.10.3.cjs\nnodeLinker: node-modules'},
    {'.yarnrc.yml': 'yarnPath: ./x.cjs'},
    {'../.yarnrc.yml': 'yarn-path: y.js'},
    {'.yarnrc': 'yarn-path "/opt/yarn/yarn-1.22.22.js"'},
    {'.yarnrc': 'yarn-path "../../../x/yarn.js"'},
    {'.yarnrc': 'cache-folder "./cache/"'},
    {'.yarnrc': '--cache-folder "c"'},
    {'../.yarnrc': '--install.modules-folder "nm"'},
    {'../.yarnrc': 'yarn-path "a.js"', '.yarnrc': 'yarn-path "b.js"'},
    {'.yarnrc': 'yarn-path "a.js"', '.yarnrc.yml': 'yarnPath: b.cjs'},
    {'.yarnrc': 'yarnPath "./z.js"'},
    {'.yarnrc': 'yarn-path true'},
    {'.yarnrc': 'yarn-path 5'},
    {'../.yarnrc': 'cache-folder false'},
    {'.yarnrc': 'yarn-path ""'},
  ]
  const KEYS = /^(--)?([^.]+\.)*(yarn-path|cache-folder|global-folder|modules-folder|cwd|offline-cache-folder)$/
  section('ConfigTest.rcDirectoryKeys', cases.map(files => {
    const repo = fs.mkdtempSync(path.join(scratch, 'repo-'))
    const app = path.join(repo, 'mid', 'app')
    fs.mkdirSync(app, {recursive: true})
    for (const [file, text] of Object.entries(files)) {
      fs.writeFileSync(path.join(app, file), text + '\n')
    }
    let answer
    try {
      const config = rc.getRcConfigForCwd(app, [])
      answer = '{' + Object.keys(config).map(key => key + '=' + (!KEYS.test(key) ? config[key] :
        config[key].startsWith(scratch) ? path.relative(app, config[key]) || '.' : config[key])).join(', ') + '}'
    } catch (e) {
      answer = String(e).split('\n')[0]
    }
    return 'rcRow(' + [q(answer), ...Object.entries(files).flatMap(([f, t]) => [q(f), q(t)])].join(', ') + '),'
  }))
}

async function normalize() {
  // readJsonAndFile reads a manifest through util/map.js, which gives every object a null prototype
  const run = async manifest => {
    const info = nullify(manifest)
    const reporter = new NoopReporter()
    reporter.lang = Object.getPrototypeOf(Object.getPrototypeOf(reporter)).lang.bind(reporter)
    const dir = fs.mkdtempSync(path.join(scratch, 'normalize-'))
    const config = {reporter, cwd: dir, globalFolder: path.join(scratch, 'global'), lockfileFolder: dir,
      looseSemver: true}
    try {
      await normalizeManifest(info, dir, config, true)
      return {info}
    } catch (e) {
      return {error: e}
    }
  }
  const csv = cells => q(cells.join('|')) + ','

  const prefixes = [['empty name', '{"name": "", "version": 1}'], ['no name', '{"version": 1}'],
    ['name', '{"name": "x", "version": 1}']]
  const rows = []
  for (const [label, manifest] of prefixes) {
    rows.push(csv([label, manifest, (await run(JSON.parse(manifest))).error.message]))
  }
  section('NormalizeManifestTest.prefixesTheNameWhenThereIsOne', rows)

  rows.length = 0
  for (const [label, name] of [['empty name', ''], ['name', 'x']]) {
    const {info} = await run({name, version: '1.0.0', dependencies: {a: '1'}, devDependencies: {a: '2'}})
    rows.push(csv([label, name, JSON.stringify(info.dependencies), JSON.stringify(info.devDependencies)]))
  }
  section('NormalizeManifestTest.keepsTheFirstOfACollisionWhateverTheName', rows)

  rows.length = 0
  const PREFIX = 'TypeError [ERR_INVALID_ARG_TYPE]: The "path" argument must be of type string. Received '
  for (const [label, value] of [['number', '1'], ['fraction', '1.5'], ['boolean', 'true'], ['object', '{}'],
    ['array', '["x"]']]) {
    const {error: e} = await run({name: 'r', version: '1.0.0', dependencies: {ms: JSON.parse(value)}})
    const message = String(e).split('\n')[0]
    rows.push(csv([label, value, message.startsWith(PREFIX) ? message.slice(PREFIX.length) : '!' + message]))
  }
  section('NormalizeManifestTest.rejectsADependencyValueThatIsNotAString', rows)

  rows.length = 0
  const root = '/a/b/c/proj'
  for (const [moduleLoc, value] of [['', 'file:../x'], ['', 'file:../../x'], ['', 'file:./x/'], ['', 'file:.'],
    ['packages/a', 'file:../b'], ['packages/a', 'link:./../b'], ['packages/a', 'link:../../..']]) {
    const info = nullify({dependencies: {x: value}})
    resolveRelative(info, moduleLoc ? path.join(root, moduleLoc) : root, root)
    rows.push(csv([moduleLoc || '``', value, info.dependencies.x]))
  }
  section('NormalizeManifestTest.resolvesARelativeTarget', rows)
}

;(async () => {
  try {
    await npmResolver()
    jsYaml()
    await registries()
    rcDirectoryKeys()
    await normalize()
  } finally {
    fs.rmSync(scratch, {recursive: true, force: true})
  }
})()
