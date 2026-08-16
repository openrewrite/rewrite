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
import {Marker} from "../markers";
import {J} from "./tree";
import {Type} from "./type";
import {asRef, RpcCodecs, RpcReceiveQueue, RpcSendQueue} from "../rpc";
import {updateIfChanged} from "../util";
// The `RpcCodec` for `J.Space` is registered in the `rpc` module.
import {TypeReceiver, TypeSender} from "./rpc";

declare module "./tree" {
    namespace J {
        export const Markers: {
            readonly Semicolon: "org.openrewrite.java.marker.Semicolon";
            readonly TrailingComma: "org.openrewrite.java.marker.TrailingComma";
            readonly OmitParentheses: "org.openrewrite.java.marker.OmitParentheses";
            readonly JavaSourceSet: "org.openrewrite.java.marker.JavaSourceSet";
        };
    }
}

// At runtime actually attach it to J
(J as any).Markers = {
    Semicolon: "org.openrewrite.java.marker.Semicolon",
    TrailingComma: "org.openrewrite.java.marker.TrailingComma",
    OmitParentheses: "org.openrewrite.java.marker.OmitParentheses",
    JavaSourceSet: "org.openrewrite.java.marker.JavaSourceSet"
} as const;

export interface Semicolon extends Marker {
    readonly kind: typeof J.Markers.Semicolon;
}

export interface TrailingComma extends Marker {
    readonly kind: typeof J.Markers.TrailingComma;
    readonly suffix: J.Space;
}

export interface OmitParentheses extends Marker {
    readonly kind: typeof J.Markers.OmitParentheses;
}

/**
 * Rides on every source file in a Java source set, resource files included, so it reaches this
 * peer through PlainText/JSON/YAML as well as JavaScript.
 */
export interface JavaSourceSet extends Marker {
    readonly kind: typeof J.Markers.JavaSourceSet;
    readonly name: string;
    readonly classpath: Type.FullyQualified[];
    readonly gavToTypes: { [gav: string]: Type.FullyQualified[] };
}

// Register codecs for all Java markers with additional properties
RpcCodecs.registerCodec(J.Markers.TrailingComma, {
    async rpcReceive(before: TrailingComma, q: RpcReceiveQueue): Promise<TrailingComma> {
        return updateIfChanged(before, {
            id: await q.receive(before.id),
            suffix: await q.receive(before.suffix),
        });
    },

    async rpcSend(after: TrailingComma, q: RpcSendQueue): Promise<void> {
        await q.getAndSend(after, a => a.id);
        await q.getAndSend(after, a => a.suffix);
    }
});

// Field order mirrors org.openrewrite.java.marker.JavaSourceSet#rpcSend, which is the canonical
// protocol. gavToTypes travels as a key list plus one ref-deduplicated bucket per key, so its
// values resolve to the classpath instances sent above rather than a second copy of the graph.
RpcCodecs.registerCodec(J.Markers.JavaSourceSet, {
    async rpcReceive(before: JavaSourceSet, q: RpcReceiveQueue): Promise<JavaSourceSet> {
        const typeReceiver = new TypeReceiver();
        const id = await q.receive(before.id);
        const name = await q.receive(before.name);
        const classpath = await q.receiveList(before.classpath,
            t => typeReceiver.visit(t, q) as Promise<Type.FullyQualified>);
        const gavs = await q.receiveList<string>(before.gavToTypes && Object.keys(before.gavToTypes));
        const gavToTypes: { [gav: string]: Type.FullyQualified[] } = {};
        for (const gav of gavs || []) {
            gavToTypes[gav] = (await q.receiveList(before.gavToTypes?.[gav],
                t => typeReceiver.visit(t, q) as Promise<Type.FullyQualified>))!;
        }
        return updateIfChanged(before, {id, name, classpath, gavToTypes});
    },

    async rpcSend(after: JavaSourceSet, q: RpcSendQueue): Promise<void> {
        const typeSender = new TypeSender();
        await q.getAndSend(after, a => a.id);
        await q.getAndSend(after, a => a.name);
        await q.getAndSendList(after, a => (a.classpath || []).map(t => asRef(t)),
            t => Type.signature(t), t => typeSender.visit(t, q));
        await q.getAndSendList(after, a => Object.keys(a.gavToTypes || {}), gav => gav);
        for (const gav of Object.keys(after.gavToTypes || {})) {
            await q.getAndSendList(after, a => (a.gavToTypes[gav] || []).map(t => asRef(t)),
                t => Type.signature(t), t => typeSender.visit(t, q));
        }
    }
});

/**
 * Registers an RPC codec for any marker without additional properties.
 */
export function registerMarkerCodec<M extends Marker>(
    kind: M["kind"]
) {
    RpcCodecs.registerCodec(kind, {
        async rpcReceive(before: M, q: RpcReceiveQueue): Promise<M> {
            return updateIfChanged(before, {
                id: await q.receive(before.id),
            } as Partial<M>);
        },

        async rpcSend(after: M, q: RpcSendQueue): Promise<void> {
            await q.getAndSend(after, a => a.id);
        }
    });
}

registerMarkerCodec(J.Markers.Semicolon);
registerMarkerCodec(J.Markers.OmitParentheses);
