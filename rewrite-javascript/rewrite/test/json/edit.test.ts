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
import {
    appendElement,
    elementsOf,
    emptyArray,
    emptyObject,
    fromPlain,
    insertElement,
    isArray,
    isEmptyContainer,
    isMember,
    isObject,
    Json,
    json,
    JsonParser,
    JsonVisitor,
    mapElements,
    jsonLiteral,
    findMember,
    membersOf,
    pathOf,
    removeAt,
    removeElements,
    removeMember,
    setAt,
    setMember,
    space,
    toPlain,
    updateAt,
    updateMember,
    valueAt
} from "../../src/json";
import {ExecutionContext, Recipe, SourceFile, TreePrinters, TreeVisitor} from "../../src";
import {RecipeSpec} from "../../src/test";

async function parse(text: string): Promise<Json.Document> {
    for await (const sf of new JsonParser().parse({text, sourcePath: "a.json"})) {
        return sf as SourceFile as Json.Document;
    }
    throw new Error("no source file produced");
}

function root(doc: Json.Document): Json.Object {
    return doc.value as Json.Object;
}

async function editRoot(text: string, edit: (obj: Json.Object) => Json.Object): Promise<string> {
    const doc = await parse(text);
    return TreePrinters.print({...doc, value: edit(root(doc))} as Json.Document);
}

async function editDocument(text: string, edit: (doc: Json.Document) => Json.Document): Promise<string> {
    return TreePrinters.print(edit(await parse(text)));
}

async function editArray(text: string, edit: (arr: Json.Array) => Json.Array): Promise<string> {
    const doc = await parse(text);
    return TreePrinters.print({...doc, value: edit(doc.value as Json.Array)} as Json.Document);
}

const isValue = (text: string) => (v: Json.Value) => toPlain(v) === text;

function withIndentationOnKeys(doc: Json.Document): Json.Document {
    const move = (value: Json.Value): Json.Value => {
        if (!isObject(value)) {
            return value;
        }
        return {
            ...value,
            members: value.members.map(padded => {
                const m = padded.element;
                if (!isMember(m)) {
                    return padded;
                }
                const key = {...m.key.element, prefix: m.prefix};
                const element: Json.Member = {...m, prefix: {...m.prefix, whitespace: "", comments: []}, key: {...m.key, element: key}, value: move(m.value)};
                return {...padded, element};
            })
        };
    };
    return {...doc, value: move(doc.value)};
}

const packageJson = `{
  "name": "app",
  "version": "1.0.0",
  "dependencies": {
    "express": "^4.18.0",
    "react": "^18.2.0"
  }
}
`;

describe("reading", () => {
    test("membersOf skips the Empty of a trailing comma", async () => {
        // given
        const obj = root(await parse(`{"a": 1, "b": 2,}`));

        // when
        const keys = membersOf(obj).map(m => (m.key.element as Json.Literal).value);

        // then
        expect(keys).toEqual(["a", "b"]);
    });

    test("findMember matches a key with escapes by its decoded name", async () => {
        // given
        const obj = root(await parse(String.raw`{"\\.(css|less)$": "identity-obj-proxy", "a\"b": 1}`));

        // when
        const proxy = findMember(obj, String.raw`\.(css|less)$`);
        const quoted = findMember(obj, `a"b`);

        // then
        expect(toPlain(proxy!.value)).toBe("identity-obj-proxy");
        expect(toPlain(quoted!.value)).toBe(1);
        expect(findMember(obj, "missing")).toBeUndefined();
    });

    test("valueAt walks nested objects", async () => {
        // given
        const doc = await parse(packageJson);

        // when
        const react = valueAt(doc, ["dependencies", "react"]);

        // then
        expect(toPlain(react!)).toBe("^18.2.0");
        expect(valueAt(doc, ["dependencies", "vue"])).toBeUndefined();
        expect(valueAt(doc, ["name", "nested"])).toBeUndefined();
        expect(valueAt(root(doc), [])).toBe(root(doc));
    });

    test("toPlain reads literals from their source", async () => {
        // given
        const doc = await parse(`{"n": 5000, "f": 1.5e3, "t": true, "z": null, "s": "a\\nb", "arr": [1, "x", {}], "o": {"k": []}}`);
        const n = findMember(root(doc), "n")!.value as Json.Literal;

        // when
        const plain = toPlain(doc);
        const typedAsString = toPlain({...n, value: "5000"} as Json.Literal);

        // then
        expect(plain).toEqual({n: 5000, f: 1500, t: true, z: null, s: "a\nb", arr: [1, "x", {}], o: {k: []}});
        expect(typedAsString).toBe(5000);
    });

    test("isEmptyContainer recognizes the single-Empty form", async () => {
        // given
        const obj = root(await parse(`{"o": {}, "a": [ ], "full": {"x": 1}, "list": [1]}`));

        // then
        expect(isEmptyContainer(findMember(obj, "o")!.value as Json.Object)).toBe(true);
        expect(isEmptyContainer(findMember(obj, "a")!.value as Json.Array)).toBe(true);
        expect(isEmptyContainer(findMember(obj, "full")!.value as Json.Object)).toBe(false);
        expect(isEmptyContainer(findMember(obj, "list")!.value as Json.Array)).toBe(false);
        expect(isEmptyContainer(emptyObject())).toBe(true);
        expect(isEmptyContainer(emptyArray())).toBe(true);
    });
});

