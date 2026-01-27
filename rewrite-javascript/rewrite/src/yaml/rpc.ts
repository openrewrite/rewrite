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
import {YamlVisitor} from "./visitor";
import {RpcCodecs, RpcReceiveQueue, RpcSendQueue} from "../rpc";
import {Yaml} from "./tree";
import {updateIfChanged} from "../util";

class YamlSender extends YamlVisitor<RpcSendQueue> {

    protected preVisit(y: Yaml, q: RpcSendQueue): Yaml | undefined {
        q.getAndSend(y, y2 => y2.id);
        q.getAndSend(y, y2 => y2.prefix);
        q.getAndSend(y, y2 => y2.markers);
        return y;
    }

    protected visitDocuments(documents: Yaml.Documents, q: RpcSendQueue): Yaml | undefined {
        q.getAndSend(documents, d => d.sourcePath);
        q.getAndSend(documents, d => d.charsetName);
        q.getAndSend(documents, d => d.charsetBomMarked);
        q.getAndSend(documents, d => d.checksum);
        q.getAndSend(documents, d => d.fileAttributes);
        q.getAndSendList(documents, d => d.documents, doc => doc.id,
            doc => this.visit(doc, q));
        q.getAndSend(documents, d => d.suffix);
        return documents;
    }

    protected visitDocument(document: Yaml.Document, q: RpcSendQueue): Yaml | undefined {
        q.getAndSendList(document, d => d.directives, dir => dir.id,
            dir => this.visit(dir, q));
        q.getAndSend(document, d => d.explicit);
        q.getAndSend(document, d => d.block, b => this.visit(b, q));
        q.getAndSend(document, d => d.end, e => this.visit(e, q));
        return document;
    }

    protected visitDirective(directive: Yaml.Directive, q: RpcSendQueue): Yaml | undefined {
        q.getAndSend(directive, d => d.value);
        q.getAndSend(directive, d => d.suffix);
        return directive;
    }

    protected visitDocumentEnd(end: Yaml.DocumentEnd, q: RpcSendQueue): Yaml | undefined {
        q.getAndSend(end, e => e.explicit);
        return end;
    }

    protected visitMapping(mapping: Yaml.Mapping, q: RpcSendQueue): Yaml | undefined {
        q.getAndSend(mapping, m => m.openingBracePrefix);
        q.getAndSendList(mapping, m => m.entries, e => e.id,
            e => this.visit(e, q));
        q.getAndSend(mapping, m => m.closingBracePrefix);
        q.getAndSend(mapping, m => m.anchor, a => a ? this.visit(a, q) : undefined);
        q.getAndSend(mapping, m => m.tag, t => t ? this.visit(t, q) : undefined);
        return mapping;
    }

    protected visitMappingEntry(entry: Yaml.MappingEntry, q: RpcSendQueue): Yaml | undefined {
        q.getAndSend(entry, e => e.key, k => this.visit(k, q));
        q.getAndSend(entry, e => e.beforeMappingValueIndicator);
        q.getAndSend(entry, e => e.value, v => this.visit(v, q));
        return entry;
    }

    protected visitScalar(scalar: Yaml.Scalar, q: RpcSendQueue): Yaml | undefined {
        q.getAndSend(scalar, s => s.style);
        q.getAndSend(scalar, s => s.anchor, a => a ? this.visit(a, q) : undefined);
        q.getAndSend(scalar, s => s.tag, t => t ? this.visit(t, q) : undefined);
        q.getAndSend(scalar, s => s.value);
        return scalar;
    }

    protected visitSequence(sequence: Yaml.Sequence, q: RpcSendQueue): Yaml | undefined {
        q.getAndSend(sequence, s => s.openingBracketPrefix);
        q.getAndSendList(sequence, s => s.entries, e => e.id,
            e => this.visit(e, q));
        q.getAndSend(sequence, s => s.closingBracketPrefix);
        q.getAndSend(sequence, s => s.anchor, a => a ? this.visit(a, q) : undefined);
        q.getAndSend(sequence, s => s.tag, t => t ? this.visit(t, q) : undefined);
        return sequence;
    }

    protected visitSequenceEntry(entry: Yaml.SequenceEntry, q: RpcSendQueue): Yaml | undefined {
        q.getAndSend(entry, e => e.block, b => this.visit(b, q));
        q.getAndSend(entry, e => e.dash);
        q.getAndSend(entry, e => e.trailingCommaPrefix);
        return entry;
    }

