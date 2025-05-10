import {Marker} from "../markers";
import {J} from "./tree";
import {RpcCodecs, RpcReceiveQueue, RpcSendQueue} from "../rpc";
import {createDraft, finishDraft} from "immer";

declare module "./tree" {
    namespace J {
        export const Markers: {
            readonly Semicolon: "org.openrewrite.java.marker.Semicolon";
            readonly TrailingComma: "org.openrewrite.java.marker.TrailingComma";
            readonly OmitParentheses: "org.openrewrite.java.marker.OmitParentheses";
        };
    }
}

export interface Semicolon extends Marker {
    readonly kind: typeof J.Markers.Semicolon
}

export interface TrailingComma extends Marker {
    readonly kind: typeof J.Markers.TrailingComma;
    readonly suffix: J.Space;
}

export interface OmitParentheses extends Marker {
    readonly kind: typeof J.Markers.OmitParentheses;
}

const spaceCodec = RpcCodecs.forType(J.Kind.Space)!;

// Register codecs for all Java markers with additional properties
RpcCodecs.registerCodec(J.Markers.TrailingComma, {
    async rpcReceive(before: TrailingComma, q: RpcReceiveQueue): Promise<TrailingComma> {
        const draft = createDraft(before);
        draft.id = await q.receive(before.id);
        draft.suffix = await q.receive(before.suffix, space => spaceCodec.rpcReceive(space, q))
        return finishDraft(draft);
    },

    async rpcSend(after: TrailingComma, q: RpcSendQueue): Promise<void> {
        await q.getAndSend(after, a => a.id);
        await q.getAndSend(after, a => a.suffix, space => spaceCodec.rpcSend(space, q));
    }
});
