import {Marker} from "../markers";
import {J} from "./tree";
import {RpcCodecs, RpcReceiveQueue, RpcSendQueue} from "../rpc";
import {createDraft, finishDraft} from "immer";
// The `RpcCodec` for `J.Space` is registered in the `rpc` module.
import "./rpc";

declare module "./tree" {
    namespace J {
        export const Markers: {
            readonly Semicolon: "org.openrewrite.java.marker.Semicolon";
            readonly TrailingComma: "org.openrewrite.java.marker.TrailingComma";
            readonly OmitParentheses: "org.openrewrite.java.marker.OmitParentheses";
        };
    }
}

// At runtime actually attach it to J
(J as any).Markers = {
    Semicolon: "org.openrewrite.java.marker.Semicolon",
    TrailingComma: "org.openrewrite.java.marker.TrailingComma",
    OmitParentheses: "org.openrewrite.java.marker.OmitParentheses"
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

// Register codecs for all Java markers with additional properties
RpcCodecs.registerCodec(J.Markers.TrailingComma, {
    async rpcReceive(before: TrailingComma, q: RpcReceiveQueue): Promise<TrailingComma> {
        const draft = createDraft(before);
        draft.id = await q.receive(before.id);
        draft.suffix = await q.receive(before.suffix)
        return finishDraft(draft);
    },

    async rpcSend(after: TrailingComma, q: RpcSendQueue): Promise<void> {
        await q.getAndSend(after, a => a.id);
        await q.getAndSend(after, a => a.suffix);
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
            const draft = createDraft(before);
            draft.id = await q.receive(before.id);
            return finishDraft(draft) as M;
        },

        async rpcSend(after: M, q: RpcSendQueue): Promise<void> {
            await q.getAndSend(after, a => a.id);
        }
    });
}

registerMarkerCodec(J.Markers.Semicolon);
registerMarkerCodec(J.Markers.OmitParentheses);
