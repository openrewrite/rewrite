import {Json} from "../../src/json";
import {asRef, ReferenceMap, RpcReceiveQueue, RpcSendQueue, RpcObjectState, StringInternTable} from "../../src/rpc";
import type {RpcObjectData} from "../../src/rpc";
import {JavaScriptParser, JS, sourceFileCache} from "../../src/javascript";
import {TreePrinters} from "../../src/print";

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

    test("an unchanged list is a single NO_CHANGE", async () => {
        const before = ["A", "B"];
        const batch = await sendList(before, before);

        expect(batch.map(d => d.state)).toEqual([
            RpcObjectState.NO_CHANGE, RpcObjectState.END_OF_OBJECT,
        ]);
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
