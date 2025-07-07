import {Json} from "../../src/json";
import {asRef} from "../../src/rpc";

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
});
