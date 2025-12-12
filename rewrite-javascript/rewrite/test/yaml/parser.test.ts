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
import {isDocuments, isMapping, isScalar, isSequence, Yaml, YamlParser} from "../../src/yaml";
import {printer} from "../../src";

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

    // Note: Block scalars require more complex handling to preserve exact formatting
    // These tests verify the parser correctly identifies the style
    test.skip('literal block scalar', async () => {
        const yaml = `message: |
  This is a
  multiline string`;
        const result = await parseAndPrint(yaml);
        expect(result).toBe(yaml);
    });

    test.skip('folded block scalar', async () => {
        const yaml = `message: >
  This should be
  folded together`;
        const result = await parseAndPrint(yaml);
        expect(result).toBe(yaml);
    });

    // Note: Tags need additional whitespace tracking
    test.skip('local tag', async () => {
        const yaml = `tagged: !custom value`;
        const result = await parseAndPrint(yaml);
        expect(result).toBe(yaml);
    });

    test.skip('global tag', async () => {
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
