// noinspection JSUnusedLocalSymbols

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
import {describe, test} from "@jest/globals";
import {fromVisitor, RecipeSpec} from "../../src/test";
import {typescript} from "../../src/javascript";
import {IsSourceFile} from "../../src/search";

describe('IsSourceFile visitor', () => {
    test('should match files based on glob pattern', async () => {
        const spec = new RecipeSpec();
        spec.recipe = fromVisitor(new IsSourceFile("src/**/*.tsx"));

        await spec.rewriteRun(
            // Should match - file matches the pattern
            {
                //language=typescript
                ...typescript(
                    `const a = 1`,
                    //@formatter:off
                    `/*~~>*/const a = 1`
                    //@formatter:on
                ),
                path: "src/components/deep/Component.tsx",
            },
            // Should not match - different extension
            {
                //language=typescript
                ...typescript(
                    `const a = 1`
                ),
                path: "src/utils/helper.ts"
            }
        );
    });
});
