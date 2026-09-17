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
import {JavaScriptParser, JavaScriptVisitor, JS, typescript, sourceFileCache} from "../../../src/javascript";
import {create as produce} from "mutative";
import {emptyMarkers, mapAsync, ParserInput, randomId, SourceFile} from "../../../src";
import {emptySpace, J} from "../../../src/java";

class RemoveSpacesVisitor<P> extends JavaScriptVisitor<P> {
    override async visitSpace(space: J.Space, p: P): Promise<J.Space> {
        const ret = await super.visitSpace(space, p) as J.Space;
        return ret && produce(ret, draft => {
            draft.whitespace = "";
        });
    }
}

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

            override async *parse(...inputs: ParserInput[]): AsyncGenerator<JS.CompilationUnit> {
                const removeSpaces = new RemoveSpacesVisitor();

                for await (const file of super.parse(...inputs)) {
                    yield (await removeSpaces.visit<JS.CompilationUnit>(file, undefined))!;
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

    test('case label', () => {
        return spec.rewriteRun(
            // @formatter:off
            //language=typescript
            typescriptWithSpacesRemoved(
                `switch (x) { case 1: break; default: break; }`,
                `switch(x){case 1:break;default:break;}`,
                // @formatter:on
            ))
    });

    test('yield value', () => {
        return spec.rewriteRun(
            // @formatter:off
            //language=typescript
            typescriptWithSpacesRemoved(
                `function* g(a) { yield a; yield* a; }`,
                `function* g(a){yield a;yield*a;}`,
                // @formatter:on
            ))
    });

    test('instanceof operands', () => {
        return spec.rewriteRun(
            // @formatter:off
            //language=typescript
            typescriptWithSpacesRemoved(
                `const b = x instanceof Foo;`,
                `const b=x instanceof Foo;`,
                // @formatter:on
            ))
    });

    test('in operands', () => {
        return spec.rewriteRun(
            // @formatter:off
            //language=typescript
            typescriptWithSpacesRemoved(
                `const b = "a" in obj; const c = x ?? y;`,
                `const b="a" in obj;const c=x??y;`,
                // @formatter:on
            ))
    });

    test('export assignment', () => {
        return spec.rewriteRun(
            // @formatter:off
            //language=typescript
            typescriptWithSpacesRemoved(
                `export default 1;`,
                `export default 1;`,
                // @formatter:on
            ),
            //language=typescript
            typescriptWithSpacesRemoved(
                `export = foo;`,
                `export=foo;`,
            ))
    });

    test('void and delete', () => {
        return spec.rewriteRun(
            // @formatter:off
            //language=typescript
            typescriptWithSpacesRemoved(
                `const v = void a; const d = delete a.b;`,
                `const v=void a;const d=delete a.b;`,
                // @formatter:on
            ))
    });

    test('new class expression', () => {
        return spec.rewriteRun(
            // @formatter:off
            //language=typescript
            typescriptWithSpacesRemoved(
                `const n = new a.B();`,
                `const n=new a.B();`,
                // @formatter:on
            ),
            //language=typescript
            typescriptWithSpacesRemoved(
                `const p = new (getClass())();`,
                `const p=new (getClass())();`,
            ))
    });

    test('type operator', () => {
        return spec.rewriteRun(
            // @formatter:off
            //language=typescript
            typescriptWithSpacesRemoved(
                `type K = keyof T;`,
                `type K=keyof T;`,
                // @formatter:on
            ))
    });

    test('scoped variable declarations', () => {
        return spec.rewriteRun(
            // @formatter:off
            //language=typescript
            typescriptWithSpacesRemoved(
                `const b = 1, c = 2;`,
                `const b=1,c=2;`,
                // @formatter:on
            ))
    });

    test('class extends', () => {
        return spec.rewriteRun(
            // @formatter:off
            //language=typescript
            typescriptWithSpacesRemoved(
                `class A extends B {}`,
                `class A extends B{}`,
                // @formatter:on
            ))
    });

    test('as and satisfies operands', () => {
        return spec.rewriteRun(
            // @formatter:off
            //language=typescript
            typescriptWithSpacesRemoved(
                `const y = x as T; const z = x satisfies T;`,
                `const y=x as T;const z=x satisfies T;`,
                // @formatter:on
            ))
    });

    test('type predicate', () => {
        return spec.rewriteRun(
            // @formatter:off
            //language=typescript
            typescriptWithSpacesRemoved(
                `function f(a: string): a is T { return true; }`,
                `function f(a:string):a is T{return true;}`,
                // @formatter:on
            ),
            //language=typescript
            typescriptWithSpacesRemoved(
                `function g(a: unknown): asserts a is T {}`,
                `function g(a:unknown):asserts a is T{}`,
            ))
    });

    test('for-of and for-in', () => {
        return spec.rewriteRun(
            // @formatter:off
            //language=typescript
            typescriptWithSpacesRemoved(
                `for (const x of y) {} for (const k in y) {}`,
                `for(const x of y){}for(const k in y){}`,
                // @formatter:on
            ))
    });

    test('import clause', () => {
        return spec.rewriteRun(
            // @formatter:off
            //language=typescript
            typescriptWithSpacesRemoved(
                `import a from "m";`,
                `import a from"m";`,
                // @formatter:on
            ),
            //language=typescript
            typescriptWithSpacesRemoved(
                `import type b from "n";`,
                `import type b from"n";`,
            ),
            //language=typescript
            typescriptWithSpacesRemoved(
                `import { c as d } from "o";`,
                `import{c as d}from"o";`,
            ),
            //language=typescript
            typescriptWithSpacesRemoved(
                `import "p";`,
                `import"p";`,
            ),
            //language=typescript
            typescriptWithSpacesRemoved(
                `import q = require("r");`,
                `import q=require("r");`,
            ))
    });

    test('export declaration clauses', () => {
        return spec.rewriteRun(
            // @formatter:off
            //language=typescript
            typescriptWithSpacesRemoved(
                `export * as ns from "m";`,
                `export* as ns from"m";`,
                // @formatter:on
            ),
            //language=typescript
            typescriptWithSpacesRemoved(
                `export { a } from "n";`,
                `export{a}from"n";`,
            ),
            //language=typescript
            typescriptWithSpacesRemoved(
                `export type { a } from "o";`,
                `export type{a}from"o";`,
            ))
    });

    test('break and continue label', () => {
        return spec.rewriteRun(
            // @formatter:off
            //language=typescript
            typescriptWithSpacesRemoved(
                `outer: for (;;) { for (;;) { continue outer; } break outer; }`,
                `outer:for(;;){for(;;){continue outer;}break outer;}`,
                // @formatter:on
            ))
    });

    test('optional catch binding', () => {
        return spec.rewriteRun(
            // @formatter:off
            //language=typescript
            typescriptWithSpacesRemoved(
                `try { doWork() } catch { recover() }`,
                `try{doWork()}catch{recover()}`,
                // @formatter:on
            ))
    });

    test('catch parameter with a modifier and no variables', async () => {
        const modifier: J.Modifier = {
            kind: J.Kind.Modifier,
            id: randomId(),
            prefix: emptySpace,
            markers: emptyMarkers,
            keyword: "const",
            type: J.ModifierType.LanguageExtension,
            annotations: []
        };

        class AddModifierVisitor<P> extends JavaScriptVisitor<P> {
            override async visitVariableDeclarations(v: J.VariableDeclarations, p: P): Promise<J | undefined> {
                const ret = await super.visitVariableDeclarations(v, p) as J.VariableDeclarations;
                return ret.variables.length === 0 ? produce(ret, draft => {
                    // a modifier is what makes the visitor go looking for a first variable
                    draft.modifiers = [modifier];
                }) : ret;
            }
        }

        const parser = new JavaScriptParser({sourceFileCache});
        for await (const cu of parser.parse({text: `try { } catch { }`, sourcePath: "a.ts"})) {
            const withModifier = (await new AddModifierVisitor().visit<JS.CompilationUnit>(cu, undefined))!;
            const formatted = await new MinimumViableSpacingVisitor().visit(withModifier, undefined);
            expect(formatted).toBeDefined();
        }
    });
});
