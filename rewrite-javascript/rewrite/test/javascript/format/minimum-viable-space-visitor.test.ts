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
import {MinimumViableSpacingVisitor} from "../../../src/javascript/format";
import {typescript} from "../../../src/javascript";

// TODO remodel the tests after we no longer remove all whitespace in MinimumViableSpacingVisitor
describe('MinimumViableSpacingVisitor', () => {
    const spec = new RecipeSpec()
    spec.recipe = fromVisitor(new MinimumViableSpacingVisitor());

    test.skip('basic', () => {
        return spec.rewriteRun(
            // @formatter:off
            //language=typescript
            typescript(`
                class TodoItem {
                    constructor(public title: string, public done: boolean = false) {
                    }
                    toggle(): void {
                        this.done = !this.done;
                    }

                    toString(): string {
                        return this.done ? "[x]" : "[ ]" + this.title
                    }
                }

                const list: TodoItem[] = [
                    new TodoItem("Buy milk"),
                    new TodoItem("Walk the dog")
                ];

                list[1].toggle(); // mark "Walk the dog" as done

                list.forEach(item => console.log(item.toString()));
            `,
             `class TodoItem{constructor(public title:string,public done:boolean=false){}toggle():void{this.done=!this.done;}toString():string{return this.done?"[x]":"[ ]"+this.title}}const list:TodoItem[]=[new TodoItem("Buy milk"),new TodoItem("Walk the dog")];list[1].toggle();list.forEach(item=>console.log(item.toString()));`
                // @formatter:on
        ))
    });

    test.skip('throw new', () => {
        return spec.rewriteRun(
            // @formatter:off
            //language=typescript
            typescript(`throw new Error("things went south");`,
                `throw new Error("things went south");`
                // @formatter:on
            ))
    });

    test.skip('type', () => {
        return spec.rewriteRun(
            // @formatter:off
            //language=typescript
            typescript(
                `type T1 = string;`,
                `type T1=string;`
                // @formatter:on
            ))
    });

    test.skip('await', () => {
        return spec.rewriteRun(
            // @formatter:off
            //language=typescript
            typescript(
                `const response = await fetch("https://api.example.com/users/2");`,
                `const response=await fetch("https://api.example.com/users/2");`
                // @formatter:on
            ))
    });

    test.skip('type parameters', () => {
        return spec.rewriteRun(
            // @formatter:off
            //language=typescript
            typescript(
                `function m<T extends MyType>(a:T): T {return a}`,
                `function m<T extends MyType>(a:T):T{return a}`
                // @formatter:on
            ))
    });

    test.skip('typeof', () => {
        return spec.rewriteRun(
            // @formatter:off
            //language=typescript
            typescript(
                `const a = "A";console.log(typeof a)`,
                `const a="A";console.log(typeof a)`,
                // @formatter:on
            ))
    });

    test.skip('namespace', () => {
        return spec.rewriteRun(
            // @formatter:off
            //language=typescript
            typescript(
                `export namespace MathUtils { export const PI = 3.14}`,
                `export namespace MathUtils{export const PI=3.14}`,
                // @formatter:on
            ))
    });
});

