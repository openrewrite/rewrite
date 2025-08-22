/*
 * Copyright 2025 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.rpc.request;

import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.PrintOutputCapture;

import java.util.List;

@Value
public class Print implements RpcRequest {
    String treeId;

    /**
     * A list of IDs representing the cursor whose objects are stored in the
     * caller's local object cache.
     */
    @Nullable
    List<String> cursor;

    @Nullable
    MarkerPrinter markerPrinter;

    public enum MarkerPrinter {
        DEFAULT,
        SEARCH_MARKERS_ONLY,
        FENCED,
        SANITIZED,
        ;

        public static MarkerPrinter from(PrintOutputCapture.MarkerPrinter markerPrinter) {
            if (markerPrinter == PrintOutputCapture.MarkerPrinter.DEFAULT) {
                return DEFAULT;
            }
            if (markerPrinter == PrintOutputCapture.MarkerPrinter.SEARCH_MARKERS_ONLY) {
                return SEARCH_MARKERS_ONLY;
            }
            if (markerPrinter == PrintOutputCapture.MarkerPrinter.FENCED) {
                return FENCED;
            }
            if (markerPrinter == PrintOutputCapture.MarkerPrinter.SANITIZED) {
                return SANITIZED;
            }
            throw new IllegalArgumentException("Unknown marker printer " + markerPrinter);
        }
    }
}
