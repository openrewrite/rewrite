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

describe('assignment mapping', () => {
    const spec = new RecipeSpec();

    test('simple', () => spec.rewriteRun(
        //language=typescript
        typescript('foo = 1')
    ));
    test('?? assignment', () => spec.rewriteRun(
        //language=typescript
        typescript(`
              const a = { duration: 50 };

              a.speed ??= 25;
              console.log(a.speed);
              // Expected output: 25

              a.duration ??= 10;
              console.log(a.duration);
              // Expected output: 50
          `)
    ));
    test('And assignment', () => spec.rewriteRun(
        //language=typescript
        typescript(`
              let a = 1;
              let b = 0;

              a &&= 2;
              console.log(a);
              // Expected output: 2

              b &&= 2;
              console.log(b);
              // Expected output: 0

          `)
    ));
    test('Or assignment', () => spec.rewriteRun(
        //language=typescript
        typescript(`
              const a = { duration: 50, title: '' };

              a.duration ||= 10;
              console.log(a.duration);
              // Expected output: 50

              a.title ||= 'title is empty.';
              console.log(a.title);
              // Expected output: "title is empty."

          `)
    ));
});
