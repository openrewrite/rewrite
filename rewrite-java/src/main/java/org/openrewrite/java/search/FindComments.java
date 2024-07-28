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
package org.openrewrite.java.search;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.*;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.Space;
import org.openrewrite.java.tree.TextComment;
import org.openrewrite.marker.SearchResult;

import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;

import static org.openrewrite.Tree.randomId;

@EqualsAndHashCode(callSuper = false)
@Value
public class FindComments extends Recipe {

    @Option(displayName = "Text patterns",
            description = "A list of regular expressions to search for.",
            example = "-----BEGIN RSA PRIVATE KEY-----")
    List<String> patterns;

    @Override
    public String getDisplayName() {
        return "Find within comments and literals";
    }

    @Override
    public String getDescription() {
        return "Find regular expression matches within comments and literals. \"Literals\" includes string literals, " +
               "character literals, and numeric literals.";
    }

    @Override
    public Validated<Object> validate() {
        return super.validate().and(Validated.test(
                "patterns",
                "Patterns must be compilable regular expressions",
                patterns, ps -> {
                    for (String p : ps) {
                        try {
                            Pattern.compile(p);
                        } catch (PatternSyntaxException e) {
                            return false;
                        }
                    }
                    return true;
                })
        );
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        List<Pattern> compiledPatterns = patterns.stream()
                .map(Pattern::compile)
                .collect(Collectors.toList());

        return new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public Space visitSpace(Space space, Space.Location loc, ExecutionContext ctx) {
                return space.withComments(ListUtils.map(space.getComments(), comment -> {
                    if(comment instanceof TextComment) {
                        for (Pattern p : compiledPatterns) {
                            if (p.matcher(((TextComment) comment).getText()).find()) {
                                return comment.withMarkers(comment.getMarkers().
                                        computeByType(new SearchResult(randomId(), null), (s1, s2) -> s1 == null ? s2 : s1));
                            }
                        }
                    }
                    return comment;
                }));
            }

            @Override
            public J.Literal visitLiteral(J.Literal literal, ExecutionContext ctx) {
                if (literal.getType() == JavaType.Primitive.Null) {
                    return literal;
                }

                assert literal.getValue() != null;
                if (compiledPatterns.stream().anyMatch(p -> p
                        .matcher(literal.getValue().toString()).find())) {
                    return SearchResult.found(literal);
                }

                return literal;
            }
        };
    }
}
