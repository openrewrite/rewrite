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
import {fromVisitor, RecipeSpec, SourceSpec} from "../../../src/test";
import {capture, JavaScriptVisitor, npm, packageJson, Pattern, pattern, typescript} from "../../../src/javascript";
import {J} from "../../../src/java";
import {withDir} from "tmp-promise";

describe('a pattern naming a variable', () => {
    const spec = new RecipeSpec();

    /** The names of the classes in `source` that `pat` matches, and of the variables whose initializer it matches. */
    async function declarationsMatching(pat: Pattern, source: string, ...others: SourceSpec<any>[]): Promise<string[]> {
        const matched: string[] = [];
        spec.recipe = fromVisitor(new class extends JavaScriptVisitor<any> {
            override async visitVariable(variable: J.VariableDeclarations.NamedVariable, p: any): Promise<J | undefined> {
                const initializer = variable.initializer?.element;
                if (initializer && await pat.match(initializer, this.cursor)) {
                    matched.push((variable.name as J.Identifier).simpleName);
                }
                return super.visitVariable(variable, p);
            }

            override async visitClassDeclaration(classDecl: J.ClassDeclaration, p: any): Promise<J | undefined> {
                if (await pat.match(classDecl, this.cursor)) {
                    matched.push(classDecl.name.simpleName);
                }
                return super.visitClassDeclaration(classDecl, p);
            }
        });
        await withDir(async (repo) => {
            await spec.rewriteRun(
                npm(
                    repo.path,
                    typescript(source),
                    ...others,
                    //language=json
                    packageJson(
                        `
                        {
                          "name": "test",
                          "devDependencies": {
                            "@types/node": "^20.0.0"
                          }
                        }
                        `
                    )
                )
            );
        }, {unsafeCleanup: true});
        return matched;
    }

    //language=typescript
    const references = `
        import {EOL} from 'os';
        import * as os from 'os';

        const settings = {EOL: '\\n'};
        export const imported = EOL;
        export const qualified = os.EOL;
        export const siblingExport = os.devNull;
        export const sameNamedProperty = settings.EOL;
    `;

    test('matches the constant read through a namespace import', async () => {
        const eol = pattern`EOL`.configure({
            context: [`import {EOL} from 'os'`],
            dependencies: {'@types/node': '^20.0.0'}
        });

        expect(await declarationsMatching(eol, references)).toEqual(['imported', 'qualified']);
    });

    test('written through a namespace import matches the named import', async () => {
        const eol = pattern`os.EOL`.configure({
            context: [`import * as os from 'os'`],
            dependencies: {'@types/node': '^20.0.0'}
        });

        expect(await declarationsMatching(eol, references)).toEqual(['imported', 'qualified']);
    });

    test('does not match a same-named export of another module under strict type matching', async () => {
        const eol = pattern`EOL`.configure({
            context: [`import {EOL} from 'os'`],
            dependencies: {'@types/node': '^20.0.0'},
            lenientTypeMatching: false
        });

        expect(await declarationsMatching(
            eol,
            //language=typescript
            `
            import {EOL} from './line-endings';

            export const fromOtherModule = EOL;
            `,
            //language=typescript
            {...typescript(`export const EOL = '\\r\\n';`), path: 'line-endings.ts'}
        )).toEqual([]);
    });

    test('does not match a local variable of the same name under strict type matching', async () => {
        const eol = pattern`EOL`.configure({
            context: [`import {EOL} from 'os'`],
            dependencies: {'@types/node': '^20.0.0'},
            lenientTypeMatching: false
        });

        expect(await declarationsMatching(
            eol,
            //language=typescript
            `
            import {EOL as NL} from 'os';

            export const aliased = NL;

            export function local() {
                const EOL = 'x';
                const shadowing = EOL;
                return shadowing;
            }
            `
        )).toEqual(['aliased']);
    });

    test('leaves the class declaring a member out of strict type matching', async () => {
        const read = pattern`error.message`.configure({
            context: [`declare const error: Error`],
            lenientTypeMatching: false
        });
        const declaration = pattern`class ${capture('name')} { count = 0 }`.configure({lenientTypeMatching: false});

        expect(await declarationsMatching(
            read,
            //language=typescript
            `
            class ParseError extends Error {
                message = 'unparseable';
            }

            declare const error: ParseError;
            export const redeclared = error.message;
            `
        )).toEqual(['redeclared']);

        expect(await declarationsMatching(
            declaration,
            //language=typescript
            `
            class Counter { count = 0 }
            `
        )).toEqual(['Counter']);
    });
});
