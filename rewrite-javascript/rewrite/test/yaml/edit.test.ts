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
    appendItem,
    entryKey,
    findEntry,
    itemsOf,
    mapItems,
    pathOf,
    removeAt,
    removeItems,
    setAt,
    setEntry,
    setScalarValue,
    toPlain,
    updateAt,
    valueAt,
    Yaml,
    YamlParser,
    YamlVisitor,
    yamlScalar
} from "../../src/yaml";
import {SourceFile, TreePrinters} from "../../src";

async function parse(text: string): Promise<Yaml.Documents> {
    for await (const sf of new YamlParser().parse({text, sourcePath: "a.yml"})) {
        return sf as SourceFile as Yaml.Documents;
    }
    throw new Error("no source file produced");
}

async function edit(text: string, fn: (docs: Yaml.Documents) => Yaml.Documents): Promise<string> {
    return TreePrinters.print(fn(await parse(text)));
}

async function editSequence(text: string, path: string[], fn: (seq: Yaml.Sequence) => Yaml.Sequence): Promise<string> {
    return edit(text, docs => updateAt(docs, path, old => fn(old as Yaml.Sequence)));
}

const below = (major: number) => (item: Yaml.Block) => {
    const value = toPlain(item);
    return parseInt(String(value), 10) < major;
};

describe("reading", () => {
    test("toPlain reads the core schema's types", async () => {
        expect(toPlain(await parse(
            "a: 1\nb: 1.5\nc: true\nd: null\ne: ~\nf: '8'\ng: \"x\\ty\"\nh: 8.x\ni:\nj: 'it''s'\n"
        ))).toEqual({a: 1, b: 1.5, c: true, d: null, e: null, f: "8", g: "x\ty", h: "8.x", i: null, j: "it's"});
    });

    test("toPlain reads block scalars", async () => {
        expect(toPlain(await parse("a: |\n  x\n  y\nb: >-\n  p\n  q\nc: |-\n    z\n"))).toEqual({a: "x\ny\n", b: "p q", c: "z"});
    });

    test("toPlain resolves aliases and merge keys", async () => {
        expect(toPlain(await parse("base: &b\n  x: 1\n  y: 2\nderived:\n  <<: *b\n  y: 3\nlist: [*b]\n"))).toEqual({
            base: {x: 1, y: 2},
            derived: {x: 1, y: 3},
            list: [{x: 1, y: 2}]
        });
    });

    test("toPlain doesn't read a value with a custom tag", async () => {
        expect(toPlain(await parse("a: !Ref foo\n"))).toBeUndefined();
    });

    test("valueAt follows mapping keys", async () => {
        const docs = await parse("jobs:\n  build:\n    strategy:\n      matrix:\n        node-version: [8.x, 10.x]\n");
        expect(toPlain(valueAt(docs, ["jobs", "build", "strategy", "matrix", "node-version"])!)).toEqual(["8.x", "10.x"]);
        expect(valueAt(docs, ["jobs", "test"])).toBeUndefined();
        expect(valueAt(docs, ["jobs", "build", "strategy", "matrix", "node-version", "x"])).toBeUndefined();
    });

    test("entryKey decodes a quoted key", async () => {
        const mapping = (await parse("\"a b\": 1\n'c': 2\nd: 3\n")).documents[0].block as Yaml.Mapping;
        expect(mapping.entries.map(entryKey)).toEqual(["a b", "c", "d"]);
        expect(toPlain(findEntry(mapping, "c")!.value)).toBe(2);
    });

    test("pathOf gives the keys down to a node", async () => {
        const paths: (string[] | undefined)[] = [];
        await new class extends YamlVisitor<number> {
            protected async visitScalar(scalar: Yaml.Scalar, p: number): Promise<Yaml | undefined> {
                if (scalar.value === "8") {
                    paths.push(pathOf(this.cursor));
                }
                return scalar;
            }
        }().visit(await parse("a:\n  b: [8]\n"), 0);
        expect(paths).toEqual([["a", "b"]]);
    });
});

