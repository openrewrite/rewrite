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
import {omitColon, tagBeforeAnchor} from "./markers";
import {Parser, ParserInput, parserInputRead, Parsers} from "../parser";
import {randomId} from "../uuid";
import {SourceFile} from "../tree";
import {Yaml, isYamlKey} from "./tree";
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

/**
 * The anchor and tag before a node, with the space before, between and after them.
 */
interface Properties {
    before: string;
    anchor?: Yaml.Anchor;
    tag?: Yaml.Tag;
    tagFirst: boolean;
    between: string;
    after: string;
    source: string;
}

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
    private readonly anchors = new Map<string, Yaml.Anchor>();

    constructor(source: string) {
        const parser = new YamlCstParser();
        this.cstTokens = [...parser.parse(source)];
    }

    parse(): Omit<Yaml.Documents, "sourcePath"> {
        const documents: Yaml.Document[] = [];
        let pendingPrefix = "";
        let pendingDirectives: Yaml.Directive[] = [];

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
                const {doc, afterEnd} = this.convertDocument(cstDoc, docEnd, pendingPrefix, pendingDirectives);
                documents.push(doc);
                pendingPrefix = afterEnd;  // Content after ... becomes next doc's prefix
                pendingDirectives = [];    // Reset directives for next document
            } else if (token.type === 'doc-end') {
                // Standalone doc-end without preceding document
                const docEnd = token as CstDocumentEnd;
                pendingPrefix += this.concatenateSources(docEnd.end || []);
            } else if (token.type === 'directive') {
                // Directive token (e.g., %YAML 1.2 or %TAG !yaml! tag:yaml.org,2002:)
                // Look ahead for trailing newline/whitespace to use as suffix
                let suffix = "";
                while (i + 1 < this.cstTokens.length) {
                    const nextToken = this.cstTokens[i + 1];
                    if (nextToken.type === 'newline' || nextToken.type === 'space') {
                        suffix += (nextToken as CstSourceToken).source;
                        i++;
                    } else {
                        break;
                    }
                }
                pendingDirectives.push(this.convertDirective((token as any).source as string, pendingPrefix, suffix));
                pendingPrefix = "";  // Prefix consumed by directive
            } else if (token.type === 'comment' || token.type === 'newline' || token.type === 'space') {
                // Content before document (comments, whitespace) becomes document prefix or directive prefix
                pendingPrefix += (token as CstSourceToken).source;
            }
        }

        return {
            kind: Yaml.Kind.Documents,
            id: randomId(),
            prefix: "",
            markers: emptyMarkers,
            documents,
            suffix: pendingPrefix || undefined
        };
    }

    private convertDirective(source: string, prefix: string, suffix: string): Yaml.Directive {
        // Directive source is like "%YAML 1.2" (without trailing newline in CST)
        // The suffix is provided from looking ahead at subsequent tokens
        // Remove the leading '%'
        const value = source.substring(1);

        return {
            kind: Yaml.Kind.Directive,
            id: randomId(),
            prefix,
            markers: emptyMarkers,
            value,
            suffix
        };
    }

    private convertDocument(cstDoc: CstDocument, cstDocEnd?: CstDocumentEnd, pendingPrefix: string = "", directives: Yaml.Directive[] = []): {doc: Yaml.Document, afterEnd: string} {
        // Extract prefix from document start tokens
        // The document prefix is content BEFORE the --- marker
        // Content AFTER --- goes into the block's first entry
        const startTokens = cstDoc.start || [];
        const docStart = startTokens.findIndex(token => token.type === 'doc-start');
        const explicit = docStart >= 0;
        let prefix = pendingPrefix + this.concatenateSources(startTokens.slice(0, Math.max(docStart, 0)));
        let afterDocStart = startTokens.slice(docStart + 1);

        const properties = this.readProperties(afterDocStart);
        if (!explicit) {
            // Without ---, what comes before the root and its properties is the document's
            const firstProperty = afterDocStart.findIndex(token => token.type === 'anchor' || token.type === 'tag');
            const before = properties ? afterDocStart.slice(0, firstProperty) : afterDocStart;
            prefix += this.concatenateSources(before);
            afterDocStart = afterDocStart.slice(before.length);
        }
        const {node: block, trailing} = this.convertValue(afterDocStart, cstDoc.value, "", "prefix");

        // Convert document end, passing the trailing content from block
        const {end, afterEnd} = this.convertDocumentEnd(cstDoc.end, cstDocEnd, trailing);

        return {
            doc: {
                kind: Yaml.Kind.Document,
                id: randomId(),
                prefix,
                markers: emptyMarkers,
                directives,
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
        const {prefix, key, keyTrailing} = this.convertKey(item, pendingPrefix);

        let beforeMappingValueIndicator = keyTrailing;
        const afterColon: CstSourceToken[] = [];
        let seenColon = false;
        for (const token of item.sep || []) {
            if (token.type === 'map-value-ind') {
                seenColon = true;
            } else if (seenColon) {
                afterColon.push(token);
            } else {
                beforeMappingValueIndicator += token.source;
            }
        }
        const {node: value, trailing: valueTrailing} = this.convertValue(afterColon, item.value, "", "trailing");

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

    /**
     * Converts the key of a mapping entry, with its anchor and tag. The space before them is the entry's prefix.
     */
    private convertKey(item: CstCollectionItem, pendingPrefix: string): {prefix: string, key: Yaml.YamlKey, keyTrailing: string} {
        const startTokens = item.start || [];
        const properties = item.key ? this.readProperties(startTokens) : undefined;
        const prefix = pendingPrefix + (properties ? properties.before : this.concatenateSources(startTokens));

        let keyTrailing = "";
        let key: Yaml.YamlKey;
        if (item.key) {
            const keyResult = this.convertTokenWithTrailing(item.key);
            if (isYamlKey(keyResult.node)) {
                key = keyResult.node;
            } else {
                // Complex keys (mappings or sequences used as keys) are valid YAML
                // but our tree model only supports Scalar | Alias as keys.
                // Degrade to a Scalar containing the source text.
                key = {
                    kind: Yaml.Kind.Scalar,
                    id: randomId(),
                    prefix: keyResult.node.prefix,
                    markers: emptyMarkers,
                    style: Yaml.ScalarStyle.PLAIN,
                    anchor: undefined,
                    tag: undefined,
                    value: CST.stringify(item.key).trim()
                };
            }
            keyTrailing = keyResult.trailing;
            if (properties) {
                key = this.withProperties(key, {...properties, before: ""}).node as Yaml.YamlKey;
            }
        } else {
            key = this.createEmptyScalar();
        }
        return {prefix, key, keyTrailing};
    }

    private convertBlockSequenceWithTrailing(cst: CstBlockSequence): {node: Yaml.Sequence, trailing: string} {
        const entries: Yaml.SequenceEntry[] = [];
        let pendingPrefix = "";

        for (const item of cst.items) {
            if (item.value !== undefined || (item.start || []).some(t => t.type === 'seq-item-ind')) {
                const result = this.convertSequenceEntry(item, true, pendingPrefix);
                entries.push(result.entry);
                pendingPrefix = result.trailingContent;
            } else {
                pendingPrefix += this.concatenateSources(item.start || []);
            }
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
        const startTokens = item.start || [];
        const dashIndex = startTokens.findIndex(token => token.type === 'seq-item-ind');
        const prefix = pendingPrefix + this.concatenateSources(dashIndex >= 0 ? startTokens.slice(0, dashIndex) : startTokens);
        const afterDash = dashIndex >= 0 ? startTokens.slice(dashIndex + 1) : [];
        const {node: block, trailing} = this.convertValue(afterDash, item.value, "", "trailing");

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
                let start = item.start || [];
                const comma = start.findIndex(token => token.type === 'comma');
                if (comma >= 0) {
                    if (comma > 0 && entries.length > 0) {
                        const previous = entries[entries.length - 1];
                        entries[entries.length - 1] = {
                            ...previous,
                            trailingCommaPrefix: (previous.trailingCommaPrefix ?? "") + this.concatenateSources(start.slice(0, comma))
                        };
                    }
                    start = start.slice(comma + 1);
                }
                if (isLast && item.key === undefined && !item.value &&
                    start.every(token => token.type === 'space' || token.type === 'newline' || token.type === 'comment')) {
                    pendingPrefix += this.concatenateSources(start);
                    break;
                }
                const result = this.convertFlowSequenceEntry({...item, start}, isLast, pendingPrefix);
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
        const {prefix, key, keyTrailing} = this.convertKey(item, pendingPrefix);

        let beforeMappingValueIndicator = keyTrailing;
        const afterColon: CstSourceToken[] = [];
        let seenColon = false;

        if (item.sep) {
            for (const token of item.sep) {
                if (token.type === 'map-value-ind') {
                    seenColon = true;
                } else if (seenColon) {
                    afterColon.push(token);
                } else if (token.type === 'comma') {
                    // This is the comma after the entry, not before
                    // Skip it as we handle comma separately
                } else {
                    beforeMappingValueIndicator += token.source;
                }
            }
        }

        const {node: value, trailing: valueTrailing} = this.convertValue(afterColon, item.value, "", "prefix");

        // In flow mappings like { "MV7", "7J04" }, entries may lack explicit colons
        const entryMarkers = seenColon ? emptyMarkers : markers(omitColon());

        return {
            entry: {
                kind: Yaml.Kind.MappingEntry,
                id: randomId(),
                prefix,
                markers: entryMarkers,
                key,
                beforeMappingValueIndicator,
                value
            },
            trailingContent: valueTrailing
        };
    }

    private convertFlowSequenceEntry(item: CstCollectionItem, isLast: boolean, pendingPrefix: string): {entry: Yaml.SequenceEntry, trailingContent: string} {
        const startTokens = item.start || [];
        let prefix = pendingPrefix;
        let block: Yaml.Block;
        let trailing = "";

        if (item.key !== undefined) {
            const entryResult = this.convertFlowMappingEntry({...item, start: startTokens}, "");
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
        } else if (item.value || startTokens.some(token => token.type === 'anchor' || token.type === 'tag')) {
            ({node: block, trailing} = this.convertValue(startTokens, item.value, prefix, "trailing"));
            prefix = "";
        } else {
            prefix += this.concatenateSources(startTokens);
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
        let style: Yaml.ScalarStyle;
        let value: string;
        switch (cst.type) {
            case 'single-quoted-scalar':
                style = Yaml.ScalarStyle.SINGLE_QUOTED;
                value = this.unwrapQuotes(cst.source);
                break;
            case 'double-quoted-scalar':
                style = Yaml.ScalarStyle.DOUBLE_QUOTED;
                value = this.unwrapQuotes(cst.source);
                break;
            default:
                style = Yaml.ScalarStyle.PLAIN;
                value = cst.source;
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

        // As in the Java parser, an alias holds the anchor it refers to
        const anchor: Yaml.Anchor = this.anchors.get(key) ?? {
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

        const anchor: Yaml.Anchor = {
            kind: Yaml.Kind.Anchor,
            id: randomId(),
            prefix,
            markers: emptyMarkers,
            postfix: "",
            key
        };
        this.anchors.set(key, anchor);
        return anchor;
    }

    private parseTagToken(source: string, prefix: string): Yaml.Tag {
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

        return {
            kind: Yaml.Kind.Tag,
            id: randomId(),
            prefix,
            markers: emptyMarkers,
            name,
            suffix: "",
            tagKind
        };
    }

    /**
     * Converts the value after an indicator such as `:` or `-`, given the tokens between them. Any anchor and tag
     * among the tokens go on the value. Without them, the space before an absent value goes in the empty scalar's
     * prefix or in the trailing content, as `emptySpace` says.
     */
    private convertValue(tokens: CstSourceToken[], token: CstToken | undefined, lead: string,
                         emptySpace: "prefix" | "trailing"): {node: Yaml.Block, trailing: string} {
        const result = token ? this.convertTokenWithTrailing(token) : undefined;
        const properties = this.readProperties(tokens);
        if (properties) {
            const placed = this.withProperties(result?.node as Yaml.Block | undefined, {...properties, before: lead + properties.before});
            return {node: placed.node, trailing: placed.trailing + (result?.trailing ?? "")};
        }
        const space = lead + this.concatenateSources(tokens);
        if (result) {
            const node = result.node as Yaml.Block;
            return {node: space ? this.prependWhitespaceToValue(node, space) : node, trailing: result.trailing};
        }
        return emptySpace === "prefix" ?
            {node: this.createEmptyScalar(space), trailing: ""} :
            {node: this.createEmptyScalar(), trailing: space};
    }

    /**
     * Reads the anchor and tag among the tokens before a node, or `undefined` if there are none.
     */
    private readProperties(tokens: CstSourceToken[]): Properties | undefined {
        let anchor: Yaml.Anchor | undefined;
        let tag: Yaml.Tag | undefined;
        let tagFirst = false;
        let before = "";
        let between = "";
        let after = "";
        for (const token of tokens) {
            if (token.type === 'anchor' || token.type === 'tag') {
                if (anchor === undefined && tag === undefined) {
                    tagFirst = token.type === 'tag';
                } else {
                    between = after;
                    after = "";
                }
                if (token.type === 'anchor') {
                    anchor = this.parseAnchorToken(token.source, "");
                } else {
                    tag = this.parseTagToken(token.source, "");
                }
            } else if (anchor === undefined && tag === undefined) {
                before += token.source;
            } else {
                after += token.source;
            }
        }
        return anchor || tag ? {before, anchor, tag, tagFirst, between, after, source: this.concatenateSources(tokens)} : undefined;
    }

    /**
     * Gives a node the anchor and tag before it, with the space around them where the Java parser puts it. An absent
     * node becomes an empty scalar, and the space after its properties is returned as trailing content.
     */
    private withProperties(node: Yaml.Block | undefined, properties: Properties): {node: Yaml.Block, trailing: string} {
        const {before, between, after, tagFirst} = properties;
        let anchor = properties.anchor;
        let tag = properties.tag;
        const nodeMarkers = anchor && tag && tagFirst ? markers(tagBeforeAnchor()) : emptyMarkers;

        if (node === undefined || node.kind === Yaml.Kind.Scalar) {
            // On a scalar, each property holds the space after it, unless the scalar is empty
            const empty = node === undefined || ((node as Yaml.Scalar).style === Yaml.ScalarStyle.PLAIN && (node as Yaml.Scalar).value === "");
            const last = empty ? "" : after;
            if (anchor && tag) {
                anchor = {...anchor, postfix: tagFirst ? last : between};
                tag = {...tag, suffix: tagFirst ? between : last};
            } else if (anchor) {
                anchor = {...anchor, postfix: last};
            } else if (tag) {
                tag = {...tag, suffix: last};
            }
            this.remember(anchor);
            const scalar = (node ?? this.createEmptyScalar()) as Yaml.Scalar;
            return {
                node: {...scalar, prefix: before + scalar.prefix, markers: nodeMarkers, anchor, tag},
                trailing: empty ? after : ""
            };
        }

        if (node.kind === Yaml.Kind.Alias) {
            // An alias can't have properties
            return {node: this.prependWhitespaceToValue(node, properties.source), trailing: ""};
        }

        // A collection has no prefix, so its first property holds the space before it
        let rest: string;
        if (anchor && tag && tagFirst) {
            tag = {...tag, prefix: before, suffix: between};
            anchor = {...anchor, prefix: "", postfix: ""};
            rest = after;
        } else {
            if (anchor) {
                anchor = {...anchor, prefix: before, postfix: ""};
            }
            if (tag) {
                // As the Java parser does, a tag holds the space after it up to the end of its line
                const lineBreak = after.indexOf("\n");
                tag = {...tag, prefix: anchor ? between : before, suffix: lineBreak >= 0 ? after.slice(0, lineBreak + 1) : after};
                rest = lineBreak >= 0 ? after.slice(lineBreak + 1) : "";
            } else {
                rest = after;
            }
        }
        this.remember(anchor);
        const collection = {...node, markers: nodeMarkers, anchor, tag} as Yaml.Mapping | Yaml.Sequence;
        return {node: rest ? this.prependWhitespaceToValue(collection, rest) : collection, trailing: ""};
    }

    private remember(anchor: Yaml.Anchor | undefined): void {
        if (anchor) {
            this.anchors.set(anchor.key, anchor);
        }
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

    private unwrapQuotes(source: string): string {
        return source.substring(1, source.length - 1);
    }
}

Parsers.registerParser("yaml", YamlParser);
