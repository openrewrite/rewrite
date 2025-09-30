import {MethodMatcher} from "../../src/javascript/method-matcher";
import {ExecutionContext, foundSearchResult, Recipe} from "../../src";
import {JavaScriptVisitor, typescript} from "../../src/javascript";
import {J} from "../../src/java";
import {RecipeSpec} from "../../src/test";

describe('MethodMatcher', () => {
    function markMatchedMethods(pattern: string): Recipe {
        class MethodMatcherRecipe extends Recipe {
            name = 'Method matcher';
            displayName = 'Mark matched methods';
            description = 'Marks methods that match the pattern';

            async editor(): Promise<JavaScriptVisitor<ExecutionContext>> {
                const matcher = new MethodMatcher(pattern);
                return new class extends JavaScriptVisitor<ExecutionContext> {
                    async visitMethodInvocation(method: J.MethodInvocation, p: ExecutionContext): Promise<J.MethodInvocation> {
                        const visited = await super.visitMethodInvocation(method, p) as J.MethodInvocation;
                        // Debug: Log when we don't have method type
                        if (!method.methodType) {
                            console.log(`No method type for: ${method.name.simpleName}`);
                        }
                        if (method.methodType && matcher.matches(method.methodType)) {
                            return foundSearchResult(visited);
                        }
                        return visited;
                    }
                };
            }
        }

        return new MethodMatcherRecipe();
    }

    describe('Pattern: *.Array *(..)', () => {
        test('should match any method of Array regardless of package', async () => {
            const spec = new RecipeSpec();
            spec.recipe = markMatchedMethods('*.Array *(..)');

            //language=typescript
            await spec.rewriteRun(
                typescript(
                    `
                        const arr = [];
                        arr.map(x => x);
                        arr.filter(x => x);
                        const str = "hello";
                        str.split("");
                    `,
                    //@formatter:off
                `
                    const arr = [];
                    /*~~>*/arr.map(x => x);
                    /*~~>*/arr.filter(x => x);
                    const str = "hello";
                    str.split("");
                `
                //@formatter:on
                )
            );
        });
    });

    describe('Pattern: *..* *(..)', () => {
        test('should match any package, any type, any method, any args', async () => {
            const spec = new RecipeSpec();
            spec.recipe = markMatchedMethods('*..* *(..)');

            //language=typescript
            await spec.rewriteRun(
                typescript(
                    `
                        console.log("hello");
                        Math.max(1, 2);
                        [1, 2].map(x => x);
                    `,
                    //@formatter:off
                `
                    /*~~>*/console.log("hello");
                    /*~~>*/Math.max(1, 2);
                    /*~~>*/[1, 2].map(x => x);
                `
                //@formatter:on
                )
            );
        });
    });

    describe('Pattern: *..* map(Function, ..)', () => {
        // TODO support function type matching
        test.skip('should match map method with Function as first arg', async () => {
            const spec = new RecipeSpec();
            spec.recipe = markMatchedMethods('*..* map(Function, ..)');

            //language=typescript
            await spec.rewriteRun(
                typescript(
                    `
                        const arr = [1, 2, 3];
                        arr.map(x => x * 2);
                        arr.filter(x => x > 1);
                        arr.reduce((a, b) => a + b, 0);
                    `,
                    //@formatter:off
                `
                    const arr = [1, 2, 3];
                    /*~~>*/arr.map(x => x * 2);
                    arr.filter(x => x > 1);
                    arr.reduce((a, b) => a + b, 0);
                `
                //@formatter:on
                )
            );
        });
    });

    describe('Pattern: *..* sum(number, number)', () => {
        test('should match sum method with exactly two number arguments', async () => {
            const spec = new RecipeSpec();
            spec.recipe = markMatchedMethods('*..* sum(number, number)');

            //language=typescript
            await spec.rewriteRun(
                typescript(
                    `
                        class Calculator {
                            sum(a: number, b: number): number {
                                return a + b;
                            }

                            subtract(a: number, b: number): number {
                                return a - b;
                            }
                        }

                        const calc = new Calculator();
                        calc.sum(1, 2);
                        calc.subtract(5, 3);
                    `,
                    //@formatter:off
                `
                    class Calculator {
                        sum(a: number, b: number): number {
                            return a + b;
                        }

                        subtract(a: number, b: number): number {
                            return a - b;
                        }
                    }

                    const calc = new Calculator();
                    /*~~>*/calc.sum(1, 2);
                    calc.subtract(5, 3);
                `
                //@formatter:on
                )
            );
        });
    });

    describe('Pattern: Array m*(..)', () => {
        test('should match Array methods starting with m', async () => {
            const spec = new RecipeSpec();
            spec.recipe = markMatchedMethods('Array m*(..)');

            //language=typescript
            await spec.rewriteRun(
                typescript(
                    `
                        const arr = [1, 2, 3];
                        arr.map(x => x);
                        arr.filter(x => x);
                    `,
                    //@formatter:off
                `
                    const arr = [1, 2, 3];
                    /*~~>*/arr.map(x => x);
                    arr.filter(x => x);
                `
                //@formatter:on
                )
            );
        });
    });

    describe('Pattern: Math m*(..)', () => {
        test('should match Math methods starting with m', async () => {
            const spec = new RecipeSpec();
            spec.recipe = markMatchedMethods('Math m*(..)');

            //language=typescript
            await spec.rewriteRun(
                typescript(
                    `
                        Math.max(1, 2);
                        Math.min(3, 4);
                        Math.floor(5.6);
                    `,
                    //@formatter:off
                `
                    /*~~>*/Math.max(1, 2);
                    /*~~>*/Math.min(3, 4);
                    Math.floor(5.6);
                `
                //@formatter:on
                )
            );
        });
    });

    describe('Pattern: console *(..)', () => {
        test('should match any console method', async () => {
            const spec = new RecipeSpec();
            spec.recipe = markMatchedMethods('Console *(..)');

            //language=typescript
            await spec.rewriteRun(
                typescript(
                    `
                        console.log("hello");
                        console.error("error");
                        console.warn("warning");
                        Math.max(1, 2);
                    `,
                    //@formatter:off
                `
                    /*~~>*/console.log("hello");
                    /*~~>*/console.error("error");
                    /*~~>*/console.warn("warning");
                    Math.max(1, 2);
                `
                //@formatter:on
                )
            );
        });
    });

    describe('Pattern: Math max(number, number)', () => {
        test('should match Math.max with two number arguments', async () => {
            const spec = new RecipeSpec();
            spec.recipe = markMatchedMethods('Math max(Array)');

            //language=typescript
            await spec.rewriteRun(
                typescript(
                    `
                        Math.max(1, 2);
                        Math.min(1, 2);
                        [1, 2].map(x => x);
                    `,
                    //@formatter:off
                `
                    /*~~>*/Math.max(1, 2);
                    Math.min(1, 2);
                    [1, 2].map(x => x);
                `
                //@formatter:on
                )
            );
        });
    });

    describe('Pattern with empty arguments', () => {
        test('should match methods with no arguments', async () => {
            const spec = new RecipeSpec();
            spec.recipe = markMatchedMethods('Date now()');

            //language=typescript
            await spec.rewriteRun(
                typescript(
                    `
                        Date.now();
                        Date.parse("2024");
                        Math.random();
                    `,
                    //@formatter:off
                `
                    /*~~>*/Date.now();
                    Date.parse("2024");
                    Math.random();
                `
                //@formatter:on
                )
            );
        });
    });
});
