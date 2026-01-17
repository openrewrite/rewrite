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

import {Cursor, ExecutionContext, produceAsync} from "../../src";
import {emptySpace, Expression, J, rightPadded} from "../../src/java";
import {JavaScriptVisitor, JS, template, typescript} from "../../src/javascript";
import {autoFormat, maybeAutoFormat} from "../../src/javascript/format";
import {fromVisitor, RecipeSpec} from "../../src/test";

describe('JavaScript visitor formatting', () => {
    test.each([
        ['maybeAutoFormat', true],
        ['autoFormat', false]
    ])('%s formats code after modification', async (formatMethod: string) => {
        // given
        class AddArgumentVisitor extends JavaScriptVisitor<ExecutionContext> {
            protected override async visitMethodInvocation(
                method: J.MethodInvocation,
                ctx: ExecutionContext
            ): Promise<J | undefined> {
                const original = await super.visitMethodInvocation(method, ctx) as J.MethodInvocation;
                const altered = await produceAsync(original, async draft => {
                    const newArg = await template`\`extra\``.apply(original, this.cursor) as Expression;

                    draft.arguments.elements = [
                        ...draft.arguments.elements,
                        rightPadded(newArg, emptySpace)
                    ];
                });

                if (formatMethod == 'maybeAutoFormat') {
                    return maybeAutoFormat(original, altered!, ctx);
                } else {
                    return autoFormat(altered!, ctx);
                }
            }
        }

        const spec = new RecipeSpec();
        spec.recipe = fromVisitor(new AddArgumentVisitor());

        // when && test
        await spec.rewriteRun(
            //language=typescript
            typescript(
                'console.log("hello");',
                'console.log("hello", `extra`);'
            )
        );
    });

    describe('cursor navigation', () => {
        test('cursor.parent', async () => {
            // given
            let pathToRoot: any[] = [];

            class CursorInspectionVisitor extends JavaScriptVisitor<ExecutionContext> {
                protected override async visitLiteral(literal: J.Literal, ctx: ExecutionContext): Promise<J | undefined> {
                    if (literal.valueSource === '"b"') {
                        let current: Cursor | undefined = this.cursor;
                        while (current !== undefined) {
                            pathToRoot.push(current.value.kind);
                            current = current.parent;
                        }
                    }
                    return super.visitLiteral(literal, ctx);
                }
            }

            const spec = new RecipeSpec();
            spec.recipe = fromVisitor(new CursorInspectionVisitor());

            // when
            await spec.rewriteRun(
                //language=typescript
                typescript(`
                    function m(): void {
                      if (1 > 0) {
                      } else {
                          console.log("b");
                      }
                    }
                `)
            );

            // then
            // With intersection types, RightPadded<T> IS the element T with padding mixed in,
            // so cursor path shows the element's kind instead of J.Kind.RightPadded
            expect(pathToRoot).toEqual([
                J.Kind.Literal,           // The literal "b" itself
                J.Kind.Literal,           // RightPadded<Literal> - same kind as element
                J.Kind.Container,         // Arguments container
                J.Kind.MethodInvocation,  // console.log(...)
                J.Kind.MethodInvocation,  // RightPadded<MethodInvocation> - same kind
                J.Kind.Block,             // else { ... }
                J.Kind.Block,             // RightPadded<Block> - same kind
                J.Kind.IfElse,            // else branch
                J.Kind.If,                // if statement
                J.Kind.If,                // RightPadded<If> - same kind
                J.Kind.Block,             // function body
                J.Kind.MethodDeclaration, // function m()
                J.Kind.MethodDeclaration, // RightPadded<MethodDeclaration> - same kind
                JS.Kind.CompilationUnit,  // File
                undefined                 // Root
            ]);
        });
    });
});
