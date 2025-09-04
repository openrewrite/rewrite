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
import {Expression, J, Statement} from "../../src/java";
import {fromVisitor, RecipeSpec} from "../../src/test";
import {JavaScriptVisitor, typescript} from "../../src/javascript";

describe('visitor', () => {
    test('call visitStatement for subclasses', async () => {
        // given
        let global = "";
        const CustomVisitor = class extends JavaScriptVisitor<ExecutionContext> {
            protected async visitStatement(statement: Statement, p: ExecutionContext): Promise<J | undefined> {
                global = global + "/" + statement.kind;
                return await super.visitStatement(statement, p);
            }
        }
        const spec = new RecipeSpec();
        spec.recipe = fromVisitor(new CustomVisitor());

        // when
        await spec.rewriteRun(
            //language=typescript
            typescript('class A {};"a".includes("b")')
        );

        // test
        expect(global).toEqual("/org.openrewrite.java.tree.J$ClassDeclaration/org.openrewrite.java.tree.J$Empty/org.openrewrite.java.tree.J$MethodInvocation");
    });

    test('call visitExpression for subclasses', async () => {
        // given
        let visitCounter = 0;
        let visits = "";
        const CustomVisitor = class extends JavaScriptVisitor<ExecutionContext> {
            protected async visitExpression(expression: Expression, p: ExecutionContext): Promise<J | undefined> {
                visitCounter++;
                visits = visits + "/" + expression.kind;
                return await super.visitExpression(expression, p);
            }
        }
        const spec = new RecipeSpec();
        spec.recipe = fromVisitor(new CustomVisitor());

        // when
        await spec.rewriteRun(
            //language=typescript
            typescript('const x = 3 + 3;')
        );

        // test
        expect(visitCounter).toEqual(4);
        expect(visits).toEqual("/org.openrewrite.java.tree.J$Identifier/org.openrewrite.java.tree.J$Binary/org.openrewrite.java.tree.J$Literal/org.openrewrite.java.tree.J$Literal");
    });
});
