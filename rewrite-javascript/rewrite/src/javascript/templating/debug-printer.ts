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
import {MarkerPrinter} from '../../print';
import {Cursor} from '../../tree';
import {Marker} from '../../markers';
import {PatternMismatchMarker} from './debug-marker';

/**
 * Custom MarkerPrinter for pattern debugging that renders PatternMismatchMarker
 * with custom before/after strings instead of the default SearchResult format.
 */
export const PATTERN_DEBUG_MARKER_PRINTER: MarkerPrinter = {
    beforeSyntax(marker: Marker, cursor: Cursor, commentWrapper: (input: string) => string): string {
        if (marker instanceof PatternMismatchMarker) {
            return marker.before;
        }
        // Fall back to default behavior for other markers
        return MarkerPrinter.DEFAULT.beforeSyntax(marker, cursor, commentWrapper);
    },

    beforePrefix(marker: Marker, cursor: Cursor, commentWrapper: (input: string) => string): string {
        // Fall back to default behavior
        return MarkerPrinter.DEFAULT.beforePrefix(marker, cursor, commentWrapper);
    },

    afterSyntax(marker: Marker, cursor: Cursor, commentWrapper: (input: string) => string): string {
        if (marker instanceof PatternMismatchMarker) {
            return marker.after;
        }
        // Fall back to default behavior for other markers
        return MarkerPrinter.DEFAULT.afterSyntax(marker, cursor, commentWrapper);
    }
};
