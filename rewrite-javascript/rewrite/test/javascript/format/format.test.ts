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

/**
 * Tests for AutoformatVisitor - the full formatting pipeline.
 *
 * GUIDELINES FOR TEST AUTHORS:
 *
 * 1. COMPACT TESTS: Prefer fewer, more comprehensive tests over many small focused tests.
 *    Since test output shows the full source diff, it's more efficient to combine related
 *    formatting scenarios into a single test with multiple variations in the source text.
 *    For example, test type annotations, shorthand properties, and related features together.
 *
 * 2. VISITOR-SPECIFIC TESTS: If your test targets a specific visitor's behavior, put it in
 *    that visitor's dedicated test file instead:
 *    - SpacesVisitor tests → spaces-visitor.test.ts
 *    - BlankLinesVisitor tests → blank-lines-visitor.test.ts
 *    - TabsAndIndentsVisitor tests → tabs-and-indents-visitor.test.ts
 *    - MinimumViableSpacingVisitor tests → minimum-viable-space-visitor.test.ts
 *
 *    This file (format.test.ts) should test the formatter as a whole, focusing on
 *    integration scenarios and end-to-end formatting behavior.
 */

import {fromVisitor, RecipeSpec} from "../../../src/test";
import {
    autoFormat,
    AutoformatVisitor,
    JavaScriptVisitor,
    SpacesVisitor,
    tsx,
    typescript
} from "../../../src/javascript";


