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
import * as fs from "fs";
import * as path from "path";

/** UI5 declares its modules ambiently: `declare module "sap/m/Button"`, with no such path on disk. */
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
    // Declaring the dependency is the whole setup here: no reference directive, no `types`.
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

    test("a fixture stating its own compiler options is not given its manifest's packages", async () => {
        const {recipe, identifierTypes} = captureTypes(["button"], []);
        const spec = new RecipeSpec();
        spec.recipe = recipe;

        await withDir(async (repo) => {
            fs.mkdirSync(repo.path, {recursive: true});
            fs.writeFileSync(path.join(repo.path, "tsconfig.json"), `{"compilerOptions": {}}`);

            await spec.rewriteRun(
                npm(
                    repo.path,
                    //language=typescript
                    typescript(`
                        import Button from "sap/m/Button";

                        const button = new Button({text: "Go"});
                    `),
                    //language=json
                    packageJson(UI5_PACKAGE_JSON)
                )
            );
        }, {unsafeCleanup: true});

        expect(identifierTypes.get("button")).toBe("<unknown>");
    }, 180000);

    // The same file reached through a directive instead, which no real UI5 source carries.
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
