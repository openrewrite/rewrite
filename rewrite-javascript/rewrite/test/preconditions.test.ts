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
import {RecipeSpec} from "../src/test";
import {javascript} from "../src/javascript";
import {ConditionalFindIdentifier, FindIdentifierWithPathPrecondition} from "../fixtures/path-precondition";
import {and, check, CompositePrecondition, not, or, RecipeRef} from "../src/preconditions";
import {findMethods, findTypes, hasSourcePath, usesMethod, usesType} from "../src/javascript/preconditions";
import {ExecutionContext} from "../src/execution";
import {TreeVisitor} from "../src/visitor";
import {Cursor, Tree} from "../src/tree";

describe('Preconditions', () => {
    test('visitor precondition - should only mark identifiers in matching path', async () => {
        const spec = new RecipeSpec();
        spec.recipe = new FindIdentifierWithPathPrecondition({
            requiredPath: 'test.js',
            identifier: 'foo'
        });

        await spec.rewriteRun(
            {
                ...javascript('const foo = 1;', 'const /*~~>*/foo = 1;'),
                path: 'test.js'
            },
            {
                ...javascript('const foo = 2;'),
                path: 'other.js'
            }
        );
    });

    test('boolean precondition - should mark when true', async () => {
        const spec = new RecipeSpec();
        spec.recipe = new ConditionalFindIdentifier({
            shouldSearch: true,
            identifier: 'bar'
        });

        await spec.rewriteRun(
            javascript('const bar = 1;', 'const /*~~>*/bar = 1;')
        );
    });

    test('boolean precondition - should not mark when false', async () => {
        const spec = new RecipeSpec();
        spec.recipe = new ConditionalFindIdentifier({
            shouldSearch: false,
            identifier: 'bar'
        });

        await spec.rewriteRun(
            javascript('const bar = 1;')
        );
    });
});

/**
 * Stub source-file sentinel: only ``isSourceFile(tree)`` needs to be true
 * inside ``Check.visit``; the wrapper doesn't read source-file fields.
 */
const stubSourceFile = (): Tree => ({
    kind: "org.openrewrite.tree.SourceFile",
    id: "00000000-0000-0000-0000-000000000000",
    markers: {kind: "org.openrewrite.marker.Markers", id: "00000000-0000-0000-0000-000000000000", markers: []},
    sourcePath: "stub",
    charsetName: undefined,
    charsetBomMarked: false,
    checksum: undefined,
    fileAttributes: undefined
} as unknown as Tree);

class RecordingVisitor extends TreeVisitor<any, ExecutionContext> {
    calls = 0;

    async visit<R extends any>(tree: Tree, _: ExecutionContext, _parent?: Cursor): Promise<R | undefined> {
        this.calls++;
        return tree as R;
    }
}

class MarkingVisitor extends TreeVisitor<any, ExecutionContext> {
    calls = 0;

    async visit<R extends any>(tree: Tree, _: ExecutionContext, _parent?: Cursor): Promise<R | undefined> {
        this.calls++;
        return ({...(tree as any)}) as R;
    }
}

