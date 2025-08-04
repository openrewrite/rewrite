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
import {RecipeSpec} from "../../../src/test";
import {typescript} from "../../../src/javascript";

describe('throw mapping', () => {
    const spec = new RecipeSpec();

    test('simple throw', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
               throw new Error("Cannot divide by zero!");
           `)
        ));

    test('simple throw with comments', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
               /*a*/ throw /*b*/ new /*c*/ Error/*d*/(/*e*/'Cannot divide by zero!'/*f*/)/*g*/;
           `)
        ));

    test('re-throwing', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
                function riskyOperation() {
                    try {
                        throw new Error("An error occurred during risky operation.");
                    } catch (error) {
                        console.error("Logging Error:", (error as Error).message);
                        throw error;  // Re-throw the error to be handled at a higher level
                    }
                }
            `)
        ));
});
