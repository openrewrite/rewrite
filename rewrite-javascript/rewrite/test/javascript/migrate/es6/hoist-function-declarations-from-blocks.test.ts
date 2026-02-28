// noinspection TypeScriptUnresolvedReference,TypeScriptValidateTypes,JSUnusedLocalSymbols,PointlessBooleanExpressionJS

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
import {HoistFunctionDeclarationsFromBlocks} from "../../../../src/javascript/migrate/es6";
import {typescript} from "../../../../src/javascript";

describe("hoist-function-declarations-from-blocks", () => {
    const spec = new RecipeSpec()
    spec.recipe = new HoistFunctionDeclarationsFromBlocks();

    test("functionUsedOutsideBlock", () => {
        return spec.rewriteRun(
            //language=typescript
            typescript(
                `
                if (true) {
                    function helper() {
                        return 42;
                    }
                }
                const result = helper();
                `,
                `
                let helper;
                if (true) {
                    helper = function () {
                        return 42;
                    };
                }
                const result = helper();
                `
            )
        );
    });

    test("functionUsedOnlyInsideBlock", () => {
        return spec.rewriteRun(
            //language=typescript
            typescript(
                `
                if (true) {
                    function helper() {
                        return 42;
                    }
                    const result = helper();
                }
                `
            )
        );
    });

    test("functionInWhileLoop", () => {
        return spec.rewriteRun(
            //language=typescript
            typescript(
                `
                while (condition) {
                    function process() {
                        return data;
                    }
                }
                process();
                `,
                `
                let process;
                while (condition) {
                    process = function () {
                        return data;
                    };
                }
                process();
                `
            )
        );
    });

    test("multipleFunctionsNeedingHoisting", () => {
        return spec.rewriteRun(
            //language=typescript
            typescript(
                `
                if (condition1) {
                    function helper1() {
                        return 1;
                    }
                }
                if (condition2) {
                    function helper2() {
                        return 2;
                    }
                }
                helper1();
                helper2();
                `,
                `
                let helper1;
                let helper2;
                if (condition1) {
                    helper1 = function () {
                        return 1;
                    };
                }
                if (condition2) {
                    helper2 = function () {
                        return 2;
                    };
                }
                helper1();
                helper2();
                `
            )
        );
    });

    test("topLevelFunctionNotAffected", () => {
        return spec.rewriteRun(
            //language=typescript
            typescript(
                `
                function topLevel() {
                    return 42;
                }
                topLevel();
                `
            )
        );
    });

    test("nestedBlocksUsedOutsideBoth", () => {
        return spec.rewriteRun(
            //language=typescript
            typescript(
                `
                if (condition1) {
                    if (condition2) {
                        function helper() {
                            return 42;
                        }
                    }
                }
                const result = helper();
                `,
                `
                let helper;
                if (condition1) {
                    if (condition2) {
                        helper = function () {
                            return 42;
                        };
                    }
                }
                const result = helper();
                `
            )
        );
    });

    test("nestedBlocksUsedInOuterBlock", () => {
        return spec.rewriteRun(
            //language=typescript
            typescript(
                `
                if (condition1) {
                    if (condition2) {
                        function helper() {
                            return 42;
                        }
                    }
                    const result = helper();
                }
                `,
                `
                let helper;
                if (condition1) {
                    if (condition2) {
                        helper = function () {
                            return 42;
                        };
                    }
                    const result = helper();
                }
                `
            )
        );
    });

    test("functionInElseBlock", () => {
        return spec.rewriteRun(
            //language=typescript
            typescript(
                `
                if (condition) {
                    console.log('then');
                } else {
                    function helper() {
                        return 42;
                    }
                }
                const result = helper();
                `,
                `
                let helper;
                if (condition) {
                    console.log('then');
                } else {
                    helper = function () {
                        return 42;
                    };
                }
                const result = helper();
                `
            )
        );
    });

    test("functionWithoutBlockUsedOutside", () => {
        return spec.rewriteRun(
            //language=typescript
            typescript(
                `
                if (true)
                    function helper() {
                        return 42;
                    }
                const result = helper();
                `,
                `
                let helper;
                if (true)
                    helper = function () {
                        return 42;
                    };
                const result = helper();
                `
            )
        );
    });

    test("hoistingInsideFunction", () => {
        return spec.rewriteRun(
            //language=typescript
            typescript(
                `
                function outer() {
                    if (condition) {
                        function helper(n) {
                            return 42 + n;
                        }
                    }
                    return helper(1);
                }
                `,
                `
                function outer() {
                    let helper;
                    if (condition) {
                        helper = function (n) {
                            return 42 + n;
                        };
                    }
                    return helper(1);
                }
                `
            )
        );
    });

    test("hoistingInsideNestedFunctions", () => {
        return spec.rewriteRun(
            //language=typescript
            typescript(
                `
                function outer() {
                    function inner() {
                        if (condition) {
                            function helper() {
                                return 42;
                            }
                        }
                        return helper();
                    }
                    return inner();
                }
                `,
                `
                function outer() {
                    function inner() {
                        let helper;
                        if (condition) {
                            helper = function () {
                                return 42;
                            };
                        }
                        return helper();
                    }
                    return inner();
                }
                `
            )
        );
    });

    test("sameFunctionNameInDifferentScopes", () => {
        return spec.rewriteRun(
            //language=typescript
            typescript(
                `
                if (condition1) {
                    function helper() {
                        return 1;
                    }
                }
                if (condition2) {
                    function helper() {
                        return 2;
                    }
                }
                helper();
                `,
                `
                let helper;
                if (condition1) {
                    helper = function () {
                        return 1;
                    };
                }
                if (condition2) {
                    helper = function () {
                        return 2;
                    };
                }
                helper();
                `
            )
        );
    });

    test("functionUsedInSiblingBlock", () => {
        return spec.rewriteRun(
            //language=typescript
            typescript(
                `
                function outer() {
                    if (condition1) {
                        function helper() {
                            return 1;
                        }
                    }
                    if (condition2) {
                        helper();
                    }
                }
                `,
                `
                function outer() {
                    let helper;
                    if (condition1) {
                        helper = function () {
                            return 1;
                        };
                    }
                    if (condition2) {
                        helper();
                    }
                }
                `
            )
        );
    });

    test("functionWithParameters", () => {
        return spec.rewriteRun(
            //language=typescript
            typescript(
                `
                if (true) {
                    function helper(a, b) {
                        return a + b;
                    }
                }
                const result = helper(1, 2);
                `,
                `
                let helper;
                if (true) {
                    helper = function (a, b) {
                        return a + b;
                    };
                }
                const result = helper(1, 2);
                `
            )
        );
    });

    test("methodInvocationVsFunctionCall", () => {
        return spec.rewriteRun(
            //language=typescript
            typescript(
                `
                const obj = {
                    helper: function() {
                        return 42;
                    }
                };
                if (true) {
                    function helper() {
                        return 99;
                    }
                }
                obj.helper();
                `
            )
        );
    });

    test("switchStatementCases", () => {
        return spec.rewriteRun(
            //language=typescript
            typescript(
                `
                switch (value) {
                    case 1: {
                        function helper() {
                            return 42;
                        }
                        break;
                    }
                }
                helper();
                `
            )
        );
    });

    test("tryCatchFinallyBlocks", () => {
        return spec.rewriteRun(
            //language=typescript
            typescript(
                `
                try {
                    function helper() {
                        return 42;
                    }
                } catch (e) {
                    console.error(e);
                }
                helper();
                `
            )
        );
    });

    test("functionCallViaVariableReference", () => {
        return spec.rewriteRun(
            //language=typescript
            typescript(
                `
                const fn = function() { return 99;};
                if (true) {
                    function helper() {
                        return 42;
                    }
                }
                const a = fn();
                const b = helper();
                `,
                `
                let helper;
                const fn = function() { return 99;};
                if (true) {
                    helper = function () {
                        return 42;
                    };
                }
                const a = fn();
                const b = helper();
                `
            )
        );
    });

    test("functionAssignedThenCalledViaOptionalChaining", () => {
        // The function is assigned to `fn`, which is a reference outside the block,
        // so the function needs to be hoisted
        return spec.rewriteRun(
            //language=typescript
            typescript(
                `
                if (true) {
                    function helper() {
                        return 42;
                    }
                }
                const fn = helper;
                fn?.();
                `,
                `
                let helper;
                if (true) {
                    helper = function () {
                        return 42;
                    };
                }
                const fn = helper;
                fn?.();
                `
            )
        );
    });

    test("functionWithReturnType", () => {
        return spec.rewriteRun(
            //language=typescript
            typescript(
                `
                if (true) {
                    function helper(): number {
                        return 42;
                    }
                }
                const result = helper();
                `,
                `
                let helper;
                if (true) {
                    helper = function (): number {
                        return 42;
                    };
                }
                const result = helper();
                `
            )
        );
    });

    // Tests for control flow types not previously covered

    test("functionInForLoop", () => {
        return spec.rewriteRun(
            //language=typescript
            typescript(
                `
                for (let i = 0; i < 10; i++) {
                    function helper() {
                        return i;
                    }
                }
                const result = helper();
                `,
                `
                let helper;
                for (let i = 0; i < 10; i++) {
                    helper = function () {
                        return i;
                    };
                }
                const result = helper();
                `
            )
        );
    });

    test("functionInForInLoop", () => {
        return spec.rewriteRun(
            //language=typescript
            typescript(
                `
                const obj = {a: 1, b: 2};
                for (const key in obj) {
                    function helper() {
                        return key;
                    }
                }
                const result = helper();
                `,
                `
                let helper;
                const obj = {a: 1, b: 2};
                for (const key in obj) {
                    helper = function () {
                        return key;
                    };
                }
                const result = helper();
                `
            )
        );
    });

    test("functionInForOfLoop", () => {
        return spec.rewriteRun(
            //language=typescript
            typescript(
                `
                const arr = [1, 2, 3];
                for (const item of arr) {
                    function helper() {
                        return item;
                    }
                }
                const result = helper();
                `,
                `
                let helper;
                const arr = [1, 2, 3];
                for (const item of arr) {
                    helper = function () {
                        return item;
                    };
                }
                const result = helper();
                `
            )
        );
    });

    test("functionInDoWhileLoop", () => {
        return spec.rewriteRun(
            //language=typescript
            typescript(
                `
                do {
                    function helper() {
                        return 42;
                    }
                } while (condition);
                const result = helper();
                `,
                `
                let helper;
                do {
                    helper = function () {
                        return 42;
                    };
                } while (condition);
                const result = helper();
                `
            )
        );
    });

    // Tests for edge cases with function types

    test("asyncFunctionInBlock", () => {
        return spec.rewriteRun(
            //language=typescript
            typescript(
                `
                if (true) {
                    async function helper() {
                        return await Promise.resolve(42);
                    }
                }
                const result = helper();
                `,
                `
                let helper;
                if (true) {
                    helper = async function () {
                        return await Promise.resolve(42);
                    };
                }
                const result = helper();
                `
            )
        );
    });

    test("generatorFunctionInBlock", () => {
        return spec.rewriteRun(
            //language=typescript
            typescript(
                `
                if (true) {
                    function* helper() {
                        yield 1;
                        yield 2;
                    }
                }
                const result = helper();
                `,
                `
                let helper;
                if (true) {
                    helper = function* () {
                        yield 1;
                        yield 2;
                    };
                }
                const result = helper();
                `
            )
        );
    });

    test("functionWithDefaultParameters", () => {
        return spec.rewriteRun(
            //language=typescript
            typescript(
                `
                if (true) {
                    function helper(a = 1, b = 2) {
                        return a + b;
                    }
                }
                const result = helper();
                `,
                `
                let helper;
                if (true) {
                    helper = function (a = 1, b = 2) {
                        return a + b;
                    };
                }
                const result = helper();
                `
            )
        );
    });

    test("functionWithRestParameters", () => {
        return spec.rewriteRun(
            //language=typescript
            typescript(
                `
                if (true) {
                    function helper(...args) {
                        return args.length;
                    }
                }
                const result = helper(1, 2, 3);
                `,
                `
                let helper;
                if (true) {
                    helper = function (...args) {
                        return args.length;
                    };
                }
                const result = helper(1, 2, 3);
                `
            )
        );
    });

    test("functionWithTypeParameters", () => {
        return spec.rewriteRun(
            //language=typescript
            typescript(
                `
                if (true) {
                    function helper<T>(value: T): T {
                        return value;
                    }
                }
                const result = helper<number>(42);
                `,
                `
                let helper;
                if (true) {
                    helper = function <T>(value: T): T {
                        return value;
                    };
                }
                const result = helper<number>(42);
                `
            )
        );
    });

    // Tests for enclosing scope variations

    test("hoistingInsideArrowFunction", () => {
        return spec.rewriteRun(
            //language=typescript
            typescript(
                `
                const outer = () => {
                    if (condition) {
                        function helper() {
                            return 42;
                        }
                    }
                    return helper();
                };
                `,
                `
                const outer = () => {
                    let helper;
                    if (condition) {
                        helper = function () {
                            return 42;
                        };
                    }
                    return helper();
                };
                `
            )
        );
    });

    test("hoistingInsideAsyncFunction", () => {
        return spec.rewriteRun(
            //language=typescript
            typescript(
                `
                async function outer() {
                    if (condition) {
                        function helper() {
                            return 42;
                        }
                    }
                    return helper();
                }
                `,
                `
                async function outer() {
                    let helper;
                    if (condition) {
                        helper = function () {
                            return 42;
                        };
                    }
                    return helper();
                }
                `
            )
        );
    });

    // Test that verifies the analyzer correctly identifies usage patterns

    test("functionUsedInDeeplyNestedScope", () => {
        return spec.rewriteRun(
            //language=typescript
            typescript(
                `
                if (a) {
                    function helper() {
                        return 42;
                    }
                }
                if (b) {
                    if (c) {
                        if (d) {
                            const x = helper();
                        }
                    }
                }
                `,
                `
                let helper;
                if (a) {
                    helper = function () {
                        return 42;
                    };
                }
                if (b) {
                    if (c) {
                        if (d) {
                            const x = helper();
                        }
                    }
                }
                `
            )
        );
    });

    test("multipleControlFlowTypesInSameFile", () => {
        return spec.rewriteRun(
            //language=typescript
            typescript(
                `
                if (cond1) {
                    function a() { return 1; }
                }
                while (cond2) {
                    function b() { return 2; }
                }
                for (let i = 0; i < 10; i++) {
                    function c() { return 3; }
                }
                a();
                b();
                c();
                `,
                `
                let a;
                let b;
                let c;
                if (cond1) {
                    a = function () { return 1; };
                }
                while (cond2) {
                    b = function () { return 2; };
                }
                for (let i = 0; i < 10; i++) {
                    c = function () { return 3; };
                }
                a();
                b();
                c();
                `
            )
        );
    });

    // Tests for identifier reference detection (not just function calls)

    test("functionPassedAsCallback", () => {
        // The recipe detects that `helper` is used outside its block
        // when passed as a callback to another function.
        return spec.rewriteRun(
            //language=typescript
            typescript(
                `
                if (true) {
                    function helper(x) {
                        return x * 2;
                    }
                }
                const results = [1, 2, 3].map(helper);
                `,
                `
                let helper;
                if (true) {
                    helper = function (x) {
                        return x * 2;
                    };
                }
                const results = [1, 2, 3].map(helper);
                `
            )
        );
    });

    test("functionAssignedToVariable", () => {
        // The recipe detects that `helper` is used outside its block
        // when assigned to a variable.
        return spec.rewriteRun(
            //language=typescript
            typescript(
                `
                if (true) {
                    function helper() {
                        return 42;
                    }
                }
                const fn = helper;
                fn();
                `,
                `
                let helper;
                if (true) {
                    helper = function () {
                        return 42;
                    };
                }
                const fn = helper;
                fn();
                `
            )
        );
    });

    // KNOWN LIMITATION: The recipe compares functions by name only, not by identity.
    // This causes incorrect transformations when a block function shadows an outer function.
    test.skip("functionShadowingOuterFunction - KNOWN LIMITATION", () => {
        // When a block function shadows an outer function of the same name,
        // and only the outer function is called outside, we should NOT transform
        // the block function (it's not being used outside its scope).
        //
        // Currently the recipe incorrectly transforms because it matches by
        // function name rather than type identity.
        return spec.rewriteRun(
            //language=typescript
            typescript(
                `
                function helper() {
                    return 1;
                }
                if (true) {
                    function helper() {
                        return 2;
                    }
                    // inner helper is only used here
                    const x = helper();
                }
                // This calls the outer helper, not the block one
                const y = helper();
                `
            )
        );
    });
});
