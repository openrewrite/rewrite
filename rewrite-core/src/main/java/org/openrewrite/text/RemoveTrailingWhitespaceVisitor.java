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
package org.openrewrite.text;

import com.fasterxml.jackson.annotation.JsonCreator;
import org.jspecify.annotations.Nullable;
import org.openrewrite.Cursor;
import org.openrewrite.Tree;

public class RemoveTrailingWhitespaceVisitor<P> extends PlainTextVisitor<P> {
    @Nullable
    private final Tree stopAfter;

    @JsonCreator
    public RemoveTrailingWhitespaceVisitor(@Nullable Tree stopAfter) {
        this.stopAfter = stopAfter;
    }

    public RemoveTrailingWhitespaceVisitor() {
        this(null);
    }

    @Override
    public @Nullable Tree visit(@Nullable Tree tree, P p, Cursor parent) {
        return super.visit(tree, p, parent);
    }

}
