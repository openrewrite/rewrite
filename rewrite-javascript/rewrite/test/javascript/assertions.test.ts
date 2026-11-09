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
import {ExecutionContext} from "../../src";
import {Parser} from "../../src/parser";
import {npm, typescript} from "../../src/javascript";
import {withDir} from "tmp-promise";

/** The parser factory `npm(dir, ...)` gives a spec. Each call looks the parser up in the pool again. */
async function parserIn(dir: string): Promise<() => Parser> {
    for await (const spec of npm(dir, typescript("const a = 1"))) {
        return () => spec.parser(new ExecutionContext());
    }
    throw new Error("npm() yielded no spec");
}

describe("npm", () => {
    test("pools a directory's parser for as long as the directory exists", async () => {
        await withDir(async live => {
            const liveParser = await parserIn(live.path);
            const pooled = liveParser();
            let removedPooled: Parser | undefined;
            const removedParser = await withDir(async removed => {
                const parser = await parserIn(removed.path);
                removedPooled = parser();
                return parser;
            }, {unsafeCleanup: true});

            // The pool drops removed directories when another directory's parser is first requested.
            await withDir(async next => (await parserIn(next.path))(), {unsafeCleanup: true});

            expect(liveParser()).toBe(pooled);
            expect(removedParser()).not.toBe(removedPooled);
        }, {unsafeCleanup: true});
    });
});
