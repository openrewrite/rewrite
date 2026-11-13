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
import * as fs from "fs";
import * as path from "path";
import {withDir} from "tmp-promise";
import {RecipeSpec} from "../../../src/test";
import {javascript, JS, npm, typescript} from "../../../src/javascript";

describe('import defer mapping', () => {
    const spec = new RecipeSpec();

    test('namespace import', () =>
        spec.rewriteRun({
            //language=typescript
            ...typescript('import defer * as ns from "foo"'),
            afterRecipe: (cu: JS.CompilationUnit) => {
                const clause = (cu.statements[0].element as JS.Import).importClause!;
                expect(clause.deferred).toBe(true);
                expect(clause.typeOnly).toBe(false);
            }
        }));

    test('comments', () =>
        spec.rewriteRun(
            //language=typescript
            typescript('import /*1*/ defer /*2*/ * /*3*/ as /*4*/ ns /*5*/ from "foo";')
        ));

    test('not deferred', () =>
        spec.rewriteRun({
            //language=typescript
            ...typescript('import * as defer from "foo"'),
            afterRecipe: (cu: JS.CompilationUnit) => {
                expect((cu.statements[0].element as JS.Import).importClause!.deferred).toBe(false);
            }
        }));

    test('dynamic', () =>
        spec.rewriteRun(
            //language=typescript
            typescript('const ns = await import.defer("foo")')
        ));

    test('dynamic with options', () =>
        spec.rewriteRun(
            //language=javascript
            javascript('const ns = import.defer("foo", {with: {type: "json"}})')
        ));

    test('module setting that rejects deferred imports', () =>
        withDir(async repo => {
            fs.writeFileSync(path.join(repo.path, 'tsconfig.json'), `{"compilerOptions": {"module": "commonjs"}}`);
            await spec.rewriteRun(
                npm(
                    repo.path,
                    //language=typescript
                    typescript(
                        `
                        import defer * as ns from "foo";
                        const other = import.defer("bar");
                        `
                    )
                )
            );
        }, {unsafeCleanup: true}));
});