describe("setMember", () => {
    test("appends to a 2-space indented object", async () => {
        expect(await editRoot(packageJson, obj => setMember(obj, "private", jsonLiteral(true)))).toBe(`{
  "name": "app",
  "version": "1.0.0",
  "dependencies": {
    "express": "^4.18.0",
    "react": "^18.2.0"
  },
  "private": true
}
`);
    });

    test("prepends to a 4-space indented object", async () => {
        expect(await editRoot(`{
    "a": 1,
    "b": 2
}`, obj => setMember(obj, "_version", jsonLiteral("1.65.0"), {position: "first"}))).toBe(`{
    "_version": "1.65.0",
    "a": 1,
    "b": 2
}`);
    });

    test("appends to a tab indented object", async () => {
        expect(await editRoot(`{
\t"a": {
\t\t"b": 1
\t}
}`, obj => updateMember(obj, "a", a => setMember(a as Json.Object, "c", jsonLiteral(2))))).toBe(`{
\t"a": {
\t\t"b": 1,
\t\t"c": 2
\t}
}`);
    });

    test("inserts in sorted place into a dependencies section", async () => {
        // when
        const printed = await editDocument(packageJson, doc => setAt(doc, ["dependencies", "lodash"], jsonLiteral("^4.17.21"), {position: "sorted"}));

        // then
        expect(printed).toBe(`{
  "name": "app",
  "version": "1.0.0",
  "dependencies": {
    "express": "^4.18.0",
    "lodash": "^4.17.21",
    "react": "^18.2.0"
  }
}
`);
    });

    test("sorted insertion goes first and last where it belongs", async () => {
        // when
        const printed = await editDocument(packageJson, doc => {
            const scoped = setAt(doc, ["dependencies", "@babel/core"], jsonLiteral("^7.0.0"), {position: "sorted"});
            return setAt(scoped, ["dependencies", "zod"], jsonLiteral("^3.0.0"), {position: "sorted"});
        });

        // then
        expect(printed).toBe(`{
  "name": "app",
  "version": "1.0.0",
  "dependencies": {
    "@babel/core": "^7.0.0",
    "express": "^4.18.0",
    "react": "^18.2.0",
    "zod": "^3.0.0"
  }
}
`);
    });

    test("replaces the value of an existing member in place, keeping the space after the colon", async () => {
        expect(await editRoot(packageJson, obj => setMember(obj, "version", jsonLiteral("2.0.0"), {position: "first"}))).toBe(`{
  "name": "app",
  "version": "2.0.0",
  "dependencies": {
    "express": "^4.18.0",
    "react": "^18.2.0"
  }
}
`);
    });

    test("returns the same object when the value is the same", async () => {
        // given
        const obj = root(await parse(`{"a": 1}`));

        // when
        const edited = setMember(obj, "a", findMember(obj, "a")!.value);

        // then
        expect(edited).toBe(obj);
    });

    test("fills an empty object on its own line with the given indent", async () => {
        expect(await editDocument(`{
  "name": "app",
  "dependencies": {}
}`, doc => setAt(doc, ["dependencies", "react"], jsonLiteral("^18.2.0")))).toBe(`{
  "name": "app",
  "dependencies": {
    "react": "^18.2.0"
  }
}`);
    });

    test("fills an empty object whose braces are already on separate lines", async () => {
        expect(await editRoot(`{
  "dependencies": {
  }
}`, obj => updateMember(obj, "dependencies", d => setMember(d as Json.Object, "react", jsonLiteral("^18.2.0"), {indent: "  "})))).toBe(`{
  "dependencies": {
    "react": "^18.2.0"
  }
}`);
    });

    test("fills an empty object inline when no indent is known", async () => {
        expect(await editRoot(`{}`, obj => setMember(obj, "a", jsonLiteral(1)))).toBe(`{"a": 1}`);
    });

    test("fills an empty root document with its detected indent", async () => {
        expect(await editDocument(`{}`, doc => setAt(doc, ["a"], jsonLiteral(1)))).toBe(`{
    "a": 1
}`);
    });

    test("fills an empty object in a document on one line inline", async () => {
        expect(await editDocument(`{"a": {}}`, doc => setAt(doc, ["a", "b"], jsonLiteral(1)))).toBe(`{"a": {"b": 1}}`);
        expect(await editDocument(`{"a": 1}`, doc => setAt(doc, ["x", "b"], jsonLiteral(1), {createMissing: true}))).toBe(`{"a": 1, "x": {"b": 1}}`);
    });

    test("fills an empty object that holds a comment", async () => {
        expect(await editDocument(`{
  "a": {
    // nothing yet
  }
}`, doc => setAt(doc, ["a", "b"], jsonLiteral(1)))).toBe(`{
  "a": {
    "b": 1
    // nothing yet
  }
}`);
    });

    test("keeps a comment after the last member on its line", async () => {
        expect(await editRoot(`{
  "a": 1 // about a
}`, obj => setMember(obj, "b", jsonLiteral(2)))).toBe(`{
  "a": 1, // about a
  "b": 2
}`);
    });

    test("inserts before a member with a comment in its prefix on its own line", async () => {
        expect(await editRoot(`{
  "a": 1, // about a
  "c": 3
}`, obj => setMember(obj, "b", jsonLiteral(2), {position: "sorted"}))).toBe(`{
  "a": 1, // about a
  "b": 2,
  "c": 3
}`);
    });

    test("inserts before a comment on the line above the next member", async () => {
        expect(await editRoot(`{
  "a": 1,
  // about c
  "c": 3
}`, obj => setMember(obj, "b", jsonLiteral(2), {position: "sorted"}))).toBe(`{
  "a": 1,
  "b": 2,
  // about c
  "c": 3
}`);
    });

    test("prepends below a comment on the line of the opening brace", async () => {
        expect(await editRoot(`{ // config
  "a": 1
}`, obj => setMember(obj, "b", jsonLiteral(2), {position: "first"}))).toBe(`{ // config
  "b": 2,
  "a": 1
}`);
        expect(await editRoot(`{ // config
  "a": 1
}`, obj => setMember(obj, "b", jsonLiteral(2)))).toBe(`{ // config
  "a": 1,
  "b": 2
}`);
    });

    test("adds to a single-member object", async () => {
        expect(await editRoot(`{"a": 1}`, obj => setMember(obj, "b", jsonLiteral(2)))).toBe(`{"a": 1, "b": 2}`);
    });

    test("prepends to an object on one line", async () => {
        expect(await editRoot(`{"a": 1}`, obj => setMember(obj, "_version", jsonLiteral("2"), {position: "first"}))).toBe(`{"_version": "2", "a": 1}`);
    });

    test("leaves a member that already holds the same data", async () => {
        // given
        const obj = root(await parse(`{"a": 1.0, "b": {"c": [1]}}`));

        // when
        const literalEdit = setMember(obj, "a", jsonLiteral(1));
        const objectEdit = setMember(obj, "b", fromPlain({c: [1]}, "  "));

        // then
        expect(literalEdit).toBe(obj);
        expect(objectEdit).toBe(obj);
    });

    test("leaves an object that holds the same members in another order", async () => {
        // given
        const obj = root(await parse(`{"cfg": {"a": 1, "b": 2}}`));

        // when
        const edited = setMember(obj, "cfg", fromPlain({b: 2, a: 1}, "  "));

        // then
        expect(edited).toBe(obj);
    });

    test("replaces an integer that differs only beyond double precision", async () => {
        expect(await editRoot(`{"v": 9007199254740993}`, obj => setMember(obj, "v", jsonLiteral(9007199254740992)))).toBe(`{"v": 9007199254740992}`);
    });

    test("keeps a trailing comma", async () => {
        expect(await editRoot(`{
  "a": 1,
}`, obj => setMember(obj, "b", jsonLiteral(2)))).toBe(`{
  "a": 1,
  "b": 2,
}`);
    });

    test("escapes the key and the value", async () => {
        expect(await editRoot(`{
  "a": 1
}`, obj => setMember(obj, String.raw`\.(css|less)$`, jsonLiteral(`say "hi"`)))).toBe(String.raw`{
  "a": 1,
  "\\.(css|less)$": "say \"hi\""
}`);
    });

    test("copies the indentation of a sibling whose indentation is on its key", async () => {
        // given
        const doc = withIndentationOnKeys(await parse(packageJson));

        // when
        const printed = await TreePrinters.print(setAt(doc, ["dependencies", "lodash"], jsonLiteral("^4.17.21"), {position: "sorted"}));

        // then
        expect(printed).toBe(`{
  "name": "app",
  "version": "1.0.0",
  "dependencies": {
    "express": "^4.18.0",
    "lodash": "^4.17.21",
    "react": "^18.2.0"
  }
}
`);
    });
});

