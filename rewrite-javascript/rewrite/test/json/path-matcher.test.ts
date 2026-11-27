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
import {emptySpace, Json, JsonParser, JsonPathMatcher, JsonVisitor} from "../../src/json";
import {Cursor, SourceFile, TreePrinters} from "../../src";
import {trimIndent} from "../../src/util";

// Mirrors rewrite-json/src/test/java/org/openrewrite/json/JsonPathMatcherTest.java

async function parse(text: string): Promise<Json.Document> {
    for await (const sf of new JsonParser().parse({text, sourcePath: "a.json"})) {
        return sf as SourceFile as Json.Document;
    }
    throw new Error("no source file produced");
}

async function printTrimmed(tree: Json, cursor: Cursor): Promise<string> {
    return trimIndent(await TreePrinters.printer(cursor).print(tree, undefined, cursor));
}

async function visit(jsonPath: string, source: string): Promise<string[]> {
    const matcher = new JsonPathMatcher(jsonPath);
    const matches: string[] = [];
    await new class extends JsonVisitor<string[]> {
        protected async visitMember(member: Json.Member, p: string[]): Promise<Json | undefined> {
            const e = (await super.visitMember(member, p))!;
            if (matcher.matches(this.cursor)) {
                p.push(await printTrimmed(e, this.cursor.parent!));
            }
            return e;
        }

        protected async visitObject(obj: Json.Object, p: string[]): Promise<Json | undefined> {
            const e = (await super.visitObject(obj, p))!;
            if (matcher.matches(this.cursor)) {
                p.push(await printTrimmed(e, this.cursor.parent!));
            }
            return e;
        }

        protected async visitArray(array: Json.Array, p: string[]): Promise<Json | undefined> {
            const e = (await super.visitArray(array, p))!;
            if (matcher.matches(this.cursor)) {
                p.push(await printTrimmed({...e, prefix: emptySpace}, this.cursor.parent!));
            }
            return e;
        }

        protected async visitLiteral(literal: Json.Literal, p: string[]): Promise<Json | undefined> {
            const e = (await super.visitLiteral(literal, p))!;
            if (matcher.matches(this.cursor)) {
                p.push(await printTrimmed({...e, prefix: emptySpace}, this.cursor.parent!));
            }
            return e;
        }
    }().visit(await parse(source), matches);
    return matches;
}

async function assertMatched(jsonPath: string, source: string, expected: string[]) {
    expect(await visit(jsonPath, source)).toEqual(expected.map(trimIndent));
}

async function assertNotMatched(jsonPath: string, source: string) {
    expect(await visit(jsonPath, source)).toEqual([]);
}

const simple = `
    {
      "root": {
        "literal": "$.root.literal",
        "object": {
          "literal": "$.root.object.literal",
          "list": [{
            "literal": "$.root.object.list[0]"
          }]
        },
        "list": [{
          "literal": "$.root.list[0]"
        }]
      }
    }
`;

const listOfScalars = `
    {
      "list": [
        "item 1",
        "item 2",
        "item 3"
      ]
    }
`;

const sliceList = `
    {
      "list": [
        {
          "item1": "index0",
          "property": "property"
        },
        {
          "item2": "index1",
          "property": "property"
        },
        {
          "item3": "index2",
          "property": "property"
        }
      ]
    }
`;

const complex = `
    {
      "literal": "$.literal",
      "object": {
        "literal": "$.object.literal",
        "object": {
          "literal": "$.object.object.literal"
        },
        "list": [{
          "literal": "$.object.list[0].literal"
        }]
      },
      "literals": [
        "$.literal[0]"
      ],
      "objects": {
        "literal": "$.objects.literal",
        "object": {
          "literal": "$.objects.object.literal",
          "object": {
            "literal": "$.objects.object.object.literal"
          },
          "list": [{
            "literal": "$.objects.object.list[0].literal"
          }]
        }
      },
      "lists": [{
        "list": [{
          "object": {
            "literal": "$.lists[0].list[0].object.literal",
            "object": {
              "literal": "$.lists[0].list[0].object.object.literal"
            },
            "list": [{
              "literal": "$.lists[0].list[0].object.list[0].literal"
            }]
          }
        }]
      }]
    }
`;

