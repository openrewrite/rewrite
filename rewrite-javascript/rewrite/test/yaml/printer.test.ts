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
import {Yaml} from "../../src/yaml";
import {emptyMarkers, printer, randomId} from "../../src";

// Import the printer to register it
import "../../src/yaml/print";

function print(yaml: Yaml.Documents): string {
    const p = printer(yaml);
    return p.print(yaml);
}

describe('printing YAML', () => {

    test('print plain scalar', () => {
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

        const result = print(documents);
        expect(result).toBe("test");
    });

    test('print double quoted scalar', () => {
        const scalar: Yaml.Scalar = {
            kind: Yaml.Kind.Scalar,
            id: randomId(),
            prefix: "",
            markers: emptyMarkers,
            style: Yaml.ScalarStyle.DOUBLE_QUOTED,
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

        const result = print(documents);
        expect(result).toBe('"test"');
    });

    test('print single quoted scalar', () => {
        const scalar: Yaml.Scalar = {
            kind: Yaml.Kind.Scalar,
            id: randomId(),
            prefix: "",
            markers: emptyMarkers,
            style: Yaml.ScalarStyle.SINGLE_QUOTED,
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

        const result = print(documents);
        expect(result).toBe("'test'");
    });

    test('print mapping with entry', () => {
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
            explicit: false,
            block: mapping,
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

        const result = print(documents);
        expect(result).toBe("key: value");
    });

    test('print sequence with dash', () => {
        const item: Yaml.Scalar = {
            kind: Yaml.Kind.Scalar,
            id: randomId(),
            prefix: " ",
            markers: emptyMarkers,
            style: Yaml.ScalarStyle.PLAIN,
            anchor: undefined,
            tag: undefined,
            value: "item"
        };

        const entry: Yaml.SequenceEntry = {
            kind: Yaml.Kind.SequenceEntry,
            id: randomId(),
            prefix: "",
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
            explicit: false,
            block: sequence,
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

        const result = print(documents);
        expect(result).toBe("- item");
    });

    test('print explicit document start', () => {
        const scalar: Yaml.Scalar = {
            kind: Yaml.Kind.Scalar,
            id: randomId(),
            prefix: "\n",
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
            explicit: true,
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

        const result = print(documents);
        expect(result).toBe("---\ntest");
    });

    test('print explicit document end', () => {
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
            prefix: "\n",
            markers: emptyMarkers,
            explicit: true
        };

        const document: Yaml.Document = {
            kind: Yaml.Kind.Document,
            id: randomId(),
            prefix: "",
            markers: emptyMarkers,
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

        const result = print(documents);
        expect(result).toBe("test\n...");
    });

    test('print anchor', () => {
        const anchor: Yaml.Anchor = {
            kind: Yaml.Kind.Anchor,
            id: randomId(),
            prefix: "",
            markers: emptyMarkers,
            postfix: " ",
            key: "myanchor"
        };

        const scalar: Yaml.Scalar = {
            kind: Yaml.Kind.Scalar,
            id: randomId(),
            prefix: "",
            markers: emptyMarkers,
            style: Yaml.ScalarStyle.PLAIN,
            anchor,
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

        const result = print(documents);
        expect(result).toBe("&myanchor test");
    });

    test('print alias', () => {
        const anchor: Yaml.Anchor = {
            kind: Yaml.Kind.Anchor,
            id: randomId(),
            prefix: "",
            markers: emptyMarkers,
            postfix: "",
            key: "myanchor"
        };

        const alias: Yaml.Alias = {
            kind: Yaml.Kind.Alias,
            id: randomId(),
            prefix: "",
            markers: emptyMarkers,
            anchor
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
            explicit: false,
            block: alias,
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

        const result = print(documents);
        expect(result).toBe("*myanchor");
    });

    test('print inline sequence', () => {
        const item1: Yaml.Scalar = {
            kind: Yaml.Kind.Scalar,
            id: randomId(),
            prefix: "",
            markers: emptyMarkers,
            style: Yaml.ScalarStyle.PLAIN,
            anchor: undefined,
            tag: undefined,
            value: "a"
        };

        const item2: Yaml.Scalar = {
            kind: Yaml.Kind.Scalar,
            id: randomId(),
            prefix: " ",
            markers: emptyMarkers,
            style: Yaml.ScalarStyle.PLAIN,
            anchor: undefined,
            tag: undefined,
            value: "b"
        };

        const entry1: Yaml.SequenceEntry = {
            kind: Yaml.Kind.SequenceEntry,
            id: randomId(),
            prefix: "",
            markers: emptyMarkers,
            block: item1,
            dash: false,
            trailingCommaPrefix: ""
        };

        const entry2: Yaml.SequenceEntry = {
            kind: Yaml.Kind.SequenceEntry,
            id: randomId(),
            prefix: "",
            markers: emptyMarkers,
            block: item2,
            dash: false,
            trailingCommaPrefix: undefined
        };

        const sequence: Yaml.Sequence = {
            kind: Yaml.Kind.Sequence,
            id: randomId(),
            prefix: "",
            markers: emptyMarkers,
            openingBracketPrefix: "",
            entries: [entry1, entry2],
            closingBracketPrefix: "",
            anchor: undefined,
            tag: undefined
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
            explicit: false,
            block: sequence,
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

        const result = print(documents);
        expect(result).toBe("[a, b]");
    });
});
