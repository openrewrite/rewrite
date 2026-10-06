import {MethodMatcher} from "../../src/javascript/method-matcher";
import {ExecutionContext, foundSearchResult, Recipe} from "../../src";
import {JavaScriptVisitor, npm, packageJson, typescript} from "../../src/javascript";
import {J} from "../../src/java";
import {fromVisitor, RecipeSpec} from "../../src/test";
import {usesMethod} from "../../src/javascript/preconditions";
import {withDir} from "tmp-promise";

describe('MethodMatcher', () => {
    function markMatchedMethods(pattern: string, matchOverrides: boolean = false): Recipe {
        class MethodMatcherRecipe extends Recipe {
            name = 'Method matcher';
            displayName = 'Mark matched methods';
            description = 'Marks methods that match the pattern';

            async editor(): Promise<JavaScriptVisitor<ExecutionContext>> {
                const matcher = new MethodMatcher(pattern, matchOverrides);
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

    describe('Pattern: Array indexOf(..)', () => {
        test('a type without a package matches only the type of that name in no package', async () => {
            const spec = new RecipeSpec();
            spec.recipe = markMatchedMethods('Array indexOf(..)');

            //language=typescript
            await spec.rewriteRun(
                typescript(
                    `
                        namespace NS {
                            export class Array {
                                indexOf(x: number): number { return x; }
                            }
                        }
                        new NS.Array().indexOf(1);
                        [1].indexOf(1);
                    `,
                    //@formatter:off
                `
                    namespace NS {
                        export class Array {
                            indexOf(x: number): number { return x; }
                        }
                    }
                    new NS.Array().indexOf(1);
                    /*~~>*/[1].indexOf(1);
                `
                //@formatter:on
                )
            );
        });
    });

    describe('Pattern: Foo bar(..)', () => {
        test('a `declare global` augmentation is in no package, with or without its `global.` prefix', async () => {
            for (const pattern of ['Foo bar(..)', 'global.Foo bar(..)']) {
                const spec = new RecipeSpec();
                spec.recipe = markMatchedMethods(pattern);

                //language=typescript
                await spec.rewriteRun(
                    typescript(
                        `
                            declare global {
                                interface Foo { bar(): void }
                            }
                            declare const f: Foo;
                            f.bar();
                            export {};
                        `,
                        //@formatter:off
                    `
                        declare global {
                            interface Foo { bar(): void }
                        }
                        declare const f: Foo;
                        /*~~>*/f.bar();
                        export {};
                    `
                    //@formatter:on
                    )
                );
            }
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
    describe('Pattern: fs-extra *(..)', () => {
        test('should match a package method however the module is bound', async () => {
            const spec = new RecipeSpec();
            spec.recipe = markMatchedMethods('fs-extra ensureDir(..)');

            await withDir(async (repo) => {
                await spec.rewriteRun(
                    npm(
                        repo.path,
                        //language=typescript
                        typescript(
                            `
                                import fse from 'fs-extra';
                                import * as ns from 'fs-extra';
                                import {ensureDir} from 'fs-extra';

                                fse.ensureDir('a');
                                ns.ensureDir('b');
                                ensureDir('c');
                                fse.pathExists('d');
                            `,
                            //@formatter:off
                            `
                                import fse from 'fs-extra';
                                import * as ns from 'fs-extra';
                                import {ensureDir} from 'fs-extra';

                                /*~~>*/fse.ensureDir('a');
                                /*~~>*/ns.ensureDir('b');
                                /*~~>*/ensureDir('c');
                                fse.pathExists('d');
                            `
                            //@formatter:on
                        ),
                        //language=json
                        packageJson(
                            `
                              {
                                "name": "test-project",
                                "version": "1.0.0",
                                "dependencies": {
                                  "fs-extra": "^11"
                                },
                                "devDependencies": {
                                  "@types/fs-extra": "^11"
                                }
                              }
                            `
                        )
                    )
                );
            }, {unsafeCleanup: true});
        });
    });

    describe('matchOverrides', () => {
        const hierarchy = `
            interface Saver { save(): void }
            interface Store extends Saver { save(): void }
            class Base implements Saver { save(): void {} }
            class Mid extends Base {}
            class Leaf extends Mid { save(): void {} }
            declare const store: Store;
        `;

        test('a pattern naming a superclass or superinterface matches a call declared on a subtype', async () => {
            const spec = new RecipeSpec();
            spec.recipe = markMatchedMethods('Base save()', true);
            //language=typescript
            await spec.rewriteRun(
                typescript(
                    `${hierarchy}
                    new Leaf().save();
                    `,
                    `${hierarchy}
                    /*~~>*/new Leaf().save();
                    `
                )
            );

            spec.recipe = markMatchedMethods('Saver save()', true);
            //language=typescript
            await spec.rewriteRun(
                typescript(
                    `${hierarchy}
                    store.save();
                    `,
                    `${hierarchy}
                    /*~~>*/store.save();
                    `
                ),
                typescript(
                    `${hierarchy}
                    new Leaf().save();
                    `,
                    `${hierarchy}
                    /*~~>*/new Leaf().save();
                    `
                )
            );
        });

        test('walks up from the declaring type only, as in Java', async () => {
            // `base.save()` is declared on `Base`, which no pattern naming `Leaf` reaches.
            const spec = new RecipeSpec();
            spec.recipe = markMatchedMethods('Leaf save()', true);
            //language=typescript
            await spec.rewriteRun(
                typescript(
                    `${hierarchy}
                    const base: Base = new Leaf();
                    base.save();
                    `
                )
            );
        });

        test('terminates on heritage cycles TypeScript accepts', async () => {
            const spec = new RecipeSpec();
            spec.recipe = markMatchedMethods('Unrelated save()', true);
            //language=typescript
            await spec.rewriteRun(
                typescript(
                    `
                    interface I extends B { save(): void }
                    class B implements I { save(): void {} }
                    new B().save();
                    `
                ),
                typescript(
                    `
                    class D implements J { save(): void {} }
                    class C extends D { save(): void {} }
                    interface J extends C {}
                    new C().save();
                    `
                )
            );
        });

        test('usesMethod passes matchOverrides to its native visitor', async () => {
            const spec = new RecipeSpec();
            spec.recipe = fromVisitor(usesMethod('Base save()', true).localVisitor!);
            //language=typescript
            await spec.rewriteRun(
                typescript(
                    `${hierarchy}
                    new Leaf().save();
                    `,
                    `${hierarchy}
                    /*~~>*/new Leaf().save();
                    `
                )
            );

            spec.recipe = fromVisitor(usesMethod('Base save()').localVisitor!);
            //language=typescript
            await spec.rewriteRun(
                typescript(
                    `${hierarchy}
                    new Leaf().save();
                    `
                )
            );
        });
    });
});
