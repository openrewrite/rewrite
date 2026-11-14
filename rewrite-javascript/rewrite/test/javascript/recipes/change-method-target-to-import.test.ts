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
import {RecipeSpec, SourceSpec} from "../../../src/test";
import {ChangeMethodTargetToImport, javascript, JS, MethodMatcher, npm, packageJson, typescript} from "../../../src/javascript";
import {J, Type} from "../../../src/java";
import {activate, RecipeMarketplace} from "../../../src";
import {withDir} from "tmp-promise";

describe("change-method-target-to-import", () => {
    const jestToVi = () => new ChangeMethodTargetToImport({
        methodPattern: "jest *(..)",
        targetModule: "vitest",
        targetMember: "vi"
    });

    const withTypes = async (spec: RecipeSpec, devDependencies: string, ...sources: SourceSpec<any>[]) =>
        withDir(async repo => spec.rewriteRun(npm(repo.path, ...sources, packageJson(
            `{"name": "test", "version": "1.0.0", "devDependencies": {${devDependencies}}}`
        ))), {unsafeCleanup: true});

    const jestTypes = `"@types/jest": "^29.5.13"`;

    test("moves a global's calls onto an imported member, keeping their arguments and typing them as a parse would", async () => {
        const spec = new RecipeSpec();
        spec.recipe = new ChangeMethodTargetToImport({
            methodPattern: "jest *(..)",
            targetModule: "vitest",
            targetMember: "vi",
            targetType: "vitest.VitestUtils"
        });
        await withTypes(spec, jestTypes, {
            ...typescript(
                `
                    const mock = jest.fn( (a: number) =>
                        a + 1 );
                    jest.mock('./dep', () => ({
                        x: 1
                    }));
                `,
                `
                    import {vi} from 'vitest';

                    const mock = vi.fn( (a: number) =>
                        a + 1 );
                    vi.mock('./dep', () => ({
                        x: 1
                    }));
                `
            ),
            afterRecipe: (cu: JS.CompilationUnit) => {
                const call = (cu.statements[1].element as J.VariableDeclarations)
                    .variables[0].element.initializer!.element as J.MethodInvocation;
                expect(new MethodMatcher("vitest.VitestUtils fn(..)").matches(call.methodType)).toBe(true);

                const target = call.select!.element as J.Identifier;
                expect(Type.FullyQualified.getFullyQualifiedName(target.type as Type.FullyQualified)).toBe("vitest.VitestUtils");
                expect(target.fieldType?.name).toBe("vi");
                expect(Type.FullyQualified.getFullyQualifiedName(target.fieldType!.owner as Type.FullyQualified)).toBe("vitest");
            }
        });
    });

    test("names the new target as the binding settles on it", async () => {
        const spec = new RecipeSpec();
        spec.recipe = jestToVi();
        await withTypes(spec, jestTypes, typescript(
            `
                const vi = 1;
                jest.fn();
            `,
            `
                import {vi as vi_1} from 'vitest';

                const vi = 1;
                vi_1.fn();
            `
        ));
    });

    test("moves calls through a namespace, default, named or aliased import, removing the old import", async () => {
        const spec = new RecipeSpec();
        spec.recipe = new ChangeMethodTargetToImport({
            methodPattern: "util format(..)",
            targetModule: "string-format",
            targetMember: "formatter"
        });
        await withTypes(spec, `"@types/node": "^20"`, {
            ...typescript(
                `
                    import * as util from 'util';
                    import utilDefault from 'util';
                    import {format} from 'util';
                    import {format as fmt} from 'util';

                    util.format('a');
                    utilDefault.format('b');
                    format('c');
                    fmt('d');
                `,
                `
                    import {formatter} from 'string-format';

                    formatter.format('a');
                    formatter.format('b');
                    formatter.format('c');
                    formatter.format('d');
                `
            ),
            afterRecipe: (cu: JS.CompilationUnit) => {
                const call = cu.statements[1].element as J.MethodInvocation;
                expect(call.methodType?.declaringType.kind, "untyped without a target type").toBe(Type.Kind.Unknown);
            }
        });
    });

    test("moves calls through import X = require, removing the old import", async () => {
        const spec = new RecipeSpec();
        spec.recipe = new ChangeMethodTargetToImport({
            methodPattern: "util format(..)",
            targetModule: "string-format",
            targetMember: "formatter"
        });
        // Before: unchanged, since `moduleScopeBindings` gave `util` no module, so it read as a local of the file
        await withTypes(spec, `"@types/node": "^20"`, typescript(
            `
                import util = require('util');

                util.format('a');
            `,
            `
                import {formatter} from 'string-format';

                formatter.format('a');
            `
        ));
    });

    test("leaves calls whose receiver is not the old target's name", async () => {
        const spec = new RecipeSpec();
        spec.recipe = jestToVi();
        await withTypes(spec, jestTypes, typescript(
            `
                const outer = jest;
                function setup() {
                    const jest = outer;
                    jest.fn();
                }
                const getJest = () => jest;
                getJest().fn();
            `
        ));
    });

    test("binds the target as the file does, unless told the import style", async () => {
        const jestFnToVi = (importStyle?: "ES6Named" | "CommonJS") => new ChangeMethodTargetToImport({
            methodPattern: "jest fn(..)",
            targetModule: "vitest",
            targetMember: "vi",
            importStyle
        });
        const commonJs = `
            const CC = require('./');
            test('instantiates', () => { const onChange = jest.fn(); });
        `;
        const spec = new RecipeSpec();
        spec.recipe = jestFnToVi();
        await withTypes(spec, jestTypes, javascript(commonJs,
            `
                const CC = require('./');
                const {vi} = require('vitest');
                test('instantiates', () => { const onChange = vi.fn(); });
            `
        ));

        spec.recipe = jestFnToVi("ES6Named");
        await withTypes(spec, jestTypes, javascript(commonJs,
            `
                import {vi} from 'vitest';

                const CC = require('./');
                test('instantiates', () => { const onChange = vi.fn(); });
            `
        ));

        spec.recipe = jestFnToVi("CommonJS");
        await withTypes(spec, jestTypes, javascript(
            `
                jest.fn();
            `,
            `
                const {vi} = require('vitest');

                vi.fn();
            `
        ));
    });

    test("rejects an import style it does not know, taking null as unset", () => {
        const withStyle = (importStyle: any) => () => new ChangeMethodTargetToImport({
            methodPattern: "jest fn(..)",
            targetModule: "vitest",
            targetMember: "vi",
            importStyle
        });
        // A declarative recipe's typo, which the type system does not see.
        expect(withStyle("ES6named")).toThrow("Unknown import style `ES6named`, expected one of ES6Named, CommonJS.");

        expect(withStyle(null)).not.toThrow();
    });

    test("is installed in the marketplace", async () => {
        const marketplace = new RecipeMarketplace();
        await activate(marketplace);
        expect(marketplace.findRecipe("org.openrewrite.javascript.change-method-target-to-import")?.[1])
            .toBe(ChangeMethodTargetToImport);
    });
});
