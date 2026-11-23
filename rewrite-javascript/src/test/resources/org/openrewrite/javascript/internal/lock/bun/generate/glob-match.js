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
// Writes glob-match.tsv: bun's answer to every new Glob(pattern).match(path) of test/js/bun/glob/match.test.ts, with
// the test's home directories spelled under /srv and C:/Profiles (`pattern path answer` rows); prints the test's tally.
//
// Usage: node glob-match.js <bun checkout>/test/js/bun/glob/match.test.ts > glob-match.tsv
const fs = require('fs')
const os = require('os')
const path = require('path')
const {spawnSync} = require('child_process')

const bun = spawnSync('sh', ['-c', 'command -v bun'], {encoding: 'utf8'}).stdout.trim()
const tmp = fs.mkdtempSync(path.join(os.tmpdir(), 'glob-match-'))
const rows = path.join(tmp, 'rows.tsv')
fs.writeFileSync(path.join(tmp, 'match.test.ts'), fs.readFileSync(process.argv[2], 'utf8')
  .replace(/^import \{ Glob \} from "bun";$/m, 'import { Glob } from "./glob-recorder.ts";')
  .replace(/^import \{ isWindows \} from "harness";$/m, 'const isWindows = false;') + `
import { afterAll as recorderAfterAll } from "bun:test";
import { flush as recorderFlush } from "./glob-recorder.ts";
recorderAfterAll(() => recorderFlush());
`)
fs.writeFileSync(path.join(tmp, 'glob-recorder.ts'), `
const rows: string[] = [];
const ok = (s: unknown) => typeof s === "string" && !/[\\t\\r\\n]/.test(s);
const away = (s: any) => typeof s === "string" ?
  s.replace(/\\/home\\/foo\\b/g, "/srv/foo").replace(/C:\\/Users\\/window\\b/g, "C:/Profiles/window") : s;
export class Glob {
  inner: any; pattern: string;
  constructor(pattern: string) { this.pattern = away(pattern); this.inner = new Bun.Glob(this.pattern); }
  match(path: string) {
    const p = away(path);
    const r = this.inner.match(p);
    if (ok(this.pattern) && ok(p)) rows.push([this.pattern, p, String(r)].join("\\t"));
    return r;
  }
  scan(...a: any[]) { return this.inner.scan(...a); }
  scanSync(...a: any[]) { return this.inner.scanSync(...a); }
}
export function flush() { require("fs").writeFileSync(${JSON.stringify(rows)}, rows.join("\\n") + "\\n"); }
`)
const test = spawnSync(bun, ['test', 'match.test.ts'], {cwd: tmp, encoding: 'utf8'})
const tally = (test.stdout + test.stderr).split('\n').filter(l => /^ *\d+ (pass|fail)/.test(l)).join(', ')
console.error(`bun test exit ${test.status} (${tally})`)
process.stdout.write(fs.readFileSync(rows, 'utf8'))
fs.rmSync(tmp, {recursive: true, force: true})
