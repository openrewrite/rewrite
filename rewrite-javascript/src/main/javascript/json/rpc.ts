import {JsonVisitor} from "./visitor";
import {asRef, RpcCodec, RpcCodecs, RpcReceiveQueue, RpcSendQueue} from "../rpc";
import {
    Document,
    Empty,
    Identifier,
    Json,
    JsonArray,
    JsonKind,
    JsonObject,
    JsonRightPadded,
    JsonValue,
    Literal,
    Member,
    Space
} from "./tree";
import {produceAsync} from "../visitor";
import {Draft} from "immer";

class JsonSender extends JsonVisitor<RpcSendQueue> {

    protected async preVisit(j: Json, q: RpcSendQueue): Promise<Json | undefined> {
        q.getAndSend(j, j2 => j2.id);
        q.getAndSend(j, j2 => asRef(j2.prefix), space => this.visitSpace(space, q));
        q.sendMarkers(j, j2 => j2.markers);
        return j;
    }

    protected async visitDocument(document: Document, q: RpcSendQueue): Promise<Json | undefined> {
        q.getAndSend(document, d => d.sourcePath);
        q.getAndSend(document, d => d.charsetName);
        q.getAndSend(document, d => d.charsetBomMarked);
        q.getAndSend(document, d => d.checksum);
        q.getAndSend(document, d => d.fileAttributes);
        q.getAndSend(document, d => d.value, j => this.visit(j, q));
        q.getAndSend(document, d => asRef(d.eof));
        return document;
    }

    protected async visitArray(array: JsonArray, q: RpcSendQueue): Promise<Json | undefined> {
        q.getAndSendList(array, a => a.values, j => j.element.id, j => this.visitRightPadded(j, q));
        return array;
    }

    protected async visitEmpty(empty: Empty): Promise<Json | undefined> {
        return empty;
    }

    protected async visitIdentifier(identifier: Identifier, q: RpcSendQueue): Promise<Json | undefined> {
        q.getAndSend(identifier, id => id.name);
        return identifier;
    }

    protected async visitLiteral(literal: Literal, q: RpcSendQueue): Promise<Json | undefined> {
        q.getAndSend(literal, lit => lit.source);
        q.getAndSend(literal, lit => lit.value);
        return literal;
    }

    protected async visitMember(member: Member, q: RpcSendQueue): Promise<Json | undefined> {
        q.getAndSend(member, m => m.key, j => this.visitRightPadded(j, q));
        q.getAndSend(member, m => m.value, j => this.visit(j, q));
        return member;
    }

    protected async visitObject(obj: JsonObject, q: RpcSendQueue): Promise<Json | undefined> {
        q.getAndSendList(obj, o => o.members, j => j.element.id, j => this.visitRightPadded(j, q));
        return obj;
    }

    protected async visitSpace(space: Space, q: RpcSendQueue): Promise<Space> {
        q.getAndSendList(space, s => s.comments, c => c.text + c.suffix, c => {
            q.getAndSend(c, c2 => c2.multiline);
            q.getAndSend(c, c2 => c2.text);
            q.getAndSend(c, c2 => c2.suffix);
            q.sendMarkers(c, c2 => c2.markers);
        });
        q.getAndSend(space, s => s.whitespace);
        return space;
    }

    protected async visitRightPadded<T extends Json>(right: JsonRightPadded<T>, q: RpcSendQueue): Promise<JsonRightPadded<T> | undefined> {
        q.getAndSend(right, r => r.element, j => this.visit(j, q));
        q.getAndSend(right, r => asRef(r.after), space => this.visitSpace(space, q));
        q.sendMarkers(right, r => r.markers);
        return right;
    }
}

// noinspection ES6MissingAwait
class JsonReceiver extends JsonVisitor<RpcReceiveQueue> {

    protected async preVisit(j: Json, q: RpcReceiveQueue): Promise<Json | undefined> {
        return this.produceJson<Json>(j, q, async draft => {
            draft.id = await q.receive(j.id);
            draft.prefix = await q.receive(j.prefix, space => this.visitSpace(space, q));
            draft.markers = await q.receiveMarkers(j.markers);
        });
    }

