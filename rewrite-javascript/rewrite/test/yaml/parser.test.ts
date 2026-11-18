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
import {isDirective, isDocuments, isMapping, isScalar, isSequence, Yaml, yaml, YamlParser, YamlVisitor} from "../../src/yaml";
import {ExecutionContext, findMarker, printer, randomId} from "../../src";
import {dedent, fromVisitor, RecipeSpec} from "../../src/test";

async function parseAndPrint(yaml: string): Promise<string> {
    const parser = new YamlParser();
    const docs = await parser.parseOne({text: yaml, sourcePath: "test.yaml"}) as Yaml.Documents;
    const p = printer(docs);
    return p.print(docs);
}

async function parseYaml(yaml: string): Promise<Yaml.Documents> {
    const parser = new YamlParser();
    return await parser.parseOne({text: yaml, sourcePath: "test.yaml"}) as Yaml.Documents;
}

describe('YAML parser roundtrip', () => {

    test('simple key-value', async () => {
        const yaml = `name: my-project`;
        const result = await parseAndPrint(yaml);
        expect(result).toBe(yaml);
    });

    test('multiple key-value pairs', async () => {
        const yaml = `name: my-project
version: 0.0.1`;
        const result = await parseAndPrint(yaml);
        expect(result).toBe(yaml);
    });

    test('key-value with trailing newline', async () => {
        const yaml = `name: my-project
`;
        const result = await parseAndPrint(yaml);
        expect(result).toBe(yaml);
    });

    test('nested mapping', async () => {
        const yaml = `settings:
  debug: true
  count: 42`;
        const result = await parseAndPrint(yaml);
        expect(result).toBe(yaml);
    });

    test('block sequence', async () => {
        const yaml = `dependencies:
  - lodash
  - express`;
        const result = await parseAndPrint(yaml);
        expect(result).toBe(yaml);
    });

    test('block mapping inside sequence entry', async () => {
        const yaml = `items:
  - name: first
    value: 1`;
        const result = await parseAndPrint(yaml);
        expect(result).toBe(yaml);
    });

    test('block sequence inside sequence entry', async () => {
        const yaml = `matrix:
  - - a
    - b`;
        const result = await parseAndPrint(yaml);
        expect(result).toBe(yaml);
    });

    test('inline sequence', async () => {
        const yaml = `items: [a, b, c]`;
        const result = await parseAndPrint(yaml);
        expect(result).toBe(yaml);
    });

    test('inline mapping', async () => {
        const yaml = `person: {name: John, age: 30}`;
        const result = await parseAndPrint(yaml);
        expect(result).toBe(yaml);
    });

    test('trailing comma in inline mapping', async () => {
        const yaml = `obj: {a: 1, b: 2,}`;
        const result = await parseAndPrint(yaml);
        expect(result).toBe(yaml);
    });

    test('double quoted string', async () => {
        const yaml = `message: "hello world"`;
        const result = await parseAndPrint(yaml);
        expect(result).toBe(yaml);
    });

    test('single quoted string', async () => {
        const yaml = `message: 'hello world'`;
        const result = await parseAndPrint(yaml);
        expect(result).toBe(yaml);
    });

    test('comment', async () => {
        const yaml = `# This is a comment
name: value`;
        const result = await parseAndPrint(yaml);
        expect(result).toBe(yaml);
    });

    test('inline comment', async () => {
        const yaml = `name: value # inline comment`;
        const result = await parseAndPrint(yaml);
        expect(result).toBe(yaml);
    });

    test('standalone comment in mapping', async () => {
        const yaml = `global:
  scrape_interval: 15s
  # standalone comment

alerting:
  enabled: true`;
        const result = await parseAndPrint(yaml);
        expect(result).toBe(yaml);
    });

    test('explicit document start', async () => {
        const yaml = `---
name: value`;
        const result = await parseAndPrint(yaml);
        expect(result).toBe(yaml);
    });

    test('explicit document end', async () => {
        const yaml = `name: value
...`;
        const result = await parseAndPrint(yaml);
        expect(result).toBe(yaml);
    });

    test('multiple documents', async () => {
        const yaml = `---
first: doc
...
---
second: doc`;
        const result = await parseAndPrint(yaml);
        expect(result).toBe(yaml);
    });

    test('anchor and alias', async () => {
        const yaml = `defaults: &defaults
  timeout: 30
development:
  <<: *defaults`;
        const result = await parseAndPrint(yaml);
        expect(result).toBe(yaml);
    });

    test('literal block scalar', async () => {
        const yaml = `message: |
  This is a
  multiline string`;
        const result = await parseAndPrint(yaml);
        expect(result).toBe(yaml);
    });

    test('folded block scalar', async () => {
        const yaml = `message: >
  This should be
  folded together`;
        const result = await parseAndPrint(yaml);
        expect(result).toBe(yaml);
    });

    test('local tag', async () => {
        const yaml = `tagged: !custom value`;
        const result = await parseAndPrint(yaml);
        expect(result).toBe(yaml);
    });

    test('global tag', async () => {
        const yaml = `typed: !!str 123`;
        const result = await parseAndPrint(yaml);
        expect(result).toBe(yaml);
    });

    test('complex document with comments and various styles', async () => {
        const yaml = `# Configuration file
name: my-project
version: 0.0.1

# Dependencies
dependencies:
  - lodash
  - express

settings:
  debug: true
  env: "production"
  values: [1, 2, 3]`;
        const result = await parseAndPrint(yaml);
        expect(result).toBe(yaml);
    });

    test('preserves whitespace in inline collections', async () => {
        const yaml = `items: [ a , b , c ]`;
        const result = await parseAndPrint(yaml);
        expect(result).toBe(yaml);
    });

    test('empty value', async () => {
        const yaml = `key:`;
        const result = await parseAndPrint(yaml);
        expect(result).toBe(yaml);
    });

    test('empty document', async () => {
        const yaml = ``;
        const result = await parseAndPrint(yaml);
        expect(result).toBe(yaml);
    });
});

