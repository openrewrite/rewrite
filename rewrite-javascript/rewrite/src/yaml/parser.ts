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
import {emptyMarkers, markers, MarkersKind, ParseExceptionResult} from "../markers";
import {Parser, ParserInput, parserInputRead, Parsers} from "../parser";
import {randomId} from "../uuid";
import {SourceFile} from "../tree";
import {Yaml} from "./tree";
import {ParseError, ParseErrorKind} from "../parse-error";
import {Parser as YamlCstParser, CST} from "yaml";

// Types from yaml package CST
type CstToken = CST.Token;
type CstSourceToken = CST.SourceToken;
type CstDocument = CST.Document;
type CstDocumentEnd = CST.DocumentEnd;
type CstBlockMap = CST.BlockMap;
type CstBlockSequence = CST.BlockSequence;
type CstFlowCollection = CST.FlowCollection;
type CstFlowScalar = CST.FlowScalar;
type CstBlockScalar = CST.BlockScalar;
type CstCollectionItem = CST.CollectionItem;

export class YamlParser extends Parser {

    async *parse(...sourcePaths: ParserInput[]): AsyncGenerator<SourceFile> {
        for (const sourcePath of sourcePaths) {
            const text = parserInputRead(sourcePath);
            try {
                yield {
                    ...new YamlCstReader(text).parse(),
                    sourcePath: this.relativePath(sourcePath)
                };
            } catch (e: any) {
                // Return a ParseError for files that can't be parsed
                const parseError: ParseError = {
                    kind: ParseErrorKind,
                    id: randomId(),
                    markers: markers({
                        kind: MarkersKind.ParseExceptionResult,
                        id: randomId(),
                        parserType: "YamlParser",
                        exceptionType: e.name || "Error",
                        message: e.message || "Unknown parse error"
                    } satisfies ParseExceptionResult as ParseExceptionResult),
                    sourcePath: this.relativePath(sourcePath),
                    text
                };
                yield parseError;
            }
        }
    }
}

/**
 * Converts YAML CST from the 'yaml' package to our Yaml AST.
 * The CST preserves all whitespace, comments, and formatting.
 */
class YamlCstReader {
    private readonly cstTokens: CstToken[];

    constructor(source: string) {
        const parser = new YamlCstParser();
        this.cstTokens = [...parser.parse(source)];
    }

    parse(): Omit<Yaml.Documents, "sourcePath"> {
        const documents: Yaml.Document[] = [];
        let pendingPrefix = "";

        for (let i = 0; i < this.cstTokens.length; i++) {
            const token = this.cstTokens[i];

            if (token.type === 'document') {
                const cstDoc = token as CstDocument;
                // Check if next token is doc-end
                const nextToken = this.cstTokens[i + 1];
                let docEnd: CstDocumentEnd | undefined;
                if (nextToken?.type === 'doc-end') {
                    docEnd = nextToken as CstDocumentEnd;
                    i++; // Skip the doc-end in the main loop
                }
                const {doc, afterEnd} = this.convertDocument(cstDoc, docEnd, pendingPrefix);
                documents.push(doc);
                pendingPrefix = afterEnd;  // Content after ... becomes next doc's prefix
            } else if (token.type === 'doc-end') {
                // Standalone doc-end without preceding document
                const docEnd = token as CstDocumentEnd;
                pendingPrefix += this.concatenateSources(docEnd.end || []);
            } else if (token.type === 'comment' || token.type === 'newline' || token.type === 'space') {
                // Content before document (comments, whitespace) becomes document prefix
                pendingPrefix += (token as CstSourceToken).source;
            }
        }

        return {
            kind: Yaml.Kind.Documents,
            id: randomId(),
            prefix: "",
            markers: emptyMarkers,
            documents,
            suffix: undefined
        };
    }