describe("scalar values", () => {
    test("keeps the quoting of each item", async () => {
        expect(await editSequence("node-version: [8.x, '10.x', \"12.x\"]\n", ["node-version"],
            seq => mapItems(seq, () => "18.x"))).toBe("node-version: [18.x, '18.x', \"18.x\"]\n");
    });

    test("quotes a string that would read back as another type", async () => {
        expect(await editSequence("v: [8, 10]\n", ["v"], seq => mapItems(seq, (_, i) => i === 0 ? 18 : "18")))
            .toBe("v: [18, \"18\"]\n");
    });

    test("quotes a string that can't be written plain", async () => {
        expect(await edit("a: 1\n", docs => setAt(docs, ["a"], "x: y"))).toBe("a: \"x: y\"\n");
    });

    test("escapes a single-quoted string", async () => {
        expect(await edit("a: 'x'\n", docs => setAt(docs, ["a"], "it's"))).toBe("a: 'it''s'\n");
    });

    test("drops a core schema tag", async () => {
        expect(await edit("a: !!str 8\n", docs => setAt(docs, ["a"], 9))).toBe("a: 9\n");
    });

    test("leaves a scalar that already holds the data unchanged", async () => {
        const docs = await parse("a: '8'\nb: 8\n");
        expect(setAt(docs, ["a"], "8")).toBe(docs);
        expect(setAt(docs, ["b"], 8)).toBe(docs);
    });

    test("fills in an empty value", async () => {
        expect(await edit("a:\nb: 1\n", docs => setAt(docs, ["a"], 18))).toBe("a: 18\nb: 1\n");
    });

    test("fills in an empty value followed by a comment", async () => {
        expect(await edit("a: # c\nb: 1\n", docs => setAt(docs, ["a"], 18))).toBe("a: 18 # c\nb: 1\n");
    });

    test("rewrites a block scalar as a block scalar", async () => {
        expect(await edit("run: |\n  npm ci\n  npm test\nnext: 1\n",
            docs => setAt(docs, ["run"], "npm ci\nnpm run build\n")))
            .toBe("run: |\n  npm ci\n  npm run build\nnext: 1\n");
    });

    test("keeps the header of a block scalar, as Java's BlockScalar does", async () => {
        expect(await edit("run: |\n  npm ci\nnext: 1\n", docs => setAt(docs, ["run"], "npm ci\nnpm test")))
            .toBe("run: |\n  npm ci\n  npm test\nnext: 1\n");
    });

    test("writes a single line in place of a block scalar as a block scalar", async () => {
        expect(await edit("run: >\n  npm ci\nnext: 1\n", docs => setAt(docs, ["run"], "npm test")))
            .toBe("run: |\n  npm test\nnext: 1\n");
    });

    test("leaves a block scalar whose value differs only by the final line break its header adds", async () => {
        const docs = await parse("run: |\n  npm ci\nnext: 1\n");
        expect(setAt(docs, ["run"], "npm ci")).toBe(docs);
    });

    test("keeps a block scalar without a final line break", async () => {
        expect(await edit("run: |-\n  npm ci\nnext: 1\n", docs => setAt(docs, ["run"], "npm test")))
            .toBe("run: |-\n  npm test\nnext: 1\n");
    });

    test("keeps the line break after a block scalar replaced by a plain value", async () => {
        expect(await edit("run: |\n  x\nnext: 1\n", docs => setAt(docs, ["run"], 5))).toBe("run: 5\nnext: 1\n");
    });

    test("keeps the line break after a block scalar that is the last item of a nested sequence", async () => {
        expect(await editSequence("a:\n  - |\n    x\nb: 1\n", ["a"], seq => mapItems(seq, () => 5)))
            .toBe("a:\n  - 5\nb: 1\n");
    });

    test("keeps the line break after a block scalar in the middle of a sequence", async () => {
        expect(await editSequence("a:\n  - |\n    x\n  - y\n", ["a"], seq => mapItems(seq, (item, i) => i === 0 ? 5 : item)))
            .toBe("a:\n  - 5\n  - y\n");
    });

    test("yamlScalar writes plain where it reads back the same", () => {
        expect([yamlScalar("18.x"), yamlScalar("18"), yamlScalar(18), yamlScalar(true), yamlScalar(null), yamlScalar("lts/*")]
            .map(s => [s.style, s.value]))
            .toEqual([["PLAIN", "18.x"], ["DOUBLE_QUOTED", "18"], ["PLAIN", "18"], ["PLAIN", "true"], ["PLAIN", "null"], ["PLAIN", "lts/*"]]);
    });

    test("setScalarValue returns the same scalar for the same data", async () => {
        const scalar = ((await parse("a: 8.x\n")).documents[0].block as Yaml.Mapping).entries[0].value as Yaml.Scalar;
        expect(setScalarValue(scalar, "8.x")).toBe(scalar);
    });
});

