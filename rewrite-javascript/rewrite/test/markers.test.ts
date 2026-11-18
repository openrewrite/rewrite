/*
 * Copyright 2026 the original author or authors.
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

import {ExecutionContext, randomId, Recipe} from "../src";
import {foundSearchResult, Marker, markers, MarkersKind, removeMarkerByKind} from "../src/markers";
import {fromVisitor, RecipeSpec} from "../src/test";
import {JavaScriptVisitor, typescript} from "../src/javascript";
import {J} from "../src/java";

describe('removeMarkerByKind', () => {
    const searchResult = (): Marker => ({kind: MarkersKind.SearchResult, id: randomId()});
    const rpcMarker: Marker = {kind: MarkersKind.RpcMarker, id: randomId()};

    test('removes every marker of the kind and keeps the rest', () => {
        const removed = removeMarkerByKind(markers(searchResult(), rpcMarker, searchResult()), MarkersKind.SearchResult);
        expect(removed.markers).toEqual([rpcMarker]);
    });

    test('returns the same instance when no marker has the kind', () => {
        const unchanged = markers(rpcMarker);
        expect(removeMarkerByKind(unchanged, MarkersKind.SearchResult)).toBe(unchanged);
    });
});

describe('foundSearchResult', () => {
    const markInvocations = (description: string) => fromVisitor(new class extends JavaScriptVisitor<ExecutionContext> {
        protected async visitMethodInvocation(method: J.MethodInvocation, p: ExecutionContext): Promise<J | undefined> {
            return foundSearchResult(await super.visitMethodInvocation(method, p) as J.MethodInvocation, description);
        }
    });

    test('adds no second marker for a description when another recipe marked the node first', () => {
        const spec = new RecipeSpec();
        spec.recipe = new class extends Recipe {
            name = "org.openrewrite.test.two-searches";
            displayName = "Two searches";
            description = "Marks each method invocation with two descriptions.";

            async recipeList(): Promise<Recipe[]> {
                return [markInvocations("first"), markInvocations("second")];
            }
        };
        return spec.rewriteRun(typescript(`foo()`, `/*~~(first)~~>*//*~~(second)~~>*/foo()`));
    });
});
