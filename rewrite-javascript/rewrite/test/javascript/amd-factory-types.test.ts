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
import {RecipeSpec, SourceSpec} from "../../src/test";
import {JavaScriptParser, JavaScriptVisitor, npm, typescript} from "../../src/javascript";
import {J, Type} from "../../src/java";
import {ExecutionContext, Recipe} from "../../src";

/**
 * Records the type of selected identifiers and the declaring type of selected calls. A factory
 * parameter typed from its dependency shows up on the variable it initialises and on the calls
 * made through that variable, which is where a recipe reads it.
 */
function captureTypes(identifiers: string[], methods: string[]): {
    recipe: Recipe,
    identifierTypes: Map<string, string>,
    declaringTypes: Map<string, string>
} {
    const identifierTypes = new Map<string, string>();
    const declaringTypes = new Map<string, string>();

    class CaptureRecipe extends Recipe {
        name = "org.openrewrite.javascript.test.CaptureAmdTypes";
        displayName = "Capture AMD factory types";
        description = "Records the resolved type of selected identifiers and method receivers.";

        async editor(): Promise<JavaScriptVisitor<ExecutionContext>> {
            return new class extends JavaScriptVisitor<ExecutionContext> {
                async visitIdentifier(ident: J.Identifier, p: ExecutionContext): Promise<J.Identifier> {
                    const visited = await super.visitIdentifier(ident, p) as J.Identifier;
                    if (identifiers.includes(visited.simpleName) && !identifierTypes.has(visited.simpleName)) {
                        const type = visited.type;
                        identifierTypes.set(visited.simpleName,
                            Type.isClass(type) ? type.fullyQualifiedName : Type.signature(type));
                    }
                    return visited;
                }

                protected async visitMethodInvocation(
                    method: J.MethodInvocation,
                    p: ExecutionContext
                ): Promise<J | undefined> {
                    const visited = await super.visitMethodInvocation(method, p) as J.MethodInvocation;
                    const name = visited.name.simpleName;
                    if (methods.includes(name) && !declaringTypes.has(name)) {
                        const declaring = visited.methodType?.declaringType;
                        declaringTypes.set(name,
                            Type.isClass(declaring) ? declaring.fullyQualifiedName : Type.signature(declaring));
                    }
                    return visited;
                }
            };
        }
    }

    return {recipe: new CaptureRecipe(), identifierTypes, declaringTypes};
}

function write(repo: string, relativePath: string, content: string): void {
    const file = path.join(repo, relativePath);
    fs.mkdirSync(path.dirname(file), {recursive: true});
    fs.writeFileSync(file, content);
}

/** The module an AMD block depends on, reached by a relative specifier as a project's own modules are. */
function greeterModule(): SourceSpec<any> {
    return {
        //language=typescript
        ...typescript(`
            export default class Greeter {
                greet(name: string): string {
                    return name;
                }
            }
        `),
        path: "greeter.ts"
    };
}

/**
 * A loader declaring its own `define`, published outside `@types/` so it is only in scope where
 * a project names it. Its `any[]` factory is what leaves every parameter untyped on its own.
 */
function writeLoaderTypes(repo: string): void {
    write(repo, "node_modules/loader-types/package.json",
        `{"name": "loader-types", "version": "1.0.0", "types": "index.d.ts"}`);
    write(repo, "node_modules/loader-types/index.d.ts",
        //language=typescript
        `declare namespace my.loader {
             function define(dependencies: string[], factory: (...args: any[]) => any): void;
         }`);
}

describe("AMD factory parameters", () => {
    // The loader binds the nth dependency to the nth parameter at runtime, so the parser declares it.
    test("a factory parameter carries the type of the dependency bound to it", async () => {
        const {recipe, identifierTypes, declaringTypes} = captureTypes(["greeter"], ["greet"]);
        const spec = new RecipeSpec();
        spec.recipe = recipe;

        await withDir(async (repo) => {
            await spec.rewriteRun(
                npm(
                    repo.path,
                    //language=typescript
                    typescript(`
                        define(["./greeter"], function (Greeter) {
                            const greeter = new Greeter();
                            greeter.greet("world");
                        });
                    `),
                    greeterModule()
                )
            );
        }, {unsafeCleanup: true});

        expect(identifierTypes.get("greeter")).toBe("greeter.Greeter");
        expect(declaringTypes.get("greet")).toBe("greeter.Greeter");
    }, 120000);

    // A loader named in `types` brings its own `define` along, which the generated overload has to outrank.
    test("a loader's own declarations reached through `types` do not shadow the binding", async () => {
        const {recipe, identifierTypes, declaringTypes} = captureTypes(["greeter"], ["define", "greet"]);
        const spec = new RecipeSpec();
        spec.recipe = recipe;

        await withDir(async (repo) => {
            writeLoaderTypes(repo.path);
            write(repo.path, "tsconfig.json", `{"compilerOptions": {"types": ["loader-types"]}}`);

            await spec.rewriteRun(
                npm(
                    repo.path,
                    //language=typescript
                    typescript(`
                        my.loader.define(["./greeter"], function (Greeter) {
                            const greeter = new Greeter();
                            greeter.greet("world");
                        });
                    `),
                    greeterModule()
                )
            );
        }, {unsafeCleanup: true});

        expect(declaringTypes.get("define")).toBe("my.loader");
        expect(identifierTypes.get("greeter")).toBe("greeter.Greeter");
        expect(declaringTypes.get("greet")).toBe("greeter.Greeter");
    }, 120000);

    test("a dependency list that is not literal declares nothing", async () => {
        const {recipe, identifierTypes} = captureTypes(["greeter"], []);
        const spec = new RecipeSpec();
        spec.recipe = recipe;

        await withDir(async (repo) => {
            await spec.rewriteRun(
                npm(
                    repo.path,
                    //language=typescript
                    typescript(`
                        const dependencies = ["./greeter"];
                        define(dependencies, function (Greeter) {
                            const greeter = new Greeter();
                        });
                    `),
                    greeterModule()
                )
            );
        }, {unsafeCleanup: true});

        expect(identifierTypes.get("greeter")).toBe("<unknown>");
    }, 120000);

    test("the binding is off when the parser is told so", async () => {
        const {recipe, identifierTypes} = captureTypes(["greeter"], []);
        const spec = new RecipeSpec();
        spec.recipe = recipe;

        await withDir(async (repo) => {
            await spec.rewriteRun(
                {
                    //language=typescript
                    ...typescript(`
                        define(["./greeter"], function (Greeter) {
                            const greeter = new Greeter();
                        });
                    `),
                    parser: () => new JavaScriptParser({relativeTo: repo.path, amdFactoryTypes: false})
                },
                greeterModule()
            );
        }, {unsafeCleanup: true});

        expect(identifierTypes.get("greeter")).toBe("<unknown>");
    }, 120000);
});