describe("mapping entries", () => {
    test("adds an entry after the last", async () => {
        expect(await edit("a: 1\n", docs => setAt(docs, ["b"], 2))).toBe("a: 1\nb: 2\n");
    });

    test("keeps a comment on the last entry's line there", async () => {
        expect(await edit("m:\n  a: 1 # one\nn: 2\n", docs => setAt(docs, ["m", "b"], 2)))
            .toBe("m:\n  a: 1 # one\n  b: 2\nn: 2\n");
    });

    test("adds an entry first, below the comments before the old first entry", async () => {
        expect(await edit("m:\n  # about a\n  a: 1\n", docs => setAt(docs, ["m", "z"], 0, {position: "first"})))
            .toBe("m:\n  z: 0\n  # about a\n  a: 1\n");
    });

    test("adds an entry first in the root mapping", async () => {
        expect(await edit("a: 1\nb: 2\n", docs => setAt(docs, ["z"], 0, {position: "first"}))).toBe("z: 0\na: 1\nb: 2\n");
    });

    test("adds an entry in sorted position", async () => {
        expect(await edit("a: 1\nc: 3\n", docs => setAt(docs, ["b"], 2, {position: "sorted"}))).toBe("a: 1\nb: 2\nc: 3\n");
    });

    test("adds an entry after a block scalar", async () => {
        expect(await edit("a: |\n  x\n", docs => setAt(docs, ["b"], 1))).toBe("a: |\n  x\nb: 1\n");
    });

    test("adds an entry to a flow mapping", async () => {
        expect(await edit("x: {a: 1}\ny: {}\n", docs => setAt(setAt(docs, ["x", "b"], 2), ["y", "b"], 2)))
            .toBe("x: {a: 1, b: 2}\ny: {b: 2}\n");
    });

    test("quotes a key that can't be written plain", async () => {
        expect(await edit("a: 1\n", docs => setAt(docs, ["b: c"], 2))).toBe("a: 1\n\"b: c\": 2\n");
    });

    test("adds an entry to a mapping that starts on its item's dash", async () => {
        expect(await editSequence("include:\n  - name: a\n", ["include"],
            seq => mapItems(seq, item => setEntry(item as Yaml.Mapping, "node-version", 18))))
            .toBe("include:\n  - name: a\n    node-version: 18\n");
    });

    test("can't tell the indentation of a mapping on its item's dash without the sequence", async () => {
        const seq = valueAt(await parse("include:\n  - name: a\n"), ["include"]) as Yaml.Sequence;
        expect(() => setEntry(seq.entries[0].block as Yaml.Mapping, "b", 1)).toThrow(/indent/);
    });

    test("leaves the document unchanged when the value is the same", async () => {
        const docs = await parse("a:\n  b: 1\n");
        expect(setAt(docs, ["a", "b"], 1)).toBe(docs);
        expect(setAt(docs, ["x", "b"], 1)).toBe(docs);
    });

    test("keeps comments before and after a removed entry, and drops the one on its line", async () => {
        expect(await edit("a: 1\n# about b\nb: 2 # bee\n# about c\nc: 3\n", docs => removeAt(docs, ["b"])))
            .toBe("a: 1\n# about b\n# about c\nc: 3\n");
    });

    test("removes the first entry of the root mapping", async () => {
        expect(await edit("a: 1 # one\nb: 2\n", docs => removeAt(docs, ["a"]))).toBe("b: 2\n");
    });

    test("removes the last entry of the root mapping", async () => {
        expect(await edit("a: 1\nb: 2 # two\n", docs => removeAt(docs, ["b"]))).toBe("a: 1\n");
    });

    test("removes the last entry of a nested mapping", async () => {
        expect(await edit("m:\n  a: 1\n  b: 2 # two\nn: 3\n", docs => removeAt(docs, ["m", "b"]))).toBe("m:\n  a: 1\nn: 3\n");
    });

    test("leaves {} for a block mapping without entries", async () => {
        expect(await edit("m:\n  a: 1\nn: 3\n", docs => removeAt(docs, ["m", "a"]))).toBe("m: {}\nn: 3\n");
        expect(await edit("a: 1\n", docs => removeAt(docs, ["a"]))).toBe("{}\n");
    });

    test("removes an entry with a block scalar value", async () => {
        expect(await edit("a: |\n  x\nb: 1\n", docs => removeAt(docs, ["a"]))).toBe("b: 1\n");
        expect(await edit("b: 1\na: |\n  x\n", docs => removeAt(docs, ["a"]))).toBe("b: 1\n");
    });

    test("removes entries of a flow mapping", async () => {
        const text = "x: {a: 1, b: 2, c: 3}\n";
        expect(await edit(text, docs => removeAt(docs, ["x", "a"]))).toBe("x: {b: 2, c: 3}\n");
        expect(await edit(text, docs => removeAt(docs, ["x", "b"]))).toBe("x: {a: 1, c: 3}\n");
        expect(await edit(text, docs => removeAt(docs, ["x", "c"]))).toBe("x: {a: 1, b: 2}\n");
        expect(await edit("x: {a: 1, b: 2, }\n", docs => removeAt(docs, ["x", "b"]))).toBe("x: {a: 1, }\n");
        expect(await edit("x: {a: 1}\n", docs => removeAt(docs, ["x", "a"]))).toBe("x: {}\n");
    });
});

