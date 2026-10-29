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

import {Cursor, ExecutionContext, foundSearchResult, produceAsync} from "../../src";
import {emptySpace, Expression, J, rightPadded, TextComment} from "../../src/java";
import {JavaScriptVisitor, JS, template, typescript} from "../../src/javascript";
import {autoFormat, maybeAutoFormat} from "../../src/javascript/format";
import {fromVisitor, RecipeSpec} from "../../src/test";

describe('JavaScript visitor formatting', () => {
    test.for([
        'maybeAutoFormat',
        'autoFormat',
    ])('%s formats code after modification', async (formatMethod) => {
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
        spec.cycles = 1;

        // when && test
        await spec.rewriteRun(
            //language=typescript
            typescript(
                'console.log("hello");',
                'console.log("hello", `extra`);'
            )
        );
    });

    test('visits the space before await in for-await-of', async () => {
        // given
        class RenameCommentVisitor extends JavaScriptVisitor<ExecutionContext> {
            public override async visitSpace(space: J.Space, ctx: ExecutionContext): Promise<J.Space> {
                const s = await super.visitSpace(space, ctx);
                return {
                    ...s,
                    comments: s.comments.map(c => c.kind === J.Kind.TextComment && (c as TextComment).text === ' c ' ?
                        {...c, text: ' d '} as TextComment : c)
                };
            }
        }

        const spec = new RecipeSpec();
        spec.recipe = fromVisitor(new RenameCommentVisitor());
        spec.cycles = 1;

        // when && then
        await spec.rewriteRun(
            //language=typescript
            typescript(
                'async function f(xs: AsyncIterable<number>) { for /* c */ await (const x of xs) {} }',
                'async function f(xs: AsyncIterable<number>) { for /* d */ await (const x of xs) {} }'
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

    test('visits modifiers of constructor types', async () => {
        class MarkModifierVisitor extends JavaScriptVisitor<ExecutionContext> {
            protected override async visitModifier(modifier: J.Modifier, ctx: ExecutionContext): Promise<J | undefined> {
                return foundSearchResult(await super.visitModifier(modifier, ctx) as J.Modifier);
            }
        }

        const spec = new RecipeSpec();
        spec.recipe = fromVisitor(new MarkModifierVisitor());

        await spec.rewriteRun(
            //language=typescript
            typescript(
                'type C = abstract new () => object;',
                'type C = /*~~>*/abstract new () => object;'
            )
        );
    });
});