    private convertDocument(cstDoc: CstDocument, cstDocEnd?: CstDocumentEnd, pendingPrefix: string = ""): {doc: Yaml.Document, afterEnd: string} {
        // Extract prefix from document start tokens
        // The document prefix is content BEFORE the --- marker
        // Content AFTER --- goes into the block's first entry
        const startTokens = cstDoc.start || [];
        let prefix = pendingPrefix;  // Content before this document
        let explicit = false;
        let afterDocStart = "";      // Content after ---
        let seenDocStart = false;

        for (const token of startTokens) {
            if (token.type === 'doc-start') {
                explicit = true;
                seenDocStart = true;
            } else if (seenDocStart) {
                // Content after --- goes into block's prefix
                afterDocStart += token.source;
            } else {
                // Content before --- goes into document prefix
                prefix += token.source;
            }
        }

        // Convert the document body and get trailing content
        let block: Yaml.Block;
        let trailing = "";
        if (cstDoc.value) {
            const result = this.convertTokenWithTrailing(cstDoc.value);
            block = result.node as Yaml.Block;
            trailing = result.trailing;

            // Prepend the whitespace after --- to the block
            if (afterDocStart) {
                block = this.prependWhitespaceToValue(block, afterDocStart);
            }
        } else {
            block = this.createEmptyScalar(afterDocStart);
        }

        // Convert document end, passing the trailing content from block
        const {end, afterEnd} = this.convertDocumentEnd(cstDoc.end, cstDocEnd, trailing);

        return {
            doc: {
                kind: Yaml.Kind.Document,
                id: randomId(),
                prefix,
                markers: emptyMarkers,
                explicit,
                block,
                end
            },
            afterEnd
        };
    }

    private convertDocumentEnd(docEndTokens?: CstSourceToken[], cstDocEnd?: CstDocumentEnd, trailing: string = ""): {end: Yaml.DocumentEnd, afterEnd: string} {
        // Prefix is the trailing content from the block, plus any content before ...
        let prefix = trailing;
        let explicit = false;
        let afterEnd = "";

        // Content after the document body (before ... if present)
        if (docEndTokens) {
            prefix += this.concatenateSources(docEndTokens);
        }

        // Explicit document end marker (...)
        if (cstDocEnd) {
            explicit = true;
            // cstDocEnd.end contains tokens AFTER the ... marker - these become next doc's prefix
            if (cstDocEnd.end) {
                afterEnd = this.concatenateSources(cstDocEnd.end);
            }
        }

        return {
            end: {
                kind: Yaml.Kind.DocumentEnd,
                id: randomId(),
                prefix,
                markers: emptyMarkers,
                explicit
            },
            afterEnd
        };
    }

    private convertTokenWithTrailing(token: CstToken): {node: Yaml, trailing: string} {
        switch (token.type) {
            case 'block-map':
                return this.convertBlockMapWithTrailing(token as CstBlockMap);
            case 'block-seq':
                return this.convertBlockSequenceWithTrailing(token as CstBlockSequence);
            case 'flow-collection':
                return this.convertFlowCollectionWithTrailing(token as CstFlowCollection);
            case 'scalar':
            case 'single-quoted-scalar':
            case 'double-quoted-scalar':
                return this.convertFlowScalarWithTrailing(token as CstFlowScalar);
            case 'block-scalar':
                return this.convertBlockScalarWithTrailing(token as CstBlockScalar);
            case 'alias':
                return this.convertAliasWithTrailing(token as CstFlowScalar);
            default:
                // For unknown types, create an empty scalar
                return {node: this.createEmptyScalar(), trailing: ""};
        }
    }

    private convertBlockMapWithTrailing(cst: CstBlockMap): {node: Yaml.Mapping, trailing: string} {
        const entries: Yaml.MappingEntry[] = [];
        let pendingPrefix = "";

        for (const item of cst.items) {
            if (item.key !== undefined || item.value !== undefined) {
                const entry = this.convertMappingEntry(item, pendingPrefix);
                entries.push(entry.entry);
                pendingPrefix = entry.trailingContent;
            } else {
                // Entry with no key or value - capture start tokens (comments, whitespace)
                // into pending prefix for the next entry or as trailing content
                pendingPrefix += this.concatenateSources(item.start || []);
            }
        }

        return {
            node: {
                kind: Yaml.Kind.Mapping,
                id: randomId(),
                prefix: "",
                markers: emptyMarkers,
                openingBracePrefix: undefined, // Block map has no braces
                entries,
                closingBracePrefix: undefined,
                anchor: undefined,
                tag: undefined
            },
            trailing: pendingPrefix  // Trailing content from last entry
        };
    }

