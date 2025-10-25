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
import {ModernizeOctalEscapeSequences} from "../../../../src/javascript/migrate/es6/modernize-octal-escape-sequences";
import {javascript} from "../../../../src/javascript";

describe("modernize-octal-escape-sequences", () => {
    const spec = new RecipeSpec()
    spec.recipe = new ModernizeOctalEscapeSequences();

    test("convert null character", () => {
        return spec.rewriteRun(
            //language=javascript
            javascript(
                `const nullChar = "\\0";`,
                `const nullChar = "\\x00";`
            )
        )
    })

    test("convert single digit octal escapes", () => {
        return spec.rewriteRun(
            //language=javascript
            javascript(
                `const chars = "\\1\\2\\3\\7";`,
                `const chars = "\\x01\\x02\\x03\\x07";`
            )
        )
    })

    test("convert two digit octal escapes", () => {
        return spec.rewriteRun(
            //language=javascript
            javascript(
                `const chars = "\\12\\77";`,
                `const chars = "\\x0a\\x3f";`
            )
        )
    })

    test("convert three digit octal escapes", () => {
        return spec.rewriteRun(
            //language=javascript
            javascript(
                `const chars = "\\123\\377";`,
                `const chars = "\\x53\\xff";`
            )
        )
    })

    test("convert mixed octal escapes", () => {
        return spec.rewriteRun(
            //language=javascript
            javascript(
                `const mixed = "\\0\\12\\123";`,
                `const mixed = "\\x00\\x0a\\x53";`
            )
        )
    })

    test("convert octal escapes with regular text", () => {
        return spec.rewriteRun(
            //language=javascript
            javascript(
                `const message = "Hello\\0World";`,
                `const message = "Hello\\x00World";`
            )
        )
    })

    test("convert multiple strings with octal escapes", () => {
        return spec.rewriteRun(
            //language=javascript
            javascript(
                `
                const a = "\\0";
                const b = "\\123";
                const c = "test\\7end";
                `,
                `
                const a = "\\x00";
                const b = "\\x53";
                const c = "test\\x07end";
                `
            )
        )
    })

    test("do not convert strings without octal escapes", () => {
        return spec.rewriteRun(
            //language=javascript
            javascript(
                `const normal = "Hello World";`
            )
        )
    })

    test("do not convert other escape sequences", () => {
        return spec.rewriteRun(
            //language=javascript
            javascript(
                `const escapes = "\\n\\t\\r\\\\";`
            )
        )
    })

    test("do not convert unicode escapes", () => {
        return spec.rewriteRun(
            //language=javascript
            javascript(
                `const unicode = "\\u0000\\u00FF";`
            )
        )
    })

    test("do not convert hex escapes", () => {
        return spec.rewriteRun(
            //language=javascript
            javascript(
                `const hex = "\\x00\\xFF";`
            )
        )
    })

    test("convert octal in template literal", () => {
        return spec.rewriteRun(
            //language=javascript
            javascript(
                "const template = `test\\0end`;",
                "const template = `test\\x00end`;"
            )
        )
    })

    test("convert octal in object property", () => {
        return spec.rewriteRun(
            //language=javascript
            javascript(
                `const obj = { separator: "\\0" };`,
                `const obj = { separator: "\\x00" };`
            )
        )
    })

    test("convert octal in array", () => {
        return spec.rewriteRun(
            //language=javascript
            javascript(
                `const delimiters = ["\\0", "\\1", "\\2"];`,
                `const delimiters = ["\\x00", "\\x01", "\\x02"];`
            )
        )
    })

    test("convert octal in function call", () => {
        return spec.rewriteRun(
            //language=javascript
            javascript(
                `split("\\0");`,
                `split("\\x00");`
            )
        )
    })

    test("convert bell character", () => {
        return spec.rewriteRun(
            //language=javascript
            javascript(
                `const bell = "\\7";`,
                `const bell = "\\x07";`
            )
        )
    })

    test("convert backspace character", () => {
        return spec.rewriteRun(
            //language=javascript
            javascript(
                `const backspace = "\\10";`,
                `const backspace = "\\x08";`
            )
        )
    })

    test("do not convert numeric literals", () => {
        return spec.rewriteRun(
            //language=javascript
            javascript(
                `const num = 123;`
            )
        )
    })
});

describe("modernize-octal-escape-sequences with useUnicodeEscapes option", () => {
    const spec = new RecipeSpec()
    spec.recipe = new ModernizeOctalEscapeSequences({useUnicodeEscapes: true});

    test("convert null character to Unicode", () => {
        return spec.rewriteRun(
            //language=javascript
            javascript(
                `const nullChar = "\\0";`,
                `const nullChar = "\\u0000";`
            )
        )
    })

    test("convert single digit octal escapes to Unicode", () => {
        return spec.rewriteRun(
            //language=javascript
            javascript(
                `const chars = "\\1\\2\\3\\7";`,
                `const chars = "\\u0001\\u0002\\u0003\\u0007";`
            )
        )
    })

    test("convert two digit octal escapes to Unicode", () => {
        return spec.rewriteRun(
            //language=javascript
            javascript(
                `const chars = "\\12\\77";`,
                `const chars = "\\u000a\\u003f";`
            )
        )
    })

    test("convert three digit octal escapes to Unicode", () => {
        return spec.rewriteRun(
            //language=javascript
            javascript(
                `const chars = "\\123\\377";`,
                `const chars = "\\u0053\\u00ff";`
            )
        )
    })

    test("convert mixed octal escapes to Unicode", () => {
        return spec.rewriteRun(
            //language=javascript
            javascript(
                `const mixed = "\\0\\12\\123";`,
                `const mixed = "\\u0000\\u000a\\u0053";`
            )
        )
    })

    test("convert octal escapes with regular text to Unicode", () => {
        return spec.rewriteRun(
            //language=javascript
            javascript(
                `const message = "Hello\\0World";`,
                `const message = "Hello\\u0000World";`
            )
        )
    })
});
