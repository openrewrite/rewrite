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
    emptyArray,
    emptyObject,
    fromPlain,
    isEmpty,
    isMember,
    isObject,
    Json,
    json,
    JsonParser,
    JsonVisitor,
    literal,
    member,
    members,
    pathOf,
    removeAt,
    removeMember,
    setAt,
    setMember,
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
    test("members skips the Empty of a trailing comma", async () => {
        // given
        const obj = root(await parse(`{"a": 1, "b": 2,}`));

        // when
        const keys = members(obj).map(m => (m.key.element as Json.Literal).value);

        // then
        expect(keys).toEqual(["a", "b"]);
    });

    test("member matches a key with escapes by its decoded name", async () => {
        // given
        const obj = root(await parse(String.raw`{"\\.(css|less)$": "identity-obj-proxy", "a\"b": 1}`));

        // when
        const proxy = member(obj, String.raw`\.(css|less)$`);
        const quoted = member(obj, `a"b`);

        // then
        expect(toPlain(proxy!.value)).toBe("identity-obj-proxy");
        expect(toPlain(quoted!.value)).toBe(1);
        expect(member(obj, "missing")).toBeUndefined();
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
        const n = member(root(doc), "n")!.value as Json.Literal;

        // when
        const plain = toPlain(doc);
        const typedAsString = toPlain({...n, value: "5000"} as Json.Literal);

        // then
        expect(plain).toEqual({n: 5000, f: 1500, t: true, z: null, s: "a\nb", arr: [1, "x", {}], o: {k: []}});
        expect(typedAsString).toBe(5000);
    });

    test("isEmpty recognizes the single-Empty form", async () => {
        // given
        const obj = root(await parse(`{"o": {}, "a": [ ], "full": {"x": 1}, "list": [1]}`));

        // then
        expect(isEmpty(member(obj, "o")!.value as Json.Object)).toBe(true);
        expect(isEmpty(member(obj, "a")!.value as Json.Array)).toBe(true);
        expect(isEmpty(member(obj, "full")!.value as Json.Object)).toBe(false);
        expect(isEmpty(member(obj, "list")!.value as Json.Array)).toBe(false);
        expect(isEmpty(emptyObject())).toBe(true);
        expect(isEmpty(emptyArray())).toBe(true);
    });
});