    private convertMappingEntry(item: CstCollectionItem, pendingPrefix: string = ""): {entry: Yaml.MappingEntry, trailingContent: string} {
        // Prefix comes from start tokens plus any pending prefix from previous entry's trailing content
        const prefix = pendingPrefix + this.concatenateSources(item.start || []);

        // Convert key and get its trailing content
        let keyTrailing = "";
        let key: Yaml.YamlKey;
        if (item.key) {
            const keyResult = this.convertTokenWithTrailing(item.key);
            key = keyResult.node as Yaml.YamlKey;
            keyTrailing = keyResult.trailing;
        } else {
            key = this.createEmptyScalar();
        }

        // Extract whitespace before ':' and any anchor/tag after it
        let beforeMappingValueIndicator = keyTrailing;
        let anchorForValue: Yaml.Anchor | undefined;
        let tagForValue: Yaml.Tag | undefined;
        let afterColonWhitespace = "";
        let seenColon = false;

        if (item.sep) {
            const sepTokens = item.sep;
            for (let i = 0; i < sepTokens.length; i++) {
                const sepToken = sepTokens[i];
                if (sepToken.type === 'map-value-ind') {
                    seenColon = true;
                } else if (!seenColon) {
                    // Before the ':'
                    beforeMappingValueIndicator += sepToken.source;
                } else if (sepToken.type === 'anchor') {
                    // Collect all remaining tokens after anchor as postfix
                    let postfix = "";
                    for (let j = i + 1; j < sepTokens.length; j++) {
                        const nextToken = sepTokens[j];
                        if (nextToken.type === 'tag') {
                            tagForValue = this.parseTagTokenWithSuffix(nextToken.source, postfix, sepTokens, j + 1);
                            postfix = "";
                            break; // Tag handler consumed remaining tokens
                        } else {
                            postfix += nextToken.source;
                        }
                    }
                    anchorForValue = {
                        kind: Yaml.Kind.Anchor,
                        id: randomId(),
                        prefix: afterColonWhitespace,
                        markers: emptyMarkers,
                        postfix,
                        key: sepToken.source.substring(1) // Remove &
                    };
                    afterColonWhitespace = "";
                    break; // We've consumed all remaining tokens
                } else if (sepToken.type === 'tag') {
                    // Parse tag with its suffix (whitespace after the tag)
                    tagForValue = this.parseTagTokenWithSuffix(sepToken.source, afterColonWhitespace, sepTokens, i + 1);
                    afterColonWhitespace = ""; // Remaining whitespace is in tag suffix, not value prefix
                    break; // Tag handler consumed remaining tokens
                } else {
                    // After the ':'
                    afterColonWhitespace += sepToken.source;
                }
            }
        }

        // Convert value
        let value: Yaml.Block;
        let valueTrailing = "";
        if (item.value) {
            const valueResult = this.convertTokenWithTrailing(item.value);
            value = valueResult.node as Yaml.Block;
            valueTrailing = valueResult.trailing;

            // Apply anchor and tag if found in separator
            if (anchorForValue && 'anchor' in value) {
                value = {...value, anchor: anchorForValue} as Yaml.Block;
            }
            if (tagForValue && 'tag' in value) {
                value = {...value, tag: tagForValue} as Yaml.Block;
            }

            // Prepend accumulated whitespace based on value type
            if (afterColonWhitespace) {
                value = this.prependWhitespaceToValue(value, afterColonWhitespace);
            }
        } else {
            value = this.createEmptyScalar(afterColonWhitespace);
        }

        return {
            entry: {
                kind: Yaml.Kind.MappingEntry,
                id: randomId(),
                prefix,
                markers: emptyMarkers,
                key,
                beforeMappingValueIndicator,
                value
            },
            trailingContent: valueTrailing
        };
    }

