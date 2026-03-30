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
import {PrintOutputCapture, TreePrinters} from "../print";
import {YamlVisitor} from "./visitor";
import {printTag, Yaml} from "./tree";
import {Cursor} from "../tree";
import {findMarker, Markers} from "../markers";
import "./markers"; // Ensures Yaml.Markers is defined

class YamlPrinter extends YamlVisitor<PrintOutputCapture> {

    protected async visitDocuments(documents: Yaml.Documents, p: PrintOutputCapture): Promise<Yaml | undefined> {
        await this.visitMarkers(documents.markers, p);
        for (const document of documents.documents) {
            await this.visit(document, p);
        }
        if (documents.suffix) {
            p.append(documents.suffix);
        }
        this.afterSyntax(documents, p);
        return documents;
    }

    protected async visitDocument(document: Yaml.Document, p: PrintOutputCapture): Promise<Yaml | undefined> {
        await this.beforeSyntax(document, p);
        for (const directive of document.directives) {
            await this.visit(directive, p);
        }
        if (document.explicit) {
            p.append("---");
        }
        await this.visit(document.block, p);
        await this.visit(document.end, p);
        this.afterSyntax(document, p);
        return document;
    }

    protected async visitDirective(directive: Yaml.Directive, p: PrintOutputCapture): Promise<Yaml | undefined> {
        await this.beforeSyntax(directive, p);
        p.append('%');
        p.append(directive.value);
        p.append(directive.suffix);
        this.afterSyntax(directive, p);
        return directive;
    }

    protected async visitDocumentEnd(end: Yaml.DocumentEnd, p: PrintOutputCapture): Promise<Yaml | undefined> {
        await this.beforeSyntax(end, p);
        if (end.explicit) {
            p.append("...");
        }
        return end;
    }

    protected async visitMapping(mapping: Yaml.Mapping, p: PrintOutputCapture): Promise<Yaml | undefined> {
        await this.beforeSyntax(mapping, p);
        if (mapping.anchor) {
            await this.visit(mapping.anchor, p);
        }
        if (mapping.openingBracePrefix !== undefined) {
            p.append(mapping.openingBracePrefix);
            p.append('{');
        }
        for (const entry of mapping.entries) {
            await this.visit(entry, p);
        }
        if (mapping.closingBracePrefix !== undefined) {
            p.append(mapping.closingBracePrefix);
            p.append('}');
        }
        this.afterSyntax(mapping, p);
        return mapping;
    }

    protected async visitMappingEntry(entry: Yaml.MappingEntry, p: PrintOutputCapture): Promise<Yaml | undefined> {
        await this.beforeSyntax(entry, p);
        await this.visit(entry.key, p);
        p.append(entry.beforeMappingValueIndicator);
        if (!findMarker(entry, Yaml.Markers.OmitColon)) {
            p.append(':');
        }
        await this.visit(entry.value, p);
        this.afterSyntax(entry, p);
        return entry;
    }

    protected async visitScalar(scalar: Yaml.Scalar, p: PrintOutputCapture): Promise<Yaml | undefined> {
        await this.beforeSyntax(scalar, p);
        if (scalar.tag) {
            await this.visit(scalar.tag, p);
        }
        if (scalar.anchor) {
            await this.visit(scalar.anchor, p);
        }
        switch (scalar.style) {
            case Yaml.ScalarStyle.DOUBLE_QUOTED:
                p.append('"');
                p.append(scalar.value);
                p.append('"');
                break;
            case Yaml.ScalarStyle.SINGLE_QUOTED:
                p.append("'");
                p.append(scalar.value);
                p.append("'");
                break;
            case Yaml.ScalarStyle.LITERAL:
                p.append('|');
                p.append(scalar.value);
                break;
            case Yaml.ScalarStyle.FOLDED:
                p.append('>');
                p.append(scalar.value);
                break;
            case Yaml.ScalarStyle.PLAIN:
            default:
                p.append(scalar.value);
                break;
        }
        this.afterSyntax(scalar, p);
        return scalar;
    }

    protected async visitSequence(sequence: Yaml.Sequence, p: PrintOutputCapture): Promise<Yaml | undefined> {
        await this.beforeSyntax(sequence, p);
        if (sequence.anchor) {
            await this.visit(sequence.anchor, p);
        }
        if (sequence.openingBracketPrefix !== undefined) {
            p.append(sequence.openingBracketPrefix);
            p.append('[');
        }
        for (const entry of sequence.entries) {
            await this.visit(entry, p);
        }
        if (sequence.closingBracketPrefix !== undefined) {
            p.append(sequence.closingBracketPrefix);
            p.append(']');
        }
        this.afterSyntax(sequence, p);
        return sequence;
    }

    protected async visitSequenceEntry(entry: Yaml.SequenceEntry, p: PrintOutputCapture): Promise<Yaml | undefined> {
        p.append(entry.prefix);
        if (entry.dash) {
            p.append('-');
        }
        await this.visit(entry.block, p);
        if (entry.trailingCommaPrefix !== undefined) {
            p.append(entry.trailingCommaPrefix);
            p.append(',');
        }
        this.afterSyntax(entry, p);
        return entry;
    }

    protected async visitAnchor(anchor: Yaml.Anchor, p: PrintOutputCapture): Promise<Yaml | undefined> {
        await this.visitMarkers(anchor.markers, p);
        p.append(anchor.prefix);
        p.append('&');
        p.append(anchor.key);
        p.append(anchor.postfix);
        this.afterSyntax(anchor, p);
        return anchor;
    }

    protected async visitAlias(alias: Yaml.Alias, p: PrintOutputCapture): Promise<Yaml | undefined> {
        await this.beforeSyntax(alias, p);
        p.append('*');
        if (alias.anchor) {
            p.append(alias.anchor.key);
        }
        this.afterSyntax(alias, p);
        return alias;
    }

    protected async visitTag(tag: Yaml.Tag, p: PrintOutputCapture): Promise<Yaml | undefined> {
        await this.visitMarkers(tag.markers, p);
        p.append(tag.prefix);
        p.append(printTag(tag));
        p.append(tag.suffix);
        this.afterSyntax(tag, p);
        return tag;
    }

    private yamlMarkerWrapper = (out: string): string => `~~${out}${out ? "~~" : ""}>`;

    private async beforeSyntax(yaml: Yaml, p: PrintOutputCapture): Promise<void> {
        await this.beforeSyntaxWithMarkers(yaml.prefix, yaml.markers, p);
    }

    private async beforeSyntaxWithMarkers(prefix: string, markers: Markers, p: PrintOutputCapture): Promise<void> {
        for (const marker of markers.markers) {
            p.append(p.markerPrinter.beforePrefix(marker, new Cursor(marker, this.cursor), this.yamlMarkerWrapper));
        }
        p.append(prefix);
        await this.visitMarkers(markers, p);
        for (const marker of markers.markers) {
            p.append(p.markerPrinter.beforeSyntax(marker, new Cursor(marker, this.cursor), this.yamlMarkerWrapper));
        }
    }

    private afterSyntax(yaml: Yaml, p: PrintOutputCapture): void {
        this.afterSyntaxWithMarkers(yaml.markers, p);
    }

    private afterSyntaxWithMarkers(markers: Markers, p: PrintOutputCapture): void {
        for (const marker of markers.markers) {
            p.append(p.markerPrinter.afterSyntax(marker, new Cursor(marker, this.cursor), this.yamlMarkerWrapper));
        }
    }
}

TreePrinters.register(Yaml.Kind.Documents, () => new YamlPrinter());