describe("setMember", () => {
    test("appends to a 2-space indented object", async () => {
        expect(await editRoot(packageJson, obj => setMember(obj, "private", literal(true)))).toBe(`{
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
}`, obj => setMember(obj, "_version", literal("1.65.0"), {position: "first"}))).toBe(`{
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
}`, obj => updateMember(obj, "a", a => setMember(a as Json.Object, "c", literal(2))))).toBe(`{
\t"a": {
\t\t"b": 1,
\t\t"c": 2
\t}
}`);
    });

    test("inserts in sorted place into a dependencies section", async () => {
        // when
        const printed = await editDocument(packageJson, doc => setAt(doc, ["dependencies", "lodash"], literal("^4.17.21"), {position: "sorted"}));

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
            const scoped = setAt(doc, ["dependencies", "@babel/core"], literal("^7.0.0"), {position: "sorted"});
            return setAt(scoped, ["dependencies", "zod"], literal("^3.0.0"), {position: "sorted"});
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
        expect(await editRoot(packageJson, obj => setMember(obj, "version", literal("2.0.0"), {position: "first"}))).toBe(`{
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
        const edited = setMember(obj, "a", member(obj, "a")!.value);

        // then
        expect(edited).toBe(obj);
    });

    test("fills an empty object on its own line with the given indent", async () => {
        expect(await editDocument(`{
  "name": "app",
  "dependencies": {}
}`, doc => setAt(doc, ["dependencies", "react"], literal("^18.2.0")))).toBe(`{
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
}`, obj => updateMember(obj, "dependencies", d => setMember(d as Json.Object, "react", literal("^18.2.0"), {indent: "  "})))).toBe(`{
  "dependencies": {
    "react": "^18.2.0"
  }
}`);
    });

    test("fills an empty object inline when no indent is known", async () => {
        expect(await editRoot(`{}`, obj => setMember(obj, "a", literal(1)))).toBe(`{"a": 1}`);
    });

    test("fills an empty root document with its detected indent", async () => {
        expect(await editDocument(`{}`, doc => setAt(doc, ["a"], literal(1)))).toBe(`{
    "a": 1
}`);
    });

    test("fills an empty object in a document on one line inline", async () => {
        expect(await editDocument(`{"a": {}}`, doc => setAt(doc, ["a", "b"], literal(1)))).toBe(`{"a": {"b": 1}}`);
        expect(await editDocument(`{"a": 1}`, doc => setAt(doc, ["x", "b"], literal(1), {createMissing: true}))).toBe(`{"a": 1, "x": {"b": 1}}`);
    });

    test("fills an empty object that holds a comment", async () => {
        expect(await editDocument(`{
  "a": {
    // nothing yet
  }
}`, doc => setAt(doc, ["a", "b"], literal(1)))).toBe(`{
  "a": {
    "b": 1
    // nothing yet
  }
}`);
    });

    test("keeps a comment after the last member on its line", async () => {
        expect(await editRoot(`{
  "a": 1 // about a
}`, obj => setMember(obj, "b", literal(2)))).toBe(`{
  "a": 1, // about a
  "b": 2
}`);
    });

    test("adds to a single-member object", async () => {
        expect(await editRoot(`{"a": 1}`, obj => setMember(obj, "b", literal(2)))).toBe(`{"a": 1, "b": 2}`);
    });

    test("prepends to an object on one line", async () => {
        expect(await editRoot(`{"a": 1}`, obj => setMember(obj, "_version", literal("2"), {position: "first"}))).toBe(`{"_version": "2", "a": 1}`);
    });

    test("leaves a member that already holds the same data", async () => {
        // given
        const obj = root(await parse(`{"a": 1.0, "b": {"c": [1]}}`));

        // when
        const literalEdit = setMember(obj, "a", literal(1));
        const objectEdit = setMember(obj, "b", fromPlain({c: [1]}, "  "));

        // then
        expect(literalEdit).toBe(obj);
        expect(objectEdit).toBe(obj);
    });

    test("keeps a trailing comma", async () => {
        expect(await editRoot(`{
  "a": 1,
}`, obj => setMember(obj, "b", literal(2)))).toBe(`{
  "a": 1,
  "b": 2,
}`);
    });

    test("escapes the key and the value", async () => {
        expect(await editRoot(`{
  "a": 1
}`, obj => setMember(obj, String.raw`\.(css|less)$`, literal(`say "hi"`)))).toBe(String.raw`{
  "a": 1,
  "\\.(css|less)$": "say \"hi\""
}`);
    });

    test("copies the indentation of a sibling whose indentation is on its key", async () => {
        // given
        const doc = withIndentationOnKeys(await parse(packageJson));

        // when
        const printed = await TreePrinters.print(setAt(doc, ["dependencies", "lodash"], literal("^4.17.21"), {position: "sorted"}));

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
        expect(isEmpty(removed)).toBe(true);
        expect(await TreePrinters.print({...await parse(`{}`), value: removed} as Json.Document)).toBe(`{
}`);
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
        "resources": {
        },
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
        const edited = updateAt(doc, ["_version"], () => literal("1.65.0"));

        // then
        expect(await TreePrinters.print(edited)).toBe(manifest.replace(`"_version": "1.12.0"`, `"_version": "1.65.0"`));
        expect(updateAt(doc, ["sap.ui5", "missing", "x"], () => literal(1))).toBe(doc);
        expect(member(root(edited), "sap.ui5")).toBe(member(root(doc), "sap.ui5"));
    });

    test("setAt creates the missing objects", async () => {
        expect(await editDocument(manifest, doc => setAt(doc, ["sap.ui5", "models", "i18n", "type"], literal("sap.ui.model.resource.ResourceModel"), {createMissing: true}))).toBe(`{
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

    test("setAt leaves the root unchanged for a missing step without createMissing", async () => {
        // given
        const doc = await parse(manifest);

        // then
        expect(setAt(doc, ["sap.ui5", "models", "i18n"], literal(1))).toBe(doc);
        expect(setAt(doc, ["_version", "x"], literal(1), {createMissing: true})).toBe(doc);
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
    test("literal writes the source as JSON", () => {
        expect(literal(`a"b\\`).source).toBe(String.raw`"a\"b\\"`);
        expect(literal(`a"b\\`).value).toBe(`a"b\\`);
        expect(literal(1.5).source).toBe("1.5");
        expect(literal(null).source).toBe("null");
        expect(() => literal(NaN)).toThrow(RangeError);
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

class AddPrivate extends Recipe {
    name = "org.openrewrite.json.test.add-private";
    displayName = "Add private";
    description = "Marks the package private after its version, and drops a jest section.";

    async editor(): Promise<TreeVisitor<any, ExecutionContext>> {
        return new class extends JsonVisitor<ExecutionContext> {
            protected override async visitObject(obj: Json.Object, ctx: ExecutionContext): Promise<Json | undefined> {
                const visited = await super.visitObject(obj, ctx) as Json.Object;
                if (pathOf(this.cursor).length !== 0) {
                    return visited;
                }
                return removeMember(setMember(visited, "private", literal(true)), "jest");
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
        const paths: string[][] = [];
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
});
