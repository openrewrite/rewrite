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

import {ExecutionContext, randomId} from "../../src";
import {Expression, J, Statement} from "../../src/java";
import {fromVisitor, RecipeSpec} from "../../src/test";
import {JavaScriptParser, JavaScriptVisitor, JS, typescript} from "../../src/javascript";

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
        expect(global).toEqual("/org.openrewrite.java.tree.J$ClassDeclaration/org.openrewrite.java.tree.J$Block/org.openrewrite.java.tree.J$Empty/org.openrewrite.java.tree.J$MethodInvocation");
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

    test('visit the operands of the binary visitExpression returns under it', async () => {
        // given
        const cu = await new JavaScriptParser().parseOne({text: 'const x = 3 + 4;', sourcePath: 'binary.ts'});
        let replacement: J.Binary | undefined;
        const parents: J[] = [];
        const CustomVisitor = class extends JavaScriptVisitor<undefined> {
            protected async visitExpression(expression: Expression): Promise<J | undefined> {
                if (expression.kind === J.Kind.Binary) {
                    replacement ??= {...expression as J.Binary, id: randomId()};
                    return replacement;
                }
                return expression;
            }

            protected async visitLiteral(literal: J.Literal): Promise<J | undefined> {
                parents.push(this.cursor.parentTree()!.value);
                return literal;
            }
        }

        // when
        await new CustomVisitor().visit(cu, undefined);

        // test
        expect(parents.map(parent => parent.id)).toEqual([replacement!.id, replacement!.id]);
    });

    test('visit the children of any tree visitExpression or visitStatement returns under it', async () => {
        // given
        const cu = await new JavaScriptParser().parseOne({
            text: 'const x = !a ? b === c : -d;\nif (y) {\n    z(1);\n} else {\n    w = await v;\n}',
            sourcePath: 'children.ts'
        });
        const given = new Set<J>();
        const returned = new Set<J>();
        const misplaced: string[] = [];
        const CustomVisitor = class extends JavaScriptVisitor<undefined> {
            protected async visitExpression(expression: Expression): Promise<J | undefined> {
                given.add(expression);
                const copy = {...expression, id: randomId()};
                returned.add(copy);
                return copy;
            }

            protected async visitStatement(statement: Statement): Promise<J | undefined> {
                given.add(statement);
                const copy = {...statement, id: randomId()};
                returned.add(copy);
                return copy;
            }

            protected async preVisit(tree: J): Promise<J | undefined> {
                const parent = this.cursor.parentTree()?.value;
                if (given.has(parent)) {
                    misplaced.push(`${tree.kind} visited under the ${parent.kind} given`);
                }
                return tree;
            }

            protected async postVisit(tree: J): Promise<J | undefined> {
                if (returned.has(this.cursor.value)) {
                    misplaced.push(`${tree.kind} left the cursor on the tree returned`);
                }
                return tree;
            }
        }

        // when
        await new CustomVisitor().visit(cu, undefined);

        // test
        expect(misplaced).toEqual([]);
    });

    test('restore the cursor of the given binary once the operands are visited', async () => {
        // given
        const cu = await new JavaScriptParser().parseOne({text: 'const x = 3 + 4;', sourcePath: 'binary.ts'});
        const after: [J, string][] = [];
        const CustomVisitor = class extends JavaScriptVisitor<undefined> {
            protected async visitExpression(expression: Expression): Promise<J | undefined> {
                return expression.kind === J.Kind.Binary ? {...expression as J.Binary, id: randomId()} : expression;
            }

            protected async visitBinary(binary: J.Binary, p: undefined): Promise<J | undefined> {
                const visited = await super.visitBinary(binary, p);
                after.push([this.cursor.value, this.cursor.messages.get('operand')]);
                return visited;
            }

            protected async visitLiteral(literal: J.Literal): Promise<J | undefined> {
                this.cursor.parentTree()!.messages.set('operand', literal.valueSource);
                return literal;
            }
        }

        // when
        await new CustomVisitor().visit(cu, undefined);

        // test
        const declarations = (cu as JS.CompilationUnit).statements[0].element as J.VariableDeclarations;
        expect(after).toHaveLength(1);
        expect(after[0][0]).toBe(declarations.variables[0].element.initializer!.element);
        expect(after[0][1]).toBe('4');
    });
});
