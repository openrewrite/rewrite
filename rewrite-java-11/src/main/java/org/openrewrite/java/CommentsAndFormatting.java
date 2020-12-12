/*
 * Copyright 2020 the original author or authors.
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
package org.openrewrite.java;

import org.openrewrite.Formatting;
import org.openrewrite.java.tree.J;

import java.util.Collections;
import java.util.List;

class CommentsAndFormatting {
    private final List<J.Comment> comments;
    private final Formatting formatting;

    public CommentsAndFormatting(List<J.Comment> comments, Formatting formatting) {
        this.comments = comments;
        this.formatting = formatting;
    }

    public Formatting getFormatting() {
        return formatting;
    }

    public List<J.Comment> getComments() {
        return comments;
    }

    public static CommentsAndFormatting format(String prefix) {
        // FIXME actually parse comments
        return new CommentsAndFormatting(Collections.emptyList(), Formatting.format(prefix));
    }
}