    private convertBlockSequenceWithTrailing(cst: CstBlockSequence): {node: Yaml.Sequence, trailing: string} {
        const entries: Yaml.SequenceEntry[] = [];
        let pendingPrefix = "";

        for (const item of cst.items) {
            const result = this.convertSequenceEntry(item, true, pendingPrefix);
            entries.push(result.entry);
            pendingPrefix = result.trailingContent;
        }

        return {
            node: {
                kind: Yaml.Kind.Sequence,
                id: randomId(),
                prefix: "",
                markers: emptyMarkers,
                openingBracketPrefix: undefined, // Block sequence has no brackets
                entries,
                closingBracketPrefix: undefined,
                anchor: undefined,
                tag: undefined
            },
            trailing: pendingPrefix  // Trailing content from last entry
        };
    }

    private convertSequenceEntry(item: CstCollectionItem, dash: boolean, pendingPrefix: string = ""): {entry: Yaml.SequenceEntry, trailingContent: string} {
        // Build prefix from start tokens, but exclude the dash indicator itself
        let prefix = pendingPrefix;
        let afterDashSpace = "";
        let seenDash = false;

        for (const token of item.start || []) {
            if (token.type === 'seq-item-ind') {
                seenDash = true;
            } else if (seenDash) {
                afterDashSpace += token.source;
            } else {
                prefix += token.source;
            }
        }

        // Convert value
        let block: Yaml.Block;
        let trailing = "";
        if (item.value) {
            const valueResult = this.convertTokenWithTrailing(item.value);
            block = valueResult.node as Yaml.Block;
            trailing = valueResult.trailing;
            // Prepend the space after dash to the block's prefix
            block = {...block, prefix: afterDashSpace + block.prefix} as Yaml.Block;
        } else {
            block = this.createEmptyScalar(afterDashSpace);
        }

        return {
            entry: {
                kind: Yaml.Kind.SequenceEntry,
                id: randomId(),
                prefix,
                markers: emptyMarkers,
                block,
                dash,
                trailingCommaPrefix: undefined // Block sequence has no commas
            },
            trailingContent: trailing
        };
    }

    private convertFlowCollectionWithTrailing(cst: CstFlowCollection): {node: Yaml.Mapping | Yaml.Sequence, trailing: string} {
        const isMap = cst.start.source === '{';
        const openingPrefix = "";

        // End tokens include closing bracket/brace plus any trailing content
        // We need to separate the closing delimiter from trailing whitespace
        const endTokens = cst.end || [];
        let closingPrefix = "";
        let trailing = "";
        let seenClosing = false;

        for (const token of endTokens) {
            if (token.type === 'flow-map-end' || token.type === 'flow-seq-end') {
                seenClosing = true;
            } else if (seenClosing) {
                trailing += token.source;
            } else {
                closingPrefix += token.source;
            }
        }

        if (isMap) {
            const entries: Yaml.MappingEntry[] = [];
            let pendingPrefix = "";

            for (const item of cst.items) {
                if (item.key !== undefined || item.value !== undefined) {
                    const result = this.convertFlowMappingEntry(item, pendingPrefix);
                    entries.push(result.entry);
                    pendingPrefix = result.trailingContent;
                } else {
                    // Empty item (trailing comma) - capture start tokens including the comma
                    pendingPrefix += this.concatenateSources(item.start || []);
                }
            }

            // Trailing content from last entry becomes part of closing brace prefix
            const finalClosingPrefix = pendingPrefix + closingPrefix;

            return {
                node: {
                    kind: Yaml.Kind.Mapping,
                    id: randomId(),
                    prefix: "",
                    markers: emptyMarkers,
                    openingBracePrefix: openingPrefix,
                    entries,
                    closingBracePrefix: finalClosingPrefix,
                    anchor: undefined,
                    tag: undefined
                },
                trailing
            };
        } else {
            // Flow sequence [a, b, c]
            const entries: Yaml.SequenceEntry[] = [];
            let pendingPrefix = "";

            for (let i = 0; i < cst.items.length; i++) {
                const item = cst.items[i];
                const isLast = i === cst.items.length - 1;
                const result = this.convertFlowSequenceEntry(item, isLast, pendingPrefix);
                entries.push(result.entry);
                pendingPrefix = result.trailingContent;
            }

            // Trailing content from last entry becomes part of closing bracket prefix
            const finalClosingPrefix = pendingPrefix + closingPrefix;

            return {
                node: {
                    kind: Yaml.Kind.Sequence,
                    id: randomId(),
                    prefix: "",
                    markers: emptyMarkers,
                    openingBracketPrefix: openingPrefix,
                    entries,
                    closingBracketPrefix: finalClosingPrefix,
                    anchor: undefined,
                    tag: undefined
                },
                trailing
            };
        }
    }

