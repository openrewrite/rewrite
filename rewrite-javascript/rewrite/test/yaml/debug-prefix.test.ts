import {YamlParser} from "../../src/yaml/parser";
import {Yaml, isMapping, isSequence} from "../../src/yaml/tree";
import {TreePrinters} from "../../src/print";
import "../../src/yaml/print"; // register printer

function checkPrefixes(node: any, path: string = ""): string[] {
    const errors: string[] = [];
    if (!node || typeof node !== "object") return errors;

    if (isMapping(node)) {
        if (node.prefix !== "") {
            errors.push(`Mapping has non-empty prefix at ${path}: ${JSON.stringify(node.prefix)}`);
        }
        for (let i = 0; i < node.entries.length; i++) {
            errors.push(...checkPrefixes(node.entries[i], `${path}.entries[${i}]`));
        }
    } else if (isSequence(node)) {
        if (node.prefix !== "") {
            errors.push(`Sequence has non-empty prefix at ${path}: ${JSON.stringify(node.prefix)}`);
        }
        for (let i = 0; i < node.entries.length; i++) {
            errors.push(...checkPrefixes(node.entries[i], `${path}.entries[${i}]`));
        }
    } else if (node.kind === Yaml.Kind.Documents) {
        for (let i = 0; i < node.documents.length; i++) {
            errors.push(...checkPrefixes(node.documents[i], `documents[${i}]`));
        }
    } else if (node.kind === Yaml.Kind.Document) {
        errors.push(...checkPrefixes(node.block, `${path}.block`));
    } else if (node.kind === Yaml.Kind.MappingEntry) {
        errors.push(...checkPrefixes(node.key, `${path}.key`));
        errors.push(...checkPrefixes(node.value, `${path}.value`));
    } else if (node.kind === Yaml.Kind.SequenceEntry) {
        errors.push(...checkPrefixes(node.block, `${path}.block`));
    }
    return errors;
}