describe("removeMember", () => {
    const text = `{
  "a": 1,
  "b": 2,
  "c": 3
}`;

    test("removes the first member", async () => {
        expect(await editRoot(text, obj => removeMember(obj, "a"))).toBe(`{
  "b": 2,
  "c": 3
}`);
    });

    test("removes a middle member", async () => {
        expect(await editRoot(text, obj => removeMember(obj, "b"))).toBe(`{
  "a": 1,
  "c": 3
}`);
    });

    test("removes the last member", async () => {
        expect(await editRoot(text, obj => removeMember(obj, "c"))).toBe(`{
  "a": 1,
  "b": 2
}`);
    });

    test("gives the first member's position to the next one on the same line", async () => {
        expect(await editRoot(`{"a": 1, "b": 2}`, obj => removeMember(obj, "a"))).toBe(`{"b": 2}`);
    });

    test("removes the last member before a trailing comma", async () => {
        expect(await editRoot(`{
  "a": 1,
  "b": 2,
}`, obj => removeMember(obj, "b"))).toBe(`{
  "a": 1,
}`);
    });

    test("empties a single-member object", async () => {
        // given
        const obj = root(await parse(`{
  "a": 1
}`));

        // when
        const removed = removeMember(obj, "a");

        // then
        expect(isEmptyContainer(removed)).toBe(true);
        expect(await TreePrinters.print({...await parse(`{}`), value: removed} as Json.Document)).toBe(`{}`);
    });

    test("removes the first member when the indentation is on the keys", async () => {
        // given
        const doc = withIndentationOnKeys(await parse(text));

        // when
        const printed = await TreePrinters.print(removeAt(doc, ["a"]));

        // then
        expect(printed).toBe(`{
  "b": 2,
  "c": 3
}`);
    });

    test("returns the same object when the key is missing", async () => {
        // given
        const obj = root(await parse(text));

        // then
        expect(removeMember(obj, "d")).toBe(obj);
    });

    test("keeps a comment on the line of the member before a removed last one", async () => {
        expect(await editRoot(`{
  "a": 1, // about a
  // about b
  "b": 2
}`, obj => removeMember(obj, "b"))).toBe(`{
  "a": 1 // about a
}`);
    });

    test("removes the removed member's own comment with it", async () => {
        expect(await editRoot(`{
  "a": 1, // about a
  "b": 2, // about b
  "c": 3
}`, obj => removeMember(obj, "b"))).toBe(`{
  "a": 1, // about a
  "c": 3
}`);
        expect(await editRoot(`{
  "a": 1, // about a
  "b": 2 // about b
}`, obj => removeMember(obj, "b"))).toBe(`{
  "a": 1 // about a
}`);
        expect(await editRoot(`{
  "a": 1, // about a
  "b": 2, // about b
}`, obj => removeMember(obj, "b"))).toBe(`{
  "a": 1, // about a
}`);
        expect(await editRoot(`{
  "a": 1 // about a
}`, obj => removeMember(obj, "a"))).toBe(`{}`);
    });

    test("keeps a comment on the previous line when the indentation is on the keys", async () => {
        expect(await editDocument(`{
  "a": 1, // about a
  "b": 2,
  "c": 3
}`, doc => removeAt(withIndentationOnKeys(doc), ["b"]))).toBe(`{
  "a": 1, // about a
  "c": 3
}`);
    });

    test("keeps a comment on the line of the member before a removed middle one", async () => {
        expect(await editRoot(`{
  "a": 1, // about a
  "b": 2,
  "c": 3
}`, obj => removeMember(obj, "b"))).toBe(`{
  "a": 1, // about a
  "c": 3
}`);
    });

    test("keeps a comment on the line of the opening brace", async () => {
        expect(await editRoot(`{ // config
  "a": 1,
  "b": 2
}`, obj => removeMember(obj, "a"))).toBe(`{ // config
  "b": 2
}`);
        expect(await editRoot(`{ // config
  "a": 1
}`, obj => removeMember(obj, "a"))).toBe(`{ // config
}`);
    });

    test("keeps a comment on the line of the member before one removed before a trailing comma", async () => {
        expect(await editRoot(`{
  "a": 1, // about a
  "b": 2,
}`, obj => removeMember(obj, "b"))).toBe(`{
  "a": 1, // about a
}`);
    });
});

