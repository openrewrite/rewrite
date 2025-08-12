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
import org.openrewrite.ExecutionContext;
import org.openrewrite.Option;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.tree.Comment;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.TextComment;
import org.openrewrite.marker.Markers;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.lang.System.lineSeparator;

@Value
@EqualsAndHashCode(callSuper = false)
public class AddCommentToImport extends Recipe {
    @Override
    public String getDisplayName() {
        return "Add comment to import statement";
    }

    @Override
    public String getDescription() {
        return "Add a comment to an import statement in a Java source file.";
    }

    @Option(displayName = "Comment",
            description = "The comment to add.",
            example = "This is a comment.")
    String comment;

    @Option(displayName = "Type pattern",
            description = "A type pattern that is used to find matching imports uses.",
            example = "org.springframework..*")
    String typePattern;

    @Override
    public String getInstanceNameSuffix() {
        return "matching `" + typePattern + "`";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        TypeMatcher typeMatcher = new TypeMatcher(typePattern);
        return new JavaIsoVisitor<ExecutionContext>() {
            private boolean foundImport = false;

            @Override
            public J.Import visitImport(J.Import anImport, ExecutionContext ctx) {
                if (foundImport) {
                    return anImport;
                }
                if (typeMatcher.matchesPackage(anImport.getTypeName())) {
                    foundImport = true;
                    String prefixWhitespace = anImport.getPrefix().getWhitespace();
                    Matcher newlineMatcher = Pattern.compile("\\R").matcher(comment.trim());
                    String newCommentText = comment.trim()
                            .replaceAll("\\R", prefixWhitespace + " * ")
                            .replace("*/", "*");
                    String formattedCommentText = newlineMatcher.find() ? lineSeparator() + " * " + newCommentText + lineSeparator() + " " : " " + newCommentText + " ";
                    if (doesNotHaveComment(formattedCommentText, anImport.getComments())) {
                        TextComment textComment = new TextComment(true, formattedCommentText, prefixWhitespace, Markers.EMPTY);
                        return autoFormat(anImport.withComments(ListUtils.concat(anImport.getComments(), textComment)), ctx);
                    }
                }
                return super.visitImport(anImport, ctx);
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
        };
    }
}
