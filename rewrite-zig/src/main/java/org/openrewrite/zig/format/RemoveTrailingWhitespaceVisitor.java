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
package org.openrewrite.zig.format;

import org.jspecify.annotations.Nullable;
import org.openrewrite.Tree;
import org.openrewrite.java.tree.Space;
import org.openrewrite.zig.ZigVisitor;

public class RemoveTrailingWhitespaceVisitor<P> extends ZigVisitor<P> {
    @Nullable
    private final Tree stopAfter;

    public RemoveTrailingWhitespaceVisitor(@Nullable Tree stopAfter) {
        this.stopAfter = stopAfter;
    }

    @Override
    public Space visitSpace(Space space, Space.Location loc, P p) {
        String ws = space.getWhitespace();
        if (ws.contains("\n")) {
            // Remove spaces/tabs before each newline
            String normalized = ws.replaceAll("[ \t]+\n", "\n");
            if (!normalized.equals(ws)) {
                return space.withWhitespace(normalized);
            }
        }
        return space;
    }
}
