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
import {asRef, RpcCodecs, RpcReceiveQueue, RpcSendQueue} from "../rpc";
import {Json} from "./tree";
import {produceAsync} from "../visitor";
import {createDraft, Draft, finishDraft} from "immer";

class JsonSender extends JsonVisitor<RpcSendQueue> {

    protected async preVisit(j: Json, q: RpcSendQueue): Promise<Json | undefined> {
        await q.getAndSend(j, j2 => j2.id);
        await q.getAndSend(j, j2 => asRef(j2.prefix),
            async space => await this.visitSpace(space, q));
        await q.getAndSend(j, j2 => j2.markers);
        return j;
    }

    protected async visitDocument(document: Json.Document, q: RpcSendQueue): Promise<Json | undefined> {
        await q.getAndSend(document, d => d.sourcePath);
        await q.getAndSend(document, d => d.charsetName);
        await q.getAndSend(document, d => d.charsetBomMarked);
        await q.getAndSend(document, d => d.checksum);
        await q.getAndSend(document, d => d.fileAttributes);
        await q.getAndSend(document, d => d.value,
            async j => await this.visit(j, q));
        await q.getAndSend(document, d => asRef(d.eof),
            async space => await this.visitSpace(space, q));
        return document;
    }

    protected async visitArray(array: Json.Array, q: RpcSendQueue): Promise<Json | undefined> {
        await q.getAndSendList(array, a => a.values, j => j.element.id,
            async j => await this.visitRightPadded(j, q));
        return array;
    }

    protected async visitEmpty(empty: Json.Empty): Promise<Json | undefined> {
        return empty;
    }

    protected async visitIdentifier(identifier: Json.Identifier, q: RpcSendQueue): Promise<Json | undefined> {
        await q.getAndSend(identifier, id => id.name);
        return identifier;
    }

    protected async visitLiteral(literal: Json.Literal, q: RpcSendQueue): Promise<Json | undefined> {
        await q.getAndSend(literal, lit => lit.source);
        await q.getAndSend(literal, lit => lit.value);
        return literal;
    }

    protected async visitMember(member: Json.Member, q: RpcSendQueue): Promise<Json | undefined> {
        await q.getAndSend(member, m => m.key, async j => await this.visitRightPadded(j, q));
        await q.getAndSend(member, m => m.value, async j => await this.visit(j, q));
        return member;
    }

    protected async visitObject(obj: Json.Object, q: RpcSendQueue): Promise<Json | undefined> {
        await q.getAndSendList(obj, o => o.members, j => j.element.id,
            async j => await this.visitRightPadded(j, q));
        return obj;
    }

    public async visitSpace(space: Json.Space, q: RpcSendQueue): Promise<Json.Space> {
        await q.getAndSendList(space, s => s.comments, c => c.text + c.suffix, async c => {
            await q.getAndSend(c, c2 => c2.multiline);
            await q.getAndSend(c, c2 => c2.text);
            await q.getAndSend(c, c2 => c2.suffix);
            await q.getAndSend(c, c2 => c2.markers);
        });
        await q.getAndSend(space, s => s.whitespace);
        return space;
    }

    public async visitRightPadded<T extends Json>(right: Json.RightPadded<T>, q: RpcSendQueue): Promise<Json.RightPadded<T> | undefined> {
        await q.getAndSend(right, r => r.element, j => this.visit(j, q));
        await q.getAndSend(right, r => asRef(r.after), async space => await this.visitSpace(space, q));
        await q.getAndSend(right, r => r.markers);
        return right;
    }
}

// noinspection ES6MissingAwait
class JsonReceiver extends JsonVisitor<RpcReceiveQueue> {

    protected async preVisit(j: Json, q: RpcReceiveQueue): Promise<Json | undefined> {
        const draft = createDraft(j)
        draft.id = await q.receive(j.id);
        draft.prefix = await q.receive(j.prefix, async space => await this.visitSpace(space, q));
        draft.markers = await q.receive(j.markers);
        return finishDraft(draft);
    }

    protected async visitDocument(document: Json.Document, q: RpcReceiveQueue): Promise<Json | undefined> {
        const draft = createDraft(document);
        draft.sourcePath = await q.receive(document.sourcePath);
        draft.charsetName = await q.receive(document.charsetName);
        draft.charsetBomMarked = await q.receive(document.charsetBomMarked);
        draft.checksum = await q.receive(document.checksum);
        draft.fileAttributes = await q.receive(document.fileAttributes);
        draft.value = await q.receive<Json.Value>(document.value, async j => await this.visit(j, q)!);
        draft.eof = await q.receive(document.eof, async space => await this.visitSpace(space, q));
        return finishDraft(draft);
    }

