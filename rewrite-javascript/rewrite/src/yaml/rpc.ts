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

    protected async preVisit(y: Yaml, q: RpcSendQueue): Promise<Yaml | undefined> {
        await q.getAndSend(y, y2 => y2.id);
        await q.getAndSend(y, y2 => y2.prefix);
        await q.getAndSend(y, y2 => y2.markers);
        return y;
    }

    protected async visitDocuments(documents: Yaml.Documents, q: RpcSendQueue): Promise<Yaml | undefined> {
        await q.getAndSend(documents, d => d.sourcePath);
        await q.getAndSend(documents, d => d.charsetName);
        await q.getAndSend(documents, d => d.charsetBomMarked);
        await q.getAndSend(documents, d => d.checksum);
        await q.getAndSend(documents, d => d.fileAttributes);
        await q.getAndSendList(documents, d => d.documents, doc => doc.id,
            async doc => await this.visit(doc, q));
        await q.getAndSend(documents, d => d.suffix);
        return documents;
    }

    protected async visitDocument(document: Yaml.Document, q: RpcSendQueue): Promise<Yaml | undefined> {
        await q.getAndSendList(document, d => d.directives, dir => dir.id,
            async dir => await this.visit(dir, q));
        await q.getAndSend(document, d => d.explicit);
        await q.getAndSend(document, d => d.block, async b => await this.visit(b, q));
        await q.getAndSend(document, d => d.end, async e => await this.visit(e, q));
        return document;
    }

    protected async visitDirective(directive: Yaml.Directive, q: RpcSendQueue): Promise<Yaml | undefined> {
        await q.getAndSend(directive, d => d.value);
        await q.getAndSend(directive, d => d.suffix);
        return directive;
    }

    protected async visitDocumentEnd(end: Yaml.DocumentEnd, q: RpcSendQueue): Promise<Yaml | undefined> {
        await q.getAndSend(end, e => e.explicit);
        return end;
    }

    protected async visitMapping(mapping: Yaml.Mapping, q: RpcSendQueue): Promise<Yaml | undefined> {
        await q.getAndSend(mapping, m => m.openingBracePrefix);
        await q.getAndSendList(mapping, m => m.entries, e => e.id,
            async e => await this.visit(e, q));
        await q.getAndSend(mapping, m => m.closingBracePrefix);
        await q.getAndSend(mapping, m => m.anchor, async a => a ? await this.visit(a, q) : undefined);
        await q.getAndSend(mapping, m => m.tag, async t => t ? await this.visit(t, q) : undefined);
        return mapping;
    }

    protected async visitMappingEntry(entry: Yaml.MappingEntry, q: RpcSendQueue): Promise<Yaml | undefined> {
        await q.getAndSend(entry, e => e.key, async k => await this.visit(k, q));
        await q.getAndSend(entry, e => e.beforeMappingValueIndicator);
        await q.getAndSend(entry, e => e.value, async v => await this.visit(v, q));
        return entry;
    }

    protected async visitScalar(scalar: Yaml.Scalar, q: RpcSendQueue): Promise<Yaml | undefined> {
        await q.getAndSend(scalar, s => s.style);
        await q.getAndSend(scalar, s => s.anchor, async a => a ? await this.visit(a, q) : undefined);
        await q.getAndSend(scalar, s => s.tag, async t => t ? await this.visit(t, q) : undefined);
        await q.getAndSend(scalar, s => s.value);
        return scalar;
    }

    protected async visitSequence(sequence: Yaml.Sequence, q: RpcSendQueue): Promise<Yaml | undefined> {
        await q.getAndSend(sequence, s => s.openingBracketPrefix);
        await q.getAndSendList(sequence, s => s.entries, e => e.id,
            async e => await this.visit(e, q));
        await q.getAndSend(sequence, s => s.closingBracketPrefix);
        await q.getAndSend(sequence, s => s.anchor, async a => a ? await this.visit(a, q) : undefined);
        await q.getAndSend(sequence, s => s.tag, async t => t ? await this.visit(t, q) : undefined);
        return sequence;
    }

    protected async visitSequenceEntry(entry: Yaml.SequenceEntry, q: RpcSendQueue): Promise<Yaml | undefined> {
        await q.getAndSend(entry, e => e.block, async b => await this.visit(b, q));
        await q.getAndSend(entry, e => e.dash);
        await q.getAndSend(entry, e => e.trailingCommaPrefix);
        return entry;
    }

    protected async visitAnchor(anchor: Yaml.Anchor, q: RpcSendQueue): Promise<Yaml | undefined> {
        await q.getAndSend(anchor, a => a.postfix);
        await q.getAndSend(anchor, a => a.key);
        return anchor;
    }

    protected async visitAlias(alias: Yaml.Alias, q: RpcSendQueue): Promise<Yaml | undefined> {
        await q.getAndSend(alias, a => a.anchor, async a => await this.visit(a, q));
        return alias;
    }

    protected async visitTag(tag: Yaml.Tag, q: RpcSendQueue): Promise<Yaml | undefined> {
        await q.getAndSend(tag, t => t.name);
        await q.getAndSend(tag, t => t.suffix);
        await q.getAndSend(tag, t => t.tagKind);
        return tag;
    }
}

