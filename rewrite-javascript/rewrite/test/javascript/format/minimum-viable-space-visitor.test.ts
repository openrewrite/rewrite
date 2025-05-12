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

describe('MinimumViableSpacingVisitor', () => {
    const spec = new RecipeSpec()
    spec.recipe = fromVisitor(new MinimumViableSpacingVisitor());

    test('basic', () => {
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
});