    protected async visitArray(array: Json.Array, q: RpcReceiveQueue): Promise<Json | undefined> {
        const draft = createDraft(array);
        draft.values = await q.receiveListDefined(array.values,
            async j => await this.visitRightPadded(j, q)!)!;
        return finishDraft(draft);
    }

    protected async visitEmpty(empty: Json.Empty): Promise<Json | undefined> {
        return empty;
    }

    protected async visitIdentifier(identifier: Json.Identifier, q: RpcReceiveQueue): Promise<Json | undefined> {
        const draft = createDraft(identifier);
        draft.name = await q.receive(identifier.name);
        return finishDraft(draft);
    }

    protected async visitLiteral(literal: Json.Literal, q: RpcReceiveQueue): Promise<Json | undefined> {
        const draft = createDraft(literal);
        draft.source = await q.receive(literal.source);
        draft.value = await q.receive(literal.value);
        return finishDraft(draft);
    }

    protected async visitMember(member: Json.Member, q: RpcReceiveQueue): Promise<Json | undefined> {
        const draft = createDraft(member);
        draft.key = await q.receive(member.key,
            async j => await this.visitRightPadded(j, q)!)!;
        draft.value = await q.receive<Json.Value>(member.value,
            async j => await this.visit(j, q)!);
        return finishDraft(draft);
    }

    protected async visitObject(obj: Json.Object, q: RpcReceiveQueue): Promise<Json | undefined> {
        const draft = createDraft(obj);
        draft.members = await q.receiveListDefined(obj.members,
            async j => await this.visitRightPadded(j, q));
        return finishDraft(draft);
    }

    public async visitSpace(space: Json.Space, q: RpcReceiveQueue): Promise<Json.Space> {
        return produceAsync<Json.Space>(space, async draft => {
            draft.comments = await q.receiveListDefined(space.comments, async c => {
                return await produceAsync(c, async draft => {
                    draft.multiline = await q.receive(c.multiline);
                    draft.text = await q.receive(c.text);
                    draft.suffix = await q.receive(c.suffix);
                    draft.markers = await q.receive(c.markers);
                })
            });
            draft.whitespace = await q.receive(space.whitespace);
        });
    }

    public async visitRightPadded<T extends Json>(right: Json.RightPadded<T>, p: RpcReceiveQueue): Promise<Json.RightPadded<T> | undefined> {
        if (!right) {
            throw new Error("TreeDataReceiveQueue should have instantiated an empty padding")
        }
        return produceAsync<Json.RightPadded<T>>(right, async draft => {
            draft.element = await p.receive(right.element, async j => await this.visit(j, p)!) as Draft<T>;
            draft.after = await p.receive(right.after, async space => await this.visitSpace(space, p));
            draft.markers = await p.receiveMarkers(right.markers);
        });
    }
}

const receiver = new JsonReceiver();
const sender = new JsonSender();

// Register codec for all Java AST node types
for (const kind of Object.values(Json.Kind)) {
    if (kind === Json.Kind.Space) {
        RpcCodecs.registerCodec(kind, {
            async rpcReceive(before: Json.Space, q: RpcReceiveQueue): Promise<Json.Space> {
                return (await receiver.visitSpace(before, q))!;
            },

            async rpcSend(after: Json.Space, q: RpcSendQueue): Promise<void> {
                await sender.visitSpace(after, q);
            }
        }, Json.Kind.Document);
    } else if (kind === Json.Kind.RightPadded) {
        RpcCodecs.registerCodec(kind, {
            async rpcReceive<T extends Json>(before: Json.RightPadded<T>, q: RpcReceiveQueue): Promise<Json.RightPadded<T>> {
                return (await receiver.visitRightPadded(before, q))!;
            },

            async rpcSend<T extends Json>(after: Json.RightPadded<T>, q: RpcSendQueue): Promise<void> {
                await sender.visitRightPadded(after, q);
            }
        }, Json.Kind.Document);
    } else {
        RpcCodecs.registerCodec(kind as string, {
            async rpcReceive(before: Json, q: RpcReceiveQueue): Promise<Json> {
                return (await receiver.visit(before, q))!;
            },

            async rpcSend(after: Json, q: RpcSendQueue): Promise<void> {
                await sender.visit(after, q);
            }
        }, Json.Kind.Document);
    }
}
