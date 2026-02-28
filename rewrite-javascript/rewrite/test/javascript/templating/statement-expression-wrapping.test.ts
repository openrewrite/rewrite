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
import {beforeAll, describe, expect, it} from '@jest/globals';
import {capture, JavaScriptParser, JavaScriptVisitor, JS, pattern, template, typescript} from '../../../src/javascript';
import {J} from '../../../src/java';
import {Cursor} from '../../../src';
import {fromVisitor, RecipeSpec} from '../../../src/test';

describe('Statement Expression Wrapping', () => {
    let parser: JavaScriptParser;

    beforeAll(() => {
        parser = new JavaScriptParser();
    });

    it('should wrap function declaration in StatementExpression when replacing an expression', async () => {
        // Parse: const c = x
        const sourceCode = 'const c = x';
        const gen = parser.parse({text: sourceCode, sourcePath: 'test.ts'});
        const cu = (await gen.next()).value as JS.CompilationUnit;
        const varDecl = cu.statements[0] as unknown as J.VariableDeclarations;
        const initializer = varDecl.variables[0].initializer as unknown as J.Identifier;

        // Create cursor with parent context
        const cuCursor = new Cursor(cu);
        const varDeclCursor = new Cursor(varDecl, cuCursor);
        const initializerCursor = new Cursor(initializer, varDeclCursor);

        // Pattern to match the 'x' identifier
        const pat = pattern`${capture('expr')}`;
        const match = await pat.match(initializer, initializerCursor);
        expect(match).toBeTruthy();

        // Template that returns a function declaration (which is a Statement but not an Expression)
        const tmpl = template`function foo() { return 42; }`;

        // Apply the template - it should wrap the function in a StatementExpression
        const result = await tmpl.apply(initializer, initializerCursor, {values: match});
        expect(result).toBeTruthy();

        // The result should be a StatementExpression wrapping the function
        expect(result!.kind).toBe(JS.Kind.StatementExpression);
        const statementExpr = result as JS.StatementExpression;
        expect(statementExpr.statement.kind).toBe(J.Kind.MethodDeclaration);
    });

    it('should not wrap when replacing a statement with a statement', async () => {
        // Parse a statement: x;
        const sourceCode = 'x;';
        const gen = parser.parse({text: sourceCode, sourcePath: 'test.ts'});
        const cu = (await gen.next()).value as JS.CompilationUnit;
        const exprStmt = cu.statements[0] as unknown as JS.ExpressionStatement;

        // Create a cursor with parent context
        // In a real visitor, this.cursor would already have the parent chain
        const cuCursor = new Cursor(cu);
        const stmtCursor = new Cursor(exprStmt, cuCursor);

        // Pattern to match the statement
        const pat = pattern`${capture('stmt')};`;
        const match = await pat.match(exprStmt, stmtCursor);
        expect(match).toBeTruthy();

        // Template that returns a function declaration
        const tmpl = template`function foo() { return 42; }`;

        // Apply the template - should NOT wrap since we're replacing a statement
        const result = await tmpl.apply(exprStmt, stmtCursor, {values: match});
        expect(result).toBeTruthy();

        // The result should be a MethodDeclaration, not wrapped
        expect(result!.kind).toBe(J.Kind.MethodDeclaration);
    });

    it('should not wrap when replacing expression with another expression', async () => {
        // Parse: const c = x
        const sourceCode = 'const c = x';
        const gen = parser.parse({text: sourceCode, sourcePath: 'test.ts'});
        const cu = (await gen.next()).value as JS.CompilationUnit;
        const varDecl = cu.statements[0] as unknown as J.VariableDeclarations;
        const initializer = varDecl.variables[0].initializer as unknown as J.Identifier;

        // Create cursor (no parent context needed for this test)
        const initializerCursor = new Cursor(initializer);

        // Pattern to match the 'x' identifier
        const pat = pattern`${capture('expr')}`;
        const match = await pat.match(initializer, initializerCursor);
        expect(match).toBeTruthy();

        // Template that returns an expression
        const tmpl = template`42`;

        // Apply the template - should NOT wrap since both are expressions
        const result = await tmpl.apply(initializer, initializerCursor, {values: match});
        expect(result).toBeTruthy();

        // The result should be a Literal, not wrapped
        expect(result!.kind).toBe(J.Kind.Literal);
    });

    it('should wrap function declaration when replacing MethodInvocation in expression context', async () => {
        // Parse: const c = foo()
        // MethodInvocation is both Statement and Expression, but here it's in expression context
        const sourceCode = 'const c = foo()';
        const gen = parser.parse({text: sourceCode, sourcePath: 'test.ts'});
        const cu = (await gen.next()).value as JS.CompilationUnit;
        const varDecl = cu.statements[0] as unknown as J.VariableDeclarations;
        const initializer = varDecl.variables[0].initializer as unknown as J.MethodInvocation;

        // Create cursor with parent context
        const cuCursor = new Cursor(cu);
        const varDeclCursor = new Cursor(varDecl, cuCursor);
        const initializerCursor = new Cursor(initializer, varDeclCursor);

        // Pattern to match the method invocation
        const pat = pattern`${capture('call')}`;
        const match = await pat.match(initializer, initializerCursor);
        expect(match).toBeTruthy();

        // Template that returns a function declaration (Statement, not Expression)
        const tmpl = template`function bar() { return 42; }`;

        // Apply the template - should wrap since we're in expression context
        const result = await tmpl.apply(initializer, initializerCursor, {values: match});
        expect(result).toBeTruthy();

        // The result should be wrapped in StatementExpression
        expect(result!.kind).toBe(JS.Kind.StatementExpression);
        const statementExpr = result as JS.StatementExpression;
        expect(statementExpr.statement.kind).toBe(J.Kind.MethodDeclaration);
    });

    it('should handle method invocation replacement in both expression and statement contexts of if statement', () => {
        // Parse: if (foo()) foo();
        // First foo() is in expression context (condition)
        // Second foo() is in statement context (body)
        // Transform both to bar() and verify no unwanted wrapping occurs

        const spec = new RecipeSpec();
        const pat = pattern`1`;
        const tmpl = template`bar()`;

        spec.recipe = fromVisitor(new class extends JavaScriptVisitor<any> {
            override async visitLiteral(literal: J.Literal, p: any): Promise<J | undefined> {
                const match = await pat.match(literal, this.cursor);
                if (match) {
                    return tmpl.apply(literal, this.cursor, {values: match});
                }
                return literal;
            }
        });

        return spec.rewriteRun({
            ...typescript('if (1) 1;', 'if (bar()) bar();'),
            afterRecipe: (cu: JS.CompilationUnit) => {
                const ifStmt = cu.statements[0] as unknown as J.If;

                // Verify condition (expression context) - should be plain MethodInvocation
                const condition = ifStmt.ifCondition.tree;
                expect(condition.kind).toBe(J.Kind.MethodInvocation);
                expect((condition as unknown as J.MethodInvocation).name.simpleName).toBe('bar');

                // Verify body (statement context) - should be plain MethodInvocation, not wrapped
                const body = ifStmt.thenPart;
                expect(body.kind).toBe(JS.Kind.ExpressionStatement);
                expect((body as unknown as JS.ExpressionStatement).expression.kind).toBe(J.Kind.MethodInvocation);
            }
        });
    });
});
