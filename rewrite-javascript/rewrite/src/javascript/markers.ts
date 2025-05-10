import {Marker} from "../markers";
import {J} from "../java";
import {JS} from "./tree";
import {RpcCodecs, RpcReceiveQueue, RpcSendQueue} from "../rpc";
import {createDraft, finishDraft} from "immer";

declare module "./tree" {
    namespace JS {
        export const Markers: {
            readonly DelegatedYield: "org.openrewrite.javascript.marker.DelegatedYield";
        };
    }
}

// At runtime actually attach it to JS
(JS as any).Markers = {
    DelegatedYield: "org.openrewrite.javascript.marker.DelegatedYield"
} as const;

export interface DelegatedYield extends Marker {
    readonly kind: typeof JS.Markers.DelegatedYield;
    readonly prefix: J.Space;
}

/**
 * Registers an RPC codec for any marker that has a `prefix: J.Space` field.
 */
function registerPrefixedMarkerCodec<M extends Marker & { prefix: J.Space }>(
    kind: M["kind"]
) {
    const spaceCodec = RpcCodecs.forType(J.Kind.Space)!;
    RpcCodecs.registerCodec(kind, {
        async rpcReceive(before: M, q: RpcReceiveQueue): Promise<M> {
            const draft = createDraft(before);
            draft.id = await q.receive(before.id);
            draft.prefix = await q.receive(before.prefix, space => spaceCodec.rpcReceive(space, q));
            return finishDraft(draft) as M;
        },

        async rpcSend(after: M, q: RpcSendQueue): Promise<void> {
            await q.getAndSend(after, a => a.id);
            await q.getAndSend(after, a => a.prefix, space => spaceCodec.rpcSend(space, q));
        }
    });
}

registerPrefixedMarkerCodec<DelegatedYield>(JS.Markers.DelegatedYield);
