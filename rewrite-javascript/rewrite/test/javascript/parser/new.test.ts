/*
 * Copyright 2023 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import {RecipeSpec} from "../../../src/test";
import {typescript} from "../../../src/javascript";

describe('new mapping', () => {
    const spec = new RecipeSpec();

    test('simple', () => {
       return spec.rewriteRun(
          //language=typescript
          typescript('new Uint8Array(1)')
        );
    });

    test('space', () => {
       return spec.rewriteRun(
          //language=typescript
          typescript('new Uint8Array/*1*/(/*2*/1/*3*/)/*4*/')
        );
    });

    test('multiple', () => {
       return spec.rewriteRun(
          //language=typescript
          typescript('new Date(2023, 9, 25, 10, 30, 15, 500)')
        );
    });

    test('trailing comma', () => {
       return spec.rewriteRun(
          //language=typescript
          typescript('new Uint8Array(1 ,  )')
        );
    });
});