    protected visitAnchor(anchor: Yaml.Anchor, q: RpcSendQueue): Yaml | undefined {
        q.getAndSend(anchor, a => a.postfix);
        q.getAndSend(anchor, a => a.key);
        return anchor;
    }

    protected visitAlias(alias: Yaml.Alias, q: RpcSendQueue): Yaml | undefined {
        q.getAndSend(alias, a => a.anchor, a => this.visit(a, q));
        return alias;
    }

    protected visitTag(tag: Yaml.Tag, q: RpcSendQueue): Yaml | undefined {
        q.getAndSend(tag, t => t.name);
        q.getAndSend(tag, t => t.suffix);
        q.getAndSend(tag, t => t.tagKind);
        return tag;
    }
}

class YamlReceiver {

    public visit<T extends Yaml>(y: T | undefined, q: RpcReceiveQueue): T | undefined {
        if (!y) return undefined;

        let result: Yaml | undefined = this.preVisit(y, q);
        if (result === undefined) return undefined;

        switch (y.kind) {
            case Yaml.Kind.Documents:
                result = this.visitDocuments(result as Yaml.Documents, q);
                break;
            case Yaml.Kind.Document:
                result = this.visitDocument(result as Yaml.Document, q);
                break;
            case Yaml.Kind.Directive:
                result = this.visitDirective(result as Yaml.Directive, q);
                break;
            case Yaml.Kind.DocumentEnd:
                result = this.visitDocumentEnd(result as Yaml.DocumentEnd, q);
                break;
            case Yaml.Kind.Mapping:
                result = this.visitMapping(result as Yaml.Mapping, q);
                break;
            case Yaml.Kind.MappingEntry:
                result = this.visitMappingEntry(result as Yaml.MappingEntry, q);
                break;
            case Yaml.Kind.Scalar:
                result = this.visitScalar(result as Yaml.Scalar, q);
                break;
            case Yaml.Kind.Sequence:
                result = this.visitSequence(result as Yaml.Sequence, q);
                break;
            case Yaml.Kind.SequenceEntry:
                result = this.visitSequenceEntry(result as Yaml.SequenceEntry, q);
                break;
            case Yaml.Kind.Anchor:
                result = this.visitAnchor(result as Yaml.Anchor, q);
                break;
            case Yaml.Kind.Alias:
                result = this.visitAlias(result as Yaml.Alias, q);
                break;
            case Yaml.Kind.Tag:
                result = this.visitTag(result as Yaml.Tag, q);
                break;
        }

        return result as T | undefined;
    }

    preVisit(y: Yaml, q: RpcReceiveQueue): Yaml | undefined {
        return updateIfChanged(y, {
            id: q.receive(y.id),
            prefix: q.receive(y.prefix),
            markers: q.receive(y.markers),
        });
    }

    protected visitDocuments(documents: Yaml.Documents, q: RpcReceiveQueue): Yaml | undefined {
        return updateIfChanged(documents, {
            sourcePath: q.receive(documents.sourcePath),
            charsetName: q.receive(documents.charsetName),
            charsetBomMarked: q.receive(documents.charsetBomMarked),
            checksum: q.receive(documents.checksum),
            fileAttributes: q.receive(documents.fileAttributes),
            documents: q.receiveListDefined(documents.documents,
                doc => this.visit(doc, q) as Yaml.Document),
            suffix: q.receive(documents.suffix),
        });
    }

    protected visitDocument(document: Yaml.Document, q: RpcReceiveQueue): Yaml | undefined {
        return updateIfChanged(document, {
            directives: q.receiveListDefined(document.directives,
                dir => this.visit(dir, q) as Yaml.Directive),
            explicit: q.receive(document.explicit),
            block: q.receive(document.block,
                b => this.visit(b, q) as Yaml.Block),
            end: q.receive(document.end,
                e => this.visit(e, q) as Yaml.DocumentEnd),
        });
    }

    protected visitDirective(directive: Yaml.Directive, q: RpcReceiveQueue): Yaml | undefined {
        return updateIfChanged(directive, {
            value: q.receive(directive.value),
            suffix: q.receive(directive.suffix),
        });
    }

