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
import {RecipeSpec} from "../../src/test";
import {JavaScriptVisitor, MethodMatcher, npm, packageJson, typescript} from "../../src/javascript";
import {J, Type} from "../../src/java";
import {ExecutionContext, Recipe} from "../../src";
import {withDir} from "tmp-promise";

// Verbatim from type-mapping-ambient-modules.test.ts, so both suites reuse one cached install.
const UI5_PACKAGE_JSON = `{
  "name": "ui5-ambient-fixture",
  "version": "1.0.0",
  "devDependencies": { "@openui5/types": "1.136.0" }
}`;

/** Records which of `patterns` match a named call, stating what a recipe could select. */
function matchesOf(methodName: string, patterns: string[], sink: Map<string, boolean>): Recipe {
    class MatchesRecipe extends Recipe {
        name = 'org.openrewrite.javascript.test.UI5Matches';
        displayName = 'Record matching method patterns';
        description = 'Records which method patterns match a named call.';

        async editor(): Promise<JavaScriptVisitor<ExecutionContext>> {
            return new class extends JavaScriptVisitor<ExecutionContext> {
                override async visitMethodInvocation(method: J.MethodInvocation, p: ExecutionContext): Promise<J.MethodInvocation> {
                    if (method.name.simpleName === methodName) {
                        for (const pattern of patterns) {
                            sink.set(pattern, new MethodMatcher(pattern).matches(method.methodType));
                        }
                    }
                    return method;
                }
            };
        }
    }

    return new MatchesRecipe();
}

describe('SAP UI5 method patterns', () => {
    // A UI5 FQN is module path plus exported name, so its type segment carries slashes, not dots.
    test('a matcher selects a call on an imported UI5 control', async () => {
        const matches = new Map<string, boolean>();
        const spec = new RecipeSpec();
        spec.recipe = matchesOf('attachPress', [
            'sap/m/Button.Button attachPress(..)',
            'sap/m/Button.* attach*(..)',
            '*..* attachPress(..)',
            'sap/m/* attachPress(..)',
            'sap/ui/core/Control.Control attachPress(..)',
        ], matches);

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

        expect(matches.get('sap/m/Button.Button attachPress(..)')).toBe(true);
        expect(matches.get('sap/m/Button.* attach*(..)')).toBe(true);
        expect(matches.get('*..* attachPress(..)')).toBe(true);

        // A module path is not a package, so it does not glob.
        expect(matches.get('sap/m/* attachPress(..)')).toBe(false);

        // Matching follows the receiver's own type, not the type declaring the member.
        expect(matches.get('sap/ui/core/Control.Control attachPress(..)')).toBe(false);
    }, 180000);
});
