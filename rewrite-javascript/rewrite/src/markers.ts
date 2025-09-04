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
import {randomId, UUID} from "./uuid";
import {asRef} from "./reference";

export const MarkersKind = {
    Markers: "org.openrewrite.marker.Markers",
    NamedStyles: "org.openrewrite.marker.NamedStyles",
    SearchResult: "org.openrewrite.marker.SearchResult",
    ParseExceptionResult: "org.openrewrite.ParseExceptionResult",

    /**
     * A generic marker that is sent/received as a bare map because the type hasn't been
     * defined in both Java and JavaScript.
     */
    RpcMarker: "org.openrewrite.rpc.RpcMarker",
} as const

export interface Marker {
    readonly kind: string
    readonly id: UUID
}

export function marker(id: UUID, data?: {}): Marker {
    return {
        kind: MarkersKind.RpcMarker,
        id,
        ...data
    }
}

export function markers(...markers: Marker[]): Markers {
    return {
        kind: MarkersKind.Markers,
        id: randomId(),
        markers
    }
}

export interface Markers {
    readonly kind: typeof MarkersKind.Markers
    readonly id: UUID
    readonly markers: Marker[]
}

export function findMarker<T extends Marker>(
    o: { markers: Markers },
    kind: T["kind"]
): T | undefined {
    return o.markers.markers.find(
        (m): m is T => m.kind === kind
    );
}

export const emptyMarkers: Markers = asRef({
    kind: MarkersKind.Markers,
    id: randomId(),
    markers: []
});

export interface SearchResult extends Marker {
    readonly kind: typeof MarkersKind.SearchResult,
    readonly description?: string
}

export function foundSearchResult<T extends { markers: Markers }>(t: T, description?: string): T {
    const existing = findMarker(t, MarkersKind.SearchResult);
    if (!existing || (existing as SearchResult).description !== description) {
        return {
            ...t,
            markers: {
                ...t.markers,
                markers: [...t.markers.markers, {
                    kind: MarkersKind.SearchResult,
                    id: randomId(),
                    description: description
                } as SearchResult]
            }
        } as T;
    }
    return t;
}

export interface ParseExceptionResult extends Marker {
    readonly kind: typeof MarkersKind.ParseExceptionResult
    readonly parserType: string
    readonly exceptionType: string
    readonly message: string
    readonly treeType?: string;
}
