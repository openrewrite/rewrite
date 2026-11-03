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
import {capture, javascript, JavaScriptParser, JavaScriptVisitor, JS, pattern, rewrite, template, typescript, sourceFileCache} from '../../../src/javascript';
import {J} from '../../../src/java';
import {Cursor} from '../../../src';
import {foundSearchResult} from '../../../src/markers';
import {fromVisitor, RecipeSpec} from '../../../src/test';

describe('Statement Expression Wrapping', () => {
    let parser: JavaScriptParser;

    beforeAll(() => {
        parser = new JavaScriptParser({sourceFileCache});
    });

    it('should wrap function declaration in StatementExpression when replacing an expression', async () => {
        // Parse: const c = x
        const sourceCode = 'const c = x';
        const gen = parser.parse({text: sourceCode, sourcePath: 'test.ts'});
        const cu = (await gen.next()).value as JS.CompilationUnit;
        const varDecl = cu.statements[0].element as J.VariableDeclarations;
        const initializer = varDecl.variables[0].element.initializer?.element as J.Identifier;

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
        const exprStmt = cu.statements[0].element as JS.ExpressionStatement;

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
        const varDecl = cu.statements[0].element as J.VariableDeclarations;
        const initializer = varDecl.variables[0].element.initializer?.element as J.Identifier;

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
        const varDecl = cu.statements[0].element as J.VariableDeclarations;
        const initializer = varDecl.variables[0].element.initializer?.element as J.MethodInvocation;

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
                const ifStmt = cu.statements[0].element as J.If;

                // Verify condition (expression context) - should be plain MethodInvocation
                const condition = ifStmt.ifCondition.tree.element;
                expect(condition.kind).toBe(J.Kind.MethodInvocation);
                expect((condition as J.MethodInvocation).name.simpleName).toBe('bar');

                // Verify body (statement context) - should be plain MethodInvocation, not wrapped
                const body = ifStmt.thenPart.element;
                expect(body.kind).toBe(J.Kind.MethodInvocation);
            }
        });
    });

    it('should wrap an expression the parser does not take as a statement when replacing a call statement', () => {
        const spec = new RecipeSpec();
        const arg = capture('arg');
        const pat = pattern`applyTheme(${arg})`;
        const tmpl = template`${arg} as unknown as string`;

        spec.recipe = fromVisitor(new class extends JavaScriptVisitor<any> {
            override async visitMethodInvocation(method: J.MethodInvocation, p: any): Promise<J | undefined> {
                const match = await pat.match(method, this.cursor);
                return match ? tmpl.apply(method, this.cursor, {values: match}) : method;
            }
        });

        return spec.rewriteRun({
            ...typescript('applyTheme(svc);', 'svc as unknown as string;'),
            afterRecipe: (cu: JS.CompilationUnit) => {
                const statement = cu.statements[0].element;
                expect(statement.kind).toBe(JS.Kind.ExpressionStatement);
                expect((statement as JS.ExpressionStatement).expression.kind).toBe(JS.Kind.As);
            }
        });
    });

    it('should wrap an expression that replaces the statement of a label', () => {
        const spec = new RecipeSpec();
        spec.recipe = fromVisitor(new class extends JavaScriptVisitor<any> {
            override async visitMethodInvocation(method: J.MethodInvocation, p: any): Promise<J | undefined> {
                return template`[1, 2]`.apply(method, this.cursor);
            }
        });

        return spec.rewriteRun(typescript('lbl: Array(1, 2);', 'lbl: [1, 2];'));
    });

    it('should wrap an expression that replaces a statement of a case, but not its label', () => {
        const spec = new RecipeSpec();
        const operand = capture('operand');
        spec.recipe = fromVisitor(new class extends JavaScriptVisitor<any> {
            override async visitUnary(unary: J.Unary, p: any): Promise<J | undefined> {
                const match = await pattern`!!${operand}`.match(unary, this.cursor);
                return match ? template`${operand}`.apply(unary, this.cursor, {values: match}) :
                    super.visitUnary(unary, p);
            }
        });

        return spec.rewriteRun(
            //language=typescript
            typescript(
                `
                switch (x) {
                    case !!y:
                        !!z;
                }`,
                `
                switch (x) {
                    case y:
                        z;
                }`
            )
        );
    });

    it('should wrap an expression that replaces the statement of a with statement', () => {
        const spec = new RecipeSpec();
        const operand = capture('operand');
        spec.recipe = fromVisitor(new class extends JavaScriptVisitor<any> {
            override async visitUnary(unary: J.Unary, p: any): Promise<J | undefined> {
                const match = await pattern`!${operand}`.match(unary, this.cursor);
                return match ? template`${operand}`.apply(unary, this.cursor, {values: match}) :
                    super.visitUnary(unary, p);
            }
        });

        // rewriteRun compares the trees with a fresh parse of the text they print
        return spec.rewriteRun(javascript('with (o) !x;', 'with (o) x;'));
    });

    it('should wrap an expression that replaces a statement of a for head, but not its condition', () => {
        const spec = new RecipeSpec();
        const [name, bound] = [capture('name'), capture('bound')];
        spec.recipe = fromVisitor(new class extends JavaScriptVisitor<any> {
            override async visitVariableDeclarations(declarations: J.VariableDeclarations, p: any): Promise<J | undefined> {
                const match = await pattern`let ${name} = 0`.match(declarations, this.cursor);
                return match ? template`${name} = 0`.apply(declarations, this.cursor, {values: match}) :
                    super.visitVariableDeclarations(declarations, p);
            }

            override async visitBinary(binary: J.Binary, p: any): Promise<J | undefined> {
                const match = await pattern`i < ${bound}`.match(binary, this.cursor);
                return match ? template`i <= ${bound}`.apply(binary, this.cursor, {values: match}) :
                    super.visitBinary(binary, p);
            }
        });

        // rewriteRun compares the trees with a fresh parse of the text they print
        //language=typescript
        return spec.rewriteRun(typescript(`
            for (let i = 0; i < n;) {
            }`,
            `
            for (i = 0; i <= n;) {
            }`));
    });

    it('should make a statement that replaces the expression of an expression statement the statement itself', () => {
        const spec = new RecipeSpec();
        const [a, b, object] = [capture('a'), capture('b'), capture('object')];
        spec.recipe = fromVisitor(new class extends JavaScriptVisitor<any> {
            override async visitBinary(binary: J.Binary, p: any): Promise<J | undefined> {
                const match = await pattern`${a} || (${a} = ${b})`.match(binary, this.cursor);
                return match ? template`${a} ||= ${b}`.apply(binary, this.cursor, {values: match}) :
                    super.visitBinary(binary, p);
            }

            override async visitArrayAccess(arrayAccess: J.ArrayAccess, p: any): Promise<J | undefined> {
                const match = await pattern`${object}['bar']`.match(arrayAccess, this.cursor);
                return match ? template`${object}.bar`.apply(arrayAccess, this.cursor, {values: match}) :
                    super.visitArrayAccess(arrayAccess, p);
            }
        });

        return spec.rewriteRun(
            typescript('a || (a = b);', 'a ||= b;'),
            typescript(`foo['bar'];`, 'foo.bar;'),
            // a `for` head holds every expression in an expression statement
            //language=typescript
            typescript(`
                for (a || (a = b); ;) {
                }`,
                `
                for (a ||= b; ;) {
                }`)
        );
    });

    it('should make a statement that no expression holds, made of the expression of an expression statement, the statement itself', () => {
        const spec = new RecipeSpec();
        const [cond, call, value] = [capture('cond'), capture('call'), capture('value')];
        spec.recipe = fromVisitor(new class extends JavaScriptVisitor<any> {
            override async visitBinary(binary: J.Binary, p: any): Promise<J | undefined> {
                const match = await pattern`${cond} && ${call}`.match(binary, this.cursor);
                if (match) {
                    return template`if (${cond}) ${call}`.apply(binary, this.cursor, {values: match});
                }
                const sum = await pattern`${value} + 1`.match(binary, this.cursor);
                return sum ? template`yield ${value}`.apply(binary, this.cursor, {values: sum}) :
                    super.visitBinary(binary, p);
            }
        });

        // rewriteRun compares the trees with a fresh parse of the text they print
        return spec.rewriteRun(
            typescript('cond && doIt();', 'if (cond) doIt();'),
            // the parser holds a `yield` in a JS.StatementExpression wherever it stands
            //language=typescript
            typescript(`
                function* g() {
                    x + 1;
                }`,
                `
                function* g() {
                    yield x;
                }`)
        );
    });

    it('should make a type tree that replaces the expression of a type tree expression the type tree itself', () => {
        const spec = new RecipeSpec();
        const object = capture('object');
        spec.recipe = fromVisitor(new class extends JavaScriptVisitor<any> {
            override async visitTypeTreeExpression(typeTree: JS.TypeTreeExpression, p: any): Promise<J | undefined> {
                return super.visitTypeTreeExpression(foundSearchResult(typeTree, 'found'), p);
            }

            override async visitArrayAccess(arrayAccess: J.ArrayAccess, p: any): Promise<J | undefined> {
                const match = await pattern`${object}['bar']`.match(arrayAccess, this.cursor);
                return match ? template`${object}.bar`.apply(arrayAccess, this.cursor, {values: match}) :
                    super.visitArrayAccess(arrayAccess, p);
            }
        });

        // rewriteRun compares the trees with a fresh parse of the text they print
        return spec.rewriteRun(
            typescript(`new foo['bar']();`, 'new /*~~(found)~~>*/foo.bar();'),
            typescript(`new  foo['bar'];`, 'new  /*~~(found)~~>*/foo.bar;'),
            //language=typescript
            typescript(`
                class A extends foo['bar'] {
                }`,
                `
                class A extends /*~~(found)~~>*/foo.bar {
                }`)
        );
    });

    it('should keep the markers of the expression statement a statement takes the place of', () => {
        const spec = new RecipeSpec();
        const object = capture('object');
        spec.recipe = fromVisitor(new class extends JavaScriptVisitor<any> {
            override async visitExpressionStatement(statement: JS.ExpressionStatement, p: any): Promise<J | undefined> {
                return super.visitExpressionStatement(foundSearchResult(statement, 'found'), p);
            }

            override async visitArrayAccess(arrayAccess: J.ArrayAccess, p: any): Promise<J | undefined> {
                const match = await pattern`${object}['bar']`.match(arrayAccess, this.cursor);
                return match ? template`${object}.bar`.apply(arrayAccess, this.cursor, {values: match}) :
                    super.visitArrayAccess(arrayAccess, p);
            }
        });

        return spec.rewriteRun(typescript(`foo['bar'];`, '/*~~(found)~~>*/foo.bar;'));
    });

    it('should make a statement substituted where a template writes a statement the statement itself', () => {
        const spec = new RecipeSpec();
        spec.recipe = fromVisitor(new class extends JavaScriptVisitor<any> {
            override async visitMethodInvocation(method: J.MethodInvocation, p: any): Promise<J | undefined> {
                return this.cursor.parentTree()?.value.kind !== JS.Kind.CompilationUnit ? method :
                    template`if (ready) {\n${method}\n}`.apply(method, this.cursor);
            }
        });

        //language=typescript
        return spec.rewriteRun(typescript('go()', `
            if (ready) {
                go()
            }`));
    });

    it('should leave an expression that replaces an argument of the call statement a visitor is on bare', () => {
        const spec = new RecipeSpec();
        spec.recipe = fromVisitor(new class extends JavaScriptVisitor<any> {
            override async visitMethodInvocation(method: J.MethodInvocation, p: any): Promise<J | undefined> {
                const m = await super.visitMethodInvocation(method, p) as J.MethodInvocation;
                const [first] = m.arguments.elements;
                if (m.name.simpleName !== 'outer' || first.element.kind !== J.Kind.MethodInvocation) {
                    return m;
                }
                // The cursor is on the call, a statement whose own parent takes statements, as the argument is
                const sum = await template`${first.element} + 1`.apply(first.element, this.cursor);
                return {...m, arguments: {...m.arguments, elements: [{...first, element: sum!}]}} as J.MethodInvocation;
            }
        });

        // rewriteRun compares the trees with a fresh parse of the text they print
        return spec.rewriteRun(typescript('outer(inner());', 'outer(inner() + 1);'));
    });

    it('should wrap an expression that a chained rule makes of the argument the first rule unwrapped', () => {
        const x = capture();
        const rule = rewrite(() => ({before: pattern`outer(${x})`, after: template`${x}`}))
            .andThen(rewrite(() => ({before: pattern`inner()`, after: template`inner() + 1`})));
        const spec = new RecipeSpec();
        spec.recipe = fromVisitor(new class extends JavaScriptVisitor<any> {
            override async visitMethodInvocation(method: J.MethodInvocation, p: any): Promise<J | undefined> {
                const m = await super.visitMethodInvocation(method, p) as J.MethodInvocation;
                // The first result keeps the argument's id, but it stands where the call statement was
                return await rule.tryOn(this.cursor, m) ?? m;
            }
        });

        return spec.rewriteRun(typescript('outer(inner());', 'inner() + 1;'));
    });
});
