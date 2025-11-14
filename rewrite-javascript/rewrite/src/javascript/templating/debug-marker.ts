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
import {randomId} from '../../index';
import {Marker, Markers, MarkersKind} from '../../markers';

/**
 * Custom marker for highlighting pattern mismatches in debug output.
 * Contains before/after strings to render around the marked element.
 */
export class PatternMismatchMarker implements Marker {
    readonly kind = 'org.openrewrite.javascript.templating.PatternMismatchMarker';
    readonly id: string;

    constructor(
        public readonly before: string,
        public readonly after: string,
        id?: string
    ) {
        this.id = id || randomId();
    }
}

/**
 * Adds a PatternMismatchMarker to an AST element.
 *
 * @param tree The tree element to mark
 * @param before String to render before the element (default: ANSI inverse video)
 * @param after String to render after the element (default: ANSI inverse video to restore)
 * @returns The tree with the marker added
 */
export function withPatternMismatchMarker<T extends { markers: Markers }>(
    tree: T,
    before: string = '\x1b[7m',  // ANSI: Inverse video (swap fg/bg colors); else '👉'
    after: string = '\x1b[27m'   // ANSI: Normal video (turn off inverse); else '👈'
): T {
    const marker = new PatternMismatchMarker(before, after);
    const updatedMarkers = [...tree.markers.markers, marker];
    const newMarkers: Markers = {
        kind: MarkersKind.Markers,
        id: tree.markers.id,
        markers: updatedMarkers
    };
    return {...tree, markers: newMarkers};
}
