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

import {Recipe} from "../recipe";
import {TreeVisitor} from "../visitor";
import {ExecutionContext} from "../execution";
import {JavaScriptVisitor, JS} from "../javascript";
import {emptySpace, J, space} from "../java";
import {randomId} from "../uuid";
import {emptyMarkers} from "../markers";
import {produce} from "immer";

export class PreferSatisfiesKeyword extends Recipe {
    name = "org.openrewrite.PreferSatisfiesKeyword";
    displayName = "Use `satisfies` keyword instead of `as`";
    description = "Use `satisfies` keyword instead of `as` when constructing a new object.";


    get editor(): TreeVisitor<any, ExecutionContext> {
        return new class extends JavaScriptVisitor<ExecutionContext> {

            protected async visitNewClass(newClass: J.NewClass, p: ExecutionContext): Promise<J | undefined> {
                const ret = await super.visitNewClass(newClass, p);
                const alreadyWrappedInSatisfies = this.cursor.parent?.value?.kind === JS.Kind.SatisfiesExpression;
                const kindProperty = newClass.body?.statements?.map(x => x.element)
                    .filter(x => x.kind == JS.Kind.PropertyAssignment)
                    .map(x => x as JS.PropertyAssignment)
                    .find(pa => pa.name.element.kind === J.Kind.Identifier && (pa.name.element as J.Identifier).simpleName === "kind");
                if (!alreadyWrappedInSatisfies && kindProperty && ((kindProperty.initializer as J.FieldAccess).target).kind == J.Kind.FieldAccess) {
                    const satisfiesClass = (((kindProperty.initializer as J.FieldAccess).target) as J.FieldAccess).name.element.simpleName;
                    const satisfiesTypeSimpleName = (kindProperty.initializer as J.FieldAccess).name.element.simpleName;
                    if (["LeftPadded", "RightPadded", "Container"].includes(satisfiesTypeSimpleName) || "Markers" === satisfiesClass) {
                        return ret;
                    }
                    return {
                        kind: JS.Kind.SatisfiesExpression,
                        id: randomId(),
                        prefix: emptySpace,
                        markers: emptyMarkers,
                        expression: newClass,
                        satisfiesType: {
                            kind: J.Kind.LeftPadded,
                            before: space(" "),
                            markers: emptyMarkers,
                            element: {
                                kind: J.Kind.FieldAccess,
                                id: randomId(),
                                prefix: space(" "),
                                markers: emptyMarkers,
                                target: produce(((kindProperty.initializer as J.FieldAccess).target as J.FieldAccess).target, draft => {
                                    draft.id = randomId()
                                    }),
                                name: produce((kindProperty.initializer as J.FieldAccess).name, draft => {
                                    draft.element.id = randomId()
                                })
                                } satisfies J.FieldAccess as J.FieldAccess,
                            } satisfies J.LeftPadded<J.FieldAccess> as J.LeftPadded<J.FieldAccess>
                    } satisfies JS.SatisfiesExpression as JS.SatisfiesExpression;
                } else {
                    return ret;
                }
            }
        }
    }
}
