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
import {Yaml, YamlVisitor} from "../../src/yaml";
import {emptyMarkers, randomId, Tree} from "../../src";

describe('visiting YAML', () => {

    test('preVisit', async () => {
        const scalar: Yaml.Scalar = {
            kind: Yaml.Kind.Scalar,
            id: randomId(),
            prefix: "",
            markers: emptyMarkers,
            style: Yaml.ScalarStyle.PLAIN,
            anchor: undefined,
            tag: undefined,
            value: "test"
        };

        const visitor = new class extends YamlVisitor<number> {
            protected async preVisit(y: Yaml, p: number): Promise<Yaml | undefined> {
                const newId = randomId();
                return {...y, id: newId} as Yaml;
            }
        }
        const result = await visitor.visit(scalar as Tree, 0);
        expect(result?.id).toBeDefined();
        expect(result?.id).not.toBe(scalar.id);
    });

    test('visit scalar', async () => {
        const scalar: Yaml.Scalar = {
            kind: Yaml.Kind.Scalar,
            id: randomId(),
            prefix: "",
            markers: emptyMarkers,
            style: Yaml.ScalarStyle.PLAIN,
            anchor: undefined,
            tag: undefined,
            value: "test"
        };

        const visitor = new class extends YamlVisitor<number> {
            protected async visitScalar(s: Yaml.Scalar, p: number): Promise<Yaml | undefined> {
                const modified: Yaml.Scalar = {...s, value: "modified"};
                return modified;
            }
        }

        const after = await visitor.visit<Yaml.Scalar>(scalar, 0);
        expect(after!.value).toBe("modified");
    });

    test('visit mapping', async () => {
        const keyScalar: Yaml.Scalar = {
            kind: Yaml.Kind.Scalar,
            id: randomId(),
            prefix: "",
            markers: emptyMarkers,
            style: Yaml.ScalarStyle.PLAIN,
            anchor: undefined,
            tag: undefined,
            value: "key"
        };

        const valueScalar: Yaml.Scalar = {
            kind: Yaml.Kind.Scalar,
            id: randomId(),
            prefix: " ",
            markers: emptyMarkers,
            style: Yaml.ScalarStyle.PLAIN,
            anchor: undefined,
            tag: undefined,
            value: "value"
        };

        const entry: Yaml.MappingEntry = {
            kind: Yaml.Kind.MappingEntry,
            id: randomId(),
            prefix: "",
            markers: emptyMarkers,
            key: keyScalar,
            beforeMappingValueIndicator: "",
            value: valueScalar
        };

        const mapping: Yaml.Mapping = {
            kind: Yaml.Kind.Mapping,
            id: randomId(),
            prefix: "",
            markers: emptyMarkers,
            openingBracePrefix: undefined,
            entries: [entry],
            closingBracePrefix: undefined,
            anchor: undefined,
            tag: undefined
        };

        const visitor = new class extends YamlVisitor<number> {
            protected async visitMappingEntry(e: Yaml.MappingEntry, p: number): Promise<Yaml | undefined> {
                const result = await super.visitMappingEntry(e, p);
                if (result && result.kind === Yaml.Kind.MappingEntry) {
                    const updated = result as Yaml.MappingEntry;
                    const modified: Yaml.MappingEntry = {...updated, beforeMappingValueIndicator: " "};
                    return modified;
                }
                return result;
            }
        }

        const after = await visitor.visit<Yaml.Mapping>(mapping, 0);
        expect(after!.entries[0].beforeMappingValueIndicator).toBe(" ");
    });

    test('visit sequence', async () => {
        const item: Yaml.Scalar = {
            kind: Yaml.Kind.Scalar,
            id: randomId(),
            prefix: " ",
            markers: emptyMarkers,
            style: Yaml.ScalarStyle.PLAIN,
            anchor: undefined,
            tag: undefined,
            value: "item1"
        };

        const entry: Yaml.SequenceEntry = {
            kind: Yaml.Kind.SequenceEntry,
            id: randomId(),
            prefix: "\n",
            markers: emptyMarkers,
            block: item,
            dash: true,
            trailingCommaPrefix: undefined
        };

        const sequence: Yaml.Sequence = {
            kind: Yaml.Kind.Sequence,
            id: randomId(),
            prefix: "",
            markers: emptyMarkers,
            openingBracketPrefix: undefined,
            entries: [entry],
            closingBracketPrefix: undefined,
            anchor: undefined,
            tag: undefined
        };

        const visitor = new class extends YamlVisitor<number> {
            protected async visitScalar(s: Yaml.Scalar, p: number): Promise<Yaml | undefined> {
                const modified: Yaml.Scalar = {...s, value: "modified"};
                return modified;
            }
        }

        const after = await visitor.visit<Yaml.Sequence>(sequence, 0);
        expect((after!.entries[0].block as Yaml.Scalar).value).toBe("modified");
    });

    test('visit document', async () => {
        const scalar: Yaml.Scalar = {
            kind: Yaml.Kind.Scalar,
            id: randomId(),
            prefix: "",
            markers: emptyMarkers,
            style: Yaml.ScalarStyle.PLAIN,
            anchor: undefined,
            tag: undefined,
            value: "test"
        };

        const documentEnd: Yaml.DocumentEnd = {
            kind: Yaml.Kind.DocumentEnd,
            id: randomId(),
            prefix: "",
            markers: emptyMarkers,
            explicit: false
        };

        const document: Yaml.Document = {
            kind: Yaml.Kind.Document,
            id: randomId(),
            prefix: "",
            markers: emptyMarkers,
            directives: [],
            explicit: false,
            block: scalar,
            end: documentEnd
        };

        const visitor = new class extends YamlVisitor<number> {
            protected async visitDocument(doc: Yaml.Document, p: number): Promise<Yaml | undefined> {
                const result = await super.visitDocument(doc, p);
                if (result && result.kind === Yaml.Kind.Document) {
                    const updated = result as Yaml.Document;
                    const modified: Yaml.Document = {...updated, explicit: true};
                    return modified;
                }
                return result;
            }
        }

        const after = await visitor.visit<Yaml.Document>(document, 0);
        expect(after!.explicit).toBe(true);
    });

    test('visit documents', async () => {
        const scalar: Yaml.Scalar = {
            kind: Yaml.Kind.Scalar,
            id: randomId(),
            prefix: "",
            markers: emptyMarkers,
            style: Yaml.ScalarStyle.PLAIN,
            anchor: undefined,
            tag: undefined,
            value: "test"
        };

        const documentEnd: Yaml.DocumentEnd = {
            kind: Yaml.Kind.DocumentEnd,
            id: randomId(),
            prefix: "",
            markers: emptyMarkers,
            explicit: false
        };

        const document: Yaml.Document = {
            kind: Yaml.Kind.Document,
            id: randomId(),
            prefix: "",
            markers: emptyMarkers,
            directives: [],
            explicit: false,
            block: scalar,
            end: documentEnd
        };

        const documents: Yaml.Documents = {
            kind: Yaml.Kind.Documents,
            id: randomId(),
            prefix: "",
            markers: emptyMarkers,
            sourcePath: "test.yaml",
            documents: [document],
            suffix: undefined
        };

        const visitor = new YamlVisitor<number>();
        const result = await visitor.isAcceptable(documents);
        expect(result).toBe(true);
    });
});
