import {randomId, UUID} from "./uuid";

export const MarkersKind = {
    Markers: "org.openrewrite.marker.Markers",
    SearchResult: "org.openrewrite.marker.SearchResult",
    ParseExceptionResult: "org.openrewrite.marker.ParseExceptionResult"
} as const

export interface Marker {
    readonly id: UUID
}

export interface Markers {
    readonly kind: typeof MarkersKind.Markers
    readonly id: UUID
    readonly markers: Marker[]
}

export const emptyMarkers: Markers = {
    kind: MarkersKind.Markers,
    id: randomId(),
    markers: []
}

export interface SearchResult extends Marker {
    readonly kind: typeof MarkersKind.SearchResult,
    readonly description?: string
}

export interface ParseExceptionResult extends Marker {
    readonly kind: typeof MarkersKind.ParseExceptionResult
    readonly parserType: string
    readonly exceptionType?: string
    readonly message?: string
}
