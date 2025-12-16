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
import {produceAsync} from "../visitor";
import {createDraft, finishDraft} from "immer";

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
        await q.getAndSend(document, d => d.explicit);
        await q.getAndSend(document, d => d.block, async b => await this.visit(b, q));
        await q.getAndSend(document, d => d.end, async e => await this.visit(e, q));
        return document;
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
        const draft = createDraft(y);
        draft.id = await q.receive(y.id);
        draft.prefix = await q.receive(y.prefix);
        draft.markers = await q.receive(y.markers);
        return finishDraft(draft);
    }

    protected async visitDocuments(documents: Yaml.Documents, q: RpcReceiveQueue): Promise<Yaml | undefined> {
        const draft = createDraft(documents);
        draft.sourcePath = await q.receive(documents.sourcePath);
        draft.charsetName = await q.receive(documents.charsetName);
        draft.charsetBomMarked = await q.receive(documents.charsetBomMarked);
        draft.checksum = await q.receive(documents.checksum);
        draft.fileAttributes = await q.receive(documents.fileAttributes);
        draft.documents = await q.receiveListDefined(documents.documents,
            async doc => await this.visit(doc, q) as Yaml.Document);
        draft.suffix = await q.receive(documents.suffix);
        return finishDraft(draft);
    }

    protected async visitDocument(document: Yaml.Document, q: RpcReceiveQueue): Promise<Yaml | undefined> {
        const draft = createDraft(document);
        draft.explicit = await q.receive(document.explicit);
        draft.block = await q.receive(document.block,
            async b => await this.visit(b, q) as Yaml.Block);
        draft.end = await q.receive(document.end,
            async e => await this.visit(e, q) as Yaml.DocumentEnd);
        return finishDraft(draft);
    }

    protected async visitDocumentEnd(end: Yaml.DocumentEnd, q: RpcReceiveQueue): Promise<Yaml | undefined> {
        const draft = createDraft(end);
        draft.explicit = await q.receive(end.explicit);
        return finishDraft(draft);
    }

    protected async visitMapping(mapping: Yaml.Mapping, q: RpcReceiveQueue): Promise<Yaml | undefined> {
        const draft = createDraft(mapping);
        draft.openingBracePrefix = await q.receive(mapping.openingBracePrefix);
        draft.entries = await q.receiveListDefined(mapping.entries,
            async e => await this.visit(e, q) as Yaml.MappingEntry);
        draft.closingBracePrefix = await q.receive(mapping.closingBracePrefix);
        draft.anchor = await q.receive(mapping.anchor,
            async a => a ? await this.visit(a, q) as Yaml.Anchor : undefined);
        draft.tag = await q.receive(mapping.tag,
            async t => t ? await this.visit(t, q) as Yaml.Tag : undefined);
        return finishDraft(draft);
    }

    protected async visitMappingEntry(entry: Yaml.MappingEntry, q: RpcReceiveQueue): Promise<Yaml | undefined> {
        const draft = createDraft(entry);
        draft.key = await q.receive(entry.key,
            async k => await this.visit(k, q) as Yaml.YamlKey);
        draft.beforeMappingValueIndicator = await q.receive(entry.beforeMappingValueIndicator);
        draft.value = await q.receive(entry.value,
            async v => await this.visit(v, q) as Yaml.Block);
        return finishDraft(draft);
    }

    protected async visitScalar(scalar: Yaml.Scalar, q: RpcReceiveQueue): Promise<Yaml | undefined> {
        const draft = createDraft(scalar);
        draft.style = await q.receive(scalar.style);
        draft.anchor = await q.receive(scalar.anchor,
            async a => a ? await this.visit(a, q) as Yaml.Anchor : undefined);
        draft.tag = await q.receive(scalar.tag,
            async t => t ? await this.visit(t, q) as Yaml.Tag : undefined);
        draft.value = await q.receive(scalar.value);
        return finishDraft(draft);
    }

    protected async visitSequence(sequence: Yaml.Sequence, q: RpcReceiveQueue): Promise<Yaml | undefined> {
        const draft = createDraft(sequence);
        draft.openingBracketPrefix = await q.receive(sequence.openingBracketPrefix);
        draft.entries = await q.receiveListDefined(sequence.entries,
            async e => await this.visit(e, q) as Yaml.SequenceEntry);
        draft.closingBracketPrefix = await q.receive(sequence.closingBracketPrefix);
        draft.anchor = await q.receive(sequence.anchor,
            async a => a ? await this.visit(a, q) as Yaml.Anchor : undefined);
        draft.tag = await q.receive(sequence.tag,
            async t => t ? await this.visit(t, q) as Yaml.Tag : undefined);
        return finishDraft(draft);
    }

    protected async visitSequenceEntry(entry: Yaml.SequenceEntry, q: RpcReceiveQueue): Promise<Yaml | undefined> {
        const draft = createDraft(entry);
        draft.block = await q.receive(entry.block,
            async b => await this.visit(b, q) as Yaml.Block);
        draft.dash = await q.receive(entry.dash);
        draft.trailingCommaPrefix = await q.receive(entry.trailingCommaPrefix);
        return finishDraft(draft);
    }

    protected async visitAnchor(anchor: Yaml.Anchor, q: RpcReceiveQueue): Promise<Yaml | undefined> {
        const draft = createDraft(anchor);
        draft.postfix = await q.receive(anchor.postfix);
        draft.key = await q.receive(anchor.key);
        return finishDraft(draft);
    }

    protected async visitAlias(alias: Yaml.Alias, q: RpcReceiveQueue): Promise<Yaml | undefined> {
        const draft = createDraft(alias);
        draft.anchor = await q.receive(alias.anchor,
            async a => await this.visit(a, q) as Yaml.Anchor);
        return finishDraft(draft);
    }

    protected async visitTag(tag: Yaml.Tag, q: RpcReceiveQueue): Promise<Yaml | undefined> {
        const draft = createDraft(tag);
        draft.name = await q.receive(tag.name);
        draft.suffix = await q.receive(tag.suffix);
        draft.tagKind = await q.receive(tag.tagKind);
        return finishDraft(draft);
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
