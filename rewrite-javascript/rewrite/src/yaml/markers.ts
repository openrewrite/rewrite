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
import {randomId} from "../uuid";
import {Yaml} from "./tree";
import {RpcCodecs, RpcReceiveQueue, RpcSendQueue} from "../rpc";
import {updateIfChanged} from "../util";

declare module "./tree" {
    namespace Yaml {
        export const Markers: {
            readonly OmitColon: "org.openrewrite.yaml.marker.OmitColon";
        };
    }
}

// At runtime actually attach it to Yaml
(Yaml as any).Markers = {
    OmitColon: "org.openrewrite.yaml.marker.OmitColon"
} as const;

/**
 * Marker indicating that a mapping entry should be printed without a colon.
 * This is used for flow mappings like { "key", "key2" } where entries lack explicit colons.
 */
export interface OmitColon extends Marker {
    readonly kind: typeof Yaml.Markers.OmitColon;
}

/**
 * Creates an OmitColon marker.
 */
export function omitColon(): OmitColon {
    return {
        kind: Yaml.Markers.OmitColon,
        id: randomId()
    };
}

/**
 * Registers an RPC codec for any marker without additional properties.
 */
function registerMarkerCodec<M extends Marker>(kind: M["kind"]) {
    RpcCodecs.registerCodec(kind, {
        rpcReceive(before: M, q: RpcReceiveQueue): M {
            return updateIfChanged(before, {
                id: q.receive(before.id),
            } as Partial<M>);
        },

        rpcSend(after: M, q: RpcSendQueue): void {
            q.getAndSend(after, a => a.id);
        }
    });
}

registerMarkerCodec(Yaml.Markers.OmitColon);
