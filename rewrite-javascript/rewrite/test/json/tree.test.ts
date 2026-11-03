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
    test("decodes the escapes in a quoted key, whichever parser built the tree", async () => {
        const member = await firstMember(String.raw`{"\\.(css|less)$": "identity-obj-proxy"}`);
        expect(getMemberKeyName(member)).toBe(String.raw`\.(css|less)$`);

        const key = member.key.element as Json.Literal;
        const javaParsed = {...member, key: {...member.key, element: {...key, value: String.raw`\\.(css|less)$`}}};
        expect(getMemberKeyName(javaParsed)).toBe(String.raw`\.(css|less)$`);
    });
});
