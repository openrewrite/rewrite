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
import {asRef, RpcCodecs, RpcReceiveQueue, RpcSendQueue} from "../rpc";
import {Json} from "./tree";
import {updateIfChanged} from "../util";

/**
 * Dispatches a Json element to the sender's or receiver's method for its kind, like the Java module's `JavaRpcVisitor`.
 */
abstract class JsonRpcVisitor<Q> {
    visit<R extends Json>(tree: Json | undefined, q: Q): R | undefined {
        if (!tree) {
            return undefined;
        }
        const t = this.preVisit(tree, q);
        return t === undefined ? undefined : this.accept(t, q) as R | undefined;
    }

    protected abstract preVisit(tree: Json, q: Q): Json | undefined;

    protected accept(t: Json, q: Q): Json | undefined {
        switch (t.kind) {
            case Json.Kind.Array:
                return this.visitArray(t as unknown as Json.Array, q);
            case Json.Kind.Document:
                return this.visitDocument(t as unknown as Json.Document, q);
            case Json.Kind.Empty:
                return this.visitEmpty(t as unknown as Json.Empty, q);
            case Json.Kind.Identifier:
                return this.visitIdentifier(t as unknown as Json.Identifier, q);
            case Json.Kind.Literal:
                return this.visitLiteral(t as unknown as Json.Literal, q);
            case Json.Kind.Member:
                return this.visitMember(t as unknown as Json.Member, q);
            case Json.Kind.Object:
                return this.visitObject(t as unknown as Json.Object, q);
            default:
                throw new Error(`No RPC codec for kind ${t.kind}`);
        }
    }

    protected abstract visitArray(tree: Json.Array, q: Q): Json | undefined;
    protected abstract visitDocument(tree: Json.Document, q: Q): Json | undefined;
    protected abstract visitEmpty(tree: Json.Empty, q: Q): Json | undefined;
    protected abstract visitIdentifier(tree: Json.Identifier, q: Q): Json | undefined;
    protected abstract visitLiteral(tree: Json.Literal, q: Q): Json | undefined;
    protected abstract visitMember(tree: Json.Member, q: Q): Json | undefined;
    protected abstract visitObject(tree: Json.Object, q: Q): Json | undefined;
}

class JsonSender extends JsonRpcVisitor<RpcSendQueue> {

    protected preVisit(j: Json, q: RpcSendQueue): Json | undefined {
        q.getAndSend(j, j2 => j2.id);
        q.getAndSend(j, j2 => asRef(j2.prefix),
            space => this.visitSpace(space, q));
        q.getAndSend(j, j2 => j2.markers);
        return j;
    }

    protected visitDocument(document: Json.Document, q: RpcSendQueue): Json | undefined {
        q.getAndSend(document, d => d.sourcePath);
        q.getAndSend(document, d => d.charsetName);
        q.getAndSend(document, d => d.charsetBomMarked);
        q.getAndSend(document, d => d.checksum);
        q.getAndSend(document, d => d.fileAttributes);
        q.getAndSend(document, d => d.value,
            j => this.visit(j, q));
        q.getAndSend(document, d => asRef(d.eof),
            space => this.visitSpace(space, q));
        return document;
    }

    protected visitArray(array: Json.Array, q: RpcSendQueue): Json | undefined {
        q.getAndSendList(array, a => a.values, j => j.element.id,
            j => this.visitRightPadded(j, q));
        return array;
    }

    protected visitEmpty(empty: Json.Empty): Json | undefined {
        return empty;
    }

    protected visitIdentifier(identifier: Json.Identifier, q: RpcSendQueue): Json | undefined {
        q.getAndSend(identifier, id => id.name);
        return identifier;
    }

    protected visitLiteral(literal: Json.Literal, q: RpcSendQueue): Json | undefined {
        q.getAndSend(literal, lit => lit.source);
        q.getAndSend(literal, lit => lit.value);
        return literal;
    }

    protected visitMember(member: Json.Member, q: RpcSendQueue): Json | undefined {
        q.getAndSend(member, m => m.key, j => this.visitRightPadded(j, q));
        q.getAndSend(member, m => m.value, j => this.visit(j, q));
        return member;
    }

    protected visitObject(obj: Json.Object, q: RpcSendQueue): Json | undefined {
        q.getAndSendList(obj, o => o.members, j => j.element.id,
            j => this.visitRightPadded(j, q));
        return obj;
    }

    public visitSpace(space: Json.Space, q: RpcSendQueue): Json.Space {
        q.getAndSendList(space, s => s.comments, c => c.text + c.suffix, c => {
            q.getAndSend(c, c2 => c2.multiline);
            q.getAndSend(c, c2 => c2.text);
            q.getAndSend(c, c2 => c2.suffix);
            q.getAndSend(c, c2 => c2.markers);
        });
        q.getAndSend(space, s => s.whitespace);
        return space;
    }

    public visitRightPadded<T extends Json>(right: Json.RightPadded<T>, q: RpcSendQueue): Json.RightPadded<T> | undefined {
        q.getAndSend(right, r => r.element, j => this.visit(j, q));
        q.getAndSend(right, r => asRef(r.after), space => this.visitSpace(space, q));
        q.getAndSend(right, r => r.markers);
        return right;
    }
}

// noinspection ES6MissingAwait
class JsonReceiver extends JsonRpcVisitor<RpcReceiveQueue> {

    protected preVisit(j: Json, q: RpcReceiveQueue): Json | undefined {
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
            markers: p.receiveMarkers(right.markers),
        });
    }
}

const receiver = new JsonReceiver();
const sender = new JsonSender();

// Register codec for all Java AST node types
for (const kind of Object.values(Json.Kind)) {
    if (kind === Json.Kind.Space) {
        RpcCodecs.registerCodec(kind, {
            rpcReceive(before: Json.Space, q: RpcReceiveQueue): Json.Space {
                return (receiver.visitSpace(before, q))!;
            },

            rpcSend(after: Json.Space, q: RpcSendQueue): void {
                sender.visitSpace(after, q);
            }
        }, Json.Kind.Document);
    } else if (kind === Json.Kind.RightPadded) {
        RpcCodecs.registerCodec(kind, {
            rpcReceive<T extends Json>(before: Json.RightPadded<T>, q: RpcReceiveQueue): Json.RightPadded<T> {
                return (receiver.visitRightPadded(before, q))!;
            },

            rpcSend<T extends Json>(after: Json.RightPadded<T>, q: RpcSendQueue): void {
                sender.visitRightPadded(after, q);
            }
        }, Json.Kind.Document);
    } else {
        RpcCodecs.registerCodec(kind as string, {
            rpcReceive(before: Json, q: RpcReceiveQueue): Json {
                return (receiver.visit(before, q))!;
            },

            rpcSend(after: Json, q: RpcSendQueue): void {
                sender.visit(after, q);
            }
        }, Json.Kind.Document);
    }
}
