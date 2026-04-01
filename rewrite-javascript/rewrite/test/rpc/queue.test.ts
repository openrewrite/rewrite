import {Json} from "../../src/json";
import {asRef, ReferenceMap, RpcReceiveQueue, RpcSendQueue} from "../../src/rpc";

describe("RPC queues", () => {

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
});
