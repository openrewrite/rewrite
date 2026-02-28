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

import {Recipe} from "../../recipe";
import {TreeVisitor} from "../../visitor";
import {ExecutionContext} from "../../execution";
import {JavaScriptVisitor} from "../visitor";
import {J, singleSpace} from "../../java";
import {emptyMarkers} from "../../markers";
import {randomId} from "../../uuid";

/**
 * Adds the radix parameter to parseInt() calls when missing.
 *
 * The radix parameter specifies the base of the numeral system to use.
 * Without it, parseInt can produce unexpected results:
 * - parseInt("08") was 0 in older JavaScript (octal interpretation)
 * - Modern JavaScript defaults to base 10, but explicit radix is clearer
 *
 * Examples:
 * - `parseInt(x)` becomes `parseInt(x, 10)`
 * - `parseInt(str)` becomes `parseInt(str, 10)`
 */
export class AddParseIntRadix extends Recipe {
    name = "org.openrewrite.javascript.cleanup.add-parse-int-radix";
    displayName = "Add radix to `parseInt`";
    description = "Adds the radix parameter (base 10) to `parseInt()` calls that are missing it, preventing potential parsing issues.";

    async editor(): Promise<TreeVisitor<any, ExecutionContext>> {
        return new class extends JavaScriptVisitor<ExecutionContext> {

            protected async visitMethodInvocation(method: J.MethodInvocation, ctx: ExecutionContext): Promise<J | undefined> {
                const visited = await super.visitMethodInvocation(method, ctx) as J.MethodInvocation;

                // Check if this is a call to parseInt
                if (visited.select) {
                    // This is a method call on an object (e.g., obj.parseInt())
                    // parseInt is typically called as a global function, not a method
                    return visited;
                }

                // Check the method name
                if (visited.name.simpleName !== 'parseInt') {
                    return visited;
                }

                // Check if there's exactly one argument (no radix)
                const args = visited.arguments;
                if (!args || args.elements.length !== 1) {
                    return visited;
                }

                // Add the radix parameter (10)
                const existingArg = args.elements[0];
                const radixLiteral: J.Literal = {
                    kind: J.Kind.Literal,
                    id: randomId(),
                    prefix: singleSpace,
                    markers: emptyMarkers,
                    value: 10,
                    valueSource: '10',
                    unicodeEscapes: undefined,
                    type: undefined
                };

                return {
                    ...visited,
                    arguments: {
                        ...args,
                        elements: [
                            {
                                ...existingArg,
                                padding: {
                                    ...existingArg.padding,
                                    after: {
                                        ...existingArg.padding.after,
                                        whitespace: ''
                                    }
                                }
                            },
                            {
                                ...radixLiteral,
                                padding: existingArg.padding
                            }
                        ]
                    }
                } as J.MethodInvocation;
            }
        };
    }
}