describe('AutoformatVisitor', () => {
    const spec = new RecipeSpec()
    spec.recipe = fromVisitor(new AutoformatVisitor());

    test('everything', () => {
        return spec.rewriteRun(
            // TODO there should be no newline after the default case in switch
            // TODO not sure if there should be a newline after the if and after the finally
            // @formatter:off
            //language=typescript
            typescript(`
                     type T1=string;
                     export   type   T2   =   string;
                    abstract class L {}
                    class K extends L{
                        constructor  ( ){
                            super();
                        }
                        m ( x :number  ,  y  :  number[] ) :number{
                            this.m( x, [1] );
                            return y[ 0 ];
                        }
                        s ( s: string ):number{
                            switch( s  ){
                                case "apple"  :
                                    return 1;
                                default   :
                                    return 0;
                            }
                        }
                    }
                    if(   1>0 ){
                        console.log  (   "four"   ,    "three"  ,    "six"   );
                    }
                    let i                                  =   1;
                    while(   i<4   ){
                        i++;
                    }
                    try{
                        throw new Error("test");
                    }catch(  error  ){
                        console.log("Error " + error);
                    }finally{
                        console.log("finally");
                    }
                    const isTypeScriptFun = i > 3?"yes":"hell yeah!";
                    for(   let j=1 ;j<=5 ;j++ ){
                        console.log(\`Number: \` + j);
                    }
                `,
                `
                    type T1 = string;
                    export type T2 = string;

                    abstract class L {
                    }

                    class K extends L {
                        constructor() {
                            super();
                        }

                        m(x: number, y: number[]): number {
                            this.m(x, [1]);
                            return y[0];
                        }

                        s(s: string): number {
                            switch (s) {
                                case "apple":
                                    return 1;
                                default:
                                    return 0;
                            }
                        }
                    }
                    if (1 > 0) {
                        console.log("four", "three", "six");
                    }
                    let i = 1;
                    while (i < 4) {
                        i++;
                    }
                    try {
                        throw new Error("test");
                    } catch (error) {
                        console.log("Error " + error);
                    } finally {
                        console.log("finally");
                    }
                    const isTypeScriptFun = i > 3 ? "yes" : "hell yeah!";
                    for (let j = 1; j <= 5; j++) {
                        console.log(\`Number: \` + j);
                    }
                `)
            // @formatter:on
        )
    });

    test('types', () => {
        return spec.rewriteRun(
            // @formatter:off
            //language=typescript
            typescript(`
            type Values={[key:string]:string;}
            `,
            `
            type Values = { [key: string]: string; }
            `)
            // @formatter:on
        )
    });

    test('inline type annotations stay single-line', () => {
        return spec.rewriteRun(
            // @formatter:off
            //language=typescript
            typescript(`
            function foo(x:{name:string}):void{}
            const bar=(opts:{html:string})=>{};
            `,
            `
            function foo(x: { name: string }): void {
            }
            const bar = (opts: { html: string })=>{
            };
            `)
            // @formatter:on
        )
    });

    test('a statement following an if', () => {
        return spec.rewriteRun(
            // @formatter:off
            //language=typescript
            typescript(`
            if (1>0) {
            }
            let i = 1;
            `,
            `
            if (1 > 0) {
            }
            let i = 1;
            `)
            // @formatter:on
        )
    });

    test('control structure with non-block body', () => {
        return spec.rewriteRun(
            // @formatter:off
            //language=typescript
            typescript(`
            if (condition)
            doSomething();
            while (running)
            process();
            for (let i = 0; i < 10; i++)
            iterate();
            `,
            `
            if (condition)
                doSomething();
            while (running)
                process();
            for (let i = 0; i < 10; i++)
                iterate();
            `)
            // @formatter:on
        )
    });

    test('try catch-all', () => {
        return spec.rewriteRun(
            // @formatter:off
            //language=typescript
            typescript(`
            try {
                m();
            } catch {
                console.log("It failed", e);
            }
            `,
                `
            try {
                m();
            } catch {
                console.log("It failed", e);
            }
            `)
            // @formatter:on
        )
    });

    test('import', () => {
        return spec.rewriteRun(
            // @formatter:off
            //language=typescript
            typescript(`import { delta,gamma} from 'delta.js'`,
                 `import {delta, gamma} from 'delta.js'`)
            // @formatter:on
        )
    });

    test('object literal in a single line', () => {
        return spec.rewriteRun(
            // @formatter:off
            //language=typescript
            // Single-line object literals should preserve their whitespace
            typescript("const x = { a: 1 };")
            // @formatter:on
        )
    });

    test('after unary not operator', () => {
        return spec.rewriteRun(
            // @formatter:off
            //language=typescript
            typescript(
                `const b = ! true`,
                `const b = !true`,
            )
            // @formatter:on
        )
    });

    test('unary negative should not have space', () => {
        return spec.rewriteRun(
            // @formatter:off
            //language=typescript
            typescript(
                // Should not change -1 to - 1
                `if (obj === -1) {
                }`,
            )
            // @formatter:on
        )
    });

    test('nested method invocation preserves indentation when formatting subtree', () => {
        // This test simulates what happens when the templating system replaces a node
        // and calls maybeAutoFormat() on just that subtree
        const visitor = new class extends JavaScriptVisitor<any> {
            override async visitMethodInvocation(methodInvocation: any, p: any): Promise<any> {
                // Only format the logger.info() call, simulating a template replacement
                if (methodInvocation.name?.simpleName === 'info') {
                    // Format just this subtree (this is what causes the bug)
                    return await autoFormat(methodInvocation, p, undefined, this.cursor.parent);
                }
                return super.visitMethodInvocation(methodInvocation, p);
            }
        }();

        const testSpec = new RecipeSpec();
        testSpec.recipe = fromVisitor(visitor);

        return testSpec.rewriteRun(
            // @formatter:off
            //language=typescript
            typescript(
                `
                function normalFunction() {
                    logger.info("normal");
                }
                `
            )
            // @formatter:on
        )
    });

    test('honor original lack of newline before function expression', () => {
        return spec.rewriteRun(
            // @formatter:off
            //language=typescript
            typescript(
                "const x = function () { return 136; };",
                `
                const x = function () {
                    return 136;
                };
                `
            )
            // @formatter:on
        )
    });

    test('honor original newline before function expression', () => {
        return spec.rewriteRun(
            // @formatter:off
            //language=typescript
            typescript(
                `const x =
                    function () { return 136; };`,
                `
                const x =
                    function () {
                        return 136;
                    };
                `
            )
            // @formatter:on
        )
    });

    test('class method', () => {
        return spec.rewriteRun(
            // @formatter:off
            //language=typescript
            typescript(
                `
                class A {
                    m(): number {
                        return 136;
                    }
                }`
            )
            // @formatter:on
        )
    });

    test('empty braces', () => {
        return spec.rewriteRun(
            // @formatter:off
            //language=typescript
            typescript(
                "const c = typeof {} === 'object';"
            )
            // @formatter:on
        )
    });

    test('empty class', () => {
        return spec.rewriteRun(
            // @formatter:off
            //language=typescript
            typescript(
                "abstract class L {}",
                `abstract class L {
                }
                `
            )
            // @formatter:on
        )
    });

    test('anonymous class', () => {
        return spec.rewriteRun(
            // @formatter:off
            //language=typescript
            typescript(
                `const C = class extends Object {
                }
                `
            )
            // @formatter:on
        )
    });

    test.each([
        // @formatter:off
        `const short = {name: "Ivan Almeida", age: 36};`,
        `const long = {make: "Honda", model: "Jazz", year: 2008, color: "red", engine: "1.2L petrol", isRunning: true, favorite: true, parked: true};`,
        // @formatter:on
        ])('do not wrap object literals - %s', async (code) => {
        // TODO we might eventually implement the "Chop down if long" setting for this
        return spec.rewriteRun(typescript(code));
    });

    test('single-line object literal should not get extra spaces before closing brace', () => {
        return spec.rewriteRun(
            // @formatter:off
            //language=typescript
            typescript(
                `function test() {
    const x = { a: 1 };
}`,
            )
            // @formatter:on
        )
    });

    test('inline comment should stay on the same line', () => {
        return spec.rewriteRun(
            // @formatter:off
            //language=typescript
            typescript(
                `const recipe = findRecipe(registry, 'recipe', {});
expect(recipe).toBeNull(); // Multiple matches`
            )
            // @formatter:on
        )
    });

    test('empty arrow function body expands to multiple lines', () => {
        return spec.rewriteRun(
            // @formatter:off
            //language=typescript
            typescript(
                `const noop = () => {};`,
                `const noop = () => {
};`
            )
            // @formatter:on
        )
    });

    test('empty function body in method expands to multiple lines', () => {
        return spec.rewriteRun(
            // @formatter:off
            //language=typescript
            typescript(
                `jest.spyOn(console, 'error').mockImplementation(() => {});`,
                `jest.spyOn(console, 'error').mockImplementation(() => {
});`
            )
            // @formatter:on
        )
    });

    test('comment before argument should keep indentation', () => {
        return spec.rewriteRun(
            // @formatter:off
            //language=typescript
            typescript(
                `await spec.rewriteRun(
    //language=typescript
    typescript('const x = 3 + 3;')
);`
            )
            // @formatter:on
        )
    });

    test('multi-line method arguments inside arrow function should keep indentation', () => {
        return spec.rewriteRun(
            // @formatter:off
            //language=typescript
            typescript(
                `
                    test('should detect usage as direct function call', async () => {
                        const spec = new RecipeSpec();
                        spec.recipe = fromVisitor(createAddImportWithTemplateVisitor(
                            "promisify(fs.readFile)",
                            "util",
                            "promisify"
                        ));
                    });
                    `
            )
            // @formatter:on
        )
    });

    test('multi-line ternary expression should preserve line breaks', () => {
        return spec.rewriteRun(
            // @formatter:off
            //language=typescript
            typescript(
            `
                const baseContent = start > 0
                    ? content.substring(0, start)
                    : content;
                `
            )
            // @formatter:on
        )
    });

    test('multi-line ternary expression inside function should preserve indentation', () => {
        return spec.rewriteRun(
            // @formatter:off
            //language=typescript
            typescript(
                `function test() {
    const baseContent = start > 0
        ? content.substring(0, start)
        : content;
}`
            )
            // @formatter:on
        )
    });

    test('multi-line binary expression should preserve line breaks', () => {
        return spec.rewriteRun(
            // @formatter:off
            //language=typescript
            typescript(
            `
                const valid = typeof node.value === 'number' &&
                    aValue !== undefined &&
                    typeof aValue.value === 'number' &&
                    node.value > aValue.value;
                `
            )
            // @formatter:on
        )
    });

    test('anonymous class inside method argument should preserve indentation', () => {
        return spec.rewriteRun(
            // @formatter:off
            //language=typescript
            typescript(
                `await new class extends Visitor {
    protected async visitProperty(prop: Property): Promise<J | undefined> {
        expect(prop.name).toBe('foo');
        return prop;
    }
}().visit(cu, undefined);`
            )
            // @formatter:on
        )
    });

    test('async function inside arrow function body should not get extra whitespace', () => {
        return spec.rewriteRun(
            // @formatter:off
            //language=typescript
            typescript(
                `const fn = () => {
    async function inner() {
    }
};`
            )
            // @formatter:on
        )
    });

    test('generator function should have space after asterisk (default style)', () => {
        return spec.rewriteRun(
            // @formatter:off
            //language=typescript
            typescript(
                `function* myGenerator() {
    yield 1;
}`
            )
            // @formatter:on
        )
    })

    test('generator function without space after asterisk should be fixed', () => {
        return spec.rewriteRun(
            // @formatter:off
            //language=typescript
            typescript(
                `function*myGenerator() {
    yield 1;
}`,
                `function* myGenerator() {
    yield 1;
}`
            )
            // @formatter:on
        )
    })

    test('async generator method with space before asterisk should be fixed', () => {
        return spec.rewriteRun(
            // @formatter:off
            //language=typescript
            typescript(
                `class Parser {
    async *parse(): AsyncGenerator<number> {
        yield 1;
    }
}`,
                `class Parser {
    async* parse(): AsyncGenerator<number> {
        yield 1;
    }
}`
            )
            // @formatter:on
        )
    })

    test('maybeAutoFormat on top-level expression preserves no indent', async () => {
        // This test verifies that when template replaces a top-level method invocation,
        // the result doesn't get incorrectly indented.
        const visitor = new class extends JavaScriptVisitor<any> {
            override async visitMethodInvocation(methodInvocation: any, p: any): Promise<any> {
                // Only format the slice() call
                if (methodInvocation.name?.simpleName === 'slice') {
                    // Format just this subtree - simulates template replacement
                    return await autoFormat(methodInvocation, p, undefined, this.cursor.parent);
                }
                return super.visitMethodInvocation(methodInvocation, p);
            }
        }();

        const testSpec = new RecipeSpec();
        testSpec.recipe = fromVisitor(visitor);

        return testSpec.rewriteRun(
            // @formatter:off
            //language=typescript
            typescript(
                `const buf = Buffer.alloc(10);
buf.slice();`
            )
            // @formatter:on
        )
    })

    test('inline comment after last statement in block should stay on same line', () => {
        return spec.rewriteRun(
            // @formatter:off
            //language=typescript
            typescript(
                `async function getMarkerOrSkip(fixtureSubDir: string): Promise<NodeResolutionResult> {
    const marker = await parseAndGetMarker(fixtureSubDir);
    if (!marker || marker.resolvedDependencies.length === 0) {
        // Skip test if we couldn't resolve dependencies (CLI not available or packages not installed)
        return null as any; // Will be caught by the conditional skip
    }
    return marker;
}`
            )
            // @formatter:on
        )
    })

    test('ternary with template literal should not get extra space before colon', () => {
        return spec.rewriteRun(
            // @formatter:off
            //language=typescript
            // Complex template literal with expressions - should not add extra space before colon
            typescript(
                "const info = cond ? `value=${x}` : 'default';",
                "const info = cond ? `value=${x}` : 'default';"
            )
            // @formatter:on
        )
    })

    test('multi-line assignment with newline after = should preserve newline', () => {
        return spec.rewriteRun(
            // @formatter:off
            //language=typescript
            typescript(
                `
                const hasExpectedProperties =
                    memberNames.includes('innerHTML') &&
                    memberNames.includes('outerHTML');
                `
            )
            // @formatter:on
        )});

    test('type annotation and shorthand property spacing', () => {
        return spec.rewriteRun(
            // @formatter:off
            //language=typescript
            // Type annotation should have space after colon (fixed from req:Request)
            // Shorthand properties should preserve their brace spacing
            typescript(
                `function test(req:Request): Response {
    const withSpaces = { headers };
    const noSpaces = {headers};
    return req;
}`,
                `function test(req: Request): Response {
    const withSpaces = { headers };
    const noSpaces = {headers};
    return req;
}`
            )
            // @formatter:on
        )
    });

    test('multi-line import preserves structure', () => {
        return spec.rewriteRun(
            // @formatter:off
            //language=typescript
            // Multi-line imports should preserve their newlines, not collapse to single line
            typescript(
                `import {
    foo,
    bar,
    baz,
} from "module"
const x = 1;`
            )
            // @formatter:on
        )
    });
});
