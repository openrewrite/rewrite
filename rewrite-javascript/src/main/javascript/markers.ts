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
