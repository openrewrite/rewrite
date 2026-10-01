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
import {javascript, tsx} from "../../../../src/javascript";

describe("modernize-octal-escape-sequences", () => {
    const spec = new RecipeSpec()
    spec.recipe = new ModernizeOctalEscapeSequences()

    test("converts one, two and three digit octal escapes, and the surrounding text is untouched", () => {
        return spec.rewriteRun(
            //language=javascript
            javascript(
                `const chars = "Hello\\1\\7\\12\\77\\123\\377World";`,
                `const chars = "Hello\\x01\\x07\\x0a\\x3f\\x53\\xffWorld";`
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

    test("leaves template literals alone, where \\0 is valid and other octal escapes are an error", () => {
        return spec.rewriteRun(
            //language=javascript
            javascript(
                "const template = `test\\0end`;"
            )
        )
    })

    test("leaves regex backreferences alone", () => {
        return spec.rewriteRun(
            //language=javascript
            javascript(
                `const re = /(a)\\1/;`
            )
        )
    })

    test("an escaped backslash followed by digits is not an octal escape", () => {
        return spec.rewriteRun(
            //language=javascript
            javascript(
                `const s = "\\\\123";`
            )
        )
    })

    test("an escape starting with 4-7 takes at most two digits", () => {
        return spec.rewriteRun(
            //language=javascript
            javascript(
                `const s = "\\400\\777";`,
                `const s = "\\x200\\x3f7";`
            )
        )
    })

    test("leaves the NUL escape alone when no digit follows", () => {
        return spec.rewriteRun(
            //language=javascript
            javascript(
                `const s = "a\\0b";`
            )
        )
    })

    test("converts \\0 followed by 8 or 9, which is still a legacy octal escape", () => {
        return spec.rewriteRun(
            //language=javascript
            javascript(
                `const s = "\\08\\09";`,
                `const s = "\\x008\\x009";`
            )
        )
    })

    test("leaves JSX attribute strings alone, as JSX does not process escapes", () => {
        return spec.rewriteRun(
            //language=tsx
            tsx(
                `const a = <a title="\\101"/>;`
            )
        )
    })

    test("converts single-quoted strings", () => {
        return spec.rewriteRun(
            //language=javascript
            javascript(
                `const s = 'a\\101';`,
                `const s = 'a\\x41';`
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
                `const mixed = "\\01\\12\\123";`,
                `const mixed = "\\u0001\\u000a\\u0053";`
            )
        )
    })
});
