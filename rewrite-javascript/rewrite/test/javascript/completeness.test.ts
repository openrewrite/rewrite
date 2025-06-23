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
import fs = require('fs');
import path = require('path');


describe('JS LST element tree', () => {
    test("should have a .java class counterpart for every JS LST element type", () => {
        const treeTSContent = fs.readFileSync(path.resolve(__dirname, '../../src/javascript/tree.ts'), 'utf8')
            .split("export namespace JSX")[0];
        const tsInterfaces = Array.from(treeTSContent.matchAll(/export interface (\w+) extends JS/gm), m => m[1]);
        tsInterfaces.sort()

        const javaContent = fs.readFileSync(path.resolve(__dirname, '../../../src/main/java/org/openrewrite/javascript/tree/JS.java'), 'utf8');
        const javaClasses = Array.from(javaContent.matchAll(/class (\w+) implements JS/gm), m => m[1]);
        javaClasses.sort()

        expect(tsInterfaces).toEqual(javaClasses);
    })
});
