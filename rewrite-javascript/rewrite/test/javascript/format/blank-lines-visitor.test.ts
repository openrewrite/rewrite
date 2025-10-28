// noinspection JSUnusedLocalSymbols,TypeScriptCheckImport,ES6UnusedImports

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
import {BlankLinesStyle, IntelliJ, typescript} from "../../../src/javascript";
import {BlankLinesVisitor} from "../../../src/javascript/format";
import {Draft, produce} from "immer";
import {Style} from "../../../src";

type StyleCustomizer<T extends Style> = (draft: Draft<T>) => void;

function blankLines(customizer: StyleCustomizer<BlankLinesStyle>): BlankLinesStyle {
    return produce(IntelliJ.TypeScript.blankLines(), draft => customizer(draft));
}

describe('BlankLinesVisitor', () => {
    const spec = new RecipeSpec()

    test('classes', () => {
        spec.recipe = fromVisitor(new BlankLinesVisitor(blankLines(draft => {
            draft.minimum.aroundClass = 1;
            draft.minimum.afterImports = 1;
        })));
        return spec.rewriteRun(
            // @formatter:off
            //language=typescript
            typescript(`
                import { readFileSync } from 'fs';
                class A {
                }
                class B {
                } class C {}
                `,
                `
                import { readFileSync } from 'fs';

                class A {
                }

                class B {
                }

                class C {
                }
                `),
            // @formatter:on
        )
    });

    test('fields', () => {
        spec.recipe = fromVisitor(new BlankLinesVisitor(blankLines(draft => {
            draft.minimum.aroundField = 3;
            draft.keepMaximum.inCode = 3;
        })));
        return spec.rewriteRun(
            // @formatter:off
            //language=typescript
            typescript(`
                 class A {
    
    
                   field1 = 1;
                   field2 = 2;
                   m() {
                   }
                 }
                `,
                `
                 class A {
                   field1 = 1;
                 
                
                
                   field2 = 2;
                
                
                
                   m() {
                   }
                 }
                `)
            // @formatter:on
        );
    });

    test('class with no imports', () => {
        spec.recipe = fromVisitor(new BlankLinesVisitor(blankLines(draft => {
            draft.minimum.afterImports = 7;
        })));
        return spec.rewriteRun(
            // @formatter:off
            //language=typescript
            typescript(`
                 class A {
                 }
                `)
            // @formatter:on
        );
    });

    test('simple un-minify', () => {
        spec.recipe = fromVisitor(new BlankLinesVisitor(blankLines(draft => {
        })));
        return spec.rewriteRun(
            // @formatter:off
            //language=typescript
            typescript(
                `const x=["1",2,3];let y = 2;const z = 3;`,
                `
                    const x=["1",2,3];
                    let y = 2;
                    const z = 3;
                `)
            // @formatter:on
        );
    });

    test('un-minify', () => {
        spec.recipe = fromVisitor(new BlankLinesVisitor(blankLines(draft => {
        })));
        return spec.rewriteRun(
            // @formatter:off
            //language=typescript
            typescript(`class TodoItem{constructor(public title:string,public done:boolean=false){}toggle():void{this.done=!this.done;}toString():string{return (this.done?"[x]":"[ ]")+this.title}}const list:TodoItem[]=[new TodoItem("Buy milk"),new TodoItem("Walk the dog")];list[1].toggle();list.forEach(item=>console.log(item.toString()));`,
                `
                    class TodoItem{
                    constructor(public title:string,public done:boolean=false){
                    }


                    toggle():void{
                    this.done=!this.done;
                    }


                    toString():string{
                    return (this.done?"[x]":"[ ]")+this.title
                    }
                    }
                    const list:TodoItem[]=[new TodoItem("Buy milk"),new TodoItem("Walk the dog")];
                    list[1].toggle();
                    list.forEach(item=>console.log(item.toString()));
                `)
            // @formatter:on
        );
    });
});
