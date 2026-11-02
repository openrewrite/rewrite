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
import * as fs from "fs";
import * as path from "path";
import v8 from "node:v8";
import vm from "node:vm";
import {withDir} from "tmp-promise";
import {RecipeSpec} from "../../src/test";
import {JavaScriptParser, JavaScriptVisitor, JS, npm, typescript} from "../../src/javascript";
import {J, Type} from "../../src/java";
import {ExecutionContext, Recipe} from "../../src";

// Postdates the `lib` this project compiles against.
declare class WeakRef<T extends object> {
    constructor(target: T);

    deref(): T | undefined;
}

/**
 * Records the full signature attributed to each captured method invocation. An unresolved call
 * still carries a `methodType`, so its presence proves nothing; the parameter and return types
 * are what can only come from a declaration the program loaded.
 */
function captureMethodTypes(names: string[], sink: Map<string, string>): Recipe {
    class CaptureRecipe extends Recipe {
        name = 'org.openrewrite.javascript.test.CaptureMethodTypes';
        displayName = 'Capture method types';
        description = 'Records the resolved signature of selected method invocations.';

        async editor(): Promise<JavaScriptVisitor<ExecutionContext>> {
            return new class extends JavaScriptVisitor<ExecutionContext> {
                async visitMethodInvocation(method: J.MethodInvocation, p: ExecutionContext): Promise<J.MethodInvocation> {
                    const visited = await super.visitMethodInvocation(method, p) as J.MethodInvocation;
                    if (names.includes(visited.name.simpleName)) {
                        sink.set(visited.name.simpleName,
                            visited.methodType ? Type.signature(visited.methodType) : '<none>');
                    }
                    return visited;
                }
            };
        }
    }

    return new CaptureRecipe();
}

function write(repo: string, relativePath: string, content: string): void {
    const file = path.join(repo, relativePath);
    fs.mkdirSync(path.dirname(file), {recursive: true});
    fs.writeFileSync(file, content);
}

/**
 * A declaration package outside `node_modules/@types/`, which TypeScript only loads when a
 * project names it in `types`. The interface-and-const shape is the one `@sapui5/types` uses
 * for `sap/ui/core/Theming`, and its named interface is what gives the call a declaring type.
 */
function writeAmbientTypesPackage(repo: string, name: string): void {
    write(repo, `node_modules/${name}/package.json`, `{"name": "${name}", "version": "1.0.0", "types": "index.d.ts"}`);
    write(repo, `node_modules/${name}/index.d.ts`,
        //language=typescript
        `declare module "sap/ui/core/Theming" {
             interface Theming {
                 setTheme(themeId: string): void;
             }

             const Theming: Theming;
             export default Theming;
         }`);
}

/**
 * A package publishing only its root through `exports`, with a deeper file sitting on disk.
 * `Node10` predates `exports` and reaches that file; the modes that honour the map refuse it.
 */
function writeWalledPackage(repo: string, name: string): void {
    write(repo, `node_modules/${name}/package.json`, JSON.stringify({
        name, version: '1.0.0',
        exports: {'.': {types: './index.d.ts', default: './index.js'}}
    }));
    write(repo, `node_modules/${name}/index.d.ts`,
        //language=typescript
        `export declare class Root {
             top(a: string): string;
         }`);
    write(repo, `node_modules/${name}/internal/thing.d.ts`,
        //language=typescript
        `export declare class Thing {
             deep(a: string): string;
         }`);
}

/**
 * A package publishing different typings per export condition, the `import` branch returning a
 * string and the `require` branch a number, so a resolution's chosen branch is visible as a type.
 */
function writeDualPackage(repo: string, name: string): void {
    write(repo, `node_modules/${name}/package.json`, JSON.stringify({
        name, version: '1.0.0',
        exports: {
            '.': {
                import: {types: './esm.d.ts', default: './esm.js'},
                require: {types: './cjs.d.cts', default: './cjs.cjs'}
            }
        }
    }));
    write(repo, `node_modules/${name}/esm.d.ts`, `export declare function pick(): string;`);
    write(repo, `node_modules/${name}/cjs.d.cts`, `export declare function pick(): number;`);
}

//language=typescript
const WALLED_DEEP_SNIPPET = `
    import {Thing} from "walled/internal/thing";

    new Thing().deep("y");
`;

//language=typescript
const THEMING_SNIPPET = `
    import Theming from "sap/ui/core/Theming";

    Theming.setTheme("sap_belize");
`;

