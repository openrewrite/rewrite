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

import {ExecutionContext} from "../../src";
import {J, JavaVisitor, Statement} from "../../src/java";
import {fromVisitor, RecipeSpec} from "../../src/test";
import {JavaScriptVisitor, typescript} from "../../src/javascript";

describe('visitor', () => {
    test('call visitStatement for subclasses', async () => {
        // given
        let global = "not visited yet";
        const CustomVisitor = class extends JavaScriptVisitor<ExecutionContext> {
            protected async visitStatement(statement: Statement, p: ExecutionContext): Promise<J | undefined> {
                global = "visited " + statement.kind;
                return await super.visitStatement(statement, p);
            }
        }
        const spec = new RecipeSpec();
        spec.recipe = fromVisitor(new CustomVisitor());

        // when
        await spec.rewriteRun(
            // TODO something is off with the rewriteRun logic, it doesn't work without the after, even if before==after
            //language=typescript
            typescript('class A {}', 'class A {}')
        );

        // test
        expect(global).toEqual("visited org.openrewrite.java.tree.J$ClassDeclaration");
    });
});
