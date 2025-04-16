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
import {Empty, emptySpace, Json, JsonDocument, JsonKind, JsonVisitor} from "../../../main/javascript/json";
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
