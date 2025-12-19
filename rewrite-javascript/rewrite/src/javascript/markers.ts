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
import {J} from "../java";
import {JS} from "./tree";
import {RpcCodecs, RpcReceiveQueue, RpcSendQueue} from "../rpc";
import {createDraft, finishDraft} from "immer";
import {PrettierStyle, StyleKind} from "./style";

declare module "./tree" {
    namespace JS {
        export const Markers: {
            readonly Generator: "org.openrewrite.javascript.marker.Generator";
            readonly Optional: "org.openrewrite.javascript.marker.Optional";
            readonly NonNullAssertion: "org.openrewrite.javascript.marker.NonNullAssertion";
            readonly Spread: "org.openrewrite.javascript.marker.Spread";
            readonly DelegatedYield: "org.openrewrite.javascript.marker.DelegatedYield";
            readonly FunctionDeclaration: "org.openrewrite.javascript.marker.FunctionDeclaration";
        };
    }
}

// At runtime actually attach it to JS
(JS as any).Markers = {
    Generator: "org.openrewrite.javascript.marker.Generator",
    DelegatedYield: "org.openrewrite.javascript.marker.DelegatedYield",
    Optional: "org.openrewrite.javascript.marker.Optional",
    NonNullAssertion: "org.openrewrite.javascript.marker.NonNullAssertion",
    Spread: "org.openrewrite.javascript.marker.Spread",
    FunctionDeclaration: "org.openrewrite.javascript.marker.FunctionDeclaration",
} as const;

/**
 * The `yield*` operator in JavaScript is a special case of the `yield` operator,
 * used in generator functions to yield to another iterator. This marker will
 * be applied to a {@link J.Yield} expression.
 */
export interface DelegatedYield extends Marker {
    readonly kind: typeof JS.Markers.DelegatedYield;
    readonly prefix: J.Space;
}

export interface Optional extends Marker {
    readonly kind: typeof JS.Markers.Optional;
    readonly prefix: J.Space;
}

export interface Generator extends Marker {
    readonly kind: typeof JS.Markers.Generator;
    readonly prefix: J.Space;
}

export interface NonNullAssertion extends Marker {
    readonly kind: typeof JS.Markers.NonNullAssertion;
    readonly prefix: J.Space;
}

export interface Spread extends Marker {
    readonly kind: typeof JS.Markers.Spread;
    readonly prefix: J.Space;
}

export interface FunctionDeclaration extends Marker {
    readonly kind: typeof JS.Markers.FunctionDeclaration;
    readonly prefix: J.Space;
}

/**
 * Registers an RPC codec for any marker that has a `prefix: J.Space` field.
 */
function registerPrefixedMarkerCodec<M extends Marker & { prefix: J.Space }>(
    kind: M["kind"]
) {
    RpcCodecs.registerCodec(kind, {
        async rpcReceive(before: M, q: RpcReceiveQueue): Promise<M> {
            const draft = createDraft(before);
            draft.id = await q.receive(before.id);
            draft.prefix = await q.receive(before.prefix);
            return finishDraft(draft) as M;
        },

        async rpcSend(after: M, q: RpcSendQueue): Promise<void> {
            await q.getAndSend(after, a => a.id);
            await q.getAndSend(after, a => a.prefix);
        }
    });
}

registerPrefixedMarkerCodec<DelegatedYield>(JS.Markers.DelegatedYield);
registerPrefixedMarkerCodec<Optional>(JS.Markers.Optional);
registerPrefixedMarkerCodec<Generator>(JS.Markers.Generator);
registerPrefixedMarkerCodec<NonNullAssertion>(JS.Markers.NonNullAssertion);
registerPrefixedMarkerCodec<Spread>(JS.Markers.Spread);
registerPrefixedMarkerCodec<FunctionDeclaration>(JS.Markers.FunctionDeclaration);

// Register codec for PrettierStyle (a NamedStyles that contains Prettier configuration)
// Only serialize the variable fields; constant fields are defined in the class
RpcCodecs.registerCodec(StyleKind.PrettierStyle, {
    async rpcReceive(before: PrettierStyle, q: RpcReceiveQueue): Promise<PrettierStyle> {
        const draft = createDraft(before);
        (draft as any).id = await q.receive(before.id);
        (draft as any).config = await q.receive(before.config);
        (draft as any).prettierVersion = await q.receive(before.prettierVersion);
        return finishDraft(draft) as PrettierStyle;
    },

    async rpcSend(after: PrettierStyle, q: RpcSendQueue): Promise<void> {
        await q.getAndSend(after, a => a.id);
        await q.getAndSend(after, a => a.config);
        await q.getAndSend(after, a => a.prettierVersion);
    }
});
