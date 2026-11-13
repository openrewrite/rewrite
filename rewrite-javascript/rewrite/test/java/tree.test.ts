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

import {randomId} from "../../src";
import {emptyMarkers} from "../../src/markers";
import {emptySpace, Expression, J, rightPadded} from "../../src/java";

describe('Expression.unwrap', () => {
    const a: J.Identifier = {
        kind: J.Kind.Identifier,
        id: randomId(),
        prefix: emptySpace,
        markers: emptyMarkers,
        annotations: [],
        simpleName: "a"
    };
    const parenthesize = (tree: Expression): J.Parentheses<Expression> => ({
        kind: J.Kind.Parentheses,
        id: randomId(),
        prefix: emptySpace,
        markers: emptyMarkers,
        tree: rightPadded(tree, emptySpace)
    });

    test('removes every layer of parentheses', () => {
        expect(Expression.unwrap(parenthesize(parenthesize(a)))).toBe(a);
        expect(Expression.unwrap(a)).toBe(a);
        expect(Expression.unwrap(undefined)).toBeUndefined();
    });
});
