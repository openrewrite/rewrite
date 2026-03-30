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
import {PreferOptionalChain} from "../../../src/javascript/cleanup";

describe("PreferOptionalChain", () => {
    const spec = new RecipeSpec();
    spec.recipe = new PreferOptionalChain();

    // Basic field access
    test("converts foo ? foo.bar : undefined to foo?.bar", () => spec.rewriteRun(
        //language=javascript
        javascript(
            `const result = foo ? foo.bar : undefined;`,
            `const result = foo?.bar;`
        )
    ));

    // Note: We do NOT convert when false part is null, because:
    // - `foo ? foo.bar : null` returns null when foo is nullish
    // - `foo?.bar` returns undefined when foo is nullish
    // This is a semantic difference that could break code expecting null.
    test("does not convert when false part is null", () => spec.rewriteRun(
        //language=javascript
        javascript(
            `const result = foo ? foo.bar : null;`
        )
    ));

    // Method invocation
    test("converts obj ? obj.method() : undefined to obj?.method()", () => spec.rewriteRun(
        //language=javascript
        javascript(
            `const result = obj ? obj.method() : undefined;`,
            `const result = obj?.method();`
        )
    ));

    // Array access
    test("converts arr ? arr[0] : undefined to arr?.[0]", () => spec.rewriteRun(
        //language=javascript
        javascript(
            `const result = arr ? arr[0] : undefined;`,
            `const result = arr?.[0];`
        )
    ));

    // Should NOT convert when false part is something else
    test("does not convert when false part is a value", () => spec.rewriteRun(
        //language=javascript
        javascript(
            `const result = foo ? foo.bar : "default";`
        )
    ));

    test("does not convert when false part is a number", () => spec.rewriteRun(
        //language=javascript
        javascript(
            `const result = foo ? foo.bar : 0;`
        )
    ));

    // Should NOT convert when condition and access target don't match
    test("does not convert when condition differs from access target", () => spec.rewriteRun(
        //language=javascript
        javascript(
            `const result = foo ? bar.baz : undefined;`
        )
    ));

    // Should NOT convert when condition is not a simple identifier
    test("does not convert when condition is a complex expression", () => spec.rewriteRun(
        //language=javascript
        javascript(
            `const result = foo.bar ? foo.bar.baz : undefined;`
        )
    ));

    // Already optional chain
    test("preserves already optional chain", () => spec.rewriteRun(
        //language=javascript
        javascript(
            `const result = foo?.bar;`
        )
    ));

    // TypeScript
    test("works with TypeScript", () => spec.rewriteRun(
        //language=typescript
        typescript(
            `const result: string | undefined = obj ? obj.name : undefined;`,
            `const result: string | undefined = obj?.name;`
        )
    ));

    // Nested in expression
    test("converts ternary in function call argument", () => spec.rewriteRun(
        //language=javascript
        javascript(
            `doSomething(foo ? foo.bar : undefined);`,
            `doSomething(foo?.bar);`
        )
    ));

    // Method with arguments
    test("converts method call with arguments", () => spec.rewriteRun(
        //language=javascript
        javascript(
            `const result = obj ? obj.process(1, 2) : undefined;`,
            `const result = obj?.process(1, 2);`
        )
    ));

    // Preserves whitespace
    test("preserves surrounding whitespace", () => spec.rewriteRun(
        //language=javascript
        javascript(
            `const result =  foo ? foo.bar : undefined ;`,
            `const result =  foo?.bar ;`
        )
    ));
});
