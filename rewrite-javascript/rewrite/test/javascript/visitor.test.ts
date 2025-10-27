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
                    const newArg = await template`\`extra\``.apply(this.cursor, original) as Expression;

                    draft.arguments.elements = [
                        ...draft.arguments.elements,
                        rightPadded(newArg, emptySpace)
                    ];
                });

                if (formatMethod == 'maybeAutoFormat') {
                    return maybeAutoFormat(original, altered, ctx);
                } else {
                    return autoFormat(altered, ctx);
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
            expect(pathToRoot).toEqual([
                J.Kind.Literal,
                J.Kind.RightPadded,
                J.Kind.Container,
                J.Kind.MethodInvocation,
                J.Kind.RightPadded,
                J.Kind.Block,
                J.Kind.RightPadded,
                J.Kind.IfElse,
                J.Kind.If,
                J.Kind.RightPadded,
                J.Kind.Block,
                J.Kind.MethodDeclaration,
                J.Kind.RightPadded,
                JS.Kind.CompilationUnit,
                undefined
            ]);
        });
    });
});
