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
import {autoFormat, AutoformatVisitor, JavaScriptVisitor, typescript} from "../../../src/javascript";


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

    test('anonymous function expression', () => {
        return spec.rewriteRun(
            // @formatter:off
            //language=typescript
            typescript(
                `const fn = function () {return 99;};`,
                 `
                const fn =
                    function () {
                        return 99;
                    };`
            )
            // @formatter:on
        )
    });

    test('object literal in a single line', () => {
        return spec.rewriteRun(
            // @formatter:off
            //language=typescript
            typescript("const x = { a: 1 };",
                `
                    const x = {
                        a: 1
                    };
                    `
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
});
