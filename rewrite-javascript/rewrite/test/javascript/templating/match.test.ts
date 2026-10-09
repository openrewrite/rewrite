/*
 * Copyright 2025 the original author or authors.
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
import { fromVisitor, RecipeSpec } from "../../../src/test";
import { Cursor } from "../../../src";
import {
    capture,
    JavaScriptParser,
    JavaScriptVisitor,
    JS,
    pattern,
    Pattern,
    rewrite,
    template,
    typescript,
    npm,
    packageJson,
    sourceFileCache} from "../../../src/javascript";
import { Expression, J } from "../../../src/java";
import { castDraft, create as produce } from "mutative";
import { withDir } from "tmp-promise";

describe('match extraction', () => {
    const spec = new RecipeSpec();

    test('extract parts of a binary expression using string names', () => {
        spec.recipe = fromVisitor(new class extends JavaScriptVisitor<any> {
            override async visitBinary(binary: J.Binary, p: any): Promise<J | undefined> {
                if (binary.operator.element === J.Binary.Type.Addition) {

                    // Create a pattern that matches "a + b"
                    const m = await pattern`${"left"} + ${"right"}`.match(binary, this.cursor);
                    if (m) {
                        // Extract the captured parts
                        // Create a new binary expression with the swapped operands
                        return produce(binary, draft => {
                            draft.left = castDraft((m.get("right"))!);
                            draft.prefix = binary.left.prefix;
                            draft.right = castDraft((m.get("left"))!);
                            draft.right.prefix = binary.right.prefix;
                        });
                    }
                }
                return binary;
            }
        });

        return spec.rewriteRun(
            //language=typescript
            typescript('const result = 1 + 2;', 'const result = 2 + 1;'),
        );
    });

    test('extract parts of a binary expression using capture objects', () => {
        spec.recipe = fromVisitor(new class extends JavaScriptVisitor<any> {

            override async visitBinary(binary: J.Binary, _p: any): Promise<J | undefined> {
                // Create capture objects
                const left = capture(), right = capture();

                // Create a pattern that matches "a + b" using the capture objects
                const m = await pattern`${left} + ${right}`.match(binary, this.cursor);
                if (m) {
                    return await template`${right} + ${left}`.apply(binary, this.cursor, { values: m });
                }
                return binary;
            }
        });

        return spec.rewriteRun(
            //language=typescript
            typescript('const result = 1 + 2;', 'const result = 2 + 1;'),
        );
    });

    test('extract parts of a binary expression using replace function', () => {
        spec.recipe = fromVisitor(new class extends JavaScriptVisitor<any> {
            override async visitBinary(binary: J.Binary, p: any): Promise<J | undefined> {

                const swapOperands = rewrite(() => ({
                    before: pattern`${capture('left')} + ${capture('right')}`,
                    after: template`${capture('right')} + ${capture('left')}`
                })
                );
                return await swapOperands.tryOn(this.cursor, binary);
            }
        });

        return spec.rewriteRun(
            //language=typescript
            typescript('const result = 1 + 2;', 'const result = 2 + 1;'),
        );
    });

    test('extract parts of a binary expression using unnamed captures', () => {
        spec.recipe = fromVisitor(new class extends JavaScriptVisitor<any> {
            override async visitBinary(binary: J.Binary, p: any): Promise<J | undefined> {
                if (binary.operator.element === J.Binary.Type.Addition) {
                    // Create capture objects without explicit names
                    const { left, right } = { left: capture<Expression>(), right: capture<Expression>() };

                    // Create a pattern that matches "a + b" using the capture objects
                    const m = await pattern`${left} + ${right}`.match(binary, this.cursor);
                    if (m) {
                        // Extract the captured parts
                        const leftValue = m.get(left);
                        const rightValue = m.get(right);

                        // Create a new binary expression with the swapped operands
                        return produce(binary, draft => {
                            draft.left = castDraft(rightValue!);
                            draft.prefix = binary.left.prefix;
                            draft.right = castDraft(leftValue!);
                            draft.right.prefix = binary.right.prefix;
                        });
                    }
                }
                return binary;
            }
        });

        return spec.rewriteRun(
            //language=typescript
            typescript('const result = 1 + 2;', 'const result = 2 + 1;'),
        );
    });

    test('extract parts using inline named captures', () => {
        spec.recipe = fromVisitor(new class extends JavaScriptVisitor<any> {
            override async visitBinary(binary: J.Binary, p: any): Promise<J | undefined> {
                if (binary.operator.element === J.Binary.Type.Addition) {
                    // Use inline named captures
                    const m = await pattern`${capture('left')} + ${capture('right')}`.match(binary, this.cursor);
                    if (m) {
                        // Can retrieve by string name
                        return await template`${capture('right')} + ${capture('left')}`.apply(binary, this.cursor, { values: m });
                    }
                }
                return binary;
            }
        });

        return spec.rewriteRun(
            //language=typescript
            typescript('const result = 1 + 2;', 'const result = 2 + 1;'),
        );
    });

    test('a capture named twice matches only where both places hold the same code', async () => {
        const parser = new JavaScriptParser({sourceFileCache});
        const parse = async (code: string) => {
            const statement = ((await parser.parse({text: code, sourcePath: 'test.ts'}).next()).value as JS.CompilationUnit).statements[0].element;
            return statement.kind === JS.Kind.ExpressionStatement ? (statement as JS.ExpressionStatement).expression : statement;
        };
        const r = capture('r');
        const pat = pattern`${capture('req')}.map(${r} => ${r}.json())`;

        expect(await pat.match(await parse('req.map(res => res.json())'), undefined!)).toBeDefined();
        expect(await pat.match(await parse('req.map(res => other.json())'), undefined!)).toBeUndefined();

        const args = capture({variadic: true});
        const variadic = pattern`f(${args}) || g(${args})`;
        expect(await variadic.match(await parse('f(a, b) || g(a, b)'), undefined!)).toBeDefined();
        expect(await variadic.match(await parse('f(a) || g(b)'), undefined!)).toBeUndefined();
    });

    test('pattern with non-existent dependency fails', async () => {
        // Verify that specifying a non-existent package causes npm install to fail
        const nonExistentPackage = 'this-package-definitely-does-not-exist-12345';

        const pat = pattern`${capture('left')} + ${capture('right')}`
            .configure({
                context: [`import { SomeType } from "${nonExistentPackage}"`],
                dependencies: { [nonExistentPackage]: '^1.0.0' }
            });

        // Create dummy code to trigger pattern parsing (which requires workspace creation)
        const testCode = 'const result = 1 + 2;';
        const parser = new JavaScriptParser({sourceFileCache});
        const parseGen = parser.parse({ text: testCode, sourcePath: 'test.ts' });
        const cu = (await parseGen.next()).value;

        // Try to match - this should fail because npm install will fail for non-existent package
        await expect(async () => {
            await (new class extends JavaScriptVisitor<any> {
                override async visitBinary(binary: J.Binary, _p: any): Promise<J | undefined> {
                    // This should throw when trying to create workspace
                    await pat.match(binary, new Cursor(binary, undefined));
                    return binary;
                }
            }).visit(cu, undefined);
        }).rejects.toThrow(/Failed to create dependency workspace/);
    });

    test('a context-declared function matches a same-named local or imported one, even under strict type matching', async () => {
        const declaredFoo = pattern`foo(${capture('a')})`
            .configure({context: ['declare function foo(a: any): void;'], lenientTypeMatching: false});
        let matched = false;
        spec.recipe = fromVisitor(new class extends JavaScriptVisitor<any> {
            override async visitMethodInvocation(method: J.MethodInvocation, _p: any): Promise<J | undefined> {
                matched = !!await declaredFoo.match(method, this.cursor);
                return method;
            }
        });

        await spec.rewriteRun(
            typescript(`declare function foo(a: any): void;\nconst s = "x";\nfoo(s);`)
        );
        expect(matched).toBe(true);

        matched = false;
        const util = typescript(`export function foo(a: any): void {}`);
        util.path = 'util.ts';
        const main = typescript(`import {foo} from './util';\nfoo("x");`);
        main.path = 'main.ts';
        await withDir(async repo => spec.rewriteRun(npm(repo.path, util, main)), {unsafeCleanup: true});
        expect(matched).toBe(true);
    });

    test('a call pattern with a variadic capture matches the callee by symbol, not by name', async () => {
        const args = capture({variadic: true});
        const isDateCall = pattern`isDate(${args})`
            .configure({context: [`import {isDate} from 'node:util/types';`]});
        let calls = 0;
        let capturedArgs: J[] | undefined;
        spec.recipe = fromVisitor(new class extends JavaScriptVisitor<any> {
            override async visitMethodInvocation(method: J.MethodInvocation, _p: any): Promise<J | undefined> {
                calls++;
                const m = await isDateCall.match(method, this.cursor);
                if (m) {
                    capturedArgs = m.get(args) as J[];
                }
                return method;
            }
        });

        await spec.rewriteRun(
            typescript(`function isDate(value: unknown): boolean { return false; }\nconst result = isDate(new Date());`)
        );
        expect(calls).toBe(1);
        expect(capturedArgs).toBeUndefined();

        await spec.rewriteRun(
            typescript(`import {isDate as checkDate} from 'node:util/types';\nconst result = checkDate(new Date());`)
        );
        expect(capturedArgs).toHaveLength(1);
    });

    /** Whether each pattern matches the one call in `source`. */
    async function matched(source: string, ...patterns: Pattern[]): Promise<boolean[]> {
        let results: boolean[] | undefined;
        spec.recipe = fromVisitor(new class extends JavaScriptVisitor<any> {
            override async visitMethodInvocation(method: J.MethodInvocation, _p: any): Promise<J | undefined> {
                if (results) {
                    throw new Error(`more than one call in: ${source}`);
                }
                results = [];
                const subject = (method.name.simpleName === 'f' ? method.arguments.elements[0].element : method) as J;
                for (const p of patterns) {
                    results.push(!!await p.match(subject, this.cursor));
                }
                return method;
            }
        });
        await spec.rewriteRun(typescript(source));
        return results!;
    }

    test('a receiver the pattern writes out has to match the source receiver, even where the method types agree', async () => {
        const fixed = pattern`Object.assign({}, ${capture('a')})`;
        const variadic = pattern`Object.assign({}, ${capture({variadic: true})})`;

        expect(await matched(`declare const a: object;\nObject.assign({}, a);`, fixed, variadic)).toEqual([true, true]);
        expect(await matched(`declare const a: object;\nglobalThis.Object.assign({}, a);`, fixed, variadic)).toEqual([false, false]);
    });

    test('a written-out receiver matches an aliased import of its symbol, but not a variable holding it', async () => {
        const bufferFrom = pattern`Buffer.from(${capture('bytes')})`
            .configure({context: [`import {Buffer} from 'buffer';`]});

        expect(await matched(`import {Buffer as B} from 'buffer';\nB.from('x');`, bufferFrom)).toEqual([true]);
        expect(await matched(`const Buf = Buffer;\nBuf.from('x');`, bufferFrom)).toEqual([false]);

        const stdoutWrite = pattern`stdout.write(${capture('s')})`
            .configure({context: [`import {stdout} from 'process';`]});

        expect(await matched(`import {stdout as out} from 'process';\nout.write('x');`, stdoutWrite)).toEqual([true]);
    });

    test('two aliases of one parameterized type are not one declaration', async () => {
        const asNames = pattern`${capture('v')} as Names`
            .configure({context: [`type Names = Array<string>;`]});

        expect(await matched(`type Names = Array<string>;\nf(n as Names);`, asNames)).toEqual([true]);
        expect(await matched(`type Counts = Array<number>;\nf(c as Counts);`, asNames)).toEqual([false]);
    });

    test('an aliased default import of a callable module is the receiver the pattern writes out', async () => {
        const strictEqual = pattern`assert.strictEqual(${capture('a')}, ${capture('b')})`
            .configure({context: [`import assert from 'node:assert';`]});

        expect(await matched(`import a from 'node:assert';\na.strictEqual(x, y);`, strictEqual)).toEqual([true]);
    });

    test('a static call matches every spelling of its receiver, and only that function', async () => {
        const fromMoment = {
            context: [`import moment from 'moment';`],
            dependencies: {moment: '^2.30.0'},
            lenientTypeMatching: false
        };
        const utc = pattern`moment.utc(${capture('s')})`.configure(fromMoment);
        const unix = pattern`moment.unix(${capture('n')})`.configure(fromMoment);
        const bareUtc = pattern`utc(${capture('s')})`.configure({...fromMoment, context: [`import {utc} from 'moment';`]});

        const sources: Record<string, [string, Pattern[]]> = {
            alias: [`import m from 'moment';\nm.utc('x');`, [utc, bareUtc]],
            named: [`import {utc as u} from 'moment';\nu('x');`, [utc]],
            dayjsDefault: [`import dayjs from 'dayjs';\ndayjs.unix(1);`, [unix]],
            namespace: [`declare function tz(): void;\ndeclare namespace tz { function utc(s: string): void; }\ntz.utc('x');`, [utc, bareUtc]]
        };
        const results: Record<string, boolean[]> = {};
        spec.recipe = fromVisitor(new class extends JavaScriptVisitor<any> {
            override async visitMethodInvocation(method: J.MethodInvocation, _p: any): Promise<J | undefined> {
                const file = this.cursor.firstEnclosing((t: any): t is JS.CompilationUnit => t.kind === JS.Kind.CompilationUnit)!;
                const name = file.sourcePath.replace(/\.ts$/, '');
                results[name] = [];
                for (const p of sources[name][1]) {
                    results[name].push(!!await p.match(method, this.cursor));
                }
                return method;
            }
        });
        await withDir(async repo => {
            const files = Object.entries(sources).map(([name, [source]]) => ({...typescript(source), path: `${name}.ts`}));
            await spec.rewriteRun(npm(repo.path,
                packageJson(JSON.stringify({dependencies: {moment: '^2.30.0', dayjs: '^1.11.0'}})),
                ...files));
        }, {unsafeCleanup: true});

        expect(results).toEqual({
            alias: [true, true],
            named: [true],
            dayjsDefault: [false],
            namespace: [false, false]
        });
    });

    test('untyped imports from one module are not one declaration', async () => {
        const renderArg = pattern`foo(render)`
            .configure({context: [`import {render} from 'react-dom';`, `declare function foo(x: any): void;`]});

        expect(await matched(`import {render, hydrate} from 'react-dom';\ndeclare function foo(x: any): void;\nfoo(render);`, renderArg)).toEqual([true]);
        expect(await matched(`import {render, hydrate} from 'react-dom';\ndeclare function foo(x: any): void;\nfoo(hydrate);`, renderArg)).toEqual([false]);
    });

    test('two members of one function type are not one declaration', async () => {
        const promisified = pattern`promisify(fs.readFile)`
            .configure({context: [`import {promisify} from 'util';`, `import * as fs from 'fs';`]});

        expect(await matched(`import {promisify} from 'util';\nimport * as fs from 'fs';\npromisify(fs.readFile);`, promisified)).toEqual([true]);
        expect(await matched(`import {promisify} from 'util';\nimport * as fs from 'fs';\npromisify(fs.writeFile);`, promisified)).toEqual([false]);
    });
});