describe('YAML parser AST structure', () => {

    test('parses to Documents', async () => {
        const docs = await parseYaml(`name: value`);
        expect(isDocuments(docs)).toBe(true);
        expect(docs.documents.length).toBe(1);
    });

    test('document contains mapping', async () => {
        const docs = await parseYaml(`name: value`);
        const block = docs.documents[0].block;
        expect(isMapping(block)).toBe(true);
    });

    test('mapping contains entries', async () => {
        const docs = await parseYaml(`name: value`);
        const mapping = docs.documents[0].block as Yaml.Mapping;
        expect(mapping.entries.length).toBe(1);
        expect(isScalar(mapping.entries[0].key)).toBe(true);
        expect((mapping.entries[0].key as Yaml.Scalar).value).toBe('name');
    });

    test('sequence contains entries', async () => {
        const docs = await parseYaml(`items:
  - a
  - b`);
        const mapping = docs.documents[0].block as Yaml.Mapping;
        const seq = mapping.entries[0].value as Yaml.Sequence;
        expect(isSequence(seq)).toBe(true);
        expect(seq.entries.length).toBe(2);
    });

    test('scalar style detection', async () => {
        const docs = await parseYaml(`plain: hello
single: 'hello'
double: "hello"`);
        const mapping = docs.documents[0].block as Yaml.Mapping;

        const plain = mapping.entries[0].value as Yaml.Scalar;
        expect(plain.style).toBe(Yaml.ScalarStyle.PLAIN);

        const single = mapping.entries[1].value as Yaml.Scalar;
        expect(single.style).toBe(Yaml.ScalarStyle.SINGLE_QUOTED);

        const double = mapping.entries[2].value as Yaml.Scalar;
        expect(double.style).toBe(Yaml.ScalarStyle.DOUBLE_QUOTED);
    });

    test('literal and folded scalars', async () => {
        const docs = await parseYaml(`literal: |
  text
folded: >
  text`);
        const mapping = docs.documents[0].block as Yaml.Mapping;

        const literal = mapping.entries[0].value as Yaml.Scalar;
        expect(literal.style).toBe(Yaml.ScalarStyle.LITERAL);

        const folded = mapping.entries[1].value as Yaml.Scalar;
        expect(folded.style).toBe(Yaml.ScalarStyle.FOLDED);
    });

    test('inline vs block sequence detection', async () => {
        const docs = await parseYaml(`inline: [a, b]
block:
  - a
  - b`);
        const mapping = docs.documents[0].block as Yaml.Mapping;

        const inline = mapping.entries[0].value as Yaml.Sequence;
        expect(inline.openingBracketPrefix).toBeDefined();

        const block = mapping.entries[1].value as Yaml.Sequence;
        expect(block.openingBracketPrefix).toBeUndefined();
    });

    test('inline vs block mapping detection', async () => {
        const docs = await parseYaml(`inline: {a: 1}
block:
  a: 1`);
        const mapping = docs.documents[0].block as Yaml.Mapping;

        const inline = mapping.entries[0].value as Yaml.Mapping;
        expect(inline.openingBracePrefix).toBeDefined();

        const block = mapping.entries[1].value as Yaml.Mapping;
        expect(block.openingBracePrefix).toBeUndefined();
    });
});

