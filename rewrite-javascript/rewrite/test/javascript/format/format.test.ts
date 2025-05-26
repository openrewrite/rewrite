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
import {typescript} from "../../../src/javascript";
import {AutoformatVisitor} from "../../../src/javascript/format";


describe('AutoformatVisitor', () => {
    const spec = new RecipeSpec()
    spec.recipe = fromVisitor(new AutoformatVisitor());

    test('everything', () => {
        return spec.rewriteRun(
            // TODO there should be no newline after the default case in switch
            // @formatter:off
            //language=typescript
            typescript(`
                     type T1=string;
                     export   type   T2   =   string;
                    class L {}
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

                    class L {
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
        )});

    test.skip('a statement following an if', () => {
        // TODO address the extra line added before 'let'
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
        )});
});
