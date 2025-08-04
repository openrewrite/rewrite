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

describe('intersection type mapping', () => {
    const spec = new RecipeSpec();

    test('simple', () =>
        spec.rewriteRun(
            //language=typescript
            typescript('let c: number/*1*/ &/*2*/undefined/*3*/&/*4*/null')
        ));
    test('literals', () =>
        spec.rewriteRun(
            //language=typescript
            typescript('let c: & true & 1 & "foo"')
        ));
    // noinspection TypeScriptUnresolvedReference
    test('union which starts with & ', () =>
        spec.rewriteRun(
            //language=typescript
            typescript(`
                export type GeolocateResponse =
                    & GeolocateResponseSuccess
                    & GeolocateResponseError;
            `)
        ));
});
