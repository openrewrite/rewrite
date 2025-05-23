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
import {IntelliJ, TabsAndIndentsStyle, typescript} from "../../../src/javascript";
import {AutoformatVisitor, TabsAndIndentsVisitor} from "../../../src/javascript/format";
import {Draft, produce} from "immer";
import {Style} from "../../../src";

type StyleCustomizer<T extends Style> = (draft: Draft<T>) => void;

function tabsAndIndents(customizer: StyleCustomizer<TabsAndIndentsStyle>): TabsAndIndentsStyle {
    return produce(IntelliJ.TypeScript.tabsAndIndents(), draft => customizer(draft));
}

describe('TabsAndIndentsVisitor', () => {

    test('simple', () => {
        const spec = new RecipeSpec()
        spec.recipe = fromVisitor(new TabsAndIndentsVisitor(tabsAndIndents(draft => {
        })));
        return spec.rewriteRun(
            // @formatter:off
            //language=typescript
            typescript(`
                class A {
                x = 3;
                m() {
                const z = 5;
                }
                }
                `,
                `
                class A {
                    x = 3;
                    m() {
                        const z = 5;
                    }
                }    
                `)
            // @formatter:on
        )
    });

    test('indent', () => {
        const spec = new RecipeSpec()
        spec.recipe = fromVisitor(new TabsAndIndentsVisitor(tabsAndIndents(draft => {
        })));
        return spec.rewriteRun(
            // @formatter:off
            //language=typescript
            typescript(`
                class Person {
                name: string;
                age: number;

                constructor(name: string, age: number) {
                this.name = name;
                this.age = age;
                }

                greet(): void {
                console.log("Hi, I'm " + this.name + " and I'm " + this.age + " years old.");
                }
                }

                const alice = new Person("Alice", 30);
                alice.greet();                
                `,
                `
                class Person {
                    name: string;
                    age: number;

                    constructor(name: string, age: number) {
                        this.name = name;
                        this.age = age;
                    }

                    greet(): void {
                        console.log("Hi, I'm " + this.name + " and I'm " + this.age + " years old.");
                    }
                }

                const alice = new Person("Alice", 30);
                alice.greet();                    
                `)
            // @formatter:on
        )
    });

    test("not so simple", () => {
        const spec = new RecipeSpec()
        spec.recipe = fromVisitor(new TabsAndIndentsVisitor(tabsAndIndents(draft => {
        })));
        return spec.rewriteRun(
            // @formatter:off
            //language=typescript
            typescript(`
                    class K{
                    constructor  ( ){
                    }
                    m ( x :number  ,  y  :  number[] ) :number{
                    this.m( x, [1] );
                    return y[ 0 ];
                    }
                    s ( s: string ):number{
                    switch( s  ){
                    case "apple"  :
                    return 1;
                    }
                    return 0;
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
                    class K{
                        constructor  ( ){
                        }
                        m ( x :number  ,  y  :  number[] ) :number{
                            this.m( x, [1] );
                            return y[ 0 ];
                        }
                        s ( s: string ):number{
                            switch( s  ){
                                case "apple"  :
                                    return 1;
                   
                            }
                            return 0;
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
                `)
            // @formatter:on
        )
    });

    test('lambda', () => {
        const spec = new RecipeSpec()
        spec.recipe = fromVisitor(new TabsAndIndentsVisitor(tabsAndIndents(draft => {
        })));
        return spec.rewriteRun(
            // @formatter:off
            //language=typescript
            typescript(`console.log(() => "a");`)
            // @formatter:on
        )
    });
});