class YamlReceiver extends YamlVisitor<RpcReceiveQueue> {

    protected async preVisit(y: Yaml, q: RpcReceiveQueue): Promise<Yaml | undefined> {
        return updateIfChanged(y, {
            id: await q.receive(y.id),
            prefix: await q.receive(y.prefix),
            markers: await q.receive(y.markers),
        });
    }

    protected async visitDocuments(documents: Yaml.Documents, q: RpcReceiveQueue): Promise<Yaml | undefined> {
        return updateIfChanged(documents, {
            sourcePath: await q.receive(documents.sourcePath),
            charsetName: await q.receive(documents.charsetName),
            charsetBomMarked: await q.receive(documents.charsetBomMarked),
            checksum: await q.receive(documents.checksum),
            fileAttributes: await q.receive(documents.fileAttributes),
            documents: await q.receiveListDefined(documents.documents,
                async doc => await this.visit(doc, q) as Yaml.Document),
            suffix: await q.receive(documents.suffix),
        });
    }

    protected async visitDocument(document: Yaml.Document, q: RpcReceiveQueue): Promise<Yaml | undefined> {
        return updateIfChanged(document, {
            directives: await q.receiveListDefined(document.directives,
                async dir => await this.visit(dir, q) as Yaml.Directive),
            explicit: await q.receive(document.explicit),
            block: await q.receive(document.block,
                async b => await this.visit(b, q) as Yaml.Block),
            end: await q.receive(document.end,
                async e => await this.visit(e, q) as Yaml.DocumentEnd),
        });
    }

    protected async visitDirective(directive: Yaml.Directive, q: RpcReceiveQueue): Promise<Yaml | undefined> {
        return updateIfChanged(directive, {
            value: await q.receive(directive.value),
            suffix: await q.receive(directive.suffix),
        });
    }

    protected async visitDocumentEnd(end: Yaml.DocumentEnd, q: RpcReceiveQueue): Promise<Yaml | undefined> {
        return updateIfChanged(end, {
            explicit: await q.receive(end.explicit),
        });
    }

    protected async visitMapping(mapping: Yaml.Mapping, q: RpcReceiveQueue): Promise<Yaml | undefined> {
        return updateIfChanged(mapping, {
            openingBracePrefix: await q.receive(mapping.openingBracePrefix),
            entries: await q.receiveListDefined(mapping.entries,
                async e => await this.visit(e, q) as Yaml.MappingEntry),
            closingBracePrefix: await q.receive(mapping.closingBracePrefix),
            anchor: await q.receive(mapping.anchor,
                async a => a ? await this.visit(a, q) as Yaml.Anchor : undefined),
            tag: await q.receive(mapping.tag,
                async t => t ? await this.visit(t, q) as Yaml.Tag : undefined),
        });
    }

