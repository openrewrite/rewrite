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
import {mapAsync, updateIfChanged} from "../util";
import {TreeVisitor} from "../visitor";
import {SourceFile} from "../tree";
import {isYaml, Yaml} from "./tree";

export class YamlVisitor<P> extends TreeVisitor<Yaml, P> {
    async isAcceptable(sourceFile: SourceFile): Promise<boolean> {
        return isYaml(sourceFile);
    }

    protected async visitDocuments(documents: Yaml.Documents, p: P): Promise<Yaml | undefined> {
        const updates: any = {
            markers: await this.visitMarkers(documents.markers, p),
            documents: await mapAsync(documents.documents, doc => this.visitDefined(doc, p) as Promise<Yaml.Document>)
        };
        return updateIfChanged(documents, updates);
    }

    protected async visitDocument(document: Yaml.Document, p: P): Promise<Yaml | undefined> {
        const updates: any = {
            markers: await this.visitMarkers(document.markers, p),
            block: await this.visitDefined(document.block, p),
            end: await this.visitDefined(document.end, p)
        };
        return updateIfChanged(document, updates);
    }

    protected async visitDocumentEnd(end: Yaml.DocumentEnd, p: P): Promise<Yaml | undefined> {
        const updates: any = {
            markers: await this.visitMarkers(end.markers, p)
        };
        return updateIfChanged(end, updates);
    }

    protected async visitMapping(mapping: Yaml.Mapping, p: P): Promise<Yaml | undefined> {
        const updates: any = {
            markers: await this.visitMarkers(mapping.markers, p),
            tag: mapping.tag ? await this.visit(mapping.tag, p) : undefined,
            anchor: mapping.anchor ? await this.visit(mapping.anchor, p) : undefined,
            entries: await mapAsync(mapping.entries, entry => this.visitDefined(entry, p) as Promise<Yaml.MappingEntry>)
        };
        return updateIfChanged(mapping, updates);
    }

    protected async visitMappingEntry(entry: Yaml.MappingEntry, p: P): Promise<Yaml | undefined> {
        const updates: any = {
            markers: await this.visitMarkers(entry.markers, p),
            key: await this.visitDefined(entry.key, p),
            value: await this.visitDefined(entry.value, p)
        };
        return updateIfChanged(entry, updates);
    }

    protected async visitScalar(scalar: Yaml.Scalar, p: P): Promise<Yaml | undefined> {
        const updates: any = {
            markers: await this.visitMarkers(scalar.markers, p),
            anchor: scalar.anchor ? await this.visit(scalar.anchor, p) : undefined,
            tag: scalar.tag ? await this.visit(scalar.tag, p) : undefined
        };
        return updateIfChanged(scalar, updates);
    }

    protected async visitSequence(sequence: Yaml.Sequence, p: P): Promise<Yaml | undefined> {
        const updates: any = {
            markers: await this.visitMarkers(sequence.markers, p),
            tag: sequence.tag ? await this.visit(sequence.tag, p) : undefined,
            anchor: sequence.anchor ? await this.visit(sequence.anchor, p) : undefined,
            entries: await mapAsync(sequence.entries, entry => this.visitDefined(entry, p) as Promise<Yaml.SequenceEntry>)
        };
        return updateIfChanged(sequence, updates);
    }

    protected async visitSequenceEntry(entry: Yaml.SequenceEntry, p: P): Promise<Yaml | undefined> {
        const updates: any = {
            markers: await this.visitMarkers(entry.markers, p),
            block: await this.visitDefined(entry.block, p)
        };
        return updateIfChanged(entry, updates);
    }

    protected async visitAnchor(anchor: Yaml.Anchor, p: P): Promise<Yaml | undefined> {
        const updates: any = {
            markers: await this.visitMarkers(anchor.markers, p)
        };
        return updateIfChanged(anchor, updates);
    }

    protected async visitAlias(alias: Yaml.Alias, p: P): Promise<Yaml | undefined> {
        const updates: any = {
            markers: await this.visitMarkers(alias.markers, p),
            anchor: await this.visitDefined(alias.anchor, p)
        };
        return updateIfChanged(alias, updates);
    }

    protected async visitTag(tag: Yaml.Tag, p: P): Promise<Yaml | undefined> {
        const updates: any = {
            markers: await this.visitMarkers(tag.markers, p)
        };
        return updateIfChanged(tag, updates);
    }

    protected accept(t: Yaml, p: P): Promise<Yaml | undefined> {
        switch (t.kind) {
            case Yaml.Kind.Documents:
                return this.visitDocuments(t as Yaml.Documents, p);
            case Yaml.Kind.Document:
                return this.visitDocument(t as Yaml.Document, p);
            case Yaml.Kind.DocumentEnd:
                return this.visitDocumentEnd(t as Yaml.DocumentEnd, p);
            case Yaml.Kind.Mapping:
                return this.visitMapping(t as Yaml.Mapping, p);
            case Yaml.Kind.MappingEntry:
                return this.visitMappingEntry(t as Yaml.MappingEntry, p);
            case Yaml.Kind.Scalar:
                return this.visitScalar(t as Yaml.Scalar, p);
            case Yaml.Kind.Sequence:
                return this.visitSequence(t as Yaml.Sequence, p);
            case Yaml.Kind.SequenceEntry:
                return this.visitSequenceEntry(t as Yaml.SequenceEntry, p);
            case Yaml.Kind.Anchor:
                return this.visitAnchor(t as Yaml.Anchor, p);
            case Yaml.Kind.Alias:
                return this.visitAlias(t as Yaml.Alias, p);
            case Yaml.Kind.Tag:
                return this.visitTag(t as Yaml.Tag, p);
            default:
                throw new Error(`Unexpected YAML kind ${(t as any).kind}`);
        }
    }
}
