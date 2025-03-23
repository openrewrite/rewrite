import {JsonDocument, Empty, emptySpace, Json, JsonKind, JsonVisitor} from "../../../main/javascript/json";
import {emptyMarkers, randomId} from "../../../main/javascript";

class SetEmptySpace extends JsonVisitor<number> {
    protected async visitEmpty(empty: Empty, p: number): Promise<Json | undefined> {
        return this.produceJson<Empty>(
            await super.visitEmpty(empty, p), p,
            draft => {
                draft.prefix.whitespace = " "
            }
        )
    }
}

describe('visiting JSON', () => {
    test('calls super', async () => {
        const empty: Empty = {
            kind: JsonKind.Empty,
            id: randomId(),
            prefix: emptySpace,
            markers: emptyMarkers,
        }

        const json: JsonDocument = {
            kind: JsonKind.Document,
            id: randomId(),
            prefix: emptySpace,
            markers: emptyMarkers,
            sourcePath: "test.json",
            value: empty,
            eof: emptySpace,
        }

        const after = await new SetEmptySpace().visit<JsonDocument>(json, 0)
        expect(after!.value.prefix.whitespace).toBe(' ')
    });
});
