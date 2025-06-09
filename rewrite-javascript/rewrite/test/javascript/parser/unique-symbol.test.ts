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

describe('unique symbol mapping', () => {
    test('basic', async () => {
        const spec = new RecipeSpec();
        //language=typescript
        const source = typescript('const favorite = Symbol("anwil");')
        source.afterRecipe = tree => {
            const varDecl = tree.statements[0].element as J.VariableDeclarations;
            const ident = varDecl.variables[0].element.name as J.Identifier;
            expect(ident.simpleName).toEqual("favorite");
            expect(ident.type!.kind).toEqual(JavaType.Kind.UniqueSymbol);
        }
        await spec.rewriteRun(source);
    })
});