    protected visitDocumentEnd(end: Yaml.DocumentEnd, q: RpcReceiveQueue): Yaml | undefined {
        return updateIfChanged(end, {
            explicit: q.receive(end.explicit),
        });
    }

    protected visitMapping(mapping: Yaml.Mapping, q: RpcReceiveQueue): Yaml | undefined {
        return updateIfChanged(mapping, {
            openingBracePrefix: q.receive(mapping.openingBracePrefix),
            entries: q.receiveListDefined(mapping.entries,
                e => this.visit(e, q) as Yaml.MappingEntry),
            closingBracePrefix: q.receive(mapping.closingBracePrefix),
            anchor: q.receive(mapping.anchor,
                a => a ? this.visit(a, q) as Yaml.Anchor : undefined),
            tag: q.receive(mapping.tag,
                t => t ? this.visit(t, q) as Yaml.Tag : undefined),
        });
    }

    protected visitMappingEntry(entry: Yaml.MappingEntry, q: RpcReceiveQueue): Yaml | undefined {
        return updateIfChanged(entry, {
            key: q.receive(entry.key,
                k => this.visit(k, q) as Yaml.YamlKey),
            beforeMappingValueIndicator: q.receive(entry.beforeMappingValueIndicator),
            value: q.receive(entry.value,
                v => this.visit(v, q) as Yaml.Block),
        });
    }

    protected visitScalar(scalar: Yaml.Scalar, q: RpcReceiveQueue): Yaml | undefined {
        return updateIfChanged(scalar, {
            style: q.receive(scalar.style),
            anchor: q.receive(scalar.anchor,
                a => a ? this.visit(a, q) as Yaml.Anchor : undefined),
            tag: q.receive(scalar.tag,
                t => t ? this.visit(t, q) as Yaml.Tag : undefined),
            value: q.receive(scalar.value),
        });
    }

    protected visitSequence(sequence: Yaml.Sequence, q: RpcReceiveQueue): Yaml | undefined {
        return updateIfChanged(sequence, {
            openingBracketPrefix: q.receive(sequence.openingBracketPrefix),
            entries: q.receiveListDefined(sequence.entries,
                e => this.visit(e, q) as Yaml.SequenceEntry),
            closingBracketPrefix: q.receive(sequence.closingBracketPrefix),
            anchor: q.receive(sequence.anchor,
                a => a ? this.visit(a, q) as Yaml.Anchor : undefined),
            tag: q.receive(sequence.tag,
                t => t ? this.visit(t, q) as Yaml.Tag : undefined),
        });
    }

    protected visitSequenceEntry(entry: Yaml.SequenceEntry, q: RpcReceiveQueue): Yaml | undefined {
        return updateIfChanged(entry, {
            block: q.receive(entry.block,
                b => this.visit(b, q) as Yaml.Block),
            dash: q.receive(entry.dash),
            trailingCommaPrefix: q.receive(entry.trailingCommaPrefix),
        });
    }

    protected visitAnchor(anchor: Yaml.Anchor, q: RpcReceiveQueue): Yaml | undefined {
        return updateIfChanged(anchor, {
            postfix: q.receive(anchor.postfix),
            key: q.receive(anchor.key),
        });
    }

    protected visitAlias(alias: Yaml.Alias, q: RpcReceiveQueue): Yaml | undefined {
        return updateIfChanged(alias, {
            anchor: q.receive(alias.anchor,
                a => this.visit(a, q) as Yaml.Anchor),
        });
    }

    protected visitTag(tag: Yaml.Tag, q: RpcReceiveQueue): Yaml | undefined {
        return updateIfChanged(tag, {
            name: q.receive(tag.name),
            suffix: q.receive(tag.suffix),
            tagKind: q.receive(tag.tagKind),
        });
    }
}

const receiver = new YamlReceiver();
const sender = new YamlSender();

// Register codec for all YAML AST node types
for (const kind of Object.values(Yaml.Kind)) {
    RpcCodecs.registerCodec(kind as string, {
        rpcReceive(before: Yaml, q: RpcReceiveQueue): Yaml {
            return receiver.visit(before, q)!;
        },

        rpcSend(after: Yaml, q: RpcSendQueue): void {
            sender.visit(after, q);
        }
    }, Yaml.Kind.Documents);
}
