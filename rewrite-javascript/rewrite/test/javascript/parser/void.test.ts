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
import {JS, typescript} from "../../../src/javascript";
import {J, JavaType} from "../../../src/java";

describe('void operator mapping', () => {
    const spec = new RecipeSpec();

    test('void', () => spec.rewriteRun({
        //language=typescript
        ...typescript('void 1'),
        afterRecipe: (cu: JS.CompilationUnit) => {
            const statement = cu.statements[0].element as JS.Void;
            expect(statement.kind).toBe(JS.Kind.Void);
            // FIXME we can't yet get the type for `JS.Void`
            // const type = statement.type as JavaType.Primitive;
            // expect(type.kind).toBe(JavaType.Primitive.Void);
        }
    }));
});