describe('yaml assertion helper', () => {
    const spec = new RecipeSpec();

    test('simple mapping', () =>
        spec.rewriteRun(
            yaml('key: value')
        ));
});

describe('YAML directives roundtrip', () => {

    test('YAML directive', async () => {
        const yaml = `%YAML 1.2
---
key: value`;
        const result = await parseAndPrint(yaml);
        expect(result).toBe(yaml);
    });

    test('TAG directive', async () => {
        const yaml = `%TAG !yaml! tag:yaml.org,2002:
---
key: value`;
        const result = await parseAndPrint(yaml);
        expect(result).toBe(yaml);
    });

    test('multiple directives', async () => {
        const yaml = `%YAML 1.2
%TAG !yaml! tag:yaml.org,2002:
---
key: value`;
        const result = await parseAndPrint(yaml);
        expect(result).toBe(yaml);
    });

    test('directive with leading newline', async () => {
        const yaml = `
%YAML 1.2
---
key: value`;
        const result = await parseAndPrint(yaml);
        expect(result).toBe(yaml);
    });

    test('document without directives', async () => {
        const yaml = `---
key: value`;
        const result = await parseAndPrint(yaml);
        expect(result).toBe(yaml);
    });
});

describe('YAML directives AST structure', () => {

    test('parses YAML directive', async () => {
        // given
        const docs = await parseYaml(`%YAML 1.2
---
key: value`);

        // then
        const doc = docs.documents[0];
        expect(doc.directives.length).toBe(1);
        const directive = doc.directives[0];
        expect(isDirective(directive)).toBe(true);
        expect(directive.value).toBe('YAML 1.2');
    });

    test('parses TAG directive', async () => {
        // given
        const docs = await parseYaml(`%TAG !yaml! tag:yaml.org,2002:
---
key: value`);

        // then
        const doc = docs.documents[0];
        expect(doc.directives.length).toBe(1);
        const directive = doc.directives[0];
        expect(directive.value).toBe('TAG !yaml! tag:yaml.org,2002:');
    });

    test('parses multiple directives', async () => {
        // given
        const docs = await parseYaml(`%YAML 1.2
%TAG !yaml! tag:yaml.org,2002:
---
key: value`);

        // then
        const doc = docs.documents[0];
        expect(doc.directives.length).toBe(2);
        expect(doc.directives[0].value).toBe('YAML 1.2');
        expect(doc.directives[1].value).toBe('TAG !yaml! tag:yaml.org,2002:');
    });

    test('document without directives has empty directives list', async () => {
        // given
        const docs = await parseYaml(`---
key: value`);

        // then
        const doc = docs.documents[0];
        expect(doc.directives.length).toBe(0);
    });
});