    protected async visitMappingEntry(entry: Yaml.MappingEntry, q: RpcReceiveQueue): Promise<Yaml | undefined> {
        return updateIfChanged(entry, {
            key: await q.receive(entry.key,
                async k => await this.visit(k, q) as Yaml.YamlKey),
            beforeMappingValueIndicator: await q.receive(entry.beforeMappingValueIndicator),
            value: await q.receive(entry.value,
                async v => await this.visit(v, q) as Yaml.Block),
        });
    }

    protected async visitScalar(scalar: Yaml.Scalar, q: RpcReceiveQueue): Promise<Yaml | undefined> {
        return updateIfChanged(scalar, {
            style: await q.receive(scalar.style),
            anchor: await q.receive(scalar.anchor,
                async a => a ? await this.visit(a, q) as Yaml.Anchor : undefined),
            tag: await q.receive(scalar.tag,
                async t => t ? await this.visit(t, q) as Yaml.Tag : undefined),
            value: await q.receive(scalar.value),
        });
    }

    protected async visitSequence(sequence: Yaml.Sequence, q: RpcReceiveQueue): Promise<Yaml | undefined> {
        return updateIfChanged(sequence, {
            openingBracketPrefix: await q.receive(sequence.openingBracketPrefix),
            entries: await q.receiveListDefined(sequence.entries,
                async e => await this.visit(e, q) as Yaml.SequenceEntry),
            closingBracketPrefix: await q.receive(sequence.closingBracketPrefix),
            anchor: await q.receive(sequence.anchor,
                async a => a ? await this.visit(a, q) as Yaml.Anchor : undefined),
            tag: await q.receive(sequence.tag,
                async t => t ? await this.visit(t, q) as Yaml.Tag : undefined),
        });
    }

    protected async visitSequenceEntry(entry: Yaml.SequenceEntry, q: RpcReceiveQueue): Promise<Yaml | undefined> {
        return updateIfChanged(entry, {
            block: await q.receive(entry.block,
                async b => await this.visit(b, q) as Yaml.Block),
            dash: await q.receive(entry.dash),
            trailingCommaPrefix: await q.receive(entry.trailingCommaPrefix),
        });
    }

    protected async visitAnchor(anchor: Yaml.Anchor, q: RpcReceiveQueue): Promise<Yaml | undefined> {
        return updateIfChanged(anchor, {
            postfix: await q.receive(anchor.postfix),
            key: await q.receive(anchor.key),
        });
    }

    protected async visitAlias(alias: Yaml.Alias, q: RpcReceiveQueue): Promise<Yaml | undefined> {
        return updateIfChanged(alias, {
            anchor: await q.receive(alias.anchor,
                async a => await this.visit(a, q) as Yaml.Anchor),
        });
    }

    protected async visitTag(tag: Yaml.Tag, q: RpcReceiveQueue): Promise<Yaml | undefined> {
        return updateIfChanged(tag, {
            name: await q.receive(tag.name),
            suffix: await q.receive(tag.suffix),
            tagKind: await q.receive(tag.tagKind),
        });
    }
}

const receiver = new YamlReceiver();
const sender = new YamlSender();

// Register codec for all YAML AST node types
for (const kind of Object.values(Yaml.Kind)) {
    RpcCodecs.registerCodec(kind as string, {
        async rpcReceive(before: Yaml, q: RpcReceiveQueue): Promise<Yaml> {
            return (await receiver.visit(before, q))!;
        },

        async rpcSend(after: Yaml, q: RpcSendQueue): Promise<void> {
            await sender.visit(after, q);
        }
    }, Yaml.Kind.Documents);
}
