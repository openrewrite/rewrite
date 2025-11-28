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
import {fromVisitor, RecipeSpec} from "../../../src/test";
import {autoFormat, AutoformatVisitor, JavaScriptVisitor, tsx, typescript} from "../../../src/javascript";


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
            type Values = {
                [key: string]: string;
            }
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
            typescript("const x = { a: 1 };",
                // TODO the leading space before `a` seems excessive
                "const x = { a: 1};"
                    // @formatter:on
            ))
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

    test.each([
        // @formatter:off
        `const short = {name: "Ivan Almeida", age: 36};`,
        `const long = {make: "Honda", model: "Jazz", year: 2008, color: "red", engine: "1.2L petrol", isRunning: true, favorite: true, parked: true};`,
        // @formatter:on
        ])('do not wrap object literals - %s', async (code) => {
        // TODO we might eventually implement the "Chop down if long" setting for this
        return spec.rewriteRun(typescript(code));
    });

    test('TSX', () => {
        return spec.rewriteRun(
            // @formatter:off
            //language=typescript
            tsx(
                `
                const ComplexComponent = function ComplexComponent({ ref, ...props }) {
                    const handleClick = () => {
                        console.log('clicked');
                    };

                    return (
                    <div ref={ref} onClick={handleClick}>
                    <h1>{props.title}</h1>
                    <p>{props.content}</p>
                    </div>
                    );
                };
                `,
                `
                const ComplexComponent = function ComplexComponent({ ref, ...props }) {
                    const handleClick = () => {
                        console.log('clicked');
                    };

                    return (
                        <div ref={ref} onClick={handleClick}>
                            <h1>{props.title}</h1>
                            <p>{props.content}</p>
                        </div>
                    );
                };
                `
            )
            // @formatter:on
        )
    })
});
