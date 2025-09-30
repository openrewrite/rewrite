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
import {UsesMethod} from "../../../src/javascript/search";

describe('UsesMethod visitor', () => {
    test('should find exact method match', async () => {
        const spec = new RecipeSpec();
        spec.recipe = fromVisitor(new UsesMethod("Array push(..)"));

        //language=typescript
        await spec.rewriteRun(
            typescript(
                `
                    const arr = [1, 2];
                    arr.push(3);
                `,
                //@formatter:off
                `
                    const arr = [1, 2];
                    /*~~>*/arr.push(3);
                `
                //@formatter:on
            )
        );
    });

    test('should match with wildcard package pattern', async () => {
        const spec = new RecipeSpec();
        spec.recipe = fromVisitor(new UsesMethod("*.Array map(..)"));

        //language=typescript
        await spec.rewriteRun(
            typescript(
                `[1, 2].map(x => x * 2);`,
                //@formatter:off
                `/*~~>*/[1, 2].map(x => x * 2);`
                //@formatter:on
            )
        );
    });

    test('should match methods with any arguments', async () => {
        const spec = new RecipeSpec();
        spec.recipe = fromVisitor(new UsesMethod("Array push(..)"));

        //language=typescript
        await spec.rewriteRun(
            typescript(
                `
                    const arr = [1, 2];
                    arr.push(3);
                `,
                //@formatter:off
                `
                    const arr = [1, 2];
                    /*~~>*/arr.push(3);
                `
                //@formatter:on
            )
        );
    });
});
