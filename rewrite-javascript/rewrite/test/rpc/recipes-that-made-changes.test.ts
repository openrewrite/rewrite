import {ReferenceMap, RpcReceiveQueue, RpcSendQueue} from "../../src/rpc";
import {MarkersKind, RecipesThatMadeChanges} from "../../src/markers";

describe("RecipesThatMadeChanges RPC codec", () => {
    test("round-trips stacks and shares a repeated recipe within a marker", async () => {
        // given a marker whose stacks repeat the same opaque recipe descriptor
        const recipe = {name: "example.Recipe", displayName: "Example recipe"};
        const marker: RecipesThatMadeChanges = {
            kind: MarkersKind.RecipesThatMadeChanges,
            id: "aaaaaaaa-1111-2222-3333-444444444444",
            recipes: [[recipe, recipe], [recipe]],
        };

        // when it is round-tripped through the RPC codec
        const sq = new RpcSendQueue(new ReferenceMap(), undefined, false);
        const batch = await sq.generate(marker, undefined);
        const rq = new RpcReceiveQueue(new Map(), undefined, async () => batch, undefined, false);
        const received = await rq.receive<RecipesThatMadeChanges>(undefined);

        // then the descriptor content survives and the repeated recipe is a single shared instance
        expect(received.id).toBe(marker.id);
        expect(received.recipes.length).toBe(2);
        expect(received.recipes[0].length).toBe(2);
        expect(received.recipes[0][0].name).toBe("example.Recipe");
        expect(received.recipes[0][0]).toBe(received.recipes[0][1]);
        expect(received.recipes[0][0]).toBe(received.recipes[1][0]);
    });
});
