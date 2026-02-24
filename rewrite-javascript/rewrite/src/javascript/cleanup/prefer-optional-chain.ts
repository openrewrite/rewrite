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
import {emptySpace, Expression, J} from "../../java";
import {JS} from "../tree";
import {findMarker, markers} from "../../markers";
import {randomId} from "../../uuid";
import {Optional} from "../markers";

/**
 * Converts ternary expressions that check for null/undefined into optional chaining.
 *
 * Examples:
 * - `foo ? foo.bar : undefined` becomes `foo?.bar`
 * - `foo ? foo.bar : null` becomes `foo?.bar ?? null`
 * - `obj ? obj.method() : undefined` becomes `obj?.method()`
 * - `arr ? arr[0] : undefined` becomes `arr?.[0]`
 */
export class PreferOptionalChain extends Recipe {
    name = "org.openrewrite.javascript.cleanup.prefer-optional-chain";
    displayName = "Prefer optional chaining";
    description = "Converts ternary expressions like `foo ? foo.bar : undefined` to use optional chaining syntax `foo?.bar`.";

    async editor(): Promise<TreeVisitor<any, ExecutionContext>> {
        return new class extends JavaScriptVisitor<ExecutionContext> {

            protected async visitTernary(ternary: J.Ternary, ctx: ExecutionContext): Promise<J | undefined> {
                const visited = await super.visitTernary(ternary, ctx) as J.Ternary;

                // Check if the condition is an identifier
                if (visited.condition.kind !== J.Kind.Identifier) {
                    return visited;
                }
                const conditionIdent = visited.condition as J.Identifier;
                const conditionName = conditionIdent.simpleName;

                // Check if the false part is undefined
                // Note: We only convert when the false part is undefined, not null,
                // because optional chaining returns undefined (not null) when the target is nullish.
                const falsePart = visited.falsePart as Expression;
                const isUndefinedFalse = falsePart.kind === J.Kind.Identifier &&
                    (falsePart as J.Identifier).simpleName === 'undefined';

                if (!isUndefinedFalse) {
                    return visited;
                }

                // Check if the true part accesses a property/method on the condition
                const truePart = visited.truePart as Expression;
                const result = this.extractOptionalChainTarget(truePart, conditionName);

                if (!result) {
                    return visited;
                }

                // Transform to optional chaining
                // The result already has the Optional marker added, just update prefix
                return {
                    ...result,
                    prefix: visited.prefix
                } as Expression;
            }

            /**
             * Extracts the target expression for optional chaining if the expression
             * accesses a property on the given identifier.
             *
             * Returns the expression with Optional marker added, or undefined if not applicable.
             */
            private extractOptionalChainTarget(expr: Expression, targetName: string): Expression | undefined {
                // Handle FieldAccess: foo.bar
                if (expr.kind === J.Kind.FieldAccess) {
                    const fieldAccess = expr as J.FieldAccess;
                    if (fieldAccess.target.kind === J.Kind.Identifier) {
                        const target = fieldAccess.target as J.Identifier;
                        if (target.simpleName === targetName) {
                            // Already has optional marker?
                            if (findMarker(target, JS.Markers.Optional)) {
                                return fieldAccess;
                            }
                            // Add Optional marker to the target (foo?.bar means marker on foo)
                            const optionalMarker: Optional = {
                                kind: JS.Markers.Optional,
                                id: randomId(),
                                prefix: emptySpace
                            };
                            return {
                                ...fieldAccess,
                                target: {
                                    ...target,
                                    markers: markers(
                                        ...target.markers.markers,
                                        optionalMarker
                                    )
                                }
                            } as J.FieldAccess;
                        }
                    }
                }

                // Handle MethodInvocation: foo.bar()
                if (expr.kind === J.Kind.MethodInvocation) {
                    const methodInvocation = expr as J.MethodInvocation;
                    const selectExpr = methodInvocation.select as Expression | undefined;
                    if (selectExpr?.kind === J.Kind.Identifier) {
                        const select = selectExpr as J.Identifier;
                        if (select.simpleName === targetName) {
                            // Already has optional marker?
                            if (findMarker(select, JS.Markers.Optional)) {
                                return methodInvocation;
                            }
                            // Add Optional marker to the select
                            const optionalMarker: Optional = {
                                kind: JS.Markers.Optional,
                                id: randomId(),
                                prefix: emptySpace
                            };
                            // With intersection types, select IS the element (with padding mixed in)
                            // Update markers on the element directly, preserving padding
                            return {
                                ...methodInvocation,
                                select: {
                                    ...select,
                                    markers: markers(
                                        ...select.markers.markers,
                                        optionalMarker
                                    ),
                                    padding: methodInvocation.select?.padding
                                }
                            } as J.MethodInvocation;
                        }
                    }
                }

                // Handle ArrayAccess: foo[0]
                if (expr.kind === J.Kind.ArrayAccess) {
                    const arrayAccess = expr as J.ArrayAccess;
                    if (arrayAccess.indexed.kind === J.Kind.Identifier) {
                        const indexed = arrayAccess.indexed as J.Identifier;
                        if (indexed.simpleName === targetName) {
                            // Already has optional marker?
                            if (findMarker(indexed, JS.Markers.Optional)) {
                                return arrayAccess;
                            }
                            // Add Optional marker to the indexed expression
                            const optionalMarker: Optional = {
                                kind: JS.Markers.Optional,
                                id: randomId(),
                                prefix: emptySpace
                            };
                            return {
                                ...arrayAccess,
                                indexed: {
                                    ...indexed,
                                    markers: markers(
                                        ...indexed.markers.markers,
                                        optionalMarker
                                    )
                                }
                            } as J.ArrayAccess;
                        }
                    }
                }

                return undefined;
            }
        };
    }
}