    private convertFlowMappingEntry(item: CstCollectionItem, pendingPrefix: string): {entry: Yaml.MappingEntry, trailingContent: string} {
        const prefix = pendingPrefix + this.concatenateSources(item.start || []);

        let keyTrailing = "";
        let key: Yaml.YamlKey;
        if (item.key) {
            const keyResult = this.convertTokenWithTrailing(item.key);
            key = keyResult.node as Yaml.YamlKey;
            keyTrailing = keyResult.trailing;
        } else {
            key = this.createEmptyScalar();
        }

        let beforeMappingValueIndicator = keyTrailing;
        let afterColonSpace = "";
        let seenColon = false;

        if (item.sep) {
            for (const token of item.sep) {
                if (token.type === 'map-value-ind') {
                    seenColon = true;
                } else if (seenColon) {
                    afterColonSpace += token.source;
                } else if (token.type === 'comma') {
                    // This is the comma after the entry, not before
                    // Skip it as we handle comma separately
                } else {
                    beforeMappingValueIndicator += token.source;
                }
            }
        }

        let value: Yaml.Block;
        let valueTrailing = "";
        if (item.value) {
            const valueResult = this.convertTokenWithTrailing(item.value);
            value = valueResult.node as Yaml.Block;
            valueTrailing = valueResult.trailing;
            value = {...value, prefix: afterColonSpace + value.prefix} as Yaml.Block;
        } else {
            value = this.createEmptyScalar(afterColonSpace);
        }

        return {
            entry: {
                kind: Yaml.Kind.MappingEntry,
                id: randomId(),
                prefix,
                markers: emptyMarkers,
                key,
                beforeMappingValueIndicator,
                value
            },
            trailingContent: valueTrailing
        };
    }

    private convertFlowSequenceEntry(item: CstCollectionItem, isLast: boolean, pendingPrefix: string): {entry: Yaml.SequenceEntry, trailingContent: string} {
        // Start tokens may include comma from previous item
        let prefix = pendingPrefix;
        let hasComma = false;

        for (const token of item.start || []) {
            if (token.type === 'comma') {
                hasComma = true;
            } else {
                prefix += token.source;
            }
        }

        let block: Yaml.Block;
        let trailing = "";

        if (item.value) {
            const valueResult = this.convertTokenWithTrailing(item.value);
            block = valueResult.node as Yaml.Block;
            trailing = valueResult.trailing;
        } else if (item.key !== undefined) {
            // Flow sequence can contain mapping entries: [a: 1, b: 2]
            const entryResult = this.convertFlowMappingEntry(item, "");
            block = {
                kind: Yaml.Kind.Mapping,
                id: randomId(),
                prefix: "",
                markers: emptyMarkers,
                openingBracePrefix: undefined,
                entries: [entryResult.entry],
                closingBracePrefix: undefined,
                anchor: undefined,
                tag: undefined
            };
            trailing = entryResult.trailingContent;
        } else {
            block = this.createEmptyScalar();
        }

        // Trailing comma prefix captures space after value before comma
        let trailingCommaPrefix: string | undefined;
        if (!isLast) {
            trailingCommaPrefix = trailing;
            trailing = "";
        }

        return {
            entry: {
                kind: Yaml.Kind.SequenceEntry,
                id: randomId(),
                prefix,
                markers: emptyMarkers,
                block,
                dash: false, // Flow sequences don't use dashes
                trailingCommaPrefix
            },
            trailingContent: trailing
        };
    }