describe('Flow mappings without colons (OmitColon marker)', () => {

    test('flow mapping entries without colons roundtrip', async () => {
        // Flow mappings like { "MV7", "7J04" } have entries without explicit colons
        const yaml = `example:
  application/json:
    {
      "MV7",
      "7J04"
    }
`;
        const result = await parseAndPrint(yaml);
        expect(result).toBe(yaml);
    });

    test('flow mapping entries without colons have OmitColon marker', async () => {
        const docs = await parseYaml(`obj: {"MV7", "7J04"}`);
        const mapping = docs.documents[0].block as Yaml.Mapping;
        const flowMapping = mapping.entries[0].value as Yaml.Mapping;

        // Each entry in the flow mapping should have an OmitColon marker
        for (const entry of flowMapping.entries) {
            const omitColonMarker = findMarker(entry, Yaml.Markers.OmitColon);
            expect(omitColonMarker).toBeDefined();
        }
    });

    test('flow mapping entries with colons do not have OmitColon marker', async () => {
        const docs = await parseYaml(`obj: {a: 1, b: 2}`);
        const mapping = docs.documents[0].block as Yaml.Mapping;
        const flowMapping = mapping.entries[0].value as Yaml.Mapping;

        // Each entry in the flow mapping should NOT have an OmitColon marker
        for (const entry of flowMapping.entries) {
            const omitColonMarker = findMarker(entry, Yaml.Markers.OmitColon);
            expect(omitColonMarker).toBeUndefined();
        }
    });

    test('simple inline flow mapping without colons', async () => {
        const yaml = `obj: {"key1", "key2"}`;
        const result = await parseAndPrint(yaml);
        expect(result).toBe(yaml);
    });

    test('mixed flow mapping with and without colons', async () => {
        // Note: This is technically invalid YAML mixing styles, but we test roundtrip
        const yaml = `obj: {a: 1, "standalone"}`;
        const result = await parseAndPrint(yaml);
        expect(result).toBe(yaml);
    });
});

describe('Anchors on sequence entries', () => {

    test('anchor on mapping in sequence entry', async () => {
        const yaml = `- k: v
- &b
  k2: v2`;
        const result = await parseAndPrint(yaml);
        expect(result).toBe(yaml);
    });
});

describe('Single-brace template syntax', () => {

    test('quoted single-brace templates roundtrip', async () => {
        // Single-brace placeholders like {C App} are sometimes used as placeholders
        // When quoted, they're valid YAML strings
        const yaml = `swagger: '2.0'
host: "{C App}.colruyt.int/{C App}"`;
        const result = await parseAndPrint(yaml);
        expect(result).toBe(yaml);
    });

    test('unquoted single-brace templates roundtrip', async () => {
        // Unquoted {C App} patterns should also be handled correctly
        const yaml = `swagger: '2.0'
host: {C App}.colruyt.int/{C App}`;
        const result = await parseAndPrint(yaml);
        expect(result).toBe(yaml);
    });
});

describe('Multi-line flow scalars', () => {
    const spec = new RecipeSpec();

    test('plain scalar continued on the following lines', () =>
        spec.rewriteRun(
            yaml(`
                build:
                  - script: npm ci &&
                            npm test &&
                            npm run build
            `)
        ));

    test('plain scalar with a blank line and extra indentation', () =>
        spec.rewriteRun(
            yaml(`
                a: one
                  two

                    three
            `)
        ));

    test('plain scalars in flow collections', () =>
        spec.rewriteRun(
            yaml(`
                seq: [one
                  two, three]
                map: {a: one
                  two}
            `)
        ));

    test('single-quoted scalar with an escaped quote and a line break', () =>
        spec.rewriteRun(
            yaml(`
                a: 'it''s
                  continued'
            `)
        ));

    test('double-quoted scalar with escapes and an escaped line break', () =>
        spec.rewriteRun(
            yaml(String.raw`
                a: "tab\t, quote \", escaped \
                  line break"
            `)
        ));

    test('scalar value is the source text between any quotes', async () => {
        const docs = await parseYaml(dedent(String.raw`
            plain: one
              two
            single: 'it''s'
            double: "a\tb"
        `));
        const [plain, single, double] = (docs.documents[0].block as Yaml.Mapping).entries
            .map(e => (e.value as Yaml.Scalar).value);

        expect(plain).toBe(dedent(`
            one
              two`));
        expect(single).toBe("it''s");
        expect(double).toBe(String.raw`a\tb`);
    });
});

