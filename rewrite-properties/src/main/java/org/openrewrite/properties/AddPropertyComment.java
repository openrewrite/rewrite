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
package org.openrewrite.properties;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.*;
import org.openrewrite.properties.search.FindProperties;
import org.openrewrite.properties.tree.Properties;
import org.openrewrite.trait.Comments;

import static org.openrewrite.Tree.randomId;

@Value
@EqualsAndHashCode(callSuper = false)
public class AddPropertyComment extends Recipe {

    @Option(displayName = "Property key",
            description = "The name of the property to add comment.",
            example = "management.metrics.binders")
    String propertyKey;

    @Option(example = "comment", displayName = "Comment",
            description = "The comment to be added.")
    String comment;

    @Option(example = "true", displayName = "Comment out property",
            description = "If true, property will be commented out.",
            required = false)
    @Nullable
    Boolean commentOutProperty;

    String displayName = "Add comment before property key";

    String description = "Add a new comment before a property key if not already present, optionally commenting out the property.";

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        PropertiesVisitor<ExecutionContext> propertiesVisitor = new PropertiesVisitor<ExecutionContext>() {
            @Override
            public Properties visitFile(Properties.File file, ExecutionContext ctx) {
                // Insert the comment before each matching entry via the trait (the comment is a
                // preceding sibling, so the trait returns the owning file). Comment-out, when
                // requested, is applied afterwards as the file's entries are visited.
                Properties.File p = file;
                for (Properties.Content c : file.getContent()) {
                    if (c instanceof Properties.Entry && ((Properties.Entry) c).getKey().equals(propertyKey)) {
                        p = Comments.of(new Cursor(new Cursor(getCursor().getParentOrThrow(), p), c))
                                .comment(" " + comment.trim());
                    }
                }
                return super.visitFile(p, ctx);
            }

            @Override
            public Properties visitEntry(Properties.Entry entry, ExecutionContext ctx) {
                if (Boolean.TRUE.equals(commentOutProperty) && entry.getKey().equals(propertyKey)) {
                    return new Properties.Comment(
                            randomId(),
                            entry.getPrefix(),
                            entry.getMarkers(),
                            Properties.Comment.Delimiter.HASH_TAG,
                            " " + entry.printTrimmed(getCursor()));
                }
                return super.visitEntry(entry, ctx);
            }
        };
        return Preconditions.check(new FindProperties(propertyKey, false).getVisitor(), propertiesVisitor);
    }
}
