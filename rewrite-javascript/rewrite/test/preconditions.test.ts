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
import {RecipeSpec} from "../src/test";
import {javascript} from "../src/javascript";
import {FindIdentifierWithPathPrecondition, ConditionalFindIdentifier} from "../fixtures/path-precondition";

describe('Preconditions', () => {
    test('visitor precondition - should only mark identifiers in matching path', async () => {
        const spec = new RecipeSpec();
        // @ts-expect-error Fixtures import from dist while types are from src, causing expected type mismatch
        spec.recipe = new FindIdentifierWithPathPrecondition({
            requiredPath: 'test.js',
            identifier: 'foo'
        });

        await spec.rewriteRun(
            {
                ...javascript('const foo = 1;', 'const /*~~>*/foo = 1;'),
                path: 'test.js'
            },
            {
                ...javascript('const foo = 2;'),
                path: 'other.js'
            }
        );
    });

    test('boolean precondition - should mark when true', async () => {
        const spec = new RecipeSpec();
        // @ts-expect-error Fixtures import from dist while types are from src, causing expected type mismatch
        spec.recipe = new ConditionalFindIdentifier({
            shouldSearch: true,
            identifier: 'bar'
        });

        await spec.rewriteRun(
            javascript('const bar = 1;', 'const /*~~>*/bar = 1;')
        );
    });

    test('boolean precondition - should not mark when false', async () => {
        const spec = new RecipeSpec();
        // @ts-expect-error Fixtures import from dist while types are from src, causing expected type mismatch
        spec.recipe = new ConditionalFindIdentifier({
            shouldSearch: false,
            identifier: 'bar'
        });

        await spec.rewriteRun(
            javascript('const bar = 1;')
        );
    });
});
