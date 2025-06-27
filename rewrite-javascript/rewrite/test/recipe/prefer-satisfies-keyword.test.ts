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
import {describe} from "@jest/globals";
import {RecipeSpec} from "../../src/test";
import {typescript} from "../../src/javascript";
import {PreferSatisfiesKeyword} from "../../src/recipe/";

describe("prefer-satisfies-keyword", () => {
    const spec = new RecipeSpec()
    spec.recipe = new PreferSatisfiesKeyword();

    test("basic", () => {
        return spec.rewriteRun(
            typescript(
                `
            const modifier = {
                    kind: J.Kind.Modifier,
                    id: randomId()
                };
            `,
                `
            const modifier = {
                    kind: J.Kind.Modifier,
                    id: randomId()
                } satisfies J.Modifier;
            `)
        )
    })

    test("already existing `as`", () => {
        return spec.rewriteRun(
            typescript(
            `
            const modifier = {
                    kind: J.Kind.Modifier,
                    id: randomId()
                } as J.Modifier;
            `,
            `
            const modifier = {
                    kind: J.Kind.Modifier,
                    id: randomId()
                } satisfies J.Modifier as J.Modifier;
            `)
        )
    })

    test("don't touch `as const`", () => {
        return spec.rewriteRun(
            typescript(
                `
            const arr = [1, 2, 3] as const;
            `)
        )
    })
});