    private convertFlowScalarWithTrailing(cst: CstFlowScalar): {node: Yaml.Scalar, trailing: string} {
        const resolved = CST.resolveAsScalar(cst);
        const value = resolved?.value ?? "";

        // Determine scalar style
        let style: Yaml.ScalarStyle;
        switch (cst.type) {
            case 'single-quoted-scalar':
                style = Yaml.ScalarStyle.SINGLE_QUOTED;
                break;
            case 'double-quoted-scalar':
                style = Yaml.ScalarStyle.DOUBLE_QUOTED;
                break;
            default:
                style = Yaml.ScalarStyle.PLAIN;
        }

        // End tokens contain trailing whitespace/newlines
        const trailing = this.concatenateSources(cst.end || []);

        return {
            node: {
                kind: Yaml.Kind.Scalar,
                id: randomId(),
                prefix: "",
                markers: emptyMarkers,
                style,
                anchor: undefined,
                tag: undefined,
                value
            },
            trailing
        };
    }

    private convertBlockScalarWithTrailing(cst: CstBlockScalar): {node: Yaml.Scalar, trailing: string} {
        // Use CST.stringify to get the exact original source including header
        const fullSource = CST.stringify(cst);

        // Determine style from the first character (| or >)
        const headerChar = fullSource.charAt(0);
        const style = headerChar === '|' ? Yaml.ScalarStyle.LITERAL : Yaml.ScalarStyle.FOLDED;

        // The value is everything after the header indicator (| or >)
        const value = fullSource.substring(1);

        // Extract anchor and tag from props if present
        let anchor: Yaml.Anchor | undefined;
        let tag: Yaml.Tag | undefined;
        for (const prop of cst.props || []) {
            if ('type' in prop) {
                const propTyped = prop as CstSourceToken;
                if (propTyped.type === 'anchor') {
                    anchor = this.parseAnchorToken(propTyped.source, "");
                } else if (propTyped.type === 'tag') {
                    tag = this.parseTagToken(propTyped.source, "");
                }
            }
        }

        return {
            node: {
                kind: Yaml.Kind.Scalar,
                id: randomId(),
                prefix: "",
                markers: emptyMarkers,
                style,
                anchor,
                tag,
                value
            },
            trailing: "" // Block scalars don't have separate end tokens
        };
    }

    private convertAliasWithTrailing(cst: CstFlowScalar): {node: Yaml.Alias, trailing: string} {
        // Alias source is like "*anchorName"
        const key = cst.source.substring(1); // Remove the *

        const anchor: Yaml.Anchor = {
            kind: Yaml.Kind.Anchor,
            id: randomId(),
            prefix: "",
            markers: emptyMarkers,
            postfix: "",
            key
        };

        // End tokens contain trailing whitespace
        const trailing = this.concatenateSources(cst.end || []);

        return {
            node: {
                kind: Yaml.Kind.Alias,
                id: randomId(),
                prefix: "",
                markers: emptyMarkers,
                anchor
            },
            trailing
        };
    }

    private parseAnchorToken(source: string, prefix: string): Yaml.Anchor {
        // Anchor source is like "&anchorName"
        const key = source.substring(1); // Remove the &

        return {
            kind: Yaml.Kind.Anchor,
            id: randomId(),
            prefix,
            markers: emptyMarkers,
            postfix: "",
            key
        };
    }

