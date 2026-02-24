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
import {Expression, J, Statement} from "../../java";
import {JS} from "../tree";
import {create as produce} from "mutative";
import {findMarker} from "../../markers";

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

                // For tree types, the padded value IS the element (intersection type)
                const simplifiedBindings = visited.bindings.elements.map(right => {
                    if (right.kind === JS.Kind.BindingElement) {
                        const binding = right as J as JS.BindingElement;
                        const propNameExpr = binding.propertyName as Expression | undefined;
                        if (propNameExpr?.kind === J.Kind.Identifier) {
                            const propName = (propNameExpr as J.Identifier).simpleName;

                            if (binding.name?.kind === J.Kind.Identifier) {
                                const bindingName = (binding.name as J.Identifier).simpleName;

                                if (propName === bindingName) {
                                    hasChanges = true;
                                    // Spread the binding properties, override propertyName and name
                                    return {
                                        ...right,
                                        propertyName: undefined,
                                        name: {
                                            ...binding.name,
                                            prefix: propNameExpr.prefix
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
                    if (stmt.kind === JS.Kind.PropertyAssignment) {
                        const prop = stmt as Statement as JS.PropertyAssignment;
                        const nameExpr = prop.name as Expression;
                        if (nameExpr.kind === J.Kind.Identifier) {
                            const propName = (nameExpr as J.Identifier).simpleName;

                            // Check if the initializer is also an identifier with the same name
                            if (prop.initializer?.kind === J.Kind.Identifier) {
                                const init = prop.initializer as J.Identifier;
                                const initName = init.simpleName;

                                // Skip if initializer has non-null assertion marker
                                if (findMarker(init, JS.Markers.NonNullAssertion)) {
                                    return stmt;
                                }

                                if (propName === initName) {
                                    hasChanges = true;
                                    // Remove the initializer to use shorthand syntax
                                    // Spread the property with padding, override initializer
                                    return {
                                        ...stmt,
                                        initializer: undefined
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