describe('Anchor and tag on a scalar', () => {
    const spec = new RecipeSpec();

    test('either order, on every scalar style and position', () =>
        spec.rewriteRun(
            yaml(`
                plain: &a !!str one
                single: &b !!str 'two'
                double: &c !!str "three"
                literal: &d !!str |
                  four
                folded: &e !!str >-
                  five
                tagFirst: !!str &f six
                seq:
                  - &g !!str seven
                  - !!str &h eight
                flowSeq: [&i !!str nine, !!str &j ten]
                flowMap: {k: &k !!str eleven}
                &l !!str key: twelve
                aliases: [*a, *d, *g]
            `)
        ));
});

describe('Tag on a mapping or sequence', () => {
    const spec = new RecipeSpec();

    test('block and flow collections, with and without an anchor', () =>
        spec.rewriteRun(
            yaml(`
                map: !!map
                  k: v
                anchoredMap: &m !!map
                  k: v
                seq: !!seq
                  - x
                anchoredSeq: &s !!seq
                  - x
                flowMap: !!map {k: v}
                flowSeq: &f !!seq [x]
                local: !custom
                  k: v
                entries:
                  - !!map
                    k: v
                  - !!seq [x]
                aliases: [*m, *s, *f]
            `)
        ));
});

describe('Text after the last document', () => {
    const spec = new RecipeSpec();

    test('newline, blank lines and comments after the last document end marker', () =>
        spec.rewriteRun(
            yaml(`
                a: b
                ...
            `),
            yaml(`
                ---
                a: b
                ...


            `),
            yaml(`
                a: b
                ...
                ---
                c: d
                ...
                # trailing
            `)
        ));

    test('file with only a comment', () =>
        spec.rewriteRun(
            yaml(`
                # only a comment
            `)
        ));
});

describe('Comment after the last entry of a block sequence', () => {
    const spec = new RecipeSpec();
    spec.recipe = fromVisitor(new class extends YamlVisitor<ExecutionContext> {
        protected async visitSequence(sequence: Yaml.Sequence, p: ExecutionContext): Promise<Yaml | undefined> {
            const first = sequence.entries[0];
            if ((sequence.entries[sequence.entries.length - 1].block as Yaml.Scalar).value === 'c') {
                return sequence;
            }
            const appended: Yaml.Sequence = {
                ...sequence,
                entries: [...sequence.entries, {...first, id: randomId(), block: {...first.block as Yaml.Scalar, id: randomId(), value: 'c'}}]
            };
            return appended;
        }
    });

    test('an appended entry goes before the comment', () =>
        spec.rewriteRun(
            yaml(
                `
                steps:
                  - a
                  # b
                `,
                `
                steps:
                  - a
                  - c
                  # b
                `
            )
        ));

    test('comment at the sequence indent before the next key', () =>
        spec.rewriteRun(
            yaml(
                `
                steps:
                  - a
                  # b
                other: d
                `,
                `
                steps:
                  - a
                  - c
                  # b
                other: d
                `
            )
        ));

    test('an empty last entry stays an entry', () =>
        spec.rewriteRun(
            yaml(
                `
                steps:
                  - a
                  -`,
                `
                steps:
                  - a
                  -
                  - c`
            )
        ));
});