describe('tsconfig.json compiler options', () => {
    test('`types` decides whether a declaration package outside @types/ is loaded', async () => {
        const captured = new Map<string, string>();
        const withTypesEntry = new RecipeSpec();
        withTypesEntry.recipe = captureMethodTypes(['setTheme'], captured);

        await withDir(async (repo) => {
            writeAmbientTypesPackage(repo.path, 'ui5-types');
            write(repo.path, 'tsconfig.json', `{"compilerOptions": {"types": ["ui5-types"]}}`);

            await withTypesEntry.rewriteRun(npm(repo.path, typescript(THEMING_SNIPPET)));
        }, {unsafeCleanup: true});

        expect(captured.get('setTheme')).toBe('Theming{name=setTheme,return=void,parameters=[String]}');

        const withoutTypesEntry = new RecipeSpec();
        withoutTypesEntry.recipe = captureMethodTypes(['setTheme'], captured);

        await withDir(async (repo) => {
            writeAmbientTypesPackage(repo.path, 'ui5-types');
            write(repo.path, 'tsconfig.json', `{"compilerOptions": {}}`);

            await withoutTypesEntry.rewriteRun(npm(repo.path, typescript(THEMING_SNIPPET)));
        }, {unsafeCleanup: true});

        // Unloaded, the call has no signature, though the import still names the module it calls into.
        expect(captured.get('setTheme')).toBe('sap/ui/core/Theming{name=setTheme,return=<unknown>,parameters=[]}');
    }, 120000);

    test('`baseUrl` roots a bare specifier at the directory the project names', async () => {
        const captured = new Map<string, string>();
        const spec = new RecipeSpec();
        spec.recipe = captureMethodTypes(['greet'], captured);

        await withDir(async (repo) => {
            write(repo.path, 'tsconfig.json', `{"compilerOptions": {"baseUrl": "./src"}}`);

            await spec.rewriteRun(npm(
                repo.path,
                {
                    //language=typescript
                    ...typescript(`
                        import {Greeter} from "greeter";

                        new Greeter().greet("world");
                    `),
                    path: 'main.ts'
                },
                {
                    //language=typescript
                    ...typescript(`
                        export class Greeter {
                            greet(name: string): string {
                                return name;
                            }
                        }
                    `),
                    path: 'src/greeter.ts'
                }
            ));
        }, {unsafeCleanup: true});

        expect(captured.get('greet')).toBe('src/greeter.Greeter{name=greet,return=String,parameters=[String]}');
    }, 60000);

    test('sibling projects each get their own compiler options', async () => {
        const captured = new Map<string, string>();
        const spec = new RecipeSpec();
        spec.recipe = captureMethodTypes(['setTheme', 'greet'], captured);

        await withDir(async (repo) => {
            writeAmbientTypesPackage(repo.path, 'ui5-types');
            write(repo.path, 'packages/ui/tsconfig.json', `{"compilerOptions": {"types": ["ui5-types"]}}`);
            write(repo.path, 'packages/lib/tsconfig.json', `{"compilerOptions": {"paths": {"@lib/*": ["src/*"]}}}`);

            await spec.rewriteRun(npm(
                repo.path,
                {...typescript(THEMING_SNIPPET), path: 'packages/ui/theme.ts'},
                {
                    //language=typescript
                    ...typescript(`
                        import {Greeter} from "@lib/greeter";

                        new Greeter().greet("world");
                    `),
                    path: 'packages/lib/main.ts'
                },
                {
                    //language=typescript
                    ...typescript(`
                        export class Greeter {
                            greet(name: string): string {
                                return name;
                            }
                        }
                    `),
                    path: 'packages/lib/src/greeter.ts'
                }
            ));
        }, {unsafeCleanup: true});

        expect(captured.get('setTheme')).toBe('Theming{name=setTheme,return=void,parameters=[String]}');
        expect(captured.get('greet')).toBe('packages/lib/src/greeter.Greeter{name=greet,return=String,parameters=[String]}');
    }, 60000);
    test('a stated `moduleResolution` is honoured where it resolves differently from the default', async () => {
        const captured = new Map<string, string>();
        const spec = new RecipeSpec();
        spec.recipe = captureMethodTypes(['deep'], captured);

        await withDir(async (repo) => {
            writeWalledPackage(repo.path, 'walled');
            write(repo.path, 'tsconfig.json', `{"compilerOptions": {"moduleResolution": "node10"}}`);

            await spec.rewriteRun(npm(repo.path, typescript(WALLED_DEEP_SNIPPET)));
        }, {unsafeCleanup: true});

        expect(captured.get('deep')).toBe('walled.Thing{name=deep,return=String,parameters=[String]}');
    }, 60000);

    test('a project\'s `module` kind is not adopted', async () => {
        const conditions = new Map<string, string>();
        const notAdopted = new RecipeSpec();
        notAdopted.recipe = captureMethodTypes(['pick'], conditions);

        await withDir(async (repo) => {
            writeDualPackage(repo.path, 'dual');
            write(repo.path, 'tsconfig.json', `{"compilerOptions": {"module": "node16"}}`);

            await notAdopted.rewriteRun(npm(repo.path, typescript(`
                import {pick} from "dual";

                pick();
            `)));
        }, {unsafeCleanup: true});

        expect(conditions.get('pick')).toBe('dual{name=pick,return=String,parameters=[]}');
    }, 60000);

    test('a declared resolution is the only one consulted, even where it resolves less', async () => {
        const captured = new Map<string, string>();
        const spec = new RecipeSpec();
        spec.recipe = captureMethodTypes(['top', 'deep'], captured);

        await withDir(async (repo) => {
            writeWalledPackage(repo.path, 'walled');
            write(repo.path, 'tsconfig.json', `{"compilerOptions": {"moduleResolution": "bundler"}}`);

            await spec.rewriteRun(npm(
                repo.path,
                //language=typescript
                typescript(`
                    import {Root} from "walled";
                    import {Thing} from "walled/internal/thing";

                    new Root().top("x");
                    new Thing().deep("y");
                `)
            ));
        }, {unsafeCleanup: true});

        expect(captured.get('top')).toBe('walled.Root{name=top,return=String,parameters=[String]}');

        expect(captured.get('deep')).toBe('<unknown>{name=deep,return=<unknown>,parameters=[]}');
    }, 60000);
    test('a JavaScript project states its `paths` in jsconfig.json, which a tsconfig.json outranks', async () => {
        const fromJsConfigCapture = new Map<string, string>();
        const fromJsConfig = new RecipeSpec();
        fromJsConfig.recipe = captureMethodTypes(['greet'], fromJsConfigCapture);

        await withDir(async (repo) => {
            write(repo.path, 'jsconfig.json', `{"compilerOptions": {"paths": {"@app/*": ["src/*"]}}}`);

            await fromJsConfig.rewriteRun(npm(
                repo.path,
                {
                    //language=typescript
                    ...typescript(`
                        import {Greeter} from "@app/greeter";

                        new Greeter().greet("world");
                    `),
                    path: 'main.ts'
                },
                {
                    //language=typescript
                    ...typescript(`
                        export class Greeter {
                            greet(name: string): string {
                                return name;
                            }
                        }
                    `),
                    path: 'src/greeter.ts'
                }
            ));
        }, {unsafeCleanup: true});

        expect(fromJsConfigCapture.get('greet')).toBe('src/greeter.Greeter{name=greet,return=String,parameters=[String]}');

        const bothPresentCapture = new Map<string, string>();
        const bothPresent = new RecipeSpec();
        bothPresent.recipe = captureMethodTypes(['greet'], bothPresentCapture);

        await withDir(async (repo) => {
            write(repo.path, 'jsconfig.json', `{"compilerOptions": {"paths": {"@app/*": ["nowhere/*"]}}}`);
            write(repo.path, 'tsconfig.json', `{"compilerOptions": {"paths": {"@app/*": ["src/*"]}}}`);

            await bothPresent.rewriteRun(npm(
                repo.path,
                {
                    //language=typescript
                    ...typescript(`
                        import {Greeter} from "@app/greeter";

                        new Greeter().greet("world");
                    `),
                    path: 'main.ts'
                },
                {
                    //language=typescript
                    ...typescript(`
                        export class Greeter {
                            greet(name: string): string {
                                return name;
                            }
                        }
                    `),
                    path: 'src/greeter.ts'
                }
            ));
        }, {unsafeCleanup: true});

        expect(bothPresentCapture.get('greet')).toBe('src/greeter.Greeter{name=greet,return=String,parameters=[String]}');
    }, 120000);
});

