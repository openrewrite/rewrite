// noinspection TypeScriptUnresolvedReference,JSUnusedLocalSymbols

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
import {describe} from "@jest/globals";
import {RecipeSpec} from "../../../../src/test";
import {ModernizeOctalLiterals} from "../../../../src/javascript/migrate/es6/modernize-octal-literals";
import {javascript} from "../../../../src/javascript";

describe("modernize-octal-literals", () => {
    const spec = new RecipeSpec()
    spec.recipe = new ModernizeOctalLiterals();

    test("convert octal literal", () => {
        return spec.rewriteRun(
            //language=javascript
            javascript(
                `const permissions = 0777;`,
                `const permissions = 0o777;`
            )
        )
    })

    test("convert multiple octal literals", () => {
        return spec.rewriteRun(
            //language=javascript
            javascript(
                `
                const permissions = 0777;
                const readable = 0444;
                const writable = 0222;
                `,
                `
                const permissions = 0o777;
                const readable = 0o444;
                const writable = 0o222;
                `
            )
        )
    })

    test("convert various octal literals", () => {
        return spec.rewriteRun(
            //language=javascript
            javascript(
                `
                const a = 0755;
                const b = 0644;
                const c = 07;
                const d = 077;
                `,
                `
                const a = 0o755;
                const b = 0o644;
                const c = 0o7;
                const d = 0o77;
                `
            )
        )
    })

    test("do not convert zero", () => {
        return spec.rewriteRun(
            //language=javascript
            javascript(
                `const zero = 0;`
            )
        )
    })

    test("do not convert hexadecimal literals", () => {
        return spec.rewriteRun(
            //language=javascript
            javascript(
                `const hex = 0xFF;`
            )
        )
    })

    test("do not convert binary literals", () => {
        return spec.rewriteRun(
            //language=javascript
            javascript(
                `const binary = 0b1010;`
            )
        )
    })

    test("do not convert modern octal literals", () => {
        return spec.rewriteRun(
            //language=javascript
            javascript(
                `const modernOctal = 0o777;`
            )
        )
    })

    // Note: Numbers like 089 and 098 are not valid in strict mode JavaScript/TypeScript
    // so they would cause a parse error. The parser correctly rejects them.

    test("convert octal in object literal", () => {
        return spec.rewriteRun(
            //language=javascript
            javascript(
                `const config = { mode: 0755 };`,
                `const config = { mode: 0o755 };`
            )
        )
    })

    test("convert octal in array", () => {
        return spec.rewriteRun(
            //language=javascript
            javascript(
                `const modes = [0644, 0755, 0777];`,
                `const modes = [0o644, 0o755, 0o777];`
            )
        )
    })

    test("convert octal in function call", () => {
        return spec.rewriteRun(
            //language=javascript
            javascript(
                `chmod(file, 0755);`,
                `chmod(file, 0o755);`
            )
        )
    })
});
