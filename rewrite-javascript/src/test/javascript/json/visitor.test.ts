import {Document, Empty, EmptySpace, Json, JsonKind, JsonVisitor, space} from "../../../main/javascript/json";
import {EmptyMarkers, randomId} from "../../../main/javascript";

class SetEmptySpace extends JsonVisitor<number> {
    protected async visitEmpty(empty: Empty, p: number): Promise<Json | undefined> {
        return this.produceJson<Empty>(
            await super.visitEmpty(empty, p) as Empty, p,
            draft => {
                draft.prefix = space(" ")
            }
        )
    }
}

describe('visiting JSON', () => {
    test('calls super', async () => {
        const empty: Empty = {
            kind: JsonKind.Empty,
            id: randomId(),
            prefix: EmptySpace,
            markers: EmptyMarkers,
        }

        const json: Document = {
            kind: JsonKind.Document,
            id: randomId(),
            prefix: EmptySpace,
            markers: EmptyMarkers,
            sourcePath: "test.json",
            value: empty,
            eof: EmptySpace,
        }

        const after = await new SetEmptySpace().visit<Document>(json, 0)
        expect(after!.value.prefix.whitespace).toBe(' ')
    });
});