describe("updateMember", () => {
    test("keeps the prefix that fn gives the value", async () => {
        expect(await editRoot(`{"a": 1}`, obj => updateMember(obj, "a", v => ({...v, prefix: space("   ")})))).toBe(`{"a":   1}`);
    });

    test("places a new value after the old value's prefix", async () => {
        expect(await editRoot(`{"a":  1}`, obj => updateMember(obj, "a", () => jsonLiteral(2)))).toBe(`{"a":  2}`);
    });
});

describe("path edits", () => {
    const manifest = `{
    "_version": "1.12.0",
    "sap.ui5": {
        "resources": {
            "js": []
        },
        "dependencies": {
            "libs": {
                "sap.m": {},
                "sap.makit": {}
            }
        }
    }
}
`;

    test("removeAt prunes the parents it empties", async () => {
        expect(await editDocument(manifest, doc => removeAt(doc, ["sap.ui5", "resources", "js"], {pruneEmptyParents: true}))).toBe(`{
    "_version": "1.12.0",
    "sap.ui5": {
        "dependencies": {
            "libs": {
                "sap.m": {},
                "sap.makit": {}
            }
        }
    }
}
`);
    });

    test("removeAt without pruning leaves the empty parent", async () => {
        expect(await editDocument(manifest, doc => removeAt(doc, ["sap.ui5", "resources", "js"]))).toBe(`{
    "_version": "1.12.0",
    "sap.ui5": {
        "resources": {},
        "dependencies": {
            "libs": {
                "sap.m": {},
                "sap.makit": {}
            }
        }
    }
}
`);
    });

    test("removeAt never removes the root object", async () => {
        expect(await editDocument(`{"a": {"b": 1}}`, doc => removeAt(doc, ["a", "b"], {pruneEmptyParents: true}))).toBe(`{}`);
    });

    test("updateAt rebuilds the path back to the root", async () => {
        // given
        const doc = await parse(manifest);

        // when
        const edited = updateAt(doc, ["_version"], () => jsonLiteral("1.65.0"));

        // then
        expect(await TreePrinters.print(edited)).toBe(manifest.replace(`"_version": "1.12.0"`, `"_version": "1.65.0"`));
        expect(updateAt(doc, ["sap.ui5", "missing", "x"], () => jsonLiteral(1))).toBe(doc);
        expect(findMember(root(edited), "sap.ui5")).toBe(findMember(root(doc), "sap.ui5"));
    });

    test("setAt creates the missing objects", async () => {
        expect(await editDocument(manifest, doc => setAt(doc, ["sap.ui5", "models", "i18n", "type"], jsonLiteral("sap.ui.model.resource.ResourceModel"), {createMissing: true}))).toBe(`{
    "_version": "1.12.0",
    "sap.ui5": {
        "resources": {
            "js": []
        },
        "dependencies": {
            "libs": {
                "sap.m": {},
                "sap.makit": {}
            }
        },
        "models": {
            "i18n": {
                "type": "sap.ui.model.resource.ResourceModel"
            }
        }
    }
}
`);
    });

    test("setAt places the objects it creates by parentPosition", async () => {
        const text = `{
  "name": "x",
  "version": "1.0.0",
  "main": "index.js"
}`;
        expect(await editDocument(text, doc => setAt(doc, ["dependencies", "lodash"], jsonLiteral("^4"), {createMissing: true, position: "sorted"}))).toBe(`{
  "name": "x",
  "version": "1.0.0",
  "main": "index.js",
  "dependencies": {
    "lodash": "^4"
  }
}`);
        expect(await editDocument(text, doc => setAt(doc, ["dependencies", "lodash"], jsonLiteral("^4"), {createMissing: true, parentPosition: "first"}))).toBe(`{
  "dependencies": {
    "lodash": "^4"
  },
  "name": "x",
  "version": "1.0.0",
  "main": "index.js"
}`);
    });

    test("setAt leaves the root unchanged for a missing step without createMissing", async () => {
        // given
        const doc = await parse(manifest);

        // then
        expect(setAt(doc, ["sap.ui5", "models", "i18n"], jsonLiteral(1))).toBe(doc);
        expect(setAt(doc, ["_version", "x"], jsonLiteral(1), {createMissing: true})).toBe(doc);
    });

    test("setAt with a value laid out by fromPlain", async () => {
        expect(await editDocument(`{
  "name": "app"
}`, doc => setAt(doc, ["jest"], fromPlain({testEnvironment: "node", roots: ["<rootDir>/src"], coverage: {}}, "  ", 1)))).toBe(`{
  "name": "app",
  "jest": {
    "testEnvironment": "node",
    "roots": [
      "<rootDir>/src"
    ],
    "coverage": {}
  }
}`);
    });
});