    private parseTagToken(source: string, prefix: string): Yaml.Tag {
        return this.parseTagTokenWithSuffix(source, prefix, [], 0);
    }

    private parseTagTokenWithSuffix(source: string, prefix: string, remainingTokens: CstSourceToken[], startIndex: number): Yaml.Tag {
        let name: string;
        let tagKind: Yaml.TagKind;

        if (source.startsWith('!<') && source.endsWith('>')) {
            // Explicit global tag: !<tag:yaml.org,2002:str>
            name = source.substring(2, source.length - 1);
            tagKind = Yaml.TagKind.EXPLICIT_GLOBAL;
        } else if (source.startsWith('!!')) {
            // Implicit global tag: !!str
            name = source.substring(2);
            tagKind = Yaml.TagKind.IMPLICIT_GLOBAL;
        } else {
            // Local tag: !custom
            name = source.substring(1);
            tagKind = Yaml.TagKind.LOCAL;
        }

        // Collect remaining tokens as suffix (whitespace after the tag)
        let suffix = "";
        for (let i = startIndex; i < remainingTokens.length; i++) {
            suffix += remainingTokens[i].source;
        }

        return {
            kind: Yaml.Kind.Tag,
            id: randomId(),
            prefix,
            markers: emptyMarkers,
            name,
            suffix,
            tagKind
        };
    }

    private createEmptyScalar(prefix: string = ""): Yaml.Scalar {
        return {
            kind: Yaml.Kind.Scalar,
            id: randomId(),
            prefix,
            markers: emptyMarkers,
            style: Yaml.ScalarStyle.PLAIN,
            anchor: undefined,
            tag: undefined,
            value: ""
        };
    }

    /**
     * Prepends whitespace to a value node in the appropriate location based on its type.
     * - For Scalars and Aliases: prepend to prefix
     * - For flow Mappings (with braces): prepend to openingBracePrefix
     * - For flow Sequences (with brackets): prepend to openingBracketPrefix
     * - For block Mappings/Sequences: prepend to first entry's prefix
     */
    private prependWhitespaceToValue(value: Yaml.Block, whitespace: string): Yaml.Block {
        if (value.kind === Yaml.Kind.Scalar || value.kind === Yaml.Kind.Alias) {
            return {...value, prefix: whitespace + value.prefix};
        }

        if (value.kind === Yaml.Kind.Mapping) {
            const mapping = value as Yaml.Mapping;
            if (mapping.openingBracePrefix !== undefined) {
                // Flow mapping: prepend to opening brace prefix
                return {...mapping, openingBracePrefix: whitespace + mapping.openingBracePrefix};
            } else if (mapping.entries.length > 0) {
                // Block mapping: prepend to first entry's prefix
                const firstEntry = mapping.entries[0];
                const updatedFirstEntry = {...firstEntry, prefix: whitespace + firstEntry.prefix};
                return {...mapping, entries: [updatedFirstEntry, ...mapping.entries.slice(1)]};
            }
        }

        if (value.kind === Yaml.Kind.Sequence) {
            const sequence = value as Yaml.Sequence;
            if (sequence.openingBracketPrefix !== undefined) {
                // Flow sequence: prepend to opening bracket prefix
                return {...sequence, openingBracketPrefix: whitespace + sequence.openingBracketPrefix};
            } else if (sequence.entries.length > 0) {
                // Block sequence: prepend to first entry's prefix
                const firstEntry = sequence.entries[0];
                const updatedFirstEntry = {...firstEntry, prefix: whitespace + firstEntry.prefix};
                return {...sequence, entries: [updatedFirstEntry, ...sequence.entries.slice(1)]};
            }
        }

        // Fallback: prepend to prefix (shouldn't normally reach here)
        return {...value, prefix: whitespace + value.prefix} as Yaml.Block;
    }

    private concatenateSources(tokens: CstSourceToken[]): string {
        return tokens.map(t => t.source).join('');
    }
}

Parsers.registerParser("yaml", YamlParser);
