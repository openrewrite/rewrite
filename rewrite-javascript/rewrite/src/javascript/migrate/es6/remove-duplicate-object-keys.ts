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

import {Recipe} from "../../../recipe";
import {TreeVisitor} from "../../../visitor";
import {ExecutionContext} from "../../../execution";
import {JavaScriptVisitor} from "../../visitor";
import {J} from "../../../java";
import {JS} from "../../tree";
import {produce} from "immer";
import {applyRemovedElementPrefix} from "../../../java/formatting-utils";

export class RemoveDuplicateObjectKeys extends Recipe {
    name = "org.openrewrite.javascript.migrate.es6.remove-duplicate-object-keys";
    displayName = "Remove duplicate object keys";
    description = "Remove duplicate keys in object literals, keeping only the last occurrence (last-wins semantics).";

    async editor(): Promise<TreeVisitor<any, ExecutionContext>> {
        return new class extends JavaScriptVisitor<ExecutionContext> {

            protected async visitNewClass(newClass: J.NewClass, ctx: ExecutionContext): Promise<J | undefined> {
                newClass = await super.visitNewClass(newClass, ctx) as J.NewClass;

                // Only process object literals (NewClass with body but no class or arguments)
                if (!newClass.body || newClass.class || (newClass.arguments?.elements && newClass.arguments.elements.length > 0)) {
                    return newClass;
                }

                const statements = newClass.body.statements;
                if (!statements || statements.length === 0) {
                    return newClass;
                }

                // Build a map of property names to their last occurrence index
                const propertyNameToLastIndex = new Map<string, number>();
                const propertyNames: (string | null)[] = [];

                for (let i = 0; i < statements.length; i++) {
                    const stmt = statements[i];
                    if (stmt.element.kind === JS.Kind.PropertyAssignment) {
                        const prop = stmt.element as JS.PropertyAssignment;
                        const propName = this.getPropertyName(prop);
                        propertyNames.push(propName);

                        if (propName !== null) {
                            propertyNameToLastIndex.set(propName, i);
                        }
                    } else {
                        propertyNames.push(null);
                    }
                }

                // Check if there are any duplicates to remove
                const indicesToRemove = new Set<number>();
                for (let i = 0; i < propertyNames.length; i++) {
                    const propName = propertyNames[i];
                    if (propName !== null) {
                        const lastIndex = propertyNameToLastIndex.get(propName)!;
                        if (i < lastIndex) {
                            indicesToRemove.add(i);
                        }
                    }
                }

                if (indicesToRemove.size === 0) {
                    return newClass;
                }

                // Remove duplicate properties and adjust prefixes
                return produce(newClass, draft => {
                    const filteredStatements: typeof statements = [];
                    let removedElement: J | undefined = undefined;

                    for (let i = 0; i < statements.length; i++) {
                        if (indicesToRemove.has(i)) {
                            // Track the first removed element
                            if (!removedElement && statements[i].element) {
                                removedElement = statements[i].element;
                            }
                            continue;
                        }

                        const stmt = statements[i];

                        // If we removed previous elements, apply the removed element's prefix to this one
                        if (removedElement) {
                            const adjustedElement = applyRemovedElementPrefix(removedElement, stmt.element);
                            filteredStatements.push({
                                ...stmt,
                                element: adjustedElement
                            });
                            removedElement = undefined;
                        } else {
                            filteredStatements.push(stmt);
                        }
                    }

                    draft.body!.statements = filteredStatements;
                });
            }

            private getPropertyName(prop: JS.PropertyAssignment): string | null {
                const name = prop.name.element;

                // Handle identifier: { foo: 1 }
                if (name.kind === J.Kind.Identifier) {
                    return (name as J.Identifier).simpleName;
                }

                // Handle string literal: { "foo": 1 }
                if (name.kind === J.Kind.Literal) {
                    const literal = name as J.Literal;
                    if (typeof literal.value === 'string') {
                        return literal.value;
                    }
                }

                // For computed properties { [expr]: 1 }, we can't statically determine the name
                // So we return null and don't consider them for deduplication
                return null;
            }
        }
    }
}
