/*
 * Copyright 2026 the original author or authors.
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
import {JsonParser} from "../../src/json";
import {getMemberKeyName, Json} from "../../src/json/tree";
import {SourceFile} from "../../src";

async function firstMember(text: string): Promise<Json.Member> {
    for await (const sf of new JsonParser().parse({text, sourcePath: "a.json"})) {
        return ((sf as SourceFile as Json.Document).value as Json.Object).members[0].element as Json.Member;
    }
    throw new Error("no source file produced");
}

describe("getMemberKeyName", () => {
    test("decodes a quoted key from its source, even where its value keeps the escapes", async () => {
        const member = await firstMember(String.raw`{"\\.(css|less)$": "identity-obj-proxy"}`);
        expect(getMemberKeyName(member)).toBe(String.raw`\.(css|less)$`);

        const key = member.key.element as Json.Literal;
        const serializedWithEscapes = {...member, key: {...member.key, element: {...key, value: String.raw`\\.(css|less)$`}}};
        expect(getMemberKeyName(serializedWithEscapes)).toBe(String.raw`\.(css|less)$`);
    });

    test("takes the value of a JSON5 key that JSON.parse rejects", async () => {
        const member = await firstMember(`{"a": 1}`);
        const key = member.key.element as Json.Literal;
        const json5 = {...member, key: {...member.key, element: {...key, source: String.raw`'\x41'`, value: "A"}}};
        expect(getMemberKeyName(json5)).toBe("A");
    });
});
