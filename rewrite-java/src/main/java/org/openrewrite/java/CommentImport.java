/*
 * Copyright 2024 the original author or authors.
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

import lombok.EqualsAndHashCode;
import lombok.Value;
import lombok.With;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Option;
import org.openrewrite.Preconditions;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.search.UsesType;
import org.openrewrite.java.tree.Comment;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.TextComment;
import org.openrewrite.marker.Marker;
import org.openrewrite.marker.Markers;

import java.util.UUID;

@Value
@EqualsAndHashCode(callSuper = true)
public class CommentImport extends Recipe {

    @Option(displayName = "Fully-qualified type name",
            description = "Fully-qualified class name of the type.",
            example = "org.junit.Assume")
    String fullyQualifiedTypeName;

    @Option(displayName = "Comment text",
            description = "The text to add as a comment.",
            example = "This type was removed.")
    String commentText;

    @Option(displayName = "Multiline comment",
            description = "Add the comment as a multiline comment. Defaults to `false`.",
            required = false)
    @Nullable
    Boolean multiline;

    @Override
    public String getDisplayName() {
        return "Comments an import statement";
    }

    @Override
    public String getDescription() {
        return "Comments an import statement.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(
                new UsesType<>(fullyQualifiedTypeName, false),
                new JavaVisitor<ExecutionContext>() {
                    @Override
                    public J visitImport(J.Import import_, ExecutionContext ctx) {
                        import_ = (J.Import) super.visitImport(import_, ctx);
                        if (import_.getMarkers().findFirst(ImportAlreadyCommented.class).isPresent()) {
                            return import_;
                        }
                        if (import_.getTypeName().equals(fullyQualifiedTypeName)) {
                            boolean multilineComment = Boolean.TRUE.equals(multiline);
                            String formattedComment = multilineComment ? "\n" + commentText + "\n" : commentText;
                            Comment comment = new TextComment(multilineComment, formattedComment, import_.getPrefix().getWhitespace(), Markers.EMPTY);
                            return import_.withComments(ListUtils.concat(import_.getComments(), comment))
                                    .withMarkers(import_.getMarkers().add(new ImportAlreadyCommented(UUID.randomUUID())));
                        }
                        return import_;
                    }
                }
        );
    }

    @Value
    @With
    static class ImportAlreadyCommented implements Marker {
        UUID id;
    }
}
