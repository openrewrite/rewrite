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
import {Markers} from "../markers";
import {SourceFile, Tree} from "../tree";

export interface Yaml extends Tree {
    readonly prefix: string
}

export namespace Yaml {
    export const Kind = {
        Documents: "org.openrewrite.yaml.tree.Yaml$Documents",
        Document: "org.openrewrite.yaml.tree.Yaml$Document",
        DocumentEnd: "org.openrewrite.yaml.tree.Yaml$Document$End",
        Mapping: "org.openrewrite.yaml.tree.Yaml$Mapping",
        MappingEntry: "org.openrewrite.yaml.tree.Yaml$Mapping$Entry",
        Scalar: "org.openrewrite.yaml.tree.Yaml$Scalar",
        Sequence: "org.openrewrite.yaml.tree.Yaml$Sequence",
        SequenceEntry: "org.openrewrite.yaml.tree.Yaml$Sequence$Entry",
        Alias: "org.openrewrite.yaml.tree.Yaml$Alias",
        Anchor: "org.openrewrite.yaml.tree.Yaml$Anchor",
        Tag: "org.openrewrite.yaml.tree.Yaml$Tag"
    } as const;

    export enum ScalarStyle {
        DOUBLE_QUOTED = "DOUBLE_QUOTED",
        SINGLE_QUOTED = "SINGLE_QUOTED",
        LITERAL = "LITERAL",
        FOLDED = "FOLDED",
        PLAIN = "PLAIN"
    }

    export enum TagKind {
        LOCAL = "LOCAL",
        IMPLICIT_GLOBAL = "IMPLICIT_GLOBAL",
        EXPLICIT_GLOBAL = "EXPLICIT_GLOBAL"
    }

    /**
     * Either a Scalar or an Alias can be a key in a mapping
     */
    export type YamlKey = Scalar | Alias;

    /**
     * Block types are Scalar, Mapping, Sequence, or Alias
     */
    export type Block = Scalar | Mapping | Sequence | Alias;

    /**
     * A YAML file containing multiple documents
     */
    export interface Documents extends SourceFile, Yaml {
        readonly kind: typeof Kind.Documents;
        readonly documents: Document[];
        readonly suffix: string | undefined;
    }

    /**
     * A single YAML document within a Documents container
     */
    export interface Document extends Yaml {
        readonly kind: typeof Kind.Document;
        /**
         * True if the document explicitly starts with "---"
         */
        readonly explicit: boolean;
        readonly block: Block;
        readonly end: DocumentEnd;
    }

    /**
     * Document end marker. May or may not be explicit ("...")
     */
    export interface DocumentEnd extends Yaml {
        readonly kind: typeof Kind.DocumentEnd;
        /**
         * True if the document end is explicitly marked with "..."
         */
        readonly explicit: boolean;
    }

    /**
     * A scalar value in YAML (string, number, boolean, etc.)
     */
    export interface Scalar extends Yaml {
        readonly kind: typeof Kind.Scalar;
        readonly style: ScalarStyle;
        readonly anchor: Anchor | undefined;
        readonly tag: Tag | undefined;
        readonly value: string;
    }

    /**
     * A YAML mapping (object/dictionary)
     */
    export interface Mapping extends Yaml {
        readonly kind: typeof Kind.Mapping;
        /**
         * When non-null, indicates this is an inline mapping and contains the whitespace before '{'
         */
        readonly openingBracePrefix: string | undefined;
        readonly entries: MappingEntry[];
        /**
         * When non-null, indicates this is an inline mapping and contains the whitespace before '}'
         */
        readonly closingBracePrefix: string | undefined;
        readonly anchor: Anchor | undefined;
        readonly tag: Tag | undefined;
    }

    /**
     * A single key-value pair in a mapping
     */
    export interface MappingEntry extends Yaml {
        readonly kind: typeof Kind.MappingEntry;
        readonly key: YamlKey;
        /**
         * Whitespace before the mapping value indicator ':'
         */
        readonly beforeMappingValueIndicator: string;
        readonly value: Block;
    }