describe("building", () => {
    test("jsonLiteral writes the source as JSON", () => {
        expect(jsonLiteral(`a"b\\`).source).toBe(String.raw`"a\"b\\"`);
        expect(jsonLiteral(`a"b\\`).value).toBe(`a"b\\`);
        expect(jsonLiteral(1.5).source).toBe("1.5");
        expect(jsonLiteral(null).source).toBe("null");
        expect(() => jsonLiteral(NaN)).toThrow(RangeError);
    });

    test("fromPlain matches JSON.stringify", async () => {
        // given
        const value = {a: 1, b: [true, null, "x"], c: {d: {}}, e: [], f: new Date(0), skipped: undefined};

        // when
        const printed = await TreePrinters.print({...await parse(`{}`), value: fromPlain(value, "\t")} as Json.Document);

        // then
        expect(printed).toBe(JSON.stringify(value, null, "\t"));
    });
});

describe("array elements", () => {
    test("elementsOf skips the Empty of a trailing comma and of an empty array", async () => {
        // given
        const doc = await parse(`{"a": ["x", "y",], "b": []}`);

        // when
        const a = elementsOf(valueAt(doc, ["a"]) as Json.Array);
        const b = elementsOf(valueAt(doc, ["b"]) as Json.Array);

        // then
        expect(a.map(toPlain)).toEqual(["x", "y"]);
        expect(b).toEqual([]);
    });

    test("appends to an array on one line", async () => {
        expect(await editArray(`["a", "b"]`, arr => appendElement(arr, jsonLiteral("c")))).toBe(`["a", "b", "c"]`);
        expect(await editArray(`["a"]`, arr => appendElement(arr, jsonLiteral("b")))).toBe(`["a", "b"]`);
    });

    test.each(["  ", "    ", "\t"])("appends to an array indented by %j", async indent => {
        expect(await editArray(`[\n${indent}"a",\n${indent}"b"\n]`, arr => appendElement(arr, jsonLiteral("c"))))
            .toBe(`[\n${indent}"a",\n${indent}"b",\n${indent}"c"\n]`);
    });

    test("fills an empty array inline, or on its own line where the brackets are or an indent is given", async () => {
        expect(await editArray(`[]`, arr => appendElement(arr, jsonLiteral("a")))).toBe(`["a"]`);
        expect(await editArray(`[
]`, arr => appendElement(arr, jsonLiteral("a"), {indent: "  "}))).toBe(`[
  "a"
]`);
        expect(await editArray(`[]`, arr => appendElement(arr, jsonLiteral("a"), {indent: "  "}))).toBe(`[
  "a"
]`);
        expect(await editDocument(`{
  "types": []
}`, doc => updateAt(doc, ["types"], v => isArray(v) ? appendElement(v, jsonLiteral("node"), {indent: "  ", depth: 1}) : v))).toBe(`{
  "types": [
    "node"
  ]
}`);
    });

    test("keeps a trailing comma", async () => {
        expect(await editArray(`["a", "b",]`, arr => appendElement(arr, jsonLiteral("c")))).toBe(`["a", "b", "c",]`);
        expect(await editArray(`["a", "b",]`, arr => removeElements(arr, isValue("b")))).toBe(`["a",]`);
    });

    test("keeps a comment on the line of the element before", async () => {
        const text = `[
  "a", // about a
  "b"
]`;
        expect(await editArray(text, arr => appendElement(arr, jsonLiteral("c")))).toBe(`[
  "a", // about a
  "b",
  "c"
]`);
        expect(await editArray(text, arr => insertElement(arr, 1, jsonLiteral("x")))).toBe(`[
  "a", // about a
  "x",
  "b"
]`);
        expect(await editArray(text, arr => removeElements(arr, isValue("b")))).toBe(`[
  "a" // about a
]`);
    });

    test("removes the removed element's own comment with it", async () => {
        expect(await editArray(`[
  "a", // about a
  "b", // about b
  "c"
]`, arr => removeElements(arr, isValue("b")))).toBe(`[
  "a", // about a
  "c"
]`);
    });

    test("gives the first element's position to the next one", async () => {
        expect(await editArray(`[
  "a",
  "b"
]`, arr => removeElements(arr, isValue("a")))).toBe(`[
  "b"
]`);
        expect(await editArray(`["a", "b"]`, arr => removeElements(arr, isValue("a")))).toBe(`["b"]`);
    });

    test("inserts first", async () => {
        expect(await editArray(`[
  "a"
]`, arr => insertElement(arr, 0, jsonLiteral("z")))).toBe(`[
  "z",
  "a"
]`);
        expect(await editArray(`["a"]`, arr => insertElement(arr, 0, jsonLiteral("z")))).toBe(`["z", "a"]`);
    });

    test("empties an array to [] unless a comment remains", async () => {
        expect(await editArray(`[
  "a",
  "b"
]`, arr => removeElements(arr, () => true))).toBe(`[]`);
        expect(await editArray(`[ // list
  "a"
]`, arr => removeElements(arr, () => true))).toBe(`[ // list
]`);
    });

    test("removes several elements at once as it would one at a time", async () => {
        // given
        const text = `[
  "a", // about a
  "b", // about b
  "c", // about c
  "d"
]`;
        const removed = (v: Json.Value) => toPlain(v) === "b" || toPlain(v) === "c";

        // when
        const atOnce = await editArray(text, arr => removeElements(arr, removed));
        const oneAtATime = await editArray(text, arr => removeElements(removeElements(arr, isValue("b")), isValue("c")));

        // then
        expect(atOnce).toBe(`[
  "a", // about a
  "d"
]`);
        expect(oneAtATime).toBe(atOnce);
    });

    test("keeps the formatting of object and array elements", async () => {
        const text = `[
  {"name": "x",  "path": "a"},
  [1,  2],
  "s"
]`;
        expect(await editArray(text, arr => removeElements(arr, isValue("s")))).toBe(`[
  {"name": "x",  "path": "a"},
  [1,  2]
]`);
        expect(await editArray(text, arr => appendElement(arr, fromPlain({name: "y"}, "  ", 1)))).toBe(`[
  {"name": "x",  "path": "a"},
  [1,  2],
  "s",
  {
    "name": "y"
  }
]`);
    });

    test("mapElements replaces the first match and removes the others in one pass", async () => {
        // given
        const text = `{
  "compilerOptions": {
    "types": ["jest", "@types/jest", "node"]
  }
}`;
        const jestTypes = new Set(["jest", "@types/jest"]);

        // when
        const printed = await editDocument(text, doc => updateAt(doc, ["compilerOptions", "types"], types => {
            if (!isArray(types)) {
                return types;
            }
            let replaced = false;
            return mapElements(types, v => {
                if (!jestTypes.has(toPlain(v) as string)) {
                    return v;
                }
                if (replaced) {
                    return undefined;
                }
                replaced = true;
                return jsonLiteral("vitest/globals");
            });
        }));

        // then
        expect(printed).toBe(`{
  "compilerOptions": {
    "types": ["vitest/globals", "node"]
  }
}`);
    });

    test("mapElements places a new element where the old one was, and keeps a prefix derived from it", async () => {
        expect(await editArray(`["a",  "b"]`, arr => mapElements(arr, v => isValue("b")(v) ? jsonLiteral("c") : v))).toBe(`["a",  "c"]`);
        expect(await editArray(`["a", "b"]`, arr => mapElements(arr, v => isValue("b")(v) ? {...v, prefix: space("   ")} : v))).toBe(`["a",   "b"]`);
    });

    test("returns the same array when nothing changes", async () => {
        // given
        const arr = (await parse(`["a", "b"]`)).value as Json.Array;

        // then
        expect(mapElements(arr, v => v)).toBe(arr);
        expect(removeElements(arr, () => false)).toBe(arr);
        expect(appendElement(arr, jsonLiteral("a"), {unique: true})).toBe(arr);
        expect(appendElement(arr, jsonLiteral("a"))).not.toBe(arr);
    });
});

