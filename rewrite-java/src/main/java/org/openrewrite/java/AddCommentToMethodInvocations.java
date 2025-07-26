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
import org.openrewrite.*;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.search.UsesMethod;
import org.openrewrite.java.tree.*;
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
                    return addCommentToTargetElement(m, comment);
                }
                return m;
            }

            private <T extends J> T addCommentToTargetElement(T target, String comment) {
                String prefixWhitespace = target.getPrefix().getWhitespace();
                String newCommentText = comment.trim();
                Matcher matcher = NEWLINE.matcher(newCommentText);
                /* First Line * Second Line */
                newCommentText = " " + matcher.replaceAll(" * ") + " ";
                if (doesNotHaveComment(newCommentText, target.getComments())) {
                    TextComment textComment = new TextComment(true, newCommentText, prefixWhitespace, Markers.EMPTY);
                    return target.withComments(ListUtils.concat(target.getComments(), textComment));
                }
                return target;
            }

            private boolean doesNotHaveComment(String lookFor, List<Comment> comments) {
                for (Comment c : comments) {
                    if (c instanceof TextComment &&
                            lookFor.trim().equals(((TextComment) c).getText().trim())) {
                        return false;
                    }
                }
                return true;
            }
        });
    }
}