function forceGc(): void {
    // A function still being optimized on a compiler thread is held strongly by its job, and with it
    // the TypeScript checker whose closure it is, so those jobs have to finish before a collection.
    finalizeOptimization();
    if (typeof global.gc === "function") {
        global.gc();
        return;
    }
    v8.setFlagsFromString("--expose_gc");
    try {
        vm.runInNewContext("gc")();
    } finally {
        v8.setFlagsFromString("--no-expose_gc");
    }
}

function finalizeOptimization(): void {
    v8.setFlagsFromString("--allow-natives-syntax");
    try {
        vm.runInNewContext("%FinalizeOptimization()");
    } finally {
        v8.setFlagsFromString("--no-allow-natives-syntax");
    }
}

type TextInput = { text: string, sourcePath: string };

/**
 * Parses the inputs with a parser of its own and notes, as each source file comes out, how many
 * programs that parser has built and how many of those are still reachable. A program is built
 * over a compiler host of its own and refers to it for as long as it lives, so a host that has been
 * collected stands for a program that has been.
 */
function programsDuringParsesOf(repo: string): (...inputs: TextInput[]) => Promise<string[]> {
    const parser = new JavaScriptParser({relativeTo: repo});
    const hosts: WeakRef<object>[] = [];
    const internals = parser as unknown as { createCompilerHost(...args: unknown[]): object };
    const createCompilerHost = internals.createCompilerHost;
    internals.createCompilerHost = function (this: unknown, ...args: unknown[]) {
        const host = createCompilerHost.apply(this, args);
        hosts.push(new WeakRef(host));
        return host;
    };

    return async (...inputs) => {
        const observed: string[] = [];
        for await (const sourceFile of parser.parse(...inputs)) {
            expect(sourceFile.kind).toBe(JS.Kind.CompilationUnit);
            // What a turn of the event loop has referenced stays reachable until that turn ends.
            await new Promise(resolve => setImmediate(resolve));
            forceGc();
            const alive = hosts.filter(host => host.deref() !== undefined).length;
            observed.push(`${sourceFile.sourcePath}: ${hosts.length} built, ${alive} alive`);
        }
        return observed;
    };
}

