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
import {Cursor} from '../..';
import {J} from '../../java';
import {RewriteRule, RewriteConfig} from './types';
import {Pattern} from './pattern';
import {Template} from '../templating';  // Will be from './template' once template.ts is created

/**
 * Implementation of a replacement rule.
 */
class RewriteRuleImpl implements RewriteRule {
    constructor(
        private readonly before: Pattern[],
        private readonly after: Template
    ) {
    }

    async tryOn(cursor: Cursor, node: J): Promise<J | undefined> {
        for (const pattern of this.before) {
            const match = await pattern.match(node);
            if (match) {
                const result = await this.after.apply(cursor, node, match);
                if (result) {
                    return result;
                }
            }
        }

        // Return undefined if no patterns match
        return undefined;
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
 * const swapOperands = rewrite(() => ({
 *     before: pattern`${"left"} + ${"right"}`,
 *     after: template`${"right"} + ${"left"}`
 * }));
 *
 * @example
 * // Multiple patterns
 * const normalizeComparisons = rewrite(() => ({
 *     before: [
 *         pattern`${"left"} == ${"right"}`,
 *         pattern`${"left"} === ${"right"}`
 *     ],
 *     after: template`${"left"} === ${"right"}`
 * }));
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

    return new RewriteRuleImpl(Array.isArray(config.before) ? config.before : [config.before], config.after);
}
