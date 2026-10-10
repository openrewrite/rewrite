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
// ini 1.3.5 and 6.0.0 for upstream-calls.js: decode, encode, safe and unsafe.
const fs = require('fs')
const path = require('path')

module.exports = {
  library: 'ini',
  entry: dir => JSON.parse(fs.readFileSync(path.join(dir, 'package.json'), 'utf8')).main,
  functions: ['decode', 'parse', 'encode', 'stringify', 'safe', 'unsafe'],
  aliases: {parse: 'decode', stringify: 'encode'},
  tests: dir => fs.readdirSync(path.join(dir, 'test')).filter(f => f.endsWith('.js')).sort().map(f => 'test/' + f),
  // 1.3.5's own `[__proto__]` and `__proto__` key write Object.prototype, so they are not asked here
  differences: [
    ini => ini.decode('[a] \nb=c'),
    ini => ini.decode('  \na=b'),
    ini => ini.decode('a=\na[]=x\nb=false\nb[]=y'),
    ini => ini.decode('x.y=null'),
    ini => ini.decode('a=null\na.b.c=null'),
    ini => ini.decode('a[]=1\n[a.b]\nc=d'),
    ini => ini.decode('a=1\n[a]\nb=c\n[d]\ne[]=f\n[d.g]\nh=i'),
    ini => ini.decode("'null'=x"),
    ini => ini.decode("'12'=x\n'[1,2]'=y\n[\"3\"]\nz=1")
  ]
}