    protected async visitDocument(document: Document, q: RpcReceiveQueue): Promise<Json | undefined> {
        return this.produceJson<Document>(document, q, async draft => {
            draft.sourcePath = await q.receiveAndGet(document.sourcePath, (path: string) => path);
            draft.charsetName = await q.receiveAndGet(document.charsetName, (name: string) => name);
            draft.charsetBomMarked = await q.receive(document.charsetBomMarked);
            draft.checksum = await q.receive(document.checksum);
            draft.fileAttributes = await q.receive(document.fileAttributes);
            draft.value = await q.receive<JsonValue>(document.value, j => this.visit(j, q)!);
            draft.eof = await q.receive(document.eof);
        });
    }

    protected async visitArray(array: JsonArray, q: RpcReceiveQueue): Promise<Json | undefined> {
        return this.produceJson<JsonArray>(array, q, async draft => {
            draft.values = await q.receiveListDefined(array.values, j => this.visitRightPadded(j, q)!)!;
        });
    }

    protected async visitEmpty(empty: Empty): Promise<Json | undefined> {
        return empty;
    }

    protected async visitIdentifier(identifier: Identifier, q: RpcReceiveQueue): Promise<Json | undefined> {
        return this.produceJson<Identifier>(identifier, q, async draft => {
            draft.name = await q.receive(identifier.name);
        });
    }

    protected async visitLiteral(literal: Literal, q: RpcReceiveQueue): Promise<Json | undefined> {
        return this.produceJson<Literal>(literal, q, async draft => {
            draft.source = await q.receive(literal.source);
            draft.value = await q.receive(literal.value);
        });
    }

    protected async visitMember(member: Member, q: RpcReceiveQueue): Promise<Json | undefined> {
        return this.produceJson<Member>(member, q, async draft => {
            draft.key = await q.receive(member.key, j => this.visitRightPadded(j, q)!)!;
            draft.value = await q.receive<JsonValue>(member.value, j => this.visit(j, q)!);
        });
    }

    protected async visitObject(obj: JsonObject, q: RpcReceiveQueue): Promise<Json | undefined> {
        return this.produceJson<JsonObject>(obj, q, async draft => {
            draft.members = await q.receiveListDefined(obj.members, j => this.visitRightPadded(j, q));
        });
    }

    protected async visitSpace(space: Space, q: RpcReceiveQueue): Promise<Space> {
        return produceAsync<Space>(space, async draft => {
            draft.comments = await q.receiveListDefined(space.comments, async c => {
                return await produceAsync(c, async draft => {
                    draft.multiline = await q.receive(c.multiline);
                    draft.text = await q.receive(c.text);
                    draft.suffix = await q.receive(c.suffix);
                    draft.markers = await q.receiveMarkers(c.markers);
                })
            });
            draft.whitespace = await q.receive(space.whitespace);
        });
    }

    protected async visitRightPadded<T extends Json>(right: JsonRightPadded<T>, p: RpcReceiveQueue): Promise<JsonRightPadded<T> | undefined> {
        if (!right) {
            throw new Error("TreeDataReceiveQueue should have instantiated an empty padding")
        }
        return produceAsync<JsonRightPadded<T>>(right, async draft => {
            draft.element = await p.receive(right.element, j => this.visit(j, p)!) as Draft<T>;
            draft.after = await p.receive(right.after, space => this.visitSpace(space, p));
            draft.markers = await p.receiveMarkers(right.markers);
        });
    }
}

const jsonCodec: RpcCodec<Json> = {
    async rpcReceive(before: Json, q: RpcReceiveQueue): Promise<Json> {
        return (await new JsonReceiver().visit(before, q))!;
    },

    rpcSend(after: Json, q: RpcSendQueue): void {
        new JsonSender().visit(after, q).then();
    }
}

Object.values(JsonKind).forEach(kind => {
    RpcCodecs.registerCodec(kind, jsonCodec);
});