describe("sequence items", () => {
    test("removes items of a flow sequence", async () => {
        expect(await editSequence("node-version: [8.x, 10.x, 18.x, 20.x]\n", ["node-version"], seq => removeItems(seq, below(18))))
            .toBe("node-version: [18.x, 20.x]\n");
        expect(await editSequence("v: [8, 10, 12]\n", ["v"], seq => removeItems(seq, item => toPlain(item) === 12)))
            .toBe("v: [8, 10]\n");
        expect(await editSequence("v: [8, 10]\n", ["v"], seq => removeItems(seq, () => true))).toBe("v: []\n");
    });

    test("keeps the comment of the item before a removed last item of a flow sequence laid out on lines", async () => {
        expect(await editSequence("a: [\n  1, # one\n  2 # two\n]\n", ["a"], seq => removeItems(seq, item => toPlain(item) === 2)))
            .toBe("a: [\n  1 # one\n]\n");
    });

    test("removes items of a block sequence", async () => {
        expect(await editSequence("v:\n  - 8\n  - 10\n  - 18\nnext: 1\n", ["v"], seq => removeItems(seq, below(18))))
            .toBe("v:\n  - 18\nnext: 1\n");
        expect(await editSequence("v:\n  - 18\n  - 8 # eight\nnext: 1\n", ["v"], seq => removeItems(seq, below(18))))
            .toBe("v:\n  - 18\nnext: 1\n");
    });

    test("leaves [] for a block sequence without items", async () => {
        expect(await editSequence("v:\n  - 8\n  - 10\nnext: 1\n", ["v"], seq => removeItems(seq, () => true)))
            .toBe("v: []\nnext: 1\n");
        expect(await editSequence("v: # versions\n  - 8 # eight\nnext: 1\n", ["v"], seq => removeItems(seq, () => true)))
            .toBe("v: [] # versions\nnext: 1\n");
    });

    test("removes include entries of a GitHub Actions matrix", async () => {
        const text = "include:\n  - name: a\n    node-version: \"8\"\n\n  - name: b\n    node-version: \"18\"\n\n  - name: c\n    node-version: \"20\"\n";
        const old = (item: Yaml.Block) => parseInt(String(toPlain(valueAt(item, ["node-version"])!)), 10) < 18;
        expect(await editSequence(text, ["include"], seq => removeItems(seq, old)))
            .toBe("include:\n  - name: b\n    node-version: \"18\"\n\n  - name: c\n    node-version: \"20\"\n");
    });

    test("removes an item after a block scalar", async () => {
        expect(await editSequence("a:\n  - |\n    x\n  - y\nb: 1\n", ["a"], seq => removeItems(seq, (_, i) => i === 1)))
            .toBe("a:\n  - |\n    x\nb: 1\n");
    });

    test("appends to a block sequence", async () => {
        expect(await editSequence("v:\n  - 8\nnext: 1\n", ["v"], seq => appendItem(seq, 18))).toBe("v:\n  - 8\n  - 18\nnext: 1\n");
        expect(await editSequence("v:\n  - 8 # eight\nnext: 1\n", ["v"], seq => appendItem(seq, 18)))
            .toBe("v:\n  - 8 # eight\n  - 18\nnext: 1\n");
    });

    test("appends to a flow sequence", async () => {
        expect(await editSequence("v: [8]\n", ["v"], seq => appendItem(seq, 18))).toBe("v: [8, 18]\n");
        expect(await editSequence("v: []\n", ["v"], seq => appendItem(seq, 18))).toBe("v: [18]\n");
        expect(await editSequence("v: [8, ]\n", ["v"], seq => appendItem(seq, 18))).toBe("v: [8, 18, ]\n");
        expect(await editSequence("a: [\n  1 # one\n]\n", ["a"], seq => appendItem(seq, 2))).toBe("a: [\n  1, # one\n  2\n]\n");
    });

    test("doesn't append an item that is already there when asked for unique items", async () => {
        const docs = await parse("v: [8, 18]\n");
        expect(updateAt(docs, ["v"], seq => appendItem(seq as Yaml.Sequence, 18, {unique: true}))).toBe(docs);
    });

    test("itemsOf gives the items", async () => {
        const seq = valueAt(await parse("v: [8, '10']\n"), ["v"]) as Yaml.Sequence;
        expect(itemsOf(seq).map(toPlain)).toEqual([8, "10"]);
    });
});

