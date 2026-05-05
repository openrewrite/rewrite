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
package org.openrewrite.golang.rpc;

import lombok.Value;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;

class ParseProjectResponse extends ArrayList<ParseProjectResponse.Item> {

    @Value
    static class Item {
        String id;
        String sourceFileType;

        /**
         * The relative source path of the file. May be null when talking to an
         * older Go peer that doesn't populate it yet — callers fall back to
         * {@link #id} for error reporting in that case.
         */
        @Nullable String sourcePath;
    }
}
