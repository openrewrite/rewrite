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
import {RecipeSpec} from "../../../../src/test";
import {ModernizeOctalEscapeSequences} from "../../../../src/javascript/migrate/es6/modernize-octal-escape-sequences";
import {javascript} from "../../../../src/javascript";

describe("modernize-octal-escape-sequences", () => {
    const spec = new RecipeSpec()
    spec.recipe = new ModernizeOctalEscapeSequences()

    test("converts one, two and three digit octal escapes, and the surrounding text is untouched", () => {
        return spec.rewriteRun(
            //language=javascript
            javascript(
                `const chars = "Hello\\0\\7\\12\\77\\123\\377World";`,
                `const chars = "Hello\\x00\\x07\\x0a\\x3f\\x53\\xffWorld";`
            )
        )
    })

    test("leaves alone every escape that is not octal", () => {
        return spec.rewriteRun(
            //language=javascript
            javascript(
                `const escapes = "Hello World\\n\\t\\r\\\\\\u0000\\u00FF\\x00\\xFF";`
            )
        )
    })

    test("a numeric literal is not a string, so its digits stay put", () => {
        return spec.rewriteRun(
            //language=javascript
            javascript(
                `const num = 123;`
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
});

describe("modernize-octal-escape-sequences with useUnicodeEscapes option", () => {
    const spec = new RecipeSpec()
    spec.recipe = new ModernizeOctalEscapeSequences({useUnicodeEscapes: true})

    test("the option chooses \\u over \\x for the same escapes", () => {
        return spec.rewriteRun(
            //language=javascript
            javascript(
                `const mixed = "\\0\\12\\123";`,
                `const mixed = "\\u0000\\u000a\\u0053";`
            )
        )
    })
});
