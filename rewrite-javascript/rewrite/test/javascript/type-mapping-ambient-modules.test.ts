// noinspection JSUnusedLocalSymbols

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
import {RecipeSpec} from "../../src/test";
import {JavaScriptVisitor, npm, packageJson, typescript} from "../../src/javascript";
import {J, Type} from "../../src/java";
import {ExecutionContext, Recipe} from "../../src";
import {withDir} from "tmp-promise";

/**
 * Two ways a dependency can declare the types a source file uses:
 *
 *  - by **path**, where the import specifier resolves to a directory under `node_modules`.
 *    `express`, `luxon` and anything under `node_modules/@types` work this way, and the parser
 *    attributes them with no configuration at all.
 *  - **ambiently**, where the package declares module names that are not paths —
 *    `declare module "sap/m/Button"` with no `node_modules/sap/m/Button` on disk.
 *
 * SAP UI5 is the second kind, because UI5 loader module names are not filesystem paths in the
 * consuming project. `@openui5/types` is Apache-2.0 and installs cleanly; its declarations then
 * sit on disk unreferenced, because the parser's default `types: ["*"]` enumerates
 * `node_modules/@types` and nothing else, and `npm()` offers no way to name another package.
 *
 * UI5 is a large migration surface, so this is worth closing.
 */
function captureTypes(identifiers: string[], methods: string[]): {
    recipe: Recipe,
    identifierTypes: Map<string, string>,
    declaringTypes: Map<string, string>
} {
    const identifierTypes = new Map<string, string>();
    const declaringTypes = new Map<string, string>();

    class CaptureRecipe extends Recipe {
        name = "org.openrewrite.javascript.test.CaptureAmbientTypes";
        displayName = "Capture ambient module types";
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
                        const declaring = (visited.methodType as Type.Method | undefined)?.declaringType;
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

const UI5_PACKAGE_JSON = `{
  "name": "ui5-ambient-fixture",
  "version": "1.0.0",
  "devDependencies": { "@openui5/types": "1.136.0" }
}`;

describe("ambient module declarations from an external dependency", () => {
    // FAILS. `@openui5/types` is installed and declares `declare module "sap/m/Button"`, but
    // nothing brings those declarations into scope: `types` defaults to `["*"]`, which globs
    // `node_modules/@types` only, and `npm()` takes no `types` argument to widen it.
    //
    // Actual today: every capture is `<unknown>`.
    test("a declared ambient package is in scope for the files that import it", async () => {
        const {recipe, identifierTypes, declaringTypes} = captureTypes(["button"], ["attachPress"]);
        const spec = new RecipeSpec();
        spec.recipe = recipe;

        await withDir(async (repo) => {
            await spec.rewriteRun(
                npm(
                    repo.path,
                    //language=typescript
                    typescript(`
                        import Button from "sap/m/Button";

                        const button = new Button({text: "Go"});
                        button.attachPress(() => {});
                    `),
                    //language=json
                    packageJson(UI5_PACKAGE_JSON)
                )
            );
        }, {unsafeCleanup: true});

        expect(identifierTypes.get("button")).toBe("sap/m/Button.Button");
        expect(declaringTypes.get("attachPress")).toBe("sap/m/Button.Button");
    }, 180000);

    // PASSES — the control. The declarations resolve perfectly once a reference directive drags
    // them in, which is what shows the test above is about *scope* rather than about the parser
    // being unable to read UI5's types. The directive is scaffolding no real UI5 source carries.
    test("the same file resolves when a reference directive pulls the declarations in", async () => {
        const {recipe, identifierTypes, declaringTypes} = captureTypes(["button"], ["attachPress"]);
        const spec = new RecipeSpec();
        spec.recipe = recipe;

        await withDir(async (repo) => {
            await spec.rewriteRun(
                npm(
                    repo.path,
                    //language=typescript
                    typescript(`/// <reference types="@openui5/types" />
                        import Button from "sap/m/Button";

                        const button = new Button({text: "Go"});
                        button.attachPress(() => {});
                    `),
                    //language=json
                    packageJson(UI5_PACKAGE_JSON)
                )
            );
        }, {unsafeCleanup: true});

        expect(identifierTypes.get("button")).toBe("sap/m/Button.Button");
        expect(declaringTypes.get("attachPress")).toBe("sap/m/Button.Button");
    }, 180000);
});

describe("AMD module bindings", () => {
    // FAILS, and not for want of declarations — the reference directive above is present, so
    // `sap.ui.define` itself attributes. What does not attribute is the factory parameter, because
    // `sap.ui.define(["sap/m/Button"], function (Button) {…})` binds the nth dependency string to
    // the nth parameter positionally, at loader runtime. There is no declaration for a checker to
    // follow, so no `types` configuration reaches this — the UI5 linter transpiles the block into
    // a real ES module before type-checking it, which is the shape a fix here would take.
    //
    // Legacy AMD is where the UI5 migration work is, so every type-aware recipe is blocked on it.
    //
    // Actual today: `define` resolves to `sap.ui`, everything reached through the parameter is
    // `<unknown>`.
    test("a factory parameter carries the type of the dependency bound to it", async () => {
        const {recipe, identifierTypes, declaringTypes} = captureTypes(["button"], ["define", "attachPress"]);
        const spec = new RecipeSpec();
        spec.recipe = recipe;

        await withDir(async (repo) => {
            await spec.rewriteRun(
                npm(
                    repo.path,
                    //language=typescript
                    typescript(`/// <reference types="@openui5/types" />
                        sap.ui.define(["sap/m/Button"], function (Button) {
                            const button = new Button({text: "Go"});
                            button.attachPress(() => {});
                        });
                    `),
                    //language=json
                    packageJson(UI5_PACKAGE_JSON)
                )
            );
        }, {unsafeCleanup: true});

        // The namespace is declared, so this half already works — it is the contrast that makes
        // the diagnosis precise rather than an assertion at risk.
        expect(declaringTypes.get("define")).toBe("sap.ui");

        expect(identifierTypes.get("button")).toBe("sap/m/Button.Button");
        expect(declaringTypes.get("attachPress")).toBe("sap/m/Button.Button");
    }, 180000);
});
