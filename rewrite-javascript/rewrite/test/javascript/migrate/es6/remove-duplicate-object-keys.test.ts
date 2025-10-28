// noinspection JSUnusedLocalSymbols,JSDuplicatedDeclaration

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
import {RemoveDuplicateObjectKeys} from "../../../../src/javascript/migrate/es6/remove-duplicate-object-keys";
import {typescript} from "../../../../src/javascript";

describe("remove-duplicate-object-keys", () => {
    const spec = new RecipeSpec()
    spec.recipe = new RemoveDuplicateObjectKeys();

    test("remove duplicate key (last wins)", () => {
        return spec.rewriteRun(
            //language=typescript
            typescript(
                `const c = {foo: 1, foo: 2};`,
                `const c = {foo: 2};`
            )
        )
    })

    test("remove multiple duplicates of same key", () => {
        return spec.rewriteRun(
            //language=typescript
            typescript(
                `const c = {foo: 1, foo: 2, foo: 3};`,
                `const c = {foo: 3};`
            )
        )
    })

    test("remove duplicates of different keys", () => {
        return spec.rewriteRun(
            //language=typescript
            typescript(
                `const c = {foo: 1, bar: 2, foo: 3, bar: 4};`,
                `const c = {foo: 3, bar: 4};`
            )
        )
    })

    test("keep unique keys", () => {
        return spec.rewriteRun(
            //language=typescript
            typescript(
                `const c = {foo: 1, bar: 2};`
            )
        )
    })

    test("remove duplicate string keys", () => {
        return spec.rewriteRun(
            //language=typescript
            typescript(
                `const c = {"foo": 1, "foo": 2};`,
                `const c = {"foo": 2};`
            )
        )
    })

    test("remove duplicates mixing identifier and string keys", () => {
        return spec.rewriteRun(
            //language=typescript
            typescript(
                `const c = {foo: 1, "foo": 2};`,
                `const c = {"foo": 2};`
            )
        )
    })

    test("preserve non-duplicate properties", () => {
        return spec.rewriteRun(
            //language=typescript
            typescript(
                `const c = {a: 1, b: 2, a: 3, c: 4};`,
                `const c = {b: 2, a: 3, c: 4};`
            )
        )
    })

    test("do not remove computed properties", () => {
        return spec.rewriteRun(
            //language=typescript
            typescript(
                `const c = {[key]: 1, [key]: 2};`
            )
        )
    })

    test("handle empty object", () => {
        return spec.rewriteRun(
            //language=typescript
            typescript(
                `const c = {};`
            )
        )
    })

    test("remove duplicates in nested objects", () => {
        return spec.rewriteRun(
            //language=typescript
            typescript(
                `const c = {outer: {foo: 1, foo: 2}};`,
                `const c = {outer: {foo: 2}};`
            )
        )
    })

    test("preserve comments on last occurrence", () => {
        return spec.rewriteRun(
            //language=typescript
            typescript(
                `const c = {foo: 1, /* comment */ foo: 2};`,
                `const c = {/* comment */ foo: 2};`
            )
        )
    })

    test("remove inline comment on same line as removed duplicate", () => {
        return spec.rewriteRun(
            //language=typescript
            typescript(
                `
                    const c = {
                        foo: 1, // inline comment
                        bar: 2, foo: 3
                    };
                `,
                `
                    const c = {
                        bar: 2, foo: 3
                    };
                `
            )
        )
    })

    test("preserve comment on subsequent line after removed duplicate", () => {
        return spec.rewriteRun(
            //language=typescript
            typescript(
                `
                    const c = {
                        foo: 1,
                        // comment about bar
                        bar: 2,
                        foo: 3
                    };
                `,
                `
                    const c = {
                        // comment about bar
                        bar: 2,
                        foo: 3
                    };
                `
            )
        )
    })

    test("remove inline comment but preserve block comment on next line", () => {
        return spec.rewriteRun(
            //language=typescript
            typescript(
                `
                    const c = {
                        foo: 1, // inline
                        /* block comment */
                        bar: 2,
                        foo: 3
                    };
                `,
                `
                    const c = {
                        /* block comment */
                        bar: 2,
                        foo: 3
                    };
                `
            )
        )
    })

    test("remove trailing line comment from removed first element", () => {
        return spec.rewriteRun(
            //language=typescript
            typescript(
                `
                    const c = {
                        foo: 1, // trailing comment
                        bar: 2, foo: 3
                    };
                `,
                `
                    const c = {
                        bar: 2, foo: 3
                    };
                `
            )
        )
    })

    test("remove leading comment when removing first element", () => {
        return spec.rewriteRun(
            //language=typescript
            typescript(
                `
                    const c = {
                        // comment about foo
                        foo: 1,
                        bar: 2, foo: 3
                    };
                `,
                `
                    const c = {
                        bar: 2, foo: 3
                    };
                `
            )
        )
    })
});
