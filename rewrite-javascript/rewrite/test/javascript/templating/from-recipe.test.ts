/*
 * Copyright 2025 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import {fromVisitor, RecipeSpec} from "../../../src/test";
import {
    capture,
    fromRecipe,
    JavaScriptVisitor,
    pattern,
    rewrite,
    template,
    typescript
} from "../../../src/javascript";
import {J} from "../../../src/java";
import {ExecutionContext, Recipe, TreeVisitor} from "../../../src";
import {produce} from "immer";

describe('fromRecipe', () => {
    const spec = new RecipeSpec();

    test('creates a RewriteRule from a Recipe', () => {
        // Create a recipe that changes 1 to 2
        class ChangeOneToTwoRecipe extends Recipe {
            name = 'test.change-one-to-two';
            displayName = 'Change One to Two';
            description = 'Changes literal 1 to literal 2.';

            async editor(): Promise<TreeVisitor<any, ExecutionContext>> {
                return new class extends JavaScriptVisitor<ExecutionContext> {
                    override async visitLiteral(literal: J.Literal, p: ExecutionContext): Promise<J | undefined> {
                        if (literal.valueSource === '1') {
                            return produce(literal, draft => {
                                draft.value = 2;
                                draft.valueSource = '2';
                            });
                        }
                        return literal;
                    }
                };
            }
        }

        spec.recipe = fromVisitor(new class extends JavaScriptVisitor<any> {
            override async visitLiteral(literal: J.Literal, p: any): Promise<J | undefined> {
                const rule = fromRecipe(new ChangeOneToTwoRecipe(), p);
                return await rule.tryOn(this.cursor, literal) || literal;
            }
        });

        return spec.rewriteRun(
            //language=typescript
            typescript('const a = 1', 'const a = 2'),
        );
    });

    test('chains a Recipe with a rewrite rule using andThen', () => {
        // Recipe: Change x to y
        class ChangeXToYRecipe extends Recipe {
            name = 'test.change-x-to-y';
            displayName = 'Change X to Y';
            description = 'Changes identifier x to y.';

            async editor(): Promise<TreeVisitor<any, ExecutionContext>> {
                return new class extends JavaScriptVisitor<ExecutionContext> {
                    override async visitIdentifier(ident: J.Identifier, p: ExecutionContext): Promise<J | undefined> {
                        if (ident.simpleName === 'x') {
                            return produce(ident, draft => {
                                draft.simpleName = 'y';
                            });
                        }
                        return ident;
                    }
                };
            }
        }

        // Rule: Swap operands
        const swapRule = rewrite(() => ({
            before: pattern`${capture('a')} + ${capture('b')}`,
            after: template`${capture('b')} + ${capture('a')}`
        }));

        spec.recipe = fromVisitor(new class extends JavaScriptVisitor<any> {
            override async visitBinary(binary: J.Binary, p: any): Promise<J | undefined> {
                // Chain: First swap, then change x to y
                const combined = swapRule.andThen(fromRecipe(new ChangeXToYRecipe(), p));
                return await combined.tryOn(this.cursor, binary) || binary;
            }
        });

        return spec.rewriteRun(
            //language=typescript
            // x + z -> z + x -> z + y
            typescript('const a = x + z', 'const a = z + y'),
        );
    });

    test('chains a rewrite rule with a Recipe using andThen', () => {
        // Recipe: Change 2 to 3
        class ChangeTwoToThreeRecipe extends Recipe {
            name = 'test.change-two-to-three';
            displayName = 'Change Two to Three';
            description = 'Changes literal 2 to literal 3.';

            async editor(): Promise<TreeVisitor<any, ExecutionContext>> {
                return new class extends JavaScriptVisitor<ExecutionContext> {
                    override async visitLiteral(literal: J.Literal, p: ExecutionContext): Promise<J | undefined> {
                        if (literal.valueSource === '2') {
                            return produce(literal, draft => {
                                draft.value = 3;
                                draft.valueSource = '3';
                            });
                        }
                        return literal;
                    }
                };
            }
        }

        // Rule: Change 1 to 2
        const changeRule = rewrite(() => ({
            before: pattern`1`,
            after: template`2`
        }));

        spec.recipe = fromVisitor(new class extends JavaScriptVisitor<any> {
            override async visitLiteral(literal: J.Literal, p: any): Promise<J | undefined> {
                // Chain: First change 1 to 2, then change 2 to 3
                const combined = changeRule.andThen(fromRecipe(new ChangeTwoToThreeRecipe(), p));
                return await combined.tryOn(this.cursor, literal) || literal;
            }
        });

        return spec.rewriteRun(
            //language=typescript
            // 1 -> 2 -> 3
            typescript('const a = 1', 'const a = 3'),
        );
    });

    test('returns undefined when Recipe makes no changes', () => {
        // Recipe that doesn't change anything
        class NoOpRecipe extends Recipe {
            name = 'test.noop';
            displayName = 'No-op';
            description = 'Does nothing.';

            async editor(): Promise<TreeVisitor<any, ExecutionContext>> {
                return new class extends JavaScriptVisitor<ExecutionContext> {
                    override async visitLiteral(literal: J.Literal, p: ExecutionContext): Promise<J | undefined> {
                        // Always return the same literal unchanged
                        return literal;
                    }
                };
            }
        }

        spec.recipe = fromVisitor(new class extends JavaScriptVisitor<any> {
            override async visitLiteral(literal: J.Literal, p: any): Promise<J | undefined> {
                const rule = fromRecipe(new NoOpRecipe(), p);
                return await rule.tryOn(this.cursor, literal) || literal;
            }
        });

        return spec.rewriteRun(
            //language=typescript
            // No change
            typescript('const a = 1'),
        );
    });

    test('chains multiple Recipes', () => {
        // Recipe 1: Change 1 to 2
        class ChangeOneToTwoRecipe extends Recipe {
            name = 'test.change-one-to-two';
            displayName = 'Change One to Two';
            description = 'Changes literal 1 to literal 2.';

            async editor(): Promise<TreeVisitor<any, ExecutionContext>> {
                return new class extends JavaScriptVisitor<ExecutionContext> {
                    override async visitLiteral(literal: J.Literal, p: ExecutionContext): Promise<J | undefined> {
                        if (literal.valueSource === '1') {
                            return produce(literal, draft => {
                                draft.value = 2;
                                draft.valueSource = '2';
                            });
                        }
                        return literal;
                    }
                };
            }
        }

        // Recipe 2: Change 2 to 3
        class ChangeTwoToThreeRecipe extends Recipe {
            name = 'test.change-two-to-three';
            displayName = 'Change Two to Three';
            description = 'Changes literal 2 to literal 3.';

            async editor(): Promise<TreeVisitor<any, ExecutionContext>> {
                return new class extends JavaScriptVisitor<ExecutionContext> {
                    override async visitLiteral(literal: J.Literal, p: ExecutionContext): Promise<J | undefined> {
                        if (literal.valueSource === '2') {
                            return produce(literal, draft => {
                                draft.value = 3;
                                draft.valueSource = '3';
                            });
                        }
                        return literal;
                    }
                };
            }
        }

        spec.recipe = fromVisitor(new class extends JavaScriptVisitor<any> {
            override async visitLiteral(literal: J.Literal, p: any): Promise<J | undefined> {
                // Chain two recipes
                const combined = fromRecipe(new ChangeOneToTwoRecipe(), p)
                    .andThen(fromRecipe(new ChangeTwoToThreeRecipe(), p));
                return await combined.tryOn(this.cursor, literal) || literal;
            }
        });

        return spec.rewriteRun(
            //language=typescript
            // 1 -> 2 -> 3
            typescript('const a = 1', 'const a = 3'),
        );
    });
});
