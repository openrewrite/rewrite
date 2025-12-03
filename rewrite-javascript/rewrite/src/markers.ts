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

    // Markup markers for errors, warnings, info, and debug messages
    MarkupError: "org.openrewrite.marker.Markup$Error",
    MarkupWarn: "org.openrewrite.marker.Markup$Warn",
    MarkupInfo: "org.openrewrite.marker.Markup$Info",
    MarkupDebug: "org.openrewrite.marker.Markup$Debug",

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

/**
 * Replaces a marker in a Markers collection with a new marker.
 * If the old marker is not found, the new marker is added.
 *
 * @param markers The markers collection to update
 * @param oldMarker The marker to replace (matched by id)
 * @param newMarker The new marker to insert
 * @returns A new Markers object with the replacement applied
 */
export function replaceMarker(markers: Markers, oldMarker: Marker, newMarker: Marker): Markers {
    const newMarkers = markers.markers.map(m =>
        m.id === oldMarker.id ? newMarker : m
    );

    // If old marker wasn't found, add the new one
    if (!markers.markers.some(m => m.id === oldMarker.id)) {
        newMarkers.push(newMarker);
    }

    return {
        ...markers,
        markers: newMarkers
    };
}

/**
 * Replaces the first marker with the same kind as the new marker, or adds it if not found.
 * This is useful when there's typically only one marker of each kind.
 *
 * @param markers The markers collection to update
 * @param newMarker The new marker to insert (its kind is used to find the marker to replace)
 * @returns A new Markers object with the replacement applied
 */
export function replaceMarkerByKind(markers: Markers, newMarker: Marker): Markers {
    let found = false;
    const newMarkers = markers.markers.map(m => {
        if (!found && m.kind === newMarker.kind) {
            found = true;
            return newMarker;
        }
        return m;
    });

    // If marker with kind wasn't found, add the new one
    if (!found) {
        newMarkers.push(newMarker);
    }

    return {
        ...markers,
        markers: newMarkers
    };
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

/**
 * Base interface for Markup markers that attach messages to AST nodes.
 * Used for errors, warnings, info, and debug messages.
 */
export interface Markup extends Marker {
    readonly message: string;
    readonly detail?: string;
}

export interface MarkupError extends Markup {
    readonly kind: typeof MarkersKind.MarkupError;
}

export interface MarkupWarn extends Markup {
    readonly kind: typeof MarkersKind.MarkupWarn;
}

export interface MarkupInfo extends Markup {
    readonly kind: typeof MarkersKind.MarkupInfo;
}

export interface MarkupDebug extends Markup {
    readonly kind: typeof MarkersKind.MarkupDebug;
}

/**
 * Attaches an error marker to a tree node.
 */
export function markupError<T extends { markers: Markers }>(t: T, message: string, detail?: string): T {
    return addMarkup(t, {
        kind: MarkersKind.MarkupError,
        id: randomId(),
        message,
        detail
    });
}

/**
 * Attaches a warning marker to a tree node.
 */
export function markupWarn<T extends { markers: Markers }>(t: T, message: string, detail?: string): T {
    return addMarkup(t, {
        kind: MarkersKind.MarkupWarn,
        id: randomId(),
        message,
        detail
    });
}

/**
 * Attaches an info marker to a tree node.
 */
export function markupInfo<T extends { markers: Markers }>(t: T, message: string, detail?: string): T {
    return addMarkup(t, {
        kind: MarkersKind.MarkupInfo,
        id: randomId(),
        message,
        detail
    });
}

/**
 * Attaches a debug marker to a tree node.
 */
export function markupDebug<T extends { markers: Markers }>(t: T, message: string, detail?: string): T {
    return addMarkup(t, {
        kind: MarkersKind.MarkupDebug,
        id: randomId(),
        message,
        detail
    });
}

/**
 * Helper to add a markup marker to a tree node.
 */
function addMarkup<T extends { markers: Markers }>(t: T, markup: Markup): T {
    return {
        ...t,
        markers: {
            ...t.markers,
            markers: [...t.markers.markers, markup]
        }
    } as T;
}