const numbers = `
    {
      "list": [
        {"n": 1},
        {"n": 2},
        {"n": -1},
        {"n": 1.0},
        {"n": 1000},
        {"n": 3000000000},
        {"n": 12345678901234567890},
        {"n": "1"}
      ]
    }
`;

const rootObject = `
    "object": {
      "literal": "$.root.object.literal",
      "list": [{
        "literal": "$.root.object.list[0]"
      }]
    }
`;

const rootObjectList = `
    "list": [{
      "literal": "$.root.object.list[0]"
    }]
`;

const slice0 = `
    {
      "item1": "index0",
      "property": "property"
    }
`;

const slice1 = `
    {
      "item2": "index1",
      "property": "property"
    }
`;

const slice2 = `
    {
      "item3": "index2",
      "property": "property"
    }
`;

const bar = `
    {
      "foo": {
        "bar": [
          {
            "type": "type",
            "group": "group",
            "category": "other",
            "types": "something",
            "pattern": "p1"
          },
          {
            "type": "type",
            "group": "group",
            "category": "match",
            "types": "something",
            "pattern": "p2"
          }
        ]
      }
    }
`;

describe("JsonPathMatcher", () => {
    test("does not match missing property", () =>
        assertNotMatched("$.none", simple));

    test("find scope of object", () =>
        assertMatched("$.root.object[?(@.literal == '$.root.object.literal')]", simple, [rootObject]));

    test("find literal in object", () =>
        assertMatched("$.root.object[?(@.literal == '$.root.object.literal')].literal", simple,
            ['"literal": "$.root.object.literal"']));

    test("boolean literal matching", async () => {
        await assertMatched("$[?(@.bool == true)]", '{\n  "bool": true\n}', ['{\n  "bool": true\n}']);
        await assertNotMatched("$[?(@.bool == true)]", '{\n  "bool": false\n}');
    });

    test("numeric equality", async () => {
        await assertMatched("$.list[?(@.n == 1)]", numbers, ['{"n": 1}', '{"n": 1.0}']);
        await assertMatched("$.list[?(@.n == -1)]", numbers, ['{"n": -1}']);
        await assertMatched("$.list[?(@.n == 3000000000)]", numbers, ['{"n": 3000000000}']);
        await assertMatched("$.list[?(@.n == 12345678901234567890)]", numbers, ['{"n": 12345678901234567890}']);
        await assertMatched("$.list[?(@.n == 1e3)]", numbers, ['{"n": 1000}']);
    });

    test("numeric inequality", () =>
        assertMatched("$.list[?(@.n != 1)]", numbers, [
            '{"n": 2}',
            '{"n": -1}',
            '{"n": 1000}',
            '{"n": 3000000000}',
            '{"n": 12345678901234567890}',
            '{"n": "1"}'
        ]));

    test("numeric equality on scalar list", () =>
        assertMatched("$.list[?(@ == 2)]", '{\n  "list": [1, 2, 3]\n}', ["2"]));

    test("numeric equality is strict about strings", async () => {
        await assertNotMatched("$[?(@.n == 1)]", '{\n  "n": "1"\n}');
        await assertNotMatched("$[?(@.n == '1')]", '{\n  "n": 1\n}');
    });

    test("multiple wildcards", () =>
        assertMatched("$.*.*", simple, [
            '"literal": "$.root.literal"',
            rootObject,
            `
                "list": [{
                  "literal": "$.root.list[0]"
                }]
            `
        ]));

    test("all properties in key from root", () =>
        assertMatched("$.*.literal", simple, ['"literal": "$.root.literal"']));

    test("match object at root", () =>
        assertMatched("$.root.object", simple, [rootObject]));

    test("match literal", () =>
        assertMatched("$.root.literal", simple, ['"literal": "$.root.literal"']));

    test("dot operator into object", () =>
        assertMatched("$.root.object.literal", simple, ['"literal": "$.root.object.literal"']));

    test("dot operator by bracket name", () =>
        assertMatched("$.['root'].['object'].['literal']", simple, ['"literal": "$.root.object.literal"']));

    test("bracket name at root", () =>
        assertMatched("['root'].['object']", simple, [rootObject]));

    test("relative path", () =>
        assertMatched(".literal", simple, [
            '"literal": "$.root.literal"',
            '"literal": "$.root.object.literal"',
            '"literal": "$.root.object.list[0]"',
            '"literal": "$.root.list[0]"'
        ]));

    test("contains in list", () =>
        assertMatched("$.root.object[?(@.literal contains 'object')].list", simple, [rootObjectList]));

    test("contains in lhs string", () =>
        assertMatched("$.root.object[?(@.literal contains 'literal')].list", simple, [rootObjectList]));

    test("contains in rhs string", () =>
        assertMatched("$.root.object[?('$.root.object.literal' contains @.literal)].list", simple, [rootObjectList]));

    test("return scope of member on binary expression", () =>
        assertMatched("$.root.object[?($.root.literal == '$.root.literal')].list", simple, [rootObjectList]));

    test("match by condition and item in list", () =>
        assertMatched("$.root.object[?($.root.literal == '$.root.literal' && @.literal contains 'literal')].list",
            simple, [rootObjectList]));

    test("recurse to match properties", () =>
        assertMatched("$..object.literal", complex, [
            '"literal": "$.object.literal"',
            '"literal": "$.object.object.literal"',
            '"literal": "$.objects.object.literal"',
            '"literal": "$.objects.object.object.literal"',
            '"literal": "$.lists[0].list[0].object.literal"',
            '"literal": "$.lists[0].list[0].object.object.literal"'
        ]));

    test("recurse from scope of object", () =>
        assertMatched("$.object..literal", complex, [
            '"literal": "$.object.literal"',
            '"literal": "$.object.object.literal"',
            '"literal": "$.object.list[0].literal"'
        ]));

    test("first n elements", () =>
        assertMatched("$.list[:1]", sliceList, [slice0]));

    test("last n elements", () =>
        assertMatched("$.list[-1:]", sliceList, [slice2]));

    test("from start to end position", () =>
        assertMatched("$.list[0:1]", sliceList, [slice0, slice1]));

    test("all elements from start position", () =>
        assertMatched("$.list[1:]", sliceList, [slice1, slice2]));

    test("all elements", () =>
        assertMatched("$.list[*]", sliceList, [slice0, slice1, slice2]));

    test("bracket operator by names", () =>
        assertMatched("$.root['literal', 'object']", simple, ['"literal": "$.root.literal"', rootObject]));

    test("does not match index", () =>
        assertNotMatched("$.list[4]", sliceList));

    test("bracket operator by indexes", () =>
        assertMatched("$.list[0, 1]", sliceList, [slice0, slice1]));

    test("bracket operator by indexes with multiple digits", () =>
        assertMatched("$.list[10]", `
            {
              "list": [
                {"item1": "index0"},
                {"item2": "index1"},
                {"item3": "index2"},
                {"item4": "index3"},
                {"item5": "index4"},
                {"item6": "index5"},
                {"item7": "index6"},
                {"item8": "index7"},
                {"item9": "index8"},
                {"item10": "index9"},
                {"item11": "index10"},
                {"item12": "index11"},
                {"item13": "index12"},
                {"item3": "index13"}
              ]
            }
        `, ['{"item11": "index10"}']));

    test("does not match wrong value", () =>
        assertNotMatched("$..list[?(@.literal == 'no-match')].literal", complex));

    test("filter on property with equality", () =>
        assertMatched("$..list[?(@.literal == '$.lists[0].list[0].object.list[0].literal')].literal", complex,
            ['"literal": "$.lists[0].list[0].object.list[0].literal"']));

    test("does not match wrong and", () =>
        assertNotMatched(
            "$..list[?(@.literal == 'no-match' && @.literal == '$.lists[0].list[0].object.list[0].literal')].literal",
            complex));

    test("filter on property with and", () =>
        assertMatched(
            "$..list[?($.literal == '$.literal' && @.literal == '$.lists[0].list[0].object.list[0].literal')].literal",
            complex, ['"literal": "$.lists[0].list[0].object.list[0].literal"']));

    test("does not match wrong or", () =>
        assertNotMatched("$..list[?(@.literal == 'no-match-1' || @.literal == 'no-match-2')].literal", complex));

    test("filter on property with or", () =>
        assertMatched(
            "$..list[?(@.literal == 'no-match' || @.literal == '$.lists[0].list[0].object.list[0].literal')].literal",
            complex, ['"literal": "$.lists[0].list[0].object.list[0].literal"']));

    test("unary expression by at", () =>
        assertMatched("$.list[?(@ == 'item 1')]", listOfScalars, ['"item 1"']));

    test("quoted names and filter strings match by the text the escapes stand for", async () => {
        const jest = `
            {
              "moduleNameMapper": {
                "\\\\.(css|less)$": "identity-obj-proxy"
              },
              "testRegex": ["\\\\.test\\\\.js$"]
            }
        `;
        await assertMatched("$.moduleNameMapper['\\\\.(css|less)$']", jest,
            ['"\\\\.(css|less)$": "identity-obj-proxy"']);
        await assertMatched("$.testRegex[?(@ == '\\\\.test\\\\.js$')]", jest,
            ['"testRegex": ["\\\\.test\\\\.js$"]']);
    });

    test("unary expression by bracket operator", () =>
        assertMatched("$.list[?(@.['item1'])]", sliceList, [slice0]));

    test("unary expression by scope", () =>
        assertMatched("$.list[?(@.property)]", sliceList, [slice0, slice1, slice2]));

    test("unary expression by value", () =>
        assertMatched("$.list[?(@.property == 'property')]", sliceList, [slice0, slice1, slice2]));

    test("filter on property with matches", () =>
        assertMatched("$..list[?(@.literal =~ '.*objects.*')].literal", complex,
            ['"literal": "$.objects.object.list[0].literal"']));

    test("recursive descent to filter", () =>
        assertMatched("$..[?(@.literal =~ '.*objects.object.literal.*')].literal", complex,
            ['"literal": "$.objects.object.literal"']));

    test("nested sequences", () =>
        assertMatched("$..list[*].[?(@.literal == '$.lists[0].list[0].object.list[0].literal')].literal", complex,
            ['"literal": "$.lists[0].list[0].object.list[0].literal"']));

    test("matches literal in list by filter condition", () =>
        assertMatched("$.list.*[?(@.item1 == 'index0')].item1", sliceList, ['"item1": "index0"']));

    test("matches list with by filter condition in list", () =>
        assertMatched("$.list.*[?(@.item3 == 'index2')]", sliceList, [slice2]));

    test("multiple binary expressions", () =>
        assertMatched(
            "$.foo.bar[?(@.types == 'something' && @.group == 'group' && @.category == 'match' && @.type == 'type')].pattern",
            bar, ['"pattern": "p2"']));

    test("no match with binary expressions", () =>
        assertMatched(
            "$.foo.bar[?(@.types == 'something' && @.group == 'group' && @.category == 'nomatch' && @.type == 'type')].pattern",
            bar, []));

    test("negate unary existence", () =>
        assertMatched("$.list[?(!@.item1)]", sliceList, [slice1, slice2]));

    test("negate unary existence matches none", () =>
        assertNotMatched("$.list[?(!@.property)]", sliceList));

    test("negate equality expression", () =>
        assertNotMatched("$.list[?(!(@.property == 'property'))]", sliceList));

    test("negate equality expression partial match", () =>
        assertMatched("$.list[?(!(@.item1 == 'index0'))]", sliceList, [slice1, slice2]));

    test("negate with logical and", () =>
        assertMatched("$.list[?(!@.item1 && @.property == 'property')]", sliceList, [slice1, slice2]));

    test("dollar to match the root", async () => {
        // given
        const matcher = new JsonPathMatcher("$");
        const results: Json.Object[] = [];

        // when
        await new class extends JsonVisitor<Json.Object[]> {
            protected async visitObject(obj: Json.Object, p: Json.Object[]): Promise<Json | undefined> {
                const e = await super.visitObject(obj, p);
                if (matcher.matches(this.cursor)) {
                    p.push(obj);
                }
                return e;
            }
        }().visit(await parse(simple), results);

        // then
        expect(results).toHaveLength(1);
    });

    test("return results with visit document", async () => {
        // given
        const matcher = new JsonPathMatcher("$.root.literal");
        const results: Json[] = [];

        // when
        await new class extends JsonVisitor<Json[]> {
            protected async visitDocument(document: Json.Document, p: Json[]): Promise<Json | undefined> {
                if (matcher.find(this.cursor) !== undefined) {
                    p.push(document);
                }
                return super.visitDocument(document, p);
            }
        }().visit(await parse(simple), results);

        // then
        expect(results).toHaveLength(1);
    });

    test("a wildcard after a key selects the members of its object", () =>
        assertMatched("$.root.*", simple, [
            '"literal": "$.root.literal"',
            rootObject,
            `
                "list": [{
                  "literal": "$.root.list[0]"
                }]
            `
        ]));

    test("a wildcard after a nested key selects the members of its object", () =>
        assertMatched("$.root.object.*", simple, ['"literal": "$.root.object.literal"', rootObjectList]));

    test("a wildcard after a key selects the elements of its array", () =>
        assertMatched("$.list.*", sliceList, [slice0, slice1, slice2]));

    test("a wildcard after quoted keys selects each member", () =>
        assertMatched("$['sap.ui5'].dependencies.libs.*", `
            {
              "sap.ui5": {
                "dependencies": {
                  "libs": {
                    "sap.m": {},
                    "sap.f": {}
                  }
                }
              }
            }
        `, ['"sap.m": {}', '"sap.f": {}']));

    test("a wildcard after dotted keys selects each member of the nested object only", () =>
        assertMatched("$.sap.ui5.dependencies.libs.*", `
            {
              "sap.ui5": {
                "dependencies": {
                  "libs": {"sap.m": {}, "sap.ui.core": {}}
                }
              },
              "sap": {
                "ui5": {
                  "dependencies": {
                    "libs": {"sap.f": {}}
                  }
                }
              }
            }
        `, ['"sap.f": {}']));

    test("a recursive wildcard selects every member and element", () =>
        assertMatched("$..*", '{"a": {"b": 1}, "c": [{"d": 2}]}',
            ['"b": 1', '"a": {"b": 1}', '"d": 2', '{"d": 2}', '"c": [{"d": 2}]']));

    test("a scoped recursive wildcard selects every descendant", () =>
        assertMatched("$.a..*", '{"a": {"b": 1, "c": {"d": 2}, "l": [{"x": "y"}, "z"]}, "other": 3}',
            ['"b": 1', '"d": 2', '"c": {"d": 2}', '"x": "y"', '{"x": "y"}', '"z"', '"l": [{"x": "y"}, "z"]']));

    test("quoted names and filter strings match surrogate escapes", async () => {
        const surrogates = `
            {
              "\\uD800": "unpaired",
              "strings": ["a\\ud800b"],
              "emoji": ["\\ud83d\\ude00"]
            }
        `;
        await assertMatched("$['\\\\ud800']", surrogates, ['"\\uD800": "unpaired"']);
        await assertMatched("$.strings[?(@ == 'a\\\\ud800b')]", surrogates, ['"strings": ["a\\ud800b"]']);
        await assertMatched("$.strings[?(@ =~ '.*ud800.*')]", surrogates, ['"a\\ud800b"']);
        await assertMatched("$.emoji[?(@ == '\u{1F600}')]", surrogates, ['"emoji": ["\\ud83d\\ude00"]']);
    });

    test("rejects an invalid expression on first use", async () => {
        // given
        const matcher = new JsonPathMatcher("$.root[");
        const doc = await parse(simple);

        // when
        const match = () => matcher.matches(new Cursor(doc, new Cursor("root")));

        // then
        expect(match).toThrow(/Syntax error/);
    });
});
