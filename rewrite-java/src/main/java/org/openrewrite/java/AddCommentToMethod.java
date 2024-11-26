/*
 * Copyright 2021 the original author or authors.
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
import org.jspecify.annotations.Nullable;
import org.openrewrite.*;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.search.DeclaresMethod;
import org.openrewrite.java.tree.Comment;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.TextComment;
import org.openrewrite.marker.Markers;

import java.util.List;

/**
 * This recipe adds a comment to a method in a Java source file. The comment can be a single line or a multiline comment.
 * <p>
 * The {@link AddCommentToMethod#comment} must be supplied and is the comment to add.
 * <p>
 * The {@link AddCommentToMethod#methodPattern} is a pattern to match methods to add the comment to.
 * <p>
 * The {@link AddCommentToMethod#isMultiline} is an optional flag (defaulted to false) to indicate if the comment is a multiline comment.
 */
@Value
@EqualsAndHashCode(callSuper = false)
public class AddCommentToMethod extends Recipe {
    @Override
    public String getDisplayName() {
        return "Add comment";
    }

    @Override
    public String getDescription() {
        return "Add a comment to a Java source file.";
    }

    @Option(displayName = "Comment",
            description = "The comment to add.",
            example = "This is a comment.")
    String comment;

    @Option(displayName = "Method pattern",
            description = "A pattern to match methods to add the comment to.",
            example = "java.util.List add*(..)")
    String methodPattern;

    @Option(displayName = "Multiline",
            description = "Comments use by default single line // but they can use multiline /* */.",
            required = false)
    @Nullable
    Boolean isMultiline;

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        MethodMatcher methodMatcher = new MethodMatcher(methodPattern);
        return Preconditions.check(new DeclaresMethod<>(methodMatcher), new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext ctx) {
                J.MethodDeclaration md = super.visitMethodDeclaration(method, ctx);
                J.ClassDeclaration cd = getCursor().firstEnclosingOrThrow(J.ClassDeclaration.class);
                List<Comment> comments = md.getComments();
                if (methodMatcher.matches(md, cd) && doesNotHaveComment(comments)) {
                    TextComment textComment = new TextComment(Boolean.TRUE.equals(isMultiline), comment, md.getPrefix().getWhitespace(), Markers.EMPTY);
                    return md.withComments(ListUtils.concat(comments, textComment));
                }
                return md;
            }

            private boolean doesNotHaveComment(List<Comment> comments) {
                for (Comment c : comments) {
                    if (c instanceof TextComment &&
                        comment.equals(((TextComment) c).getText())) {
                        return false;
                    }
                }
                return true;
            }
        });
    }
}