    /**
     * A YAML sequence (array/list)
     */
    export interface Sequence extends Yaml {
        readonly kind: typeof Kind.Sequence;
        /**
         * When non-null, indicates this is an inline sequence and contains the whitespace before '['
         */
        readonly openingBracketPrefix: string | undefined;
        readonly entries: SequenceEntry[];
        /**
         * When non-null, indicates this is an inline sequence and contains the whitespace before ']'
         */
        readonly closingBracketPrefix: string | undefined;
        readonly anchor: Anchor | undefined;
        readonly tag: Tag | undefined;
    }

    /**
     * A single entry in a sequence
     */
    export interface SequenceEntry extends Yaml {
        readonly kind: typeof Kind.SequenceEntry;
        readonly block: Block;
        /**
         * True when this entry is part of a block sequence (uses '-')
         * False when this entry is part of an inline sequence (uses '[' and ']')
         */
        readonly dash: boolean;
        /**
         * When non-null, contains the whitespace before the trailing comma in inline sequences
         */
        readonly trailingCommaPrefix: string | undefined;
    }

    /**
     * An alias reference to an anchor (*anchorName)
     */
    export interface Alias extends Yaml {
        readonly kind: typeof Kind.Alias;
        readonly anchor: Anchor;
    }

    /**
     * An anchor definition (&anchorName)
     */
    export interface Anchor extends Yaml {
        readonly kind: typeof Kind.Anchor;
        readonly postfix: string;
        readonly key: string;
    }

    /**
     * A YAML tag (!tag, !!tag, or !<tag>)
     */
    export interface Tag extends Yaml {
        readonly kind: typeof Kind.Tag;
        readonly name: string;
        readonly suffix: string;
        readonly tagKind: TagKind;
    }
}

const yamlKindValues = new Set(Object.values(Yaml.Kind));

export function isYaml(tree: any): tree is Yaml {
    return yamlKindValues.has(tree?.kind);
}

export function isDocuments(yaml: Yaml): yaml is Yaml.Documents {
    return yaml.kind === Yaml.Kind.Documents;
}

export function isDocument(yaml: Yaml): yaml is Yaml.Document {
    return yaml.kind === Yaml.Kind.Document;
}

export function isDocumentEnd(yaml: Yaml): yaml is Yaml.DocumentEnd {
    return yaml.kind === Yaml.Kind.DocumentEnd;
}

export function isScalar(yaml: Yaml): yaml is Yaml.Scalar {
    return yaml.kind === Yaml.Kind.Scalar;
}

export function isMapping(yaml: Yaml): yaml is Yaml.Mapping {
    return yaml.kind === Yaml.Kind.Mapping;
}

export function isMappingEntry(yaml: Yaml): yaml is Yaml.MappingEntry {
    return yaml.kind === Yaml.Kind.MappingEntry;
}

export function isSequence(yaml: Yaml): yaml is Yaml.Sequence {
    return yaml.kind === Yaml.Kind.Sequence;
}

export function isSequenceEntry(yaml: Yaml): yaml is Yaml.SequenceEntry {
    return yaml.kind === Yaml.Kind.SequenceEntry;
}

export function isAlias(yaml: Yaml): yaml is Yaml.Alias {
    return yaml.kind === Yaml.Kind.Alias;
}

export function isAnchor(yaml: Yaml): yaml is Yaml.Anchor {
    return yaml.kind === Yaml.Kind.Anchor;
}

export function isTag(yaml: Yaml): yaml is Yaml.Tag {
    return yaml.kind === Yaml.Kind.Tag;
}

export function isBlock(yaml: Yaml): yaml is Yaml.Block {
    return isScalar(yaml) || isMapping(yaml) || isSequence(yaml) || isAlias(yaml);
}

export function isYamlKey(yaml: Yaml): yaml is Yaml.YamlKey {
    return isScalar(yaml) || isAlias(yaml);
}

/**
 * Gets the value from a YamlKey (either a Scalar or an Alias)
 */
export function getYamlKeyValue(key: Yaml.YamlKey): string {
    if (isScalar(key)) {
        return key.value;
    } else {
        return key.anchor.key;
    }
}

/**
 * Prints a tag according to its kind
 */
export function printTag(tag: Yaml.Tag): string {
    switch (tag.tagKind) {
        case Yaml.TagKind.LOCAL:
            return `!${tag.name}`;
        case Yaml.TagKind.IMPLICIT_GLOBAL:
            return `!!${tag.name}`;
        case Yaml.TagKind.EXPLICIT_GLOBAL:
            return `!<${tag.name}>`;
    }
}