describe("documents", () => {
    test("keeps Windows line breaks", async () => {
        expect(await edit("a: 1\r\nb: 2\r\nc: 3\r\n", docs => removeAt(docs, ["b"]))).toBe("a: 1\r\nc: 3\r\n");
        expect(await edit("m:\r\n  a: 1\r\n", docs => setAt(docs, ["m", "b"], 2))).toBe("m:\r\n  a: 1\r\n  b: 2\r\n");
        expect(await edit("m:\r\n  a: 1\r\n", docs => setAt(docs, ["m", "z"], 0, {position: "first"})))
            .toBe("m:\r\n  z: 0\r\n  a: 1\r\n");
        expect(await edit("run: |\r\n  a\r\nnext: 1\r\n", docs => setAt(docs, ["run"], "b\nc\n")))
            .toBe("run: |\r\n  b\r\n  c\r\nnext: 1\r\n");
    });

    test("edits the first of several documents", async () => {
        expect(await edit("a: 1\n---\na: 1\n", docs => setAt(docs, ["a"], 2))).toBe("a: 2\n---\na: 1\n");
    });

    test("keeps the start of an explicit document", async () => {
        expect(await edit("# head\n---\na: 1\nb: 2\n", docs => removeAt(docs, ["a"]))).toBe("# head\n---\nb: 2\n");
    });

    test("replaces an alias", async () => {
        expect(await edit("a: &x 1\nb: *x\n", docs => setAt(docs, ["b"], 2))).toBe("a: &x 1\nb: 2\n");
    });

    test("removes the last item of a sequence of mappings at the end of the document", async () => {
        expect(await editSequence("include:\n  - name: a\n  - name: b # bee\n", ["include"], seq => removeItems(seq, (_, i) => i === 1)))
            .toBe("include:\n  - name: a\n");
    });

    test("appends to a sequence that starts on its item's dash", async () => {
        const docs = await new class extends YamlVisitor<number> {
            protected async visitSequence(seq: Yaml.Sequence, p: number): Promise<Yaml | undefined> {
                return seq.entries[0].block.kind === Yaml.Kind.Sequence ?
                    mapItems(seq, item => item.kind === Yaml.Kind.Sequence ? appendItem(item as Yaml.Sequence, "c") : item) :
                    seq;
            }
        }().visit(await parse("- - a\n- b\n"), 0);
        expect(await TreePrinters.print(docs as Yaml.Documents)).toBe("- - a\n  - c\n- b\n");
    });

    test("removes from a root sequence in a visitor", async () => {
        const docs = await new class extends YamlVisitor<number> {
            protected async visitSequence(seq: Yaml.Sequence, p: number): Promise<Yaml | undefined> {
                return removeItems(seq, item => toPlain(item) === "b");
            }
        }().visit(await parse("- a\n- b # bee\n# end\n"), 0);
        expect(await TreePrinters.print(docs as Yaml.Documents)).toBe("- a\n# end\n");
    });
});

