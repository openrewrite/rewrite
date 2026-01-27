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
 * Tests for MinimumViableSpacingVisitor - ensures minimum required spacing exists.
 *
 * GUIDELINES FOR TEST AUTHORS:
 *
 * 1. COMPACT TESTS: Prefer fewer, more comprehensive tests over many small focused tests.
 *    Since test output shows the full source diff, it's more efficient to combine related
 *    spacing scenarios into a single test with multiple variations in the source text.
 *
 * 2. SCOPE: This file should contain tests specific to MinimumViableSpacingVisitor behavior.
 *    For full formatter integration tests, use format.test.ts.
 */

import {fromVisitor, RecipeSpec, SourceSpec} from "../../../src/test";
import {MinimumViableSpacingVisitor} from "../../../src/javascript/format";
import {JavaScriptParser, JavaScriptVisitor, JS, typescript} from "../../../src/javascript";
import {create as produce} from "mutative";
import {mapAsync, ParserInput, SourceFile} from "../../../src";
import {J} from "../../../src/java";

describe('MinimumViableSpacingVisitor', () => {
    const spec = new RecipeSpec()
    spec.checkParsePrintIdempotence = false;
    spec.recipe = fromVisitor(new MinimumViableSpacingVisitor());

    function typescriptWithSpacesRemoved(before: string | null, after?: string): SourceSpec<JS.CompilationUnit> {
        const ret = typescript(before, after);
        class JavaScriptParserWithSpacesRemoved extends JavaScriptParser {
            constructor() {
                super({});
            }

            static RemoveSpaces = class <P> extends JavaScriptVisitor<P> {
                override visitSpace(space: J.Space, p: P): J.Space {
                    const ret = super.visitSpace(space, p) as J.Space;
                    return ret && produce(ret, draft => {
                        draft.whitespace = "";
                    });
                }
            }

            override async *parse(...inputs: ParserInput[]): AsyncGenerator<JS.CompilationUnit> {
                const removeSpaces = new JavaScriptParserWithSpacesRemoved.RemoveSpaces();

                for await (const file of super.parse(...inputs)) {
                    yield removeSpaces.visit<JS.CompilationUnit>(file, undefined)!;
                }
            }
        }
        return produce(ret, draft => {
            draft.parser = () => new JavaScriptParserWithSpacesRemoved()
        });
    }

    test('basic', () => {
        return spec.rewriteRun(
            // @formatter:off
            //language=typescript
            typescriptWithSpacesRemoved(`
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

                list[1].toggle();

                list.forEach(item => console.log(item.toString()));
            `,
             `class TodoItem{constructor(public title:string,public done:boolean=false){}toggle():void{this.done=!this.done;}toString():string{return this.done?"[x]":"[ ]"+this.title}}const list:TodoItem[]=[new TodoItem("Buy milk"),new TodoItem("Walk the dog")];list[1].toggle();list.forEach(item=>console.log(item.toString()));`
                // @formatter:on
                // TODO it fails when ` // mark "Walk the dog" as done` is added to the toggle line.
        ))
    });

    test('throw new', () => {
        return spec.rewriteRun(
            // @formatter:off
            //language=typescript
            typescriptWithSpacesRemoved(`throw new Error("things went south");`,
                `throw new Error("things went south");`
                // @formatter:on
            ))
    });

    test('type', () => {
        return spec.rewriteRun(
            // @formatter:off
            //language=typescript
            typescriptWithSpacesRemoved(
                `type T1 = string;`,
                `type T1=string;`
                // @formatter:on
            ))
    });

    test('await', () => {
        return spec.rewriteRun(
            // @formatter:off
            //language=typescript
            typescriptWithSpacesRemoved(
                `const response = await fetch("https://api.example.com/users/2");`,
                `const response=await fetch("https://api.example.com/users/2");`
                // @formatter:on
            ))
    });

    test('type parameters', () => {
        return spec.rewriteRun(
            // @formatter:off
            //language=typescript
            typescriptWithSpacesRemoved(
                `function m<T extends MyType>(a:T): T {return a}`,
                `function m<T extends MyType>(a:T):T{return a}`
                // @formatter:on
            ))
    });

    test('typeof', () => {
        return spec.rewriteRun(
            // @formatter:off
            //language=typescript
            typescriptWithSpacesRemoved(
                `const a = "A";console.log(typeof a)`,
                `const a="A";console.log(typeof a)`,
                // @formatter:on
            ))
    });

    test('namespace', () => {
        return spec.rewriteRun(
            // @formatter:off
            //language=typescript
            typescriptWithSpacesRemoved(
                `export namespace MathUtils { export const PI = 3.14}`,
                `export namespace MathUtils{export const PI=3.14}`,
                // @formatter:on
            ))
    });
});