describe('Preconditions composites (in-process)', () => {
    test('or short-circuits on the first matching operand', async () => {
        const matching = new MarkingVisitor();
        const nonMatching = new RecordingVisitor();
        const editor = new RecordingVisitor();

        const wrapped = await check(or(matching, nonMatching), editor);
        await wrapped.visit(stubSourceFile(), {} as ExecutionContext);

        expect(matching.calls).toBe(1);
        expect(nonMatching.calls).toBe(0);
        expect(editor.calls).toBe(1);
    });

    test('or skips editor when no operand matches', async () => {
        const a = new RecordingVisitor();
        const b = new RecordingVisitor();
        const editor = new RecordingVisitor();

        const wrapped = await check(or(a, b), editor);
        await wrapped.visit(stubSourceFile(), {} as ExecutionContext);

        expect(a.calls).toBe(1);
        expect(b.calls).toBe(1);
        expect(editor.calls).toBe(0);
    });

    test('and runs editor only when all operands match', async () => {
        const matchA = new MarkingVisitor();
        const matchB = new MarkingVisitor();
        const editor = new RecordingVisitor();

        const wrapped = await check(and(matchA, matchB), editor);
        await wrapped.visit(stubSourceFile(), {} as ExecutionContext);
        expect(editor.calls).toBe(1);

        const editor2 = new RecordingVisitor();
        const wrapped2 = await check(and(matchA, new RecordingVisitor()), editor2);
        await wrapped2.visit(stubSourceFile(), {} as ExecutionContext);
        expect(editor2.calls).toBe(0);
    });

    test('not inverts a single-operand match', async () => {
        const matching = new MarkingVisitor();
        const editor1 = new RecordingVisitor();
        await (await check(not(matching), editor1)).visit(stubSourceFile(), {} as ExecutionContext);
        expect(editor1.calls).toBe(0);

        const nonMatching = new RecordingVisitor();
        const editor2 = new RecordingVisitor();
        await (await check(not(nonMatching), editor2)).visit(stubSourceFile(), {} as ExecutionContext);
        expect(editor2.calls).toBe(1);
    });

    test('or/and require at least two operands', () => {
        expect(() => or(new RecordingVisitor())).toThrow(/at least two operands/);
        expect(() => and(new RecordingVisitor())).toThrow(/at least two operands/);
    });

    test('CompositePrecondition is a plain class', () => {
        const composite = or(new RecordingVisitor(), new MarkingVisitor());
        expect(composite).toBeInstanceOf(CompositePrecondition);
        expect(composite.op).toBe("or");
        expect(composite.operands).toHaveLength(2);
    });

    test('RecipeRef helpers are lazy and identifiable', () => {
        // Helpers must NOT fire any RPC when called — they just bundle a
        // recipe name + options for later wire emission.
        const refs = [
            hasSourcePath("**/foo.ts"),
            usesMethod("*..* tostring(..)"),
            usesType("java.util.List"),
            findMethods("*..* fromstring(..)"),
            findTypes("java.util.Map"),
        ];

        for (const ref of refs) {
            expect(ref).toBeInstanceOf(RecipeRef);
        }

        expect(refs[0].recipeName).toBe("org.openrewrite.FindSourceFiles");
        expect(refs[0].options).toEqual({filePattern: "**/foo.ts"});

        expect(refs[1].recipeName).toBe("org.openrewrite.java.search.HasMethod");
        expect(refs[1].options).toEqual({methodPattern: "*..* tostring(..)", matchOverrides: false});

        expect(refs[2].recipeName).toBe("org.openrewrite.java.search.HasType");
        expect(refs[2].options).toEqual({fullyQualifiedTypeName: "java.util.List", checkAssignability: false});

        expect(refs[3].recipeName).toBe("org.openrewrite.java.search.FindMethods");
        expect(refs[4].recipeName).toBe("org.openrewrite.java.search.FindTypes");
    });

    test('RecipeRef without localVisitor short-circuits to "matches"', async () => {
        // Construct a RecipeRef directly without bundling a native
        // visitor — in-process callers should fall back to "always
        // matches" so the wrapped editor still runs (the host evaluates
        // the gate for real once the response goes over the wire).
        const editor = new RecordingVisitor();
        const refWithoutLocal = new RecipeRef("org.openrewrite.java.search.HasMethod", {
            methodPattern: "*..* nope(..)",
            matchOverrides: false,
        });
        await (await check(refWithoutLocal, editor))
            .visit(stubSourceFile(), {} as ExecutionContext);
        expect(editor.calls).toBe(1);
    });

    test('RecipeRef with localVisitor evaluates the gate for real', async () => {
        // Helpers like usesMethod populate a native local visitor so
        // unit tests still see real filtering behavior. The stub source
        // file has no method invocations, so the gate fails and the
        // editor is skipped.
        const editor = new RecordingVisitor();
        await (await check(usesMethod("*..* nope(..)"), editor))
            .visit(stubSourceFile(), {} as ExecutionContext);
        expect(editor.calls).toBe(0);
    });

    test('helpers populate localVisitor for real in-process filtering', () => {
        // Spot-check that helpers bundle a TreeVisitor for offline
        // eval. The actual filtering behavior is exercised by the
        // RecipeSpec / fixture tests at the top of this file.
        expect(hasSourcePath("**/*.ts").localVisitor).toBeDefined();
        expect(usesMethod("*..* a(..)").localVisitor).toBeDefined();
        expect(usesType("X").localVisitor).toBeDefined();
        expect(findMethods("*..* a(..)").localVisitor).toBeDefined();
        expect(findTypes("X").localVisitor).toBeDefined();
    });
});
