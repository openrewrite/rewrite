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
import {fromVisitor, RecipeSpec} from "../../../src/test";
import {typescript} from "../../../src/javascript";
import {UsesType} from "../../../src/javascript/search";

describe('UsesType visitor', () => {
    test('should find exact type match', async () => {
        const spec = new RecipeSpec();
        spec.recipe = fromVisitor(new UsesType("Array"));

        //language=typescript
        await spec.rewriteRun(
            typescript(
                `const arr = [1, 2]`,
                `const /*~~>*/arr = /*~~>*/[1, 2]`
            )
        );
    });

    test('should match with glob pattern', async () => {
        const spec = new RecipeSpec();
        spec.recipe = fromVisitor(new UsesType("*romise"));

        //language=typescript
        await spec.rewriteRun(
            typescript(
                `const p = Promise.resolve("data")`,
                `const /*~~>*/p = /*~~>*/Promise.resolve("data")`
            )
        );
    });
});
