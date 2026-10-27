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

    // A checker that counts the questions it answers one at a time, and a batch that drives each
    // question as the client does, settling its one request with `settle`.
    const counting = () => {
        const checker = {
            asked: 0,
            getTypeOfSymbolAtLocation: Object.assign(() => (checker.asked++, type), {
                gen: function* (): Generator<string, Type, Type> {
                    return yield "request";
                }
            }),
        };
        return checker;
    };
    const batching = (settle: (question: Generator) => unknown) => {
        const batches: number[] = [];
        const batch = (...questions: Generator[]) => {
            batches.push(questions.length);
            return questions.map(question => {
                question.next();
                return settle(question);
            });
        };
        return {batches, api: {batch} as never};
    };

    test("an asked question is answered from its batch, and is asked once", () => {
        const checker = counting();
        const {batches, api} = batching(question => question.next(type).value);
        const memo = memoizing(checker as unknown as Checker, api);

        memo.ask([["getTypeOfSymbolAtLocation", symbol, location], ["getTypeOfSymbolAtLocation", symbol, location]]);
        expect(memo.getTypeOfSymbolAtLocation(symbol, location)).toBe(type);
        expect(checker.asked).toBe(0);

        memo.ask([["getTypeOfSymbolAtLocation", symbol, location]]);
        expect(batches).toEqual([1]);
    });

    test("a question that fails in a batch is asked again on its own", () => {
        const checker = counting();
        const {api} = batching(question => question.throw(new Error("no answer")).value);
        const memo = memoizing(checker as unknown as Checker, api);

        memo.ask([["getTypeOfSymbolAtLocation", symbol, location]]);
        expect(memo.getTypeOfSymbolAtLocation(symbol, location)).toBe(type);
        expect(checker.asked).toBe(1);
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
