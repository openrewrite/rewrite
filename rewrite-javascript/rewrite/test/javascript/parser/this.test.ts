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
import {JS, typescript} from "../../../src/javascript";
import {J, JavaType} from "../../../src/java";

describe('this mapping', () => {
    const spec = new RecipeSpec();

    test('simple', () => spec.rewriteRun(
        typescript('this')
    ));

    test('this type mapping', () => spec.rewriteRun({
        ...typescript(
           `
            class A {
               m(): A { return this; }
            }
            class B {
               m(): B { return this; }
            }
            const aa = new A().m();
            const bb = new B().m();
            `),
            afterRecipe: (tree: JS.CompilationUnit) => {
                for (let i = 0; i < 2; i++) {
                    const methodInA = (tree.statements[i].element as J.ClassDeclaration).body.statements[0].element as J.MethodDeclaration;
                    const typeOfThisInA = ((methodInA.body?.statements![0].element as J.Return).expression! as J.Identifier).type! as JavaType.Class;
                    expect(typeOfThisInA.fullyQualifiedName).toEqual(i == 0 ? "A" : "B");
                }
            }
        })
    );
});
