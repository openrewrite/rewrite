/*
 * Copyright 2026 the original author or authors.
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

import {randomId} from "../src";
import {Marker, markers, MarkersKind, removeMarkerByKind} from "../src/markers";

describe('removeMarkerByKind', () => {
    const searchResult = (): Marker => ({kind: MarkersKind.SearchResult, id: randomId()});
    const rpcMarker: Marker = {kind: MarkersKind.RpcMarker, id: randomId()};

    test('removes every marker of the kind and keeps the rest', () => {
        const removed = removeMarkerByKind(markers(searchResult(), rpcMarker, searchResult()), MarkersKind.SearchResult);
        expect(removed.markers).toEqual([rpcMarker]);
    });

    test('returns the same instance when no marker has the kind', () => {
        const unchanged = markers(rpcMarker);
        expect(removeMarkerByKind(unchanged, MarkersKind.SearchResult)).toBe(unchanged);
    });
});