describe("YAML parser Mapping/Sequence prefix invariant", () => {
    test("pnpm-lock.yaml style nested block mappings", async () => {
        const yaml = `lockfileVersion: '9.0'

settings:
  autoInstallPeers: true
  excludeLinksFromLockfile: false

importers:

  .:
    dependencies:
      rou3:
        specifier: ^0.7.12
        version: 0.7.12

packages:

  '@babel/generator@8.0.0-rc.1':
    resolution: {integrity: sha512-abc}
    engines: {node: '>=6.9.0'}

  '@babel/parser@7.29.0':
    resolution: {integrity: sha512-def}
    engines: {node: '>=6.0.0'}
    hasBin: true
`;
        const parser = new YamlParser();
        for await (const sf of parser.parse({sourcePath: "pnpm-lock.yaml", text: yaml})) {
            const docs = sf as Yaml.Documents;
            const errors = checkPrefixes(docs);
            if (errors.length > 0) {
                console.log("Prefix violations found:");
                errors.forEach(e => console.log("  " + e));
            }
            expect(errors).toEqual([]);
        }
    });

    test("simple nested block mapping", async () => {
        const yaml = `a:
  b:
    c: d
`;
        const parser = new YamlParser();
        for await (const sf of parser.parse({sourcePath: "test.yaml", text: yaml})) {
            const docs = sf as Yaml.Documents;
            const errors = checkPrefixes(docs);
            expect(errors).toEqual([]);
        }
    });

    test("flow mapping as value in block mapping", async () => {
        const yaml = `resolution: {integrity: sha512-abc}
engines: {node: '>=6.9.0'}
`;
        const parser = new YamlParser();
        for await (const sf of parser.parse({sourcePath: "test.yaml", text: yaml})) {
            const docs = sf as Yaml.Documents;
            const errors = checkPrefixes(docs);
            expect(errors).toEqual([]);
        }
    });

    test("nested flow mapping", async () => {
        const yaml = `{a: {b: c}}
`;
        const parser = new YamlParser();
        for await (const sf of parser.parse({sourcePath: "test.yaml", text: yaml})) {
            const docs = sf as Yaml.Documents;
            const errors = checkPrefixes(docs);
            if (errors.length > 0) {
                console.log("Prefix violations found:");
                errors.forEach(e => console.log("  " + e));
            }
            expect(errors).toEqual([]);
            // Verify print idempotency
            const printed = await TreePrinters.print(docs);
            expect(printed).toBe(yaml);
        }
    });

    test("nested flow mapping with spaces", async () => {
        const yaml = `{a: {b: c}, d: {e: f}}
`;
        const parser = new YamlParser();
        for await (const sf of parser.parse({sourcePath: "test.yaml", text: yaml})) {
            const docs = sf as Yaml.Documents;
            const errors = checkPrefixes(docs);
            expect(errors).toEqual([]);
            const printed = await TreePrinters.print(docs);
            expect(printed).toBe(yaml);
        }
    });

    test("flow sequence with nested flow mapping", async () => {
        const yaml = `[{a: 1}, {b: 2}]
`;
        const parser = new YamlParser();
        for await (const sf of parser.parse({sourcePath: "test.yaml", text: yaml})) {
            const docs = sf as Yaml.Documents;
            const errors = checkPrefixes(docs);
            expect(errors).toEqual([]);
            const printed = await TreePrinters.print(docs);
            expect(printed).toBe(yaml);
        }
    });

    test("pnpm-lock.yaml style with flow mappings prints idempotently", async () => {
        const yaml = `packages:

  '@babel/generator@8.0.0-rc.1':
    resolution: {integrity: sha512-abc}
    engines: {node: '>=6.9.0'}
`;
        const parser = new YamlParser();
        for await (const sf of parser.parse({sourcePath: "test.yaml", text: yaml})) {
            const docs = sf as Yaml.Documents;
            const errors = checkPrefixes(docs);
            expect(errors).toEqual([]);
            const printed = await TreePrinters.print(docs);
            expect(printed).toBe(yaml);
        }
    });

    test("block sequence with block mapping values", async () => {
        const yaml = `packages:
  - playground
  - examples

ignoredBuiltDependencies:
  - esbuild
`;
        const parser = new YamlParser();
        for await (const sf of parser.parse({sourcePath: "test.yaml", text: yaml})) {
            const docs = sf as Yaml.Documents;
            const errors = checkPrefixes(docs);
            expect(errors).toEqual([]);
        }
    });

    test("real pnpm-lock.yaml from h3 project", async () => {
        const fs = require('fs');
        const filePath = '/Users/knut/moderne/working-set-js-ts-static-analysis/h3js/h3/pnpm-lock.yaml';
        if (!fs.existsSync(filePath)) {
            console.log("Skipping: file not found");
            return;
        }
        const yaml = fs.readFileSync(filePath, 'utf8');
        const parser = new YamlParser();
        for await (const sf of parser.parse({sourcePath: "pnpm-lock.yaml", text: yaml})) {
            const docs = sf as Yaml.Documents;
            const errors = checkPrefixes(docs);
            if (errors.length > 0) {
                console.log("Prefix violations found (first 10):");
                errors.slice(0, 10).forEach(e => console.log("  " + e));
            }
            expect(errors).toEqual([]);
            // Also check print idempotency
            const printed = await TreePrinters.print(docs);
            expect(printed).toBe(yaml);
        }
    }, 30000);

    test("block mapping value after blank line", async () => {
        const yaml = `importers:

  .:
    dependencies:
      rou3:
        specifier: ^0.7.12
`;
        const parser = new YamlParser();
        for await (const sf of parser.parse({sourcePath: "test.yaml", text: yaml})) {
            const docs = sf as Yaml.Documents;
            const errors = checkPrefixes(docs);
            if (errors.length > 0) {
                console.log("Prefix violations found:");
                errors.forEach(e => console.log("  " + e));
            }
            expect(errors).toEqual([]);
        }
    });
});
