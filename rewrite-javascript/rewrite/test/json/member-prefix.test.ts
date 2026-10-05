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
import {json, JsonParser, JsonVisitor} from "../../src/json";
import {detectIndent, Json} from "../../src/json/tree";
import {ExecutionContext, foundSearchResult, Recipe, SourceFile, TreeVisitor} from "../../src";
import {RecipeSpec} from "../../src/test";

// Mirrored by rewrite-json/src/test/java/org/openrewrite/json/JsonMemberPrefixTest.java

async function parse(text: string): Promise<Json.Document> {
    for await (const sf of new JsonParser().parse({text, sourcePath: "a.json"})) {
        return sf as SourceFile as Json.Document;
    }
    throw new Error("no source file produced");
}

function members(doc: Json.Document): Json.Member[] {
    return (doc.value as Json.Object).members.map(m => m.element as Json.Member);
}

describe("member prefix placement", () => {
    test("leading whitespace belongs to the member, not its key", async () => {
        const [member] = members(await parse('{\n  "_version": "1.65.0"\n}\n'));

        expect(member.prefix.whitespace).toBe("\n  ");
        expect(member.key.element.prefix.whitespace).toBe("");
    });

    test("every member in a multi-member object", async () => {
        const [first, second] = members(await parse('{\n  "a": 1,\n  "b": 2\n}'));

        expect(first.prefix.whitespace).toBe("\n  ");
        expect(first.key.element.prefix.whitespace).toBe("");
        expect(second.prefix.whitespace).toBe("\n  ");
        expect(second.key.element.prefix.whitespace).toBe("");
    });

    test("nested object members", async () => {
        const [outer] = members(await parse('{\n  "libs": {\n    "sap.makit": {}\n  }\n}'));
        const [inner] = (outer.value as Json.Object).members.map(m => m.element as Json.Member);

        expect(outer.prefix.whitespace).toBe("\n  ");
        expect(inner.prefix.whitespace).toBe("\n    ");
        expect(inner.key.element.prefix.whitespace).toBe("");
    });

    test("comments preceding a member belong to the member", async () => {
        const [member] = members(await parse('{\n  // a comment\n  "a": 1\n}'));

        expect(member.prefix.whitespace).toBe("\n  ");
        expect(member.prefix.comments.map(c => c.text)).toEqual([" a comment"]);
        expect(member.key.element.prefix.comments).toEqual([]);
    });

    test("detectIndent reads the member prefix", async () => {
        expect(detectIndent(await parse('{\n  "a": 1\n}'))).toBe("  ");
        expect(detectIndent(await parse('{\n\t"a": 1\n}'))).toBe("\t");
        expect(detectIndent(await parse('{"a": 1}'))).toBe("    ");
        expect(detectIndent(await parse('[\n  1\n]'))).toBe("  ");
    });
});

class MarkMembers extends Recipe {
    name = "org.openrewrite.json.test.mark-members";
    displayName = "Mark members";
    description = "Attaches a search result marker to every JSON object member.";

    async editor(): Promise<TreeVisitor<any, ExecutionContext>> {
        return new class extends JsonVisitor<ExecutionContext> {
            protected override async visitMember(member: Json.Member, ctx: ExecutionContext): Promise<Json | undefined> {
                return foundSearchResult(await super.visitMember(member, ctx) as Json.Member);
            }
        };
    }
}

describe("marking a member", () => {
    const spec = new RecipeSpec();
    spec.recipe = new MarkMembers();

    test("renders the marker against the key, not the end of the previous line", () => spec.rewriteRun(
        json(
            `{
  "libs": {
    "sap.makit": {}
  }
}`,
            `{
  /*~~>*/"libs": {
    /*~~>*/"sap.makit": {}
  }
}`
        )
    ));
});
