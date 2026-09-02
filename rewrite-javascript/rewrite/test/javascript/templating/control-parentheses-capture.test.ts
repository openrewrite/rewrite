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
import {Cursor, ExecutionContext, SourceFile} from "../../../src";
import {fromVisitor, RecipeSpec} from "../../../src/test";
import {
    capture,
    JavaScriptParser,
    JavaScriptVisitor,
    JS,
    MatchResult,
    Pattern,
    pattern,
    rewrite,
    template,
    typescript
} from "../../../src/javascript";
import {Expression, J} from "../../../src/java";

/** The first node of `kind` in `text`, with the cursor that reached it. */
async function findFirst(text: string, kind: string): Promise<[J, Cursor]> {
    const cu: SourceFile = await new JavaScriptParser().parseOne({text, sourcePath: "match.ts"});
    let found: [J, Cursor] | undefined;
    const visitor = new class extends JavaScriptVisitor<ExecutionContext> {
        protected override async postVisit(node: J, ctx: ExecutionContext): Promise<J | undefined> {
            const visited = await super.postVisit(node, ctx);
            if (!found && visited && visited.kind === kind) {
                found = [visited, this.cursor];
            }
            return visited;
        }
    };
    await visitor.visit(cu, new ExecutionContext());
    expect(found).toBeDefined();
    return found!;
}

/** Matches `pat` against the first node of `kind` in `text`, returning the match result. */
async function matchFirst(text: string, kind: string, pat: Pattern): Promise<MatchResult | undefined> {
    const [node, cursor] = await findFirst(text, kind);
    return pat.match(node, cursor);
}

describe('a capture that is a whole ControlParentheses', () => {
    // Looking through the parentheses must walk past a marker below them, not claim it.
    test('a capture nested inside a condition still binds only itself', async () => {
        const n = capture('n');
        const match = await matchFirst('if (x > 1) { a(); }', J.Kind.If, pattern`if (x > ${n}) { a(); }`);
        expect((match!.get(n) as Expression).kind).toBe(J.Kind.Literal);
    });

    test('if condition', async () => {
        expect(await matchFirst('if (c) { a(); }', J.Kind.If,
            pattern`if (${capture()}) { a(); }`)).toBeDefined();
    });

    test('while condition', async () => {
        expect(await matchFirst('while (c) { a(); }', J.Kind.WhileLoop,
            pattern`while (${capture()}) { a(); }`)).toBeDefined();
    });

    test('do-while condition', async () => {
        expect(await matchFirst('do { a(); } while (c);', J.Kind.DoWhileLoop,
            pattern`do { a(); } while (${capture()});`)).toBeDefined();
    });

    test('switch selector', async () => {
        expect(await matchFirst('switch (c) { case 1: break; }', J.Kind.Switch,
            pattern`switch (${capture()}) { case 1: break; }`)).toBeDefined();
    });

    test('with expression', async () => {
        expect(await matchFirst('with (o) { a(); }', JS.Kind.WithStatement,
            pattern`with (${capture()}) { a(); }`)).toBeDefined();
    });

    // The debug comparator has its own copy of the capture check, so it has to be exercised too.
    test('the debug comparator agrees with the plain one', async () => {
        const c = capture('c');
        const [node, cursor] = await findFirst('if (x > 1) { a(); }', J.Kind.If);
        const attempt = await pattern`if (${c}) { a(); }`.matchWithExplanation(node, cursor);
        expect(attempt.matched).toBe(true);
        expect(attempt.result!.get(c)).toBeDefined();
    });

    test('binds the condition itself, not the parentheses around it', async () => {
        const c = capture('c');
        const match = await matchFirst('if (x > 1) { a(); }', J.Kind.If, pattern`if (${c}) { a(); }`);
        const bound = match!.get(c) as Expression;
        expect(bound.kind).toBe(J.Kind.Binary);
    });

    test('a constraint sees the condition it is applied to', async () => {
        const identifierOnly = capture({name: 'c', constraint: (n: J) => n.kind === J.Kind.Identifier});
        expect(await matchFirst('if (c) { a(); }', J.Kind.If,
            pattern`if (${identifierOnly}) { a(); }`)).toBeDefined();
        expect(await matchFirst('if (x > 1) { a(); }', J.Kind.If,
            pattern`if (${identifierOnly}) { a(); }`)).toBeUndefined();
    });

    // The pattern and the source need not agree on how many layers of parentheses they use.
    test('a parenthesized capture matches an unparenthesized condition', async () => {
        const c = capture('c');
        const match = await matchFirst('if (c) { a(); }', J.Kind.If, pattern`if ((${c})) { a(); }`);
        expect((match!.get(c) as Expression).kind).toBe(J.Kind.Identifier);
    });

    test('a plain capture keeps the parentheses a redundantly parenthesized condition had', async () => {
        const c = capture('c');
        const match = await matchFirst('if ((c)) { a(); }', J.Kind.If, pattern`if (${c}) { a(); }`);
        expect((match!.get(c) as Expression).kind).toBe(J.Kind.Parentheses);
    });
});

describe('rewriting through a captured condition', () => {
    const spec = new RecipeSpec();

    test('reuses a captured if condition in the replacement', () => {
        const c = capture('c');
        const negate = rewrite(() => ({
            before: pattern`if (${c}) { a(); }`,
            after: template`if (!(${c})) { a(); }`
        }));

        spec.recipe = fromVisitor(new class extends JavaScriptVisitor<any> {
            override async visitIf(iff: J.If, p: any): Promise<J | undefined> {
                return await negate.tryOn(this.cursor, iff) || super.visitIf(iff, p);
            }
        });

        return spec.rewriteRun(
            //language=typescript
            typescript(
                `if (x > 1) {
    a();
}`,
                `if (!(x > 1)) {
    a();
}`
            )
        );
    });
});
