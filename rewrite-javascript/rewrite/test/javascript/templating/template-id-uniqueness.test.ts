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
import {Cursor, rootCursor} from "../../../src";
import {fromVisitor, RecipeSpec} from "../../../src/test";
import {J} from "../../../src/java";
import {
    capture,
    JavaScriptParser,
    JavaScriptVisitor,
    JS,
    Pattern,
    pattern,
    Template,
    template,
    typescript,
    sourceFileCache} from "../../../src/javascript";

// Template ASTs are cached, so without fresh ids per application two applications share ids (customer-requests#3057)

const PADDING_KINDS: ReadonlySet<string> = new Set<string>([
    J.Kind.RightPadded,
    J.Kind.LeftPadded,
    J.Kind.Container
]);

/** Every `Tree` position in `tree`, counting repeated object references separately so duplicates are not hidden. */
function treeNodes(value: any, out: { id: string, kind: string, node: any }[] = []): { id: string, kind: string, node: any }[] {
    if (value === null || typeof value !== 'object') {
        return out;
    }
    if (Array.isArray(value)) {
        for (const element of value) {
            treeNodes(element, out);
        }
        return out;
    }
    const isPadding = typeof value.kind === 'string' && PADDING_KINDS.has(value.kind);
    const isTreeNode = typeof value.id === 'string' && typeof value.kind === 'string' &&
        typeof value.markers === 'object';
    if (!isPadding && !isTreeNode) {
        return out;
    }
    if (isTreeNode) {
        out.push({id: value.id, kind: value.kind, node: value});
    }
    for (const key of Object.keys(value)) {
        if (key !== 'markers') {
            treeNodes(value[key], out);
        }
    }
    return out;
}

function duplicateIds(tree: any): string[] {
    const counts = new Map<string, number>();
    for (const node of treeNodes(tree)) {
        counts.set(node.id, (counts.get(node.id) ?? 0) + 1);
    }
    return [...counts].filter(([, count]) => count > 1).map(([id]) => id);
}

function expectUniqueIds(cu: JS.CompilationUnit): void {
    expect(duplicateIds(cu)).toEqual([]);
}

/** Applies `tmpl` wherever `pat` matches, the way a recipe would. */
function matchAndReplace(pat: Pattern, tmpl: Template) {
    return new class extends JavaScriptVisitor<any> {
        override async visitUnary(unary: J.Unary, p: any): Promise<J | undefined> {
            const u = (await super.visitUnary(unary, p)) as J.Unary;
            const match = await pat.match(u, this.cursor);
            return match ? tmpl.apply(u, this.cursor, {values: match}) : u;
        }

        override async visitMethodInvocation(method: J.MethodInvocation, p: any): Promise<J | undefined> {
            const m = (await super.visitMethodInvocation(method, p)) as J.MethodInvocation;
            const match = await pat.match(m, this.cursor);
            return match ? tmpl.apply(m, this.cursor, {values: match}) : m;
        }
    };
}

async function parse(src: string, sourcePath = 'a.ts'): Promise<JS.CompilationUnit> {
    const parser = new JavaScriptParser({sourceFileCache});
    return (await parser.parse({text: src, sourcePath}).next()).value as JS.CompilationUnit;
}

async function firstOfKind<T extends J>(cu: JS.CompilationUnit, kind: string): Promise<{ node: T, cursor: Cursor }> {
    let found: { node: T, cursor: Cursor } | undefined;
    await new class extends JavaScriptVisitor<any> {
        protected override async preVisit(tree: J, p: any): Promise<J | undefined> {
            if (!found && tree.kind === kind) {
                found = {node: tree as T, cursor: this.cursor};
            }
            return tree;
        }
    }().visit(cu, null, rootCursor());
    return found!;
}

// The recipe from the issue, with the pattern/template pair at module scope as a real recipe's would be
const x = capture('x');
const doubleNegation = pattern`!!${x}`;
const booleanCall = template`Boolean(${x})`;

