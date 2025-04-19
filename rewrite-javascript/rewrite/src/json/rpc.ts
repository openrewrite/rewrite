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
import {JsonVisitor} from "./visitor";
import {asRef, RpcCodec, RpcCodecs, RpcReceiveQueue, RpcSendQueue} from "../rpc";
import {
    Empty,
    Identifier,
    Json,
    JsonArray,
    JsonDocument,
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
        await q.getAndSend(j, j2 => j2.id);
        await q.getAndSend(j, j2 => asRef(j2.prefix),
            async space => await this.visitSpace(space, q));
        await q.sendMarkers(j, j2 => j2.markers);
        return j;
    }

    protected async visitDocument(document: JsonDocument, q: RpcSendQueue): Promise<Json | undefined> {
        await q.getAndSend(document, d => d.sourcePath);
        await q.getAndSend(document, d => d.charsetName);
        await q.getAndSend(document, d => d.charsetBomMarked);
        await q.getAndSend(document, d => d.checksum);
        await q.getAndSend(document, d => d.fileAttributes);
        await q.getAndSend(document, d => d.value,
            async j => await this.visit(j, q));
        await q.getAndSend(document, d => asRef(d.eof));
        return document;
    }

    protected async visitArray(array: JsonArray, q: RpcSendQueue): Promise<Json | undefined> {
        await q.getAndSendList(array, a => a.values, j => j.element.id,
            async j => await this.visitRightPadded(j, q));
        return array;
    }

    protected async visitEmpty(empty: Empty): Promise<Json | undefined> {
        return empty;
    }

    protected async visitIdentifier(identifier: Identifier, q: RpcSendQueue): Promise<Json | undefined> {
        await q.getAndSend(identifier, id => id.name);
        return identifier;
    }

    protected async visitLiteral(literal: Literal, q: RpcSendQueue): Promise<Json | undefined> {
        await q.getAndSend(literal, lit => lit.source);
        await q.getAndSend(literal, lit => lit.value);
        return literal;
    }

    protected async visitMember(member: Member, q: RpcSendQueue): Promise<Json | undefined> {
        await q.getAndSend(member, m => m.key, async j => await this.visitRightPadded(j, q));
        await q.getAndSend(member, m => m.value, async j => await this.visit(j, q));
        return member;
    }

    protected async visitObject(obj: JsonObject, q: RpcSendQueue): Promise<Json | undefined> {
        await q.getAndSendList(obj, o => o.members, j => j.element.id,
            async j => await this.visitRightPadded(j, q));
        return obj;
    }

    protected async visitSpace(space: Space, q: RpcSendQueue): Promise<Space> {
        await q.getAndSendList(space, s => s.comments, c => c.text + c.suffix, async c => {
            await q.getAndSend(c, c2 => c2.multiline);
            await q.getAndSend(c, c2 => c2.text);
            await q.getAndSend(c, c2 => c2.suffix);
            await q.sendMarkers(c, c2 => c2.markers);
        });
        await q.getAndSend(space, s => s.whitespace);
        return space;
    }

    protected async visitRightPadded<T extends Json>(right: JsonRightPadded<T>, q: RpcSendQueue): Promise<JsonRightPadded<T> | undefined> {
        await q.getAndSend(right, r => r.element, j => this.visit(j, q));
        await q.getAndSend(right, r => asRef(r.after), async space => await this.visitSpace(space, q));
        await q.sendMarkers(right, r => r.markers);
        return right;
    }
}

// noinspection ES6MissingAwait
class JsonReceiver extends JsonVisitor<RpcReceiveQueue> {

    protected async preVisit(j: Json, q: RpcReceiveQueue): Promise<Json | undefined> {
        return this.produceJson<Json>(j, q, async draft => {
            draft.id = await q.receive(j.id);
            draft.prefix = await q.receive(j.prefix, async space => await this.visitSpace(space, q));
            draft.markers = await q.receiveMarkers(j.markers);
        });
    }

    protected async visitDocument(document: JsonDocument, q: RpcReceiveQueue): Promise<Json | undefined> {
        return this.produceJson<JsonDocument>(document, q, async draft => {
            draft.sourcePath = await q.receive(document.sourcePath);
            draft.charsetName = await q.receive(document.charsetName);
            draft.charsetBomMarked = await q.receive(document.charsetBomMarked);
            draft.checksum = await q.receive(document.checksum);
            draft.fileAttributes = await q.receive(document.fileAttributes);
            draft.value = await q.receive<JsonValue>(document.value, async j => await this.visit(j, q)!);
            draft.eof = await q.receive(document.eof);
        });
    }

    protected async visitArray(array: JsonArray, q: RpcReceiveQueue): Promise<Json | undefined> {
        return this.produceJson<JsonArray>(array, q, async draft => {
            draft.values = await q.receiveListDefined(array.values,
                async j => await this.visitRightPadded(j, q)!)!;
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
            draft.key = await q.receive(member.key,
                async j => await this.visitRightPadded(j, q)!)!;
            draft.value = await q.receive<JsonValue>(member.value,
                async j => await this.visit(j, q)!);
        });
    }

    protected async visitObject(obj: JsonObject, q: RpcReceiveQueue): Promise<Json | undefined> {
        return this.produceJson<JsonObject>(obj, q, async draft => {
            draft.members = await q.receiveListDefined(obj.members,
                async j => await this.visitRightPadded(j, q));
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
            draft.element = await p.receive(right.element,  async j => await this.visit(j, p)!) as Draft<T>;
            draft.after = await p.receive(right.after, async space => await this.visitSpace(space, p));
            draft.markers = await p.receiveMarkers(right.markers);
        });
    }
}

const jsonCodec: RpcCodec<Json> = {
    async rpcReceive(before: Json, q: RpcReceiveQueue): Promise<Json> {
        return (await new JsonReceiver().visit(before, q))!;
    },

    async rpcSend(after: Json, q: RpcSendQueue): Promise<void> {
        await new JsonSender().visit(after, q);
    }
}

Object.values(JsonKind).forEach(kind => {
    RpcCodecs.registerCodec(kind, jsonCodec);
});
