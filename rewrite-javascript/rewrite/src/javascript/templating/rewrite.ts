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
import {Cursor, ExecutionContext, Recipe} from '../..';
import {J} from '../../java';
import {RewriteRule, RewriteConfig} from './types';
import {Pattern, MatchResult} from './pattern';
import {Template} from './template';

/**
 * Implementation of a replacement rule.
 */
class RewriteRuleImpl implements RewriteRule {
    constructor(
        private readonly before: Pattern[],
        private readonly after: Template | ((match: MatchResult) => Template),
        private readonly where?: (node: J, cursor: Cursor) => boolean | Promise<boolean>,
        private readonly whereNot?: (node: J, cursor: Cursor) => boolean | Promise<boolean>
    ) {
    }

    async tryOn(cursor: Cursor, node: J): Promise<J | undefined> {
        for (const pattern of this.before) {
            // Pass cursor to pattern.match() for context-aware capture constraints
            const match = await pattern.match(node, cursor);
            if (match) {
                // Evaluate context predicates after structural match
                if (this.where) {
                    const whereResult = await this.where(node, cursor);
                    if (!whereResult) {
                        continue; // Pattern matched but context doesn't, try next pattern
                    }
                }

                if (this.whereNot) {
                    const whereNotResult = await this.whereNot(node, cursor);
                    if (whereNotResult) {
                        continue; // Pattern matched but context is excluded, try next pattern
                    }
                }

                // Apply transformation
                let result: J | undefined;

                if (typeof this.after === 'function') {
                    // Call the function to get a template, then apply it
                    const template = this.after(match);
                    result = await template.apply(cursor, node, match);
                } else {
                    // Use template.apply() as before
                    result = await this.after.apply(cursor, node, match);
                }

                if (result) {
                    return result;
                }
            }
        }

        // Return undefined if no patterns match or all context checks failed
        return undefined;
    }

    andThen(next: RewriteRule): RewriteRule {
        const first = this;
        return new (class extends RewriteRuleImpl {
            constructor() {
                // Pass empty patterns and a function that will never be called
                // since we override tryOn
                super([], () => undefined as unknown as Template);
            }

            async tryOn(cursor: Cursor, node: J): Promise<J | undefined> {
                const firstResult = await first.tryOn(cursor, node);
                if (firstResult !== undefined) {
                    const secondResult = await next.tryOn(cursor, firstResult);
                    return secondResult ?? firstResult;
                }
                return undefined;
            }
        })();
    }

    orElse(alternative: RewriteRule): RewriteRule {
        const first = this;
        return new (class extends RewriteRuleImpl {
            constructor() {
                // Pass empty patterns and a function that will never be called
                // since we override tryOn
                super([], () => undefined as unknown as Template);
            }

            async tryOn(cursor: Cursor, node: J): Promise<J | undefined> {
                const firstResult = await first.tryOn(cursor, node);
                if (firstResult !== undefined) {
                    return firstResult;
                }
                return await alternative.tryOn(cursor, node);
            }
        })();
    }
}

/**
 * Creates a replacement rule using a capture context and configuration.
 *
 * @param builderFn Function that takes a capture context and returns before/after configuration
 * @returns A replacement rule that can be applied to AST nodes
 *
 * @example
 * // Single pattern
 * const swapOperands = rewrite(() => {
 *     const { left, right } = { left: capture(), right: capture() };
 *     return {
 *         before: pattern`${left} + ${right}`,
 *         after: template`${right} + ${left}`
 *     };
 * });
 *
 * @example
 * // Multiple patterns
 * const normalizeComparisons = rewrite(() => {
 *     const { left, right } = { left: capture(), right: capture() };
 *     return {
 *         before: [
 *             pattern`${left} == ${right}`,
 *             pattern`${left} === ${right}`
 *         ],
 *         after: template`${left} === ${right}`
 *     };
 * });
 *
 * @example
 * // Using in a visitor - IMPORTANT: use `|| node` to handle undefined when no match
 * class MyVisitor extends JavaScriptVisitor<any> {
 *     override async visitBinary(binary: J.Binary, p: any): Promise<J | undefined> {
 *         const rule = rewrite(() => ({
 *             before: pattern`${capture('a')} + ${capture('b')}`,
 *             after: template`${capture('b')} + ${capture('a')}`
 *         }));
 *         // tryOn() returns undefined if no pattern matches, so always use || node
 *         return await rule.tryOn(this.cursor, binary) || binary;
 *     }
 * }
 */
export function rewrite(
    builderFn: () => RewriteConfig
): RewriteRule {
    const config = builderFn();

    // Ensure we have valid before and after properties
    if (!config.before || !config.after) {
        throw new Error('Builder function must return an object with before and after properties');
    }

    return new RewriteRuleImpl(
        Array.isArray(config.before) ? config.before : [config.before],
        config.after,
        config.where,
        config.whereNot
    );
}

/**
 * Creates a RewriteRule from a Recipe by using its editor visitor.
 *
 * This allows recipes to be used in the same chaining pattern as other rewrite rules,
 * enabling composition with `andThen()`.
 *
 * @param recipe The recipe whose editor will be used to transform nodes
 * @param ctx The execution context to pass to the recipe's editor
 * @returns A RewriteRule that applies the recipe's editor to nodes
 *
 * @example
 * ```typescript
 * class MyRecipe extends Recipe {
 *     name = "my.recipe";
 *     displayName = "My Recipe";
 *     description = "Transforms code.";
 *
 *     async editor(): Promise<TreeVisitor<any, ExecutionContext>> {
 *         return new MyVisitor();
 *     }
 * }
 *
 * // In a visitor:
 * override async visitBinary(binary: J.Binary, p: ExecutionContext): Promise<J | undefined> {
 *     const rule1 = rewrite(() => ({
 *         before: pattern`${capture('a')} + ${capture('b')}`,
 *         after: template`${capture('b')} + ${capture('a')}`
 *     }));
 *
 *     const rule2 = fromRecipe(new MyRecipe(), p);
 *
 *     // Chain the pattern-based rule with the recipe
 *     const combined = rule1.andThen(rule2);
 *     return await combined.tryOn(this.cursor, binary) || binary;
 * }
 * ```
 */
export const fromRecipe = (recipe: Recipe, ctx: ExecutionContext): RewriteRule => {
    return new (class extends RewriteRuleImpl {
        constructor() {
            // Pass empty patterns and a function that will never be called
            // since we override tryOn
            super([], () => undefined as unknown as Template);
        }

        async tryOn(cursor: Cursor, tree: J): Promise<J | undefined> {
            const visitor = await recipe.editor();
            const result = await visitor.visit<J>(tree, ctx, cursor);

            // Return undefined if the visitor didn't change the node
            return result !== tree ? result : undefined;
        }
    })();
}
