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
import {Expression, J, Statement} from "../../../java";
import {JS} from "../../tree";
import {create as produce} from "mutative";
import {ElementRemovalFormatter} from "../../../java/formatting-utils";

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
                    if (stmt.kind === JS.Kind.PropertyAssignment) {
                        const prop = stmt as Statement as JS.PropertyAssignment;
                        const propName = this.getPropertyName(prop);
                        propertyNames.push(propName);

                        if (propName !== null) {
                            propertyNameToLastIndex.set(propName, i);
                        }
                    } else {
                        propertyNames.push(null);
                    }
                }

                // Remove duplicate properties and adjust prefixes
                return produce(newClass, draft => {
                    const filteredStatements: typeof statements = [];
                    const formatter = new ElementRemovalFormatter<J>();

                    for (let i = 0; i < statements.length; i++) {
                        const propName = propertyNames[i];

                        // Check if this is a duplicate that should be removed
                        if (propName !== null) {
                            const lastIndex = propertyNameToLastIndex.get(propName)!;
                            if (i < lastIndex) {
                                formatter.markRemoved(statements[i]);
                                continue;
                            }
                        }

                        const stmt = statements[i];
                        const adjustedElement = formatter.processKept(stmt);
                        // Merge adjusted element with padding
                        filteredStatements.push({
                            ...adjustedElement,
                            padding: stmt.padding
                        } as J.RightPadded<Statement>);
                    }

                    if (!formatter.hasRemovals) {
                        return; // No changes needed
                    }

                    draft.body!.statements = filteredStatements;
                });
            }

            private getPropertyName(prop: JS.PropertyAssignment): string | null {
                const name = prop.name as Expression;

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