describe('template id uniqueness', () => {
    const spec = new RecipeSpec();

    test('two applications of one template within a tree share no ids', () => {
        spec.recipe = fromVisitor(matchAndReplace(doubleNegation, booleanCall));

        return spec.rewriteRun({
            ...typescript(
                'const b = !!!!foo;',
                'const b = Boolean(Boolean(foo));'
            ),
            afterRecipe: expectUniqueIds
        });
    });

    test('three applications of one template within a tree share no ids', () => {
        spec.recipe = fromVisitor(matchAndReplace(doubleNegation, booleanCall));

        return spec.rewriteRun({
            ...typescript(
                'const b = !!!!!!foo;',
                'const b = Boolean(Boolean(Boolean(foo)));'
            ),
            afterRecipe: expectUniqueIds
        });
    });

    test('two applications at sibling positions share no ids', () => {
        // Unlike the nested case, neither application can clean up after the other
        spec.recipe = fromVisitor(matchAndReplace(doubleNegation, booleanCall));

        return spec.rewriteRun({
            ...typescript(
                'const a = !!foo;\nconst b = !!bar;',
                'const a = Boolean(foo);\nconst b = Boolean(bar);'
            ),
            afterRecipe: expectUniqueIds
        });
    });

    test('a variadic capture expanded in two applications shares no ids', () => {
        // Variadic expansion bypasses `visit()` in `PlaceholderReplacementVisitor`
        const args = capture({variadic: true});
        spec.recipe = fromVisitor(matchAndReplace(pattern`foo(${args})`, template`bar(${args})`));

        return spec.rewriteRun({
            ...typescript(
                'foo(1, 2);\nfoo(3, 4);',
                'bar(1, 2);\nbar(3, 4);'
            ),
            afterRecipe: expectUniqueIds
        });
    });

    test('a capture spliced into a template twice yields no duplicate ids', () => {
        const y = capture('y');
        spec.recipe = fromVisitor(matchAndReplace(pattern`foo(${y})`, template`bar(${y}, ${y})`));

        return spec.rewriteRun({
            ...typescript(
                'foo(1);',
                'bar(1, 1);'
            ),
            afterRecipe: expectUniqueIds
        });
    });

    test('source files rewritten in one process share no ids and no node instances', async () => {
        spec.recipe = fromVisitor(matchAndReplace(doubleNegation, booleanCall));

        const rewritten: JS.CompilationUnit[] = [];
        await spec.rewriteRun(
            {
                ...typescript('const b = !!foo;', 'const b = Boolean(foo);'),
                afterRecipe: (cu: JS.CompilationUnit) => {
                    rewritten.push(cu);
                }
            },
            {
                ...typescript('const d = !!bar;', 'const d = Boolean(bar);'),
                afterRecipe: (cu: JS.CompilationUnit) => {
                    rewritten.push(cu);
                }
            }
        );
        expect(rewritten).toHaveLength(2);

        const [a, c] = rewritten;
        const cNodes = treeNodes(c);
        const cIds = new Set(cNodes.map(n => n.id));
        const cInstances = new Set(cNodes.map(n => n.node));

        expect(treeNodes(a).filter(n => cIds.has(n.id))).toEqual([]);
        expect(treeNodes(a).filter(n => cInstances.has(n.node))).toEqual([]);
    });

    test('two Template instances with identical code share no ids', async () => {
        // The AST caches are keyed on template text, so a per-visit template does not sidestep the sharing
        const first = await firstOfKind<J.Literal>(await parse('const a = 1;\n'), J.Kind.Literal);
        const second = await firstOfKind<J.Literal>(await parse('const b = 1;\n'), J.Kind.Literal);

        const firstResult = (await template`Boolean(someIdentifier)`.apply(first.node, first.cursor))!;
        const secondResult = (await template`Boolean(someIdentifier)`.apply(second.node, second.cursor))!;

        // Guards against the assertions below passing vacuously on an unapplied template
        expect((firstResult as J.MethodInvocation).name.simpleName).toBe('Boolean');
        expect((secondResult as J.MethodInvocation).name.simpleName).toBe('Boolean');

        const secondIds = new Set(treeNodes(secondResult).map(n => n.id));
        const firstIds = treeNodes(firstResult).map(n => n.id);
        expect(firstIds.length).toBeGreaterThan(1);
        expect(firstIds.filter(id => secondIds.has(id))).toEqual([]);
    });

    test('a node spliced into two applications shares no ids', () => {
        // Hoisted out of the visitor, so every application splices the very same node
        let hoisted: J | undefined;
        spec.recipe = fromVisitor(new class extends JavaScriptVisitor<any> {
            override async visitMethodInvocation(method: J.MethodInvocation, p: any): Promise<J | undefined> {
                if (method.name.simpleName !== 'wrap') {
                    return method;
                }
                hoisted ??= method.arguments.elements[0].element;
                return template`f(${hoisted!})`.apply(method, this.cursor);
            }
        });

        return spec.rewriteRun({
            ...typescript(
                'wrap(1);\nwrap(2);',
                'f(1);\nf(1);'
            ),
            afterRecipe: expectUniqueIds
        });
    });

    test('a substituted capture keeps the id it had in the source tree', async () => {
        // Ids are refreshed before substitution, so nodes carried over from the source tree keep their identity
        const cu = await parse('foo(1);\n');
        const {node, cursor} = await firstOfKind<J.MethodInvocation>(cu, J.Kind.MethodInvocation);

        const y = capture('y');
        const match = await pattern`foo(${y})`.match(node, cursor);
        const captured = match!.get('y') as J;

        const result = (await template`bar(${y})`.apply(node, cursor, {values: match}))!;

        expect((result as J.MethodInvocation).name.simpleName).toBe('bar');
        expect(treeNodes(result).map(n => n.id)).toContain(captured.id);
    });
});
