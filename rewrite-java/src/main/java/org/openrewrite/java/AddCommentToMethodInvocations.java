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
package org.openrewrite.java;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.*;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.search.UsesMethod;
import org.openrewrite.java.tree.Comment;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.TextComment;
import org.openrewrite.marker.Markers;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Value
@EqualsAndHashCode(callSuper = false)
public class AddCommentToMethodInvocations extends Recipe {
    @Override
    public String getDisplayName() {
        return "Add comment to method invocations";
    }

    @Override
    public String getDescription() {
        return "Add a comment to method invocations in a Java source file.";
    }

    @Option(displayName = "Comment",
            description = "The comment to add.",
            example = "This is a comment.")
    String comment;

    @Option(displayName = "Method pattern",
            description = "A pattern to match methods to add the comment to. " + MethodMatcher.METHOD_PATTERN_INVOCATIONS_DESCRIPTION,
            example = "java.util.List add*(..)")
    String methodPattern;

    @Option(displayName = "Multiline",
            description = "Comments use by default single line // but they can use multiline /* */.",
            required = false)
    @Nullable
    Boolean isMultiline;

    private static final Pattern NEWLINE = Pattern.compile("\\R");

    @Override
    public Validated<Object> validate() {
        return super.validate().and(MethodMatcher.validate(methodPattern));
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        MethodMatcher methodMatcher = new MethodMatcher(methodPattern);
        return Preconditions.check(new UsesMethod<>(methodMatcher), new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                J.MethodInvocation m = super.visitMethodInvocation(method, ctx);

                if (methodMatcher.matches(m)) {
                    String methodPrefixWhitespace = m.getPrefix().getWhitespace();

                    boolean createMultiline = Boolean.TRUE.equals(isMultiline);
                    Matcher matcher = NEWLINE.matcher(comment);
                    String newCommentText = matcher.find() ? matcher.replaceAll(createMultiline ? methodPrefixWhitespace : " ") : comment;

                    if (doesNotHaveComment(newCommentText, m.getComments())) {
                        TextComment textComment = new TextComment(createMultiline, newCommentText, methodPrefixWhitespace, Markers.EMPTY);
                        return m.withComments(ListUtils.concat(m.getComments(), textComment));
                    }
                }
                return m;
            }

            private boolean doesNotHaveComment(String lookFor, List<Comment> comments) {
                for (Comment c : comments) {
                    if (c instanceof TextComment &&
                            lookFor.equals(((TextComment) c).getText())) {
                        return false;
                    }
                }
                return true;
            }
        });
    }
}
