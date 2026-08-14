/*
 * Copyright 2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import {ReferenceMap, RpcReceiveQueue, RpcSendQueue} from "../../src/rpc";
import {Markers, MarkersKind, RecipesThatMadeChanges} from "../../src/markers";

async function sendMarkers(markers: Markers) {
    const sq = new RpcSendQueue(new ReferenceMap(), undefined, false);
    await sq.send(markers, undefined);
    return sq.finish();
}

async function receiveMarkers(batch: any[]): Promise<Markers> {
    const rq = new RpcReceiveQueue(new Map(), undefined, async () => batch, undefined, false);
    return rq.receive<Markers>(undefined);
}

describe("RecipesThatMadeChanges", () => {

    test("round trips a recipe stack as identity", async () => {
        const marker: RecipesThatMadeChanges = {
            kind: MarkersKind.RecipesThatMadeChanges,
            id: "11111111-2222-3333-4444-555555555555",
            recipes: [[
                {kind: MarkersKind.RecipeThatMadeChanges, name: "org.openrewrite.text.ChangeText"},
                {
                    kind: MarkersKind.RecipeThatMadeChanges,
                    name: "org.openrewrite.text.FindAndReplace",
                    displayName: "Find and replace",
                    instanceName: "Find and replace `blacklist`",
                    options: {find: "blacklist", regex: true},
                    estimatedEffortPerOccurrenceMillis: 300000
                }
            ]]
        };

        // Markers travel as refs, so both hops go through the Markers codec. Hop 1 exercises the
        // receiver against a Java-shaped stream; hop 2 exercises this peer's own sender.
        const markers: Markers = {
            kind: MarkersKind.Markers,
            id: "99999999-8888-7777-6666-555555555555",
            markers: [marker]
        };

        const firstHop = await sendMarkers(markers);
        let received = (await receiveMarkers(firstHop)).markers[0] as RecipesThatMadeChanges;

        const secondHop = await sendMarkers({...markers, markers: [received]});
        expect(secondHop.some(d => d.value === "org.openrewrite.text.FindAndReplace"))
            .toBe(true);
        received = (await receiveMarkers(secondHop)).markers[0] as RecipesThatMadeChanges;

        expect(received.id).toBe("11111111-2222-3333-4444-555555555555");
        expect(received.recipes).toHaveLength(1);
        expect(received.recipes[0]).toHaveLength(2);

        // A frame carrying only a name must not acquire values from its neighbour.
        expect(received.recipes[0][0].name).toBe("org.openrewrite.text.ChangeText");
        expect(received.recipes[0][0].displayName).toBeUndefined();
        expect(received.recipes[0][0].options).toBeUndefined();

        expect(received.recipes[0][1]).toMatchObject({
            name: "org.openrewrite.text.FindAndReplace",
            displayName: "Find and replace",
            instanceName: "Find and replace `blacklist`",
            options: {find: "blacklist", regex: true},
            estimatedEffortPerOccurrenceMillis: 300000
        });
    });
});
