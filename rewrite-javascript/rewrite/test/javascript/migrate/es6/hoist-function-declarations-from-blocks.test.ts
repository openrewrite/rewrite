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
import {
    HoistFunctionDeclarationsFromBlocks
} from "../../../../src/javascript/migrate/es6/hoist-function-declarations-from-blocks";
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

    test("functionCallViaOptionalChaining", () => {
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
});
