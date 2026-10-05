/*
 * Copyright 2026 the original author or authors.
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
import {describeJavaRpc, testJavaRpc} from "../../src/test/java-rpc";
import {ExecutionContext} from "../../src/execution";
import {emptySpace, J} from "../../src/java";
import {
    autodetect,
    createNodeResolutionResultMarker,
    JavaScriptParser,
    JavaScriptVisitor,
    JS,
    NodeResolutionResultKind,
    prettierStyle,
    StyleKind
} from "../../src/javascript";
import {Marker, marker, MarkersKind} from "../../src/markers";
import {randomId} from "../../src/uuid";
import {omitColon, Yaml} from "../../src/yaml";

/**
 * One of each marker that can be attached on this side. A marker type that is added to one of the
 * `Markers` constants needs an entry here, which is what puts it through the trip below.
 */
const samples: { [kind: string]: () => Marker } = {
    [MarkersKind.SearchResult]: () => ({kind: MarkersKind.SearchResult, id: randomId(), description: "found"} as Marker),
    [MarkersKind.ParseExceptionResult]: () => ({
        kind: MarkersKind.ParseExceptionResult,
        id: randomId(),
        parserType: "JavaScriptParser",
        exceptionType: "Error",
        message: "message",
        treeType: "Unknown"
    } as Marker),
    [MarkersKind.MarkupError]: () => ({kind: MarkersKind.MarkupError, id: randomId(), message: "message", detail: "detail"} as Marker),
    [MarkersKind.MarkupWarn]: () => ({kind: MarkersKind.MarkupWarn, id: randomId(), message: "message", detail: "detail"} as Marker),
    [MarkersKind.MarkupInfo]: () => ({kind: MarkersKind.MarkupInfo, id: randomId(), message: "message", detail: "detail"} as Marker),
    [MarkersKind.MarkupDebug]: () => ({kind: MarkersKind.MarkupDebug, id: randomId(), message: "message", detail: "detail"} as Marker),
    [MarkersKind.RecipesThatMadeChanges]: () => ({
        kind: MarkersKind.RecipesThatMadeChanges,
        id: randomId(),
        recipes: [[{
            kind: MarkersKind.RecipeThatMadeChanges,
            name: "org.openrewrite.text.FindAndReplace",
            displayName: "Find and replace",
            instanceName: "Find and replace `a`",
            options: {find: "a", replace: "b"},
            estimatedEffortPerOccurrenceMillis: 300000
        }]]
    } as Marker),
    [MarkersKind.NamedStyles]: () => ({
        kind: MarkersKind.NamedStyles,
        id: randomId(),
        name: "org.openrewrite.Example",
        displayName: "Example",
        description: "An example.",
        tags: ["example"],
        styles: []
    } as Marker),
    [MarkersKind.RpcMarker]: () => marker(randomId(), {tool: "example", version: 1}),
    [J.Markers.Semicolon]: () => ({kind: J.Markers.Semicolon, id: randomId()}),
    [J.Markers.TrailingComma]: () => ({kind: J.Markers.TrailingComma, id: randomId(), suffix: emptySpace} as Marker),
    [J.Markers.OmitParentheses]: () => ({kind: J.Markers.OmitParentheses, id: randomId()}),
    [JS.Markers.Generator]: () => ({kind: JS.Markers.Generator, id: randomId(), prefix: emptySpace} as Marker),
    [JS.Markers.Optional]: () => ({kind: JS.Markers.Optional, id: randomId(), prefix: emptySpace} as Marker),
    [JS.Markers.NonNullAssertion]: () => ({kind: JS.Markers.NonNullAssertion, id: randomId(), prefix: emptySpace} as Marker),
    [JS.Markers.DelegatedYield]: () => ({kind: JS.Markers.DelegatedYield, id: randomId(), prefix: emptySpace} as Marker),
    [JS.Markers.FunctionDeclaration]: () => ({kind: JS.Markers.FunctionDeclaration, id: randomId(), prefix: emptySpace} as Marker),
    [JS.Markers.Computed]: () => ({kind: JS.Markers.Computed, id: randomId(), suffix: emptySpace} as Marker),
    [NodeResolutionResultKind]: () => createNodeResolutionResultMarker("package.json", {
        name: "example",
        version: "1.0.0",
        dependencies: {"is-odd": "^3.0.1"}
    }),
    [StyleKind.Autodetect]: () => autodetect(randomId(), []),
    [StyleKind.PrettierStyle]: () => prettierStyle(randomId(), {semi: false}, "3.0.0"),
    [`${StyleKind.PrettierStyle} without a version`]: () => prettierStyle(randomId(), {semi: false}),
    [Yaml.Markers.OmitColon]: () => omitColon(),
};

const kinds: string[] = [
    // the collection itself and the frames of a recipe stack are not markers
    ...Object.values(MarkersKind).filter(kind => kind !== MarkersKind.Markers && kind !== MarkersKind.RecipeThatMadeChanges),
    ...Object.values(J.Markers),
    ...Object.values(JS.Markers),
    ...Object.values(Yaml.Markers),
    NodeResolutionResultKind,
    StyleKind.Autodetect,
    StyleKind.PrettierStyle,
];

describeJavaRpc("markers attached here reach Java and come back", () => {

    test("every marker type has a sample", () => {
        expect(kinds.filter(kind => !samples[kind])).toEqual([]);
    });

    // Java simplifies `b || false` to `b`, and the tree it puts in a different place is one it sends
    // back whole, so the marker comes back through both codecs rather than as "no change".
    for (const kind of Object.keys(samples)) testJavaRpc(kind, async ({javaRpc}) => {
        const sample = samples[kind]();
        // sending tags what it sends, so what comes back is compared with a copy taken first
        const expected = structuredClone(sample);
        const cu = (await new JavaScriptParser().parse({
            text: "function f(b: boolean) {\n    const c = b || false;\n}",
            sourcePath: "markers.ts"
        }).next()).value as JS.CompilationUnit;

        const marked = await new class extends JavaScriptVisitor<number> {
            protected override async visitBinary(binary: J.Binary, _p: number): Promise<J | undefined> {
                return {...binary, left: {...binary.left, markers: {...binary.left.markers, markers: [sample]}}} as J.Binary;
            }
        }().visitDefined<JS.CompilationUnit>(cu, 0);

        const after = await javaRpc.rpc.visit(marked,
            "org.openrewrite.java.cleanup.SimplifyBooleanExpressionVisitor", new ExecutionContext());

        const returned: Marker[][] = [];
        await new class extends JavaScriptVisitor<number> {
            protected override async visitVariable(variable: J.VariableDeclarations.NamedVariable, p: number): Promise<J | undefined> {
                if (variable.initializer) {
                    returned.push(variable.initializer.element.markers.markers);
                }
                return super.visitVariable(variable, p);
            }
        }().visit(after, 0);
        expect(returned).toEqual([[expected]]);
    });
});
