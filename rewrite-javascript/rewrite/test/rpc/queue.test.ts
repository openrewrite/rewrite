import {Json} from "../../src/json";
import {asRef, ReferenceMap, RpcCodecs, RpcReceiveQueue, RpcSendQueue, RpcObjectState, StringInternTable} from "../../src/rpc";
import type {RpcObjectData} from "../../src/rpc";
import {IntelliJ, JavaScriptParser, JS, sourceFileCache, SpacesStyleDetailKind, StyleKind} from "../../src/javascript";
import {MarkersKind} from "../../src/markers";
import {NamedStyles} from "../../src/style";
import {TreePrinters} from "../../src/print";
import {ExecutionContext} from "../../src/execution";

describe("RPC queues", () => {

    async function sendList<T>(after: T[] | undefined, before: T[] | undefined): Promise<RpcObjectData[]> {
        const queue = new RpcSendQueue(new ReferenceMap(), Json.Kind.Document, false);
        await queue.sendList(after, before, t => t);
        return queue.finish();
    }

    // The positions array is what lets a reorder cost one integer per element instead
    // of re-sending the elements themselves.
    test("reordered elements are repositioned, not resent", async () => {
        const batch = await sendList(["C", "A", "B"], ["A", "B", "C"]);

        expect(batch.map(d => d.state)).toEqual([
            RpcObjectState.CHANGE, RpcObjectState.CHANGE,
            RpcObjectState.NO_CHANGE, RpcObjectState.NO_CHANGE, RpcObjectState.NO_CHANGE,
            RpcObjectState.END_OF_OBJECT,
        ]);
        expect(batch[1].value).toEqual([2, 0, 1]);
    });

    test("every element is added when the before list is empty", async () => {
        const batch = await sendList(["A", "B"], []);

        expect(batch.map(d => d.state)).toEqual([
            RpcObjectState.CHANGE, RpcObjectState.CHANGE,
            RpcObjectState.ADD, RpcObjectState.ADD,
            RpcObjectState.END_OF_OBJECT,
        ]);
        expect(batch[1].value).toEqual([-1, -1]);
    });

    test("mixed adds and removals", async () => {
        const batch = await sendList(["A", "E", "F", "C"], ["A", "B", "C", "D"]);

        expect(batch.map(d => d.state)).toEqual([
            RpcObjectState.CHANGE, RpcObjectState.CHANGE,
            RpcObjectState.NO_CHANGE, RpcObjectState.ADD, RpcObjectState.ADD, RpcObjectState.NO_CHANGE,
            RpcObjectState.END_OF_OBJECT,
        ]);
        expect(batch[1].value).toEqual([0, -1, -1, 2]);
    });

    test("null is sent as an absent value, never as an ADD that carries nothing", async () => {
        const send = (after: any, before: any) =>
            new RpcSendQueue(new ReferenceMap(), Json.Kind.Document, false).generate(after, before);

        expect(await send(null, undefined)).toEqual([
            {state: RpcObjectState.NO_CHANGE}, {state: RpcObjectState.END_OF_OBJECT}]);
        expect(await send(null, "before")).toEqual([
            {state: RpcObjectState.DELETE}, {state: RpcObjectState.END_OF_OBJECT}]);
        expect((await send("after", null))[0].state).toEqual(RpcObjectState.ADD);
    });

    test("the execution context is sent under the name Java knows it by", async () => {
        const batch = await new RpcSendQueue(new ReferenceMap(), Json.Kind.Document, false).generate(new ExecutionContext(), undefined);
        expect(batch[0]).toEqual({state: RpcObjectState.ADD, valueType: "org.openrewrite.InMemoryExecutionContext"});
    });

    test("an inline value from Java loses the keys its serializer added", async () => {
        const batch: RpcObjectData[] = [
            {state: RpcObjectState.ADD, valueType: "org.openrewrite.rpc.RpcMarker", value: {"@c": "org.openrewrite.rpc.RpcMarker", "@ref": 1, id: "1", tool: "example"}},
            {state: RpcObjectState.ADD, value: null},
        ];
        const queue = new RpcReceiveQueue(new Map(), undefined, async () => batch.splice(0), undefined, false);

        expect(await queue.receive(undefined)).toEqual({kind: "org.openrewrite.rpc.RpcMarker", id: "1", tool: "example"});
        expect(await queue.receive(undefined)).toBeNull();
    });

    test("a set of styles changed here travels with its type, in the shape Java decodes", async () => {
        const before: NamedStyles = {
            kind: MarkersKind.NamedStyles,
            id: "1",
            name: "example",
            displayName: "Example",
            tags: [],
            styles: [IntelliJ.TypeScript.tabsAndIndents()]
        };
        const after: NamedStyles = {...before, styles: [{...IntelliJ.TypeScript.tabsAndIndents(), indentSize: 2} as any]};

        const batch = await new RpcSendQueue(new ReferenceMap(), JS.Kind.CompilationUnit, false).generate(after, before);

        expect(batch[0]).toEqual({
            state: RpcObjectState.CHANGE,
            valueType: MarkersKind.NamedStyles,
            value: {
                ...before, styles: [{
                    "@c": StyleKind.TabsAndIndentsStyle,
                    "@ref": 2,
                    useTabCharacter: false,
                    tabSize: 4,
                    indentSize: 2,
                    continuationIndent: 4,
                    keepIndentsOnEmptyLines: false,
                    indentChainedMethods: true,
                    indentAllChainedCallsInAGroup: false
                }]
            }
        });
    });

    test("a set of styles from Java has the kinds the styles here are found by", async () => {
        const batch: RpcObjectData[] = [{
            state: RpcObjectState.ADD,
            valueType: MarkersKind.NamedStyles,
            value: {
                "@c": MarkersKind.NamedStyles, "@ref": 1, id: "1", name: "example", displayName: "Example", tags: [],
                styles: [
                    {"@c": StyleKind.SpacesStyle, "@ref": 2, within: {es6ImportExportBraces: true}},
                    {"@c": "org.openrewrite.style.GeneralFormatStyle", "@ref": 3, useCRLFNewLines: false}
                ]
            }
        }];
        const q = new RpcReceiveQueue(new Map(), JS.Kind.CompilationUnit, async () => batch.splice(0), undefined, false);

        expect((await q.receive<NamedStyles>(undefined)).styles).toEqual([
            {kind: StyleKind.SpacesStyle, within: {kind: SpacesStyleDetailKind.SpacesStyleWithin, es6ImportExportBraces: true}},
            {kind: "org.openrewrite.style.GeneralFormatStyle", useCRLFNewLines: false}
        ]);
    });

    test("an unchanged list is a single NO_CHANGE", async () => {
        const before = ["A", "B"];
        const batch = await sendList(before, before);

        expect(batch.map(d => d.state)).toEqual([
            RpcObjectState.NO_CHANGE, RpcObjectState.END_OF_OBJECT,
        ]);
    });

    test("a source file of another type than requested is received with its own codecs", async () => {
        // given
        const document = "test.ReplacementDocument";
        const leaf = "test.ReplacementLeaf";
        RpcCodecs.registerCodec(document, {
            async rpcReceive(before: any, q: RpcReceiveQueue) {
                return {...before, leaf: await q.receive(before.leaf)};
            },
            async rpcSend() {
            }
        }, document);
        RpcCodecs.registerCodec(leaf, {
            async rpcReceive(before: any, q: RpcReceiveQueue) {
                return {...before, text: await q.receive(before.text)};
            },
            async rpcSend() {
            }
        }, document);
        const batch: RpcObjectData[] = [
            {state: RpcObjectState.ADD, valueType: document},
            {state: RpcObjectState.ADD, valueType: leaf},
            {state: RpcObjectState.ADD, value: "Goodbye"},
        ];
        const q = new RpcReceiveQueue(new Map(), JS.Kind.CompilationUnit, async () => batch.splice(0), undefined, false);

        // when
        const received = await q.receive<any>({kind: JS.Kind.CompilationUnit});

        // then
        expect(received).toEqual({kind: document, leaf: {kind: leaf, text: "Goodbye"}});
    });

    test("reading past END_OF_OBJECT fails rather than re-serving the batch", async () => {
        const batch = await sendList(["A"], []);
        const q = new RpcReceiveQueue(new Map(), undefined, async () => batch, undefined, false);

        await q.receiveList<string>(undefined);
        expect((await q.take()).state).toBe(RpcObjectState.END_OF_OBJECT);
        await expect(q.take()).rejects.toThrow(/past END_OF_OBJECT/);
    });

    test("asRef doesn't create a new instance", () => {
        const space = {kind: Json.Kind.Space, comments: [], whitespace: "\n"};

        const ref1 = asRef(space)!;
        const ref2 = asRef(space)!;

        expect(ref1).toBe(ref2);

        // So it works when used in a WeakMap of references
        const refs = new WeakMap<Object, number>();
        refs.set(ref1, 1);
        expect(refs.has(ref2)).toBeTruthy();
    })

    test("reuses reference ID zero", async () => {
        const value = asRef({kind: Json.Kind.Space, comments: [], whitespace: "\n"});
        const queue = new RpcSendQueue(new ReferenceMap(), Json.Kind.Document, false);

        await queue.generate(value, undefined);
        const batch = await queue.generate(value, undefined);

        expect(batch).toEqual([
            {state: RpcObjectState.ADD, ref: 0},
            {state: RpcObjectState.END_OF_OBJECT},
        ]);
    });

    test("a changed ref slot is re-added instead of changed", async () => {
        const spaceA = {kind: Json.Kind.Space, comments: [], whitespace: " "};
        const spaceB = {kind: Json.Kind.Space, comments: [], whitespace: "  "};
        const queue = new RpcSendQueue(new ReferenceMap(), Json.Kind.Document, false);

        await queue.generate(asRef(spaceA), undefined);

        // A changed ref slot is re-added under a fresh ref, never CHANGEd (see RpcSendQueue.send)
        const reAdd = await queue.generate(asRef(spaceB), asRef(spaceA));
        expect(reAdd[0].state).toBe(RpcObjectState.ADD);
        expect(reAdd[0].ref).toBe(1);
        expect(reAdd[0].valueType).toBe(Json.Kind.Space);

        // A repeat of the same transition dedups against the ref registered by the re-add
        const refOnly = await queue.generate(asRef(spaceB), asRef(spaceA));
        expect(refOnly).toEqual([
            {state: RpcObjectState.ADD, ref: 1},
            {state: RpcObjectState.END_OF_OBJECT},
        ]);
    });

    test("changePropertyType", async () => {
        // Test changing a property from one type to another type
        // This simulates a recipe that changes the type of an object assigned to a property

        // Create a wrapper object with a property that changes type
        interface Wrapper {
            id: string;
            value: any; // This property will change from Literal to Identifier
        }

        const beforeWrapper: Wrapper = {
            id: "test",
            value: {kind: Json.Kind.Literal, value: "old-value"}
        };

        const afterWrapper: Wrapper = {
            id: "test",
            value: {kind: Json.Kind.Identifier, name: "new-name"}
        };

        const sq = new RpcSendQueue(new ReferenceMap(), Json.Kind.Document, false);
        const batch = await sq.generate(afterWrapper, beforeWrapper);
        expect(batch.length).toBeGreaterThan(0);

        const rq = new RpcReceiveQueue(new Map(), undefined, async () => batch, undefined, false);
        const received = await rq.receive(beforeWrapper) as Wrapper;

        // Verify the wrapper's id stayed the same
        expect(received.id).toBe("test");
        // Verify the property changed from Literal to Identifier
        expect(received.value.kind).toBe(Json.Kind.Identifier);
        expect(received.value.kind).not.toBe(beforeWrapper.value.kind);
    });

    test("changed list element without a codec round-trips", async () => {
        // Markers like GitProvenance or BuildTool have no codec; a changed marker
        // that keeps its id diffs as CHANGE rather than ADD
        const before = [{kind: "org.openrewrite.marker.BuildTool", id: "m1", type: "Gradle", version: "7.0"}];
        const after = [{...before[0], version: "8.0"}];

        const sq = new RpcSendQueue(new ReferenceMap(), undefined, false);
        await sq.sendList(after, before, m => m.id);
        const batch = sq.finish();

        const rq = new RpcReceiveQueue(new Map(), undefined, async () => batch, undefined, false);
        const received = await rq.receiveList(before);

        expect(received![0].version).toBe("8.0");
    });

    test("interning across a received tree preserves it", async () => {
        // given
        const source = "const a = a + a + a;";
        const parser = new JavaScriptParser({sourceFileCache});
        const parsed = (await parser.parse({text: source, sourcePath: "t.ts"}).next()).value as JS.CompilationUnit;

        // when
        const batch = await new RpcSendQueue(new ReferenceMap(), JS.Kind.CompilationUnit, false).generate(parsed, undefined);
        const received = await new RpcReceiveQueue(new Map(), JS.Kind.CompilationUnit, async () => batch, undefined, false)
            .receive<JS.CompilationUnit>(undefined);

        // then
        expect(await TreePrinters.print(received)).toBe(source);
    });

    test("intern table always interns discriminators but bounds scalar values", () => {
        // given
        const maxEntries = 3;
        const maxValueLength = 5;
        const table = new StringInternTable(maxEntries, maxValueLength);

        // when / then: a value longer than the limit is returned uninterned and never stored
        const long = "x".repeat(maxValueLength + 1);
        expect(table.internValue(long)).toBe(long);
        expect(table.size).toBe(0);

        // and: short values are interned up to the cap, then returned uninterned
        table.internValue("aa");
        table.internValue("bb");
        table.internValue("cc");
        expect(table.size).toBe(maxEntries);
        expect(table.internValue("dd")).toBe("dd");
        expect(table.size).toBe(maxEntries);

        // and: discriminators are interned even past the value cap, since their set is bounded
        table.internType("org.openrewrite.java.tree.J$Identifier");
        expect(table.size).toBe(maxEntries + 1);

        // and: clear empties the table
        table.clear();
        expect(table.size).toBe(0);
    });

    test("empty lists deserialize to a single shared frozen instance", async () => {
        // given
        const emptyBatch = await sendList<string>([], undefined);

        // when
        const first = await new RpcReceiveQueue(new Map(), undefined, async () => emptyBatch, undefined, false)
            .receiveList<string>(undefined);
        const second = await new RpcReceiveQueue(new Map(), undefined, async () => emptyBatch, undefined, false)
            .receiveList<string>(undefined);

        // then
        expect(first).toEqual([]);
        expect(first).toBe(second);
        expect(Object.isFrozen(first)).toBe(true);
    });

    test("detects missing codec on receiver side", async () => {
        // given
        const batch: RpcObjectData[] = [
            {state: RpcObjectState.ADD, valueType: "com.example.UnknownMarker", value: undefined},
        ];
        const rq = new RpcReceiveQueue(new Map(), undefined, async () => batch, undefined, false);

        // when / then
        await expect(rq.receive(undefined)).rejects.toThrow(
            "No RPC codec registered on the TypeScript side for 'com.example.UnknownMarker'"
        );
    });
});