describe("YamlVisitor", () => {
    test("keeps the line break after a block scalar it replaces", async () => {
        const docs = await new class extends YamlVisitor<number> {
            protected async visitScalar(scalar: Yaml.Scalar, p: number): Promise<Yaml | undefined> {
                return scalar.style === Yaml.ScalarStyle.LITERAL ? setScalarValue(scalar, 5) : scalar;
            }
        }().visit(await parse("a:\n  - |\n    x\nb: 1\n"), 0);
        expect(await TreePrinters.print(docs as Yaml.Documents)).toBe("a:\n  - 5\nb: 1\n");
    });

    test("keeps the line break after an empty value it fills in", async () => {
        const docs = await new class extends YamlVisitor<number> {
            protected async visitScalar(scalar: Yaml.Scalar, p: number): Promise<Yaml | undefined> {
                return scalar.value === "" ? setScalarValue(scalar, 1) : scalar;
            }
        }().visit(await parse("a:\nb: 1\n"), 0);
        expect(await TreePrinters.print(docs as Yaml.Documents)).toBe("a: 1\nb: 1\n");
    });
});

/**
 * The layouts that the Java parser, and the TypeScript parser before #10155, give some nodes, made from what the
 * TypeScript parser gives them now.
 */
const layouts = {
    /** The space before each item of a flow sequence is in the item's prefix, not the entry's. */
    javaFlowSequences: (docs: Yaml.Documents) => rearrange(docs, {
        sequence: seq => seq.openingBracketPrefix === undefined ? seq : {
            ...seq,
            entries: seq.entries.map(e => ({...e, prefix: "", block: {...e.block, prefix: e.prefix + e.block.prefix}}))
        }
    }),
    /** The blank lines after a block scalar are in it, not in the prefix of the next entry. */
    javaBlockScalars: (docs: Yaml.Documents) => rearrange(docs, {
        mapping: mapping => ({
            ...mapping,
            entries: mapping.entries.map((e, i, all) => {
                const next = all[i + 1];
                const scalar = e.value as Yaml.Scalar;
                return next && scalar.style === Yaml.ScalarStyle.LITERAL ? {...e, value: {...scalar, value: scalar.value + next.prefix}} : e;
            }).map((e, i, all) => i > 0 && (all[i - 1].value as Yaml.Scalar).style === Yaml.ScalarStyle.LITERAL ? {...e, prefix: ""} : e)
        })
    }),
    /** The space after an empty value is in its prefix, not in the prefix of the next entry. */
    emptyValuesBefore10155: (docs: Yaml.Documents) => rearrange(docs, {
        mapping: mapping => ({
            ...mapping,
            entries: mapping.entries.map((e, i, all) => {
                const next = all[i + 1];
                return next && (e.value as Yaml.Scalar).value === "" ? {...e, value: {...e.value, prefix: next.prefix}} : e;
            }).map((e, i, all) => i > 0 && (all[i - 1].value as Yaml.Scalar).value === "" ? {...e, prefix: ""} : e)
        })
    })
};

async function rearrange(docs: Yaml.Documents, change: {
    mapping?: (mapping: Yaml.Mapping) => Yaml.Mapping,
    sequence?: (seq: Yaml.Sequence) => Yaml.Sequence
}): Promise<Yaml.Documents> {
    const rearranged = await new class extends YamlVisitor<number> {
        protected async visitMapping(mapping: Yaml.Mapping, p: number): Promise<Yaml | undefined> {
            const visited = await super.visitMapping(mapping, p) as Yaml.Mapping;
            return change.mapping ? change.mapping(visited) : visited;
        }

        protected async visitSequence(seq: Yaml.Sequence, p: number): Promise<Yaml | undefined> {
            const visited = await super.visitSequence(seq, p) as Yaml.Sequence;
            return change.sequence ? change.sequence(visited) : visited;
        }
    }().visit(docs, 0) as Yaml.Documents;
    expect(await TreePrinters.print(rearranged)).toBe(await TreePrinters.print(docs));
    return rearranged;
}

async function editIn(layout: (docs: Yaml.Documents) => Promise<Yaml.Documents>, text: string,
                      fn: (docs: Yaml.Documents) => Yaml.Documents): Promise<string> {
    return TreePrinters.print(fn(await layout(await parse(text))));
}