class AddPrivate extends Recipe {
    name = "org.openrewrite.json.test.add-private";
    displayName = "Add private";
    description = "Marks the package private after its version, and drops a jest section.";

    async editor(): Promise<TreeVisitor<any, ExecutionContext>> {
        return new class extends JsonVisitor<ExecutionContext> {
            protected override async visitObject(obj: Json.Object, ctx: ExecutionContext): Promise<Json | undefined> {
                const visited = await super.visitObject(obj, ctx) as Json.Object;
                if (pathOf(this.cursor)?.length !== 0) {
                    return visited;
                }
                return removeMember(setMember(visited, "private", jsonLiteral(true)), "jest");
            }
        };
    }
}

describe("in a recipe", () => {
    const spec = new RecipeSpec();
    spec.recipe = new AddPrivate();

    test("edits the root object and leaves nested ones alone", () => spec.rewriteRun(
        json(
            `{
  "name": "app",
  "jest": {
    "name": "nested"
  }
}`,
            `{
  "name": "app",
  "private": true
}`
        )
    ));

    test("pathOf gives the member keys down to the cursor", async () => {
        // given
        const paths: (string[] | undefined)[] = [];
        const visitor = new class extends JsonVisitor<ExecutionContext> {
            protected override async visitLiteral(lit: Json.Literal, ctx: ExecutionContext): Promise<Json | undefined> {
                if (lit.value === "x") {
                    paths.push(pathOf(this.cursor));
                }
                return lit;
            }
        };

        // when
        await visitor.visit(await parse(`{"a": {"b": "x", "c": ["x"]}}`), new ExecutionContext());

        // then
        expect(paths).toEqual([["a", "b"], ["a", "c"]]);
    });

    test("pathOf is undefined below a key it can't read", async () => {
        // given
        const doc = await parse(`{"a": {"b": "x"}}`);
        const a = findMember(root(doc), "a")!;
        const unreadable = {...a, key: {...a.key, element: {...a.key.element, kind: Json.Kind.Empty}}} as unknown as Json.Member;
        const edited = {...doc, value: {...root(doc), members: [{...root(doc).members[0], element: unreadable}]}} as Json.Document;
        const paths: (string[] | undefined)[] = [];
        const visitor = new class extends JsonVisitor<ExecutionContext> {
            protected override async visitLiteral(lit: Json.Literal, ctx: ExecutionContext): Promise<Json | undefined> {
                if (lit.value === "x") {
                    paths.push(pathOf(this.cursor));
                }
                return lit;
            }
        };

        // when
        await visitor.visit(edited, new ExecutionContext());

        // then
        expect(paths).toEqual([undefined]);
    });
});
