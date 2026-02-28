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
import {RecipeSpec, fromVisitor} from "../../../src/test";
import {typescript, JS, JavaScriptVisitor} from "../../../src/javascript";
import {J} from "../../../src/java";
import {ExecutionContext} from "../../../src";

describe('new mapping', () => {
    const spec = new RecipeSpec();

    test('simple', () =>
        spec.rewriteRun(
            //language=typescript
            typescript('new Uint8Array(1)')
        ));

    test('space', () =>
        spec.rewriteRun(
            //language=typescript
            typescript('new Uint8Array/*1*/(/*2*/1/*3*/)/*4*/')
        ));

    test('multiple', () =>
        spec.rewriteRun(
            //language=typescript
            typescript('new Date(2023, 9, 25, 10, 30, 15, 500)')
        ));

    test('trailing comma', () =>
        spec.rewriteRun(
            //language=typescript
            typescript('new Uint8Array(1 ,  )')
        ));

    test('simple identifier should not be wrapped in TypeTreeExpression', async () => {
        class CheckIdentifierNotWrappedVisitor extends JavaScriptVisitor<ExecutionContext> {
            protected override async visitNewClass(newClass: J.NewClass, ctx: ExecutionContext): Promise<J | undefined> {
                // Check that simple identifiers are not wrapped in JS.TypeTreeExpression
                if (newClass.class?.kind === JS.Kind.TypeTreeExpression) {
                    const typeTreeExpr = newClass.class as JS.TypeTreeExpression;
                    if (typeTreeExpr.expression.kind === J.Kind.Identifier) {
                        throw new Error(
                            `J.Identifier should be used directly as the class reference, ` +
                            `not wrapped in JS.TypeTreeExpression since J.Identifier implements both Expression and TypeTree`
                        );
                    }
                } else if (newClass.class?.kind === J.Kind.Identifier) {
                    // This is the expected behavior - verify the identifier name
                    const identifier = newClass.class as J.Identifier;
                    expect(identifier.simpleName).toBe('ApolloServer');
                }
                return super.visitNewClass(newClass, ctx);
            }
        }

        spec.recipe = fromVisitor(new CheckIdentifierNotWrappedVisitor());

        await spec.rewriteRun(
            //language=typescript
            typescript('const server = new ApolloServer();')
        );
    });

    test('field access should not be wrapped in TypeTreeExpression', async () => {
        class CheckFieldAccessNotWrappedVisitor extends JavaScriptVisitor<ExecutionContext> {
            protected override async visitNewClass(newClass: J.NewClass, ctx: ExecutionContext): Promise<J | undefined> {
                // J.FieldAccess implements TypeTree, so it should not be wrapped
                if (newClass.class?.kind === JS.Kind.TypeTreeExpression) {
                    const typeTreeExpr = newClass.class as JS.TypeTreeExpression;
                    if (typeTreeExpr.expression.kind === J.Kind.FieldAccess) {
                        throw new Error(
                            `J.FieldAccess should be used directly as the class reference, ` +
                            `not wrapped in JS.TypeTreeExpression since J.FieldAccess implements TypeTree`
                        );
                    }
                } else if (newClass.class?.kind === J.Kind.FieldAccess) {
                    // This is the expected behavior - verify the field access
                    const fieldAccess = newClass.class as J.FieldAccess;
                    expect(fieldAccess.name.simpleName).toBe('MyClass');
                }
                return super.visitNewClass(newClass, ctx);
            }
        }

        spec.recipe = fromVisitor(new CheckFieldAccessNotWrappedVisitor());

        await spec.rewriteRun(
            //language=typescript
            typescript('const instance = new MyNamespace.MyClass();')
        );
    });

    test('parameterized type with identifier should not wrap base class', async () => {
        class CheckParameterizedTypeVisitor extends JavaScriptVisitor<ExecutionContext> {
            protected override async visitNewClass(newClass: J.NewClass, ctx: ExecutionContext): Promise<J | undefined> {
                // Check parameterized types
                if (newClass.class?.kind === J.Kind.ParameterizedType) {
                    const paramType = newClass.class as J.ParameterizedType;
                    // The base class in a parameterized type should not be wrapped if it's an identifier
                    if (paramType.class?.kind === JS.Kind.TypeTreeExpression) {
                        const typeTreeExpr = paramType.class as JS.TypeTreeExpression;
                        if (typeTreeExpr.expression.kind === J.Kind.Identifier) {
                            throw new Error(
                                `J.Identifier in ParameterizedType should be used directly, ` +
                                `not wrapped in JS.TypeTreeExpression`
                            );
                        }
                    } else if (paramType.class?.kind === J.Kind.Identifier) {
                        // This is the expected behavior
                        const identifier = paramType.class as J.Identifier;
                        expect(identifier.simpleName).toBe('Map');
                    }
                }
                return super.visitNewClass(newClass, ctx);
            }
        }

        spec.recipe = fromVisitor(new CheckParameterizedTypeVisitor());

        await spec.rewriteRun(
            //language=typescript
            typescript('const map = new Map<string, number>();')
        );
    });
});
