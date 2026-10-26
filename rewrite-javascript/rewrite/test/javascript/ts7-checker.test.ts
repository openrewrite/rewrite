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
import type {Node} from "typescript/ast";
import type {Checker, Symbol as Symbol7, Type} from "typescript/sync";
import {memoizing} from "../../src/javascript/ts7/checker";

describe("memoizing checker", () => {
    const symbol = {id: 7} as unknown as Symbol7;
    const location = {} as Node;
    const type = {id: 3} as unknown as Type;

    test("a prefetched type of a symbol at a location is answered from one batch", () => {
        let asked = 0;
        const batches: unknown[][] = [];
        const checker = {
            getTypeOfSymbolAtLocation: Object.assign(() => (asked++, type), {gen: () => ({})}),
        } as unknown as Checker;
        const memo = memoizing(checker, {batch: (...questions: unknown[]) => (batches.push(questions), questions.map(() => type))} as never);

        memo.prefetchTypesOfSymbols([[symbol, location]]);
        expect(memo.getTypeOfSymbolAtLocation(symbol, location)).toBe(type);
        expect(asked).toBe(0);

        memo.prefetchTypesOfSymbols([[symbol, location]]);
        expect(batches.length).toBe(1);
    });

    test("a type renders once, unless it is rendered in context", () => {
        let asked = 0;
        const checker = {typeToString: () => (asked++, "string")} as unknown as Checker;
        const memo = memoizing(checker, {} as never);

        memo.typeToString(type);
        memo.typeToString(type);
        expect(asked).toBe(1);

        memo.typeToString(type, location);
        expect(asked).toBe(2);
    });
});
