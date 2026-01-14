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
import {isMapping, isSequence, Yaml, YamlParser} from "../../src/yaml";
import {printer} from "../../src";

async function parseAndPrint(yaml: string): Promise<string> {
    const parser = new YamlParser();
    const docs = await parser.parseOne({text: yaml, sourcePath: "test.yaml"});
    const p = printer(docs);
    return p.print(docs);
}

async function parseYaml(yaml: string): Promise<Yaml.Documents> {
    const parser = new YamlParser();
    return await parser.parseOne({text: yaml, sourcePath: "test.yaml"}) as Yaml.Documents;
}

describe('YAML parser CircleCI patterns', () => {

    test('anchor on sequence entry', async () => {
        const yaml = `aliases:
  - &restore_yarn_cache
    name: Restore Yarn cache
    keys:
      - yarn-packages`;
        const result = await parseAndPrint(yaml);
        expect(result).toBe(yaml);
    });

    test('anchor on mapping value', async () => {
        const yaml = `defaults: &defaults
  working_directory: ~/docsearch`;
        const result = await parseAndPrint(yaml);
        expect(result).toBe(yaml);
    });

    test('merge key with alias', async () => {
        const yaml = `defaults: &defaults
  working_directory: ~/docsearch

jobs:
  build:
    <<: *defaults`;
        const result = await parseAndPrint(yaml);
        expect(result).toBe(yaml);
    });

    test('alias as mapping value', async () => {
        const yaml = `aliases:
  - &restore_yarn_cache
    name: Restore Yarn cache

jobs:
  build:
    steps:
      - restore_cache: *restore_yarn_cache`;
        const result = await parseAndPrint(yaml);
        expect(result).toBe(yaml);
    });

    test('alias in sequence', async () => {
        const yaml = `aliases:
  - &attach_workspace
    attach_workspace:
      at: /tmp/workspace

jobs:
  build:
    steps:
      - *attach_workspace`;
        const result = await parseAndPrint(yaml);
        expect(result).toBe(yaml);
    });

    test('anchor on scalar value', async () => {
        const yaml = `references:
  workspace_root: &workspace_root /tmp/workspace
  attach_workspace:
    at: *workspace_root`;
        const result = await parseAndPrint(yaml);
        expect(result).toBe(yaml);
    });

    test('complex circleci-like config', async () => {
        const yaml = `aliases:
  - &restore_yarn_cache
    name: Restore Yarn cache
    keys:
      - yarn-packages

defaults: &defaults
  working_directory: ~/docsearch

jobs:
  build:
    <<: *defaults
    steps:
      - checkout
      - restore_cache: *restore_yarn_cache`;
        const result = await parseAndPrint(yaml);
        expect(result).toBe(yaml);
    });
});

describe('YAML anchor AST structure', () => {

    test('anchor on sequence entry is in anchor field, not prefix', async () => {
        const yaml = `aliases:
  - &restore_yarn_cache
    name: Restore Yarn cache`;

        const docs = await parseYaml(yaml);
        const root = docs.documents[0].block as Yaml.Mapping;
        const aliasesEntry = root.entries.find(e => (e.key as Yaml.Scalar).value === 'aliases');
        expect(aliasesEntry).toBeDefined();

        const sequence = aliasesEntry!.value as Yaml.Sequence;
        expect(isSequence(sequence)).toBe(true);

        const firstEntry = sequence.entries[0];
        const block = firstEntry.block as Yaml.Mapping;
        expect(isMapping(block)).toBe(true);

        // The anchor MUST be in the anchor field, not embedded in the prefix
        expect(block.anchor).toBeDefined();
        expect(block.anchor!.key).toBe('restore_yarn_cache');
        expect(block.prefix).toBe('');  // No anchor text in prefix
    });

    test('anchor on mapping value is in anchor field', async () => {
        const yaml = `defaults: &defaults
  working_directory: ~/docsearch`;

        const docs = await parseYaml(yaml);
        const root = docs.documents[0].block as Yaml.Mapping;
        const defaultsEntry = root.entries.find(e => (e.key as Yaml.Scalar).value === 'defaults');
        expect(defaultsEntry).toBeDefined();

        const mapping = defaultsEntry!.value as Yaml.Mapping;
        expect(isMapping(mapping)).toBe(true);

        expect(mapping.anchor).toBeDefined();
        expect(mapping.anchor!.key).toBe('defaults');
    });

    test('mappings and sequences always have empty prefix', async () => {
        // This constraint matches Java's Yaml.Mapping and Yaml.Sequence which
        // throw UnsupportedOperationException if you try to set a non-empty prefix
        const yaml = `jobs:
  build:
    steps:
      - restore_cache: *restore_yarn_cache
      - run: echo hello`;

        const docs = await parseYaml(yaml);

        function checkPrefixes(node: any): void {
            if (!node || typeof node !== 'object') return;

            if (node.kind === Yaml.Kind.Mapping) {
                expect(node.prefix).toBe('');
            }
            if (node.kind === Yaml.Kind.Sequence) {
                expect(node.prefix).toBe('');
            }

            // Recurse into children
            if (node.documents) node.documents.forEach(checkPrefixes);
            if (node.block) checkPrefixes(node.block);
            if (node.entries) {
                node.entries.forEach((entry: any) => {
                    checkPrefixes(entry.key);
                    checkPrefixes(entry.value);
                    checkPrefixes(entry.block);
                });
            }
        }

        checkPrefixes(docs);
    });
});