/** A project of two inputs, the second importing the first, under a tsconfig.json of its own. */
function writeProject(repo: string, name: string): TextInput[] {
    write(repo, `packages/${name}/tsconfig.json`, `{"compilerOptions": {}}`);
    return [
        {sourcePath: `packages/${name}/one.ts`, text: `export const one = 1;\n`},
        {sourcePath: `packages/${name}/two.ts`, text: `import {one} from "./one";\n\nexport const two = one + 1;\n`}
    ];
}

describe('the programs of a parse', () => {
    test('one spanning several projects builds each as its inputs come up and lets it go with its last', async () => {
        await withDir(async (repo) => {
            const [a1, a2] = writeProject(repo.path, 'a');
            const [b1, b2] = writeProject(repo.path, 'b');
            const [c1, c2] = writeProject(repo.path, 'c');
            const parse = programsDuringParsesOf(repo.path);

            expect(await parse(a1, a2, b1, b2, c1, c2)).toEqual([
                'packages/a/one.ts: 1 built, 1 alive',
                'packages/a/two.ts: 1 built, 0 alive',
                'packages/b/one.ts: 2 built, 1 alive',
                'packages/b/two.ts: 2 built, 0 alive',
                'packages/c/one.ts: 3 built, 1 alive',
                'packages/c/two.ts: 3 built, 0 alive',
            ]);
        }, {unsafeCleanup: true});
    }, 120000);

    test('one whose projects take turns keeps each program until that project has had its last input', async () => {
        await withDir(async (repo) => {
            const [a1, a2] = writeProject(repo.path, 'a');
            const [b1, b2] = writeProject(repo.path, 'b');
            const parse = programsDuringParsesOf(repo.path);

            expect(await parse(a1, b1, a2, b2)).toEqual([
                'packages/a/one.ts: 1 built, 1 alive',
                'packages/b/one.ts: 2 built, 2 alive',
                'packages/a/two.ts: 2 built, 1 alive',
                'packages/b/two.ts: 2 built, 0 alive',
            ]);
        }, {unsafeCleanup: true});
    }, 120000);

    test('one of a single project leaves its program with the parser for the next parse', async () => {
        await withDir(async (repo) => {
            const inputs = writeProject(repo.path, 'a');
            const parse = programsDuringParsesOf(repo.path);

            expect(await parse(...inputs)).toEqual([
                'packages/a/one.ts: 1 built, 1 alive',
                'packages/a/two.ts: 1 built, 1 alive',
            ]);
            // the program the next parse builds takes the place of the one carried into it
            expect(await parse(...inputs)).toEqual([
                'packages/a/one.ts: 2 built, 1 alive',
                'packages/a/two.ts: 2 built, 1 alive',
            ]);
        }, {unsafeCleanup: true});
    }, 120000);
});
