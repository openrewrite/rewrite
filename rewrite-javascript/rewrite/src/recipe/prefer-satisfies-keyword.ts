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
import {produceAsync, TreeVisitor} from "../visitor";
import {ExecutionContext} from "../execution";
import {JavaScriptVisitor, JS} from "../javascript";
import {emptySpace, J} from "../java";
import {Draft, produce} from "immer";
import {AutoformatVisitor} from "../javascript/format";
import {randomId} from "../uuid";
import {emptyMarkers} from "../markers";

export class PreferSatisfiesKeyword extends Recipe {
    name = "org.openrewrite.PreferSatisfiesKeyword";
    displayName = "Use `satisfies` keyword instead of `as`";
    description = "Use `satisfies` keyword instead of `as` when constructing a new object.";


    get editor(): TreeVisitor<any, ExecutionContext> {
        return new class extends JavaScriptVisitor<ExecutionContext> {
            protected async visitBinaryExtensions(jsBinary: JS.Binary, p: ExecutionContext): Promise<JS.Binary | JS.SatisfiesExpression | undefined> {
                if (jsBinary.operator.element === JS.Binary.Type.As) {
                    return {
                        kind: JS.Kind.SatisfiesExpression,
                        id: randomId(),
                        prefix: jsBinary.prefix,
                        markers: jsBinary.markers,
                        expression: jsBinary.left,
                        satisfiesType: {
                            kind: J.Kind.LeftPadded,
                            before: jsBinary.operator.before,
                            element: jsBinary.right,
                            markers: emptyMarkers
                        }
                    } satisfies JS.SatisfiesExpression; // <--- Satisfying, isn't it?
                } else {
                    return await super.visitBinaryExtensions(jsBinary, p) as JS.Binary | undefined;
                }
            }
        }
    }
}