const inSequence = (path: string[], fn: (seq: Yaml.Sequence) => Yaml.Sequence) =>
    (docs: Yaml.Documents) => updateAt(docs, path, old => fn(old as Yaml.Sequence));

describe("layouts of other parsers", () => {
    describe("flow sequences from the Java parser", () => {
        test("removes items", async () => {
            const layout = layouts.javaFlowSequences;
            expect(await editIn(layout, "v: [8.x, 10.x, 18.x, 20.x]\n", inSequence(["v"], seq => removeItems(seq, below(18)))))
                .toBe("v: [18.x, 20.x]\n");
            expect(await editIn(layout, "v: [8, 10, 12]\n", inSequence(["v"], seq => removeItems(seq, item => toPlain(item) === 12))))
                .toBe("v: [8, 10]\n");
            expect(await editIn(layout, "v: [8, 10]\n", inSequence(["v"], seq => removeItems(seq, () => true)))).toBe("v: []\n");
            expect(await editIn(layout, "a: [\n  1, # one\n  2 # two\n]\n", inSequence(["a"], seq => removeItems(seq, item => toPlain(item) === 2))))
                .toBe("a: [\n  1 # one\n]\n");
        });

        test("keeps the space before an item in the item", async () => {
            const docs = await layouts.javaFlowSequences(await parse("v: [8.x, 10.x, 18.x, 20.x]\n"));
            const seq = valueAt(updateAt(docs, ["v"], old => removeItems(old as Yaml.Sequence, below(18))), ["v"]) as Yaml.Sequence;
            expect(seq.entries.map(e => [e.prefix, e.block.prefix])).toEqual([["", ""], ["", " "]]);
            const appended = appendItem(seq, "22.x");
            expect(appended.entries.map(e => [e.prefix, e.block.prefix])).toEqual([["", ""], ["", " "], ["", " "]]);
        });

        test("appends items", async () => {
            const layout = layouts.javaFlowSequences;
            expect(await editIn(layout, "v: [8, 10]\n", inSequence(["v"], seq => appendItem(seq, 18)))).toBe("v: [8, 10, 18]\n");
            expect(await editIn(layout, "a: [\n  1 # one\n]\n", inSequence(["a"], seq => appendItem(seq, 2))))
                .toBe("a: [\n  1, # one\n  2\n]\n");
        });

        test("replaces items keeping their quoting", async () => {
            expect(await editIn(layouts.javaFlowSequences, "v: [8.x, '10.x']\n", inSequence(["v"], seq => mapItems(seq, () => "18.x"))))
                .toBe("v: [18.x, '18.x']\n");
        });
    });

    describe("block scalars from the Java parser", () => {
        test("keeps the blank line after a block scalar replaced by a plain value", async () => {
            expect(await editIn(layouts.javaBlockScalars, "a: |-\n  x\n\nb: 1\n", docs => setAt(docs, ["a"], 5)))
                .toBe("a: 5\n\nb: 1\n");
        });

        test("removes an entry with a block scalar value and the blank line after it", async () => {
            expect(await editIn(layouts.javaBlockScalars, "a: |-\n  x\n\nb: 1\n", docs => removeAt(docs, ["a"]))).toBe("b: 1\n");
        });
    });

    describe("empty values from the TypeScript parser before #10155", () => {
        test("fills in an empty value", async () => {
            expect(await editIn(layouts.emptyValuesBefore10155, "a:\nb: 1\n", docs => setAt(docs, ["a"], 18))).toBe("a: 18\nb: 1\n");
            expect(await editIn(layouts.emptyValuesBefore10155, "a: # c\nb: 1\n", docs => setAt(docs, ["a"], 18)))
                .toBe("a: 18 # c\nb: 1\n");
        });

        test("removes an entry with an empty value", async () => {
            expect(await editIn(layouts.emptyValuesBefore10155, "a:\nb: 1\n", docs => removeAt(docs, ["a"]))).toBe("b: 1\n");
            expect(await editIn(layouts.emptyValuesBefore10155, "x: 1\na:\nb: 1\n", docs => removeAt(docs, ["a"]))).toBe("x: 1\nb: 1\n");
        });
    });
});
