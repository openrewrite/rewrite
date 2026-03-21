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

import {RecipeSpec} from "../../../src/test";
import {javascript, typescript} from "../../../src/javascript";
import {AddParseIntRadix} from "../../../src/javascript/cleanup";

describe("AddParseIntRadix", () => {
    const spec = new RecipeSpec();
    spec.recipe = new AddParseIntRadix();

    test("adds radix to parseInt with variable", () => spec.rewriteRun(
        //language=javascript
        javascript(
            `const num = parseInt(str);`,
            `const num = parseInt(str, 10);`
        )
    ));

    test("adds radix to parseInt with string literal", () => spec.rewriteRun(
        //language=javascript
        javascript(
            `const num = parseInt("42");`,
            `const num = parseInt("42", 10);`
        )
    ));

    test("adds radix to parseInt in expression", () => spec.rewriteRun(
        //language=javascript
        javascript(
            `const result = parseInt(value) + 1;`,
            `const result = parseInt(value, 10) + 1;`
        )
    ));

    test("does not modify parseInt that already has radix", () => spec.rewriteRun(
        //language=javascript
        javascript(
            `const num = parseInt(str, 10);`
        )
    ));

    test("does not modify parseInt with different radix", () => spec.rewriteRun(
        //language=javascript
        javascript(
            `const num = parseInt(hex, 16);`
        )
    ));

    test("does not modify Number.parseInt", () => spec.rewriteRun(
        //language=javascript
        javascript(
            `const num = Number.parseInt(str);`
        )
    ));

    test("works with TypeScript", () => spec.rewriteRun(
        //language=typescript
        typescript(
            `const num: number = parseInt(str);`,
            `const num: number = parseInt(str, 10);`
        )
    ));

    test("adds radix to parseInt in function call", () => spec.rewriteRun(
        //language=javascript
        javascript(
            `doSomething(parseInt(x));`,
            `doSomething(parseInt(x, 10));`
        )
    ));

    test("adds radix to parseInt with complex expression", () => spec.rewriteRun(
        //language=javascript
        javascript(
            `const num = parseInt(obj.value);`,
            `const num = parseInt(obj.value, 10);`
        )
    ));
});
