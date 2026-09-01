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
import {withDir} from "tmp-promise";
import {RecipeSpec} from "../../src/test";
import {JavaScriptVisitor, npm, typescript} from "../../src/javascript";
import {J, Type} from "../../src/java";
import {ExecutionContext, Recipe} from "../../src";

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
 * A package with no `exports` map, whose subpath is a directory. Reaching its declarations means
 * the directory-index lookup that `Bundler` performs and the `node16` family does not.
 */
function writeDirectorySubpathPackage(repo: string, name: string): void {
    write(repo, `node_modules/${name}/package.json`, JSON.stringify({name, version: '1.0.0'}));
    write(repo, `node_modules/${name}/sub/index.d.ts`,
        //language=typescript
        `export declare function go(a: string): string;`);
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

        expect(captured.get('setTheme')).toBe('<unknown>{name=setTheme,return=<unknown>,parameters=[]}');
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

    test('a project\'s `module` kind sets its resolution without being adopted', async () => {
        const resolution = new Map<string, string>();
        const implied = new RecipeSpec();
        implied.recipe = captureMethodTypes(['go'], resolution);

        await withDir(async (repo) => {
            writeDirectorySubpathPackage(repo.path, 'plain');
            write(repo.path, 'tsconfig.json', `{"compilerOptions": {"module": "node16"}}`);

            await implied.rewriteRun(npm(repo.path, typescript(`
                import {go} from "plain/sub";

                go("x");
            `)));
        }, {unsafeCleanup: true});

        expect(resolution.get('go')).toBe('plain/sub{name=unknown,return=<unknown>,parameters=[]}');

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
    }, 120000);

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
