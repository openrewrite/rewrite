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
import org.jspecify.annotations.Nullable;
import org.openrewrite.*;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.search.FindImplementations;
import org.openrewrite.java.tree.Comment;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.TextComment;
import org.openrewrite.marker.Markers;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * This recipe adds a comment to a method in a Java source file. The comment can be a single line or a multiline comment.
 * <p>
 * The {@link AddCommentToClass#comment} must be supplied and is the comment to add.
 * <p>
 * The {@link AddCommentToClass#methodPattern} is a pattern to match methods to add the comment to.
 * <p>
 * The {@link AddCommentToClass#isMultiline} is an optional flag (defaulted to false) to indicate if the comment is a multiline comment.
 */
@Value
@EqualsAndHashCode(callSuper = false)
public class AddCommentToClass extends Recipe {
    @Override
    public String getDisplayName() {
        return "Add comment to Java class";
    }

    @Override
    public String getDescription() {
        return "Add a comment to Java class.";
    }

    @Option(displayName = "Comment",
        description = "The comment to add.",
        example = "This is a comment.")
    String comment;

    @Option(displayName = "Superclasses or interfaces",
        description = "The FQNs of the superclasses or interfaces to match class to add the comment to. ",
        example = "com.netflix.hystrix.HystrixCommand")
    List<String> implementations;

    @Option(displayName = "Multiline",
        description = "Comments use by default single line // but they can use multiline /* */.",
        required = false)
    @Nullable
    Boolean isMultiline;

    private static final Pattern NEWLINE = Pattern.compile("\\R");

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        List<TreeVisitor<?, ExecutionContext>> findImpls = implementations.stream().map(o -> new FindImplementations(o).getVisitor()).collect(Collectors.toList());
        TreeVisitor precondition = Preconditions.and(findImpls.toArray(new TreeVisitor[0]));

        return Preconditions.check(precondition, new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext ctx) {
                J.ClassDeclaration cd = super.visitClassDeclaration(classDecl, ctx);

                String classPrefixWhitespace = cd.getPrefix().getWhitespace();
                classPrefixWhitespace = classPrefixWhitespace.substring(0, classPrefixWhitespace.length() - 1);

                boolean isMultiline = Boolean.TRUE.equals(AddCommentToClass.this.isMultiline);
                Matcher matcher = NEWLINE.matcher(comment);
                String newCommentText = matcher.find() ? matcher.replaceAll(isMultiline ? classPrefixWhitespace : " ") : comment;

                if (doesNotHaveComment(newCommentText, cd.getComments())) {
                    TextComment textComment = new TextComment(isMultiline, newCommentText, classPrefixWhitespace, Markers.EMPTY);
                    return cd.withComments(ListUtils.concat(cd.getComments(), textComment));
                }
                return cd;
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
