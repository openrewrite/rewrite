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
 * Simplifies object properties where the key and value have the same name,
 * in both destructuring patterns and object literals.
 *
 * Destructuring examples:
 * - `const { x: x } = obj` becomes `const { x } = obj`
 * - `function({ ref: ref, ...props })` becomes `function({ ref, ...props })`
 *
 * Object literal examples:
 * - `{ x: x }` becomes `{ x }`
 * - `{ foo: foo, bar: bar }` becomes `{ foo, bar }`
 */
export class UseObjectPropertyShorthand extends Recipe {
    name = "org.openrewrite.javascript.cleanup.use-object-property-shorthand";
    displayName = "Use object property shorthand";
    description = "Simplifies object properties where the property name and value/variable name are the same (e.g., `{ x: x }` becomes `{ x }`). Applies to both destructuring patterns and object literals.";

    async editor(): Promise<TreeVisitor<any, ExecutionContext>> {
        return new class extends JavaScriptVisitor<ExecutionContext> {

            /**
             * Handle object binding patterns (destructuring): const { x: x } = obj
             */
            protected async visitObjectBindingPattern(pattern: JS.ObjectBindingPattern, p: ExecutionContext): Promise<J | undefined> {
                const visited = await super.visitObjectBindingPattern(pattern, p) as JS.ObjectBindingPattern;

                let hasChanges = false;

                const simplifiedBindings = visited.bindings.elements.map(right => {
                    const element = right.element;

                    if (element.kind === JS.Kind.BindingElement) {
                        const binding = element as JS.BindingElement;

                        if (binding.propertyName?.element.kind === J.Kind.Identifier) {
                            const propName = (binding.propertyName.element as J.Identifier).simpleName;

                            if (binding.name?.kind === J.Kind.Identifier) {
                                const bindingName = (binding.name as J.Identifier).simpleName;

                                if (propName === bindingName) {
                                    hasChanges = true;
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

                return produce(visited, draft => {
                    draft.bindings.elements = simplifiedBindings;
                });
            }

            /**
             * Handle object literals: { x: x }
             * Object literals are represented as J.NewClass with a body containing JS.PropertyAssignment statements.
             */
            protected async visitNewClass(newClass: J.NewClass, ctx: ExecutionContext): Promise<J | undefined> {
                const visited = await super.visitNewClass(newClass, ctx) as J.NewClass;

                // Only process object literals (NewClass with body but no class or arguments)
                if (!visited.body || visited.class || (visited.arguments?.elements && visited.arguments.elements.length > 0)) {
                    return visited;
                }

                const statements = visited.body.statements;
                if (!statements || statements.length === 0) {
                    return visited;
                }

                let hasChanges = false;

                const simplifiedStatements = statements.map(stmt => {
                    if (stmt.element.kind === JS.Kind.PropertyAssignment) {
                        const prop = stmt.element as JS.PropertyAssignment;

                        // Check if the property name is an identifier
                        if (prop.name.element.kind === J.Kind.Identifier) {
                            const propName = (prop.name.element as J.Identifier).simpleName;

                            // Check if the initializer is also an identifier with the same name
                            if (prop.initializer?.kind === J.Kind.Identifier) {
                                const initName = (prop.initializer as J.Identifier).simpleName;

                                if (propName === initName) {
                                    hasChanges = true;
                                    // Remove the initializer to use shorthand syntax
                                    return {
                                        ...stmt,
                                        element: {
                                            ...prop,
                                            initializer: undefined
                                        }
                                    } as J.RightPadded<JS.PropertyAssignment>;
                                }
                            }
                        }
                    }

                    return stmt;
                });

                if (!hasChanges) {
                    return visited;
                }

                return produce(visited, draft => {
                    draft.body!.statements = simplifiedStatements;
                });
            }
        };
    }
}
