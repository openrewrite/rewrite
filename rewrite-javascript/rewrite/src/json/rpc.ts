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
import {updateIfChanged} from "../util";

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

class JsonReceiver {

    public visit<T extends Json>(j: T | undefined, q: RpcReceiveQueue): T | undefined {
        if (!j) return undefined;

        let result: Json | undefined = this.preVisit(j, q);
        if (result === undefined) return undefined;

        switch (j.kind) {
            case Json.Kind.Document:
                result = this.visitDocument(result as Json.Document, q);
                break;
            case Json.Kind.Array:
                result = this.visitArray(result as Json.Array, q);
                break;
            case Json.Kind.Empty:
                result = this.visitEmpty(result as Json.Empty);
                break;
            case Json.Kind.Identifier:
                result = this.visitIdentifier(result as Json.Identifier, q);
                break;
            case Json.Kind.Literal:
                result = this.visitLiteral(result as Json.Literal, q);
                break;
            case Json.Kind.Member:
                result = this.visitMember(result as Json.Member, q);
                break;
            case Json.Kind.Object:
                result = this.visitObject(result as Json.Object, q);
                break;
        }

        return result as T | undefined;
    }

    preVisit(j: Json, q: RpcReceiveQueue): Json | undefined {
        return updateIfChanged(j, {
            id: q.receive(j.id),
            prefix: q.receive(j.prefix, space => this.visitSpace(space, q)),
            markers: q.receive(j.markers),
        });
    }

    protected visitDocument(document: Json.Document, q: RpcReceiveQueue): Json | undefined {
        return updateIfChanged(document, {
            sourcePath: q.receive(document.sourcePath),
            charsetName: q.receive(document.charsetName),
            charsetBomMarked: q.receive(document.charsetBomMarked),
            checksum: q.receive(document.checksum),
            fileAttributes: q.receive(document.fileAttributes),
            value: q.receive<Json.Value>(document.value, j => this.visit(j, q)!),
            eof: q.receive(document.eof, space => this.visitSpace(space, q)),
        });
    }

    protected visitArray(array: Json.Array, q: RpcReceiveQueue): Json | undefined {
        return updateIfChanged(array, {
            values: q.receiveListDefined(array.values,
                j => this.visitRightPadded(j, q)!)!,
        });
    }

    protected visitEmpty(empty: Json.Empty): Json | undefined {
        return empty;
    }

    protected visitIdentifier(identifier: Json.Identifier, q: RpcReceiveQueue): Json | undefined {
        return updateIfChanged(identifier, {
            name: q.receive(identifier.name),
        });
    }

    protected visitLiteral(literal: Json.Literal, q: RpcReceiveQueue): Json | undefined {
        return updateIfChanged(literal, {
            source: q.receive(literal.source),
            value: q.receive(literal.value),
        });
    }

    protected visitMember(member: Json.Member, q: RpcReceiveQueue): Json | undefined {
        return updateIfChanged(member, {
            key: q.receive(member.key,
                j => this.visitRightPadded(j, q)!)!,
            value: q.receive<Json.Value>(member.value,
                j => this.visit(j, q)!),
        });
    }

    protected visitObject(obj: Json.Object, q: RpcReceiveQueue): Json | undefined {
        return updateIfChanged(obj, {
            members: q.receiveListDefined(obj.members,
                j => this.visitRightPadded(j, q)),
        });
    }

    public visitSpace(space: Json.Space, q: RpcReceiveQueue): Json.Space {
        return updateIfChanged(space, {
            comments: q.receiveListDefined(space.comments, c => {
                return updateIfChanged(c, {
                    multiline: q.receive(c.multiline),
                    text: q.receive(c.text),
                    suffix: q.receive(c.suffix),
                    markers: q.receive(c.markers),
                });
            }),
            whitespace: q.receive(space.whitespace),
        });
    }

    public visitRightPadded<T extends Json>(right: Json.RightPadded<T>, p: RpcReceiveQueue): Json.RightPadded<T> | undefined {
        if (!right) {
            throw new Error("TreeDataReceiveQueue should have instantiated an empty padding")
        }
        return updateIfChanged(right, {
            element: p.receive(right.element, j => this.visit(j, p)!) as T,
            after: p.receive(right.after, space => this.visitSpace(space, p)),
            markers: p.receive(right.markers),
        });
    }
}

const receiver = new JsonReceiver();
const sender = new JsonSender();

// Register codec for all Json AST node types
for (const kind of Object.values(Json.Kind)) {
    if (kind === Json.Kind.Space) {
        RpcCodecs.registerCodec(kind, {
            rpcReceive(before: Json.Space, q: RpcReceiveQueue): Json.Space {
                return receiver.visitSpace(before, q)!;
            },

            async rpcSend(after: Json.Space, q: RpcSendQueue): Promise<void> {
                await sender.visitSpace(after, q);
            }
        }, Json.Kind.Document);
    } else if (kind === Json.Kind.RightPadded) {
        RpcCodecs.registerCodec(kind, {
            rpcReceive<T extends Json>(before: Json.RightPadded<T>, q: RpcReceiveQueue): Json.RightPadded<T> {
                return receiver.visitRightPadded(before, q)!;
            },

            async rpcSend<T extends Json>(after: Json.RightPadded<T>, q: RpcSendQueue): Promise<void> {
                await sender.visitRightPadded(after, q);
            }
        }, Json.Kind.Document);
    } else {
        RpcCodecs.registerCodec(kind as string, {
            rpcReceive(before: Json, q: RpcReceiveQueue): Json {
                return receiver.visit(before, q)!;
            },

            async rpcSend(after: Json, q: RpcSendQueue): Promise<void> {
                await sender.visit(after, q);
            }
        }, Json.Kind.Document);
    }
}
