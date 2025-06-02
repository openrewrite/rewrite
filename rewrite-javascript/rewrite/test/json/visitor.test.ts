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
import {emptySpace, Json, JsonVisitor} from "../../src/json";
import {emptyMarkers, randomId, Tree} from "../../src";

class SetEmptySpace extends JsonVisitor<number> {
    protected async visitEmpty(empty: Json.Empty, p: number): Promise<Json | undefined> {
        return this.produceJson<Json.Empty>(
            await super.visitEmpty(empty, p), p,
            draft => {
                draft.prefix.whitespace = " "
            }
        )
    }
}

describe('visiting JSON', () => {

    test('preVisit', async () => {
        const partialDocument = {kind: Json.Kind.Document};
        const visitor = new class extends JsonVisitor<number> {
            protected async preVisit(j: Json, p: number): Promise<Json | undefined> {
                return this.produceJson<Json>(j, p, async draft => {
                    draft.id = randomId();
                });
            }
        }
        expect((await visitor.visit(partialDocument as Tree, 0))?.id).toBeDefined();
    });

    test('calls super', async () => {
        const empty: Json.Empty = {
            kind: Json.Kind.Empty,
            id: randomId(),
            prefix: emptySpace,
            markers: emptyMarkers,
        }

        const json: Json.Document = {
            kind: Json.Kind.Document,
            id: randomId(),
            prefix: emptySpace,
            markers: emptyMarkers,
            sourcePath: "test.json",
            value: empty,
            eof: emptySpace,
        }

        const after = await new SetEmptySpace().visit<Json.Document>(json, 0)
        expect(after!.value.prefix.whitespace).toBe(' ')
    });
});
