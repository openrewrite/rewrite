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
import {J} from "../../java";
import {JS} from "../tree";
import {produce} from "immer";

/**
 * Simplifies object binding properties where the key and value have the same name.
 *
 * Examples:
 * - `{ x: x }` becomes `{ x }`
 * - `{ ref: ref, ...props }` becomes `{ ref, ...props }`
 * - `{ foo: foo, bar: bar }` becomes `{ foo, bar }`
 */
export class SimplifyObjectBindingProperties extends Recipe {
    name = "org.openrewrite.javascript.cleanup.simplify-object-binding-properties";
    displayName = "Simplify object binding properties";
    description = "Simplifies object destructuring patterns where the property name and variable name are the same (e.g., `{ x: x }` becomes `{ x }`).";

    async editor(): Promise<TreeVisitor<any, ExecutionContext>> {
        return new class extends JavaScriptVisitor<ExecutionContext> {
            protected async visitObjectBindingPattern(pattern: JS.ObjectBindingPattern, p: ExecutionContext): Promise<J | undefined> {
                // First, recursively visit children
                const visited = await super.visitObjectBindingPattern(pattern, p) as JS.ObjectBindingPattern;

                let hasChanges = false;

                // Simplify any properties where key === value
                const simplifiedBindings = visited.bindings.elements.map(right => {
                    const element = right.element;

                    // Check if this is a binding element (e.g., { x: x })
                    if (element.kind === JS.Kind.BindingElement) {
                        const binding = element as JS.BindingElement;

                        // Check if the property name is an identifier
                        if (binding.propertyName?.element.kind === J.Kind.Identifier) {
                            const propName = (binding.propertyName.element as J.Identifier).simpleName;

                            // Check if the binding name is also an identifier with the same name
                            if (binding.name?.kind === J.Kind.Identifier) {
                                const bindingName = (binding.name as J.Identifier).simpleName;

                                // If they match, simplify to shorthand syntax
                                if (propName === bindingName) {
                                    hasChanges = true;
                                    // Remove propertyName and transfer its prefix to the name
                                    return {
                                        ...right,
                                        element: {
                                            ...binding,
                                            propertyName: undefined,
                                            name: {
                                                ...binding.name,
                                                prefix: binding.propertyName.element.prefix
                                            }
                                        }
                                    } as J.RightPadded<JS.BindingElement>;
                                }
                            }
                        }
                    }

                    return right;
                });

                if (!hasChanges) {
                    return visited;
                }

                // Return the pattern with simplified bindings
                return produce(visited, draft => {
                    draft.bindings.elements = simplifiedBindings;
                });
            }
        };
    }
}
